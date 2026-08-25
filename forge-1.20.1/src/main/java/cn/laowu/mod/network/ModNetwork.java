package cn.laowu.mod.network;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.CatClothesData;
import cn.laowu.mod.CatChestData;
import cn.laowu.mod.genetics.CatAttributeData;
import cn.laowu.mod.genetics.CatGenomeData;
import cn.laowu.mod.genetics.CatTraitData;
import cn.laowu.mod.genetics.CatTraitEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String VERSION = "9";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(LaoWuMod.MOD_ID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        CHANNEL.registerMessage(0, SyncCatPosePacket.class, SyncCatPosePacket::encode,
                SyncCatPosePacket::decode, SyncCatPosePacket::handle);
        CHANNEL.registerMessage(1, AudioSessionPacket.class, AudioSessionPacket::encode,
                AudioSessionPacket::decode, AudioSessionPacket::handle);
        CHANNEL.registerMessage(2, SyncCatChestPacket.class, SyncCatChestPacket::encode,
                SyncCatChestPacket::decode, SyncCatChestPacket::handle);
        CHANNEL.registerMessage(3, SetCatAddressPacket.class, SetCatAddressPacket::encode,
                SetCatAddressPacket::decode, SetCatAddressPacket::handle);
        CHANNEL.registerMessage(4, LogisticsSoundPacket.class, LogisticsSoundPacket::encode,
                LogisticsSoundPacket::decode, LogisticsSoundPacket::handle);
        CHANNEL.registerMessage(5, CatPackageLoadPacket.class, CatPackageLoadPacket::encode,
                CatPackageLoadPacket::decode, CatPackageLoadPacket::handle);
        CHANNEL.registerMessage(6, CatArmorPouncePacket.class, CatArmorPouncePacket::encode,
                CatArmorPouncePacket::decode, CatArmorPouncePacket::handle);
        CHANNEL.registerMessage(7, SyncCatClothesPacket.class, SyncCatClothesPacket::encode,
                SyncCatClothesPacket::decode, SyncCatClothesPacket::handle);
        CHANNEL.registerMessage(8, ToggleCatToolEmpowerPacket.class, ToggleCatToolEmpowerPacket::encode,
                ToggleCatToolEmpowerPacket::decode, ToggleCatToolEmpowerPacket::handle);
        CHANNEL.registerMessage(9, CatTotemActivationPacket.class, CatTotemActivationPacket::encode,
                CatTotemActivationPacket::decode, CatTotemActivationPacket::handle);
        CHANNEL.registerMessage(10, SyncCatGenomePacket.class, SyncCatGenomePacket::encode,
                SyncCatGenomePacket::decode, SyncCatGenomePacket::handle);
        CHANNEL.registerMessage(11, SyncCatAttributesPacket.class, SyncCatAttributesPacket::encode,
                SyncCatAttributesPacket::decode, SyncCatAttributesPacket::handle);
        CHANNEL.registerMessage(12, SyncCatTraitsPacket.class, SyncCatTraitsPacket::encode,
                SyncCatTraitsPacket::decode, SyncCatTraitsPacket::handle);
        CHANNEL.registerMessage(13, SyncCatTraitStatePacket.class,
                SyncCatTraitStatePacket::encode, SyncCatTraitStatePacket::decode,
                SyncCatTraitStatePacket::handle);
        CHANNEL.registerMessage(14, SetCatProfileNamePacket.class,
                SetCatProfileNamePacket::encode, SetCatProfileNamePacket::decode,
                SetCatProfileNamePacket::handle);
    }

    public static void syncToTracking(Cat cat, int pose) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> cat),
                new SyncCatPosePacket(cat.getId(), pose));
    }

    public static void syncToPlayer(net.minecraft.server.level.ServerPlayer player, Cat cat, int pose) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncCatPosePacket(cat.getId(), pose));
    }

    public static void setAudioSession(Cat cat, boolean active) {
        if (cat.getPersistentData().getBoolean("LaoWuAudioSession") == active) return;
        cat.getPersistentData().putBoolean("LaoWuAudioSession", active);
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> cat),
                new AudioSessionPacket(cat.getId(), active));
    }

    public static void syncAudioToPlayer(net.minecraft.server.level.ServerPlayer player, Cat cat) {
        if (cat.getPersistentData().getBoolean("LaoWuAudioSession")) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new AudioSessionPacket(cat.getId(), true));
        }
    }

    public static void syncCatChestToTracking(Cat cat) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> cat),
                new SyncCatChestPacket(cat.getId(), CatChestData.hasChest(cat)));
    }

    public static void syncCatChestToPlayer(net.minecraft.server.level.ServerPlayer player, Cat cat) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncCatChestPacket(cat.getId(), CatChestData.hasChest(cat)));
    }

    public static void syncCatClothesToTracking(Cat cat) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> cat),
                new SyncCatClothesPacket(cat.getId(), CatClothesData.getOutfit(cat).ordinal()));
    }

    public static void syncCatClothesToPlayer(net.minecraft.server.level.ServerPlayer player, Cat cat) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncCatClothesPacket(cat.getId(), CatClothesData.getOutfit(cat).ordinal()));
    }

    public static void syncCatGenomeToTracking(Cat cat) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> cat),
                new SyncCatGenomePacket(cat.getId(), CatGenomeData.serialized(cat)));
    }

    public static void syncCatGenomeToPlayer(net.minecraft.server.level.ServerPlayer player, Cat cat) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncCatGenomePacket(cat.getId(), CatGenomeData.serialized(cat)));
    }

    public static void syncCatAttributesToTracking(Cat cat) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> cat),
                new SyncCatAttributesPacket(cat.getId(), CatAttributeData.serialized(cat)));
    }

    public static void syncCatAttributesToPlayer(net.minecraft.server.level.ServerPlayer player, Cat cat) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncCatAttributesPacket(cat.getId(), CatAttributeData.serialized(cat)));
    }

    public static void syncCatTraitsToTracking(Cat cat) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> cat),
                new SyncCatTraitsPacket(cat.getId(), CatTraitData.serialized(cat)));
    }

    public static void syncCatTraitsToPlayer(net.minecraft.server.level.ServerPlayer player, Cat cat) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncCatTraitsPacket(cat.getId(), CatTraitData.serialized(cat)));
    }

    public static void syncCatTraitStateToTracking(Cat cat) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> cat),
                traitState(cat));
    }

    public static void syncCatTraitStateToPlayer(
            net.minecraft.server.level.ServerPlayer player, Cat cat) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), traitState(cat));
    }

    private static SyncCatTraitStatePacket traitState(Cat cat) {
        return new SyncCatTraitStatePacket(cat.getId(),
                CatTraitEffects.isBristlingRageActive(cat),
                CatTraitEffects.isLuBuOutnumbered(cat),
                CatTraitEffects.isTimidOutnumbered(cat));
    }

    public static void setCatAddress(int catId, String address) {
        CHANNEL.sendToServer(new SetCatAddressPacket(catId, address));
    }

    public static void setCatProfileName(int catId, String name) {
        CHANNEL.sendToServer(new SetCatProfileNamePacket(catId, name));
    }

    public static void playLogisticsSound(ServerLevel level, BlockPos position, boolean arrival) {
        CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension),
                new LogisticsSoundPacket(position.getX() + 0.5D, position.getY() + 0.5D,
                        position.getZ() + 0.5D, arrival));
    }

    public static void startPackageLoadAnimation(ServerLevel level, int catId, BlockPos seat,
                                                 ItemStack parcel, int duration) {
        CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension),
                new CatPackageLoadPacket(catId, seat, parcel.copy(), duration));
    }

    public static void requestCatArmorPounce() {
        CHANNEL.sendToServer(new CatArmorPouncePacket());
    }

    public static void requestCatToolEmpowerToggle() {
        CHANNEL.sendToServer(new ToggleCatToolEmpowerPacket());
    }

    public static void showCatTotemActivation(net.minecraft.server.level.ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CatTotemActivationPacket());
    }

    private ModNetwork() {}
}
