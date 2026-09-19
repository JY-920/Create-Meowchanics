package cn.laowu.mod.accessory;

import cn.laowu.mod.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.ai.attributes.*;
import java.lang.ref.WeakReference;
import java.util.*;

/** Short-lived, strongest-source accessory buffs; no saved genes/effects or stacking modifiers. */
public final class CatAccessoryAuras {
    private static final CatVisualStates<Buffs> STATES=new CatVisualStates<>();
    private static final UUID MOVEMENT_ID=UUID.fromString("d60202f1-bd2e-49cc-8a95-67c995a793bc");
    private record Source(WeakReference<Cat> cat,long until,double amount) {}
    private static final class Buffs {
        final Map<UUID,Source> protection=new HashMap<>(),movement=new HashMap<>(),speed=new HashMap<>();
        int lastSpeedBonus;
    }
    private static Map<UUID,Source> sources(Buffs state,String key) {
        return switch(key) {
            case "medical_guard" -> state.protection;
            case "music_speed_bonus" -> state.speed;
            default -> state.movement; // Retained only for legacy data-pack percentage effects.
        };
    }
    private static void offer(Cat source,LivingEntity target,String key,int lifetime) {
        if(source.level().isClientSide || source.level()!=target.level() || !target.isAlive())return;
        double amount=CatAccessories.value(source,key);
        if(amount<=0)return;
        var state=STATES.getOrCreate(target,Buffs::new);
        var sources=sources(state,key);
        sources.put(source.getUUID(),new Source(new WeakReference<>(source),target.level().getGameTime()+lifetime,amount));
    }
    public static void protect(Cat healer,LivingEntity target){offer(healer,target,"medical_guard",10);}
    public static void accelerate(Cat musician,Cat target){
        offer(musician,target,"music_speed_bonus",CatMusicRules.BUFF_TICKS);
        offer(musician,target,"music_movement_bonus",CatMusicRules.BUFF_TICKS);
    }
    private static double strongest(LivingEntity target,String key) {
        var state=STATES.get(target);
        if(state==null)return 0;
        var sources=sources(state,key);
        double best=0;
        for(var it=sources.values().iterator();it.hasNext();){
            var offer=it.next();var source=offer.cat.get();
            if(source==null || !source.isAlive() || source.isRemoved() || source.level()!=target.level()
                    || offer.until<=target.level().getGameTime()
                    || CatAccessories.value(source,key)<=0
                    || key.equals("medical_guard")&&!CatMedicalHealing.casting(source)) {it.remove();continue;}
            best=Math.max(best,offer.amount);
        }
        return best;
    }
    public static float mitigate(LivingEntity target,DamageSource damage,float amount) {
        if(target.level().isClientSide || amount<=0 || damage.is(DamageTypeTags.BYPASSES_INVULNERABILITY))return amount;
        return (float)(amount*(1-Math.min(80,strongest(target,"medical_guard"))/100));
    }
    /** Flat, transient six-stat Speed bonus. Saved genes and the musician's own stats stay unchanged. */
    public static int speedBonus(Cat cat) {
        if(cat.level().isClientSide)
            return Math.max(0,Math.min(300,cat.getPersistentData().getCompound(CatAccessories.CLIENT_STATE).getInt("MusicSpeedBonus")));
        return CatMusicSupport.bonus(cat)>0 ? (int)strongest(cat,"music_speed_bonus") : 0;
    }
    public static void tick(Cat cat) {
        if(cat.level().isClientSide)return;
        var state=STATES.get(cat);
        int statBonus=speedBonus(cat);
        if(state!=null && state.lastSpeedBonus!=statBonus){
            state.lastSpeedBonus=statBonus;
            cn.laowu.mod.genetics.CatAttributeEffects.refresh(cat);
            CatAccessories.syncDynamic(cat);
        }
        // No built-in item uses this legacy percentage modifier after accessories.29.
        var speed=cat.getAttribute(Attributes.MOVEMENT_SPEED);
        if(speed==null)return;
        double bonus=CatMusicSupport.bonus(cat)>0?Math.min(100,strongest(cat,"music_movement_bonus"))/100:0;
        var old=speed.getModifier(MOVEMENT_ID);
        if(old!=null && Math.abs(old.getAmount()-bonus)<1e-8)return;
        if(old!=null)speed.removeModifier(MOVEMENT_ID);
        if(bonus>0)speed.addTransientModifier(new AttributeModifier(MOVEMENT_ID,"Music accessory stride",bonus,AttributeModifier.Operation.MULTIPLY_TOTAL));
    }
    private CatAccessoryAuras(){}
}
