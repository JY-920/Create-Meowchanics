package cn.laowu.mod.item;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.genetics.CatTraitRegistry;
import cn.laowu.mod.genetics.CatTraitType;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** A whole saved trait, usable only through the server-authoritative cat editor. */
public final class CatTraitTokenItem extends Item {
    private static final String TAG = "LaoWuCatTraitToken";

    public CatTraitTokenItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.neoforged.neoforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    var minecraft = net.minecraft.client.Minecraft.getInstance();
                    renderer = new cn.laowu.mod.client.CatTraitTokenItemRenderer(
                            minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
                }
                return renderer;
            }
        });
    }

    public static ItemStack create(CatTraitType trait, int level) {
        if (trait == null || trait.id().toString().length() > 128 || !validLevel(level)) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(LaoWuMod.CAT_TRAIT_TOKEN.get());
        CompoundTag entry = new CompoundTag();
        entry.putString("Id", trait.id().toString());
        entry.putInt("Level", level);
        ItemCustomData.update(stack, root -> root.put(TAG, entry));
        return stack;
    }

    public static ResourceLocation traitId(ItemStack stack) {
        CompoundTag entry = data(stack);
        if (entry == null || !entry.contains("Id", Tag.TAG_STRING)) return null;
        String id = entry.getString("Id");
        return id.length() > 128 ? null : ResourceLocation.tryParse(id);
    }

    public static int level(ItemStack stack) {
        CompoundTag entry = data(stack);
        if (entry == null || !entry.contains("Level", Tag.TAG_INT)) return 0;
        int level = entry.getInt("Level");
        return validLevel(level) ? level : 0;
    }

    private static boolean validLevel(int level) {
        return level >= 1 && level <= CatTrait.MAX_UPGRADABLE_LEVEL;
    }

    private static CompoundTag data(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(LaoWuMod.CAT_TRAIT_TOKEN.get())) return null;
        CompoundTag root = ItemCustomData.copy(stack);
        return root != null && root.contains(TAG, Tag.TAG_COMPOUND) ? root.getCompound(TAG) : null;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                  LivingEntity target, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation id = traitId(stack);
        int storedLevel = level(stack);
        if (id == null || storedLevel == 0) {
            tooltip.add(Component.translatable("tooltip.laowu.cat_trait_token.invalid").withStyle(ChatFormatting.RED));
        } else {
            CatTraitType trait = CatTraitRegistry.resolve(id, net.neoforged.fml.util.thread.EffectiveSide.get().isClient());
            tooltip.add(Component.translatable("tooltip.laowu.cat_trait_token.trait", trait.title(), storedLevel)
                    .withStyle(trait.rarity().textFormatting()));
            tooltip.add(Component.translatable("tooltip.laowu.cat_trait_token.id", id.toString())
                    .withStyle(ChatFormatting.DARK_GRAY));
            if (trait.available()) tooltip.add(trait.description(storedLevel).copy().withStyle(ChatFormatting.GRAY));
            else tooltip.add(Component.translatable("tooltip.laowu.cat_trait_token.missing").withStyle(ChatFormatting.RED));
        }
        tooltip.add(Component.translatable("tooltip.laowu.cat_trait_token.editor_only").withStyle(ChatFormatting.GRAY));
    }
}
