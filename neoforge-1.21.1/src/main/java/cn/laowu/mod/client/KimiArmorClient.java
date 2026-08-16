package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/** Client-only model and texture selection for the Kimi armour set. */
public final class KimiArmorClient {
    public static ResourceLocation armorTexture(Entity entity, EquipmentSlot slot) {
        if (slot == EquipmentSlot.HEAD) {
            return LaoWuMod.id("textures/models/armor/kimi_helmet.png");
        }
        if (slot == EquipmentSlot.CHEST && isSlimPlayer(entity)) {
            return LaoWuMod.id("textures/models/armor/kimi_armor_slim.png");
        }
        return LaoWuMod.id("textures/models/armor/kimi_armor.png");
    }

    public static IClientItemExtensions extensions() {
        return new IClientItemExtensions() {
            private KimiArmorModel model;
            private KimiArmorModel slimModel;

            @Override
            public Model getGenericArmorModel(LivingEntity entity, ItemStack stack,
                                               EquipmentSlot slot, HumanoidModel<?> original) {
                if (model == null) {
                    model = new KimiArmorModel(Minecraft.getInstance().getEntityModels()
                            .bakeLayer(KimiArmorModel.LAYER));
                    slimModel = new KimiArmorModel(Minecraft.getInstance().getEntityModels()
                            .bakeLayer(KimiArmorModel.SLIM_LAYER));
                }
                KimiArmorModel selected = isSlimPlayer(entity) ? slimModel : model;
                ClientHooks.copyModelProperties(original, selected);
                selected.setVisibleFor(slot);
                return selected;
            }
        };
    }

    private static boolean isSlimPlayer(Entity entity) {
        return entity instanceof AbstractClientPlayer player
                && player.getSkin().model() == PlayerSkin.Model.SLIM;
    }

    private KimiArmorClient() {}
}
