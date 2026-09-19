package cn.laowu.mod.test;

import cn.laowu.mod.*;
import cn.laowu.mod.api.*;
import cn.laowu.mod.genetics.*;
import cn.laowu.mod.item.CatPancakeItem;
import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class CatTraitScriptProbe {
    private static final String SWIFT="examplepack:swift_paws", HEAL="examplepack:healing_rhythm", ATTACK="examplepack:relentless";
    private static final UUID OWNER=UUID.fromString("4b2bb5a8-686b-44b0-8cd5-8814eec6b2d0");
    private static boolean scripts(GameTestHelper h) {
        if(Boolean.getBoolean("laowu.trait_examples")) return true;
        System.out.println("SKIP: trait script example fixture not enabled");h.succeed();return false;
    }
    private static Vec3 floor(GameTestHelper h) {
        if(h.getLevel().getServer() instanceof GameTestServer) {
            var first=h.absolutePos(BlockPos.ZERO);var last=h.absolutePos(new BlockPos(71,11,13));
            for(int x=Math.floorDiv(first.getX(),16);x<=Math.floorDiv(last.getX(),16);x++)
                for(int z=Math.floorDiv(first.getZ(),16);z<=Math.floorDiv(last.getZ(),16);z++) h.getLevel().setChunkForced(x,z,true);
        }
        for(int x=0;x<10;x++)for(int z=0;z<10;z++) h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
        return Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(4,1,4)));
    }
    private static Cat cat(GameTestHelper h) {
        var cat=EntityType.CAT.create(h.getLevel());cat.setTame(true,true);cat.setOwnerUUID(OWNER);
        cat.setNoAi(true);cat.setNoGravity(true);cat.setPos(floor(h));h.getLevel().addFreshEntity(cat);
        CatTraitData.set(cat,CatTraitProfile.EMPTY);
        var genes=CatAttributeData.ensure(cat);
        for(var stat:CatStat.values())genes=genes.withValues(stat,30,100);
        CatAttributeData.set(cat,genes);cat.setHealth(cat.getMaxHealth());return cat;
    }
    private static ServerPlayer player(GameTestHelper h,Cat cat) {
        var level=h.getLevel();
        var connection=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"trait-connection"));
        var player=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"trait-probe"),net.minecraft.server.level.ClientInformation.createDefault());
        player.connection=connection.connection;player.moveTo(cat.getX()+1,cat.getY(),cat.getZ(),0,0);return player;
    }
    private static Map<ResourceLocation,JsonElement> definitions() {
        var result=new LinkedHashMap<ResourceLocation,JsonElement>();
        var entries=CatTraitRegistry.networkData().getList("Definitions",Tag.TAG_COMPOUND);
        for(int i=0;i<entries.size();i++) {
            var value=entries.getCompound(i);
            result.put(ResourceLocation.tryParse(value.getString("Id")),JsonParser.parseString(value.getString("Json")));
        }
        return result;
    }
    private static void reload(Map<ResourceLocation,JsonElement> values) {
        try {
            var method=CatTraitRegistry.class.getDeclaredMethod("apply",Map.class,
                    net.minecraft.server.packs.resources.ResourceManager.class,net.minecraft.util.profiling.ProfilerFiller.class);
            method.setAccessible(true);method.invoke(new CatTraitRegistry(),values,null,null);
        } catch(ReflectiveOperationException error){throw new RuntimeException(error);}
    }
    private static void expectExpired(GameTestHelper h,Runnable action) {
        boolean rejected=false;try{action.run();}catch(IllegalStateException expected){rejected=true;}
        h.assertTrue(rejected,"Old trait handle cannot be reused");
    }
    private static void cleanup(GameTestHelper h,Cat cat) {
        for(var drop:h.getLevel().getEntitiesOfClass(ItemEntity.class,cat.getBoundingBox().inflate(4)))drop.discard();
        cat.discard();
    }
    @GameTest(template="artillery_probe",batch="trait_core",timeoutTicks=30)
    public static void optionalCoreAndEditor(GameTestHelper h) {
        boolean scripted=Boolean.getBoolean("laowu.trait_examples");
        var backup=definitions();String id=scripted?SWIFT:"testpack:optional";
        if(!scripted) {
            var fixture=new LinkedHashMap<>(backup);
            fixture.put(ResourceLocation.tryParse(id),JsonParser.parseString("{\"schema_version\":1,\"title\":\"Optional\","
                    + "\"rarity\":\"good\",\"max_level\":3,\"stat_bonuses\":{\"speed\":[5,10,15]}}"));
            reload(fixture);
        }
        var cat=cat(h);var player=player(h,cat);
        try {
            int before=CatAttributeEffects.effectiveValue(cat,CatStat.SPEED);
            h.assertTrue(CatTraitApi.supportsApi(1)&&CatTraitApi.schemaVersion()==1&&CatTraitApi.isRegistered(id),"Real definition loaded");
            h.assertTrue(CatTraitApi.setLevel(cat,id,3),"Custom trait can be acquired");
            h.assertTrue(CatAttributeEffects.effectiveValue(cat,CatStat.SPEED)==before+15,"Static levelled bonus applied");
            var handle=CatTraitApi.trait(cat,id);handle.setStatBonus("speed",7);
            h.assertTrue(CatAttributeEffects.effectiveValue(cat,CatStat.SPEED)==before+22,"Script effective bonus stacks once");
            h.assertTrue(CatAttributeData.ensure(cat).current(CatStat.SPEED)==30,"Bonuses never modify saved genes");
            h.assertTrue(CatTraitData.serialized(cat).getCompound("ScriptBonuses").getInt("speed")==7,"Client bonus aggregate sent");
            handle.setNumber("probe:counter",12);handle.setText("probe:text","saved");
            h.assertTrue(handle.tryActivate("probe:cooldown",80)&&!handle.tryActivate("probe:cooldown",80),"Atomic cooldown reservation");
            var menu=new CatTraitEditorMenu(71,player.getInventory(),cat);player.containerMenu=menu;
            var buffer=new FriendlyByteBuf(Unpooled.buffer());
            try {
                CatTraitEditorMenu.writeOpeningData(buffer,cat);
                var clientMenu=new CatTraitEditorMenu(71,player.getInventory(),buffer);
                h.assertTrue(!buffer.isReadable()&&clientMenu.catalog().stream().map(t->t.id().toString()).toList()
                        .equals(menu.catalog().stream().map(t->t.id().toString()).toList()),"ID roster roundtrip, no enum ordinal assumption");
                clientMenu.removed(player);
            }finally{buffer.release();}
            var type=CatTraitRegistry.find(id,false);
            h.assertTrue(menu.clickMenuButton(player,menu.action(type,false,false))&&CatTraitApi.level(cat,id)==2,"Editor decrements custom trait");
            expectExpired(h,()->handle.getLevel());
            h.assertTrue(menu.clickMenuButton(player,menu.action(type,true,true))&&CatTraitApi.level(cat,id)==3,"Editor custom maximum");
            menu.removed(player);
            var pancake=CatPancakeItem.capture(cat);
            var filter=new ItemStack(LaoWuMod.CAT_FILTER.get());
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,filter);
            var filterMenu=new CatFilterMenu(72,player.getInventory(),filter);
            int selection=0;
            for(int i=0;i<filterMenu.traitCatalog().size();i++)
                if(filterMenu.traitCatalog().get(i).id().toString().equals(id))selection=i+1;
            h.assertTrue(selection>0&&filterMenu.clickMenuButton(player,CatFilterMenu.addTraitButton(selection)),"Filter selects custom trait");
            h.assertTrue(filterMenu.rules().requiredTraits().get(0).id().toString().equals(id),"Filter preserves custom ID");
            filterMenu.rules().write(filter);
            h.assertTrue(cn.laowu.mod.item.CatFilterRules.read(filter).requiredTraits().get(0).id().toString().equals(id),"Filter ID survives item storage");
            filterMenu.removed(player);
            h.assertTrue(CatTraitApi.pancakeLevel(pancake,id)==3,"Captured pancake trait");
            h.assertTrue(CatTraitApi.setPancakeLevel(pancake,id,2)&&CatTraitApi.pancakeLevel(pancake,id)==2,"Server pancake API");
            var current=CatTraitApi.trait(cat,id);
            CatTraitApi.remove(cat,id);
            h.assertTrue(CatAttributeEffects.effectiveValue(cat,CatStat.SPEED)==before,"Removal clears all bonuses");
            h.assertTrue(!cat.getPersistentData().getCompound(CatTraitScriptState.TAG).contains(id),"Removal prunes only owned state");
            expectExpired(h,()->current.getLevel());
            h.succeed();System.out.println("PASS: trait data/API/editor/bonus/cooldown/pancake, KubeJS examples="+scripted);
        }finally{if(!scripted)reload(backup);player.discard();cleanup(h,cat);}
    }
    @GameTest(template="artillery_probe",batch="trait_events",timeoutTicks=80)
    public static void realCombatEvents(GameTestHelper h) {
        if(!scripts(h))return;
        var cat=cat(h);cat.addTag("trait_probe");cat.addTag("trait_probe_bonus");
        CatTraitApi.setLevel(cat,ATTACK,2);
        var enemy=EntityType.HUSK.create(h.getLevel());enemy.setNoAi(true);enemy.setNoGravity(true);
        enemy.setPos(cat.position().add(2,0,0));enemy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);enemy.setHealth(1000);
        // Husks have two native armor points; remove them to test exact scripted raw damage.
        enemy.getAttribute(Attributes.ARMOR).setBaseValue(0);
        h.getLevel().addFreshEntity(enemy);enemy.setTarget(cat);
        h.runAfterDelay(25,()->{
            var trait=CatTraitApi.trait(cat,ATTACK);
            h.assertTrue(trait.number("probe:added")==1&&trait.text("probe:reason").equals("load"),"Added load event once");
            h.assertTrue(trait.number("probe:tick")>=1&&trait.number("probe:fast_tick")>=2,"Real staggered tick callbacks");
            float hp=cat.getHealth();
            enemy.hurt(cat.damageSources().mobAttack(cat),2);
            h.assertTrue(Math.abs(enemy.getHealth()-996)<.01,"Example script adds two attack damage; health="+enemy.getHealth());
            cat.hurt(cat.damageSources().generic(),10);
            h.assertTrue(Math.abs(cat.getHealth()-hp+1)<.01,"Real beforeHurt reduces accepted damage");
            cat.setHealth(cat.getMaxHealth()-5);cat.heal(1);
            h.assertTrue(Math.abs(cat.getHealth()-(cat.getMaxHealth()-3.5))<.01,"Real beforeHeal modifies amount");
            h.runAfterDelay(2,()->{
                var active=CatTraitApi.trait(cat,ATTACK);
                h.assertTrue(active.number("probe:before_attack")==1&&active.number("probe:after_attack")==1
                        &&active.number("examplepack:hits")==1,"Attack events fire once, no nested recursion");
                h.assertTrue(active.number("probe:bonus_accepted")==1&&enemy.getHealth()<995,"Non-recursive helper accepts real extra hit");
                h.assertTrue(active.number("probe:after_hurt")==1&&active.number("probe:before_heal")==1,"Hurt/heal observer counts");
                cat.addTag("trait_probe_cancel");cat.invulnerableTime=0;float old=cat.getHealth();
                cat.hurt(cat.damageSources().generic(),10);
                h.assertTrue(cat.getHealth()==old,"beforeHurt cancellation prevents damage");
                cat.removeTag("trait_probe_cancel");enemy.setHealth(1);enemy.invulnerableTime=0;
                enemy.hurt(cat.damageSources().mobAttack(cat),1);
                h.runAfterDelay(2,()->{
                    h.assertTrue(CatTraitApi.trait(cat,ATTACK).number("probe:kill")==1,"Real kill observer");
                    cat.setHealth(.5F);cat.invulnerableTime=0;cat.hurt(cat.damageSources().generic(),1);
                    h.runAfterDelay(2,()->{
                        h.assertTrue(!cat.isAlive()&&cat.getTags().contains("trait_probe_dead"),"Real death observer after confirmed death");
                        h.assertTrue(!cat.getTags().contains("trait_probe_dead_after_hurt"),"Fatal hurt is handled by death, not a writable afterHurt callback on a dead cat");
                        cleanup(h,cat);enemy.discard();h.succeed();
                        System.out.println("PASS: real KubeJS damage, heal, cancellation, kill, death and no recursive events");
                    });
                });
            });
        });
    }
    @GameTest(template="artillery_probe",batch="trait_heal",timeoutTicks=140)
    public static void exampleHealingCooldown(GameTestHelper h) {
        if(!scripts(h))return;
        var cat=cat(h);CatTraitApi.setLevel(cat,HEAL,2);cat.setHealth(cat.getMaxHealth()-10);float health=cat.getHealth();
        h.runAfterDelay(35,()->{
            var trait=CatTraitApi.trait(cat,HEAL);
            h.assertTrue(trait.number("examplepack:heals")==1&&Math.abs(cat.getHealth()-health-2)<.01,"Tick script heals two health once");
            h.runAfterDelay(25,()->{
                h.assertTrue(CatTraitApi.trait(cat,HEAL).number("examplepack:heals")==1,"Heal cannot repeat during cooldown");
                h.runAfterDelay(45,()->{
                    h.assertTrue(CatTraitApi.trait(cat,HEAL).number("examplepack:heals")==2,"Heal resumes after 80-tick cooldown");
                    CatTraitApi.remove(cat,HEAL);float healed=cat.getHealth();
                    h.runAfterDelay(25,()->{
                        h.assertTrue(cat.getHealth()==healed,"Removing trait stops healing");
                        cleanup(h,cat);h.succeed();System.out.println("PASS: shipped healing example, actual health, cooldown and removal");
                    });
                });
            });
        });
    }
    @GameTest(template="artillery_probe",batch="trait_persistence",timeoutTicks=50)
    public static void captureRestoreAndStaleHandles(GameTestHelper h) {
        if(!scripts(h))return;
        var cat=cat(h);CatTraitApi.setLevel(cat,SWIFT,3);
        var handle=CatTraitApi.trait(cat,SWIFT);handle.setNumber("probe:value",19);handle.setText("probe:text","hello");
        handle.startCooldown("probe:cooldown",100);handle.setStatBonus("attack",8);
        var pos=cat.position();var stack=CatPancakeItem.capture(cat);cat.discard();
        var item=new ItemEntity(h.getLevel(),pos.x,pos.y,pos.z,stack);h.getLevel().addFreshEntity(item);
        try {
            var restore=CatPancakeItem.class.getDeclaredMethod("restoreCat",ServerLevel.class,ItemEntity.class,ItemStack.class);
            restore.setAccessible(true);restore.invoke(null,h.getLevel(),item,stack);
        }catch(ReflectiveOperationException error){throw new RuntimeException(error);}
        h.runAfterDelay(2,()->{
            var cats=h.getLevel().getEntitiesOfClass(Cat.class,new AABB(pos,pos).inflate(3),Cat::isAlive);
            h.assertTrue(cats.size()==1&&item.isRemoved(),"Production restore creates exactly one cat");
            var restored=cats.get(0);var active=CatTraitApi.trait(restored,SWIFT);
            h.assertTrue(OWNER.equals(restored.getOwnerUUID())&&active.getLevel()==3,"Owner and custom level retained");
            h.assertTrue(active.number("probe:value")==19&&active.text("probe:text").equals("hello"),"Namespaced state retained");
            h.assertTrue(active.cooldownRemaining("probe:cooldown")>90&&active.cooldownRemaining("probe:cooldown")<100,"Cooldown does not reset on capture");
            h.assertTrue(CatTraitScriptState.bonus(restored,CatStat.ATTACK)==8,"Persistent effective bonus restored");
            h.runAfterDelay(1,()->{
                expectExpired(h,()->active.getLevel());cleanup(h,restored);h.succeed();
                System.out.println("PASS: real pancake restore preserves trait level/state/cooldown; handles expire next tick");
            });
        });
    }
    @GameTest(template="artillery_probe",batch="trait_breed",timeoutTicks=20)
    public static void realScriptBreeding(GameTestHelper h) {
        if(!scripts(h))return;
        var attack=CatTraitRegistry.find(ATTACK,false);var swift=CatTraitRegistry.find(SWIFT,false);
        var parent=CatTraitProfile.EMPTY.withLevel(attack,3).withLevel(swift,3);
        var child=CatTraitProfile.breed(parent,parent,0F,h.getLevel().random);
        h.assertTrue(child.level(attack)==2&&child.level(swift)==1,"Actual JS breed hook can amend child; other inheritance unchanged");
        h.assertTrue(parent.level(attack)==3&&parent.level(swift)==3,"Parents never mutated");
        var pancake=new ItemStack(LaoWuMod.CAT_PANCAKE.get());CatTraitData.set(pancake,child);
        h.assertTrue(CatTraitApi.pancakeLevel(pancake,ATTACK)==2,"Scripted child survives pancake data");
        h.succeed();System.out.println("PASS: actual KubeJS breed callback, child edits and parent/pancake preservation");
    }
    @GameTest(template="artillery_probe",batch="trait_changes",timeoutTicks=75)
    public static void changeEventsAndMissingDefinitions(GameTestHelper h) {
        if(!scripts(h))return;
        var cat=cat(h);cat.addTag("trait_probe");CatTraitApi.setLevel(cat,ATTACK,1);CatTraitApi.setLevel(cat,SWIFT,3);
        h.runAfterDelay(15,()->{
            CatTraitApi.setLevel(cat,ATTACK,2);
            h.runAfterDelay(15,()->{
                h.assertTrue(CatTraitApi.trait(cat,ATTACK).number("probe:level_changed")==1,"Level change event");
                CatTraitApi.remove(cat,SWIFT);
                h.runAfterDelay(15,()->{
                    h.assertTrue(cat.getTags().contains("trait_probe_removed"),"Removal event");
                    var state=CatTraitApi.trait(cat,ATTACK);state.setNumber("probe:kept",71);state.setStatBonus("luck",12);
                    var backup=definitions();var saved=CatTraitData.ensure(cat).save();
                    try {
                        reload(Map.of());
                        h.assertTrue(CatTraitApi.level(cat,ATTACK)==0&&CatTraitApi.storedLevel(cat,ATTACK)==2,"Missing definition suspends but retains ownership");
                        h.assertTrue(CatTraitApi.trait(cat,ATTACK)==null&&CatTraitScriptState.bonus(cat,CatStat.LUCK)==0,"Missing script gives no bonus");
                        CatTraitData.set(cat,CatTraitProfile.load(saved).orElseThrow());
                        h.assertTrue(cat.getPersistentData().getCompound(CatTraitScriptState.TAG).contains(ATTACK),"Missing trait state is not pruned");
                    }finally{reload(backup);}
                    expectExpired(h,()->state.getLevel());
                    h.assertTrue(CatTraitApi.trait(cat,ATTACK).number("probe:kept")==71&&CatTraitScriptState.bonus(cat,CatStat.LUCK)==12,
                            "Restoring definition reactivates existing state");
                    cleanup(h,cat);h.succeed();System.out.println("PASS: change events, definition removal/reinstall and state recovery");
                });
            });
        });
    }
    @GameTest(template="artillery_probe",batch="zz_trait_reload",timeoutTicks=400)
    public static void realServerReload(GameTestHelper h) {
        if(!scripts(h))return;
        var cat=cat(h);CatTraitApi.setLevel(cat,SWIFT,3);
        var state=CatTraitApi.trait(cat,SWIFT);state.setNumber("probe:reload",42);
        var player=player(h,cat);var menu=new CatTraitEditorMenu(79,player.getInventory(),cat);player.containerMenu=menu;
        long revision=CatTraitRegistry.revision(false);
        var server=h.getLevel().getServer();
        // NEVER join an incomplete future on the server thread; poll with the game-test scheduler.
        var future=server.reloadResources(server.getPackRepository().getSelectedIds());
        h.succeedWhen(()->{
            h.assertTrue(future.isDone(),"Waiting for asynchronous /reload");
            future.join();
            h.assertTrue(CatTraitRegistry.revision(false)>revision,"Production reload listener executed");
            h.assertTrue(CatTraitApi.level(cat,SWIFT)==3&&CatTraitApi.trait(cat,SWIFT).number("probe:reload")==42,"Reload retains level and state");
            h.assertTrue(!menu.stillValid(player),"Old editor roster invalidated by reload");
            expectExpired(h,()->state.getLevel());
            menu.removed(player);player.discard();cleanup(h,cat);
            System.out.println("PASS: real server resource reload completes without blocking, persistent traits and stale-menu guard");
        });
    }
}
