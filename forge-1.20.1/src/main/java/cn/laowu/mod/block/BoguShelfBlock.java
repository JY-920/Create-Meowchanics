package cn.laowu.mod.block;
import cn.laowu.mod.*;
import cn.laowu.mod.entity.BoguShelfSeat;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
/** Nine actual occupied cells; geometry/collision stays in each cell's chunk. */
public final class BoguShelfBlock extends Block {
 public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
 public static final IntegerProperty PART=IntegerProperty.create("part",0,8);
 public static final int ROOT=1;
 private static final VoxelShape[][] SHAPES=new VoxelShape[4][9];
 private final ThreadLocal<Boolean> removing=ThreadLocal.withInitial(()->false);
 static {
  for(int turn=0;turn<4;turn++)for(int part=0;part<9;part++) {
   VoxelShape shape=Shapes.empty();
   for(double[] b:BoguShelfShapeData.BOXES[part]) {
    double x0=b[0],z0=b[2],x1=b[3],z1=b[5];
    for(int i=0;i<turn;i++) {double old=x0;x0=1-z1;z1=x1;x1=1-z0;z0=old;}
    shape=Shapes.or(shape,Shapes.box(x0,b[1],z0,x1,b[4],z1));
   }
   SHAPES[turn][part]=shape.optimize();
  }
 }
 public BoguShelfBlock() {super(Properties.of().strength(2F).sound(SoundType.WOOD).noOcclusion());
  registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(PART,ROOT));
 }
 public static BlockPos partPos(BlockPos root,Direction facing,int part) {return root.relative(facing.getClockWise(),part%3-1).above(part/3);}
 public static BlockPos origin(BlockPos pos,BlockState state) {return pos.relative(state.getValue(FACING).getClockWise(),1-state.getValue(PART)%3).below(state.getValue(PART)/3);}
 public static boolean complete(Level level,BlockPos root,Direction facing) {
  for(int i=0;i<9;i++){var s=level.getBlockState(partPos(root,facing,i));if(!(s.getBlock() instanceof BoguShelfBlock)||s.getValue(PART)!=i||s.getValue(FACING)!=facing)return false;}return true;
 }
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,PART);}
 @Override public BlockState getStateForPlacement(BlockPlaceContext c) {
  var facing=c.getHorizontalDirection().getOpposite();var root=c.getClickedPos();
  for(int i=0;i<9;i++) {
   var at=partPos(root,facing,i);var state=defaultBlockState().setValue(FACING,facing).setValue(PART,i);
   if(c.getLevel().isOutsideBuildHeight(at)||!c.getLevel().getWorldBorder().isWithinBounds(at)
      ||!c.getLevel().getBlockState(at).canBeReplaced(c)
      ||(c.getPlayer()!=null&&(!c.getLevel().mayInteract(c.getPlayer(),at)||!c.getPlayer().mayUseItemAt(at,c.getClickedFace(),c.getItemInHand())))
      ||!c.getLevel().isUnobstructed(state,at,CollisionContext.empty()))return null;
  }
  return defaultBlockState().setValue(FACING,facing);
 }
 @Override public void setPlacedBy(Level level,BlockPos pos,BlockState s,LivingEntity placer,ItemStack stack) {
  if(level.isClientSide)return;
  for(int i=0;i<9;i++)if(i!=ROOT)level.setBlock(partPos(pos,s.getValue(FACING),i),s.setValue(PART,i),3);
 }
 @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPES[s.getValue(FACING).get2DDataValue()==2?0:s.getValue(FACING)==Direction.EAST?1:s.getValue(FACING)==Direction.SOUTH?2:3][s.getValue(PART)];}
 @Override public PushReaction getPistonPushReaction(BlockState state){return PushReaction.BLOCK;}
 @Override public BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
 @Override public BlockState mirror(BlockState s,Mirror m){return m==Mirror.NONE?s:rotate(s,m.getRotation(s.getValue(FACING))).setValue(PART,s.getValue(PART)/3*3+2-s.getValue(PART)%3);}
 @Override public void onRemove(BlockState s,Level level,BlockPos p,BlockState next,boolean moving) {
  if(!level.isClientSide&&!next.is(this)&&!removing.get()) {
   var root=origin(p,s);var facing=s.getValue(FACING);removing.set(true);
   try {
    for(int i=0;i<9;i++){var at=partPos(root,facing,i);var other=level.getBlockState(at);
     if(!at.equals(p)&&other.is(this)&&other.getValue(FACING)==facing&&other.getValue(PART)==i)level.removeBlock(at,false);
    }
    for(var seat:level.getEntitiesOfClass(BoguShelfSeat.class,new AABB(root).inflate(4),e->e.anchor().equals(root)))seat.release();
   }finally{removing.remove();}
  }
  super.onRemove(s,level,p,next,moving);
 }
 public InteractionResult interact(BlockState s,Level level,BlockPos pos,Player player,InteractionHand hand) {
  return InteractionResult.PASS; // Display attraction is automatic; shelf itself has no right-click action.
 }
 public void attract(Level level,BlockPos root,BlockState s) {
  if(level.isClientSide||!level.hasChunksAt(root.offset(-2,0,-2),root.offset(2,2,2))
      ||!complete(level,root,s.getValue(FACING)))return;
  var seats=level.getEntitiesOfClass(BoguShelfSeat.class,new AABB(root).inflate(4),e->e.anchor().equals(root));
  if(seats.stream().anyMatch(e->e.cat()!=null))return;
  var center=Vec3.atCenterOf(root);
  var cats=level.getEntitiesOfClass(Cat.class,new AABB(root).inflate(3),cat->BoguShelfSeat.eligible(cat)&&cat.distanceToSqr(center)<=9);
  cats.sort(java.util.Comparator.comparingDouble((Cat cat)->cat.distanceToSqr(center)).thenComparingInt(Entity::getId));
  if(cats.isEmpty())return;
  var cat=cats.get(0);var seat=seats.isEmpty()?LaoWuMod.BOGU_SHELF_SEAT.get().create(level):seats.get(0);
  if(seat==null)return;
  seat.configure(root,s.getValue(FACING),cat);
  var target=seat.position();var bounds=cat.getBoundingBox().move(target.subtract(cat.position()));
  if(!level.getWorldBorder().isWithinBounds(bounds)||!level.getEntityCollisions(cat,bounds).isEmpty())return;
  // The full-size hitbox may touch this display's trim while its .64-rendered cat fits.
  // Ignore only this complete shelf's cells, not neighboring blocks or other entities.
  // Vanilla collision traversal also inspects adjacent cells for protruding shapes.
  for(var at:BlockPos.betweenClosed(BlockPos.containing(bounds.minX,bounds.minY,bounds.minZ).offset(-1,-1,-1),BlockPos.containing(bounds.maxX,bounds.maxY,bounds.maxZ).offset(1,1,1))){
   var other=level.getBlockState(at);
   if(other.is(this)&&origin(at,other).equals(root))continue;
   if(Shapes.joinIsNotEmpty(other.getCollisionShape(level,at).move(at.getX(),at.getY(),at.getZ()),Shapes.create(bounds),BooleanOp.AND))return;
  }
  if(seats.isEmpty()&&!level.addFreshEntity(seat))return;
  if(!cat.startRiding(seat,true)){if(seats.isEmpty())seat.discard();return;}
  cat.setPos(target);seat.hold(cat);
 }
 @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){return interact(s,l,p,player,hand);}
}
