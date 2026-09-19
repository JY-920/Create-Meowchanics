package cn.laowu.mod.network;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.client.ClientPacketHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MedicalHealingPacket(int entityId, java.util.UUID uuid, boolean casting, boolean aura, int age, float radius, boolean stationed) implements CustomPacketPayload {
    public static final Type<MedicalHealingPacket> TYPE = new Type<>(LaoWuMod.id("medical_healing"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MedicalHealingPacket> STREAM_CODEC = StreamCodec.of(
            (b, p) -> { b.writeVarInt(p.entityId); b.writeUUID(p.uuid); b.writeBoolean(p.casting); b.writeBoolean(p.aura); b.writeVarInt(p.age); b.writeFloat(p.radius); b.writeBoolean(p.stationed); },
            b -> new MedicalHealingPacket(b.readVarInt(), b.readUUID(), b.readBoolean(), b.readBoolean(), b.readVarInt(), b.readFloat(), b.readBoolean()));
    public static void handle(MedicalHealingPacket p, IPayloadContext context) { ClientPacketHandler.handleMedical(p); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
