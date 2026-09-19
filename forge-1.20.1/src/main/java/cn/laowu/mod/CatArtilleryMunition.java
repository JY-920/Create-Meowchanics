package cn.laowu.mod;

/** Stable firing cycle; no new item/entity registry IDs are required. */
public enum CatArtilleryMunition {
    SMALL_COG, LARGE_COG, SHAFT;
    public static CatArtilleryMunition select(net.minecraft.world.entity.animal.Cat cat) {
        return select(cn.laowu.mod.accessory.CatAccessories.value(cat,"engineering_special_ammo")>0,cat.getRandom().nextDouble());
    }
    public static CatArtilleryMunition select(boolean enabled,double roll) {
        return !enabled?SMALL_COG:roll<.2?LARGE_COG:roll<.4?SHAFT:SMALL_COG;
    }
    public float damageMultiplier(){return this==SHAFT?1.2F:1F;}
    public static CatArtilleryMunition shot(int sequence) { return values()[Math.floorMod(sequence, values().length)]; }
}
