package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** Client-only model and texture selection for the Kimi armour set. */
public final class KimiArmorClient {
    public static String armorTexture(Entity entity, EquipmentSlot slot) {
        if (slot == EquipmentSlot.HEAD) {
            return LaoWuMod.id("textures/models/armor/kimi_helmet.png").toString();
        }
        if (slot == EquipmentSlot.CHEST && isSlimPlayer(entity)) {
            return LaoWuMod.id("textures/models/armor/kimi_armor_slim.png").toString();
        }
        return LaoWuMod.id("textures/models/armor/kimi_armor.png").toString();
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
                // Copy the animation first, then apply slot visibility so the
                // torso does not reappear while leggings are being rendered.
                KimiArmorModel selected = isSlimPlayer(entity) ? slimModel : model;
                ForgeHooksClient.copyModelProperties(original, selected);
                selected.setVisibleFor(slot);
                return selected;
            }
        };
    }

    private static boolean isSlimPlayer(Entity entity) {
        if (!(entity instanceof AbstractClientPlayer player)) return false;
        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        var activeRenderer = dispatcher.getRenderer(player);
        if (activeRenderer == dispatcher.getSkinMap().get("slim")) return true;
        String modelName = player.getModelName();
        return "slim".equalsIgnoreCase(modelName) || "alex".equalsIgnoreCase(modelName);
    }

    private KimiArmorClient() {}
}
