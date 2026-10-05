package cn.laowu.mod.create;
import cn.laowu.mod.LaoWuMod;
import com.simibubi.create.AllBlocks;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import net.minecraft.core.registries.Registries;
public final class CreatureTransmitterRegistration {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,LaoWuMod.MOD_ID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,LaoWuMod.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,LaoWuMod.MOD_ID);
    public static final DeferredHolder<Block,CreatureTransmitterBlock> BLOCK=BLOCKS.register("creature_transmitter",
        ()->new CreatureTransmitterBlock(BlockBehaviour.Properties.ofFullCopy(AllBlocks.ANDESITE_CASING.get())));
    public static final DeferredHolder<Item,Item> ITEM=ITEMS.register("creature_transmitter",()->new BlockItem(BLOCK.get(),new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<CreatureTransmitterBlockEntity>> ENTITY=ENTITIES.register("creature_transmitter",
        ()->BlockEntityType.Builder.of(CreatureTransmitterBlockEntity::new,BLOCK.get()).build(null));
    public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);}
    private CreatureTransmitterRegistration(){}
}

