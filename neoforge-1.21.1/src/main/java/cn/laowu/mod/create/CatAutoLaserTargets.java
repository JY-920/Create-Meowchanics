package cn.laowu.mod.create;

import cn.laowu.mod.*;
import com.simibubi.create.content.contraptions.actors.seat.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.*;
import java.util.*;

/** Loaded-only station queries. Distances use block centers, never cat attributes. */
public final class CatAutoLaserTargets {
    public static boolean available(Cat cat) {
        return CatClothesData.getOutfit(cat)==CatOutfitType.TERMINATOR && CatSupportRules.canAssist(cat)
            && !DynamiteCatLastStand.isActive(cat) && !CatGiantMount.carried(cat)
            && seat(cat)!=null;
    }
    public static BlockPos seat(Cat cat) {
        if(!(cat.getVehicle() instanceof SeatEntity seat)||!seat.isAlive())return null;
        BlockPos p=seat.blockPosition();
        return cat.level().hasChunkAt(p)&&cat.level().getBlockState(p).getBlock() instanceof SeatBlock
            && seat.getPassengers().contains(cat)?p:null;
    }
    public static List<Cat> participants(ServerLevel level,BlockPos machine) {
        Vec3 center=Vec3.atCenterOf(machine);
        return level.getEntitiesOfClass(Cat.class,new AABB(machine).inflate(7),cat->{
            BlockPos seat=seat(cat);
            return available(cat)&&seat!=null&&Vec3.atCenterOf(seat).distanceToSqr(center)<=25;
        });
    }
    public static boolean validTarget(ServerLevel level,BlockPos machine,Cat cat,LivingEntity target) {
        if(!validMark(level,machine,target)||!(target instanceof Mob mob))return false;
        return level.getBlockEntity(machine) instanceof CatAutoLaserBlockEntity be && be.hasFilter()
            ?be.matchesFilter(mob)&&CatTeamRules.canHarm(cat,mob):CatAgentWatch.hostile(cat,mob);
    }
    public static boolean eligible(ServerLevel level,BlockPos machine,LivingEntity target,List<Cat> cats) {
        if(!(target instanceof Mob mob))return false;
        if(level.getBlockEntity(machine) instanceof CatAutoLaserBlockEntity be && be.hasFilter())
            return be.matchesFilter(mob)&&(cats.isEmpty()||cats.stream().anyMatch(cat->CatTeamRules.canHarm(cat,mob)));
        return cats.isEmpty()?(mob instanceof Enemy||mob.getType().getCategory()==MobCategory.MONSTER)
            :cats.stream().anyMatch(cat->CatAgentWatch.hostile(cat,mob));
    }
    public static boolean validMark(ServerLevel level,BlockPos machine,LivingEntity target) {
        return target!=null&&target.level()==level&&target.isAlive()&&!target.isRemoved()&&!target.isSpectator()
            && level.hasChunkAt(target.blockPosition())
            && !(target instanceof Cat c&&CatProfileData.isBeingViewed(c))
            && target.position().distanceToSqr(Vec3.atCenterOf(machine))<=576
            && visible(level,Vec3.atCenterOf(machine),target.getEyePosition(),machine)
            && (!(level.getBlockEntity(machine) instanceof CatAutoLaserBlockEntity be)
                || visible(level,CatAutoLaserAim.aim(be.getBlockState(),machine,be.getExtension(1),target.getEyePosition()).muzzle(),target.getEyePosition(),machine));
    }
    public static LivingEntity nearest(ServerLevel level,BlockPos machine,List<Cat> cats) {
        Vec3 center=Vec3.atCenterOf(machine);
        return level.getEntitiesOfClass(Mob.class,new AABB(machine).inflate(24),mob->
            validMark(level,machine,mob)&&eligible(level,machine,mob,cats))
            .stream().min(Comparator.<Mob>comparingDouble(mob->mob.position().distanceToSqr(center))
                .thenComparing(Entity::getUUID)).orElse(null);
    }
    /** Treat unloaded terrain as opaque. No raycast can acquire a chunk. */
    public static boolean visible(ServerLevel level,Vec3 from,Vec3 to,BlockPos ignoredMachine) {
        BlockGetter loaded=new BlockGetter() {
            @Override public BlockState getBlockState(BlockPos p) {
                if(p.equals(ignoredMachine))return Blocks.AIR.defaultBlockState();
                return level.hasChunkAt(p)?level.getBlockState(p):Blocks.BARRIER.defaultBlockState();
            }
            @Override public FluidState getFluidState(BlockPos p) {return level.hasChunkAt(p)?level.getFluidState(p):Fluids.EMPTY.defaultFluidState();}
            @Override public BlockEntity getBlockEntity(BlockPos p) {return level.hasChunkAt(p)?level.getBlockEntity(p):null;}
            @Override public int getHeight() {return level.getHeight();}
            @Override public int getMinBuildHeight() {return level.getMinBuildHeight();}
        };
        return loaded.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,net.minecraft.world.phys.shapes.CollisionContext.empty())).getType()==HitResult.Type.MISS;
    }
    private CatAutoLaserTargets() {}
}
