package dev.slopium.client;
import net.caffeinemc.mods.sodium.client.config.structure.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.contents.TranslatableContents;
public final class ExactNumberInput {
    private ExactNumberInput(){}
    public static boolean open(Option option,int button){
        if(button!=1&&!(button==0&&Screen.hasShiftDown()))return false;
        if(!(option instanceof IntegerOption integer)||!option.isEnabled())return false;
        if(!(option.getName().getContents() instanceof TranslatableContents text)||!text.getKey().startsWith("slopium."))return false;
        var mc=Minecraft.getInstance();mc.setScreen(new NumberEntryScreen(mc.screen,integer));return true;
    }
}
