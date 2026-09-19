package cn.laowu.mod.test;
import cn.laowu.mod.*;
import cn.laowu.mod.entity.*;
import cn.laowu.mod.genetics.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.common.util.FakePlayerFactory;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class PilotAndTacticsProbe {
    @GameTest(template="accessory_probe", batch="pilot_rules", timeoutTicks=20)
    public static void flightRules(GameTestHelper h) {
        h.assertTrue(CatPilotFlightRules.durationTicks(50)==350 && CatPilotFlightRules.durationTicks(100)==600, "Stamina examples: 17.5 / 30 seconds");
        h.assertTrue(CatPilotFlightRules.durationTicks(0)==100 && CatPilotFlightRules.durationTicks(-50)==100
                && CatPilotFlightRules.durationTicks(Double.NaN)==100, "Minimum 5 seconds with invalid / negative stats");
        h.assertTrue(Math.abs(CatPilotFlightRules.speedPerTick(50)*20-7)<1e-9
                && Math.abs(CatPilotFlightRules.speedPerTick(100)*20-10)<1e-9, "Speed examples: 7 / 10 blocks per second");
        h.assertTrue(CatPilotFlightRules.durationTicks(999999)==5000095L
                && CatPilotFlightRules.speedPerTick(999999)>2999, "High stats keep scaling beyond the old caps");
        h.assertTrue(CatPilotFlightRules.durationTicks(1_000_000_000D)>Integer.MAX_VALUE, "Duration uses long ticks, not overflowing int");
        Vec3 fast=CatPilotFlightRules.step(Vec3.ZERO,1,0,0,0,false,false,false,10);
        h.assertTrue(fast.length()>1.5,"No hidden motion magnitude cap");
        for(int pitch=-90;pitch<=90;pitch+=10){
            Vec3 down=CatPilotFlightRules.step(Vec3.ZERO,1,0,0,pitch,false,true,false,1);
            Vec3 both=CatPilotFlightRules.step(Vec3.ZERO,1,0,0,pitch,true,true,false,1);
            h.assertTrue(down.y<0 && Math.abs(both.y)<1e-6,"Ctrl descends despite look-up; Space+Ctrl cancels altitude");
        }
        h.assertTrue(!CatPilotFlightRules.validInput(Float.NaN,0,0,0)
                && !CatPilotFlightRules.validInput(0,2,0,0)
                && !CatPilotFlightRules.validInput(0,0,Float.POSITIVE_INFINITY,0)
                && !CatPilotFlightRules.validInput(0,0,0,91), "Reject malformed movement packets");
        int checks=0;
        for (int oldYaw=-180;oldYaw<=180;oldYaw+=15) for(int newYaw=-180;newYaw<=180;newYaw+=15) {
            Vec3 direction=Vec3.directionFromRotation(0,newYaw);
            Vec3 turned=CatPilotFlightRules.step(Vec3.directionFromRotation(0,oldYaw).scale(.5),
                    1,0,newYaw,0,false,false,false,.5);
            h.assertTrue(turned.normalize().dot(direction.normalize())>0.99999, "No previous-heading drift, including a full reversal: "+oldYaw+" -> "+newYaw);
            checks++;
        }
        Vec3 stopped=CatPilotFlightRules.step(new Vec3(.5,0,0),0,0,90,0,false,false,false,.5);
        h.assertTrue(stopped.length()<.18,"Releasing movement keys brakes promptly");
        for (int yaw=-180;yaw<=180;yaw+=15) for(int pitch=-90;pitch<=90;pitch+=10)
            for(double oldY:new double[]{-3,-0.1,0,0.5,3}) for(float axis:new float[]{-1,0,1}) {
                Vec3 motion=CatPilotFlightRules.step(new Vec3(.3,oldY,.1),axis,axis,yaw,pitch,true,false,true,1.2);
                h.assertTrue(motion.y<0 && Double.isFinite(motion.length()), "Glide cannot climb, including jump / look-up / momentum");
                checks++;
            }
        for(int intelligence=0;intelligence<=300;intelligence++) for(int d=0;d<=40;d++) {
            h.assertTrue(CatArtilleryTactics.relocate(intelligence,d,40)==(intelligence>=80&&(d<8||d>24)), "Intelligence and distance gates");
            h.assertTrue(!CatArtilleryTactics.relocate(intelligence,d,31), "Minimum deployment dwell avoids rapid packing");
            checks+=2;
        }
        System.out.println("PASS: flight physics and artillery tactics: "+checks+" bounded control / glide / hysteresis checks");
        h.succeed();
    }

    @GameTest(template="artillery_probe", batch="smart_artillery", timeoutTicks=440)
    public static void smartArtillery(GameTestHelper h) {
        ServerLevel level=h.getLevel();
        BlockPos origin=h.absolutePos(new BlockPos(22,1,6));
        floor(level,origin);
        var owner=FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"smart-probe"));
        owner.moveTo(origin.getX()-1.5,origin.getY(),origin.getZ()+.5,-90,0);
        Cat cat=cat(level,owner,origin,CatOutfitType.ENGINEERING);
        var genes=CatAttributeData.ensure(cat).withValues(CatStat.INTELLIGENCE,100,100);
        CatAttributeData.set(cat,genes);
        CareerCatBehavior.tick(cat); CatCombatControl.tick(cat);
        var enemy=EntityType.IRON_GOLEM.create(level);
        enemy.setNoAi(true);
        enemy.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(5000);
        enemy.setHealth(5000);
        enemy.moveTo(cat.getX()+10,cat.getY(),cat.getZ(),0,0);level.addFreshEntity(enemy);
        // Forge may accept addFreshEntity before its chunk's UUID lookup is visible.
        // Real clients cannot aim at an untracked entity; wait for the same precondition in this headless fixture.
        cat.setNoAi(true);
        h.startSequence().thenWaitUntil(()->h.assertTrue(level.getEntity(enemy.getUUID())==enemy
                &&level.getEntity(cat.getUUID())==cat,"Waiting for fixture entities to become visible"))
                .thenExecute(()->{
        cat.setNoAi(false);
        h.assertTrue(CatLaserCommands.issue(owner,new CatLaserTargeting.Aim(enemy.position(),enemy))==1,"Laser selects intelligent cat");
        var old=new java.util.concurrent.atomic.AtomicReference<EngineeringCannon>();
        h.runAfterDelay(90,()->{
            h.assertTrue(CatEngineeringCombat.deployed(cat),"Intelligent cat settles and fires instead of retreating forever; age="+cat.getAge()+", pos="+cat.position()+", target="+cat.getTarget()+", canDeploy="+CatEngineeringCombat.canDeploy(cat));
            var cannon=(EngineeringCannon)cat.getVehicle();
            double distance=cannon.position().distanceTo(enemy.position());
            h.assertTrue(distance>=13&&distance<=19,"Open-field deployment is near half range, actual="+distance);
            h.assertTrue(enemy.getHealth()<5000,"Intelligent cat actually attacks");
            old.set(cannon);
            enemy.teleportTo(cannon.getX()+4,cannon.getY(),cannon.getZ());
        });
        h.runAfterDelay(130,()->h.assertTrue(old.get().isRemoved(),"Intelligent cat packs when an enemy enters 8 blocks"));
        h.runAfterDelay(195,()->{
            h.assertTrue(CatEngineeringCombat.deployed(cat),"Re-deploys after retreat");
            var cannon=(EngineeringCannon)cat.getVehicle();
            h.assertTrue(cannon.position().distanceTo(enemy.position())>old.get().position().distanceTo(enemy.position())+4,"Retreat meaningfully increases distance, including diagonal paths; old="+old.get().position()+", new="+cannon.position()+", enemy="+enemy.position());
            old.set(cannon);
            enemy.teleportTo(cannon.getX()+29,cannon.getY(),cannon.getZ());
        });
        h.runAfterDelay(235,()->h.assertTrue(old.get().isRemoved(),"Packs beyond 24 even though still within the 32-block weapon range"));
        h.runAfterDelay(305,()->{
            h.assertTrue(CatEngineeringCombat.deployed(cat),"Re-deploys after closing on distant enemy");
            h.assertTrue(cat.getVehicle().position().distanceTo(enemy.position())<old.get().position().distanceTo(enemy.position())-4,"Approach meaningfully reduces distance, including diagonal paths");
            CatEngineeringCombat.release(cat);cat.discard();enemy.discard();owner.discard();
            System.out.println("PASS: smart artillery: real vanilla AI retreats, deploys at half range, fires, approaches and re-deploys");
            h.succeed();
        });
        });
    }

    @GameTest(template="accessory_probe", batch="pilot_legacy_inventory", timeoutTicks=20)
    public static void legacyPilotInventory(GameTestHelper h) {
        var level=h.getLevel();
        var origin=h.absolutePos(new BlockPos(2,1,2));
        var connectionStub=FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"legacy-connection"));
        var owner=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"legacy-pilot"));
        owner.connection=connectionStub.connection;
        owner.moveTo(origin.getX()+.5,origin.getY(),origin.getZ()+.5,0,0);
        owner.setShiftKeyDown(true);
        var hand=net.minecraft.world.InteractionHand.MAIN_HAND;
        var empty=net.minecraft.world.item.ItemStack.EMPTY;
        owner.setItemInHand(hand,empty);
        Cat cat=cat(level,owner,origin,CatOutfitType.FLIGHT);
        cat.setNoAi(true);
        var initial=cat.getPersistentData().copy();
        var noItems=new net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract(owner,hand,cat);
        CommonEvents.onCatInteract(noItems);
        h.assertTrue(noItems.isCanceled()&&owner.containerMenu==owner.inventoryMenu,
                "Empty pilot consumes sneak gesture without opening or toggling sit");
        h.assertTrue(initial.equals(cat.getPersistentData()),"Checking empty storage never creates or rewrites saved data");

        var sword=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
        sword.setDamageValue(17);
        var inventory=CatChestData.openContainer(cat);
        inventory.setItem(0,sword);
        inventory.setItem(26,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,7));
        var savedItems=cat.getPersistentData().get(CatChestData.ITEMS_TAG).copy();
        Cat restored=EntityType.CAT.create(level);
        restored.load(cat.saveWithoutId(new CompoundTag()));
        h.assertTrue(CatChestData.openContainer(restored).countItem(net.minecraft.world.item.Items.DIAMOND)==7,
                "Legacy contents survive entity save/reload, including original final slot");
        restored.discard();

        // Empty hand, ordinary item and sneaking with a wrench must all reach
        // the same compatibility menu without starting a passenger flight.
        for(var held:new net.minecraft.world.item.ItemStack[]{empty,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE),
                com.simibubi.create.AllItems.WRENCH.asStack()}) {
            owner.setItemInHand(hand,held);
            CommonEvents.onCatInteract(new net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract(owner,hand,cat));
            h.assertTrue(owner.containerMenu instanceof net.minecraft.world.inventory.ChestMenu,
                    "Nonempty legacy backpack opens for empty hand / held item / sneaking wrench");
            var menu=(net.minecraft.world.inventory.ChestMenu)owner.containerMenu;
            h.assertTrue(menu.getRowCount()==3&&menu.getContainer().countItem(net.minecraft.world.item.Items.DIAMOND)==7
                    &&menu.getContainer().getItem(0).getDamageValue()==17,"All 27 slots, counts and item metadata retained");
            h.assertTrue(!owner.isPassenger()&&!cat.isPassenger(),"Sneaking wrench never mounts");
            h.assertTrue(savedItems.equals(cat.getPersistentData().get(CatChestData.ITEMS_TAG)),
                    "Opening compatibility menu never rewrites legacy NBT");
            owner.closeContainer();
        }
        connectionStub.setShiftKeyDown(true);
        var denied=new net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract(connectionStub,hand,cat);
        CommonEvents.onCatInteract(denied);
        h.assertTrue(!denied.isCanceled()&&connectionStub.containerMenu==connectionStub.inventoryMenu,
                "A different player cannot open the pilot backpack");

        owner.setItemInHand(hand,empty);
        CommonEvents.onCatInteract(new net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract(owner,hand,cat));
        var menu=(net.minecraft.world.inventory.ChestMenu)owner.containerMenu;
        for(int slot=0;slot<27;slot++) menu.quickMoveStack(owner,slot);
        h.assertTrue(CatChestData.openContainer(cat).isEmpty()
                &&owner.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND)==7
                &&owner.getInventory().countItem(net.minecraft.world.item.Items.IRON_SWORD)==1,
                "Shift-click recovers every item exactly once and persists the empty state");
        owner.closeContainer();
        for(var held:new net.minecraft.world.item.ItemStack[]{empty,com.simibubi.create.AllItems.WRENCH.asStack()}) {
            owner.setItemInHand(hand,held);
            CommonEvents.onCatInteract(new net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract(owner,hand,cat));
            h.assertTrue(owner.containerMenu==owner.inventoryMenu,"Emptied and closed pilot backpack cannot reopen");
        }
        owner.setItemInHand(hand,empty);
        CatClothesData.equip(cat,CatOutfitType.TRANSPORT);
        CommonEvents.onCatInteract(new net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract(owner,hand,cat));
        h.assertTrue(owner.containerMenu instanceof CatPackageMenu,"Empty logistics cat retains its normal package menu");
        owner.closeContainer();
        cat.discard();owner.discard();connectionStub.discard();
        System.out.println("PASS: legacy pilot inventory: server interaction, 27 slots, NBT/reload, all gestures, ownership, shift recovery, empty lockout, logistics unaffected");
        h.succeed();
    }

    @GameTest(template="artillery_probe", batch="pilot_passenger", timeoutTicks=120)
    public static void passengerFlight(GameTestHelper h) {
        ServerLevel level=h.getLevel();
        BlockPos ground=h.absolutePos(new BlockPos(22,1,6));
        floor(level,ground);
        // FakePlayer rejects riding on NeoForge; use an ordinary player with only the headless connection stubbed.
        var connectionStub=FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"flight-connection"));
        var owner=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"pilot-probe"));
        owner.connection=connectionStub.connection;
        BlockPos origin=ground.above(4);
        owner.moveTo(origin.getX()+.5,origin.getY(),origin.getZ()+.5,-90,0);
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,com.simibubi.create.AllItems.WRENCH.asStack());
        Cat cat=cat(level,owner,origin,CatOutfitType.FLIGHT);
        h.assertTrue(cat.getMaxHealth()==60
                        && cat.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)==17
                        && cat.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS)==5.5,
                "Pilot melee survival: actual 50-stat entity has 60 health / 17 armor / 5.5 toughness");
        long capacity=CatPilotFlight.duration(cat);
        h.assertTrue(capacity==350,"Effective stamina yields expected flight duration");
        cat.getPersistentData().putLong(CatPilotFlight.USED,capacity-3);
        h.assertTrue(CatPilotFlight.interact(cat,owner,net.minecraft.world.InteractionHand.MAIN_HAND).consumesAction(),"Real wrench interaction mounts owned adult cat");
        h.assertTrue(owner.getVehicle()==cat.getVehicle()&&cat.getVehicle() instanceof CatFlightCarrier,"Cat and player share a server-controlled root");
        var carrier=(CatFlightCarrier)cat.getVehicle();
        h.assertTrue(Math.abs(cat.getY()-owner.getY()-CatFlightCarrier.CAT_HEIGHT)<1e-6,"Player hangs below the cat");
        h.assertTrue(Math.abs(cat.getY()-owner.getY()-2.5)<1e-6
                &&cat.getBoundingBox().maxY<=carrier.getBoundingBox().maxY,
                "Calibrated single-ring height remains fully inside the carrier collision box");
        h.assertTrue(!CareerCatBehavior.canParticipateInCombat(cat)&&carrier.getControllingPassenger()==null,"Carrying pauses combat and rejects vanilla vehicle authority");
        CompoundTag saved=new CompoundTag();
        h.assertTrue(carrier.saveAsPassenger(saved),"Carrier root is saveable");
        var restored=EntityType.loadEntityRecursive(saved,level,e->e);
        h.assertTrue(restored instanceof CatFlightCarrier&&((CatFlightCarrier)restored).cat()!=null,"Passenger cat survives root save/reload");
        Cat loaded=((CatFlightCarrier)restored).cat();
        h.assertTrue(loaded.getPersistentData().getLong(CatPilotFlight.USED)==capacity-3,"Used stamina survives save");
        restored.tick();
        h.assertTrue(!loaded.isRemoved()&&!loaded.isPassenger(),"Orphan root releases the saved cat safely");
        h.runAfterDelay(10,()->{
            h.assertTrue(cat.getVehicle()==carrier&&carrier.gliding(),"Exhaustion switches to glide without detaching");
            h.assertTrue(carrier.getDeltaMovement().y<0,"Glide descends even after powered ascent");
            h.assertTrue(cat.getPersistentData().getLong(CatPilotFlight.USED)==capacity&&carrier.seconds()==0,"Fuel stops at exhaustion");
            carrier.input(owner,1,0,-90,-90,true,false);
        });
        h.runAfterDelay(14,()->{
            h.assertTrue(carrier.getDeltaMovement().y<0,"Jump and looking up cannot regain powered flight");
            carrier.input(owner,Float.NaN,0,0,0,true,true);
        });
        h.runAfterDelay(25,()->{
            h.assertTrue(carrier.position().distanceToSqr(Vec3.atBottomCenterOf(origin))<200,"Invalid packets never teleport the carrier");
            carrier.setPos(ground.getX()+.5,ground.getY()+.05,ground.getZ()+.5);
            carrier.setDeltaMovement(0,-.2,0);
        });
        h.runAfterDelay(50,()->{
            h.assertTrue(carrier.isRemoved()&&!cat.isPassenger()&&!owner.isPassenger(),"Ground landing releases both passengers");
            h.assertTrue(cat.isAlive()&&owner.isAlive(),"Landing preserves cat and player");
            long used=cat.getPersistentData().getLong(CatPilotFlight.USED);
            h.assertTrue(used>0&&used<capacity,"Ground rest recovers gradually, not a full reset");
            // The test covers remounting, not random post-landing wandering into fixture walls.
            // Reuse the cleared takeoff cell without changing recovered stamina or eligibility.
            cat.moveTo(ground.getX()+.5,ground.getY(),ground.getZ()+.5,0,0);
            cat.setDeltaMovement(Vec3.ZERO);
            owner.moveTo(cat.getX(),cat.getY(),cat.getZ(),-90,0);
            h.assertTrue(CatPilotFlight.start(cat,owner),"Can take off again after landing");
            h.assertTrue(cat.getPersistentData().getLong(CatPilotFlight.USED)==used,"Remounting does not refill endurance");
            CatClothesData.equip(cat,CatOutfitType.MEDICAL);
            h.assertTrue(!cat.isPassenger()&&!owner.isPassenger()&&cat.isAlive(),"Outfit change releases both without losing cat");
            h.assertTrue(!owner.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING),"Emergency detach must not grant slow falling");
            CatClothesData.equip(cat,CatOutfitType.FLIGHT);
            cat.getPersistentData().putInt(CatPilotFlight.USED,12000);
            cat.setOnGround(true);
            CatPilotFlight.recover(cat);
            h.assertTrue(cat.getPersistentData().getLong(CatPilotFlight.USED)==capacity-2,"Legacy long-duration fuel clamps before recovery");
            owner.moveTo(cat.getX(),cat.getY(),cat.getZ(),-90,0);
            h.assertTrue(CatPilotFlight.start(cat,owner),"Can mount after legacy fuel migration");
            owner.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOW_FALLING,80));
            ((CatFlightCarrier)cat.getVehicle()).release(true);
            h.assertTrue(owner.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING),"Do not remove unrelated potion effects");
            cat.discard();owner.discard();connectionStub.discard();
            System.out.println("PASS: passenger flight: actual wrench mount, real player, fuel/glide, landing/rest, save/reload, outfit cleanup");
            h.succeed();
        });
    }

    @GameTest(template="artillery_probe", batch="auto_artillery", timeoutTicks=260)
    public static void autoArtillery32(GameTestHelper h) {
        var level=h.getLevel();
        var origin=h.absolutePos(new BlockPos(22,1,6));
        floor(level,origin);
        // No real player exists in a headless test. Keep the entire shot lane entity-ticking,
        // including chunks the cat enters while staging, just as a nearby test player would.
        var forcedLane=new java.util.ArrayList<net.minecraft.world.level.ChunkPos>();
        for(int x=Math.floorDiv(origin.getX()-20,16);x<=Math.floorDiv(origin.getX()+45,16);x++)
            for(int z=Math.floorDiv(origin.getZ()-5,16);z<=Math.floorDiv(origin.getZ()+5,16);z++){
                var chunk=new net.minecraft.world.level.ChunkPos(x,z);
                if(!level.getForcedChunks().contains(chunk.toLong())){
                    level.setChunkForced(x,z,true);forcedLane.add(chunk);
                }
            }
        // Reused GameTest worlds retain entities after a failed assertion. Clear only this
        // autonomous arena before selecting targets, never entities in player saves.
        var arena=new net.minecraft.world.phys.AABB(origin.offset(-20,-1,-5),origin.offset(46,9,6));
        var stale=level.getEntities((net.minecraft.world.entity.Entity)null,arena,
                entity->!(entity instanceof net.minecraft.server.level.ServerPlayer));
        if(!stale.isEmpty()) System.out.println("INFO: clearing "+stale.size()+" stale autonomous-arena test entities");
        stale.forEach(net.minecraft.world.entity.Entity::discard);
        var owner=FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"auto-probe"));
        owner.moveTo(origin.getX()-.5,origin.getY(),origin.getZ()+.5,-90,0);
        var cat=cat(level,owner,origin,CatOutfitType.ENGINEERING);
        CatAttributeData.set(cat,CatAttributeData.ensure(cat).withValues(CatStat.INTELLIGENCE,100,100));
        CareerCatBehavior.tick(cat);CatCombatControl.tick(cat);
        CatCombatPreferences.get(level.getServer()).setAggressive(owner.getUUID(),true);
        var enemy=EntityType.HUSK.create(level);
        enemy.setNoAi(true);
        enemy.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
        enemy.setHealth(1000);
        enemy.moveTo(cat.getX()+28,cat.getY(),cat.getZ(),0,0);level.addFreshEntity(enemy);
        h.succeedWhen(()->{
            h.assertTrue(cat.getTarget()==enemy&&CatEngineeringCombat.deployed(cat),
                    "Autonomous target beyond 16 blocks persists at half-range staging: target="+cat.getTarget()
                            +", enemy="+enemy+", cat="+cat.position()+", sitting="+cat.isOrderedToSit()
                            +", vehicle="+cat.getVehicle()+", enemyHealth="+enemy.getHealth());
            h.assertTrue(enemy.getHealth()<1000,"32-block automatic acquisition leads to a real hit, with no laser order; cat="+cat.position()
                            +", target="+enemy.position()+", targetMotion="+enemy.getDeltaMovement()
                            +", shots="+cat.getPersistentData().getInt("LaoWuArtilleryShot")+", catTicks="+cat.tickCount+", cannonTicks="+cat.getVehicle().tickCount
                            +", cannon="+cat.getVehicle().position()
                            +", clear="+((cn.laowu.mod.entity.EngineeringCannon)cat.getVehicle()).hasClearShot(enemy));
            double distance=cat.getVehicle().position().distanceTo(enemy.position());
            // The 48-tick fallback may fire once before repositioning even beyond OUTER. Verify progress from the initial 28-block distance and a real hit; smartArtillery separately tests preferred-range staging and relocation.
            h.assertTrue(distance>=13&&distance<28,"Automatic combat approaches before its bounded fallback shot: "+distance);
            CatCombatPreferences.get(level.getServer()).setAggressive(owner.getUUID(),false);
            CatEngineeringCombat.release(cat);cat.discard();enemy.discard();owner.discard();
            for(var c:forcedLane)level.setChunkForced(c.x,c.z,false);
            System.out.println("PASS: autonomous artillery: acquires 28-block hostile, keeps target at half range and fires");
        });
    }
    private static Cat cat(ServerLevel level,net.minecraft.server.level.ServerPlayer owner,BlockPos origin,CatOutfitType outfit) {
        Cat cat=new Cat(EntityType.CAT,level) {
            @Override public net.minecraft.world.entity.LivingEntity getOwner(){return owner;}
        };
        cat.setTame(true);
        cat.setOwnerUUID(owner.getUUID());cat.setAge(0);
        cat.moveTo(origin.getX()+.5,origin.getY(),origin.getZ()+.5,-90,0);
        level.addFreshEntity(cat);
        CatTraitData.set(cat,CatTraitProfile.EMPTY);
        cat.setAge(0); // Joining may randomly assign LOLI before the fixture clears traits.
        var genes=CatAttributeData.ensure(cat);
        for(var stat:CatStat.values()) genes=genes.withValues(stat,50,100);
        CatAttributeData.set(cat,genes);
        CatPoseData.setPose(cat,CatPoseData.NORMAL);CatClothesData.equip(cat,outfit);
        return cat;
    }
    private static void floor(ServerLevel level,BlockPos origin) {
        // Headless test servers have no player simulation tickets. Physics and mounted AI
        // must tick across the entire movement/shot lane, independent of batch placement.
        // These tickets belong only to the disposable GameTestServer and end with that process.
        if(level.getServer() instanceof net.minecraft.gametest.framework.GameTestServer)
            for(int x=Math.floorDiv(origin.getX()-20,16);x<=Math.floorDiv(origin.getX()+45,16);x++)
                for(int z=Math.floorDiv(origin.getZ()-5,16);z<=Math.floorDiv(origin.getZ()+5,16);z++)
                    level.setChunkForced(x,z,true);
        // This isolated test world is reused between runs; a failed run must not leave blocking cannons/pets.
        var bounds=new net.minecraft.world.phys.AABB(Vec3.atLowerCornerOf(origin.offset(-20,0,-5)),Vec3.atLowerCornerOf(origin.offset(46,10,6)));
        for(var entity:level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,bounds,
                e -> !(e instanceof net.minecraft.server.level.ServerPlayer))) entity.discard();
        for(int x=-20;x<=45;x++) for(int z=-5;z<=5;z++) {
            level.setBlockAndUpdate(origin.offset(x,-1,z),Blocks.STONE.defaultBlockState());
            for(int y=0;y<=8;y++)level.setBlockAndUpdate(origin.offset(x,y,z),Blocks.AIR.defaultBlockState());
        }
    }
}
