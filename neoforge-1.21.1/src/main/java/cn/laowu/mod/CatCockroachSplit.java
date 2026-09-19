package cn.laowu.mod;
import cn.laowu.mod.accessory.CatAccessories;
import cn.laowu.mod.genetics.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;

/** Genuine death only; two new UUIDs, half raw attributes, no equipment/inventory or temporary buffs. */
public final class CatCockroachSplit {
    private static final String HANDLED="LaoWuCockroachSplitHandled";
    public static boolean trySplit(Cat parent){return trySplit(parent,parent.getLastDamageSource());}
    public static boolean trySplit(Cat parent,net.minecraft.world.damagesource.DamageSource deathSource){
        if(!(parent.level() instanceof ServerLevel level) || parent.getHealth()>0 || !parent.isTame() || parent.getOwnerUUID()==null
                ||CatClothesData.getOutfit(parent)!=CatOutfitType.COCKROACH
                ||CatAccessories.value(parent,"cockroach_split")<=0||parent.getPersistentData().getBoolean(HANDLED))return false;
        // A new kitten has no target/retaliation memory of its own.
        var inherited=parent.getTarget();
        if(!validTarget(parent,inherited)) {
            var source=deathSource;
            inherited=source!=null&&source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker
                    ? attacker : parent.getLastHurtByMob();
        }
        if(!validTarget(parent,inherited))inherited=null;
        var genes=CatAttributeData.ensure(parent);
        var reduced=genes;
        for(CatStat stat:CatStat.values())reduced=reduced.withValues(stat,genes.current(stat)/2,genes.potential(stat));
        Cat[] children=new Cat[2];
        for(int i=0;i<2;i++){
            Cat child=EntityType.CAT.create(level);
            if(child==null)return false;
            child.setTame(true,false);child.setOwnerUUID(parent.getOwnerUUID());
            var collar=new net.minecraft.nbt.CompoundTag();child.addAdditionalSaveData(collar);
            collar.putByte("CollarColor",(byte)parent.getCollarColor().getId());child.readAdditionalSaveData(collar);
            child.setVariant(parent.getVariant());child.setAge(-24000);child.setPersistenceRequired();
            child.moveTo(parent.getX(),parent.getY(),parent.getZ(),parent.getYRot(),0);
            child.getPersistentData().putBoolean(CatClothesData.EQUIPPED_TAG,true);
            child.getPersistentData().putString(CatClothesData.OUTFIT_TAG,CatOutfitType.COCKROACH.id());
            CatTraitData.set(child,CatTraitProfile.EMPTY);
            CatGenomeData.set(child,CatGenomeData.getOrFallback(parent));
            CatAttributeData.set(child,reduced);
            if(parent.hasCustomName())child.setCustomName(parent.getCustomName().copy());
            child.setHealth(child.getMaxHealth());
            children[i]=child;
        }
        if(!level.addFreshEntity(children[0]))return false;
        if(!level.addFreshEntity(children[1])){children[0].discard();return false;}
        // Install career AI before assigning the target: equipment initialization may clear it.
        for(Cat child:children){
            CareerCatBehavior.tick(child);
            CatCombatControl.tick(child);
            if(validTarget(child,inherited)) {
                child.setOrderedToSit(false);child.setInSittingPose(false);
                child.setTarget(inherited);
                child.setLastHurtByMob(inherited);
                child.setAggressive(true);
            }
        }
        parent.getPersistentData().putBoolean(HANDLED,true);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,parent.getX(),parent.getY(.5),parent.getZ(),16,.35,.2,.35,.035);
        return true;
    }
    private static boolean validTarget(Cat cat,net.minecraft.world.entity.LivingEntity target){
        return target!=null&&target!=cat&&target.isAlive()&&!target.isRemoved()
                &&!target.isSpectator()&&target.level()==cat.level()&&CatTeamRules.canHarm(cat,target);
    }
    private CatCockroachSplit(){}
}
