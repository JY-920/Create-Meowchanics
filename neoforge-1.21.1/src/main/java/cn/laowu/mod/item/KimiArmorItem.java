package cn.laowu.mod.item;

import cn.laowu.mod.client.KimiArmorClient;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** The four wearable pieces authored in the supplied Blockbench project. */
public final class KimiArmorItem extends ArmorItem {
    public KimiArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof Player player)
                || player.getItemBySlot(getType().getSlot()) != stack
                || CatToolBehavior.isExhausted(stack)) return;

        if (getType() == Type.HELMET && level.isNight()) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,
                    20 * 15, 0, true, false, true));
        }
        if (getType() == Type.CHESTPLATE && player.isShiftKeyDown()
                && player.tickCount % 40 == 0 && player.getHealth() < player.getMaxHealth()) {
            player.heal(player.getMaxHealth() * 0.05F);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity,
                                            EquipmentSlot slot, ArmorMaterial.Layer layer,
                                            boolean innerModel) {
        return KimiArmorClient.armorTexture(entity, slot);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(KimiArmorClient.extensions());
    }
}
