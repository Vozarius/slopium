package dev.slopium.benchmark;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AnalysisScreen extends Screen {
    private final Screen parent;
    private double scroll;
    private int contentHeight;
    private boolean spark=SparkProfiler.available();
    public AnalysisScreen(Screen parent){super(text("title"));this.parent=parent;}
    private static Component text(String key,Object...args){return Component.translatable("slopium.analysis."+key,args);}
    @Override protected void init(){
        int y=height-118,buttonWidth=Math.min(360,width-24);
        for(boolean extended:new boolean[]{false,true}){
            int count=PerformanceAnalysis.trialCount(extended);
            var button=addRenderableWidget(Button.builder(text(extended?"extended":"standard",count,(count*PerformanceAnalysis.secondsPerTrial(extended)+(spark?180:0)+59)/60),b->PerformanceAnalysis.start(extended,spark)).bounds((width-buttonWidth)/2,y,buttonWidth,20).build());
            button.active=minecraft.level!=null&&!PerformanceAnalysis.running()&&!SparkProfiler.busy();
            if(SparkProfiler.busy())button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(text("spark_busy")));y+=24;
        }
        addRenderableWidget(Button.builder(text("spark",spark?Component.translatable("options.on"):Component.translatable("options.off")),b->{spark=!spark;init();}).bounds((width-buttonWidth)/2,y,buttonWidth,20).build()).active=SparkProfiler.available();
        addRenderableWidget(Button.builder(Component.translatable("gui.back"),b->onClose()).bounds(width/2-80,height-28,160,20).build());
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        scroll=Math.clamp(scroll-vertical*22,0,Math.max(0,contentHeight-(height-162)));return true;
    }
    @Override public void render(GuiGraphics g,int mx,int my,float dt){
        renderBackground(g,mx,my,dt);super.render(g,mx,my,dt);g.drawCenteredString(font,title,width/2,12,0xffffff);
        g.enableScissor(15,32,width-15,height-127);
        int y=38-(int)scroll;for(String key:new String[]{"intro","conditions","restore","coverage"}){
            for(var line:font.split(text(key),Math.min(560,width-40))){g.drawString(font,line,Math.max(20,(width-560)/2),y,0xdddddd);y+=font.lineHeight+2;}y+=5;
        }
        contentHeight=y-38+(int)scroll;g.disableScissor();
        if(contentHeight>height-162)g.drawString(font,Component.literal("↕"),width-15,36,0xaaaaaa);
    }
}
