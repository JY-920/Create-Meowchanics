package cn.laowu.mod.network;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.entity.CatGiantCarrier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Input only: the client never supplies a destination or velocity. */
public record GiantCatInputPacket(float forward,float side,float yaw,boolean jump,boolean sprint) implements CustomPacketPayload {
    public static final Type<GiantCatInputPacket> TYPE=new Type<>(LaoWuMod.id("giant_cat_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf,GiantCatInputPacket> STREAM_CODEC=StreamCodec.of(
            (b,p)->{b.writeFloat(p.forward);b.writeFloat(p.side);b.writeFloat(p.yaw);b.writeBoolean(p.jump);b.writeBoolean(p.sprint);},
            b->new GiantCatInputPacket(b.readFloat(),b.readFloat(),b.readFloat(),b.readBoolean(),b.readBoolean()));
    public static void handle(GiantCatInputPacket p,IPayloadContext context) {
        if (context.player() instanceof net.minecraft.server.level.ServerPlayer player
                && player.getVehicle() instanceof CatGiantCarrier carrier)
            carrier.input(player,p.forward,p.side,p.yaw,p.jump,p.sprint);
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
