package cn.laowu.mod.create;

import cn.laowu.mod.LaoWuMod;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.content.decoration.encasing.EncasingRegistry;
import com.simibubi.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.*;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.capabilities.*;

/** Additive variants: never replace Create's block or block-entity registrations. */
public final class CatMachineBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, LaoWuMod.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, LaoWuMod.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, LaoWuMod.MOD_ID);

    public static final DeferredHolder<Block, CasingBlock> CAT_CASING = BLOCKS.register("cat_casing",
            () -> new CasingBlock(BlockBehaviour.Properties.ofFullCopy(AllBlocks.ANDESITE_CASING.get())));
    public static final DeferredHolder<Block, CatEncasedShaftBlock> CAT_ENCASED_SHAFT = BLOCKS.register("cat_encased_shaft",
            () -> new CatEncasedShaftBlock(BlockBehaviour.Properties.ofFullCopy(AllBlocks.ANDESITE_ENCASED_SHAFT.get())));
    public static final DeferredHolder<Block, CatEncasedCogwheelBlock> CAT_ENCASED_COGWHEEL = BLOCKS.register("cat_encased_cogwheel",
            () -> new CatEncasedCogwheelBlock(BlockBehaviour.Properties.ofFullCopy(AllBlocks.ANDESITE_ENCASED_COGWHEEL.get()), false));
    public static final DeferredHolder<Block, CatEncasedCogwheelBlock> CAT_ENCASED_LARGE_COGWHEEL = BLOCKS.register("cat_encased_large_cogwheel",
            () -> new CatEncasedCogwheelBlock(BlockBehaviour.Properties.ofFullCopy(AllBlocks.ANDESITE_ENCASED_LARGE_COGWHEEL.get()), true));
    public static final DeferredHolder<Block, HajiBasinBlock> HAJI_BASIN = BLOCKS.register("haji_basin",
            () -> new HajiBasinBlock(BlockBehaviour.Properties.ofFullCopy(AllBlocks.BASIN.get())));

    public static final DeferredHolder<Item, Item> CAT_CASING_ITEM = ITEMS.register("cat_casing",
            () -> new BlockItem(CAT_CASING.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> HAJI_BASIN_ITEM = ITEMS.register("haji_basin",
            () -> new CatWorkSurfaceBlockItem(HAJI_BASIN.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SimpleKineticBlockEntity>> SHAFT_BE = ENTITIES.register("cat_encased_shaft",
            () -> BlockEntityType.Builder.of(CatMachineBlocks::shaft, CAT_ENCASED_SHAFT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SimpleKineticBlockEntity>> COG_BE = ENTITIES.register("cat_encased_cogwheel",
            () -> BlockEntityType.Builder.of(CatMachineBlocks::cog, CAT_ENCASED_COGWHEEL.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SimpleKineticBlockEntity>> LARGE_COG_BE = ENTITIES.register("cat_encased_large_cogwheel",
            () -> BlockEntityType.Builder.of(CatMachineBlocks::largeCog, CAT_ENCASED_LARGE_COGWHEEL.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HajiBasinBlockEntity>> BASIN_BE = ENTITIES.register("haji_basin",
            () -> BlockEntityType.Builder.of(HajiBasinBlockEntity::new, HAJI_BASIN.get()).build(null));

    private static SimpleKineticBlockEntity shaft(net.minecraft.core.BlockPos p, net.minecraft.world.level.block.state.BlockState s) {
        return new SimpleKineticBlockEntity(SHAFT_BE.get(), p, s);
    }
    private static SimpleKineticBlockEntity cog(net.minecraft.core.BlockPos p, net.minecraft.world.level.block.state.BlockState s) {
        return new SimpleKineticBlockEntity(COG_BE.get(), p, s);
    }
    private static SimpleKineticBlockEntity largeCog(net.minecraft.core.BlockPos p, net.minecraft.world.level.block.state.BlockState s) {
        return new SimpleKineticBlockEntity(LARGE_COG_BE.get(), p, s);
    }
    public static final DeferredHolder<Item, Item> CAT_BELT_ITEM = ITEMS.register("cat_belt",
            () -> new CatBeltItem(new Item.Properties()));

    public static void register(IEventBus bus) {
        CatDepotRegistration.register(bus);
        CreatureTransmitterRegistration.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        bus.addListener(CatMachineBlocks::setup);
        bus.addListener(CatMachineBlocks::capabilities);
    }
    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            com.simibubi.create.api.stress.BlockStressValues.IMPACTS.register(CAT_AUTO_LASER.get(), () -> 4.0);
            EncasingRegistry.addVariant(AllBlocks.SHAFT.get(), CAT_ENCASED_SHAFT.get());
            EncasingRegistry.addVariant(AllBlocks.COGWHEEL.get(), CAT_ENCASED_COGWHEEL.get());
            EncasingRegistry.addVariant(AllBlocks.LARGE_COGWHEEL.get(), CAT_ENCASED_LARGE_COGWHEEL.get());
        });
    }
    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BASIN_BE.get(), HajiBasinBlockEntity::getItemHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, BASIN_BE.get(), HajiBasinBlockEntity::getFluidHandler);
    }
    public static final DeferredHolder<Block, CatPressBlock> CAT_PRESS = BLOCKS.register("cat_press",
            () -> new CatPressBlock(BlockBehaviour.Properties.ofFullCopy(AllBlocks.MECHANICAL_PRESS.get())));
    public static final DeferredHolder<Block, CatMixerBlock> CAT_MIXER = BLOCKS.register("cat_mixer",
            () -> new CatMixerBlock(BlockBehaviour.Properties.ofFullCopy(AllBlocks.MECHANICAL_MIXER.get())));
    public static final DeferredHolder<Item, Item> CAT_PRESS_ITEM = ITEMS.register("cat_press",
            () -> new CatProcessorBlockItem(CAT_PRESS.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> CAT_MIXER_ITEM = ITEMS.register("cat_mixer",
            () -> new CatProcessorBlockItem(CAT_MIXER.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CatPressBlockEntity>> PRESS_BE = ENTITIES.register("cat_press",
            () -> BlockEntityType.Builder.of(CatPressBlockEntity::new, CAT_PRESS.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CatMixerBlockEntity>> MIXER_BE = ENTITIES.register("cat_mixer",
            () -> BlockEntityType.Builder.of(CatMixerBlockEntity::new, CAT_MIXER.get()).build(null));
    private CatMachineBlocks() {}
    public static final DeferredHolder<Block, CatAutoLaserBlock> CAT_AUTO_LASER = BLOCKS.register("cat_auto_laser",
            () -> new CatAutoLaserBlock(BlockBehaviour.Properties.ofFullCopy(AllBlocks.ANDESITE_CASING.get())));
    public static final DeferredHolder<Item, Item> CAT_AUTO_LASER_ITEM = ITEMS.register("cat_auto_laser",
            () -> new CatAutoLaserBlockItem(CAT_AUTO_LASER.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CatAutoLaserBlockEntity>> AUTO_LASER_BE = ENTITIES.register("cat_auto_laser",
            () -> BlockEntityType.Builder.of(CatAutoLaserBlockEntity::new, CAT_AUTO_LASER.get()).build(null));
}
