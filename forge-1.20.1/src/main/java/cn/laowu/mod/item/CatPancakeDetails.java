package cn.laowu.mod.item;

import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Metadata-only tooltip: never constructs a preview entity or performs an online lookup. */
public final class CatPancakeDetails {
    public static final String OWNER_NAME = "LaoWuPancakeOwnerName";
    public static void append(ItemStack stack, CompoundTag root, Level level, List<Component> lines) {
        if (root == null) root = new CompoundTag();
        CompoundTag cat = root.getCompound(CatPancakeItem.CAT_DATA_TAG);
        UUID ownerId = root.hasUUID("LaoWuPancakeOwner") ? root.getUUID("LaoWuPancakeOwner")
                : cat.hasUUID("Owner") ? cat.getUUID("Owner") : null;
        if (ownerId == null && cat.contains("OwnerUUID")) {
            try { ownerId = UUID.fromString(cat.getString("OwnerUUID")); }
            catch (IllegalArgumentException ignored) {}
        }
        Component owner = Component.translatable("gui.laowu.cat_filter.ownership.unowned");
        if (CatPancakeItem.hasOwner(stack)) {
            var player = ownerId == null || level == null ? null : level.getPlayerByUUID(ownerId);
            String savedName = root.getString(OWNER_NAME);
            owner = player != null ? player.getName()
                    : !savedName.isBlank() ? Component.literal(savedName)
                    : ownerId != null ? Component.literal(ownerId.toString())
                    : Component.translatable("tooltip.laowu.cat_pancake.owner_unknown");
        }
        lines.add(Component.translatable("tooltip.laowu.cat_pancake.owner", owner).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.laowu.cat_pancake.career",
                Component.translatable("gui.laowu.cat_filter.career." + CatPancakeItem.getOutfit(stack).id()))
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.laowu.cat_pancake.age",
                Component.translatable(CatPancakeItem.isBaby(stack)
                        ? "tooltip.laowu.cat_pancake.baby" : "tooltip.laowu.cat_pancake.adult"))
                .withStyle(ChatFormatting.GRAY));
        DyeColor colour = cat.contains("CollarColor") ? DyeColor.byId(cat.getInt("CollarColor")) : DyeColor.RED;
        lines.add(Component.translatable("tooltip.laowu.cat_pancake.team",
                Component.translatable("tooltip.laowu.cat_team." + colour.getName()))
                .withStyle(ChatFormatting.GRAY));
    }
    private CatPancakeDetails() {}
}
