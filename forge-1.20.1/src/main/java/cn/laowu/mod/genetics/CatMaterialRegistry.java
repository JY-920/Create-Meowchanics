package cn.laowu.mod.genetics;

import cn.laowu.mod.LaoWuMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Stable ids for built-in, mod-supplied and block-mapped cat materials.
 *
 * <p>The genome saves these ids rather than texture paths, allowing texture
 * packs and future material additions without rewriting existing cat NBT.</p>
 */
public final class CatMaterialRegistry {
    public static final ResourceLocation OBSIDIAN = LaoWuMod.id("material/obsidian");
    public static final ResourceLocation WOOD = LaoWuMod.id("material/wood");
    private static final String BLOCK_PREFIX = "block/";

    private static final List<ResourceLocation> CUSTOM_MATERIALS = List.of(OBSIDIAN, WOOD);
    private static volatile List<Block> allMappedBlocks;
    private static volatile List<ResourceLocation> coreMaterials;
    private static volatile List<ResourceLocation> allMutationMaterials;

    public static Optional<ResourceLocation> fixedTexture(ResourceLocation material) {
        if (OBSIDIAN.equals(material)) {
            return Optional.of(LaoWuMod.id("textures/entity/cat/materials/obsidian.png"));
        }
        if (WOOD.equals(material)) {
            return Optional.of(LaoWuMod.id("textures/entity/cat/materials/wood.png"));
        }
        return Optional.empty();
    }

    /**
     * Every material eligible for appearance mutation. This is a stable,
     * registry-derived pool: it does not depend on which blocks one client has
     * viewed, exported, or previously cached.
     */
    public static List<ResourceLocation> mutationMaterials() {
        List<ResourceLocation> materials = allMutationMaterials;
        if (materials != null) return materials;
        synchronized (CatMaterialRegistry.class) {
            materials = allMutationMaterials;
            if (materials == null) {
                Set<ResourceLocation> result = new LinkedHashSet<>(coreMaterials());
                mappedBlocks().stream().map(CatMaterialRegistry::blockMaterial)
                        .flatMap(Optional::stream).forEach(result::add);
                materials = List.copyOf(result);
                allMutationMaterials = materials;
            }
        }
        return materials;
    }

    /** Materials displayed by the editor, including its most recent block sample. */
    public static List<ResourceLocation> selectableMaterials(ResourceLocation selected) {
        return selectableMaterials(selected, null);
    }

    /** Also retains every material already present on the edited cat. */
    public static List<ResourceLocation> selectableMaterials(ResourceLocation selected,
                                                             CatGenome genome) {
        // Do not put thousands of registry blocks into the debug editor. A
        // selected/mutated block material is retained when it is already on
        // this cat, while the random breeding pool remains registry-wide.
        Set<ResourceLocation> result = new LinkedHashSet<>(coreMaterials());
        if (selected != null) result.add(selected);
        if (genome != null) {
            for (CatRegion region : CatRegion.values()) {
                result.add(genome.material(region));
            }
        }
        return List.copyOf(result);
    }

    private static List<ResourceLocation> coreMaterials() {
        List<ResourceLocation> materials = coreMaterials;
        if (materials != null) return materials;
        synchronized (CatMaterialRegistry.class) {
            materials = coreMaterials;
            if (materials == null) {
                Set<ResourceLocation> result = new LinkedHashSet<>();
                BuiltInRegistries.CAT_VARIANT.keySet().stream()
                        .sorted(Comparator.comparing(ResourceLocation::toString))
                        .forEach(result::add);
                result.addAll(CUSTOM_MATERIALS);
                materials = List.copyOf(result);
                coreMaterials = materials;
            }
        }
        return materials;
    }

    public static ResourceLocation blockMaterial(ResourceLocation blockId) {
        return LaoWuMod.id(BLOCK_PREFIX + blockId.getNamespace() + "/" + blockId.getPath());
    }

    /** Returns a mapping only for visible, item-backed blocks. */
    public static Optional<ResourceLocation> blockMaterial(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return Optional.empty();
        return blockMaterial(blockItem.getBlock());
    }

    public static Optional<ResourceLocation> blockMaterial(Block block) {
        if (!isEligibleBlock(block)) return Optional.empty();
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        return id == null ? Optional.empty() : Optional.of(blockMaterial(id));
    }

    /**
     * A cached registry-wide pool used only for the rare non-terrain wild-cat
     * roll. It is built once after registries are frozen, never per spawn or
     * per JEI frame.
     */
    public static Optional<ResourceLocation> randomBlockMaterial(RandomSource random) {
        List<Block> blocks = mappedBlocks();
        if (blocks.isEmpty()) return Optional.empty();
        return blockMaterial(blocks.get(random.nextInt(blocks.size())));
    }

    private static List<Block> mappedBlocks() {
        List<Block> blocks = allMappedBlocks;
        if (blocks != null) return blocks;
        synchronized (CatMaterialRegistry.class) {
            blocks = allMappedBlocks;
            if (blocks == null) {
                blocks = BuiltInRegistries.BLOCK.stream()
                        .filter(CatMaterialRegistry::isEligibleBlock)
                        .toList();
                allMappedBlocks = blocks;
            }
        }
        return blocks;
    }

    public static boolean isEligibleBlock(Block block) {
        return block != null && block.asItem() != Items.AIR
                && block.asItem() instanceof BlockItem
                && block.defaultBlockState().getRenderShape() != RenderShape.INVISIBLE;
    }

    public static Optional<ResourceLocation> blockId(ResourceLocation material) {
        if (!LaoWuMod.MOD_ID.equals(material.getNamespace())
                || !material.getPath().startsWith(BLOCK_PREFIX)) return Optional.empty();
        String encoded = material.getPath().substring(BLOCK_PREFIX.length());
        int separator = encoded.indexOf('/');
        if (separator <= 0 || separator >= encoded.length() - 1) return Optional.empty();
        return Optional.ofNullable(ResourceLocation.tryBuild(
                encoded.substring(0, separator), encoded.substring(separator + 1)));
    }

    public static Component displayName(ResourceLocation material) {
        if (OBSIDIAN.equals(material)) return Component.translatable("material.laowu.obsidian");
        if (WOOD.equals(material)) return Component.translatable("material.laowu.wood");

        Optional<ResourceLocation> blockId = blockId(material);
        if (blockId.isPresent()) {
            Block block = BuiltInRegistries.BLOCK.get(blockId.get());
            if (block != null && BuiltInRegistries.BLOCK.getKey(block) != null) {
                return block.getName().copy().append(Component.translatable(
                        "material.laowu.block_suffix"));
            }
        }

        CatVariant variant = BuiltInRegistries.CAT_VARIANT.get(material);
        if (variant != null) {
            return Component.translatable("material.laowu.cat_variant."
                    + material.getNamespace() + "." + material.getPath().replace('/', '.'));
        }
        return Component.literal(material.toString());
    }

    private CatMaterialRegistry() {}
}
