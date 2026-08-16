package cn.laowu.mod.item;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

public final class CatStripItem extends Item {
    public CatStripItem(Properties properties) {
        super(properties.food(new FoodProperties.Builder()
                .nutrition(4)
                .saturationMod(0.3F)
                .fast()
                .build()));
    }
}
