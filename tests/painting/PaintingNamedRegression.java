import cn.laowu.mod.painting.*;
import java.util.*;

public final class PaintingNamedRegression {
 public static void main(String[] args){
  var cream=PaintingBuiltinCatalog.named("奶油").orElseThrow();
  var millet=PaintingBuiltinCatalog.named("小米").orElseThrow();
  check(cream.id().equals("naiyou")&&millet.id().equals("xiaomi"),"Each name selects its own supplied photo");
  check(!cream.hash().equals(millet.hash()),"Photos have distinct saved identities");
  check(cream.spec().equals(new PaintingSpec(3,2,PaintingSpec.Mode.ORIGINAL,PaintingSpec.Crop.KEEP_HEIGHT)),"Landscape photo fills the frame");
  check(millet.spec().equals(new PaintingSpec(3,4,PaintingSpec.Mode.ORIGINAL,PaintingSpec.Crop.KEEP_HEIGHT)),"Portrait photo keeps its full height");
  for(var e:List.of(cream,millet)){
   check(e.namedOnly(),"Named pictures are explicitly excluded from random placement");
   check(PaintingBuiltinCatalog.named("《"+e.triggerName()+"》").orElseThrow().equals(e),"Book-title brackets are optional");
   check(PaintingBuiltinCatalog.named(" "+e.triggerName()+" ").orElseThrow().equals(e),"Surrounding whitespace is harmless");
   check(PaintingBuiltinCatalog.find(e.hash()).orElseThrow().equals(e),"Saved named artwork resolves without uploading");
   check(PaintingBuiltinCatalog.entries().stream().noneMatch(r->r.hash().equals(e.hash())),"Named image cannot enter the random pool");
   check(e.acceptsSavedSpec(e.spec()),"Named picture survives save/load validation");
   check(!e.acceptsSavedSpec(new PaintingSpec(1,1,PaintingSpec.Mode.PIXEL,PaintingSpec.Crop.KEEP_WIDTH)),"Invalid named size is rejected");
  }
  check(PaintingBuiltinCatalog.entries().size()==12,"Existing random gallery unchanged");
  for(String other:List.of("", "猫画", "奶油猫", "小米粥", "《奶油", "小米》", "Naiyou"))check(PaintingBuiltinCatalog.named(other).isEmpty(),"No partial/fuzzy trigger: "+other);
  check(PaintingBuiltinCatalog.named(null).isEmpty(),"Unnamed item stays ordinary");
  System.out.println("PASS PaintingNamedRegression: exact names, separate random pool, HD proportions and persistent identities");
 }
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
