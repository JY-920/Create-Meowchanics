package cn.laowu.mod.network;

import cn.laowu.mod.CatArmorPounceBehavior;
import cn.laowu.mod.LaoWuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client intent only; all equipment, target and cooldown checks run server-side. */
public record CatArmorPouncePacket() implements CustomPacketPayload {
    public static final Type<CatArmorPouncePacket> TYPE =
            new Type<>(LaoWuMod.id("cat_armor_pounce"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CatArmorPouncePacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> { }, buffer -> new CatArmorPouncePacket());

    public static void handle(CatArmorPouncePacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            CatArmorPounceBehavior.tryStart(player);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
