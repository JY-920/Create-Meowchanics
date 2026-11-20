package cn.laowu.mod.painting;
import cn.laowu.mod.LaoWuMod;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public record CatPaintingData(int schema,String hash,PaintingSpec spec,UUID author) {
 public CatPaintingData{
  if(schema!=1 || !PaintingAssetStore.validHash(hash) || spec==null || author==null)throw new IllegalArgumentException("painting_data");
  var builtin=PaintingBuiltinCatalog.find(hash);
  if(builtin.isPresent() && !builtin.get().acceptsSavedSpec(spec))throw new IllegalArgumentException("builtin_painting_spec");
 }
 public CompoundTag tag(){
  var tag=new CompoundTag();tag.putInt("Schema",schema);tag.putString("Hash",hash);
  tag.putInt("Width",spec.widthBlocks());tag.putInt("Height",spec.heightBlocks());
  tag.putString("Mode",spec.mode().name());tag.putString("Crop",spec.crop().name());tag.putUUID("Author",author);return tag;
 }
 public static Optional<CatPaintingData> fromTag(CompoundTag tag){
  try{return Optional.of(new CatPaintingData(tag.getInt("Schema"),tag.getString("Hash"),new PaintingSpec(tag.getInt("Width"),tag.getInt("Height"),PaintingSpec.Mode.valueOf(tag.getString("Mode")),PaintingSpec.Crop.valueOf(tag.getString("Crop"))),tag.getUUID("Author")));}
  catch(RuntimeException e){return Optional.empty();}
 }
 public static Optional<CatPaintingData> read(ItemStack stack){return fromTag(stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getCompound("CatPainting"));}
 public static void write(ItemStack stack,CatPaintingData data){net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,stack,root->root.put("CatPainting",data.tag()));}
 /** Resolve names on demand, so anvil output glints immediately without a tick or NBT rewrite. */
 public static Optional<PaintingBuiltinCatalog.Entry> namedEntry(ItemStack stack){
  var name=stack.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);return name==null?Optional.empty():PaintingBuiltinCatalog.named(name.getString());
 }
 public static Optional<CatPaintingData> resolve(ItemStack stack){
  return namedEntry(stack).map(e->new CatPaintingData(1,e.hash(),e.spec(),new UUID(0,0))).or(()->read(stack));
 }
 public static ItemStack createStack(CatPaintingData data){
  var stack=new ItemStack(LaoWuMod.CAT_PAINTING.get());var builtin=PaintingBuiltinCatalog.find(data.hash());
  if(builtin.filter(PaintingBuiltinCatalog.Entry::namedOnly).isPresent()){
   var name=net.minecraft.network.chat.Component.literal(builtin.get().triggerName());
   stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,name);
  }else write(stack,data);
  return stack;
 }
 public static boolean hasStoredPainting(ItemStack stack){return stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().contains("CatPainting");}
}

