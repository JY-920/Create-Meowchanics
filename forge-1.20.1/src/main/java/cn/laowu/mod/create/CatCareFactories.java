package cn.laowu.mod.create;
import static cn.laowu.mod.LaoWuMod.*;
import cn.laowu.mod.AdoptionBoxMenu;
import cn.laowu.mod.WishAdoptionBoxMenu;
import cn.laowu.mod.BreedingBoxMenu;
import cn.laowu.mod.item.BreedingBoxBlockItem;
import cn.laowu.mod.item.AdoptionBoxBlockItem;
import cn.laowu.mod.item.WishAdoptionBoxBlockItem;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.extensions.IForgeMenuType;


public final class CatCareFactories {
    public static Block blocks_basic_breeding_box() { return new BreedingBoxBlock(BreedingBoxTier.BASIC,
                    BlockBehaviour.Properties.copy(Blocks.BARREL).noOcclusion().strength(0.8F)); }
    public static Block blocks_intermediate_breeding_box() { return new BreedingBoxBlock(BreedingBoxTier.INTERMEDIATE,
                    BlockBehaviour.Properties.copy(Blocks.BARREL).noOcclusion().strength(1.2F)); }
    public static Block blocks_advanced_breeding_box() { return new BreedingBoxBlock(BreedingBoxTier.ADVANCED,
                    BlockBehaviour.Properties.copy(Blocks.BARREL).noOcclusion().strength(1.6F)); }
    public static Block blocks_adoption_box() { return new AdoptionBoxBlock(BlockBehaviour.Properties.copy(Blocks.BARREL)
                    .noOcclusion().strength(0.8F)); }
    public static Block blocks_wish_adoption_box() { return new WishAdoptionBoxBlock(BlockBehaviour.Properties.copy(Blocks.BARREL)
                    .noOcclusion().strength(0.8F)); }
    public static BlockEntityType<AdoptionBoxBlockEntity> block_entities_adoption_box() { return BlockEntityType.Builder
                    .of(AdoptionBoxBlockEntity::new, ADOPTION_BOX.get()).build(null); }
    public static BlockEntityType<WishAdoptionBoxBlockEntity> block_entities_wish_adoption_box() { return BlockEntityType.Builder
                    .of(WishAdoptionBoxBlockEntity::new, WISH_ADOPTION_BOX.get()).build(null); }
    public static BlockEntityType<net.minecraft.world.level.block.entity.BlockEntity> block_entities_breeding_box() {
        BlockEntityType.BlockEntitySupplier<net.minecraft.world.level.block.entity.BlockEntity> factory = cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::newBreedingBox : BreedingBoxData::new;
        return BlockEntityType.Builder.of(factory, BASIC_BREEDING_BOX.get(), INTERMEDIATE_BREEDING_BOX.get(), ADVANCED_BREEDING_BOX.get()).build(null);
    }
    public static Item items_basic_breeding_box() { return new BreedingBoxBlockItem(
                    BASIC_BREEDING_BOX.get(), new Item.Properties()); }
    public static Item items_intermediate_breeding_box() { return new BreedingBoxBlockItem(
                    INTERMEDIATE_BREEDING_BOX.get(), new Item.Properties()); }
    public static Item items_advanced_breeding_box() { return new BreedingBoxBlockItem(
                    ADVANCED_BREEDING_BOX.get(), new Item.Properties()); }
    public static Item items_adoption_box() { return new AdoptionBoxBlockItem(ADOPTION_BOX.get(), new Item.Properties()); }
    public static Item items_wish_adoption_box() { return new WishAdoptionBoxBlockItem(WISH_ADOPTION_BOX.get(), new Item.Properties()); }
    public static Item items_cat_storage_box() { return new cn.laowu.mod.item.CatStorageBoxItem(new Item.Properties()); }
    public static MenuType<AdoptionBoxMenu> menus_adoption_box() { return IForgeMenuType.create(AdoptionBoxMenu::new); }
    public static MenuType<WishAdoptionBoxMenu> menus_wish_adoption_box() { return IForgeMenuType.create(WishAdoptionBoxMenu::new); }
    public static MenuType<BreedingBoxMenu> menus_breeding_box() { return IForgeMenuType.create(BreedingBoxMenu::new); }
}
