package dev.slopium.smoke;
import dev.slopium.client.NumberEntryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.*;
import net.minecraft.client.gui.components.EditBox;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.minecraft.network.chat.contents.TranslatableContents;
import java.util.*;
/** Dispatch from the actual screen, not directly to a constructed Sodium slider. */
final class SmokeNumberRouting {
    static void run(){
        var mc=Minecraft.getInstance();var parent=mc.screen;
        var row=find(parent,Collections.newSetFromMap(new IdentityHashMap<>()));
        if(row==null)throw new AssertionError("Actual target FPS row not in screen tree");
        int x=-1,y=-1;
        search: for(int py=2;py<mc.getWindow().getGuiScaledHeight()-2;py+=2)
            for(int px=4;px<mc.getWindow().getGuiScaledWidth()-4;px+=4)
                if(row.isMouseOver(px,py)){x=px;y=py;break search;}
        if(x<0)throw new AssertionError("Target FPS row is not visible");
        if(!parent.mouseClicked(x,y+2,1)||!(mc.screen instanceof NumberEntryScreen))throw new AssertionError("Screen did not route right click: "+row.getClass());
        var edit=(EditBox)mc.screen.children().stream().filter(e->e instanceof EditBox).findFirst().orElseThrow();edit.setValue("2");
        mc.screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER,0,0);
        if(mc.screen!=parent||!SmokeOptionsChecks.option("frame_generation_input").getValidatedValue().equals(2))throw new AssertionError("Routed input did not persist pending value");
        com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: actual screen -> {} -> number editor -> exact 2 -> original screen passed",row.getClass().getName());
    }
    private static GuiEventListener find(GuiEventListener element,Set<Object> visited){
        if(!visited.add(element))return null;
        try{
            var method=element.getClass().getMethod("getOption");method.setAccessible(true);
            if(method.invoke(element) instanceof Option o&&o.getName().getContents() instanceof TranslatableContents text&&text.getKey().equals("slopium.frame_generation_input"))return element;
        }catch(NoSuchMethodException ignored){}catch(ReflectiveOperationException e){throw new AssertionError(e);}
        if(element instanceof ContainerEventHandler parent)for(var child:parent.children()){var found=find(child,visited);if(found!=null)return found;}
        return null;
    }
}
