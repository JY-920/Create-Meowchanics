package cn.laowu.mod.network;

import cn.laowu.mod.entity.CatGiantCarrier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Input only: the client never supplies a destination or velocity. */
public record GiantCatInputPacket(float forward,float side,float yaw,boolean jump,boolean sprint) {
    public static void encode(GiantCatInputPacket p,FriendlyByteBuf b) {
        b.writeFloat(p.forward);b.writeFloat(p.side);b.writeFloat(p.yaw);b.writeBoolean(p.jump);b.writeBoolean(p.sprint);
    }
    public static GiantCatInputPacket decode(FriendlyByteBuf b) {
        return new GiantCatInputPacket(b.readFloat(),b.readFloat(),b.readFloat(),b.readBoolean(),b.readBoolean());
    }
    public static void handle(GiantCatInputPacket p,Supplier<NetworkEvent.Context> source) {
        var context=source.get();
        context.enqueueWork(() -> {
            var player=context.getSender();
            if (player!=null && player.getVehicle() instanceof CatGiantCarrier carrier)
                carrier.input(player,p.forward,p.side,p.yaw,p.jump,p.sprint);
        });
        context.setPacketHandled(true);
    }
}
