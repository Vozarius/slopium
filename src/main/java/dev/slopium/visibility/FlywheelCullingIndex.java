package dev.slopium.visibility;

import dev.engine_room.flywheel.api.visual.*;
import dev.slopium.client.*;
import dev.slopium.mixin.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Worker threads publish lifecycle events; only the render thread consumes and changes instances. */
public final class FlywheelCullingIndex {
    private static final CullingWorkBudget BUDGET = new CullingWorkBudget(350_000,512,System::nanoTime);
    private record Change(Visual visual, boolean add) {}
    private final ConcurrentLinkedQueue<Change> changes = new ConcurrentLinkedQueue<>();
    private final IdentityHashMap<Visual,Entry> entries = new IdentityHashMap<>();
    private final IdentityHashMap<BlockEntity,Entry> byBlock = new IdentityHashMap<>();
    private final Set<Entry> altered = Collections.newSetFromMap(new IdentityHashMap<>());
    private final NavigableSet<Entry> due = new TreeSet<>(Comparator.<Entry>comparingLong(e->e.next).thenComparingLong(e->e.order));
    private Iterator<Entry> restoring;
    private long order;
    private Object world;
    private int policy=-1;
    private long selectionRevision=-1;
    private BlockEntity aimed;
    private net.minecraft.world.phys.Vec3 lastEye;
    private long lastCamera=Long.MIN_VALUE,lastGeometry=Long.MIN_VALUE;
    private int supported;
    private boolean nativeDepth, gpuModels;

    private static final class Entry {
        Visual stored;
        BlockEntityVisual<?> visual;
        BlockEntity block;
        VisualOcclusionState state;
        InstanceVisibility visibility;
        OwnedInstances owner;
        boolean alive=true, paused, supported, cameraDependent;
        long next, order, checked=-1, validated=-1;
        String reason="not checked";
        Entry(Visual stored) { this.stored=stored; }
    }
    public void add(Visual visual) { if(visual instanceof BlockEntityVisual<?>) changes.add(new Change(visual,true)); }
    public void remove(Visual visual) { if(visual instanceof BlockEntityVisual<?>) changes.add(new Change(visual,false)); }
    public void clear() { changes.add(new Change(null,false)); }
    public int tracked() { return entries.size(); }
    public int supported() { return supported; }
    public int altered() { return altered.size(); }
    public boolean restoring() { return restoring!=null; }

    private static Visual unwrap(Visual visual) {
        for(int i=0;i<8;i++) {
            if(!(visual instanceof WrappedVisualAccess wrapper)) return visual;
            Visual next=wrapper.slopium$delegateVisual();
            if(next==null || next==visual) return null;
            visual=next;
        }
        return null;
    }
    private void register(Visual stored) {
        if(entries.containsKey(stored)) return;
        Entry e=new Entry(stored);e.owner=OwnedInstances.get(stored);
        Visual inner=unwrap(stored);
        if(inner instanceof VisualBlockEntityAccess access) {
            e.block=access.slopium$getBlockEntity();
        }
        if(e.owner!=null)e.block=e.owner.block;
        if(e.block!=null)byBlock.put(e.block,e);
        if(e.owner!=null&&stored instanceof BlockEntityVisual<?> visual&&e.block!=null){
            e.visual=visual;e.state=e.owner;e.supported=true;supported++;schedule(e,0);
        } else
        if(inner instanceof BlockEntityVisual<?> visual && inner instanceof VisualOcclusionState state
                && e.block!=null) {
            e.visual=visual; e.state=state; e.supported=true; supported++;
            schedule(e,0);
        }
        entries.put(stored,e);
        if(SlopiumConfig.get().diagnostics) {
            SlopiumStats.flywheelRegistered++;
            if(stored instanceof WrappedVisualAccess) SlopiumStats.flywheelVisualsWrapped++;
            if(!e.supported) SlopiumStats.flywheelVisualsUnsupported++;
        }
    }
    private void unregister(Visual stored) {
        Entry e=entries.remove(stored);
        if(e==null) return;
        e.alive=false;OwnedInstances.remove(stored);
        if(e.supported) due.remove(e);
        if(e.block!=null) byBlock.remove(e.block,e);
        if(e.supported) supported--;
        altered.remove(e); // Flywheel deleted these instances; never restore them.
    }
    private void schedule(Entry e,long frame) { e.next=frame; e.order=order++; due.add(e); }
    private void restore(Entry e) {
        if(e.visibility!=null) { e.visibility.restore(); e.visibility=null; }
        e.paused=false;e.cameraDependent=false;
        if(e.state!=null) e.state.slopium$setOccluded(false,Slopium.frame());
        e.reason="restored after world/settings change";
    }
    public void frame() { frame(false); }
    public void frame(boolean nativeDepth) {
        var c=SlopiumConfig.get();
        this.nativeDepth=nativeDepth;
        gpuModels=c.enabled && ProcessedBlocks.all() && c.occludeFlywheel && nativeDepth && (!c.flywheelAnyBlock||GpuGlassDepth.available());
        SlopiumStats.flywheelNativeDepth=nativeDepth;
        SlopiumStats.flywheelGpuModels=gpuModels;
        boolean active=c.enabled&&(c.disableFlywheelRendering||(c.occludeFlywheel&&!gpuModels)||c.occludeCreateAnimations||c.freezeCreateAnimations||(c.cameraCulling&&!nativeDepth));
        if(!active && altered.isEmpty() && changes.isEmpty()) return;
        long started=System.nanoTime(), frame=Slopium.frame();
        BUDGET.begin(frame);
        var mc=Minecraft.getInstance();
        int nextPolicy=(c.enabled?1:0)|(c.occludeFlywheel?2:0)|(c.occludeCreateAnimations?4:0)|(c.freezeCreateAnimations?8:0)
            |(c.flywheelAnyBlock?16:0)|(c.animationAnyBlock?32:0)|(nativeDepth?64:0)|(gpuModels?128:0)|(c.cameraCulling?256:0)|(c.disableFlywheelRendering?512:0)|(c.visibleSections?1024:0);
        try {
            if(policy!=nextPolicy || world!=mc.level || selectionRevision!=ProcessedBlocks.revision()) {
                policy=nextPolicy;world=mc.level;selectionRevision=ProcessedBlocks.revision();
                if(restoring==null && !altered.isEmpty()) restoring=altered.iterator();
            }
            // Only settings/world changes restore all instances. Camera movement is handled by the shared pool.
            while(restoring!=null && restoring.hasNext() && BUDGET.take()) {
                restore(restoring.next()); restoring.remove();
            }
            if(restoring!=null) {
                if(restoring.hasNext()) return;
                restoring=null;
            }
            // Reveal before the slow hide queue. This only touches previously altered visuals,
            // reuses one blocker per pool entry and never walks world voxels or waits for a ray budget.
            var eye=mc.gameRenderer.getMainCamera().getPosition();
            long camera=VisibilityPool.cameraRevision(),geometry=WallOcclusion.epoch();
            boolean sceneChanged=!eye.equals(lastEye)||geometry!=lastGeometry;
            boolean needsReveal=(!c.disableFlywheelRendering&&((c.occludeFlywheel&&!gpuModels)||(c.cameraCulling&&!nativeDepth)))
                ||(c.occludeCreateAnimations&&!c.freezeCreateAnimations);
            if(needsReveal&&(sceneChanged||camera!=lastCamera)){
                var it=altered.iterator();
                while(it.hasNext()){
                    Entry e=it.next();
                    if(!sceneChanged&&!e.cameraDependent)continue;
                    evaluate(e,c,frame,true);
                    if(e.visibility==null&&!e.paused)it.remove();
                    // Keep its existing hide-queue slot. Revealing needs no TreeSet reordering.
                }
            }
            lastEye=eye;lastCamera=camera;lastGeometry=geometry;
            // Reserve work for existing entries even during bulk chunk loading.
            for(int i=0;i<128 && !changes.isEmpty() && BUDGET.take();i++) {
                Change change=changes.poll();
                if(change.visual==null) {
                    for(var key:entries.keySet())OwnedInstances.remove(key);
                    entries.clear();byBlock.clear();altered.clear();due.clear();supported=0;
                } else if(change.add) register(change.visual); else unregister(change.visual);
            }
            if(!active) return;
            // Prioritize an aimed mechanism, but never flash it on in explicit full-hide/freeze modes.
            BlockEntity target=mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit && mc.level!=null
                ? mc.level.getBlockEntity(hit.getBlockPos()) : null;
            if(target!=aimed && target!=null && !c.disableFlywheelRendering && !c.freezeCreateAnimations) {
                Entry e=byBlock.get(target);
                if(e!=null && altered.contains(e)) {
                    if(!BUDGET.take()) return;
                    restore(e);altered.remove(e);
                }
            }
            aimed=target;
            while(!due.isEmpty() && due.first().next<=frame && BUDGET.take()) {
                Entry e=due.pollFirst();
                if(!e.alive) continue;
                evaluate(e,c,frame,false);
                schedule(e,frame+(e.reason.equals("deferred")?1:e.visibility!=null||e.paused?8:20));
            }
        } finally {
            BUDGET.end();
            if(c.diagnostics) {
                long elapsed=System.nanoTime()-started;
                SlopiumStats.flywheelCullPasses++;
                SlopiumStats.flywheelCullNanos+=elapsed;
                SlopiumStats.flywheelCullMaxNanos=Math.max(SlopiumStats.flywheelCullMaxNanos,elapsed);
                SlopiumStats.flywheelTracked=entries.size();SlopiumStats.flywheelSupported=supported;
                SlopiumStats.flywheelAltered=altered.size();
                if(restoring!=null || (!due.isEmpty()&&due.first().next<=frame) || !changes.isEmpty()) SlopiumStats.flywheelBudgetYields++;
            }
        }
    }
    private void evaluate(Entry e,SlopiumConfig c,long frame,boolean reveal) {
        if(!ProcessedBlocks.allows(e.block)){restore(e);if(!reveal)altered.remove(e);e.reason="excluded by block filter";return;}
        if(c.diagnostics) SlopiumStats.flywheelVisualsChecked++;
        var mc=Minecraft.getInstance();
        if(e.block.getLevel()!=mc.level || mc.level==null
                || (e.block instanceof com.simibubi.create.foundation.blockEntity.SmartBlockEntity smart && smart.isVirtual())
                || SableGuard.protects(mc.level,e.block.getBlockPos())) {
            restore(e);if(!reveal)altered.remove(e);e.reason="protected world / virtual block";return;
        }
        boolean outside=c.cameraCulling&&(!nativeDepth||c.occludeCreateAnimations)&&CreateWallControl.outsideCamera(e.block);
        e.cameraDependent=outside;
        Boolean hidden=c.disableFlywheelRendering?Boolean.TRUE:outside&&!nativeDepth?Boolean.TRUE:c.occludeFlywheel&&!gpuModels?CreateWallControl.modelDecision(e.block,c.flywheelAnyBlock,false):Boolean.FALSE;
        final Boolean paused;
        if(c.freezeCreateAnimations) {
            paused=e.block.getLevel()==mc.level && mc.level!=null
                && (!(e.block instanceof com.simibubi.create.foundation.blockEntity.SmartBlockEntity smart)||!smart.isVirtual())
                && !SableGuard.protects(mc.level,e.block.getBlockPos());
        } else if(c.occludeCreateAnimations) {
            // Preserve null (deferred). Mixing primitive boolean and Boolean in a ternary unboxes it.
            paused=c.occludeFlywheel&&!gpuModels&&c.animationAnyBlock==c.flywheelAnyBlock
                ?hidden:CreateWallControl.modelDecision(e.block,c.animationAnyBlock,c.cameraCulling);
        } else { paused=Boolean.FALSE; }
        e.checked=frame;
        boolean deferred=hidden==null||paused==null;
        if(hidden==null)hidden=false;
        // Never keep a stale model/CPU pause merely because the background trace queue is busy.
        boolean pause=Boolean.TRUE.equals(paused);
        if(!deferred)e.validated=frame;
        e.reason=deferred?"deferred":hidden?"hidden":pause?"animation paused":"visible / protected";
        e.paused=pause;
        // Stable worker snapshot; explicitly restored when policy or visibility changes.
        e.state.slopium$setOccludedUntil(pause,frame,Long.MAX_VALUE);
        if(hidden && (!reveal || e.visibility!=null)) {
            if(e.visibility==null) e.visibility=new InstanceVisibility();
            int count=reveal?0:e.owner==null?e.visibility.hide(e.visual):e.visibility.hide(e.owner);
            if(c.diagnostics) SlopiumStats.wallFlywheelInstancesHidden+=count;
        } else if(!hidden&&e.visibility!=null) { e.visibility.restore();e.visibility=null; }
        if(!reveal){if(e.visibility!=null||e.paused) altered.add(e);else altered.remove(e);}
    }
    public String describe(BlockEntity block) {
        Entry e=byBlock.get(block);
        if(e==null) return "Flywheel: not registered yet / no visual";
        String renderer=nativeDepth?"indirect (native GPU depth)":"CPU fallback";
        String modelPolicy=gpuModels?"native GPU; GPU result not read back":"Slopium CPU / settings";
        Visual inner=unwrap(e.stored);
        return "Flywheel: backend="+renderer+" | models="+modelPolicy+" | "+(inner==null?"unknown wrapper":inner.getClass().getName())+" | supported="+e.supported
            +" | state="+e.reason+" | age="+(e.checked<0?"never":Slopium.frame()-e.checked)+" frames";
    }
}
