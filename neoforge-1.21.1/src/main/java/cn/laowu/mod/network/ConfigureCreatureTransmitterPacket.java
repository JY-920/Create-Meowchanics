package cn.laowu.mod.network;
import cn.laowu.mod.LaoWuMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
public record ConfigureCreatureTransmitterPacket(BlockPos pos,int radius,int lower,int upper,boolean inverted,boolean analog,boolean remove) implements CustomPacketPayload {
    public ConfigureCreatureTransmitterPacket(BlockPos pos,int radius,int lower,int upper,boolean inverted){
        this(pos,radius,lower,upper,inverted,false,false);
    }
    public static ConfigureCreatureTransmitterPacket removeFilter(BlockPos pos){
        return new ConfigureCreatureTransmitterPacket(pos,0,0,1,false,false,true);
    }
    public static final Type<ConfigureCreatureTransmitterPacket> TYPE=new Type<>(LaoWuMod.id("configure_creature_transmitter"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ConfigureCreatureTransmitterPacket> STREAM_CODEC=StreamCodec.of(ConfigureCreatureTransmitterPacket::encode,ConfigureCreatureTransmitterPacket::decode);
    private static void encode(RegistryFriendlyByteBuf b,ConfigureCreatureTransmitterPacket p){b.writeBlockPos(p.pos);b.writeInt(p.radius);b.writeInt(p.lower);b.writeInt(p.upper);b.writeBoolean(p.inverted);b.writeBoolean(p.analog);b.writeBoolean(p.remove);}
    private static ConfigureCreatureTransmitterPacket decode(RegistryFriendlyByteBuf b){return new ConfigureCreatureTransmitterPacket(b.readBlockPos(),b.readInt(),b.readInt(),b.readInt(),b.readBoolean(),b.readBoolean(),b.readBoolean());}
    public static void handle(ConfigureCreatureTransmitterPacket p,IPayloadContext c){if(c.player() instanceof net.minecraft.server.level.ServerPlayer player)p.apply(player);}
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public void apply(net.minecraft.server.level.ServerPlayer player) {
        if (!cn.laowu.mod.compat.create.CreateIntegration.isLoaded()) return;
        if(player==null||!player.isAlive()||player.isSpectator()||!player.mayBuild()
            ||player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))>64
            ||!player.serverLevel().hasChunkAt(pos)||!player.level().mayInteract(player,pos))return;
        if(player.serverLevel().getBlockEntity(pos) instanceof cn.laowu.mod.create.CreatureTransmitterBlockEntity be) {
            if(remove) {
                var filter=be.removeFilter();
                if(!filter.isEmpty()&&!player.getInventory().add(filter))player.drop(filter,false);
                be.refresh();be.notifyUpdate();
            } else be.configure(radius,lower,upper,inverted,analog
                ?cn.laowu.mod.create.CreatureTransmitterBlockEntity.OutputMode.ANALOG
                :cn.laowu.mod.create.CreatureTransmitterBlockEntity.OutputMode.THRESHOLD);
        }
    }
}
