package dev.slopium.client;
import dev.slopium.visibility.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
/** Local draft. Done commits only the filter; Escape/Cancel discard it. */
public final class ProcessedBlocksScreen extends Screen {
    private final Screen parent;
    private BlockSelectionMode mode=SlopiumConfig.get().processedBlockMode;
    private final Set<String> ids=new HashSet<>(SlopiumConfig.get().processedBlockIds);
    private EditBox search;private String query="";private int page;
    private List<ProcessedBlocks.Entry> visible=List.of();
    public ProcessedBlocksScreen(Screen parent){super(Component.translatable("slopium.processed_blocks"));this.parent=parent;}
    private Component text(String key,Object...args){return Component.translatable("slopium.blocks."+key,args);}
    private int rows(){return Math.max(1,(height-154)/28);}
    @Override protected void init(){
        clearWidgets();
        addRenderableWidget(Button.builder(text("mode."+mode),b->{mode=BlockSelectionMode.values()[(mode.ordinal()+1)%3];init();}).bounds(width/2-160,32,320,20).build());
        search=addRenderableWidget(new EditBox(font,width/2-160,58,320,20, text("search")));search.setMaxLength(256);search.setHint(text("search"));search.setValue(query);
        search.setResponder(value->{query=value;page=0;refreshRows();});
        fill();
    }
    private void refreshRows(){boolean focused=search.isFocused();int cursor=search.getCursorPosition();init();if(focused){setFocused(search);search.moveCursorTo(cursor,false);}}
    private void fill(){
        String needle=query.toLowerCase(Locale.ROOT);
        visible=ProcessedBlocks.catalog().stream().filter(e->e.id().contains(needle)||e.block().getName().getString().toLowerCase(Locale.ROOT).contains(needle)).toList();
        int pages=Math.max(1,(visible.size()+rows()-1)/rows());page=Math.clamp(page,0,pages-1);
        for(int i=0;i<rows()&&page*rows()+i<visible.size();i++){
            var e=visible.get(page*rows()+i);int y=85+i*28;
            var button=Button.builder(Component.literal((ids.contains(e.id())?"[x] ":"[ ] ")+e.id()),b->{if(!ids.add(e.id()))ids.remove(e.id());init();})
                .bounds(width/2-200,y,400,20).tooltip(Tooltip.create(e.block().getName().copy().append(e.flywheel()?" · Flywheel":" · Create"))).build();
            button.active=mode!=BlockSelectionMode.ALL;addRenderableWidget(button);
        }
        addRenderableWidget(Button.builder(Component.literal("<"),b->{page--;init();}).bounds(width/2-160,height-60,40,20).build()).active=page>0;
        addRenderableWidget(Button.builder(Component.literal(">"),b->{page++;init();}).bounds(width/2+120,height-60,40,20).build()).active=page+1<pages;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->{var c=SlopiumConfig.get();c.processedBlockMode=mode;c.processedBlockIds=new ArrayList<>(new TreeSet<>(ids));Slopium.applyOptions();onClose();}).bounds(width/2-102,height-28,100,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"),b->onClose()).bounds(width/2+2,height-28,100,20).build());
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public void render(GuiGraphics g,int x,int y,float dt){renderBackground(g,x,y,dt);super.render(g,x,y,dt);g.drawCenteredString(font,title,width/2,12,0xffffff);
        g.drawCenteredString(font,text("count",visible.size(),ids.size(),page+1,Math.max(1,(visible.size()+rows()-1)/rows())),width/2,height-54,0xaaaaaa);}
}
