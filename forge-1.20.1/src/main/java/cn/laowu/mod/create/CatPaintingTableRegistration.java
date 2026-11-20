package cn.laowu.mod.create;
import cn.laowu.mod.LaoWuMod;import com.simibubi.create.AllBlocks;
import net.minecraft.world.item.*;import net.minecraft.world.level.block.Block;import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class CatPaintingTableRegistration {
 private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,LaoWuMod.MOD_ID);
 private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,LaoWuMod.MOD_ID);
 private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,LaoWuMod.MOD_ID);
 private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,LaoWuMod.MOD_ID);
 public static final RegistryObject<CatPaintingTableBlock> BLOCK=BLOCKS.register("cat_painting_table",()->new CatPaintingTableBlock(BlockBehaviour.Properties.copy(AllBlocks.SCHEMATIC_TABLE.get())));
 public static final RegistryObject<Item> ITEM=ITEMS.register("cat_painting_table",()->new BlockItem(BLOCK.get(),new Item.Properties()));
 public static final RegistryObject<BlockEntityType<CatPaintingTableBlockEntity>> ENTITY=ENTITIES.register("cat_painting_table",()->BlockEntityType.Builder.of(CatPaintingTableBlockEntity::new,BLOCK.get()).build(null));
 public static final RegistryObject<MenuType<CatPaintingTableMenu>> MENU=MENUS.register("cat_painting_table",()->net.minecraftforge.common.extensions.IForgeMenuType.create((id,inv,data)->new CatPaintingTableMenu(id,inv,data.readBlockPos())));
 public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);MENUS.register(bus);}
}
