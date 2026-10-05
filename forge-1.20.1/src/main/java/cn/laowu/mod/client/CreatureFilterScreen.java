package cn.laowu.mod.client;

import cn.laowu.mod.CreatureFilterMenu;
import cn.laowu.mod.item.CreatureFilterRules;
import cn.laowu.mod.network.ModNetwork;
import com.simibubi.create.content.logistics.filter.AbstractFilterScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.gui.widget.Label;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** Create attribute-filter semantics, with searchable creature conditions in place of a reference item. */
public final class CreatureFilterScreen extends AbstractFilterScreen<CreatureFilterMenu> {
    private static final ResourceLocation FILTERS = ResourceLocation.tryParse("create:textures/gui/filters.png");
    private static final int EXTENSION = 18;
    private static final int FOOTER_Y = 55 + EXTENSION;
    private record Entry(ResourceLocation id, CreatureFilterRules.Category category, Component name) {
        CreatureFilterRules.Condition condition(boolean inverted) {
            return id == null ? CreatureFilterRules.Condition.category(category,inverted)
                    : CreatureFilterRules.Condition.entity(id,inverted);
        }
    }
    private final List<Entry> filtered = new ArrayList<>();
    private final ItemStack summaryIcon = new ItemStack(Items.NAME_TAG);
    private EditBox search;
    private IconButton add, addInverted, whitelistDis, whitelistCon, blacklist;
    private SelectionScrollInput selector;
    private String query = "";
    private boolean reflectingSelection, displayingSelection;
    private int selection;
    private boolean forceGrouped;
    private GroupEditor groupEditor;

    /** One entity selection row and one immediately editable attribute row. */
    private final class GroupEditor {
        private SelectionScrollInput attributeMode, adult, tame;
        private EditBox value;
        private IconButton deleteGroup, removeLegacy;
        private boolean restoring;
        private String reflectedText="";
        private int currentIndex() {
            if(filtered.isEmpty())return -1;
            var entry=filtered.get(selection);
            for(int i=0;i<menu.rules().groups().size();i++) {
                var g=menu.rules().groups().get(i);
                if(Objects.equals(g.target(),entry.id())&&g.category()==entry.category())return i;
            }
            return -1;
        }
        private CreatureFilterRules.Group current() {
            int i=currentIndex();return i<0?null:menu.rules().groups().get(i);
        }
        void init() {
            whitelistDis=icon(38,79,AllIcons.I_WHITELIST_OR,"mode.whitelist",()->mode(CreatureFilterRules.Mode.WHITELIST));
            whitelistCon=icon(56,79,AllIcons.I_WHITELIST_AND,"mode.whitelist_all",()->mode(CreatureFilterRules.Mode.WHITELIST_ALL));
            blacklist=icon(74,79,AllIcons.I_WHITELIST_NOT,"mode.blacklist",()->mode(CreatureFilterRules.Mode.BLACKLIST));
            add=icon(164,26,AllIcons.I_ADD,"add_entity",()->add(false));
            addInverted=icon(182,26,AllIcons.I_ADD_INVERTED_ATTRIBUTE,"exclude_entity",()->add(true));
            deleteGroup=icon(200,26,AllIcons.I_TRASH,"delete_entity",this::delete);
            removeLegacy=icon(110,79,AllIcons.I_TRASH,"legacy_remove",()->commit(menu.rules().withoutRetainedLegacy()));
            selector=addRenderableWidget(new SelectionScrollInput(leftPos+15,topPos+26,144,18));
            selector.forOptions(List.of(Component.translatable("gui.laowu.creature_filter.no_results")));
            selector.titled(Component.translatable("gui.laowu.creature_filter.search"));
            selector.calling(i->{selection=i;showSelection();reflectedText=search.getValue();restore();});
            search=addRenderableWidget(new EditBox(font,leftPos+19,topPos+31,132,10,
                Component.translatable("gui.laowu.creature_filter.search")));
            search.setBordered(false);search.setTextColor(0xF3EBDE);search.setMaxLength(CreatureFilterRules.MAX_ID_LENGTH);
            search.setHint(Component.translatable("gui.laowu.creature_filter.search"));
            search.setResponder(text->{
                if(reflectingSelection||text.equals(reflectedText))return;
                query=text;selection=0;displayingSelection=false;rebuildTargets();restore();
            });
            attributeMode=choice(14,43,"attribute_mode",List.of("match_all","match_any","match_none"),()->update(null));
            adult=choice(62,43,"adult_setting",List.of("any_age","adult_only","baby_only"),()->update("age"));
            tame=choice(110,43,"tame_setting",List.of("any_tame","tamed_only","untamed_only"),()->update("tame"));
            value=addRenderableWidget(new EditBox(font,leftPos+162,topPos+53,52,10,
                Component.translatable("gui.laowu.creature_filter.health_setting")));
            value.setBordered(false);value.setMaxLength(16);value.setTextColor(0xF3EBDE);
            value.setHint(Component.translatable("gui.laowu.creature_filter.health_placeholder"));
            value.setResponder(text->{if(!restoring)update("health");});
            query="";selection=0;rebuildTargets();
            if(!menu.rules().groups().isEmpty()) {
                var first=menu.rules().groups().get(0);
                for(int i=0;i<filtered.size();i++)if(Objects.equals(first.target(),filtered.get(i).id())&&first.category()==filtered.get(i).category()){selection=i;break;}
                selector.setState(selection);showSelection();reflectedText=search.getValue();
            }
            restore();
        }
        SelectionScrollInput choice(int x,int width,String title,List<String> options,Runnable action) {
            var input=addRenderableWidget(new SelectionScrollInput(leftPos+x+1,topPos+48,width-2,18));
            var label=addRenderableWidget(new Label(leftPos+x+4,topPos+53,Component.empty()).colored(0xF3EBDE));
            input.forOptions(options.stream().map(k->Component.translatable("gui.laowu.creature_filter."+k)).toList());
            input.titled(Component.translatable("gui.laowu.creature_filter."+title)).writingTo(label);
            input.calling(i->{if(!restoring)action.run();});
            return input;
        }
        void rebuildTargets() {
            filtered.clear();String q=query.trim().toLowerCase(Locale.ROOT);
            var all=Component.translatable("gui.laowu.creature_filter.all_mobs");
            if(q.isEmpty()||matches(q,"*",all))filtered.add(new Entry(null,null,all));
            for(var c:CreatureFilterRules.Category.values())if(matches(q,c.id(),categoryName(c)))
                filtered.add(new Entry(null,c,categoryName(c)));
            for(var e:menu.catalog())if(matches(q,e.id().toString(),e.name()))filtered.add(new Entry(e.id(),null,e.name()));
            for(var g:menu.rules().groups())if(g.target()!=null&&matches(q,g.target().toString(),entityName(g.target()))
                &&filtered.stream().noneMatch(e->g.target().equals(e.id())))filtered.add(new Entry(g.target(),null,entityName(g.target())));
            var id=ResourceLocation.tryParse(query.trim());
            if(q.contains(":")&&id!=null&&!id.toString().equals("minecraft:player")&&filtered.stream().noneMatch(e->id.equals(e.id())))
                filtered.add(new Entry(id,null,entityName(id)));
            // Saved entries first, so the native wheel is also the configured-entity list.
            filtered.sort(Comparator.comparingInt(e->menu.rules().groups().stream()
                .anyMatch(g->Objects.equals(g.target(),e.id())&&g.category()==e.category())?0:1));
            selection=Mth.clamp(selection,0,Math.max(0,filtered.size()-1));
            selector.forOptions(filtered.isEmpty()?List.of(Component.translatable("gui.laowu.creature_filter.no_results")):
                filtered.stream().map(e->{
                    boolean saved=menu.rules().groups().stream().anyMatch(g->Objects.equals(g.target(),e.id())&&g.category()==e.category());
                    return Component.literal(saved?"✓ ":"").append(e.name());
                }).toList());
            selector.setState(selection);refresh();
        }
        CreatureFilterRules.Mode attributeMode() {
            return switch(attributeMode.getState()){case 1->CreatureFilterRules.Mode.WHITELIST;case 2->CreatureFilterRules.Mode.BLACKLIST;default->CreatureFilterRules.Mode.WHITELIST_ALL;};
        }
        CreatureFilterRules.AttributeCondition health() {
            String text=value.getValue().trim().replace("%","");
            if(text.isEmpty())return null;
            var matcher=java.util.regex.Pattern.compile("(<=|>=|<|>|=|≤|≥)?\\s*(\\d+(?:\\.\\d+)?)").matcher(text);
            if(!matcher.matches())throw new IllegalArgumentException("Invalid percentage");
            String op=matcher.group(1);
            var comparison=op==null?CreatureFilterRules.Comparison.LE:switch(op){
                case "<"->CreatureFilterRules.Comparison.LT;case ">"->CreatureFilterRules.Comparison.GT;
                case ">=","≥"->CreatureFilterRules.Comparison.GE;case "="->CreatureFilterRules.Comparison.EQ;
                default->CreatureFilterRules.Comparison.LE;};
            return new CreatureFilterRules.AttributeCondition(CreatureFilterRules.Attribute.HEALTH_PERCENT,comparison,Double.parseDouble(matcher.group(2)),false);
        }
        boolean validHealth() {try{health();return true;}catch(IllegalArgumentException e){return false;}}
        List<CreatureFilterRules.AttributeCondition> conditions(String family) {
            var g=current();var cs=new ArrayList<>(g==null?List.<CreatureFilterRules.AttributeCondition>of():g.conditions());
            if(family==null)return cs;
            cs.removeIf(c->switch(family){
                case "age"->c.attribute()==CreatureFilterRules.Attribute.ADULT||c.attribute()==CreatureFilterRules.Attribute.BABY;
                case "tame"->c.attribute()==CreatureFilterRules.Attribute.TAMED||c.attribute()==CreatureFilterRules.Attribute.UNTAMED;
                case "health"->c.attribute()==CreatureFilterRules.Attribute.HEALTH_PERCENT;
                default->false;});
            CreatureFilterRules.Attribute a=null;
            if(family.equals("age")&&adult.getState()!=0)a=adult.getState()==1?CreatureFilterRules.Attribute.ADULT:CreatureFilterRules.Attribute.BABY;
            if(family.equals("tame")&&tame.getState()!=0)a=tame.getState()==1?CreatureFilterRules.Attribute.TAMED:CreatureFilterRules.Attribute.UNTAMED;
            if(a!=null)cs.add(new CreatureFilterRules.AttributeCondition(a,CreatureFilterRules.Comparison.EQ,1,false));
            if(family.equals("health")&&health()!=null)cs.add(health());
            return cs;
        }
        void update(String family) {
            if(restoring)return;
            boolean valid=validHealth();
            value.setTextColor(valid?0xF3EBDE:0xFF7777);
            if(!valid&&"health".equals(family)){refresh();return;}
            var g=current();
            if(g!=null) {
                var changed=conditions(family);
                if(menu.rules().conditionCount()-g.conditions().size()+changed.size()>CreatureFilterRules.MAX_CONDITIONS){restore();return;}
                replace(new CreatureFilterRules.Group(g.target(),g.category(),attributeMode(),changed,g.inverted()));
            }
            refresh();
        }
        void restore() {
            if(value==null)return;
            restoring=true;
            try {
                var g=current();
                attributeMode.setState(g==null||g.mode()==CreatureFilterRules.Mode.WHITELIST_ALL?0:g.mode()==CreatureFilterRules.Mode.WHITELIST?1:2);
                adult.setState(0);tame.setState(0);value.setValue("");
                if(g!=null)for(var c:g.conditions()) {
                    switch(c.attribute()) {
                        case ADULT->adult.setState(c.inverted()?2:1);
                        case BABY->adult.setState(c.inverted()?1:2);
                        case TAMED->tame.setState(c.inverted()?2:1);
                        case UNTAMED->tame.setState(c.inverted()?1:2);
                        case HEALTH_PERCENT->{
                            if(!c.inverted())value.setValue(switch(c.comparison()){case EQ->"=";case LT->"<";case LE->"<=";case GT->">";case GE->">=";}
                                +new java.math.BigDecimal(c.value()).stripTrailingZeros().toPlainString());
                        }
                        default->{}
                    }
                }
                value.setTextColor(0xF3EBDE);
            } finally {restoring=false;}
            refresh();
        }
        void replace(CreatureFilterRules.Group g) {
            int i=currentIndex();if(i<0)return;
            var gs=new ArrayList<>(menu.rules().groups());gs.set(i,g);commit(menu.rules().withGroups(gs));
        }
        void add(boolean inverted) {
            if(filtered.isEmpty()||!validHealth())return;
            var g=current();
            if(g!=null) {replace(new CreatureFilterRules.Group(g.target(),g.category(),g.mode(),g.conditions(),inverted));return;}
            var gs=new ArrayList<>(menu.rules().groups());if(gs.size()>=CreatureFilterRules.MAX_GROUPS)return;
            var entry=filtered.get(selection);var cs=new ArrayList<CreatureFilterRules.AttributeCondition>();
            cs.addAll(conditions("age"));cs.addAll(conditions("tame"));cs.addAll(conditions("health"));
            if(menu.rules().conditionCount()+cs.size()>CreatureFilterRules.MAX_CONDITIONS)return;
            gs.add(new CreatureFilterRules.Group(entry.id(),entry.category(),attributeMode(),cs,inverted));
            commit(menu.rules().withGroups(gs));refreshCatalogue();showSelection();reflectedText=search.getValue();restore();
        }
        void delete() {
            int i=currentIndex();if(i<0)return;
            var gs=new ArrayList<>(menu.rules().groups());gs.remove(i);commit(menu.rules().withGroups(gs));refreshCatalogue();restore();
        }
        void refreshCatalogue() {
            var selected=filtered.isEmpty()?null:filtered.get(selection);
            rebuildTargets();
            if(selected!=null)for(int i=0;i<filtered.size();i++)
                if(Objects.equals(selected.id(),filtered.get(i).id())&&selected.category()==filtered.get(i).category()){
                    selection=i;selector.setState(i);break;
                }
        }
        void mode(CreatureFilterRules.Mode mode) {commit(menu.rules().withEntryMode(mode));}
        void clear() {
            query="";reflectedText="";reflectingSelection=true;
            try{search.setValue("");}finally{reflectingSelection=false;}
            selection=0;rebuildTargets();restore();
        }
        void refresh() {
            if(value==null)return;
            var g=current();boolean valid=!filtered.isEmpty()&&validHealth();
            if(g==null&&menu.rules().conditionCount()+(adult.getState()==0?0:1)+(tame.getState()==0?0:1)
                +(value.getValue().isBlank()?0:1)>CreatureFilterRules.MAX_CONDITIONS)valid=false;
            add.active=valid&&(g==null?menu.rules().groups().size()<CreatureFilterRules.MAX_GROUPS:g.inverted());
            addInverted.active=valid&&(g==null?menu.rules().groups().size()<CreatureFilterRules.MAX_GROUPS:!g.inverted());
            deleteGroup.active=g!=null;
            whitelistDis.green=menu.rules().mode()==CreatureFilterRules.Mode.WHITELIST;
            whitelistCon.green=menu.rules().mode()==CreatureFilterRules.Mode.WHITELIST_ALL;
            blacklist.green=menu.rules().mode()==CreatureFilterRules.Mode.BLACKLIST;
            removeLegacy.visible=removeLegacy.active=menu.rules().retainedLegacy()!=null;
        }
    }
    public CreatureFilterScreen(CreatureFilterMenu menu, Inventory inventory, Component title) {
        super(menu,inventory,title,AllGuiTextures.ATTRIBUTE_FILTER);
    }
    @Override protected void init() {
        int previousSelection = selection;
        boolean previousDisplay = displayingSelection;
        String previousQuery = query;
        setWindowOffset(-11,7);
        super.init();
        topPos -= EXTENSION / 2;
        setWindowSize(background.getWidth(), background.getHeight() + EXTENSION + 4
                + AllGuiTextures.PLAYER_INVENTORY.getHeight());
        for (var listener : children())
            if (listener instanceof IconButton button) button.setY(button.getY() + EXTENSION / 2);
        if(forceGrouped||menu.rules().isGrouped()||menu.rules().isDefault()) {
            if(groupEditor==null)groupEditor=new GroupEditor();
            groupEditor.init();return;
        }
        icon(110,61+EXTENSION,AllIcons.I_ADD,"groups",()->{forceGrouped=true;clearWidgets();init();});
        whitelistDis = icon(38,61+EXTENSION,AllIcons.I_WHITELIST_OR,"mode.whitelist",() -> mode(CreatureFilterRules.Mode.WHITELIST));
        whitelistCon = icon(56,61+EXTENSION,AllIcons.I_WHITELIST_AND,"mode.whitelist_all",() -> mode(CreatureFilterRules.Mode.WHITELIST_ALL));
        blacklist = icon(74,61+EXTENSION,AllIcons.I_WHITELIST_NOT,"mode.blacklist",() -> mode(CreatureFilterRules.Mode.BLACKLIST));
        add = icon(182,26,AllIcons.I_ADD,"add",() -> addCondition(false));
        addInverted = icon(200,26,AllIcons.I_ADD_INVERTED_ATTRIBUTE,"add_inverted",() -> addCondition(true));
        selector = addRenderableWidget(new SelectionScrollInput(leftPos+39,topPos+26,137,18));
        selector.calling(state -> { selection=state; showSelection(); refresh(); });
        search = addRenderableWidget(new EditBox(font,leftPos+43,topPos+31,125,10,
                Component.translatable("gui.laowu.creature_filter.search")));
        search.setBordered(false);search.setTextColor(0xF3EBDE);
        search.setMaxLength(CreatureFilterRules.MAX_ID_LENGTH);
        search.setHint(Component.translatable("gui.laowu.creature_filter.search"));
        search.setResponder(value -> {
            if(reflectingSelection)return;
            query=value;displayingSelection=false;selection=0;rebuild();
        });
        search.setValue(previousQuery);
        selection=previousSelection;
        rebuild();
        if(previousDisplay)showSelection();
    }
    private IconButton icon(int x,int y,AllIcons icon,String key,Runnable action) {
        var button=addRenderableWidget(new IconButton(leftPos+x,topPos+y,icon));
        button.setToolTip(Component.translatable("gui.laowu.creature_filter."+key));
        button.withCallback(action);return button;
    }
    private void mode(CreatureFilterRules.Mode mode) {
        if(groupEditor!=null){groupEditor.mode(mode);return;}
        commit(menu.rules().withConditions(mode,menu.rules().conditions()));
    }
    private void rebuild() {
        if(groupEditor!=null){groupEditor.rebuildTargets();return;}
        filtered.clear();
        String normalized=query.trim().toLowerCase(Locale.ROOT);
        for(var category:CreatureFilterRules.Category.values()) {
            var name=categoryName(category);
            if(matches(normalized,category.id(),name))filtered.add(new Entry(null,category,name));
        }
        for(var entry:menu.catalog())
            if(matches(normalized,entry.id().toString(),entry.name()))filtered.add(new Entry(entry.id(),null,entry.name()));
        var manual=ResourceLocation.tryParse(query.trim());
        if(normalized.contains(":")&&manual!=null&&!manual.toString().equals("minecraft:player")
                &&filtered.stream().noneMatch(entry->manual.equals(entry.id())))
            filtered.add(new Entry(manual,null,entityName(manual)));
        selection=Mth.clamp(selection,0,Math.max(0,filtered.size()-1));
        selector.forOptions(filtered.isEmpty()?List.of(Component.translatable("gui.laowu.creature_filter.no_results"))
                :filtered.stream().map(Entry::name).toList());
        selector.setState(selection);
        selector.titled(Component.translatable("gui.laowu.creature_filter.search"));
        selector.active=true;
        refresh();
    }
    private void showSelection() {
        if(filtered.isEmpty())return;
        // Showing a result must not re-run the text query and collapse the wheel's list.
        reflectingSelection=true;
        try {
            search.setValue(filtered.get(selection).name().getString());
            search.moveCursorToEnd();search.setHighlightPos(0);
        } finally { reflectingSelection=false; }
        displayingSelection=true;
        setFocused(search);
    }
    private static boolean matches(String query,String id,Component name) {
        return id.contains(query)||name.getString().toLowerCase(Locale.ROOT).contains(query);
    }
    private static Component entityName(ResourceLocation id) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(id).map(type->type.getDescription()).orElse(Component.literal(id.toString()));
    }
    private static Component categoryName(CreatureFilterRules.Category category) {
        return Component.translatable("gui.laowu.creature_filter.category_all",
                Component.translatable("gui.laowu.creature_filter.category."+category.id()));
    }
    private boolean contains(Entry entry) {
        return menu.rules().conditions().stream().anyMatch(condition->Objects.equals(entry.id(),condition.entityId())
                &&entry.category()==condition.category());
    }
    private void addCondition(boolean inverted) {
        if(groupEditor!=null){groupEditor.add(inverted);return;}
        if(filtered.isEmpty())return;
        var entry=filtered.get(selection);
        if(contains(entry)||menu.rules().conditions().size()>=CreatureFilterRules.MAX_CONDITIONS)return;
        var conditions=new ArrayList<>(menu.rules().conditions());
        conditions.add(entry.condition(inverted));
        commit(menu.rules().withConditions(menu.rules().mode(),conditions));
        showSelection();
    }
    private void commit(CreatureFilterRules rules) {
        if(menu.configure(rules)) {
            ModNetwork.setCreatureFilter(menu.containerId,rules);
            refresh();
        }
    }
    private void refresh() {
        if(groupEditor!=null){groupEditor.refresh();return;}
        if(add==null)return;
        var entry=filtered.isEmpty()?null:filtered.get(selection);
        add.active=addInverted.active=entry!=null&&!contains(entry)
                &&menu.rules().conditions().size()<CreatureFilterRules.MAX_CONDITIONS;
        whitelistDis.green=menu.rules().mode()==CreatureFilterRules.Mode.WHITELIST;
        whitelistCon.green=menu.rules().mode()==CreatureFilterRules.Mode.WHITELIST_ALL;
        blacklist.green=menu.rules().mode()==CreatureFilterRules.Mode.BLACKLIST;
        add.setToolTip(actionTip("add",entry));
        addInverted.setToolTip(actionTip("add_inverted",entry));
    }
    private Component actionTip(String key,Entry entry) {
        var text=Component.translatable("gui.laowu.creature_filter."+key);
        if(entry!=null) {
            text.append("\n").append(entry.name());
            if(entry.id()!=null)text.append("\n"+entry.id());
        }
        return text;
    }
    @Override protected void contentsCleared() { if(groupEditor!=null)groupEditor.clear();else refresh(); }
    @Override public boolean mouseClicked(double x,double y,int button) {
        if(button==0&&selector.visible&&selector.isMouseOver(x,y)) {
            setFocused(search);search.mouseClicked(x,y,button);return true;
        }
        boolean adding=button==0&&(add.active&&add.isMouseOver(x,y)||addInverted.active&&addInverted.isMouseOver(x,y));
        boolean handled=super.mouseClicked(x,y,button);
        // Parent dispatch focuses the clicked button after its callback returns.
        if(adding&&handled)setFocused(search);
        return handled;
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if(groupEditor!=null&&groupEditor.value!=null&&groupEditor.value.isFocused()&&key!=GLFW.GLFW_KEY_ESCAPE) {
            if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER)groupEditor.update("health");
            else groupEditor.value.keyPressed(key,scan,modifiers);
            return true;
        }
        if(search!=null&&search.isFocused()&&key!=GLFW.GLFW_KEY_ESCAPE) {
            if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER)addCondition(false);
            else if(key==GLFW.GLFW_KEY_DOWN||key==GLFW.GLFW_KEY_UP) {
                selector.setState(Mth.clamp(selection+(key==GLFW.GLFW_KEY_DOWN?1:-1),0,Math.max(0,filtered.size()-1)));
                selector.onChanged();
            } else search.keyPressed(key,scan,modifiers);
            return true;
        }
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean mouseScrolled(double x,double y,double delta) {
        if(selector.visible&&selector.isMouseOver(x,y)) { selector.mouseScrolled(x,y,delta);return true; }
        return super.mouseScrolled(x,y,delta);
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        renderBackground(graphics);super.render(graphics,mouseX,mouseY,partialTick);
        renderTooltip(graphics,mouseX,mouseY);
    }
    @Override protected void renderTooltip(GuiGraphics graphics,int mouseX,int mouseY) {
        if(mouseX>=leftPos+15&&mouseX<leftPos+33&&mouseY>=topPos+61+EXTENSION&&mouseY<topPos+79+EXTENSION) {
            if(menu.rules().isGrouped()) {
                var draft=menu.contentHolder.copy();menu.rules().write(draft);
                graphics.renderComponentTooltip(font,((cn.laowu.mod.item.CreatureFilterItem)menu.contentHolder.getItem())
                    .makeSummary(draft),mouseX,mouseY);return;
            }
            var lines=new ArrayList<Component>();
            lines.add(Component.translatable("gui.laowu.creature_filter.selected_conditions").withStyle(net.minecraft.ChatFormatting.YELLOW));
            for(var condition:menu.rules().conditions()) {
                Component name=condition.category()!=null?categoryName(condition.category()):entityName(condition.entityId());
                if(condition.inverted())name=Component.translatable("gui.laowu.creature_filter.inverted",name);
                lines.add(Component.literal("- ").append(name).withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            if(menu.rules().conditions().isEmpty())lines.add(Component.translatable("gui.laowu.creature_filter.none"));
            if(menu.rules().categoryMask()!=CreatureFilterRules.ALL_CATEGORIES)lines.add(
                    Component.translatable("gui.laowu.creature_filter.legacy_categories",menu.rules().categoryMask()).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            graphics.renderComponentTooltip(font,lines,mouseX,mouseY);
        } else if(groupEditor!=null&&groupEditor.value.isMouseOver(mouseX,mouseY)) {
            graphics.renderComponentTooltip(font,List.of(Component.translatable("gui.laowu.creature_filter.health_setting"),
                Component.translatable("gui.laowu.creature_filter.health_help")),mouseX,mouseY);
        } else super.renderTooltip(graphics,mouseX,mouseY);
    }
    @Override protected void renderBg(GuiGraphics graphics,float partialTick,int mouseX,int mouseY) {
        renderPlayerInventory(graphics,getLeftOfCentered(AllGuiTextures.PLAYER_INVENTORY.getWidth()),topPos+background.getHeight()+EXTENSION+4);
        graphics.blit(FILTERS,leftPos,topPos,0,99,241,54,256,256);
        for (int row=0;row<EXTENSION;row++)
            graphics.blit(FILTERS,leftPos,topPos+54+row,0,115+(row&1),241,1,256,256);
        graphics.blit(FILTERS,leftPos,topPos+FOOTER_Y-1,0,153,241,1,256,256);
        graphics.blit(FILTERS,leftPos,topPos+FOOTER_Y,0,154,241,30,256,256);
        graphics.blit(FILTERS,leftPos+14,topPos+25,4,124,10,20,256,256);
        graphics.blit(FILTERS,leftPos+24,topPos+25,4,124,10,20,256,256);
        if(groupEditor!=null) {
            // Use an uninterrupted native background strip, not a patch over the old item slot.
            for(int row=15;row<FOOTER_Y-1;row++)
                graphics.blit(FILTERS,leftPos+1,topPos+row,1,115+(row&1),230,1,256,256);
            well(graphics,14,25,146);
            well(graphics,14,47,43);well(graphics,62,47,43);well(graphics,110,47,43);well(graphics,158,47,60);
        }
        graphics.drawString(font,title,leftPos+(background.getWidth()-8)/2-font.width(title)/2,topPos+4,getTitleColor(),false);
        graphics.renderItem(summaryIcon,leftPos+16,topPos+62+EXTENSION);
        graphics.renderItemDecorations(font,summaryIcon,leftPos+16,topPos+62+EXTENSION,String.valueOf(menu.rules().conditionCount()));
        if(hasPreview())GuiGameElement.of(menu.contentHolder).scale(4).at(leftPos+background.getWidth()+8f,topPos+background.getHeight()-52f,-200f).render(graphics);
    }
    private void well(GuiGraphics g,int x,int y,int width) {
        g.blit(FILTERS,leftPos+x,topPos+y,38,124,4,20,256,256);
        // The native dropdown slice has an arrow notch; keep these rectangular wells closed.
        for(int row=7;row<=12;row++)g.blit(FILTERS,leftPos+x,topPos+y+row,38,130,2,1,256,256);
        for(int i=4;i<width-4;i++)g.blit(FILTERS,leftPos+x+i,topPos+y,45,124,1,20,256,256);
        g.blit(FILTERS,leftPos+x+width-4,topPos+y,173,124,4,20,256,256);
    }
    @Override protected boolean isButtonEnabled(IconButton button) { return true; }
    private boolean hasPreview() { return leftPos+background.getWidth()+72<=width; }
    @Override public List<Rect2i> getExtraAreas() {
        return hasPreview()?List.of(new Rect2i(leftPos+background.getWidth()+8,topPos+background.getHeight()-52,64,64)):List.of();
    }
}
