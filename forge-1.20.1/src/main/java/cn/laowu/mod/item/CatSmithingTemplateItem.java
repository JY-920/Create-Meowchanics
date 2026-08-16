package cn.laowu.mod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.SmithingTemplateItem;

import java.util.List;

public final class CatSmithingTemplateItem extends SmithingTemplateItem {
    private static final String DESCRIPTION_ID = "item.laowu.cat_upgrade_smithing_template";

    public CatSmithingTemplateItem() {
        super(Component.translatable("item.laowu.smithing_template.applies_to"),
                Component.translatable("item.laowu.smithing_template.ingredients"),
                Component.translatable("item.laowu.smithing_template.upgrade"),
                Component.translatable("item.laowu.smithing_template.base_slot"),
                Component.translatable("item.laowu.smithing_template.addition_slot"),
                List.of(slot("sword"), slot("pickaxe"), slot("axe"), slot("hoe"), slot("shovel")),
                List.of(slot("ingot")));
    }

    private static ResourceLocation slot(String name) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "item/empty_slot_" + name);
    }

    @Override
    public String getDescriptionId() {
        return DESCRIPTION_ID;
    }
}
