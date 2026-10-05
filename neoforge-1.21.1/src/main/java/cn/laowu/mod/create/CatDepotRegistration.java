package cn.laowu.mod.create;

import cn.laowu.mod.LaoWuMod;
import com.simibubi.create.AllBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class CatDepotRegistration {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, LaoWuMod.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, LaoWuMod.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, LaoWuMod.MOD_ID);

    public static final DeferredHolder<Block, CatDepotBlock> CAT_DEPOT = BLOCKS.register("cat_depot",
            () -> new CatDepotBlock(BlockBehaviour.Properties.ofFullCopy(AllBlocks.DEPOT.get())));
    public static final DeferredHolder<Item, Item> CAT_DEPOT_ITEM = ITEMS.register("cat_depot",
            () -> new CatWorkSurfaceBlockItem(CAT_DEPOT.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CatDepotBlockEntity>> DEPOT_BE = ENTITIES.register("cat_depot",
            () -> BlockEntityType.Builder.of(CatDepotBlockEntity::new, CAT_DEPOT.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        bus.addListener(CatDepotRegistration::capabilities);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DEPOT_BE.get(), CatDepotBlockEntity::getItemHandler);
    }

    private CatDepotRegistration() {}
}
