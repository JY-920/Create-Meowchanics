package cn.laowu.mod.client;

import cn.laowu.mod.CreatureFilterMenu;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.CreatureFilterRules;
import com.simibubi.create.foundation.gui.widget.IconButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import java.util.List;

/** Run on the render thread after the existing world probe has loaded a player. */
public final class CreatureFilterClientProbe {
    private static final int WIDTH = 600, HEIGHT = 400;
    private CreatureFilterClientProbe() {}

    public static void verify(Minecraft mc) throws Exception {
        check(mc.level != null && mc.player != null, "GUI probe requires world");
        var previousScreen=mc.screen;var previousMenu=mc.player.containerMenu;
        int slot=mc.player.getInventory().selected;var oldStack=mc.player.getInventory().getItem(slot);
        try {
            var stack=new ItemStack(LaoWuMod.CREATURE_FILTER.get());mc.player.getInventory().setItem(slot,stack);
            var groupMenu=new CreatureFilterMenu(30000,mc.player.getInventory(),stack);
            var groupScreen=new CreatureFilterScreen(groupMenu,mc.player.getInventory(),Component.translatable("item.laowu.creature_filter"));
            mc.player.containerMenu=groupMenu;mc.setScreen(groupScreen);groupScreen.init(mc,WIDTH,HEIGHT);
            check(groupScreen.children().stream().filter(com.simibubi.create.foundation.gui.widget.SelectionScrollInput.class::isInstance)
                .map(com.simibubi.create.foundation.gui.widget.SelectionScrollInput.class::cast).filter(w->w.visible).count()==4,
                "Entity and all three attribute selectors are visible together");
            var editor=field(groupScreen,"groupEditor",Object.class);
            var adult=nested(editor,"adult",com.simibubi.create.foundation.gui.widget.SelectionScrollInput.class);
            var tame=nested(editor,"tame",com.simibubi.create.foundation.gui.widget.SelectionScrollInput.class);
            var attributeMode=nested(editor,"attributeMode",com.simibubi.create.foundation.gui.widget.SelectionScrollInput.class);
            var health=nested(editor,"value",EditBox.class);
            addManual(groupScreen,field(groupScreen,"search",EditBox.class),"minecraft:cat");
            check(groupMenu.rules().groups().size()==1,"Enter adds one selected entity without closing UI");
            check(field(groupScreen,"selector",com.simibubi.create.foundation.gui.widget.SelectionScrollInput.class)
                .getToolTip().stream().anyMatch(c->c.getString().contains("✓")),
                "Native entity list immediately marks a newly configured entry as saved");
            tame.setState(1);tame.onChanged();health.setValue("<=50");
            check(groupMenu.rules().groups().get(0).conditions().size()==2,"Direct tame and percentage edits save to the selected cat");
            for(var modeName:List.of("whitelistDis","blacklist","whitelistCon")) {
                click(groupScreen,field(groupScreen,modeName,IconButton.class));
                var expected=modeName.equals("whitelistDis")?CreatureFilterRules.Mode.WHITELIST:
                    modeName.equals("blacklist")?CreatureFilterRules.Mode.BLACKLIST:CreatureFilterRules.Mode.WHITELIST_ALL;
                check(groupMenu.rules().mode()==expected&&field(groupScreen,modeName,IconButton.class).green,
                    "Footer changes global mode and exactly the clicked selected state");
                check(groupMenu.rules().groups().get(0).mode()==CreatureFilterRules.Mode.WHITELIST_ALL,
                    "Footer must not overwrite the selected entity attribute mode");
            }
            addManual(groupScreen,field(groupScreen,"search",EditBox.class),"minecraft:cow");
            adult.setState(1);adult.onChanged();
            check(groupMenu.rules().groups().get(1).conditions().size()==1&&groupMenu.rules().groups().get(0).conditions().size()==2,
                "Cow age config does not alter cat health/tame config");
            field(groupScreen,"search",EditBox.class).setValue("minecraft:cat");
            var targetWheel=field(groupScreen,"selector",com.simibubi.create.foundation.gui.widget.SelectionScrollInput.class);
            targetWheel.onChanged();
            check(tame.getState()==1&&health.getValue().equals("<=50"),"Selecting saved species restores its controls");
            health.setValue("NaN");
            check(!field(groupScreen,"addInverted",IconButton.class).active&&groupMenu.rules().groups().get(0).conditions().size()==2,
                "Invalid numeric draft cannot mutate or submit a filter");
            health.setValue("<=40");
            capture(mc,groupScreen,"creature-filter-two-row.png");
            var clickable=groupScreen.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast)
                .filter(w->w.visible&&(w instanceof IconButton||w instanceof EditBox||
                    w instanceof com.simibubi.create.foundation.gui.widget.SelectionScrollInput)).toList();
            for(int i=0;i<clickable.size();i++)for(int j=i+1;j<clickable.size();j++) {
                var a=clickable.get(i);var b=clickable.get(j);
                if((a==field(groupScreen,"search",EditBox.class)&&b==targetWheel)||(b==field(groupScreen,"search",EditBox.class)&&a==targetWheel))continue;
                check(a.getX()+a.getWidth()<=b.getX()||b.getX()+b.getWidth()<=a.getX()
                    ||a.getY()+a.getHeight()<=b.getY()||b.getY()+b.getHeight()<=a.getY(),
                    "Visible controls cannot overlap: "+a.getClass().getSimpleName()+" / "+b.getClass().getSimpleName());
            }
            groupScreen.init(mc,320,240);bounds(groupScreen,groupMenu,320,240);
            capture(mc,groupScreen,"creature-filter-two-row-compact.png",320,240);
            groupMenu.rules().write(stack);
            var reopened=new CreatureFilterMenu(30002,mc.player.getInventory(),stack);
            check(reopened.rules().groups().size()==2&&reopened.rules().conditionCount()==3
                &&reopened.rules().mode()==CreatureFilterRules.Mode.WHITELIST_ALL,"Entry attributes and global mode persist on reopen");
            editor=field(groupScreen,"groupEditor",Object.class);
            health=nested(editor,"value",EditBox.class);
            adult=nested(editor,"adult",com.simibubi.create.foundation.gui.widget.SelectionScrollInput.class);
            health.setValue("101");adult.setState(1);adult.onChanged();health.setValue("<=40");
            check(groupMenu.rules().groups().get(0).conditions().stream().anyMatch(c->c.attribute()==CreatureFilterRules.Attribute.ADULT),
                "Correcting invalid health must not discard an independent age edit");
            var full=new java.util.ArrayList<CreatureFilterRules.AttributeCondition>();
            for(int i=0;i<64;i++)full.add(new CreatureFilterRules.AttributeCondition(CreatureFilterRules.Attribute.HEALTH,
                CreatureFilterRules.Comparison.GE,i,false));
            groupMenu.configure(CreatureFilterRules.grouped(List.of(new CreatureFilterRules.Group(
                ResourceLocation.tryParse("minecraft:cat"),CreatureFilterRules.Mode.WHITELIST_ALL,full))));
            groupScreen.init(mc,WIDTH,HEIGHT);
            editor=field(groupScreen,"groupEditor",Object.class);
            adult=nested(editor,"adult",com.simibubi.create.foundation.gui.widget.SelectionScrollInput.class);
            adult.setState(1);adult.onChanged();
            check(groupMenu.rules().conditionCount()==64&&adult.getState()==0,
                "Editing a full legacy filter must reject excess attributes without throwing or displaying unsaved values");
            field(groupScreen,"search",EditBox.class).setValue("minecraft:wolf");
            tame=nested(editor,"tame",com.simibubi.create.foundation.gui.widget.SelectionScrollInput.class);
            tame.setState(1);tame.onChanged();
            check(!field(groupScreen,"add",IconButton.class).active&&!field(groupScreen,"addInverted",IconButton.class).active,
                "New entity draft cannot be submitted over the shared attribute budget");
            groupMenu.clearContents();groupScreen.contentsCleared();
            check(field(groupScreen,"search",EditBox.class).getValue().isEmpty(),"Clearing filters also clears the old selection label");
            mc.setScreen(groupScreen);
            check(mc.screen==groupScreen,"Editing does not close filter screen");
            stack=new ItemStack(LaoWuMod.CREATURE_FILTER.get());mc.player.getInventory().setItem(slot,stack);

            CreatureFilterRules.conditions(CreatureFilterRules.Mode.WHITELIST_ALL,List.of()).write(stack);
            var menu=new CreatureFilterMenu(30001,mc.player.getInventory(),stack);
            var screen=new CreatureFilterScreen(menu,mc.player.getInventory(),Component.translatable("item.laowu.creature_filter"));
            mc.player.containerMenu=menu;mc.setScreen(screen);screen.init(mc,WIDTH,HEIGHT);
            check(screen.children().stream().filter(EditBox.class::isInstance).count()==1,"Exactly one search field");
            check(origin(screen,"imageWidth")==241,"Retain native 241px width");
            var selector=screen.children().stream().filter(com.simibubi.create.foundation.gui.widget.SelectionScrollInput.class::isInstance)
                    .map(com.simibubi.create.foundation.gui.widget.SelectionScrollInput.class::cast).findFirst().orElseThrow();
            check(selector.getClass()==com.simibubi.create.foundation.gui.widget.SelectionScrollInput.class,"Use unmodified native list widget");
            check(field(screen,"summaryIcon",ItemStack.class).is(net.minecraft.world.item.Items.NAME_TAG),"Native NAME_TAG condition summary");
            bounds(screen,menu,WIDTH,HEIGHT);
            capture(mc,screen,"creature-filter-native-panel.png");
            var search=field(screen,"search",EditBox.class);
            screen.mouseClicked(origin(screen,"leftPos")+40,origin(screen,"topPos")+27,0);
            check(search.isFocused()&&screen.charTyped('c',0)&&search.getValue().equals("c"),"Whole native input well accepts typing");
            search.setValue("");
            int resultCount=entries(screen).size();
            scroll(screen,selector.getX()+10,selector.getY()+8,-1);
            check(selector.getState()==1&&integer(screen,"selection")==1,"Native wheel changes selected result");
            check(search.getValue().equals(Component.translatable("gui.laowu.creature_filter.category_all",
                    Component.translatable("gui.laowu.creature_filter.category.neutral")).getString()),
                    "Scrolling must show selected creature/category name in the input");
            check(entries(screen).size()==resultCount,"Reflecting a selection must not shrink search results");
            check(menu.rules().conditions().isEmpty()&&menu.rules().mode()==CreatureFilterRules.Mode.WHITELIST_ALL,"Browsing does not change legacy rules");
            check(selector.getToolTip().stream().anyMatch(line->line.getString().contains("->")),"Native tooltip lists selected entry");
            capture(mc,screen,"creature-filter-native-selector.png");
            search.setValue("minecraft:zombie"); click(screen,field(screen,"add",IconButton.class));
            var zombie=ResourceLocation.tryParse("minecraft:zombie");
            check(menu.rules().conditions().equals(List.of(CreatureFilterRules.Condition.entity(zombie,false))),"Positive condition add");
            check(mc.screen==screen,"Adding must not close the UI");
            check(search.isFocused(),"Mouse Add keeps search focused after parent event dispatch");
            check(screen.charTyped('c',0)&&search.getValue().equals("c"),"Immediate typing replaces selected name after mouse Add");
            search.setValue("minecraft:zombie");
            check(!field(screen,"add",IconButton.class).active&&!field(screen,"addInverted",IconButton.class).active,"Both duplicate forms disabled");
            search.setValue(net.minecraft.world.entity.EntityType.COW.getDescription().getString());
            click(screen,field(screen,"addInverted",IconButton.class));
            check(search.isFocused(),"Mouse inverted Add keeps search focused");
            check(menu.rules().conditions().contains(CreatureFilterRules.Condition.entity(ResourceLocation.tryParse("minecraft:cow"),true)),"Right second button adds inverted condition, not remove");
            search.setValue("hostile"); click(screen,field(screen,"add",IconButton.class));
            check(menu.rules().conditions().contains(CreatureFilterRules.Condition.category(CreatureFilterRules.Category.HOSTILE,false)),"Category is a condition in same list");
            click(screen,field(screen,"whitelistCon",IconButton.class));
            check(menu.rules().mode()==CreatureFilterRules.Mode.WHITELIST_ALL,"Middle bottom button is AND");
            click(screen,field(screen,"blacklist",IconButton.class));
            check(menu.rules().mode()==CreatureFilterRules.Mode.BLACKLIST,"Right bottom button is NOR");
            click(screen,field(screen,"whitelistDis",IconButton.class));
            check(menu.rules().mode()==CreatureFilterRules.Mode.WHITELIST,"Left bottom button is OR");
            search.setValue("removedpack:creature"); click(screen,search);
            check(screen.keyPressed(GLFW.GLFW_KEY_ENTER,0,0),"Search Enter adds full ID");
            check(menu.rules().entityIds().contains(ResourceLocation.tryParse("removedpack:creature")),"Unknown valid ID preserved");
            search.setValue("minecraft:player");
            check(entries(screen).isEmpty()&&!field(screen,"add",IconButton.class).active,"Players cannot be added");
            search.setValue("Minecraft:bad ID");check(entries(screen).isEmpty(),"Invalid ID produced a candidate");
            screen.mouseClicked(origin(screen,"leftPos")+40,origin(screen,"topPos")+27,0);
            check(search.isFocused()&&selector.isMouseOver(origin(screen,"leftPos")+40,origin(screen,"topPos")+27),
                    "Empty search accepts whole-well focus and native hover feedback");
            check(selector.getToolTip().stream().anyMatch(line->line.getString().contains(
                    Component.translatable("gui.laowu.creature_filter.no_results").getString())),"Native no-results message");
            search.setValue("");
            for(int i=0;i<3;i++)screen.keyPressed(GLFW.GLFW_KEY_DOWN,0,0);
            check(selector.getState()==3&&entries(screen).size()==resultCount,"Keyboard reflects name without shrinking list");
            capture(mc,screen,"creature-filter-native-selected-selector.png");
            screen.init(mc,320,240);bounds(screen,menu,320,240);
            capture(mc,screen,"creature-filter-compact-native-panel.png",320,240);
            search=field(screen,"search",EditBox.class);search.setValue("cow");click(screen,search);
            check(screen.keyPressed(GLFW.GLFW_KEY_E,0,0)&&screen.charTyped('e',0)&&search.getValue().equals("cowe"),"Inventory key remains text input");
            var icons=screen.children().stream().filter(IconButton.class::isInstance).map(IconButton.class::cast).toList();
            var reset=icons.stream().filter(b->b.getX()==originUnchecked(screen,"leftPos")+179&&b.getY()==originUnchecked(screen,"topPos")+79).findFirst().orElseThrow();
            click(screen,reset);check(menu.rules().conditions().isEmpty(),"Native reset clears all conditions");
            System.out.println("[CreatureFilterClientProbe] PASS native dropdown/search synchronization, NAME_TAG, OR/AND/NOR, positive/inverted add, duplicate guard, native panel pixel-seam check, keyboard/wheel/focus, compact inventory");
        } finally {mc.player.getInventory().setItem(slot,oldStack);mc.player.containerMenu=previousMenu;mc.setScreen(previousScreen);}
    }
    private static int originUnchecked(CreatureFilterScreen screen,String field){
        try{return origin(screen,field);}catch(Exception e){throw new RuntimeException(e);}
    }

    private static void addManual(CreatureFilterScreen screen, EditBox manual, String value) {
        click(screen, manual);
        manual.setValue(value);
        check(screen.keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0), "manual Enter was not handled");
    }
    private static void click(CreatureFilterScreen screen, AbstractWidget widget) {
        check(widget.active, "cannot click inactive widget: " + widget.getMessage().getString());
        double x = widget.getX() + widget.getWidth() / 2.0, y = widget.getY() + widget.getHeight() / 2.0;
        check(screen.mouseClicked(x, y, 0), "mouse click not handled: " + widget.getMessage().getString());
        screen.mouseReleased(x, y, 0);
    }
    private static void scroll(CreatureFilterScreen screen, double x, double y, double delta) {
        screen.mouseScrolled(x, y, 0, delta);
    }
    private static int integer(CreatureFilterScreen screen, String name) throws Exception {
        return field(screen, name, Integer.class);
    }
    @SuppressWarnings("unchecked")
    private static List<Button> buttons(CreatureFilterScreen screen, String name) throws Exception {
        return field(screen, name, List.class);
    }
    @SuppressWarnings("unchecked")
    private static List<CreatureFilterMenu.EntityEntry> entries(CreatureFilterScreen screen) throws Exception {
        return field(screen, "filtered", List.class);
    }
    private static <T> T field(CreatureFilterScreen screen, String name, Class<T> type) throws Exception {
        var field = CreatureFilterScreen.class.getDeclaredField(name);
        field.setAccessible(true); return type.cast(field.get(screen));
    }
    private static <T>T nested(Object instance,String name,Class<T> type)throws Exception {
        var f=instance.getClass().getDeclaredField(name);f.setAccessible(true);return type.cast(f.get(instance));
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError("[CreatureFilterClientProbe] " + message);
    }
    private static void bounds(CreatureFilterScreen screen, CreatureFilterMenu menu, int width, int height) throws Exception {
        for (var child : screen.children()) if (child instanceof AbstractWidget widget)
            check(widget.getX() >= 0 && widget.getY() >= 0
                            && widget.getX() + widget.getWidth() <= width && widget.getY() + widget.getHeight() <= height,
                    width + "x" + height + " clips widget " + widget.getMessage().getString());
        int left = origin(screen, "leftPos"), top = origin(screen, "topPos");
        check(menu.slots.size() == 36, "compact GUI must retain the complete player inventory");
        check(menu.slots.get(0).x == 51 && menu.slots.get(0).y == 125,
                "Inventory slots must align with native centered inventory including the -11px window offset");
        for (var slot : menu.slots)
            check(left + slot.x >= 0 && top + slot.y >= 0
                            && left + slot.x + 16 <= width && top + slot.y + 16 <= height,
                    width + "x" + height + " clips inventory slot " + slot.index);
    }
    private static int origin(CreatureFilterScreen screen, String name) throws Exception {
        for (Class<?> type = screen.getClass(); type != null; type = type.getSuperclass()) {
            try {
                var field = type.getDeclaredField(name); field.setAccessible(true); return field.getInt(screen);
            } catch (NoSuchFieldException ignored) {}
        }
        throw new AssertionError("Missing container origin " + name);
    }

    private static void capture(Minecraft mc, CreatureFilterScreen screen, String filename) throws Exception {
        capture(mc, screen, filename, WIDTH, HEIGHT);
    }
    private static void capture(Minecraft mc, CreatureFilterScreen screen, String filename, int width, int height) throws Exception {
        var projection = new org.joml.Matrix4f(com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix());
        var sorting = com.mojang.blaze3d.systems.RenderSystem.getVertexSorting();
        var view = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        view.pushMatrix(); view.identity();
        com.mojang.blaze3d.pipeline.TextureTarget output = null;
        try (var guard = new CatPerformanceOutline.State(); var framebuffer = new PerformanceSceneSnapshot.Target()) {
            output = new com.mojang.blaze3d.pipeline.TextureTarget(width * 2, height * 2, true, Minecraft.ON_OSX);
            output.bindWrite(true);
            com.mojang.blaze3d.systems.RenderSystem.clearColor(.08f, .1f, .12f, 1);
            com.mojang.blaze3d.systems.RenderSystem.clearDepth(1);
            com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
            com.mojang.blaze3d.systems.RenderSystem.clear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT | org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(new org.joml.Matrix4f().setOrtho(0, width, height, 0, 1000, 21000),
                    com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1, 1, 1, 1);
            com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
            var graphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
            graphics.pose().translate(0, 0, -11000);
            boolean hoverSelector=filename.contains("selector");
            screen.render(graphics, hoverSelector?origin(screen,"leftPos")+110:-100, hoverSelector?origin(screen,"topPos")+34:-100, 0);
            graphics.flush();
            try (var image = new com.mojang.blaze3d.platform.NativeImage(width * 2, height * 2, false)) {
                com.mojang.blaze3d.systems.RenderSystem.bindTexture(output.getColorTextureId());
                image.downloadTexture(0, false); image.flipY();
                image.writeToFile(java.nio.file.Path.of(filename));
                if(filename.contains("two-row")) {
                    int left=origin(screen,"leftPos"),top=origin(screen,"topPos");
                    for(int[] well:new int[][]{{14,25},{14,47},{62,47},{110,47},{158,47}})
                        for(int y=2;y<18;y++)
                            check((image.getPixelRGBA((left+well[0]+1)*2,(top+well[1]+y)*2)&0xffffff)==0x373737,
                                "Input well left border must be continuous, without the source dropdown arrow notch");
                    for(int x:new int[]{14+43-2,62+43-2,110+43-2,158+60-2})
                        check((image.getPixelRGBA((left+x)*2,(top+49)*2)&0xffffff)==0xffffff,
                            "Native input well must retain its complete white right border");
                }
                if(filename.contains("native-panel")) {
                    int left=origin(screen,"leftPos"),top=origin(screen,"topPos");
                    for(int y=25;y<45;y++)for(int x=14;x<34;x++)for(int dx=0;dx<2;dx++)for(int dy=0;dy<2;dy++)
                        check(image.getPixelRGBA((left+x)*2+dx,(top+y)*2+dy)
                                ==image.getPixelRGBA((left+4+(x-14)%2)*2+dx,(top+y)*2+dy),
                                "Reference slot removal must match native background checker phase at "+x+","+y);
                }
            }
        } finally {
            if (output != null) output.destroyBuffers();
            view.popMatrix();
            com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(projection, sorting);
        }
    }
}
