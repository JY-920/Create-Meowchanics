package cn.laowu.mod.network;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.NaturalCatMaterialSpawner;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
public record SetNearbyCatSpawningPacket(boolean enabled) implements CustomPacketPayload {
    public static final Type<SetNearbyCatSpawningPacket> TYPE = new Type<>(LaoWuMod.id("nearby_cat_spawning"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetNearbyCatSpawningPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> buffer.writeBoolean(packet.enabled()),
                    buffer -> new SetNearbyCatSpawningPacket(buffer.readBoolean()));
    public static void handle(SetNearbyCatSpawningPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) player.getPersistentData().putBoolean(
                NaturalCatMaterialSpawner.DISABLED_FOR_PLAYER_TAG, !packet.enabled());
    }
    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
