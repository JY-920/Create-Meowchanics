package cn.laowu.mod.create;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.CreatureFilterRules;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public final class CreatureTransmitterBlockEntity extends SmartBlockEntity {
    public static final int MAX_THRESHOLD = 4096;
    private int radius=8, lower=0, upper=1, count;
    private boolean active, inverted;
    public enum OutputMode { THRESHOLD, ANALOG }
    private OutputMode outputMode=OutputMode.THRESHOLD;
    private com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour filtering;
    public CreatureTransmitterBlockEntity(BlockPos pos,BlockState state) {
        super(CreatureTransmitterRegistration.ENTITY.get(),pos,state);setLazyTickRate(10);
    }
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        filtering=new com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour(this,new CreatureTransmitterFilterSlot()) {
            @Override public boolean canShortInteract(ItemStack stack) {
                return stack.isEmpty()||stack.is(LaoWuMod.CREATURE_FILTER.get());
            }
            @Override public boolean readFromClipboard(net.minecraft.core.HolderLookup.Provider registries,CompoundTag tag,net.minecraft.world.entity.player.Player player,
                    net.minecraft.core.Direction side,boolean simulate) {
                // Native paste returns the previous FilterItem before setFilter's predicate.
                // Reject incompatible input before that inventory side effect can occur.
                if(tag.contains("Filter")&&!canShortInteract(ItemStack.parseOptional(registries,tag.getCompound("Filter"))))return false;
                return super.readFromClipboard(registries,tag,player,side,simulate);
            }
        }.withPredicate(stack->stack.is(LaoWuMod.CREATURE_FILTER.get())).withCallback(stack->refresh());
        filtering.setLabel(net.minecraft.network.chat.Component.translatable("item.laowu.creature_filter"));
        behaviours.add(filtering);
    }
    @Override public void lazyTick() {super.lazyTick();refresh();}
    @Override public void initialize() {super.initialize();refresh();}
    public int getRadius(){return radius;}
    public int getLower(){return lower;}
    public int getUpper(){return upper;}
    public int getCount(){return count;}
    public boolean isInverted(){return inverted;}
    public boolean isActive(){return active;}
    public OutputMode getOutputMode(){return outputMode;}
    public int getSignal(){
        int normal=outputMode==OutputMode.ANALOG
            ?(int)Math.max(0,Math.min(15,15L*(count-lower)/(upper-lower)))
            :(active?15:0);
        return inverted?15-normal:normal;
    }
    public ItemStack getFilter(){return filtering.getFilter().copy();}
    public void configure(int radius,int lower,int upper,boolean inverted) {
        configure(radius,lower,upper,inverted,outputMode);
    }
    public void configure(int radius,int lower,int upper,boolean inverted,OutputMode mode) {
        int before=getSignal();
        this.radius=Mth.clamp(radius,1,16);
        this.lower=Mth.clamp(lower,0,MAX_THRESHOLD-1);
        this.upper=Mth.clamp(upper,this.lower+1,MAX_THRESHOLD);
        this.inverted=inverted;this.outputMode=mode==null?OutputMode.THRESHOLD:mode;
        refresh();changed(before);notifyUpdate();
    }
    public void setFilter(ItemStack stack) {
        filtering.setFilter(stack.is(LaoWuMod.CREATURE_FILTER.get())?stack.copyWithCount(1):ItemStack.EMPTY);
    }
    public ItemStack removeFilter(){var old=getFilter();filtering.setFilter(ItemStack.EMPTY);return old;}
    public void refresh() {
        if(level==null||level.isClientSide||isRemoved())return;
        int old=count,before=getSignal();
        var center=Vec3.atCenterOf(worldPosition);
        var filter=getFilter();
        var rules=filter.isEmpty()?null:CreatureFilterRules.read(filter);
        count=level.getEntitiesOfClass(Mob.class,new AABB(center,center).inflate(radius),
            mob->mob.isAlive()&&!mob.isSpectator()&&mob.distanceToSqr(center)<=radius*radius
                &&(rules==null||rules.matches(mob))).size();
        if(active&&count<=lower)active=false;
        else if(!active&&count>=upper)active=true;
        changed(before);
        if(old!=count||before!=getSignal())notifyUpdate();
    }
    private void changed(int before) {
        if(level!=null&&!level.isClientSide&&before!=getSignal())
            level.updateNeighborsAt(worldPosition,getBlockState().getBlock());
    }
    @Override protected void write(CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries,boolean clientPacket) {
        super.write(tag,registries,clientPacket);
        tag.putInt("Radius",radius);tag.putInt("Lower",lower);tag.putInt("Upper",upper);
        tag.putInt("Count",count);tag.putBoolean("Active",active);tag.putBoolean("Inverted",inverted);
        tag.putString("OutputMode",outputMode==OutputMode.ANALOG?"analog":"threshold");
    }
    @Override protected void read(CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries,boolean clientPacket) {
        if(!tag.contains("Filter")&&tag.contains("CreatureFilter")) {
            tag=tag.copy();tag.put("Filter",tag.get("CreatureFilter").copy());
        }
        super.read(tag,registries,clientPacket);
        radius=Mth.clamp(tag.contains("Radius")?tag.getInt("Radius"):8,1,16);
        lower=Mth.clamp(tag.getInt("Lower"),0,MAX_THRESHOLD-1);
        upper=Mth.clamp(tag.contains("Upper")?tag.getInt("Upper"):1,lower+1,MAX_THRESHOLD);
        count=Math.max(0,tag.getInt("Count"));active=tag.getBoolean("Active");inverted=tag.getBoolean("Inverted");
        outputMode=tag.getString("OutputMode").equals("analog")?OutputMode.ANALOG:OutputMode.THRESHOLD;
    }
}
