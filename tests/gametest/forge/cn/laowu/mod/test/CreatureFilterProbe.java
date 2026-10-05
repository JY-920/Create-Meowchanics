package cn.laowu.mod.test;

import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.*;
import java.lang.reflect.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class CreatureFilterProbe {

    @GameTest(template="artillery_probe",batch="creature_filter",timeoutTicks=30)
    public static void entryModesApplyAcrossSpeciesAndPreserveAttributes(GameTestHelper h) {
        Mob cow=mob(h,EntityType.COW),zombie=mob(h,EntityType.ZOMBIE),creeper=mob(h,EntityType.CREEPER);
        var hostile=group("*","whitelist_all");hostile.putString("Category","hostile");hostile.putBoolean("Inverted",false);
        var excluded=group("minecraft:creeper","whitelist_all");excluded.putBoolean("Inverted",true);
        var entries=new ListTag();entries.add(hostile);entries.add(excluded);
        var data=new CompoundTag();data.putInt("Version",5);data.putString("Mode","whitelist_all");data.put("Groups",entries);
        Object rules=call(null,type(h,RULES),"readData",new Class<?>[]{CompoundTag.class},data);
        h.assertTrue(matches(h,rules,zombie),"Global ALL admits hostile non-creeper");
        h.assertTrue(!matches(h,rules,creeper)&&!matches(h,rules,cow),"Global ALL excludes creeper and passive mobs");
        data.putString("Mode","whitelist");
        rules=call(null,type(h,RULES),"readData",new Class<?>[]{CompoundTag.class},data);
        h.assertTrue(matches(h,rules,cow)&&matches(h,rules,creeper),"Global ANY combines whole entity entries");
        data.putString("Mode","blacklist");
        rules=call(null,type(h,RULES),"readData",new Class<?>[]{CompoundTag.class},data);
        h.assertTrue(!matches(h,rules,cow)&&!matches(h,rules,zombie),"Global NONE negates the combined entries");
        var saved=(CompoundTag)call(rules,type(h,RULES),"save",new Class<?>[0]);
        h.assertTrue(saved.getString("Mode").equals("blacklist")&&saved.getList("Groups",10).getCompound(1).getBoolean("Inverted"),
            "Global mode and per-entry inversion survive saving");
        cow.discard();zombie.discard();creeper.discard();
        h.succeed();
    }

    @GameTest(template="artillery_probe",batch="creature_filter",timeoutTicks=30)
    public static void categoryRulesCombineWithAttributesAndSurviveSave(GameTestHelper h) {
        Mob cow=mob(h,EntityType.COW),wolf=mob(h,EntityType.WOLF),zombie=mob(h,EntityType.ZOMBIE);
        for(String category:List.of("passive","neutral","hostile")) {
            var entry=group("*","whitelist_all",attribute("health","lt",5,false));
            entry.putString("Category",category);
            var data=new CompoundTag();data.putInt("Version",4);
            var entries=new ListTag();entries.add(entry);data.put("Groups",entries);
            Object rules=call(null,type(h,RULES),"readData",new Class<?>[]{CompoundTag.class},data);
            for(Mob candidate:List.of(cow,wolf,zombie)) {
                candidate.setHealth(2);
                h.assertTrue(matches(h,rules,candidate)==category(h,candidate).equalsIgnoreCase(category),
                    "Category rule selects "+category+" without a second species binding");
                candidate.setHealth(6);
                h.assertTrue(!matches(h,rules,candidate),"Category and health condition are conjunctive");
            }
            var saved=(CompoundTag)call(rules,type(h,RULES),"save",new Class<?>[0]);
            h.assertTrue(saved.getList("Groups",Tag.TAG_COMPOUND).getCompound(0).getString("Category").equals(category),
                "Category persists instead of broadening to all mobs");
            entry.putString("Target","minecraft:cow");
            Object invalid=call(null,type(h,RULES),"readData",new Class<?>[]{CompoundTag.class},data);
            cow.setHealth(2);
            h.assertTrue(!matches(h,invalid,cow),"Ambiguous category and species fails closed");
        }
        h.succeed();
    }

    @GameTest(template="artillery_probe",batch="creature_filter",timeoutTicks=30)
    public static void groupedBoundsValuesAndPacketPreserveLegacy(GameTestHelper h) {
        Mob cow=mob(h,EntityType.COW),zombie=mob(h,EntityType.ZOMBIE);
        for(var c:List.of(attribute("health","lt",Double.NaN,false),attribute("health","gt",-1,false),
                attribute("health_percent","gt",101,true),attribute("removed_attribute","eq",1,true)))
            h.assertTrue(!matches(h,grouped(h,group("*","blacklist",c)),cow),"Invalid attribute fails closed");
        var nine=new CompoundTag[9];Arrays.fill(nine,group("*","whitelist"));
        h.assertTrue(!matches(h,grouped(h,nine),cow),"Nine groups rejected");
        var many=new CompoundTag[65];Arrays.fill(many,attribute("adult","eq",1,false));
        h.assertTrue(!matches(h,grouped(h,group("*","whitelist",many)),cow),"65 conditions rejected before dedup");
        var legacyConditions=new CompoundTag[64];
        for(int i=0;i<64;i++)legacyConditions[i]=condition("EntityId","test:entity_"+i,false);
        Object oversized=grouped(h,group("*","whitelist",attribute("health","ge",0,false)));
        var oversizedData=(CompoundTag)call(oversized,type(h,RULES),"save",new Class<?>[0]);
        oversizedData.put("Legacy",(CompoundTag)call(v2(h,"whitelist",legacyConditions),type(h,RULES),"save",new Class<?>[0]));
        h.assertTrue(!matches(h,call(null,type(h,RULES),"readData",new Class<?>[]{CompoundTag.class},oversizedData),cow),
                "64-condition budget includes retained legacy branch");
        Object config=grouped(h,group("minecraft:zombie","whitelist_all",attribute("health_percent","ge",50,false)));
        var data=(CompoundTag)call(config,type(h,RULES),"save",new Class<?>[0]);
        data.put("Legacy",(CompoundTag)call(rules(h,1,true),type(h,RULES),"save",new Class<?>[0]));
        config=call(null,type(h,RULES),"readData",new Class<?>[]{CompoundTag.class},data);
        h.assertTrue(matches(h,config,cow)&&matches(h,config,zombie),"Retained legacy branch and new group OR without reinterpreting legacy gates");
        try {
            var packetType=type(h,"cn.laowu.mod.network.SetCreatureFilterPacket");
            Object packet=packetType.getConstructor(int.class,type(h,RULES)).newInstance(12,config);
            var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                packetType.getMethod("encode",packetType,net.minecraft.network.FriendlyByteBuf.class).invoke(null,packet,buffer);packet=packetType.getMethod("decode",net.minecraft.network.FriendlyByteBuf.class).invoke(null,buffer);
                h.assertTrue(!buffer.isReadable(),"V3 bounded packet consumes entire config");
                Object restored=call(packet,packetType,"rules",new Class<?>[0]);
                h.assertTrue(matches(h,restored,cow)&&matches(h,restored,zombie),"V3 packet preserves legacy and percentage");
            } finally {buffer.release();}
        } catch(ReflectiveOperationException e){throw new AssertionError(e);}
        var stack=new ItemStack(cn.laowu.mod.LaoWuMod.CREATURE_FILTER.get());
        call(config,type(h,RULES),"write",new Class<?>[]{ItemStack.class},stack);
        var summary=((cn.laowu.mod.item.CreatureFilterItem)stack.getItem()).makeSummary(stack);
        h.assertTrue(summary.stream().anyMatch(c->c.getString().contains("health_percent")||c.getString().contains("Health")||c.getString().contains("生命")),
                "Item tooltip summarizes grouped numeric attribute, not empty legacy list");
        h.succeed();
    }


    private static CompoundTag attribute(String id, String comparison, double value, boolean inverted) {
        var t=new CompoundTag();t.putString("Attribute",id);t.putString("Comparison",comparison);
        t.putDouble("Value",value);t.putBoolean("Inverted",inverted);return t;
    }
    private static CompoundTag group(String target,String mode,CompoundTag... conditions) {
        var t=new CompoundTag();t.putString("Target",target);t.putString("Mode",mode);
        var l=new ListTag();for(var c:conditions)l.add(c);t.put("Conditions",l);return t;
    }
    private static Object grouped(GameTestHelper h,CompoundTag... groups) {
        var t=new CompoundTag();t.putInt("Version",3);
        var l=new ListTag();for(var g:groups)l.add(g);t.put("Groups",l);
        return call(null,type(h,RULES),"readData",new Class<?>[]{CompoundTag.class},t);
    }
    @GameTest(template="artillery_probe",batch="creature_filter",timeoutTicks=30)
    public static void groupedAttributesAreSpeciesGatedAndFailClosed(GameTestHelper h) {
        var cat=(net.minecraft.world.entity.animal.Cat)mob(h,EntityType.CAT);
        cat.setTame(true);cat.setHealth(2);
        var cow=(net.minecraft.world.entity.animal.Cow)mob(h,EntityType.COW);
        cow.setBaby(false);
        var r=grouped(h,group("minecraft:cat","whitelist_all",attribute("tamed","eq",1,false),
                attribute("health","lt",4,false)),group("minecraft:cow","whitelist_all",attribute("adult","eq",1,false)));
        h.assertTrue(matches(h,r,cat),"Tamed low-health cat matches its group");
        h.assertTrue(matches(h,r,cow),"Adult cow matches alternative group");
        cat.setTame(false);h.assertTrue(!matches(h,r,cat),"Cow group cannot admit untamed cat");
        cow.setBaby(true);h.assertTrue(!matches(h,r,cow),"Adult condition rejects baby cow");
        var zombie=(net.minecraft.world.entity.monster.Zombie)mob(h,EntityType.ZOMBIE);
        var babyZombie=grouped(h,group("minecraft:zombie","whitelist",attribute("baby","eq",1,false)));
        var adultZombie=grouped(h,group("minecraft:zombie","whitelist",attribute("adult","eq",1,false)));
        zombie.setBaby(true);
        h.assertTrue(matches(h,babyZombie,zombie)&&!matches(h,adultZombie,zombie),
                "Baby zombie must satisfy BABY but not ADULT");
        zombie.setBaby(false);
        h.assertTrue(!matches(h,babyZombie,zombie)&&matches(h,adultZombie,zombie),
                "Adult zombie must satisfy ADULT but not BABY");
        h.assertTrue(!matches(h,grouped(h,group("*","whitelist",attribute("tamed","eq",1,true))),cow),
                "Unsupported inverted tame attribute never matches a cow");
        cow.setHealth(2);
        h.assertTrue(matches(h,grouped(h,group("*","whitelist",
                attribute("health","lt",5,false),attribute("tamed","eq",1,false))),cow),
                "ANY accepts a supported low-health branch even when tame is unsupported");
        h.assertTrue(!matches(h,grouped(h,group("*","whitelist",attribute("tamed","eq",1,true))),cow),
                "ANY unsupported inverted tame alone still fails closed");
        h.assertTrue(!matches(h,grouped(h,group("*","blacklist",attribute("tamed","eq",1,false))),cow),
                "NONE unsupported tame alone still fails closed");
        h.assertTrue(matches(h,grouped(h,group("minecraft:cow","whitelist"),group("*","whitelist")),cow),
                "Overlapping target-only groups yield a single predicate match");
        var saved=(CompoundTag)call(r,type(h,RULES),"save",new Class<?>[0]);
        cat.setTame(true);h.assertTrue(matches(h,call(null,type(h,RULES),"readData",new Class<?>[]{CompoundTag.class},saved),cat),
                "Grouped serialization preserves attributes");
        h.succeed();
    }

    private static final String RULES = "cn.laowu.mod.item.CreatureFilterRules";
    private static Class<?> type(GameTestHelper h, String name) {
        try { return Class.forName(name); }
        catch (ClassNotFoundException e) { h.assertTrue(false, "Missing creature filter feature: " + name); throw new AssertionError(e); }
    }
    private static Object call(Object receiver, Class<?> owner, String name, Class<?>[] args, Object... values) {
        try { return owner.getMethod(name, args).invoke(receiver, values); }
        catch (InvocationTargetException e) { throw new RuntimeException(e.getCause()); }
        catch (ReflectiveOperationException e) { throw new AssertionError("Missing creature filter method " + name, e); }
    }
    private static Object rules(GameTestHelper h, int mask, boolean blacklist, String... ids) {
        Class<?> mode = type(h, RULES + "$Mode");
        Object value = Enum.valueOf((Class) mode, blacklist ? "BLACKLIST" : "WHITELIST");
        var entries = Arrays.stream(ids).map(ResourceLocation::tryParse).toList();
        return call(null, type(h, RULES), "of", new Class<?>[]{int.class, mode, Collection.class}, mask, value, entries);
    }
    private static boolean matches(GameTestHelper h, Object rules, Mob mob) {
        return (boolean) call(rules, type(h, RULES), "matches", new Class<?>[]{Mob.class}, mob);
    }
    private static String category(GameTestHelper h, Mob mob) {
        return call(null, type(h, RULES), "classify", new Class<?>[]{Mob.class}, mob).toString();
    }
    private static Mob mob(GameTestHelper h, EntityType<?> type) {
        Mob mob = (Mob) type.create(h.getLevel()); mob.setNoAi(true); return mob;
    }

    @GameTest(template = "artillery_probe", batch = "creature_filter", timeoutTicks = 30)
    public static void speciesCategoriesIgnoreAngerAndTeams(GameTestHelper h) {
        for (EntityType<?> type : new EntityType<?>[]{EntityType.COW, EntityType.CAT, EntityType.VILLAGER, EntityType.SNOW_GOLEM})
            h.assertTrue(category(h, mob(h, type)).equals("PASSIVE"), "Passive species: " + type);
        for (EntityType<?> type : new EntityType<?>[]{EntityType.WOLF, EntityType.BEE, EntityType.ENDERMAN, EntityType.ZOMBIFIED_PIGLIN,
                EntityType.PIGLIN, EntityType.PANDA, EntityType.LLAMA, EntityType.TRADER_LLAMA, EntityType.DOLPHIN, EntityType.GOAT, EntityType.IRON_GOLEM})
            h.assertTrue(category(h, mob(h, type)).equals("NEUTRAL"), "Stable neutral species: " + type);
        for (EntityType<?> type : new EntityType<?>[]{EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.PIGLIN_BRUTE})
            h.assertTrue(category(h, mob(h, type)).equals("HOSTILE"), "Native hostile taxonomy: " + type);
        Wolf wolf = (Wolf) mob(h, EntityType.WOLF);
        wolf.setRemainingPersistentAngerTime(400); wolf.setPersistentAngerTarget(UUID.randomUUID());
        h.assertTrue(category(h, wolf).equals("NEUTRAL") && matches(h, rules(h, 2, true), wolf)
                && !matches(h, rules(h, 4, true), wolf), "Current anger never turns neutral species into hostile category");
        h.succeed();
    }

    @GameTest(template = "artillery_probe", batch = "creature_filter", timeoutTicks = 30)
    public static void whitelistBlacklistAndCategoriesAreConjunctive(GameTestHelper h) {
        Mob cow = mob(h, EntityType.COW), zombie = mob(h, EntityType.ZOMBIE), wolf = mob(h, EntityType.WOLF);
        h.assertTrue(matches(h, rules(h, 7, true), cow) && matches(h, rules(h, 7, true), zombie), "Empty blacklist permits selected categories");
        h.assertTrue(!matches(h, rules(h, 7, false), cow), "Empty whitelist matches nothing");
        Object whitelist = rules(h, 1, false, "minecraft:cow", "minecraft:zombie");
        h.assertTrue(matches(h, whitelist, cow) && !matches(h, whitelist, zombie) && !matches(h, whitelist, wolf),
                "Whitelist does not bypass disabled categories");
        Object blacklist = rules(h, 7, true, "minecraft:cow");
        h.assertTrue(!matches(h, blacklist, cow) && matches(h, blacklist, wolf) && matches(h, blacklist, zombie), "Blacklist rejects only selected species");
        h.assertTrue(!matches(h, rules(h, 0, true), cow), "All categories off matches nothing");
        h.assertTrue(!matches(h, rules(h, 7, true), null), "Null target is never a match");
        h.succeed();
    }

    @GameTest(template = "artillery_probe", batch = "creature_filter", timeoutTicks = 30)
    public static void rulesPersistUnknownIdsAndUnrelatedItemData(GameTestHelper h) {
        Object expected = rules(h, 5, false, "minecraft:cow", "removedpack:still_saved", "minecraft:cow");
        ItemStack stack = new ItemStack(Items.PAPER, 3);
        stack.getOrCreateTag().putString("Unrelated", "kept");
        stack.setHoverName(net.minecraft.network.chat.Component.literal("kept name"));
        call(expected, type(h, RULES), "write", new Class<?>[]{ItemStack.class}, stack);
        Object restored = call(null, type(h, RULES), "read", new Class<?>[]{ItemStack.class}, stack);
        h.assertTrue((int) call(restored, type(h, RULES), "categoryMask", new Class<?>[0]) == 5
                && call(restored, type(h, RULES), "mode", new Class<?>[0]).toString().equals("WHITELIST"), "Category/mode persist exactly");
        List<?> ids = (List<?>) call(restored, type(h, RULES), "entityIds", new Class<?>[0]);
        h.assertTrue(ids.size() == 2 && ids.contains(ResourceLocation.tryParse("removedpack:still_saved")), "Deduplicate without cleaning missing mod entity IDs");
        h.assertTrue(stack.getCount() == 3 && stack.getHoverName().getString().equals("kept name") && stack.getOrCreateTag().getString("Unrelated").equals("kept"),
                "Saving rules preserves unrelated item metadata, custom name and count");
        Object empty = call(null, type(h, RULES), "readData", new Class<?>[]{CompoundTag.class}, new CompoundTag());
        h.assertTrue(!matches(h, empty, mob(h, EntityType.COW)), "Present empty settings must fail closed");
        CompoundTag malformed = new CompoundTag(); malformed.putInt("Version", 1); malformed.putInt("Categories", 255);
        Object invalid = call(null, type(h, RULES), "readData", new Class<?>[]{CompoundTag.class}, malformed);
        h.assertTrue(!matches(h, invalid, mob(h, EntityType.COW)), "Malformed saved rules fail closed");
        h.succeed();
    }

    @GameTest(template = "artillery_probe", batch = "creature_filter", timeoutTicks = 30)
    public static void configPacketBindsWindowAndHeldStack(GameTestHelper h) {
        Class<?> menuType = type(h, "cn.laowu.mod.CreatureFilterMenu");
        Class<?> packetType = type(h, "cn.laowu.mod.network.SetCreatureFilterPacket");
        var level = h.getLevel();
        var connection = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "creature-connection"));
        var player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "creature-probe"));
        player.connection = connection.connection;
        Object menu = null;
        try {
            var field = type(h, "cn.laowu.mod.LaoWuMod").getField("CREATURE_FILTER");
            Item item = (Item) ((java.util.function.Supplier<?>) field.get(null)).get();
            ItemStack held = new ItemStack(item); player.setItemInHand(InteractionHand.MAIN_HAND, held);
            menu = menuType.getConstructor(int.class, Inventory.class, ItemStack.class).newInstance(171, player.getInventory(), held);
            player.containerMenu = (net.minecraft.world.inventory.AbstractContainerMenu) menu;
            var constructor = packetType.getConstructor(int.class, int.class, boolean.class, List.class);
            Object wrong = constructor.newInstance(172, 4, false, List.of(ResourceLocation.tryParse("minecraft:zombie")));
            call(wrong, packetType, "apply", new Class<?>[]{ServerPlayer.class}, player);
            h.assertTrue((int) call(call(menu, menuType, "rules", new Class<?>[0]), type(h, RULES), "categoryMask", new Class<?>[0]) == 7,
                    "Wrong window ID cannot change filter draft");
            Object packet = constructor.newInstance(171, 4, false, List.of(ResourceLocation.tryParse("minecraft:zombie")));
            var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                packetType.getMethod("encode", packetType, net.minecraft.network.FriendlyByteBuf.class).invoke(null, packet, buffer);
                packet = packetType.getMethod("decode", net.minecraft.network.FriendlyByteBuf.class).invoke(null, buffer);
                h.assertTrue(!buffer.isReadable(), "Packet codec consumes bounded complete config");
            } finally { buffer.release(); }
            call(packet, packetType, "apply", new Class<?>[]{ServerPlayer.class}, player);
            Object applied = call(menu, menuType, "rules", new Class<?>[0]);
            h.assertTrue(matches(h, applied, mob(h, EntityType.ZOMBIE)) && !matches(h, applied, mob(h, EntityType.COW)), "Real config packet updates exact draft");
            h.assertTrue(held.equals(player.getMainHandItem()) && (int) call(call(null, type(h, RULES), "read", new Class<?>[]{ItemStack.class}, held), type(h, RULES), "categoryMask", new Class<?>[0]) == 7,
                    "Editing never mutates held metadata: native screen tick must remain open");
            ((net.minecraft.world.inventory.AbstractContainerMenu) menu).removed(player);
            Object saved = call(null, type(h, RULES), "read", new Class<?>[]{ItemStack.class}, held);
            h.assertTrue(matches(h, saved, mob(h, EntityType.ZOMBIE)) && !matches(h, saved, mob(h, EntityType.COW)), "Native close persists draft");
            menu = menuType.getConstructor(int.class, Inventory.class, ItemStack.class).newInstance(171, player.getInventory(), held);
            player.containerMenu = (net.minecraft.world.inventory.AbstractContainerMenu) menu;
            ItemStack replacement = new ItemStack(item); player.setItemInHand(InteractionHand.MAIN_HAND, replacement);
            Object stale = constructor.newInstance(171, 0, true, List.of());
            call(stale, packetType, "apply", new Class<?>[]{ServerPlayer.class}, player);
            Object unchanged = call(null, type(h, RULES), "read", new Class<?>[]{ItemStack.class}, replacement);
            h.assertTrue((int) call(unchanged, type(h, RULES), "categoryMask", new Class<?>[0]) == 7, "Held-stack replacement rejects stale config");
            h.succeed();
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
        finally { if (menu != null) ((net.minecraft.world.inventory.AbstractContainerMenu) menu).removed(player); player.discard(); }
    }

    @GameTest(template = "artillery_probe", batch = "creature_filter", timeoutTicks = 30)
    public static void explicitResetDropsLegacyCategoryGate(GameTestHelper h) {
        var level = h.getLevel();
        var connection = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "filter-reset"));
        var player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "filter-reset-probe"));
        player.connection = connection.connection;
        cn.laowu.mod.CreatureFilterMenu menu = null;
        try {
            for (int mask : new int[]{0, 1}) {
                ItemStack held = new ItemStack(cn.laowu.mod.LaoWuMod.CREATURE_FILTER.get());
                var legacy = cn.laowu.mod.item.CreatureFilterRules.of(mask,
                        cn.laowu.mod.item.CreatureFilterRules.Mode.WHITELIST, List.of(ResourceLocation.tryParse("minecraft:cow")));
                legacy.write(held); player.setItemInHand(InteractionHand.MAIN_HAND, held);
                menu = new cn.laowu.mod.CreatureFilterMenu(174, player.getInventory(), held); player.containerMenu = menu;
                var zombieCondition = cn.laowu.mod.item.CreatureFilterRules.Condition.entity(ResourceLocation.tryParse("minecraft:zombie"), false);
                var normalEdit = menu.rules().withConditions(menu.rules().mode(), List.of(zombieCondition));
                h.assertTrue(!normalEdit.matches(mob(h, EntityType.ZOMBIE)), "Ordinary editing retains legacy gate " + mask);
                menu.clearContents();
                h.assertTrue(menu.rules().mode() == cn.laowu.mod.item.CreatureFilterRules.Mode.WHITELIST
                        && !menu.rules().matches(mob(h, EntityType.COW)), "Explicit reset preserves mode and has no conditions");
                menu.configure(menu.rules().withConditions(menu.rules().mode(), List.of(zombieCondition)));
                h.assertTrue(menu.rules().matches(mob(h, EntityType.ZOMBIE)), "Explicit reset removes legacy gate before adding zombie: " + mask);
                h.assertTrue(cn.laowu.mod.item.CreatureFilterRules.read(held).categoryMask() == mask,
                        "Reset remains draft-only until close");
                menu.removed(player);
                h.assertTrue(cn.laowu.mod.item.CreatureFilterRules.read(held).matches(mob(h, EntityType.ZOMBIE)),
                        "Closing saves reset without restoring old category gate");
            }
            h.succeed();
        } finally { if (menu != null) menu.removed(player); player.discard(); }
    }
    @GameTest(template = "artillery_probe", batch = "creature_filter", timeoutTicks = 30)
    public static void conditionPacketRoundTripsModesAndInversion(GameTestHelper h) {
        Class<?> packetType = type(h, "cn.laowu.mod.network.SetCreatureFilterPacket");
        Mob cow = mob(h, EntityType.COW), sheep = mob(h, EntityType.SHEEP), zombie = mob(h, EntityType.ZOMBIE);
        try {
            for (String mode : List.of("whitelist", "whitelist_all", "blacklist")) {
                Object config = v2(h, mode, condition("Category", "passive", false), condition("EntityId", "minecraft:cow", true));
                Object packet = packetType.getConstructor(int.class, type(h, RULES)).newInstance(12, config);
                var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                try {
                    packetType.getMethod("encode", packetType, net.minecraft.network.FriendlyByteBuf.class).invoke(null, packet, buffer);
                    packet = packetType.getMethod("decode", net.minecraft.network.FriendlyByteBuf.class).invoke(null, buffer);
                    h.assertTrue(!buffer.isReadable(), "V2 codec consumes entire bounded payload");
                    Object restored = call(packet, packetType, "rules", new Class<?>[0]);
                    boolean[] expected = switch (mode) {
                        case "whitelist" -> new boolean[]{true, true, true};
                        case "whitelist_all" -> new boolean[]{false, true, false};
                        default -> new boolean[]{false, false, false};
                    };
                    h.assertTrue(matches(h, restored, cow) == expected[0] && matches(h, restored, sheep) == expected[1]
                            && matches(h, restored, zombie) == expected[2], "V2 codec preserves category, entity inversion and aggregation: " + mode);
                } finally { buffer.release(); }
            }
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
        h.succeed();
    }
    private static CompoundTag condition(String key, String value, boolean inverted) {
        CompoundTag tag = new CompoundTag(); tag.putString(key, value); tag.putBoolean("Inverted", inverted); return tag;
    }
    private static Object v2(GameTestHelper h, String mode, CompoundTag... conditions) {
        CompoundTag tag = new CompoundTag(); tag.putInt("Version", 2); tag.putInt("Categories", 7); tag.putString("Mode", mode);
        ListTag list = new ListTag(); list.addAll(Arrays.asList(conditions)); tag.put("Conditions", list);
        return call(null, type(h, RULES), "readData", new Class<?>[]{CompoundTag.class}, tag);
    }
    @GameTest(template = "artillery_probe", batch = "creature_filter", timeoutTicks = 30)
    public static void nativeConditionsUseOrAndNorAndPerConditionInversion(GameTestHelper h) {
        Mob cow = mob(h, EntityType.COW), zombie = mob(h, EntityType.ZOMBIE), wolf = mob(h, EntityType.WOLF);
        CompoundTag passive = condition("Category", "passive", false), isZombie = condition("EntityId", "minecraft:zombie", false);
        Object either = v2(h, "whitelist", passive, isZombie);
        h.assertTrue(matches(h, either, cow) && matches(h, either, zombie) && !matches(h, either, wolf), "OR accepts category OR species independently");
        Object both = v2(h, "whitelist_all", passive, condition("EntityId", "minecraft:cow", true));
        h.assertTrue(!matches(h, both, cow) && !matches(h, both, zombie) && matches(h, both, mob(h, EntityType.SHEEP)), "AND combines category with inverted species");
        Object neither = v2(h, "blacklist", passive, isZombie);
        h.assertTrue(!matches(h, neither, cow) && !matches(h, neither, zombie) && matches(h, neither, wolf), "Blacklist means NOT ANY condition");
        h.assertTrue(matches(h, v2(h, "whitelist", condition("Category", "hostile", true)), wolf), "Inverted category includes neutral creatures");
        h.succeed();
    }
    @GameTest(template = "artillery_probe", batch = "creature_filter", timeoutTicks = 30)
    public static void nativeEmptyRulesFailClosedWhileLegacyEmptyBlacklistMigrates(GameTestHelper h) {
        Mob cow = mob(h, EntityType.COW), zombie = mob(h, EntityType.ZOMBIE);
        for (String mode : List.of("whitelist", "whitelist_all", "blacklist"))
            h.assertTrue(!matches(h, v2(h, mode), cow), "Native empty filter has no creature attributes: " + mode);
        CompoundTag legacy = new CompoundTag(); legacy.putInt("Version", 1); legacy.putInt("Categories", 1);
        legacy.putString("Mode", "blacklist"); legacy.put("EntityIds", new ListTag());
        Object migrated = call(null, type(h, RULES), "readData", new Class<?>[]{CompoundTag.class}, legacy);
        CompoundTag saved = (CompoundTag) call(migrated, type(h, RULES), "save", new Class<?>[0]);
        Object restored = call(null, type(h, RULES), "readData", new Class<?>[]{CompoundTag.class}, saved);
        h.assertTrue(saved.getInt("Version") == 2 && matches(h, restored, cow) && !matches(h, restored, zombie),
                "Migration persists v1 category gate and empty blacklist acceptance across v2 save");
        ListTag species = new ListTag(); species.add(StringTag.valueOf("minecraft:cow")); species.add(StringTag.valueOf("minecraft:zombie"));
        legacy.putString("Mode", "whitelist"); legacy.put("EntityIds", species);
        migrated = call(null, type(h, RULES), "readData", new Class<?>[]{CompoundTag.class}, legacy);
        saved = (CompoundTag) call(migrated, type(h, RULES), "save", new Class<?>[0]);
        restored = call(null, type(h, RULES), "readData", new Class<?>[]{CompoundTag.class}, saved);
        h.assertTrue(matches(h, restored, cow) && !matches(h, restored, zombie) && !matches(h, restored, mob(h, EntityType.SHEEP)),
                "Migrated whitelist retains category AND selected species, never category OR species");
        h.succeed();
    }
    @GameTest(template = "artillery_probe", batch = "creature_filter", timeoutTicks = 30)
    public static void malformedV2RulesCannotBroadenTargets(GameTestHelper h) {
        Mob cow = mob(h, EntityType.COW);
        CompoundTag ambiguous = condition("Category", "passive", true); ambiguous.putString("EntityId", "minecraft:zombie");
        CompoundTag badFlag = condition("Category", "hostile", false); badFlag.putInt("Inverted", 1);
        for (CompoundTag entry : List.of(ambiguous, badFlag, condition("EntityId", "minecraft:player", true),
                condition("Category", "unknown", true), condition("EntityId", "INVALID ID", true))) {
            h.assertTrue(!matches(h, v2(h, "blacklist", entry), cow), "Malformed condition must fail closed, not vanish from blacklist");
        }
        CompoundTag[] oversized = new CompoundTag[65]; Arrays.fill(oversized, condition("Category", "hostile", false));
        h.assertTrue(!matches(h, v2(h, "blacklist", oversized), cow), "Bounds checked before deduplicating");
        Object missing = v2(h, "whitelist", condition("EntityId", "removedpack:creature", false));
        CompoundTag saved = (CompoundTag) call(missing, type(h, RULES), "save", new Class<?>[0]);
        h.assertTrue(saved.getList("Conditions", Tag.TAG_COMPOUND).getCompound(0).getString("EntityId").equals("removedpack:creature"),
                "Unknown mod entity IDs survive v2 saves without registry cleanup");
        h.succeed();
    }
}
