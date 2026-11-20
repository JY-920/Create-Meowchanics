package cn.laowu.mod.create;

import cn.laowu.mod.LaoWuMod;
import com.simibubi.create.AllBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CatDepotRegistration {
    private static final DeferredRegister<com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType> ARM_POINTS =
            DeferredRegister.create(com.simibubi.create.api.registry.CreateRegistries.ARM_INTERACTION_POINT_TYPE, LaoWuMod.MOD_ID);
    static { ARM_POINTS.register("cat_depot", CatDepotArmType::new); }
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, LaoWuMod.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, LaoWuMod.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, LaoWuMod.MOD_ID);

    public static final RegistryObject<CatDepotBlock> CAT_DEPOT = BLOCKS.register("cat_depot",
            () -> new CatDepotBlock(BlockBehaviour.Properties.copy(AllBlocks.DEPOT.get())));
    public static final RegistryObject<Item> CAT_DEPOT_ITEM = ITEMS.register("cat_depot",
            () -> new CatWorkSurfaceBlockItem(CAT_DEPOT.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<CatDepotBlockEntity>> DEPOT_BE = ENTITIES.register("cat_depot",
            () -> BlockEntityType.Builder.of(CatDepotBlockEntity::new, CAT_DEPOT.get()).build(null));

    public static void register(IEventBus bus) {
        // Create's depot matcher only accepts its own block IDs, not subclasses.
        ARM_POINTS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
    }

    private CatDepotRegistration() {}
}
