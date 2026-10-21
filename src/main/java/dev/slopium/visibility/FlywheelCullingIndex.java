package dev.slopium.visibility;

import dev.engine_room.flywheel.api.visual.*;
import dev.slopium.client.*;
import dev.slopium.mixin.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Worker threads publish lifecycle events; only the render thread consumes and changes instances. */
public final class FlywheelCullingIndex {
    private static final Set<String> AUDITED = Set.of(
        "com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual",
        "com.simibubi.create.content.kinetics.base.ShaftVisual",
        "com.simibubi.create.content.kinetics.base.OrientedRotatingVisual",
        "com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntityVisual$LargeCogVisual",
        "com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogVisual",
        "com.simibubi.create.content.kinetics.gearbox.GearboxVisual",
        "com.simibubi.create.content.kinetics.fan.FanVisual",
        "com.simibubi.create.content.kinetics.transmission.SplitShaftVisual",
        "com.simibubi.create.content.kinetics.press.PressVisual",
        "com.simibubi.create.content.kinetics.mixer.MixerVisual",
        "com.simibubi.create.content.logistics.funnel.FunnelVisual");
    private static final CullingWorkBudget BUDGET = new CullingWorkBudget(750_000,512,System::nanoTime);
    private record Change(Visual visual, boolean add) {}
    private final ConcurrentLinkedQueue<Change> changes = new ConcurrentLinkedQueue<>();
    private final IdentityHashMap<Visual,Entry> entries = new IdentityHashMap<>();
    private final IdentityHashMap<BlockEntity,Entry> byBlock = new IdentityHashMap<>();
    private final Set<Entry> altered = Collections.newSetFromMap(new IdentityHashMap<>());
    private final NavigableSet<Entry> due = new TreeSet<>(Comparator.<Entry>comparingLong(e->e.next).thenComparingLong(e->e.order));
    private Iterator<Entry> restoring;
    private long order, viewChanged;
    private Vec3 camera;
    private Object world;
    private int policy=-1;
    private BlockEntity aimed;
    private int supported;

    private static final class Entry {
        Visual stored;
        BlockEntityVisual<?> visual;
        BlockEntity block;
        VisualOcclusionState state;
        InstanceVisibility visibility;
        boolean alive=true, paused, supported;
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
        Entry e=new Entry(stored);
        Visual inner=unwrap(stored);
        if(inner instanceof VisualBlockEntityAccess access) {
            e.block=access.slopium$getBlockEntity();
            if(e.block!=null) byBlock.put(e.block,e);
        }
        if(inner instanceof BlockEntityVisual<?> visual && inner instanceof VisualOcclusionState state
                && e.block!=null && AUDITED.contains(inner.getClass().getName())) {
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
        e.alive=false;
        if(e.supported) due.remove(e);
        if(e.block!=null) byBlock.remove(e.block,e);
        if(e.supported) supported--;
        altered.remove(e); // Flywheel deleted these instances; never restore them.
    }
    private void schedule(Entry e,long frame) { e.next=frame; e.order=order++; due.add(e); }
    private void restore(Entry e,float seconds) {
        if(e.visibility!=null) { e.visibility.restore(); e.visibility=null; }
        if(e.paused) e.visual.collectCrumblingInstances(instance->{
            if(instance instanceof PausedRotation rotation) rotation.slopium$pauseRotation(false,seconds);
        });
        e.paused=false;
        if(e.state!=null) e.state.slopium$setOccluded(false,Slopium.frame());
        e.reason="restored after camera/settings change";
    }
    public void frame() {
        var c=SlopiumConfig.get();
        boolean active=c.enabled&&(c.occludeFlywheel||c.occludeCreateAnimations||c.freezeCreateAnimations);
        if(!active && altered.isEmpty() && changes.isEmpty()) return;
        long started=System.nanoTime(), frame=Slopium.frame();
        BUDGET.begin(frame);
        CreateOcclusion.limitUntil(BUDGET.deadline());
        var mc=Minecraft.getInstance();
        float seconds=(((LevelRendererAccessor)mc.levelRenderer).slopium$ticks()+mc.getTimer().getGameTimeDeltaPartialTick(true))/20f;
        int nextPolicy=(c.enabled?1:0)|(c.occludeFlywheel?2:0)|(c.occludeCreateAnimations?4:0)|(c.freezeCreateAnimations?8:0)
            |(c.flywheelAnyBlock?16:0)|(c.animationAnyBlock?32:0)|(mc.screen!=null?64:0);
        Vec3 eye=mc.gameRenderer.getMainCamera().getPosition();
        try {
            if(policy!=nextPolicy || world!=mc.level || !eye.equals(camera)) {
                policy=nextPolicy;world=mc.level;camera=eye;viewChanged=frame;
                if(restoring==null && !altered.isEmpty()) restoring=altered.iterator();
            }
            // Restoration takes priority, including global off. Work remains bounded during movement.
            while(restoring!=null && restoring.hasNext() && BUDGET.take()) {
                restore(restoring.next(),seconds); restoring.remove();
            }
            if(restoring!=null) {
                if(restoring.hasNext()) return;
                restoring=null;
            }
            // Reserve work for existing entries even during bulk chunk loading.
            for(int i=0;i<128 && !changes.isEmpty() && BUDGET.take();i++) {
                Change change=changes.poll();
                if(change.visual==null) {
                    entries.clear();byBlock.clear();altered.clear();due.clear();supported=0;
                } else if(change.add) register(change.visual); else unregister(change.visual);
            }
            if(!active || frame-viewChanged<2) return;
            // Immediately prioritize the mechanism under the crosshair without scanning the index.
            BlockEntity target=mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit && mc.level!=null
                ? mc.level.getBlockEntity(hit.getBlockPos()) : null;
            if(target!=aimed && target!=null) {
                Entry e=byBlock.get(target);
                if(e!=null && altered.contains(e)) {
                    if(!BUDGET.take()) return;
                    restore(e,seconds);altered.remove(e);
                }
            }
            aimed=target;
            while(!due.isEmpty() && due.first().next<=frame && BUDGET.take()) {
                Entry e=due.pollFirst();
                if(!e.alive) continue;
                evaluate(e,c,seconds,frame);
                schedule(e,frame+(e.reason.equals("deferred")?1:e.visibility!=null||e.paused?8:20));
            }
        } finally {
            CreateOcclusion.limitUntil(Long.MAX_VALUE);
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
    private void evaluate(Entry e,SlopiumConfig c,float seconds,long frame) {
        if(c.diagnostics) SlopiumStats.flywheelVisualsChecked++;
        var mc=Minecraft.getInstance();
        Boolean hidden=c.occludeFlywheel?CreateWallControl.decision(e.block,c.flywheelAnyBlock):Boolean.FALSE;
        final Boolean paused;
        if(c.freezeCreateAnimations) {
            paused=e.block.getLevel()==mc.level && mc.level!=null
                && (!(e.block instanceof com.simibubi.create.foundation.blockEntity.SmartBlockEntity smart)||!smart.isVirtual())
                && !SableGuard.protects(mc.level,e.block.getBlockPos());
        } else if(c.occludeCreateAnimations) {
            // Preserve null (deferred). Mixing primitive boolean and Boolean in a ternary unboxes it.
            paused=c.occludeFlywheel&&c.animationAnyBlock==c.flywheelAnyBlock
                ?hidden:CreateWallControl.decision(e.block,c.animationAnyBlock);
        } else { paused=Boolean.FALSE; }
        e.checked=frame;
        if(hidden==null || paused==null) {
            if(frame-e.validated>20) { restore(e,seconds);altered.remove(e); }
            e.reason="deferred";return;
        }
        e.validated=frame;
        e.reason=hidden?"hidden":paused?"animation paused":"visible / protected";
        if(paused || e.paused) e.visual.collectCrumblingInstances(instance->{
            if(instance instanceof PausedRotation rotation) rotation.slopium$pauseRotation(paused,seconds);
        });
        e.paused=paused;
        // CPU pause expires if the scheduler cannot revisit this entry in time.
        e.state.slopium$setOccludedUntil(paused,frame,frame+8);
        if(hidden) {
            if(e.visibility==null) e.visibility=new InstanceVisibility();
            int count=e.visibility.hide(e.visual);
            if(c.diagnostics) SlopiumStats.wallFlywheelInstancesHidden+=count;
        } else if(e.visibility!=null) { e.visibility.restore();e.visibility=null; }
        if(e.visibility!=null||e.paused) altered.add(e);else altered.remove(e);
    }
    public String describe(BlockEntity block) {
        Entry e=byBlock.get(block);
        if(e==null) return "Flywheel: not registered yet / no visual";
        Visual inner=unwrap(e.stored);
        return "Flywheel: "+(inner==null?"unknown wrapper":inner.getClass().getName())+" | supported="+e.supported
            +" | state="+e.reason+" | age="+(e.checked<0?"never":Slopium.frame()-e.checked)+" frames";
    }
}
