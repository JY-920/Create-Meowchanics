package cn.laowu.mod.test;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
@net.neoforged.neoforge.gametest.GameTestHolder("laowu")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public class CatEditorRegistrationProbe {
 @GameTest(template="accessory_probe",batch="cat_editor")
 public static void editorAndTransferTokenAvailable(GameTestHelper h){
  var id=ResourceLocation.fromNamespaceAndPath("laowu","cat_editor");
  h.assertTrue(BuiltInRegistries.BLOCK.containsKey(id)&&BuiltInRegistries.ITEM.containsKey(id),"Cat editor block/item must be available");
  h.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath("laowu","cat_trait_token")),"Trait token must be available");h.succeed();
 }
}
