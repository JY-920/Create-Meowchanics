package cn.laowu.mod.network;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.CatClothesData;
import cn.laowu.mod.CatChestData;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private static final String VERSION = "4";

    public static void register(IEventBus modBus) {
        modBus.addListener(ModNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToClient(SyncCatPosePacket.TYPE, SyncCatPosePacket.STREAM_CODEC,
                SyncCatPosePacket::handle);
        registrar.playToClient(AudioSessionPacket.TYPE, AudioSessionPacket.STREAM_CODEC,
                AudioSessionPacket::handle);
        registrar.playToClient(SyncCatChestPacket.TYPE, SyncCatChestPacket.STREAM_CODEC,
                SyncCatChestPacket::handle);
        registrar.playToServer(SetCatAddressPacket.TYPE, SetCatAddressPacket.STREAM_CODEC,
                SetCatAddressPacket::handle);
        registrar.playToClient(LogisticsSoundPacket.TYPE, LogisticsSoundPacket.STREAM_CODEC,
                LogisticsSoundPacket::handle);
        registrar.playToClient(CatPackageLoadPacket.TYPE, CatPackageLoadPacket.STREAM_CODEC,
                CatPackageLoadPacket::handle);
        registrar.playToServer(CatArmorPouncePacket.TYPE, CatArmorPouncePacket.STREAM_CODEC,
                CatArmorPouncePacket::handle);
        registrar.playToClient(SyncCatClothesPacket.TYPE, SyncCatClothesPacket.STREAM_CODEC,
                SyncCatClothesPacket::handle);
        registrar.playToServer(ToggleCatToolEmpowerPacket.TYPE,
                ToggleCatToolEmpowerPacket.STREAM_CODEC, ToggleCatToolEmpowerPacket::handle);
        registrar.playToClient(CatTotemActivationPacket.TYPE,
                CatTotemActivationPacket.STREAM_CODEC, CatTotemActivationPacket::handle);
        registrar.playToClient(SyncCatTraitStatePacket.TYPE,
                SyncCatTraitStatePacket.STREAM_CODEC, SyncCatTraitStatePacket::handle);
        registrar.playToClient(SyncCatTraitsPacket.TYPE,
                SyncCatTraitsPacket.STREAM_CODEC, SyncCatTraitsPacket::handle);
        registrar.playToClient(SyncCatGenomePacket.TYPE,
                SyncCatGenomePacket.STREAM_CODEC, SyncCatGenomePacket::handle);
        registrar.playToClient(SyncCatAttributesPacket.TYPE,
                SyncCatAttributesPacket.STREAM_CODEC, SyncCatAttributesPacket::handle);
        registrar.playToServer(SetCatProfileNamePacket.TYPE,
                SetCatProfileNamePacket.STREAM_CODEC, SetCatProfileNamePacket::handle);
        registrar.playToClient(SyncDynamiteCatLastStandPacket.TYPE,
                SyncDynamiteCatLastStandPacket.STREAM_CODEC,
                SyncDynamiteCatLastStandPacket::handle);
        registrar.playToServer(SetCatFilterNamePacket.TYPE,
                SetCatFilterNamePacket.STREAM_CODEC, SetCatFilterNamePacket::handle);
    }

    public static void syncToTracking(Cat cat, int pose) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(cat, new SyncCatPosePacket(cat.getId(), pose));
    }

    public static void syncToPlayer(net.minecraft.server.level.ServerPlayer player, Cat cat, int pose) {
        PacketDistributor.sendToPlayer(player, new SyncCatPosePacket(cat.getId(), pose));
    }

    public static void setAudioSession(Cat cat, boolean active) {
        if (cat.getPersistentData().getBoolean("LaoWuAudioSession") == active) return;
        cat.getPersistentData().putBoolean("LaoWuAudioSession", active);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(cat, new AudioSessionPacket(cat.getId(), active));
    }

    public static void syncAudioToPlayer(net.minecraft.server.level.ServerPlayer player, Cat cat) {
        if (cat.getPersistentData().getBoolean("LaoWuAudioSession")) {
            PacketDistributor.sendToPlayer(player, new AudioSessionPacket(cat.getId(), true));
        }
    }

    public static void syncCatChestToTracking(Cat cat) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(cat,
                new SyncCatChestPacket(cat.getId(), CatChestData.hasChest(cat)));
    }

    public static void syncCatChestToPlayer(net.minecraft.server.level.ServerPlayer player, Cat cat) {
        PacketDistributor.sendToPlayer(player,
                new SyncCatChestPacket(cat.getId(), CatChestData.hasChest(cat)));
    }

    public static void syncCatClothesToTracking(Cat cat) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(cat,
                new SyncCatClothesPacket(cat.getId(), CatClothesData.getOutfit(cat).ordinal()));
    }

    public static void syncCatClothesToPlayer(net.minecraft.server.level.ServerPlayer player, Cat cat) {
        PacketDistributor.sendToPlayer(player,
                new SyncCatClothesPacket(cat.getId(), CatClothesData.getOutfit(cat).ordinal()));
    }

    public static void syncCatTraitStateToTracking(Cat cat) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(cat,
                traitState(cat));
    }

    public static void syncCatTraitStateToPlayer(
            net.minecraft.server.level.ServerPlayer player, Cat cat) {
        PacketDistributor.sendToPlayer(player, traitState(cat));
    }

    private static SyncCatTraitStatePacket traitState(Cat cat) {
        return new SyncCatTraitStatePacket(cat.getId(),
                cn.laowu.mod.genetics.CatTraitEffects.isBristlingRageActive(cat),
                cn.laowu.mod.genetics.CatTraitEffects.isLuBuOutnumbered(cat),
                cn.laowu.mod.genetics.CatTraitEffects.isTimidOutnumbered(cat),
                cn.laowu.mod.genetics.CatTraitEffects.isCombatActive(cat));
    }

    public static void syncCatGenomeToTracking(Cat cat) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(cat,
                new SyncCatGenomePacket(cat.getId(),
                        cn.laowu.mod.genetics.CatGenomeData.serialized(cat)));
    }

    public static void syncCatGenomeToPlayer(
            net.minecraft.server.level.ServerPlayer player, Cat cat) {
        PacketDistributor.sendToPlayer(player,
                new SyncCatGenomePacket(cat.getId(),
                        cn.laowu.mod.genetics.CatGenomeData.serialized(cat)));
    }

    public static void syncCatAttributesToTracking(Cat cat) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(cat,
                new SyncCatAttributesPacket(cat.getId(),
                        cn.laowu.mod.genetics.CatAttributeData.serialized(cat)));
    }

    public static void syncCatAttributesToPlayer(
            net.minecraft.server.level.ServerPlayer player, Cat cat) {
        PacketDistributor.sendToPlayer(player,
                new SyncCatAttributesPacket(cat.getId(),
                        cn.laowu.mod.genetics.CatAttributeData.serialized(cat)));
    }

    public static void syncCatTraitsToTracking(Cat cat) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(cat,
                new SyncCatTraitsPacket(cat.getId(),
                        cn.laowu.mod.genetics.CatTraitData.serialized(cat)));
    }

    public static void syncCatTraitsToPlayer(
            net.minecraft.server.level.ServerPlayer player, Cat cat) {
        PacketDistributor.sendToPlayer(player,
                new SyncCatTraitsPacket(cat.getId(),
                        cn.laowu.mod.genetics.CatTraitData.serialized(cat)));
    }

    public static void syncDynamiteLastStandToTracking(Cat cat) {
        syncDynamiteLastStandToTracking(cat,
                cn.laowu.mod.DynamiteCatLastStand.isActive(cat),
                cn.laowu.mod.DynamiteCatLastStand.fuseTicks(cat));
    }

    public static void syncDynamiteLastStandToTracking(
            Cat cat, boolean active, int fuseTicks) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(cat,
                new SyncDynamiteCatLastStandPacket(cat.getId(), active, fuseTicks));
    }

    public static void syncDynamiteLastStandToPlayer(
            net.minecraft.server.level.ServerPlayer player, Cat cat) {
        PacketDistributor.sendToPlayer(player,
                new SyncDynamiteCatLastStandPacket(cat.getId(),
                        cn.laowu.mod.DynamiteCatLastStand.isActive(cat),
                        cn.laowu.mod.DynamiteCatLastStand.fuseTicks(cat)));
    }

    public static void setCatAddress(int catId, String address) {
        PacketDistributor.sendToServer(new SetCatAddressPacket(catId, address));
    }

    public static void setCatProfileName(int catId, String name) {
        PacketDistributor.sendToServer(new SetCatProfileNamePacket(catId, name));
    }

    public static void setCatFilterName(int containerId, String name) {
        PacketDistributor.sendToServer(new SetCatFilterNamePacket(containerId, name));
    }

    public static void playLogisticsSound(ServerLevel level, BlockPos position, boolean arrival) {
        PacketDistributor.sendToPlayersInDimension(level,
                new LogisticsSoundPacket(position.getX() + 0.5D, position.getY() + 0.5D,
                        position.getZ() + 0.5D, arrival));
    }

    public static void startPackageLoadAnimation(ServerLevel level, int catId, BlockPos seat,
                                                 ItemStack parcel, int duration) {
        PacketDistributor.sendToPlayersInDimension(level,
                new CatPackageLoadPacket(catId, seat, parcel.copy(), duration));
    }

    public static void requestCatArmorPounce() {
        PacketDistributor.sendToServer(new CatArmorPouncePacket());
    }

    public static void requestCatToolEmpowerToggle() {
        PacketDistributor.sendToServer(new ToggleCatToolEmpowerPacket());
    }

    public static void showCatTotemActivation(net.minecraft.server.level.ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new CatTotemActivationPacket());
    }

    private ModNetwork() {}
}
