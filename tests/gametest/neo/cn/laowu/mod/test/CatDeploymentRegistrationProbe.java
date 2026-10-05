package cn.laowu.mod.test;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class CatDeploymentRegistrationProbe {
    @GameTest(template="accessory_probe")
    public static void registeredPlatformsHaveBlockItems(GameTestHelper h) {
        for(String name:new String[]{"cat_deployment_platform","cat_ejecting_deployment_platform"}){
            var id=ResourceLocation.fromNamespaceAndPath("laowu",name);
            var block=BuiltInRegistries.BLOCK.get(id);
            h.assertTrue(block!=Blocks.AIR,"Missing deployment platform: "+name);
            h.assertTrue(block.asItem()==BuiltInRegistries.ITEM.get(id),"Deployment platform item missing: "+name);
        }
        h.succeed();
    }
}
