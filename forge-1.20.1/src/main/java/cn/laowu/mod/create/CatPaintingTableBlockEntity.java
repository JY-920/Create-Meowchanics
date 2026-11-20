package cn.laowu.mod.create;
import cn.laowu.mod.LaoWuMod;import cn.laowu.mod.painting.*;
import net.minecraft.core.BlockPos;import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;import net.minecraft.world.level.block.state.BlockState;
import java.util.UUID;

public class CatPaintingTableBlockEntity extends BlockEntity {
 private UUID token;private long revision;
 final SimpleContainer painting=new SimpleContainer(2){@Override public void setChanged(){super.setChanged();revision++;CatPaintingTableBlockEntity.this.setChanged();}};
 public CatPaintingTableBlockEntity(BlockPos pos,BlockState state){super(CatPaintingTableRegistration.ENTITY.get(),pos,state);}
 public boolean locked(){return token!=null;}
 public long revision(){return revision;}
 public boolean lock(UUID value,long expected){
  ItemStack stack=painting.getItem(0);
  if(isRemoved()||token!=null||revision!=expected||!stack.is(LaoWuMod.CAT_PAINTING.get())||stack.getCount()!=1||!painting.getItem(1).isEmpty())return false;
  token=value;return true;
 }
 public void unlock(UUID value){if(value.equals(token))token=null;}
 public boolean commit(UUID value,long expected,CatPaintingData data){
  ItemStack input=painting.getItem(0);
  if(!value.equals(token)||expected!=revision||isRemoved()||!input.is(LaoWuMod.CAT_PAINTING.get())||input.getCount()!=1||!painting.getItem(1).isEmpty())return false;
  ItemStack output=input.copy();
  CatPaintingData.write(output,data);
  painting.setItem(0,ItemStack.EMPTY);
  painting.setItem(1,output);
  return true;
 }
 @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.put("Painting",painting.getItem(0).save(new CompoundTag()));tag.put("PrintedPainting",painting.getItem(1).save(new CompoundTag()));tag.putLong("Revision",revision);}
 @Override public void load(CompoundTag tag){super.load(tag);painting.setItem(0,ItemStack.of(tag.getCompound("Painting")));painting.setItem(1,ItemStack.of(tag.getCompound("PrintedPainting")));revision=tag.getLong("Revision");token=null;}
}
