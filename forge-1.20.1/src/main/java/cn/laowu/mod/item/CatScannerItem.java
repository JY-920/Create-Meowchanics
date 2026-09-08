package cn.laowu.mod.item;

import cn.laowu.mod.CatProfileData;
import cn.laowu.mod.CatProfileMenu;
import cn.laowu.mod.client.CatScannerItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Opens the full cat profile without overloading the cat's ordinary gestures. */
public final class CatScannerItem extends Item {
    public CatScannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                   LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Cat cat)) return InteractionResult.PASS;
        return openProfile(player, cat);
    }

    public static InteractionResult openProfile(Player player, Cat cat) {
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)
                || !CatProfileData.canOpen(serverPlayer, cat)) {
            return InteractionResult.FAIL;
        }

        NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                        (containerId, playerInventory, ignored) ->
                                new CatProfileMenu(containerId, playerInventory, cat),
                        Component.translatable("container.laowu.cat_profile")),
                buffer -> {
                    buffer.writeVarInt(cat.getId());
                    buffer.writeUtf(CatProfileData.editableName(cat),
                            CatProfileData.MAX_NAME_LENGTH);
                });
        return InteractionResult.CONSUME;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft minecraft = Minecraft.getInstance();
                    renderer = new CatScannerItemRenderer(
                            minecraft.getBlockEntityRenderDispatcher(),
                            minecraft.getEntityModels());
                }
                return renderer;
            }
        });
    }
}
