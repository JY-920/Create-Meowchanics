package cn.laowu.mod.create;
import cn.laowu.mod.LaoWuMod;import com.simibubi.create.AllBlocks;
import net.minecraft.world.item.*;import net.minecraft.world.level.block.Block;import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import net.minecraft.core.registries.Registries;
public final class CatPaintingTableRegistration {
 private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,LaoWuMod.MOD_ID);
 private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,LaoWuMod.MOD_ID);
 private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,LaoWuMod.MOD_ID);
 private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,LaoWuMod.MOD_ID);
 public static final DeferredHolder<Block,CatPaintingTableBlock> BLOCK=BLOCKS.register("cat_painting_table",()->new CatPaintingTableBlock(BlockBehaviour.Properties.ofFullCopy(AllBlocks.SCHEMATIC_TABLE.get())));
 public static final DeferredHolder<Item,Item> ITEM=ITEMS.register("cat_painting_table",()->new BlockItem(BLOCK.get(),new Item.Properties()));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<CatPaintingTableBlockEntity>> ENTITY=ENTITIES.register("cat_painting_table",()->BlockEntityType.Builder.of(CatPaintingTableBlockEntity::new,BLOCK.get()).build(null));
 public static final DeferredHolder<MenuType<?>,MenuType<CatPaintingTableMenu>> MENU=MENUS.register("cat_painting_table",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id,inv,data)->new CatPaintingTableMenu(id,inv,data.readBlockPos())));
 public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);MENUS.register(bus);}
}
