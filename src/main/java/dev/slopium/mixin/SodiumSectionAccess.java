package dev.slopium.mixin;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=SodiumWorldRenderer.class,remap=false)
public interface SodiumSectionAccess {
    @Accessor("renderSectionManager") RenderSectionManager slopium$sections();
}
