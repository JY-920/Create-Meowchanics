package cn.laowu.mod.create;
/** Common data contract; Create behaviours remain owned by the actual SmartBlockEntity. */
public interface BreedingBoxAccess {
    BreedingBoxData breedingData();
    default void tickCare() {}
    default void destroyCare() {}
}
