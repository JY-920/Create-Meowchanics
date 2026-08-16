package cn.laowu.mod.item;

import cn.laowu.mod.entity.CatBallEntity;
import com.simibubi.create.content.logistics.box.PackageItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Cat ball using Create's package charge curve and bow-style throw animation. */
public final class CatBallItem extends Item {
    public CatBallItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72_000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.success(held);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (!(user instanceof Player player)) return;

        int usedTicks = getUseDuration(stack) - timeLeft;
        float charge = PackageItem.getPackageVelocity(usedTicks);
        if (charge < 0.1F || level.isClientSide) return;

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5F, 0.5F);

        ItemStack thrownStack = stack.copyWithCount(1);
        if (!player.getAbilities().instabuild) stack.shrink(1);

        // This intentionally mirrors Create's cardboard-package release:
        // charge reaches full power after 20 ticks and velocity is charge * 2.
        Vec3 velocity = user.getLookAngle().scale(charge * 2.0F);
        Vec3 spawn = new Vec3(user.getX(), user.getY() + user.getBoundingBox().getYsize() / 2.0D,
                user.getZ()).add(velocity);

        CatBallEntity ball = new CatBallEntity(level, user);
        ball.setPos(spawn.x, spawn.y, spawn.z);
        ball.setItem(thrownStack);
        ball.setDeltaMovement(velocity);
        level.addFreshEntity(ball);
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new cn.laowu.mod.client.CatBallItemRenderer(
                            mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
                }
                return renderer;
            }
        });
    }
}
