package cn.laowu.mod.test;
import cn.laowu.mod.*;
import cn.laowu.mod.entity.CatDivingCarrier;
import cn.laowu.mod.genetics.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import java.util.UUID;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class DivingRiderPlacementProbe {
    @GameTest(template="artillery_probe",batch="diver_surface_safety",timeoutTicks=40)
    public static void exhaustedRiderSettlesAtWaterSurface(GameTestHelper h) {
        var level=h.getLevel();
        for(int x=2;x<=10;x++)for(int z=2;z<=10;z++)for(int y=1;y<=6;y++)
            h.setBlock(new BlockPos(x,y,z),Blocks.WATER);
        var base=Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(6,5,6)));
        var stub=FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"surface-stub"));
        var owner=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,
                new com.mojang.authlib.GameProfile(UUID.randomUUID(),"surface-probe"),net.minecraft.server.level.ClientInformation.createDefault());
        owner.connection=stub.connection;owner.setPos(base);
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,com.simibubi.create.AllItems.WRENCH.asStack());
        var cat=new Cat(EntityType.CAT,level) {
            @Override public net.minecraft.world.entity.LivingEntity getOwner(){return owner;}
        };cat.setTame(true,true);cat.setOwnerUUID(owner.getUUID());
        cat.setAge(0);cat.setPos(base);cat.setNoAi(true);level.addFreshEntity(cat);
        CatTraitData.set(cat,CatTraitProfile.EMPTY);CatClothesData.equip(cat,CatOutfitType.DIVING);
        CatPoseData.setPose(cat,CatPoseData.NORMAL);cat.setOrderedToSit(false);cat.setInSittingPose(false);
        long capacity=CatDivingMount.duration(cat);cat.getPersistentData().putLong(CatDivingMount.USED,capacity);
        var carrier=new CatDivingCarrier(LaoWuMod.CAT_DIVING_CARRIER.get(),level);
        carrier.setPos(base);level.addFreshEntity(carrier);
        h.assertTrue(cat.startRiding(carrier,true)&&owner.startRiding(carrier,true),"Real exhausted rider fixture");
        // Holding jump must not relaunch an exhausted rider after reaching the surface.
        carrier.input(owner,0,0,0,0,true,false);
        double minY=Double.POSITIVE_INFINITY,maxY=Double.NEGATIVE_INFINITY;
        for(int i=0;i<100;i++) {
            carrier.tick();
            h.assertTrue(!carrier.isRemoved(),"Exhausted fixture retained");
            h.assertTrue(carrier.swimming(),"Water surface must never become land at tick "+i+" Y="+carrier.getY());
            h.assertTrue(carrier.surfacing()&&carrier.seconds()==0,"Surface keeps exhausted state");
            h.assertTrue(cat.getPersistentData().getLong(CatDivingMount.USED)==capacity,"Water cannot refund stamina");
            if(i>=40){minY=Math.min(minY,carrier.getY());maxY=Math.max(maxY,carrier.getY());}
        }
        h.assertTrue(maxY-minY<.00001,"Surface must settle without bobbing: min="+minY+", max="+maxY);
        h.assertTrue(carrier.getY()>base.y+1,"Exhaustion actually raises the rider to the water surface");
        carrier.positionRider(cat);carrier.positionRider(owner);
        var top=h.absolutePos(new BlockPos(6,6,6));
        double waterline=top.getY()+level.getFluidState(top).getHeight(level,top);
        h.assertTrue(cat.getEyeY()>waterline&&owner.getEyeY()>waterline,
                "Both real passengers can breathe above the water surface");
        // Returning to genuinely dry support still recovers stamina; floating never counts as support.
        h.setBlock(new BlockPos(14,4,6),Blocks.STONE);
        carrier.setPos(base.add(8,0,0));carrier.setDeltaMovement(Vec3.ZERO);carrier.setOnGround(true);
        carrier.tick();
        h.assertTrue(!carrier.swimming(),"Dry shore leaves the swimming state");
        h.assertTrue(cat.getPersistentData().getLong(CatDivingMount.USED)==capacity-2,
                "Only genuinely dry grounded shore restores the existing two ticks of stamina");
        h.assertTrue(carrier.surfacing(),"Partial recovery does not silently clear exhaustion");
        carrier.release();cat.discard();owner.discard();stub.discard();
        System.out.println("PASS: exhausted diving settles at the water surface without land state or stamina refund");
        h.succeed();
    }
    @GameTest(template="artillery_probe",batch="diver_grip_safety",timeoutTicks=40)
    public static void rearGripCannotPushPlayerThroughWall(GameTestHelper h) {
        var level=h.getLevel();
        for(int x=2;x<=10;x++)for(int z=2;z<=10;z++)for(int y=1;y<=6;y++)
            h.setBlock(new BlockPos(x,y,z),Blocks.WATER);
        var base=Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(6,3,6)));
        var stub=FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"grip-stub"));
        var owner=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,
                new com.mojang.authlib.GameProfile(UUID.randomUUID(),"grip-probe"),net.minecraft.server.level.ClientInformation.createDefault());
        owner.connection=stub.connection;owner.setPos(base);
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,com.simibubi.create.AllItems.WRENCH.asStack());
        var cat=new Cat(EntityType.CAT,level){
            @Override public net.minecraft.world.entity.LivingEntity getOwner(){return owner;}
        };cat.setTame(true,true);cat.setOwnerUUID(owner.getUUID());
        cat.setAge(0);cat.setPos(base);cat.setNoAi(true);level.addFreshEntity(cat);
        CatTraitData.set(cat,CatTraitProfile.EMPTY);cat.setAge(0);
        CatClothesData.equip(cat,CatOutfitType.DIVING);
        CatPoseData.setPose(cat,CatPoseData.NORMAL);cat.setOrderedToSit(false);cat.setInSittingPose(false);
        var carrier=new CatDivingCarrier(LaoWuMod.CAT_DIVING_CARRIER.get(),level);
        carrier.setPos(base);level.addFreshEntity(carrier);
        h.assertTrue(cat.startRiding(carrier,true)&&owner.startRiding(carrier,true),"Real rider fixture");
        for(int i=0;i<10;i++) {
            carrier.tick();
            h.assertTrue(!carrier.isRemoved(),"Fixture retained after tick "+i+": alive="+cat.isAlive()+"/"+owner.isAlive()
                    +", outfit="+CatClothesData.getOutfit(cat)+", pose="+CatPoseData.getPose(cat)
                    +", owned="+cat.isOwnedBy(owner)+", sit="+cat.isOrderedToSit()+", wrench="+CatPilotFlight.wrench(owner));
        }
        for(int yaw:new int[]{0,90,180,270}) {
            carrier.setYRot(yaw);carrier.positionRider(cat);carrier.positionRider(owner);
            var forward=Vec3.directionFromRotation(0,yaw);
            var offset=owner.position().subtract(cat.position());
            h.assertTrue(Math.abs(offset.dot(forward)+1.25)<.01,"Grip follows every heading: "+yaw+" "+offset);
        }
        carrier.setYRot(0);owner.setPos(carrier.position());
        // Wall is clear of the cat, but intersects the desired rear player's hitbox and eyes.
        for(int x=5;x<=7;x++)for(int y=1;y<=6;y++)h.setBlock(new BlockPos(x,y,5),Blocks.STONE);
        carrier.positionRider(owner);
        h.assertTrue(!level.getBlockCollisions(owner,owner.getBoundingBox()).iterator().hasNext(),
                "Rear grip must not put rider in a wall; rider="+owner.position()+", cat="+cat.position());
        h.assertTrue(owner.getZ()>base.z-.3,"Blocked rear offset clamps before wall");
        // An already seated rider must not teleport through the wall when separation exceeds 4 blocks.
        carrier.setPos(base.add(0,0,-5));
        carrier.positionRider(owner);
        h.assertTrue(owner.getZ()>base.z-.3,"Active distant rider cannot snap across an obstructed path");
        var lastSafe=owner.position();
        carrier.setPos(base.add(32,0,0));
        carrier.positionRider(owner);
        h.assertTrue(owner.position().distanceToSqr(lastSafe)<.000001,"Extreme separation keeps last safe position without a long sweep");
        carrier.release();cat.discard();owner.discard();stub.discard();
        System.out.println("PASS: real underwater rear placement across four headings and wall-safe grip");
        h.succeed();
    }
}
