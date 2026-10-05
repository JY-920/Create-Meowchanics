package cn.laowu.mod.client;

import cn.laowu.mod.*;
import cn.laowu.mod.accessory.CatAccessories;
import cn.laowu.mod.entity.CatGiantCarrier;
import cn.laowu.mod.genetics.CatTraitData;
import cn.laowu.mod.genetics.CatTraitProfile;
import cn.laowu.mod.genetics.CatAttributeData;
import cn.laowu.mod.genetics.CatAttributeProfile;
import cn.laowu.mod.genetics.CatStat;
import cn.laowu.mod.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Nonblocking integrated-server probe: real keys, normal packets, real owner and cat. */
public final class GiantCatRidingClientProbe {
    private static int stage, stageTicks, totalTicks;
    private static double startX, startZ, floorY, walkSpeed, runSpeed;
    private static boolean sawWindup, sawAir;
    private static int careerRound;
    private static boolean mountedAfterWrench;
    private static double bobMin = Double.POSITIVE_INFINITY, bobMax = Double.NEGATIVE_INFINITY;
    private static BlockPos base;
    private static final AtomicReference<UUID> CAT_ID=new AtomicReference<>();
    private static final AtomicReference<Throwable> SERVER_FAILURE=new AtomicReference<>();
    private static final AtomicBoolean SERVER_RELEASED=new AtomicBoolean();
    private static final AtomicBoolean SERVER_RESTING=new AtomicBoolean();
    private static final AtomicBoolean SERVER_STANDING=new AtomicBoolean();

    public static boolean verifyTick(Minecraft mc) {
        try { return step(mc); }
        catch(Throwable failure) {
            releaseKeys(mc);
            throw new AssertionError("Giant ride client stage="+stage+" tick="+stageTicks+" total="+totalTicks
                    +" vehicle="+(mc.player==null?"none":mc.player.getVehicle())+" base="+base,failure);
        }
    }
    private static boolean step(Minecraft mc) {
        mc.options.pauseOnLostFocus=false;
        if(mc.player==null || mc.level==null || mc.getSingleplayerServer()==null) return false;
        if(++totalTicks>500) throw new AssertionError("Timed out; stage="+stage+" stageTicks="+stageTicks
                +" catId="+CAT_ID.get()+" serverFailure="+SERVER_FAILURE.get());
        if(SERVER_FAILURE.get()!=null) throw new AssertionError("Server fixture failed",SERVER_FAILURE.get());
        stageTicks++;
        if(stage==0) {
            releaseKeys(mc);
            if(mc.screen!=null) mc.setScreen(null);
            base=mc.player.blockPosition().offset(8,8,8);
            var dimension=mc.level.dimension();
            UUID ownerId=mc.player.getUUID();
            mc.getSingleplayerServer().execute(()->{
                try {
                    ServerLevel level=mc.getSingleplayerServer().getLevel(dimension);
                    if(level==null) throw new AssertionError("Missing integrated server dimension");
                    level.getChunkAt(base);
                    // Walk + sprint travels beyond the old six-block platform before jumping.
                    for(int x=-6;x<=6;x++) for(int z=-20;z<=20;z++) {
                        var floor=base.offset(x,0,z);
                        level.setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());
                        for(int y=1;y<=7;y++) level.setBlockAndUpdate(floor.above(y),Blocks.AIR.defaultBlockState());
                    }
                    var owner=level.getServer().getPlayerList().getPlayer(ownerId);
                    if(owner==null) throw new AssertionError("Actual integrated-server player unavailable");
                    if(owner.getVehicle() instanceof CatGiantCarrier stale) stale.release();
                    else owner.stopRiding();
                    owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(com.simibubi.create.AllItems.WRENCH.get()));
                    owner.setShiftKeyDown(false);
                    owner.teleportTo(base.getX()+.5,base.getY()+1,base.getZ()+.5);
                    owner.setYRot(0);owner.setYHeadRot(0);
                    Cat cat=EntityType.CAT.create(level);
                    if(cat==null) throw new AssertionError("Cannot create real cat");
                    cat.setTame(true,true);cat.setOwnerUUID(ownerId);cat.setAge(0);
                    cat.setPos(base.getX()+.5,base.getY()+1,base.getZ()+1.5);
                    CatAttributeData.set(cat,CatAttributeProfile.founder(cat.getRandom())
                            .withValues(CatStat.HEALTH,100,100));
                    if(!level.addFreshEntity(cat)) throw new AssertionError("Cat addFreshEntity failed");
                    CatTraitData.set(cat,CatTraitProfile.EMPTY);cat.setAge(0);
                    CatClothesData.equip(cat, careerRound == 0 ? CatOutfitType.FLIGHT : CatOutfitType.DIVING);
                    ModNetwork.syncCatAttributesToTracking(cat);
                    cat.setOrderedToSit(false);cat.setInSittingPose(false);
                    var inventory=CatProfileData.openContainer(cat);
                    inventory.setItem(0,new ItemStack(BuiltInRegistries.ITEM.get(LaoWuMod.id("cat_giant_collar"))));
                    inventory.setChanged();CatAccessories.equipmentChanged(cat);
                    CAT_ID.set(cat.getUUID());
                } catch(Throwable failure) { SERVER_FAILURE.set(failure); }
            });
            next(1);return false;
        }
        if(stage==1) {
            if (!mountedAfterWrench) {
                Cat cat = clientCat(mc);
                if (cat == null || !CatGiantMount.active(cat) || mc.gameMode == null
                        || mc.player.distanceToSqr(cat) > 25 || stageTicks < 8) return false;
                mc.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(com.simibubi.create.AllItems.WRENCH.get()));
                mc.gameMode.interact(mc.player, cat, InteractionHand.MAIN_HAND);
                check(cat.isOrderedToSit(), "Real client wrench interaction predicts the sitting command");
                next(20); return false;
            }
            if(stageTicks%80==0 && mc.player.getVehicle() instanceof CatGiantCarrier debug)
                System.out.println("GIANT DEBUG ground="+debug.grounded()+" cat="+debug.cat()+" active="+(debug.cat()!=null&&CatGiantMount.active(debug.cat()))+" width="+(debug.cat()==null?-1:debug.cat().getBbWidth())+" state="+(debug.cat()==null?"none":debug.cat().getPersistentData()));
            if(!(mc.player.getVehicle() instanceof CatGiantCarrier carrier) || carrier.cat()==null
                    || !CatGiantMount.active(carrier.cat()) || carrier.cat().getBbWidth()<1.5) return false;
            mc.player.setYRot(0);
            if (stageTicks < 32) return false; // Settle the wrench-rest/mount blend before measuring locomotion.
            if(!carrier.grounded()) return false;
            check(carrier.getBbHeight()>=4,"Client carrier collision covers rider's head");
            check(mc.getEntityRenderDispatcher().getRenderer(carrier.cat()) instanceof HissingCatRenderer,
                    "Original mounted cat uses live registered renderer");
            startX=carrier.getX();startZ=carrier.getZ();floorY=carrier.getY();
            mc.options.keyUp.setDown(true);
            next(2);return false;
        }
        if (stage == 20) {
            Cat cat = clientCat(mc);
            if (cat == null || !cat.isInSittingPose() || stageTicks < 8) return false;
            mc.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            mc.getSingleplayerServer().execute(() -> {
                var owner = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                if (owner != null) owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            });
            // Let the normal hand state packet settle before the single barehand interaction.
            next(21); return false;
        }
        if (stage == 21) {
            if (stageTicks == 8) mc.gameMode.interact(mc.player, clientCat(mc), InteractionHand.MAIN_HAND);
            if (!(mc.player.getVehicle() instanceof CatGiantCarrier)) return false;
            mountedAfterWrench = true; next(1); return false;
        }
        if(stage==2) {
            var carrier=vehicle(mc);
            sampleBob(mc);
            walkSpeed=Math.max(walkSpeed,carrier.horizontalSpeed());
            if(stageTicks<12 || carrier.position().distanceToSqr(new Vec3(startX,carrier.getY(),startZ))<=.1
                    || walkSpeed<=.08)return false;
            check(carrier.position().distanceToSqr(new Vec3(startX,carrier.getY(),startZ))>.1,
                    "Normal client keyUp packets move server-owned carrier");
            check(walkSpeed>.08,"Client receives synced walking speed");
            startX=carrier.getX();startZ=carrier.getZ();
            mc.options.keySprint.setDown(true);
            next(3);return false;
        }
        if(stage==3) {
            var carrier=vehicle(mc);
            sampleBob(mc);
            runSpeed=Math.max(runSpeed,carrier.horizontalSpeed());
            if(stageTicks<10 || carrier.position().distanceToSqr(new Vec3(startX,carrier.getY(),startZ))<=.1
                    || runSpeed<=walkSpeed+.03)return false;
            check(carrier.position().distanceToSqr(new Vec3(startX,carrier.getY(),startZ))>.1,
                    "Sprint key continues server movement");
            check(runSpeed>walkSpeed+.03,"Sprint exceeds walk on synced server speed: walk="+walkSpeed+" run="+runSpeed);
            check(bobMax - bobMin > .015, "Actual walk/sprint keys animate the rider with the moving cat back");
            mc.options.keyUp.setDown(false);mc.options.keySprint.setDown(false);
            // Complete press/release between ticks: isDown is already false when controls sample.
            mc.options.keyJump.setDown(false);
            net.minecraft.client.KeyMapping.click(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE));
            next(4);return false;
        }
        if(stage==4) {
            var carrier=vehicle(mc);
            if(carrier.jumpWindup()>0 && carrier.grounded()) sawWindup=true;
            if(!carrier.grounded() && carrier.getY()>floorY+.1) sawAir=true;
            if(!sawAir)return false;
            check(sawWindup,"Client observes synced anticipation from a sub-tick tap before launch");
            mc.options.keyJump.setDown(false);
            next(5);return false;
        }
        if(stage==5) {
            var carrier=vehicle(mc);
            if(!carrier.grounded())return false;
            mc.options.keyShift.setDown(true);
            next(6);return false;
        }
        if(stage==6) {
            if(mc.player.getVehicle()!=null)return false;
            releaseKeys(mc);
            next(7);return false;
        }
        if(stage==7) {
            if(stageTicks%4==1) {
                UUID ownerId=mc.player.getUUID(),catId=CAT_ID.get();
                var dimension=mc.level.dimension();
                mc.getSingleplayerServer().execute(()->{
                    try {
                        var level=mc.getSingleplayerServer().getLevel(dimension);
                        var owner=level.getServer().getPlayerList().getPlayer(ownerId);
                        var entity=level.getEntity(catId);
                        if(owner!=null && owner.getVehicle()==null && entity instanceof Cat cat
                                && cat.isAlive() && cat.getVehicle()==null && CatGiantMount.active(cat)) {
                            owner.teleportTo(cat.getX()+1.5,cat.getY(),cat.getZ());
                            SERVER_RELEASED.set(true);
                        }
                    } catch(Throwable failure) { SERVER_FAILURE.set(failure); }
                });
            }
            if(!SERVER_RELEASED.get())return false;
            if (stageTicks < 60) return false;
            Cat freelyStanding = clientCat(mc);
            if (freelyStanding == null) return false;
            GiantCatAnimation.sample(freelyStanding, 0);
            check(!freelyStanding.isInSittingPose() && GiantCatAnimation.restWeight(freelyStanding) < .002,
                    "Wrench -> real mount -> dismount must not revive side-rest for " + CatClothesData.getOutfit(freelyStanding));
            mc.options.keyShift.setDown(true);
            next(8);return false;
        }
        if(stage==8 || stage==10) {
            // Let the actual Shift key and teleport synchronization settle, then send
            // exactly one normal entity-interaction packet; retries would toggle twice.
            if(stageTicks<8)return false;
            Cat cat=clientCat(mc);
            if(cat==null || mc.gameMode==null || !mc.player.isShiftKeyDown()
                    || cat.distanceToSqr(mc.player)>25)return false;
            mc.player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            mc.gameMode.interact(mc.player,cat,InteractionHand.MAIN_HAND);
            next(stage==8?9:11);return false;
        }
        if(stage==9 || stage==11) {
            boolean resting=stage==9;
            if(stageTicks%4==1) verifyServerRest(mc,resting);
            Cat cat=clientCat(mc);
            if(cat==null || cat.isInSittingPose()!=resting
                    || !(resting?SERVER_RESTING:SERVER_STANDING).get())return false;
            check(mc.player.getVehicle()==null && cat.getVehicle()==null,
                    "Sneak empty-hand interaction changes rest without remounting");
            if(resting) {
                if(stageTicks<40)return false;
                next(10);return false;
            }
            releaseKeys(mc);
            if (careerRound == 0) {
                careerRound++;
                mountedAfterWrench = false; sawWindup = sawAir = false;
                walkSpeed = runSpeed = 0;
                bobMin = Double.POSITIVE_INFINITY; bobMax = Double.NEGATIVE_INFINITY;
                SERVER_RELEASED.set(false); SERVER_RESTING.set(false); SERVER_STANDING.set(false); CAT_ID.set(null);
                next(0); return false;
            }
            System.out.println("PASS: real giant-cat owner interaction, client packets walk/sprint/jump windup/ground sync, Shift dismount, sneak-interact rest/recovery synchronized from server; cat="+CAT_ID.get());
            next(12);return true;
        }
        return stage==12;
    }
    private static void sampleBob(Minecraft mc) {
        double bob = mc.getEntityRenderDispatcher().getRenderer(mc.player).getRenderOffset(mc.player, 0).y;
        check(Double.isFinite(bob), "Real rider presentation stays finite while input packets are active");
        bobMin = Math.min(bobMin, bob); bobMax = Math.max(bobMax, bob);
    }
    private static Cat clientCat(Minecraft mc) {
        for(var entity:mc.level.entitiesForRendering())
            if(entity instanceof Cat cat && cat.getUUID().equals(CAT_ID.get()))return cat;
        return null;
    }
    private static void verifyServerRest(Minecraft mc,boolean resting) {
        UUID ownerId=mc.player.getUUID(),catId=CAT_ID.get();
        var dimension=mc.level.dimension();
        mc.getSingleplayerServer().execute(()->{
            try {
                var level=mc.getSingleplayerServer().getLevel(dimension);
                var owner=level.getServer().getPlayerList().getPlayer(ownerId);
                var entity=level.getEntity(catId);
                if(owner!=null && owner.getVehicle()==null && entity instanceof Cat cat
                        && cat.getVehicle()==null && cat.isOrderedToSit()==resting
                        && cat.isInSittingPose()==resting)
                    (resting?SERVER_RESTING:SERVER_STANDING).set(true);
            } catch(Throwable failure) { SERVER_FAILURE.set(failure); }
        });
    }
    private static CatGiantCarrier vehicle(Minecraft mc) {
        if(mc.player.getVehicle() instanceof CatGiantCarrier carrier)return carrier;
        throw new AssertionError("Lost giant carrier at stage "+stage);
    }
    private static void next(int value) { stage=value;stageTicks=0; }
    private static void check(boolean condition,String message) { if(!condition)throw new AssertionError(message); }
    private static void releaseKeys(Minecraft mc) {
        mc.options.keyUp.setDown(false);mc.options.keySprint.setDown(false);
        mc.options.keyJump.setDown(false);mc.options.keyShift.setDown(false);
    }
    private GiantCatRidingClientProbe() {}
}
