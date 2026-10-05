package cn.laowu.mod.client;
import cn.laowu.mod.*;
import cn.laowu.mod.item.CreatureFilterRules;
import com.simibubi.create.foundation.gui.widget.IconButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
/** Real integrated-server packet round trip across multiple screen ticks. */
final class CreatureFilterNetworkClientProbe {
    private static int stage, ticks, total;
    private static CreatureFilterScreen screen;
    private static java.util.concurrent.CompletableFuture<?> pending;
    static boolean tick(Minecraft mc) throws Exception {
        if(++total>600)throw new AssertionError("Filter network lifecycle timeout at "+stage);
        if(pending!=null) {
            if(!pending.isDone())return false;
            pending.join();pending=null;
        }
        ticks++;
        switch(stage) {
            case 0 -> {
                pending=mc.getSingleplayerServer().submit(()->{
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                    // The shared test world may leave the player underwater between runs.
                    p.setAirSupply(6000);
                    p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LaoWuMod.CREATURE_FILTER.get()));
                    CreatureFilterRules.of(5,CreatureFilterRules.Mode.WHITELIST,java.util.List.of()).write(p.getMainHandItem());
                    p.inventoryMenu.broadcastChanges();
                    LaoWuMod.CREATURE_FILTER.get().use(p.level(),p,InteractionHand.MAIN_HAND);
                });
                stage=1;ticks=0;
            }
            case 1 -> {
                if(!(mc.screen instanceof CreatureFilterScreen current))return false;
                screen=current;
                field("search",EditBox.class).setValue("minecraft:zombie");
                click("add");stage=2;ticks=0;
            }
            case 2 -> {
                check(mc.screen==screen,"Positive add closed the live screen");
                if(ticks<40)return false;
                pending=checkDraft(mc,1,CreatureFilterRules.Mode.WHITELIST);
                field("search",EditBox.class).setValue("minecraft:cow");
                click("addInverted");click("whitelistCon");stage=3;ticks=0;
            }
            case 3 -> {
                check(mc.screen==screen,"Inverted add or mode change closed the live screen");
                if(ticks<40)return false;
                pending=checkDraft(mc,2,CreatureFilterRules.Mode.WHITELIST_ALL);stage=4;ticks=0;
            }
            case 4 -> {mc.player.closeContainer();stage=5;ticks=0;}
            case 5 -> {
                if(ticks<40)return false;
                pending=mc.getSingleplayerServer().submit(()->{
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                    var rules=CreatureFilterRules.read(p.getMainHandItem());
                    check(rules.conditions().size()==2&&rules.conditions().get(1).inverted()
                        &&rules.mode()==CreatureFilterRules.Mode.WHITELIST_ALL,"Close did not persist real packet draft");
                    LaoWuMod.CREATURE_FILTER.get().use(p.level(),p,InteractionHand.MAIN_HAND);
                });
                stage=6;ticks=0;
            }
            case 6 -> {
                if(!(mc.screen instanceof CreatureFilterScreen current))return false;
                var menu=current.getMenu();
                check(menu.rules().conditions().size()==2&&menu.rules().conditions().get(1).inverted()
                    &&menu.rules().mode()==CreatureFilterRules.Mode.WHITELIST_ALL,"Reopen lost saved conditions");
                screen=current;
                var opener=screen.children().stream().filter(IconButton.class::isInstance).map(IconButton.class::cast)
                    .filter(b->b.getToolTip().stream().anyMatch(c->c.getString().equals(net.minecraft.network.chat.Component.translatable("gui.laowu.creature_filter.groups").getString())))
                    .findFirst().orElseThrow();
                screen.mouseClicked(opener.getX()+8,opener.getY()+8,0);screen.mouseReleased(opener.getX()+8,opener.getY()+8,0);
                target("minecraft:cat");groupField("value",EditBox.class).setValue("<4");
                target("passive");select("adult",1);click("blacklist");stage=7;ticks=0;
            }
            case 7 -> {
                check(mc.screen==screen,"Grouped editing closed live screen");if(ticks<40)return false;
                pending=mc.getSingleplayerServer().submit(()->{
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                    var r=((CreatureFilterMenu)p.containerMenu).rules();
                    check(r.groups().size()==2&&r.retainedLegacy()!=null&&r.conditionCount()==4
                        &&r.groups().get(1).category()==CreatureFilterRules.Category.PASSIVE&&r.mode()==CreatureFilterRules.Mode.BLACKLIST,
                        "Real packets lost category, legacy, numeric attributes or global mode");
                    check(!CreatureFilterRules.read(p.getMainHandItem()).isGrouped(),"Grouped draft saved before close");
                });stage=8;ticks=0;
            }
            case 8 -> {mc.player.closeContainer();stage=9;ticks=0;}
            case 9 -> {
                if(ticks<40)return false;
                pending=mc.getSingleplayerServer().submit(()->{
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                    var r=CreatureFilterRules.read(p.getMainHandItem());
                    check(r.groups().size()==2&&r.retainedLegacy()!=null&&r.groups().get(0).conditions().get(0).value()==4,"Grouped close did not save numeric rules");
                    LaoWuMod.CREATURE_FILTER.get().use(p.level(),p,InteractionHand.MAIN_HAND);
                });stage=10;ticks=0;
            }
            case 10 -> {
                if(!(mc.screen instanceof CreatureFilterScreen current))return false;
                var r=current.getMenu().rules();
                check(r.groups().size()==2&&r.retainedLegacy()!=null&&r.conditionCount()==4,"Grouped reopen lost saved OR groups");
                mc.player.closeContainer();
                System.out.println("PASS: CREATURE FILTER LIVE NETWORK - legacy positive/inverted/AND, grouped numeric two-group OR, retained legacy branch, draft/save/reopen");
                return true;
            }
        }
        return false;
    }
    private static java.util.concurrent.CompletableFuture<?> checkDraft(Minecraft mc,int count,CreatureFilterRules.Mode mode) {
        return mc.getSingleplayerServer().submit(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            check(p.containerMenu instanceof CreatureFilterMenu,"Server menu closed");
            var menu=(CreatureFilterMenu)p.containerMenu;
            check(menu.rules().conditions().size()==count&&menu.rules().mode()==mode,"Actual config packet not applied: count="
                +menu.rules().conditions().size()+" mode="+menu.rules().mode()+" expected="+count+"/"+mode);
            check(CreatureFilterRules.read(p.getMainHandItem()).conditions().isEmpty(),"Draft modified held item before closing");
        });
    }
    private static <T>T field(String name,Class<T> type)throws Exception {
        var f=CreatureFilterScreen.class.getDeclaredField(name);f.setAccessible(true);return type.cast(f.get(screen));
    }
    private static void click(String name)throws Exception {
        var b=field(name,IconButton.class);check(b.active,"Inactive "+name);
        double x=b.getX()+8,y=b.getY()+8;
        check(screen.mouseClicked(x,y,0),"Click unhandled "+name);screen.mouseReleased(x,y,0);
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void target(String query)throws Exception {
        var search=field("search",EditBox.class);search.setValue(query);
        screen.mouseClicked(search.getX()+2,search.getY()+2,0);screen.mouseReleased(search.getX()+2,search.getY()+2,0);
        screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER,0,0);
    }
    private static <T>T groupField(String name,Class<T> type)throws Exception {
        Object group=field("groupEditor",Object.class);var f=group.getClass().getDeclaredField(name);
        f.setAccessible(true);return type.cast(f.get(group));
    }
    private static void groupClick(String name)throws Exception {
        var b=groupField(name,IconButton.class);check(b.active,"Inactive group "+name);
        check(screen.mouseClicked(b.getX()+8,b.getY()+8,0),"Group click unhandled");screen.mouseReleased(b.getX()+8,b.getY()+8,0);
    }
    private static void select(String name,int index)throws Exception {
        var input=groupField(name,com.simibubi.create.foundation.gui.widget.SelectionScrollInput.class);
        input.setState(index);input.onChanged();
    }
}
