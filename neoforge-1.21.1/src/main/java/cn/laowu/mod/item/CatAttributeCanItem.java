package cn.laowu.mod.item;

import net.minecraft.world.item.Item;

/**
 * Minimal placeholder for the attribute-can training food item. The genetics
 * effect layer only needs the type for an {@code instanceof} food check; the
 * full consumable behaviour is ported separately.
 */
public class CatAttributeCanItem extends Item {
    public CatAttributeCanItem(Properties properties) {
        super(properties);
    }
}
