package cn.laowu.mod.item;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.client.CatCannonItemRenderer;
import com.simibubi.create.content.equipment.potatoCannon.PotatoCannonItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;
import java.util.function.Predicate;

public final class CatCannonItem extends PotatoCannonItem {
    public CatCannonItem(Properties properties) {
        super(properties);
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return stack -> stack.is(LaoWuMod.CAT_PANCAKE.get())
                || stack.is(LaoWuMod.CAT_GRENADE.get());
    }

    /** PotatoCannonItem assigns 100 durability in its constructor; the cat cannon is inexhaustible. */
    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!hasLooseAmmo(player)) moveOneCatFromPouch(player);
        ItemStack cannon = player.getItemInHand(hand);

        PotatoCannonItem.Ammo ammo = PotatoCannonItem.getAmmo(player, cannon);
        boolean firingGrenade = ammo != null && ammo.stack().is(LaoWuMod.CAT_GRENADE.get());
        InteractionResultHolder<ItemStack> result = super.use(level, player, hand);
        if (!level.isClientSide && firingGrenade && result.getResult().consumesAction()) {
            ItemStack shell = new ItemStack(LaoWuMod.CAT_SHELL.get());
            if (!player.getInventory().add(shell)) player.drop(shell, false);
        }
        return result;
    }

    private static boolean hasLooseAmmo(Player player) {
        for (ItemStack stack : player.getInventory().items)
            if (stack.is(LaoWuMod.CAT_PANCAKE.get())
                    || stack.is(LaoWuMod.CAT_GRENADE.get())) return true;
        return player.getOffhandItem().is(LaoWuMod.CAT_PANCAKE.get())
                || player.getOffhandItem().is(LaoWuMod.CAT_GRENADE.get());
    }

    private static void moveOneCatFromPouch(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (!(stack.getItem() instanceof CatPouchItem) || CatPouchItem.count(stack) <= 0) continue;
            ItemStack cat = CatPouchItem.extractOne(stack);
            if (!player.getInventory().add(cat)) CatPouchItem.insertOne(stack, cat);
            return;
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft minecraft = Minecraft.getInstance();
                    renderer = new CatCannonItemRenderer(minecraft.getBlockEntityRenderDispatcher(),
                            minecraft.getEntityModels());
                }
                return renderer;
            }
        });
    }
}
