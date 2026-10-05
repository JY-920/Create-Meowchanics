package cn.laowu.mod.test;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.create.*;
import cn.laowu.mod.genetics.*;
import cn.laowu.mod.item.CatPancakeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;
import java.util.List;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class AdoptionInputDisplayProbe {
    private static ItemStack cat(String variant) {
        ItemStack stack = CatPancakeItem.variantStack(ResourceLocation.tryParse("minecraft:" + variant));
        CatGenomeData.set(stack, CatGenome.uniform(ResourceLocation.tryParse("minecraft:" + variant)));
        CatTraitData.set(stack, CatTraitProfile.EMPTY.withLevel(CatTrait.HIM, 1));
        CompoundTag nested = new CompoundTag();
        nested.putString("Brain", "private".repeat(1000));
        nested.putString("Inventory", "private".repeat(1000));
        nested.putInt("Age", -200);
        cn.laowu.mod.item.ItemCustomData.update(stack, root -> root.put(CatPancakeItem.CAT_DATA_TAG, nested));
        return stack;
    }
    private static void appearance(GameTestHelper h, CompoundTag update, String coat) {
        CompoundTag display = update.getList("InputDisplays", net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(0);
        h.assertTrue(display.getString(CatPancakeItem.CAT_TEXTURE_TAG).equals("minecraft:textures/entity/cat/" + coat + ".png"),
                "First input pancake appearance must reach nearby clients");
        h.assertTrue(display.contains(CatGenomeData.TAG) && display.contains(CatTraitData.TAG),
                "Actual genome and visual traits survive appearance sync");
        h.assertTrue(display.getBoolean(CatPancakeItem.BABY_TAG), "Legacy captured baby appearance survives sync");
        h.assertTrue(!update.contains("Inventory") && !display.contains(CatPancakeItem.CAT_DATA_TAG)
                && !display.contains(CatAttributeData.TAG) && update.toString().length() < 6000,
                "World display never sends full inventory, brain or attribute data");
    }
    @GameTest(template="accessory_probe", batch="adoption_input_display", timeoutTicks=20)
    public static void ordinaryFirstInputAndRemoval(GameTestHelper h) {
        var box = new AdoptionBoxBlockEntity(BlockPos.ZERO, LaoWuMod.ADOPTION_BOX.get().defaultBlockState());
        box.inventory().setStackInSlot(0, new ItemStack(Items.STONE));
        ItemStack first = cat("all_black");
        box.inventory().setStackInSlot(2, first);
        box.inventory().setStackInSlot(4, cat("red"));
        box.inventory().setStackInSlot(9, cat("siamese"));
        appearance(h, box.getUpdateTag(h.getLevel().registryAccess()), "all_black");
        h.assertTrue(CatPancakeItem.isBaby(first), "Preparing preview does not mutate original cat data");
        box.inventory().extractItem(2, 1, false);
        appearance(h, box.getUpdateTag(h.getLevel().registryAccess()), "red");
        box.inventory().extractItem(4, 1, false);
        h.assertTrue(box.getUpdateTag(h.getLevel().registryAccess()).getList("InputDisplays", net.minecraft.nbt.Tag.TAG_COMPOUND).isEmpty(), "No stale pancake or output-slot pancake after extraction");
        h.succeed();
    }
    @GameTest(template="accessory_probe", batch="adoption_input_display", timeoutTicks=20)
    public static void wishRetainedInputAndImmediateTrade(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(1, 1, 1));
        h.getLevel().setBlock(pos, LaoWuMod.WISH_ADOPTION_BOX.get().defaultBlockState(), 3);
        var box = (WishAdoptionBoxBlockEntity) h.getLevel().getBlockEntity(pos);
        var card = new WishAdoptionOffer(false, List.of(
                new WishAdoptionOffer.Condition(CatStat.HEALTH, 60, -1),
                new WishAdoptionOffer.Condition(CatStat.ATTACK, 40, -1)),
                "laowu:cat_health_badge");
        CompoundTag saved = box.saveWithoutMetadata(h.getLevel().registryAccess());
        saved.put("Offer", card.save()); saved.putBoolean("Locked", true); box.loadAdditional(saved, h.getLevel().registryAccess());
        ItemStack pancake = cat("all_black");
        var attributes = CatAttributeProfile.founder(RandomSource.create(13));
        for (CatStat stat : CatStat.values()) attributes = attributes.withValues(stat, 0, 100);
        CatAttributeData.set(pancake, attributes);
        box.inventory().setStackInSlot(3, pancake);
        WishAdoptionBoxBlockEntity.serverTick(h.getLevel(), pos, box.getBlockState(), box);
        appearance(h, box.getUpdateTag(h.getLevel().registryAccess()), "all_black");
        h.assertTrue(box.getUpdateTag(h.getLevel().registryAccess()).getCompound("Offer").equals(card.save()), "Display preserves reward card");
        for (CatStat stat : CatStat.values()) attributes = attributes.withValues(stat, 100, 100);
        CatAttributeData.set(pancake, attributes);
        box.inventory().setStackInSlot(3, pancake.copy());
        WishAdoptionBoxBlockEntity.serverTick(h.getLevel(), pos, box.getBlockState(), box);
        h.assertTrue(box.inventory().getStackInSlot(3).isEmpty()
                && box.getUpdateTag(h.getLevel().registryAccess()).getList("InputDisplays", net.minecraft.nbt.Tag.TAG_COMPOUND).isEmpty()
                && box.inventory().getStackInSlot(9).is(card.rewardStack().getItem()),
                "Successful trade remains immediate and clears pancake display");
        h.succeed();
    }

    private static final String[] COATS = {"all_black", "red", "siamese", "white", "jellie",
            "calico", "british_shorthair", "persian", "ragdoll"};
    private static void allAppearances(GameTestHelper h, CompoundTag update, int... slots) {
        var displays = update.getList("InputDisplays", net.minecraft.nbt.Tag.TAG_COMPOUND);
        h.assertTrue(displays.size() == slots.length,
                "Every occupied input slot synchronizes one actual pancake: expected " + slots.length + ", got " + displays.size());
        for (int index = 0; index < slots.length; index++) {
            int slot = slots[index];
            CompoundTag display = displays.getCompound(index);
            h.assertTrue(display.getInt("Slot") == slot, "Sparse displays keep stable input-slot order");
            h.assertTrue(display.getString(CatPancakeItem.CAT_TEXTURE_TAG).equals("minecraft:textures/entity/cat/" + COATS[slot] + ".png"),
                    "Input " + slot + " keeps its actual coat");
            h.assertTrue(display.contains(CatGenomeData.TAG) && display.contains(CatTraitData.TAG)
                    && display.getBoolean(CatPancakeItem.BABY_TAG), "Each cat keeps genome, traits and legacy baby appearance");
            h.assertTrue(!display.contains(CatPancakeItem.CAT_DATA_TAG) && !display.contains(CatAttributeData.TAG),
                    "Display never sends captured entities or attributes");
        }
        h.assertTrue(!update.contains("Inventory") && !update.contains("InputDisplay") && update.toString().length() < 16000,
                "World payload is bounded and appearance-only");
    }
    @GameTest(template="accessory_probe", batch="adoption_input_display", timeoutTicks=20)
    public static void ordinaryNineInputsRemovalAndReload(GameTestHelper h) {
        var box = new AdoptionBoxBlockEntity(BlockPos.ZERO, LaoWuMod.ADOPTION_BOX.get().defaultBlockState());
        for (int slot = 0; slot < 9; slot++) box.inventory().setStackInSlot(slot, cat(COATS[slot]));
        box.inventory().setStackInSlot(9, cat("tabby"));
        allAppearances(h, box.getUpdateTag(h.getLevel().registryAccess()), 0, 1, 2, 3, 4, 5, 6, 7, 8);
        box.inventory().extractItem(4, 1, false);
        box.inventory().setStackInSlot(0, new ItemStack(Items.STONE));
        allAppearances(h, box.getUpdateTag(h.getLevel().registryAccess()), 1, 2, 3, 5, 6, 7, 8);
        var reloaded = new AdoptionBoxBlockEntity(BlockPos.ZERO, LaoWuMod.ADOPTION_BOX.get().defaultBlockState());
        reloaded.loadAdditional(box.saveWithoutMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
        allAppearances(h, reloaded.getUpdateTag(h.getLevel().registryAccess()), 1, 2, 3, 5, 6, 7, 8);
        var client = new AdoptionBoxBlockEntity(BlockPos.ZERO, LaoWuMod.ADOPTION_BOX.get().defaultBlockState());
        client.loadAdditional(box.getUpdateTag(h.getLevel().registryAccess()), h.getLevel().registryAccess());
        h.assertTrue(CatPancakeItem.texture(client.inputDisplay()).toString().endsWith("/red.png"),
                "Single-preview getter still returns first actual input");
        for (int slot = 0; slot < 9; slot++) box.inventory().setStackInSlot(slot, ItemStack.EMPTY);
        allAppearances(h, box.getUpdateTag(h.getLevel().registryAccess()));
        client.loadAdditional(box.getUpdateTag(h.getLevel().registryAccess()), h.getLevel().registryAccess());
        h.assertTrue(client.inputDisplay().isEmpty(), "Empty update clears client; output cat is never displayed");
        h.succeed();
    }
    @GameTest(template="accessory_probe", batch="adoption_input_display", timeoutTicks=20)
    public static void boundedDisplaysAndLegacyCompatibility(GameTestHelper h) {
        var inventory = new net.neoforged.neoforge.items.ItemStackHandler(12);
        for (int slot = 0; slot < 12; slot++) inventory.setStackInSlot(slot, cat(COATS[slot % 9]));
        CompoundTag update = new CompoundTag();
        update.put("InputDisplay", new CompoundTag());
        AdoptionPancakeDisplay.write(update, inventory, 12);
        allAppearances(h, update, 0, 1, 2, 3, 4, 5, 6, 7, 8);
        var decoded = AdoptionPancakeDisplay.readAll(update);
        h.assertTrue(decoded.size() == 9, "Client display storage is capped at nine slots");
        for (int slot = 0; slot < 9; slot++)
            h.assertTrue(CatPancakeItem.texture(decoded.get(slot)).toString().endsWith("/" + COATS[slot] + ".png"),
                    "Client reconstructs every input coat in its original slot");
        var extra = update.getList("InputDisplays", net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(0).copy();
        extra.putString(CatPancakeItem.CAT_TEXTURE_TAG, "minecraft:textures/entity/cat/white.png");
        update.getList("InputDisplays", net.minecraft.nbt.Tag.TAG_COMPOUND).add(extra);
        h.assertTrue(AdoptionPancakeDisplay.readAll(update).size() == 9
                && CatPancakeItem.texture(AdoptionPancakeDisplay.read(update)).toString().endsWith("/all_black.png"),
                "Client caps excessive payload entries without overwriting the first cat");
        CompoundTag legacy = new CompoundTag();
        CompoundTag appearance = new CompoundTag();
        appearance.putString(CatPancakeItem.CAT_TEXTURE_TAG, "minecraft:textures/entity/cat/siamese.png");
        legacy.put("InputDisplay", appearance);
        h.assertTrue(CatPancakeItem.texture(AdoptionPancakeDisplay.read(legacy)).toString().endsWith("/siamese.png"),
                "Old single-display packet still decodes");
        h.assertTrue(AdoptionPancakeDisplay.read(new CompoundTag()).isEmpty(), "Empty old packet clears display");
        legacy.put("InputDisplays", new net.minecraft.nbt.ListTag());
        h.assertTrue(AdoptionPancakeDisplay.read(legacy).isEmpty(), "Explicit empty new list clears even a legacy preview");
        h.succeed();
    }
    @GameTest(template="accessory_probe", batch="adoption_input_display", timeoutTicks=20)
    public static void wishNineInputsRemovalAndReload(GameTestHelper h) {
        var box = new WishAdoptionBoxBlockEntity(BlockPos.ZERO, LaoWuMod.WISH_ADOPTION_BOX.get().defaultBlockState());
        for (int slot = 0; slot < 9; slot++) box.inventory().setStackInSlot(slot, cat(COATS[slot]));
        box.inventory().setStackInSlot(9, cat("tabby"));
        allAppearances(h, box.getUpdateTag(h.getLevel().registryAccess()), 0, 1, 2, 3, 4, 5, 6, 7, 8);
        box.inventory().extractItem(4, 1, false);
        allAppearances(h, box.getUpdateTag(h.getLevel().registryAccess()), 0, 1, 2, 3, 5, 6, 7, 8);
        var reloaded = new WishAdoptionBoxBlockEntity(BlockPos.ZERO, LaoWuMod.WISH_ADOPTION_BOX.get().defaultBlockState());
        reloaded.loadAdditional(box.saveWithoutMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
        allAppearances(h, reloaded.getUpdateTag(h.getLevel().registryAccess()), 0, 1, 2, 3, 5, 6, 7, 8);
        for (int slot = 0; slot < 9; slot++) box.inventory().setStackInSlot(slot, ItemStack.EMPTY);
        allAppearances(h, box.getUpdateTag(h.getLevel().registryAccess()));
        h.succeed();
    }
}
