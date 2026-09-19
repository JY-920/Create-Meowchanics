package cn.laowu.mod.accessory;

import cn.laowu.mod.CatClothesData;
import cn.laowu.mod.CatProfileData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;

/** Deterministic trigger cost: one accepted use is one point, not random Unbreaking wear. */
public final class CatAccessoryDurability {
    public static boolean spendEffect(Cat cat,String effect) {
        if(cat.level().isClientSide)return false;
        var inventory=CatProfileData.openContainer(cat);
        for(int slot=0;slot<CatProfileData.ACCESSORY_SLOTS;slot++){
            var stack=inventory.getItem(slot);
            var definition=CatAccessoryRegistry.find(stack,false);
            if(definition==null || !definition.activeFor(CatClothesData.getOutfit(cat).id())
                    || !CatAccessories.has(cat,definition.id()) || definition.value(effect)<=0)continue;
            return damage(cat,inventory,slot,stack,1);
        }
        return false;
    }
    /** Returns whether the item broke; custom nondurable items keep their original behaviour. */
    public static boolean damage(Cat cat,net.minecraft.world.Container inventory,int slot,ItemStack stack,int amount) {
        if(cat.level().isClientSide || amount<=0 || !stack.isDamageableItem())return false;
        long next=(long)stack.getDamageValue()+amount;
        if(next>=stack.getMaxDamage()){
            stack.shrink(1);
            if(!stack.isEmpty())stack.setDamageValue(0);
            inventory.setItem(slot,stack.isEmpty()?ItemStack.EMPTY:stack);
            cat.level().playSound(null,cat.getX(),cat.getY(),cat.getZ(),SoundEvents.ITEM_BREAK,SoundSource.NEUTRAL,.8F,1);
            inventory.setChanged();return true;
        }
        stack.setDamageValue((int)next);inventory.setChanged();return false;
    }
    private CatAccessoryDurability(){}
}
