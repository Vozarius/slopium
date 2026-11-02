package dev.slopium.client;
import net.caffeinemc.mods.sodium.client.config.structure.IntegerOption;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
/** Editing changes Sodium's pending value; only its Apply button writes configuration. */
public final class NumberEntryScreen extends Screen {
    private final Screen parent;private final IntegerOption option;
    private EditBox input;private Button confirm;
    public NumberEntryScreen(Screen parent,IntegerOption option){super(option.getName());this.parent=parent;this.option=option;}
    public static Integer parse(String text,int min,int max,int step){
        try{int n=Integer.parseInt(text);return n>=min&&n<=max&&(n-min)%step==0?n:null;}catch(NumberFormatException e){return null;}
    }
    private Integer value(){var range=option.getSteppedValidator();return parse(input.getValue(),range.min(),range.max(),range.step());}
    @Override protected void init(){
        input=addRenderableWidget(new EditBox(font,width/2-100,height/2-15,200,20,title));
        input.setMaxLength(11);input.setFilter(s->s.matches("-?\\d{0,10}"));
        input.setValue(Integer.toString(option.getValidatedValue()));input.setHighlightPos(0);
        confirm=addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->accept()).bounds(width/2-102,height/2+45,100,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"),b->onClose()).bounds(width/2+2,height/2+45,100,20).build());
        input.setResponder(s->confirm.active=value()!=null&&option.isEnabled());setInitialFocus(input);confirm.active=value()!=null;
    }
    private void accept(){Integer n=value();if(n!=null&&option.isEnabled()){option.modifyValue(n);onClose();}}
    @Override public boolean keyPressed(int key,int scan,int mods){if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER){accept();return true;}return super.keyPressed(key,scan,mods);}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g,mx,my,partial);super.render(g,mx,my,partial);
        g.drawCenteredString(font,title,width/2,height/2-48,0xFFFFFF);
        var r=option.getSteppedValidator();g.drawCenteredString(font,Component.translatable("slopium.number.range",r.min(),r.max(),r.step()),width/2,height/2+12,value()==null?0xFF7777:0xAAAAAA);
    }
}
