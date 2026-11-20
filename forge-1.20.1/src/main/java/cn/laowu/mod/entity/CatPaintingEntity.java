package cn.laowu.mod.entity;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.painting.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import java.util.Optional;

public class CatPaintingEntity extends HangingEntity {
 private static final EntityDataAccessor<CompoundTag> DATA=SynchedEntityData.defineId(CatPaintingEntity.class,EntityDataSerializers.COMPOUND_TAG);
 private boolean dropped;
 public CatPaintingEntity(EntityType<? extends CatPaintingEntity> type,Level level){super(type,level);}
 @Override protected void defineSynchedData(){entityData.define(DATA,new CompoundTag());}
 public Optional<CatPaintingData> getPaintingData(){return CatPaintingData.fromTag(entityData.get(DATA));}
 public void setPaintingData(CatPaintingData data){
  CompoundTag tag=data.tag();tag.putLong("Anchor",pos.asLong());tag.putByte("Facing",(byte)direction.get2DDataValue());entityData.set(DATA,tag);
 }
 public void place(BlockPos anchor,Direction facing,CatPaintingData data){pos=anchor;setDirection(facing);setPaintingData(data);recalculateBoundingBox();}
 public int widthBlocks(){return getPaintingData().map(d->d.spec().widthBlocks()).orElse(1);}
 public int heightBlocks(){return getPaintingData().map(d->d.spec().heightBlocks()).orElse(1);}
 public AABB supportBox(){return getBoundingBox().move(-direction.getStepX()*.5,0,-direction.getStepZ()*.5).deflate(1e-7);}
 public Iterable<BlockPos> occupiedAndSupport(){
  AABB box=getBoundingBox().minmax(supportBox()).deflate(1e-7);
  return BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ));
 }
 @Override public boolean survives(){
  AABB b=getBoundingBox().minmax(supportBox());
  if(!level().getWorldBorder().isWithinBounds(getBoundingBox()))return false;
  for(BlockPos p:occupiedAndSupport())if(level().isOutsideBuildHeight(p)||!level().hasChunkAt(p))return false;
  return super.survives();
 }
 @Override public int getWidth(){return widthBlocks()*16;}
 @Override public int getHeight(){return heightBlocks()*16;}
 @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){
  super.onSyncedDataUpdated(key);
  if(DATA.equals(key)){
   CompoundTag tag=entityData.get(DATA);
   if(tag.contains("Anchor"))pos=BlockPos.of(tag.getLong("Anchor"));
   setDirection(Direction.from2DDataValue(tag.getByte("Facing")));
  }
 }
 @Override public void addAdditionalSaveData(CompoundTag tag){tag.put("Painting",entityData.get(DATA).copy());}
 @Override public void readAdditionalSaveData(CompoundTag tag){
  CompoundTag data=tag.getCompound("Painting");
  if(CatPaintingData.fromTag(data).isEmpty()){discard();return;}
  entityData.set(DATA,data);pos=BlockPos.of(data.getLong("Anchor"));setDirection(Direction.from2DDataValue(data.getByte("Facing")));
 }
 @Override public void dropItem(Entity breaker){
  if(dropped)return;dropped=true;
  if(level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)){
   playSound(SoundEvents.PAINTING_BREAK,1,1);
   if(!(breaker instanceof Player player && player.getAbilities().instabuild))
    getPaintingData().ifPresent(data->spawnAtLocation(getPickResult()));
  }
 }
 @Override public void playPlacementSound(){playSound(SoundEvents.PAINTING_PLACE,1,1);}
 @Override public ItemStack getPickResult(){return getPaintingData().filter(d->PaintingBuiltinCatalog.find(d.hash()).map(PaintingBuiltinCatalog.Entry::namedOnly).orElse(true)).map(CatPaintingData::createStack).orElseGet(()->new ItemStack(LaoWuMod.CAT_PAINTING.get()));}
 @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
}

