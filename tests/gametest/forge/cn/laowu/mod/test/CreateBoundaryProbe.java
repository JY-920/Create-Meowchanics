package cn.laowu.mod.test;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.compat.create.CreateIntegration;
import cn.laowu.mod.item.CatTotemItem;
import cn.laowu.mod.mixin.CreateOptionalMixinPlugin;
import com.simibubi.create.content.kinetics.deployer.DeployerRecipeSearchEvent;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import net.minecraftforge.gametest.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class CreateBoundaryProbe {
    @GameTest(template="artillery_probe",timeoutTicks=20)
    public static void installedCreateKeepsConditionalHooks(GameTestHelper h) {
        h.assertTrue(CreateIntegration.isLoaded(),"Installed Create must be detected during actual startup");
        h.assertTrue(new CreateOptionalMixinPlugin().shouldApplyMixin("com.simibubi.create.content.kinetics.belt.BeltBlock","cn.laowu.mod.mixin.CatBeltCasingMixin"),"Installed Create must retain its mixins");
        ItemStack totem=new ItemStack(LaoWuMod.CAT_TOTEM.get());
        while(CatTotemItem.canLoad(totem))CatTotemItem.addCharge(totem);
        var items=new ItemStackHandler(2);items.setStackInSlot(0,totem);items.setStackInSlot(1,new ItemStack(Items.TOTEM_OF_UNDYING));
        var event=new DeployerRecipeSearchEvent(null,new RecipeWrapper(items));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        h.assertTrue(event.isCanceled(),"Conditional Create handler must still reject charging a full cat totem");
        h.assertTrue(CatTotemItem.charges(items.getStackInSlot(0))==CatTotemItem.charges(totem),"Rejected processing must keep saved charge data");
        h.succeed();
    }
}

