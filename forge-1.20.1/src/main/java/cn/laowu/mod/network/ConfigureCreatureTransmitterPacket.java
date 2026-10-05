package cn.laowu.mod.network;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public record ConfigureCreatureTransmitterPacket(BlockPos pos,int radius,int lower,int upper,boolean inverted,boolean analog,boolean remove) {
    public ConfigureCreatureTransmitterPacket(BlockPos pos,int radius,int lower,int upper,boolean inverted){
        this(pos,radius,lower,upper,inverted,false,false);
    }
    public static ConfigureCreatureTransmitterPacket removeFilter(BlockPos pos){
        return new ConfigureCreatureTransmitterPacket(pos,0,0,1,false,false,true);
    }
    public static void encode(ConfigureCreatureTransmitterPacket p,FriendlyByteBuf b){b.writeBlockPos(p.pos);b.writeInt(p.radius);b.writeInt(p.lower);b.writeInt(p.upper);b.writeBoolean(p.inverted);b.writeBoolean(p.analog);b.writeBoolean(p.remove);}
    public static ConfigureCreatureTransmitterPacket decode(FriendlyByteBuf b){return new ConfigureCreatureTransmitterPacket(b.readBlockPos(),b.readInt(),b.readInt(),b.readInt(),b.readBoolean(),b.readBoolean(),b.readBoolean());}
    public static void handle(ConfigureCreatureTransmitterPacket p,Supplier<NetworkEvent.Context> s){var c=s.get();c.enqueueWork(()->p.apply(c.getSender()));c.setPacketHandled(true);}
    public void apply(net.minecraft.server.level.ServerPlayer player) {
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
