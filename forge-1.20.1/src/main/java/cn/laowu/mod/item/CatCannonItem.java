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
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

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

        // Create 6.0.8's ShootableGadgetItemMethods.shouldSwap() reads _Swap
        // through ItemStack#getTag() without a null check. Smithing-created cat
        // cannons normally have no tag at all, so initialize the same state
        // container that a used vanilla Potato Cannon already possesses before
        // entering Create's native firing path.
        cannon.getOrCreateTag();

        // The same Create method touches the main-hand stack when a shootable
        // gadget is used from the off hand. Initialize that temporary tag only
        // when it is actually a supported projectile, then remove it again if
        // Create left it empty so ammunition keeps normal stack compatibility.
        ItemStack mainHand = player.getMainHandItem();
        boolean temporaryMainHandTag = hand == InteractionHand.OFF_HAND
                && getAllSupportedProjectiles().test(mainHand)
                && !mainHand.hasTag();
        if (temporaryMainHandTag) mainHand.getOrCreateTag();

        PotatoCannonItem.Ammo ammo = PotatoCannonItem.getAmmo(player, cannon);
        boolean firingGrenade = ammo != null && ammo.stack().is(LaoWuMod.CAT_GRENADE.get());
        InteractionResultHolder<ItemStack> result;
        try {
            result = super.use(level, player, hand);
        } finally {
            if (temporaryMainHandTag && mainHand.hasTag() && mainHand.getTag().isEmpty())
                mainHand.setTag(null);
        }
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
