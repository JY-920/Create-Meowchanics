package cn.laowu.mod.create;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.CatPancakeItem;
import com.simibubi.create.content.logistics.depot.EntityLauncher;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.*;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.*;

/** Persistent fuel/input with success-only conversion. No world chunk loading. */
public final class CatDeploymentBlockEntity extends BlockEntity
        implements com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation {
    public static final int CAPACITY=4000, COST=250;
    private BlockPos targetOffset;
    private int animationTicks;
    private boolean loading,deploying;
    public final FluidTank tank=new FluidTank(CAPACITY,f->f.getFluid().isSame(LaoWuMod.HISSING_GAS.get())){
        @Override protected void onContentsChanged(){changed();}
    };
    public final ItemStackHandler inventory=new ItemStackHandler(1){
        @Override public int getSlotLimit(int slot){return 1;}
        @Override public boolean isItemValid(int slot,ItemStack stack){return stack.is(LaoWuMod.CAT_PANCAKE.get());}
        @Override protected void onContentsChanged(int slot){changed();}
    };
    public CatDeploymentBlockEntity(BlockPos pos,BlockState state){super(LaoWuMod.CAT_DEPLOYMENT_BE.get(),pos,state);}
    public boolean ejecting(){return getBlockState().getBlock() instanceof CatDeploymentBlock b&&b.ejecting;}
    public ItemStack catStack(){return inventory.getStackInSlot(0);}
    public IFluidHandler getFluidHandler(Direction side){return tank;}
    public IItemHandler getItemHandler(Direction side){return inventory;}
    public static boolean validTarget(BlockPos source,BlockPos target){
        long dx=(long)target.getX()-source.getX(),dy=(long)target.getY()-source.getY(),dz=(long)target.getZ()-source.getZ();
        int max=AllConfigs.server().kinetics.maxEjectorDistance.get();
        return (dx==0 ^ dz==0)&&Math.abs(dx)<=max&&Math.abs(dz)<=max&&Math.abs(dy)<=max;
    }
    public boolean setTarget(BlockPos target){
        if(!ejecting()||target==null||!validTarget(worldPosition,target))return false;
        targetOffset=target.subtract(worldPosition);changed();return true;
    }
    public BlockPos target(){return targetOffset==null?null:worldPosition.offset(targetOffset);}
    public float launchProgress(float partial){
        if(animationTicks<=0)return 0;
        // Begin visibly lifted on the launch tick, then settle over20ticks.
        return (float)Math.sin(Math.PI*Math.min(1,(21-animationTicks+partial)/21f));
    }
    private void changed(){
        if(loading)return;
        setChanged();
        if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    public InteractionResult interact(Player player,InteractionHand hand){
        if(level==null||player.isShiftKeyDown())return InteractionResult.PASS;
        ItemStack held=player.getItemInHand(hand);
        if(level.isClientSide&&!held.isEmpty()){
            if(FluidUtil.tryFillContainer(held,tank,Integer.MAX_VALUE,null,false).isSuccess()
                    ||FluidUtil.tryEmptyContainer(held,tank,Integer.MAX_VALUE,null,false).isSuccess())return InteractionResult.SUCCESS;
        }
        if(!level.isClientSide&&FluidUtil.interactWithFluidHandler(player,hand,tank)){
            if(!level.isClientSide)tryDeploy();
            return InteractionResult.SUCCESS;
        }
        if(held.isEmpty()&&!catStack().isEmpty()){
            if(!level.isClientSide){
                ItemStack taken=inventory.extractItem(0,1,false);
                if(!player.getInventory().add(taken))player.drop(taken,false);
            }
            return InteractionResult.SUCCESS;
        }
        if(!held.is(LaoWuMod.CAT_PANCAKE.get()))return InteractionResult.PASS;
        if(!level.isClientSide&&catStack().isEmpty()){
            inventory.setStackInSlot(0,held.copyWithCount(1));held.shrink(1);tryDeploy();
        }
        return InteractionResult.SUCCESS;
    }
    public boolean tryDeploy(){
        if(!(level instanceof ServerLevel server)||deploying||catStack().isEmpty()
                ||tank.getFluidAmount()<COST||!tank.getFluid().getFluid().isSame(LaoWuMod.HISSING_GAS.get())
                ||(ejecting()&&animationTicks>0))return false;
        BlockPos destination=target();
        if(ejecting()&&(destination==null||!validTarget(worldPosition,destination)
                ||!server.hasChunkAt(destination)||!server.getWorldBorder().isWithinBounds(destination)))return false;
        deploying=true;
        try {
            Vec3 position=new Vec3(worldPosition.getX()+.5,worldPosition.getY()+(ejecting()?1.01:13/16d+.01),worldPosition.getZ()+.5);
            var cat=CatPancakeItem.deployCat(server,catStack().copyWithCount(1),position,getBlockState().getValue(CatDeploymentBlock.FACING).toYRot(),
                    ejecting()?Vec3.atBottomCenterOf(destination.above()):null);
            if(cat==null)return false;
            inventory.extractItem(0,1,false);
            tank.drain(COST,IFluidHandler.FluidAction.EXECUTE);
            if(ejecting()){
                int dx=destination.getX()-worldPosition.getX(),dz=destination.getZ()-worldPosition.getZ();
                var launcher=new EntityLauncher(Math.abs(dx)+Math.abs(dz),destination.getY()-worldPosition.getY());
                cat.getNavigation().stop();cat.setOnGround(false);
                launcher.applyMotion(cat,Direction.getNearest(dx,0,dz));
                CatDeploymentFlight.begin(cat);
                cat.hurtMarked=true;cat.fallDistance=0;
                animationTicks=20;
            }
            changed();return true;
        }finally{deploying=false;}
    }
    public static void tick(Level level,BlockPos pos,BlockState state,CatDeploymentBlockEntity be){
        if(be.animationTicks>0)be.animationTicks--;
        if(level.isClientSide)return;
        if(be.catStack().isEmpty()){
            double top=be.ejecting()?14/16d:13/16d;
            var area=new AABB(pos.getX()+.03,pos.getY()+top-.02,pos.getZ()+.03,pos.getX()+.97,pos.getY()+top+.35,pos.getZ()+.97);
            for(var item:level.getEntitiesOfClass(ItemEntity.class,area,e->e.isAlive()&&e.getItem().is(LaoWuMod.CAT_PANCAKE.get()))){
                var stack=item.getItem();
                be.inventory.setStackInSlot(0,stack.copyWithCount(1));stack.shrink(1);
                if(stack.isEmpty())item.discard();else item.setItem(stack);
                break;
            }
        }
        be.tryDeploy();
    }
    @Override public boolean addToGoggleTooltip(java.util.List<Component> lines,boolean sneaking){
        lines.add(Component.translatable(getBlockState().getBlock().getDescriptionId()));
        lines.add(Component.translatable("tooltip.laowu.cat_carrier.gas",tank.getFluidAmount(),CAPACITY));
        if(ejecting())lines.add(target()==null?Component.translatable("tooltip.laowu.cat_deployment.no_target"):
            Component.translatable("tooltip.laowu.cat_deployment.target",target().getX(),target().getY(),target().getZ()));
        return true;
    }
    public ItemStack portableStack(){
        ItemStack stack=new ItemStack(getBlockState().getBlock());
        saveToItem(stack,level.registryAccess());
        return stack;
    }
    @Override protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);
        tag.put("Tank",tank.writeToNBT(registries,new CompoundTag()));
        tag.put("Inventory",inventory.serializeNBT(registries));
        if(targetOffset!=null)tag.putIntArray("TargetOffset",new int[]{targetOffset.getX(),targetOffset.getY(),targetOffset.getZ()});
        tag.putInt("Animation",animationTicks);
    }
    @Override public void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);loading=true;
        try{
            tank.readFromNBT(registries,tag.getCompound("Tank"));
            if(!tank.isEmpty()&&!tank.getFluid().getFluid().isSame(LaoWuMod.HISSING_GAS.get()))tank.setFluid(FluidStack.EMPTY);
            if(tank.getFluidAmount()>CAPACITY)tank.getFluid().setAmount(CAPACITY);
            inventory.deserializeNBT(registries,tag.getCompound("Inventory"));
            if(inventory.getSlots()!=1)inventory.setSize(1);
            if(!catStack().isEmpty())inventory.setStackInSlot(0,catStack().is(LaoWuMod.CAT_PANCAKE.get())?catStack().copyWithCount(1):ItemStack.EMPTY);
            int[] offset=tag.getIntArray("TargetOffset");
            targetOffset=offset.length==3?new BlockPos(offset[0],offset[1],offset[2]):null;
            if(targetOffset!=null&&!validTarget(BlockPos.ZERO,targetOffset))targetOffset=null;
            animationTicks=level!=null&&level.isClientSide?Math.max(0,Math.min(20,tag.getInt("Animation"))):0;
        }finally{loading=false;}
    }
    @Override public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries){CompoundTag tag=new CompoundTag();saveAdditional(tag,registries);return tag;}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}

}
