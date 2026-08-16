package cn.laowu.mod.network;

import cn.laowu.mod.client.ClientPacketHandler;
import cn.laowu.mod.LaoWuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Replaces the vanilla totem overlay with the Cat Totem for its owning player. */
public record CatTotemActivationPacket() implements CustomPacketPayload {
    public static final Type<CatTotemActivationPacket> TYPE =
            new Type<>(LaoWuMod.id("cat_totem_activation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CatTotemActivationPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> { }, buffer -> new CatTotemActivationPacket());

    public static void handle(CatTotemActivationPacket packet, IPayloadContext context) {
        ClientPacketHandler.handleCatTotemActivation();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
