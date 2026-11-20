package cn.laowu.mod.painting;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/** Packaged paintings use stable reserved identities, independent of world uploads.
 * Keeping IDs stable lets resource packs replace artwork without changing saves. */
public final class PaintingBuiltinCatalog {
 public record Entry(String id,int width,int height,PaintingSpec.Crop crop,PaintingSpec.Mode mode,String triggerName) {
  public Entry(String id,int width,int height,PaintingSpec.Crop crop){this(id,width,height,crop,PaintingSpec.Mode.PIXEL,null);}
  public Entry(String id,int width,int height){this(id,width,height,PaintingSpec.Crop.KEEP_WIDTH);}
  public String hash(){return PaintingAssetStore.hash(("laowu:builtin_cat_painting:v1/"+id).getBytes(StandardCharsets.UTF_8));}
  public PaintingSpec spec(){return new PaintingSpec(width,height,mode,crop);}
  public boolean namedOnly(){return triggerName!=null;}
  /** Preserve previously placed paintings without resizing them into their neighbours. */
  public boolean acceptsSavedSpec(PaintingSpec saved){return spec().equals(saved)||saved!=null&&saved.equals(LEGACY_SPECS.get(id));}
  public String texturePath(){return "textures/painting/builtin/"+id+".png";}
  public String titleKey(){return "painting.laowu.builtin."+id;}
 }
 private static final List<Entry> ENTRIES=List.of(
  new Entry("silver_loaf",2,2),
  new Entry("white_stare",2,2),
  new Entry("grass_cat",2,2,PaintingSpec.Crop.KEEP_HEIGHT),
  new Entry("angry_kitten",2,2),
  new Entry("teary_cat",2,2),
  new Entry("banana_cat",2,1),
  new Entry("side_eye",2,2),
  new Entry("cap_cat",2,2),
  new Entry("folded_ears",3,3),
  new Entry("round_tabby",1,1),
  new Entry("wide_eyes",2,2),
  new Entry("cowboy_cat",3,3)
 );
 // Only shipped painting.6/7 specs are accepted, not arbitrary spoofed builtin sizes.
 private static final Map<String,PaintingSpec> LEGACY_SPECS=Map.of(
  "naiyou",new PaintingSpec(3,2,PaintingSpec.Mode.ORIGINAL,PaintingSpec.Crop.KEEP_WIDTH),
  "white_stare",new PaintingSpec(1,1,PaintingSpec.Mode.PIXEL,PaintingSpec.Crop.KEEP_WIDTH),
  "grass_cat",new PaintingSpec(2,2,PaintingSpec.Mode.PIXEL,PaintingSpec.Crop.KEEP_WIDTH),
  "folded_ears",new PaintingSpec(2,2,PaintingSpec.Mode.PIXEL,PaintingSpec.Crop.KEEP_WIDTH),
  "cowboy_cat",new PaintingSpec(4,4,PaintingSpec.Mode.PIXEL,PaintingSpec.Crop.KEEP_WIDTH)
 );
 // Separate pool by construction: adding a secret portrait never makes it random.
 private static final List<Entry> NAMED_ENTRIES=List.of(
  new Entry("naiyou",3,2,PaintingSpec.Crop.KEEP_HEIGHT,PaintingSpec.Mode.ORIGINAL,"奶油"),
  new Entry("xiaomi",3,4,PaintingSpec.Crop.KEEP_HEIGHT,PaintingSpec.Mode.ORIGINAL,"小米")
 );
 private static final Map<String,Entry> BY_HASH=java.util.stream.Stream.concat(ENTRIES.stream(),NAMED_ENTRIES.stream()).collect(Collectors.toUnmodifiableMap(Entry::hash,e->e));
 private PaintingBuiltinCatalog(){}
 public static List<Entry> entries(){return ENTRIES;}
 public static List<Entry> namedEntries(){return NAMED_ENTRIES;}
 public static Optional<Entry> named(String name){
  if(name==null)return Optional.empty();
  String plain=name.strip();
  if(plain.startsWith("《")&&plain.endsWith("》"))plain=plain.substring(1,plain.length()-1).strip();
  final String exact=plain;
  return NAMED_ENTRIES.stream().filter(e->e.triggerName().equals(exact)).findFirst();
 }
 public static Optional<Entry> find(String hash){return hash==null?Optional.empty():Optional.ofNullable(BY_HASH.get(hash));}
}
