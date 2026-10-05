package cn.laowu.mod.create;
import cn.laowu.mod.LaoWuMod;
import com.simibubi.create.AllBlocks;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
public final class CreatureTransmitterRegistration {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,LaoWuMod.MOD_ID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,LaoWuMod.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,LaoWuMod.MOD_ID);
    public static final RegistryObject<CreatureTransmitterBlock> BLOCK=BLOCKS.register("creature_transmitter",
        ()->new CreatureTransmitterBlock(BlockBehaviour.Properties.copy(AllBlocks.ANDESITE_CASING.get())));
    public static final RegistryObject<Item> ITEM=ITEMS.register("creature_transmitter",()->new BlockItem(BLOCK.get(),new Item.Properties()));
    public static final RegistryObject<BlockEntityType<CreatureTransmitterBlockEntity>> ENTITY=ENTITIES.register("creature_transmitter",
        ()->BlockEntityType.Builder.of(CreatureTransmitterBlockEntity::new,BLOCK.get()).build(null));
    public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);}
    private CreatureTransmitterRegistration(){}
}

