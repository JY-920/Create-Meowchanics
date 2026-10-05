package cn.laowu.mod.create;

import com.simibubi.create.AllItems;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Saved legacy stacks remain usable, but now behave as plain Create belt connectors. */
public final class CatBeltItem extends Item {
    public CatBeltItem(Properties properties) { super(properties); }
    @Override public InteractionResult useOn(UseOnContext context) {
        return AllItems.BELT_CONNECTOR.get().useOn(context);
    }
}
