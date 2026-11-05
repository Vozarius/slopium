package dev.slopium.mixin;
import net.caffeinemc.mods.sodium.client.render.chunk.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value=RenderSectionManager.class,remap=false)
public interface SectionManagerAccess {
    @Invoker("getRenderSection") RenderSection slopium$section(int x,int y,int z);
}
