package cn.laowu.mod.item;

import cn.laowu.mod.client.KimiArmorClient;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

/** The four wearable pieces authored in the supplied “基米衣” Blockbench project. */
public final class KimiArmorItem extends ArmorItem {
    public KimiArmorItem(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void onArmorTick(ItemStack stack, Level level, Player player) {
        if (level.isClientSide || CatToolBehavior.isExhausted(stack)) return;

        if (getType() == Type.HELMET && level.isNight()) {
            // Refresh before the previous short instance expires, keeping the
            // vanilla night-vision fade from pulsing while the helmet is worn.
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,
                    20 * 15, 0, true, false, true));
        }

        if (getType() == Type.CHESTPLATE && player.isShiftKeyDown()
                && player.tickCount % (2 * 20) == 0 && player.getHealth() < player.getMaxHealth()) {
            // Query max health at the moment of healing so attributes supplied
            // by adventure/RPG mods are included automatically.
            player.heal(player.getMaxHealth() * 0.05F);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return KimiArmorClient.armorTexture(entity, slot);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(KimiArmorClient.extensions());
    }
}
