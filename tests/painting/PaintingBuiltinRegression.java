import cn.laowu.mod.painting.*;
import java.util.*;

public final class PaintingBuiltinRegression {
 public static void main(String[] args){
  var entries=PaintingBuiltinCatalog.entries();
  check(entries.size()==12,"All twelve supplied cats must be available");
  var ids=new HashSet<String>();var hashes=new HashSet<String>();
  for(var entry:entries){
   check(ids.add(entry.id()),"Unique IDs");check(hashes.add(entry.hash()),"Unique hashes");
   check(PaintingAssetStore.validHash(entry.hash()),"Compatible saved identity");
   check(PaintingBuiltinCatalog.find(entry.hash()).orElseThrow().equals(entry),"Stable lookup");
   check(entry.texturePath().equals("textures/painting/builtin/"+entry.id()+".png"),"Safe packaged path");
   check(entry.spec().mode()==PaintingSpec.Mode.PIXEL,"Builtin pictures use nearest-neighbour display");
  }
  check(entries.stream().anyMatch(e->e.width()==1&&e.height()==1),"Small walls still have a fitting picture");
  check(entries.stream().anyMatch(e->e.width()==2&&e.height()==1),"Horizontal cat keeps a horizontal canvas");
  check(picture("white_stare").spec().equals(new PaintingSpec(2,2,PaintingSpec.Mode.PIXEL,PaintingSpec.Crop.KEEP_WIDTH)),"White cat uses a 32-pixel canvas for readable detail");
  check(picture("folded_ears").spec().equals(new PaintingSpec(3,3,PaintingSpec.Mode.PIXEL,PaintingSpec.Crop.KEEP_WIDTH)),"Folded-ear cat places on a 3 by 3 wall");
  check(picture("cowboy_cat").spec().equals(new PaintingSpec(3,3,PaintingSpec.Mode.PIXEL,PaintingSpec.Crop.KEEP_WIDTH)),"Cowboy cat fits a 3 by 3 wall rather than requiring four blocks");
  check(picture("grass_cat").spec().equals(new PaintingSpec(2,2,PaintingSpec.Mode.PIXEL,PaintingSpec.Crop.KEEP_HEIGHT)),"Green cat keeps its 2 by 2 size and full vertical composition");
  // Literal saved identities read from the shipped painting.7 JAR, not derived by current code.
  var shippedHashes=Map.of(
   "white_stare","93f7c8662de32871eb77ca2c23958f9eb80c64aca7f17162b8450931f93f8e91",
   "grass_cat","cb1d99b60c1ab60e73a9ea56c515618360d9c76b09baa4da2bf5edd159e023c8",
   "folded_ears","ad44bc95b4d26db17d65d004f864c83da7e0962317f5b248279ef8b1f6b1e11a",
   "cowboy_cat","437238452dfb1884187a732ad691e1e3043b6a62fe9807f01047162d55c07d33"
  );
  var previous=Map.of("white_stare",1,"grass_cat",2,"folded_ears",2,"cowboy_cat",4);
  for(var saved:previous.entrySet()){
   var entry=picture(saved.getKey());
   check(PaintingBuiltinCatalog.find(shippedHashes.get(saved.getKey())).map(e->e.id().equals(saved.getKey())).orElse(false),"Shipped saved identity still resolves: "+saved.getKey());
   check(entry.acceptsSavedSpec(new PaintingSpec(saved.getValue(),saved.getValue(),PaintingSpec.Mode.PIXEL,PaintingSpec.Crop.KEEP_WIDTH)),"Previously placed painting remains readable: "+saved.getKey());
   check(entry.acceptsSavedSpec(entry.spec()),"Current dimensions remain readable");
   check(!entry.acceptsSavedSpec(new PaintingSpec(8,8,PaintingSpec.Mode.PIXEL,PaintingSpec.Crop.KEEP_WIDTH)),"Legacy support must not accept arbitrary sizes");
   check(!entry.acceptsSavedSpec(new PaintingSpec(saved.getValue(),saved.getValue(),PaintingSpec.Mode.ORIGINAL,PaintingSpec.Crop.KEEP_WIDTH)),"Legacy support must not accept an unshipped mode");
  }
  check(!picture("silver_loaf").acceptsSavedSpec(new PaintingSpec(1,1,PaintingSpec.Mode.PIXEL,PaintingSpec.Crop.KEEP_WIDTH)),"Unchanged paintings keep strict dimension checks");
  check(PaintingBuiltinCatalog.find("a".repeat(64)).isEmpty(),"Custom images remain custom");
  check(PaintingBuiltinCatalog.find("../silver_loaf").isEmpty(),"No arbitrary resource paths");
  check(PaintingBuiltinCatalog.find(null).isEmpty(),"Missing identities are safe");
  try{entries.clear();throw new AssertionError("Catalog must be immutable");}catch(UnsupportedOperationException expected){}
  System.out.println("PASS PaintingBuiltinRegression: twelve stable packaged paintings, dimensions and custom isolation");
 }
 private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 private static PaintingBuiltinCatalog.Entry picture(String id){return PaintingBuiltinCatalog.entries().stream().filter(e->e.id().equals(id)).findFirst().orElseThrow();}
}
