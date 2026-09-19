package cn.laowu.mod.network;

import cn.laowu.mod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record MedicalHealingPacket(int entityId, java.util.UUID uuid, boolean casting, boolean aura, int age, float radius, boolean stationed) {
    public static void encode(MedicalHealingPacket p, FriendlyByteBuf b) {
        b.writeVarInt(p.entityId); b.writeUUID(p.uuid); b.writeBoolean(p.casting); b.writeBoolean(p.aura); b.writeVarInt(p.age); b.writeFloat(p.radius); b.writeBoolean(p.stationed);
    }
    public static MedicalHealingPacket decode(FriendlyByteBuf b) {
        return new MedicalHealingPacket(b.readVarInt(), b.readUUID(), b.readBoolean(), b.readBoolean(), b.readVarInt(), b.readFloat(), b.readBoolean());
    }
    public static void handle(MedicalHealingPacket p, Supplier<NetworkEvent.Context> source) {
        var context = source.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleMedical(p)));
        context.setPacketHandled(true);
    }
}
