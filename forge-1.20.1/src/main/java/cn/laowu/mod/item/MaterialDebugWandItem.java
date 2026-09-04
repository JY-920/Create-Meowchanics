package cn.laowu.mod.item;

import cn.laowu.mod.CatMaterialEditorMenu;
import cn.laowu.mod.genetics.CatGenomeData;
import cn.laowu.mod.genetics.CatMaterialRegistry;
import cn.laowu.mod.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

/** Selects a source block and applies whole or semantic-region cat materials. */
public final class MaterialDebugWandItem extends Item {
    private static final String SELECTED_MATERIAL = "LaoWuSelectedCatMaterial";

    public MaterialDebugWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.FAIL;

        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(
                context.getLevel().getBlockState(context.getClickedPos()).getBlock());
        if (blockId == null) return InteractionResult.FAIL;
        ResourceLocation material = CatMaterialRegistry.blockMaterial(blockId);
        setSelectedMaterial(context.getItemInHand(), material);
        player.displayClientMessage(Component.translatable(
                "message.laowu.material_wand.selected_block",
                CatMaterialRegistry.displayName(material)), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                   LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Cat cat)) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.FAIL;

        CatGenomeData.ensure(cat);
        ModNetwork.syncCatGenomeToTracking(cat);
        ResourceLocation selected = selectedMaterial(stack);
        NetworkHooks.openScreen(serverPlayer,
                new SimpleMenuProvider((id, inventory, ignored) ->
                        new CatMaterialEditorMenu(id, inventory, cat, selected),
                        Component.translatable("screen.laowu.cat_material_editor")),
                buffer -> {
                    buffer.writeUUID(cat.getUUID());
                    buffer.writeResourceLocation(selected);
                    buffer.writeNbt(CatGenomeData.serialized(cat));
                });
        return InteractionResult.CONSUME;
    }

    public static ResourceLocation selectedMaterial(ItemStack stack) {
        if (stack.hasTag()) {
            ResourceLocation parsed = ResourceLocation.tryParse(
                    stack.getTag().getString(SELECTED_MATERIAL));
            if (parsed != null) return parsed;
        }
        return CatMaterialRegistry.OBSIDIAN;
    }

    public static void setSelectedMaterial(ItemStack stack, ResourceLocation material) {
        stack.getOrCreateTag().putString(SELECTED_MATERIAL, material.toString());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.laowu.material_debug_wand.cat")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.laowu.material_debug_wand.block")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.laowu.material_debug_wand.selected",
                        CatMaterialRegistry.displayName(selectedMaterial(stack)))
                .withStyle(ChatFormatting.AQUA));
    }
}
