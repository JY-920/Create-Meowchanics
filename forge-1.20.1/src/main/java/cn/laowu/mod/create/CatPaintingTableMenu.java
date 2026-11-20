package cn.laowu.mod.create;
import cn.laowu.mod.LaoWuMod;import cn.laowu.mod.painting.*;
import net.minecraft.core.BlockPos;import net.minecraft.world.*;import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;import net.minecraft.world.item.ItemStack;
import java.util.UUID;

public class CatPaintingTableMenu extends AbstractContainerMenu implements PaintingEditorAccess {
 public static final int FOOTER_Y=113;
 public static final int INPUT=0,OUTPUT=1;
 public static final int SLOT_X=21,OUTPUT_X=166,SLOT_Y=FOOTER_Y+10,INVENTORY_X=23,INVENTORY_Y=171;
 private final CatPaintingTableBlockEntity table;
 private final Container slot;
 private final BlockPos pos;private final Player owner;private long clientRevision;
 public CatPaintingTableMenu(int id,Inventory inventory,BlockPos pos){
  super(CatPaintingTableRegistration.MENU.get(),id);this.pos=pos;owner=inventory.player;
  table=inventory.player.level().getBlockEntity(pos) instanceof CatPaintingTableBlockEntity be?be:null;
  slot=table==null?new SimpleContainer(2):table.painting;
  addSlot(new Slot(slot,INPUT,SLOT_X,SLOT_Y){
   @Override public boolean mayPlace(ItemStack stack){return !locked()&&stack.is(LaoWuMod.CAT_PAINTING.get());}
   @Override public boolean mayPickup(Player player){return !locked();}
   @Override public int getMaxStackSize(){return 1;}
  });
  addSlot(new Slot(slot,OUTPUT,OUTPUT_X,SLOT_Y){
   @Override public boolean mayPlace(ItemStack stack){return false;}
   @Override public boolean mayPickup(Player player){return !locked();}
   @Override public int getMaxStackSize(){return 1;}
  });
  for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inventory,x+y*9+9,INVENTORY_X+x*18,INVENTORY_Y+y*18));
  for(int x=0;x<9;x++)addSlot(new Slot(inventory,x,INVENTORY_X+x*18,INVENTORY_Y+58));
  for(int part=0;part<4;part++){final int shift=part*16;addDataSlot(new DataSlot(){
   public int get(){return (int)(((table==null?clientRevision:table.revision())>>>shift)&0xffffL);}
   public void set(int value){long mask=0xffffL<<shift;clientRevision=(clientRevision&~mask)|((value&0xffffL)<<shift);}
  });}
 }
 public boolean locked(){return table!=null&&table.locked();}
 public boolean hasPainting(){return getSlot(INPUT).getItem().is(LaoWuMod.CAT_PAINTING.get());}
 public boolean hasOutput(){return getSlot(OUTPUT).hasItem();}
 public boolean canPrint(){return !locked()&&hasPainting()&&!hasOutput();}
 @Override public long revision(){return table!=null&&!owner.level().isClientSide?table.revision():clientRevision;}
 @Override public boolean stillValid(Player player){
  return player.level().hasChunkAt(pos) && player.level().getBlockEntity(pos) instanceof CatPaintingTableBlockEntity be &&
   !be.isRemoved() && player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64 && player.level().mayInteract(player,pos);
 }
 @Override public boolean lock(UUID token,long expected){return table!=null&&stillValid(owner)&&table.lock(token,expected);}
 @Override public void unlock(UUID token){if(table!=null)table.unlock(token);}
 @Override public boolean commit(UUID token,long expected,CatPaintingData data){return table!=null&&stillValid(owner)&&table.commit(token,expected,data);}
 @Override public void clicked(int slotId,int button,ClickType type,Player player){if(!locked())super.clicked(slotId,button,type,player);}
 @Override public ItemStack quickMoveStack(Player player,int index){
  if(locked()||index<0||index>=slots.size())return ItemStack.EMPTY;
  var from=slots.get(index);if(!from.hasItem())return ItemStack.EMPTY;var stack=from.getItem();var copy=stack.copy();
  if(index<=OUTPUT){if(!moveItemStackTo(stack,OUTPUT+1,slots.size(),true))return ItemStack.EMPTY;}
  else{if(!stack.is(LaoWuMod.CAT_PAINTING.get())||!moveItemStackTo(stack,INPUT,INPUT+1,false))return ItemStack.EMPTY;}
  if(stack.isEmpty())from.set(ItemStack.EMPTY);else from.setChanged();return copy;
 }
 @Override public void removed(Player player){
  if(player instanceof net.minecraft.server.level.ServerPlayer p && table!=null){
   // A close invalidates the menu identity immediately; its outstanding upload is cancelled on the next server tick.
   table.setChanged();
  }
  super.removed(player);
 }
}
