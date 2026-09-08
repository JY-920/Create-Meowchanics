package cn.laowu.mod.genetics;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** A compact, versioned visible phenotype: one material id for every cat region. */
public final class CatGenome {
    public static final int DATA_VERSION = 2;
    public static final float DEBUG_MUTATION_CHANCE = 0.20F;
    private static final String VERSION_TAG = "Version";
    private static final String REGIONS_TAG = "Regions";

    private final Map<CatRegion, ResourceLocation> materials;

    private CatGenome(EnumMap<CatRegion, ResourceLocation> materials) {
        this.materials = Collections.unmodifiableMap(materials);
    }

    public static CatGenome uniform(ResourceLocation material) {
        Objects.requireNonNull(material, "material");
        EnumMap<CatRegion, ResourceLocation> values = new EnumMap<>(CatRegion.class);
        for (CatRegion region : CatRegion.values()) values.put(region, material);
        return new CatGenome(values);
    }

    public ResourceLocation material(CatRegion region) {
        return materials.get(region);
    }

    /** Returns a new phenotype with one semantic region replaced. */
    public CatGenome withMaterial(CatRegion region, ResourceLocation material) {
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(material, "material");
        EnumMap<CatRegion, ResourceLocation> values = new EnumMap<>(CatRegion.class);
        values.putAll(materials);
        values.put(region, material);
        return new CatGenome(values);
    }

    /** Returns a new phenotype using the same material for every region. */
    public CatGenome withUniformMaterial(ResourceLocation material) {
        return uniform(material);
    }

    public boolean isUniform() {
        ResourceLocation first = material(CatRegion.values()[0]);
        for (CatRegion region : CatRegion.values()) {
            if (!first.equals(material(region))) return false;
        }
        return true;
    }

    /** Stable cache key ordered by enum declaration, never by hash-map iteration. */
    public String phenotypeKey() {
        StringBuilder key = new StringBuilder(256);
        for (CatRegion region : CatRegion.values()) {
            if (!key.isEmpty()) key.append('|');
            key.append(material(region));
        }
        return key.toString();
    }

    public CompoundTag save() {
        CompoundTag root = new CompoundTag();
        root.putInt(VERSION_TAG, DATA_VERSION);
        CompoundTag regions = new CompoundTag();
        for (CatRegion region : CatRegion.values()) {
            regions.putString(region.serializedName(), material(region).toString());
        }
        root.put(REGIONS_TAG, regions);
        return root;
    }

    public static Optional<CatGenome> load(CompoundTag root) {
        if (root == null || !root.contains(REGIONS_TAG, Tag.TAG_COMPOUND)) return Optional.empty();
        int version = root.contains(VERSION_TAG, Tag.TAG_INT) ? root.getInt(VERSION_TAG) : 1;
        if (version < 1 || version > DATA_VERSION) return Optional.empty();

        CompoundTag regions = root.getCompound(REGIONS_TAG);
        EnumMap<CatRegion, ResourceLocation> values = new EnumMap<>(CatRegion.class);
        for (CatRegion region : CatRegion.values()) {
            // Version 1 used one shared "eyes" locus and an additional
            // body_middle locus. The revised map splits the eyes and folds the
            // middle body pixels into body_front/body_rear.
            String serializedName = version == 1
                    && (region == CatRegion.LEFT_EYE || region == CatRegion.RIGHT_EYE)
                    ? "eyes" : region.serializedName();
            ResourceLocation id = ResourceLocation.tryParse(regions.getString(serializedName));
            if (id == null) return Optional.empty();
            values.put(region, id);
        }
        return Optional.of(new CatGenome(values));
    }

    /**
     * Picks every region from either parent. Each region independently has a
     * 20% chance to mutate to a material used by neither parent in that region.
     */
    public static CatGenome fuse(CatGenome first, CatGenome second,
                                 List<ResourceLocation> availableMaterials,
                                 RandomSource random) {
        return fuse(first, second, availableMaterials, DEBUG_MUTATION_CHANCE, random);
    }

    /** Same region inheritance with a machine-defined mutation chance. */
    public static CatGenome fuse(CatGenome first, CatGenome second,
                                 List<ResourceLocation> availableMaterials,
                                 float mutationChance, RandomSource random) {
        EnumMap<CatRegion, ResourceLocation> child = new EnumMap<>(CatRegion.class);
        for (CatRegion region : CatRegion.values()) {
            ResourceLocation firstMaterial = first.material(region);
            ResourceLocation secondMaterial = second.material(region);
            ResourceLocation inherited = random.nextBoolean() ? firstMaterial : secondMaterial;

            if (random.nextFloat() < mutationChance) {
                ResourceLocation mutation = pickMutation(availableMaterials,
                        firstMaterial, secondMaterial, random);
                if (mutation != null) inherited = mutation;
            }
            child.put(region, inherited);
        }
        return new CatGenome(child);
    }

    /**
     * Avoids allocating and filtering the registry-wide block pool for every
     * one of the eleven regions. Random rejection is effectively constant-time
     * because only the two parent materials are excluded; the bounded scan is
     * a deterministic fallback for very small pools.
     */
    private static ResourceLocation pickMutation(List<ResourceLocation> materials,
                                                 ResourceLocation first,
                                                 ResourceLocation second,
                                                 RandomSource random) {
        if (materials.isEmpty()) return null;
        int retries = Math.min(12, materials.size());
        for (int attempt = 0; attempt < retries; attempt++) {
            ResourceLocation candidate = materials.get(random.nextInt(materials.size()));
            if (candidate != null && !candidate.equals(first) && !candidate.equals(second)) {
                return candidate;
            }
        }
        int start = random.nextInt(materials.size());
        for (int offset = 0; offset < materials.size(); offset++) {
            ResourceLocation candidate = materials.get((start + offset) % materials.size());
            if (candidate != null && !candidate.equals(first) && !candidate.equals(second)) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof CatGenome genome && materials.equals(genome.materials);
    }

    @Override
    public int hashCode() {
        return materials.hashCode();
    }
}
