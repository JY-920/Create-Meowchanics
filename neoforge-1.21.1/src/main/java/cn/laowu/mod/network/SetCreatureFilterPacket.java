package cn.laowu.mod.network;

import cn.laowu.mod.CreatureFilterMenu;
import cn.laowu.mod.item.CreatureFilterRules;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

import cn.laowu.mod.LaoWuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetCreatureFilterPacket(int containerId, CreatureFilterRules rules) implements CustomPacketPayload {
    public static final Type<SetCreatureFilterPacket> TYPE = new Type<>(LaoWuMod.id("set_creature_filter"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetCreatureFilterPacket> STREAM_CODEC =
            StreamCodec.of(SetCreatureFilterPacket::encode, SetCreatureFilterPacket::decode);
    private static void encode(RegistryFriendlyByteBuf buffer, SetCreatureFilterPacket packet) { write(buffer, packet); }
    private static SetCreatureFilterPacket decode(RegistryFriendlyByteBuf buffer) { return read(buffer); }
    public static void handle(SetCreatureFilterPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) packet.apply(player);
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public SetCreatureFilterPacket {
        if (containerId < 0 || containerId > 32767 || rules == null) throw new IllegalArgumentException("Invalid creature config");
    }
    public SetCreatureFilterPacket(int containerId, int mask, boolean blacklist, List<ResourceLocation> ids) {
        this(containerId, CreatureFilterRules.of(mask, blacklist ? CreatureFilterRules.Mode.BLACKLIST : CreatureFilterRules.Mode.WHITELIST, ids));
    }
    public int categoryMask() { return rules.categoryMask(); }
    public boolean blacklist() { return rules.mode() == CreatureFilterRules.Mode.BLACKLIST; }
    public List<ResourceLocation> entityIds() { return rules.entityIds(); }
    private static void write(FriendlyByteBuf buffer, SetCreatureFilterPacket packet) {
        if(packet.rules.isGrouped()) {
            buffer.writeVarInt(packet.containerId);buffer.writeVarInt(5);buffer.writeUtf(packet.rules.mode().id(),16);buffer.writeVarInt(packet.rules.groups().size());
            for(var g:packet.rules.groups()) {
                buffer.writeUtf(g.target()==null?"*":g.target().toString(),CreatureFilterRules.MAX_ID_LENGTH);
                buffer.writeUtf(g.category()==null?"":g.category().id(),16);
                buffer.writeBoolean(g.inverted());buffer.writeUtf(g.mode().id(),16);buffer.writeVarInt(g.conditions().size());
                for(var c:g.conditions()) {
                    buffer.writeUtf(c.attribute().id(),16);buffer.writeUtf(c.comparison().id(),4);
                    buffer.writeDouble(c.value());buffer.writeBoolean(c.inverted());
                }
            }
            buffer.writeBoolean(packet.rules.retainedLegacy()!=null);
            if(packet.rules.retainedLegacy()!=null)write(buffer,new SetCreatureFilterPacket(packet.containerId,packet.rules.retainedLegacy()));
            return;
        }
        buffer.writeVarInt(packet.containerId); buffer.writeVarInt(2);
        buffer.writeVarInt(packet.rules.categoryMask()); buffer.writeUtf(packet.rules.mode().id(), 16);
        buffer.writeBoolean(packet.rules.save().getBoolean("LegacyEmpty"));
        buffer.writeVarInt(packet.rules.conditions().size());
        for (var condition : packet.rules.conditions()) {
            buffer.writeBoolean(condition.category() != null);
            buffer.writeUtf(condition.category() != null ? condition.category().id() : condition.entityId().toString(), CreatureFilterRules.MAX_ID_LENGTH);
            buffer.writeBoolean(condition.inverted());
        }
    }
    private static SetCreatureFilterPacket read(FriendlyByteBuf buffer) {
        int menu = buffer.readVarInt();
        int version=buffer.readVarInt();
        if(version==3||version==4||version==5) {
            var globalMode=version==5?CreatureFilterRules.Mode.valueOf(buffer.readUtf(16).toUpperCase(Locale.ROOT)):CreatureFilterRules.Mode.WHITELIST;
            int count=buffer.readVarInt(),total=0;
            if(count<0||count>CreatureFilterRules.MAX_GROUPS)throw new IllegalArgumentException("Invalid group count");
            var groups=new ArrayList<CreatureFilterRules.Group>();
            for(int i=0;i<count;i++) {
                String target=buffer.readUtf(CreatureFilterRules.MAX_ID_LENGTH);
                var id=target.equals("*")?null:ResourceLocation.tryParse(target);
                if(!target.equals("*")&&id==null)throw new IllegalArgumentException("Invalid target");
                String category=version>=4?buffer.readUtf(16):"";
                boolean inverted=version==5&&buffer.readBoolean();
                var mode=CreatureFilterRules.Mode.valueOf(buffer.readUtf(16).toUpperCase(Locale.ROOT));
                int n=buffer.readVarInt();total+=n;
                if(n<0||n>CreatureFilterRules.MAX_CONDITIONS||total>CreatureFilterRules.MAX_CONDITIONS)
                    throw new IllegalArgumentException("Invalid attribute count");
                var cs=new ArrayList<CreatureFilterRules.AttributeCondition>();
                for(int j=0;j<n;j++)cs.add(new CreatureFilterRules.AttributeCondition(
                    CreatureFilterRules.Attribute.valueOf(buffer.readUtf(16).toUpperCase(Locale.ROOT)),
                    CreatureFilterRules.Comparison.valueOf(buffer.readUtf(4).toUpperCase(Locale.ROOT)),
                    buffer.readDouble(),buffer.readBoolean()));
                groups.add(new CreatureFilterRules.Group(id,category.isEmpty()?null:
                    CreatureFilterRules.Category.valueOf(category.toUpperCase(Locale.ROOT)),mode,cs,inverted));
            }
            var result=CreatureFilterRules.grouped(groups);
            if(buffer.readBoolean()) {
                // A legacy branch may only contain the fixed-depth v2 format.
                int reader=buffer.readerIndex();
                int legacyMenu=buffer.readVarInt(),legacyVersion=buffer.readVarInt();buffer.readerIndex(reader);
                if(legacyMenu!=menu||legacyVersion!=2)throw new IllegalArgumentException("Invalid retained branch");
                result=read(buffer).rules.withGroups(groups);
            }
            return new SetCreatureFilterPacket(menu,result.withEntryMode(globalMode));
        }
        if(version!=2)throw new IllegalArgumentException("Invalid creature config version");
        var tag = new net.minecraft.nbt.CompoundTag(); tag.putInt("Version", 2);
        tag.putInt("Categories", buffer.readVarInt()); tag.putString("Mode", buffer.readUtf(16));
        if (buffer.readBoolean()) tag.putBoolean("LegacyEmpty", true);
        int count = buffer.readVarInt();
        if (count < 0 || count > CreatureFilterRules.MAX_CONDITIONS) throw new IllegalArgumentException("Oversized creature list");
        var list = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < count; i++) {
            boolean category = buffer.readBoolean();
            var entry = new net.minecraft.nbt.CompoundTag();
            entry.putString(category ? "Category" : "EntityId", buffer.readUtf(CreatureFilterRules.MAX_ID_LENGTH));
            entry.putBoolean("Inverted", buffer.readBoolean()); list.add(entry);
        }
        tag.put("Conditions", list);
        return new SetCreatureFilterPacket(menu, CreatureFilterRules.parseData(tag));
    }
    public void apply(ServerPlayer player) {
        if (player == null || !player.isAlive() || player.isSpectator()
                || !(player.containerMenu instanceof CreatureFilterMenu menu)
                || menu.containerId != containerId || !menu.stillValid(player)) return;
        menu.configure(rules);
    }

}
