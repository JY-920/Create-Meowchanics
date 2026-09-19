package cn.laowu.mod.test;

import cn.laowu.mod.*;
import cn.laowu.mod.accessory.*;
import cn.laowu.mod.api.*;
import cn.laowu.mod.entity.MechanicalLaserProjectile;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class AccessoryIntegrationProbe {
    private static float mitigated(Cat cat,net.minecraft.world.damagesource.DamageSource source,float amount) {
        return CatAccessoryHooks.beforeDamage(cat,source,CatAccessories.mitigateMovingDamage(cat,source,amount));
    }
    private static ResourceLocation id(String value) { return ResourceLocation.parse(value); }
    @GameTest(template = "artillery_probe", batch = "artillery", timeoutTicks = 220)
    public static void engineeringArtillery(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(22, 1, 6));
        for (int x = -20; x <= 45; x++) for (int z = -5; z <= 5; z++) {
            level.setBlockAndUpdate(origin.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            for (int y = 0; y <= 4; y++) level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        // A headless fixture has no negotiated client connection. Only owner lookup is stubbed;
        // the full vanilla/career goal set and real laser command implementation remain intact.
        var owner = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "artillery-probe"));
        owner.moveTo(origin.getX() - 1.5, origin.getY(), origin.getZ() + 0.5, -90, 0);
        Cat cat = new Cat(EntityType.CAT, level) {
            @Override public net.minecraft.world.entity.LivingEntity getOwner() { return owner; }
        };
        cat.setTame(true, false);
        cat.setOwnerUUID(owner.getUUID());
        cat.moveTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, -90, 0);
        level.addFreshEntity(cat);
        cn.laowu.mod.genetics.CatTraitData.set(cat, cn.laowu.mod.genetics.CatTraitProfile.EMPTY);
        var genes = cn.laowu.mod.genetics.CatAttributeData.ensure(cat);
        for (var stat : cn.laowu.mod.genetics.CatStat.values()) genes = genes.withValues(stat, 50, 100);
        cn.laowu.mod.genetics.CatAttributeData.set(cat, genes);
        cat.setAge(0);
        CatPoseData.setPose(cat, CatPoseData.NORMAL);
        CatClothesData.equip(cat, CatOutfitType.ENGINEERING);
        CareerCatBehavior.tick(cat);
        CatCombatControl.tick(cat);
        helper.assertTrue(cat.goalSelector.getAvailableGoals().stream()
                        .filter(goal -> goal.getGoal() instanceof CatEngineeringCombat).count() == 1,
                "Career setup installs artillery once alongside the original cat AI");
        var enemy = EntityType.IRON_GOLEM.create(level);
        enemy.setNoAi(true);
        enemy.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
        enemy.setHealth(1000);
        enemy.moveTo(cat.getX() + 8, cat.getY(), cat.getZ(), 0, 0);
        level.addFreshEntity(enemy);
        helper.assertTrue(CatLaserCommands.issue(owner, new CatLaserTargeting.Aim(enemy.position(), enemy)) == 1,
                "Real laser command selects the engineer for combat");
        var cannonRef = new java.util.concurrent.atomic.AtomicReference<cn.laowu.mod.entity.EngineeringCannon>();
        var damageAfterFirst = new java.util.concurrent.atomic.AtomicReference<Float>();
        var pursuitStart = new java.util.concurrent.atomic.AtomicReference<net.minecraft.world.phys.Vec3>();
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(CatEngineeringCombat.deployed(cat), "Deploys against an enemy within 32 blocks");
            helper.assertTrue(cn.laowu.mod.genetics.CatAttributeEffects.effectiveValue(cat, cn.laowu.mod.genetics.CatStat.ATTACK) == 50
                            && cn.laowu.mod.genetics.CatAttributeEffects.effectiveValue(cat, cn.laowu.mod.genetics.CatStat.LUCK) == 60
                            && cn.laowu.mod.genetics.CatAttributeEffects.effectiveValue(cat, cn.laowu.mod.genetics.CatStat.INTELLIGENCE) == 50,
                    "Engineer combat / critical stats include the configured suit bonuses");
            var cannon = (cn.laowu.mod.entity.EngineeringCannon)cat.getVehicle();
            cannonRef.set(cannon);
            helper.assertTrue(CatLaserCommands.issue(owner, new CatLaserTargeting.Aim(enemy.position(), enemy)) == 1
                            && cat.getVehicle() == cannon, "Laser retargeting keeps the mounted cannon");
            helper.assertTrue(Math.abs(cat.getY() - cannon.getY() - cn.laowu.mod.entity.EngineeringCannon.SEAT_TOP) < 0.15,
                    "Cat feet are at the real cannon cushion height");
            helper.assertTrue(!cat.isOrderedToSit() && CatEngineeringCombat.canReceiveOrders(cat)
                            && CareerCatBehavior.canParticipateInCombat(cat), "Mounted cat remains commandable and combat-ready");
            var friendly = EntityType.CAT.create(level);
            friendly.setTame(true, false); friendly.setOwnerUUID(java.util.UUID.randomUUID());
            helper.assertTrue(!cannon.fire(cat, friendly), "Team-safe cannon rejects friendly cats");
            var wall = BlockPos.containing(cannon.pivot().add(3, 0, 0));
            level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(wall.above(), Blocks.STONE.defaultBlockState());
            helper.assertTrue(!cannon.hasClearShot(enemy) && !cannon.fire(cat, enemy), "Barrel cannot fire through a wall");
            level.setBlockAndUpdate(wall, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(wall.above(), Blocks.AIR.defaultBlockState());
        });
        helper.runAfterDelay(34, () -> {
            helper.assertTrue(enemy.getHealth() < 1000, "Real gear projectile deals damage");
            damageAfterFirst.set(enemy.getHealth());
            var cannon = cannonRef.get();
            enemy.teleportTo(cannon.getX() + 0.2, cannon.getY(), cannon.getZ());
            enemy.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        });
        helper.runAfterDelay(42, () -> helper.assertTrue(cat.getVehicle() == cannonRef.get(),
                "Point-blank enemy never makes the engineer abandon the cannon"));
        helper.runAfterDelay(112, () -> {
            helper.assertTrue(enemy.getHealth() < damageAfterFirst.get(), "Point-blank gear shot still hits; does not spawn behind target");
            var cannon = cannonRef.get();
            helper.assertTrue(cannon.position().distanceToSqr(net.minecraft.world.phys.Vec3.atBottomCenterOf(origin)
                            .add(cn.laowu.mod.entity.EngineeringCannon.SEAT_BACK, 0, 0)) < 0.1,
                    "Deployed cannon stays fixed rather than kiting");
            enemy.teleportTo(cannon.getX() + 43, cannon.getY(), cannon.getZ());
        });
        helper.runAfterDelay(116, () -> {
            helper.assertTrue(!CatEngineeringCombat.deployed(cat), "Packs immediately beyond range");
            pursuitStart.set(cat.position());
        });
        helper.runAfterDelay(128, () -> {
            // Path refresh may finish between these callbacks; test actual displacement instead.
            helper.assertTrue(!CatEngineeringCombat.deployed(cat) && cat.position().distanceToSqr(pursuitStart.get()) > 0.04,
                    "Beyond 32 blocks the cannon is packed and cat pursues: mounted="+cat.isPassenger()
                            +", target="+(cat.getTarget()==enemy)+", distance="+cat.distanceTo(enemy)
                            +", grounded="+cat.onGround()+", pos="+cat.position()+", navDone="+cat.getNavigation().isDone());
            enemy.teleportTo(cat.getX() + 8, origin.getY(), cat.getZ());
        });
        helper.runAfterDelay(142, () -> {
            helper.assertTrue(CatEngineeringCombat.deployed(cat), "Re-enters cannon mode after regaining range");
            var cannon = (cn.laowu.mod.entity.EngineeringCannon)cat.getVehicle();
            CompoundTag saved = new CompoundTag();
            helper.assertTrue(cannon.saveAsPassenger(saved), "Cannon is saveable, not an unsaved temporary vehicle");
            var passengers = saved.getList("Passengers", net.minecraft.nbt.Tag.TAG_COMPOUND);
            helper.assertTrue(passengers.size() == 1 && passengers.getCompound(0).getUUID("UUID").equals(cat.getUUID()),
                    "World save includes the original passenger cat");
            var restored = EntityType.loadEntityRecursive(saved, level, entity -> entity);
            helper.assertTrue(restored instanceof cn.laowu.mod.entity.EngineeringCannon
                            && restored.getFirstPassenger() instanceof Cat, "Reload restores cannon and cat together");
            Cat loadedCat = (Cat)restored.getFirstPassenger();
            helper.assertTrue(loadedCat.getOwnerUUID().equals(cat.getOwnerUUID())
                            && CatClothesData.getOutfit(loadedCat) == CatOutfitType.ENGINEERING,
                    "Owner and suit survive the mounted save");
            loadedCat.setTarget(null);
            restored.tick();
            helper.assertTrue(!loadedCat.isRemoved() && !loadedCat.isPassenger(),
                    "After reload without a combat target the cannon releases, never deletes, its cat");
            cat.setOrderedToSit(true);
        });
        helper.runAfterDelay(150, () -> {
            helper.assertTrue(!CatEngineeringCombat.deployed(cat), "Player stay command exits artillery");
            cat.setOrderedToSit(false);
            cat.setInSittingPose(false);
            CatLaserCommands.issue(owner, new CatLaserTargeting.Aim(enemy.position(), enemy));
        });
        helper.runAfterDelay(162, () -> {
            helper.assertTrue(CatEngineeringCombat.deployed(cat), "Can deploy again after cancelling stay");
            CatClothesData.equip(cat, CatOutfitType.MEDICAL);
            helper.assertTrue(!cat.isPassenger(), "Changing outfit immediately releases the cannon");
        });
        helper.runAfterDelay(180, () -> {
            helper.assertTrue(level.getEntitiesOfClass(cn.laowu.mod.entity.EngineeringCannon.class,
                    new net.minecraft.world.phys.AABB(origin).inflate(32)).isEmpty(), "No orphan cannons remain");
            helper.assertTrue(level.getBlockState(origin.below()).is(Blocks.STONE), "Terrain is unchanged");
            cat.discard(); enemy.discard(); owner.discard();
            System.out.println("PASS: engineering artillery: actual gear hits, point blank, 32-block pursuit, safe save/reload, team/wall checks, sit/outfit cleanup");
            helper.succeed();
        });
    }


    @GameTest(template = "accessory_probe", batch = "accessory", timeoutTicks = 100)
    public static void accessoryPresentation(GameTestHelper helper) {
        var parameters = new CreativeModeTab.ItemDisplayParameters(
                net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS, true, helper.getLevel().registryAccess());
        var tab = LaoWuMod.CAT_ACCESSORIES_TAB.get();
        tab.buildContents(parameters);
        LaoWuMod.CAT_PROGRESSION_TAB.get().buildContents(parameters);
        for (String career : new String[]{"engineering", "medical", "music"}) {
            var complete = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id("laowu:"+career+"_suit"));
            var incomplete = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id("laowu:incomplete_"+career+"_suit"));
            helper.assertTrue(incomplete != net.minecraft.world.item.Items.AIR, "Keep incomplete suit registry / save compatibility");
            helper.assertTrue(LaoWuMod.CAT_PROGRESSION_TAB.get().getDisplayItems().stream().noneMatch(stack -> stack.is(incomplete)),
                    "Hide new intermediate exactly like older suits: "+career);
            helper.assertTrue(LaoWuMod.CAT_PROGRESSION_TAB.get().getDisplayItems().stream().anyMatch(stack -> stack.is(complete)),
                    "Keep completed suit visible: "+career);
        }
        helper.assertTrue(tab.getDisplayItems().size() == 36 && tab.getSearchTabDisplayItems().size() == 36,
                "Dedicated tab lists 36 distinct accessories in display and search");
        helper.assertTrue(tab.getIconItem().is(BuiltInRegistries.ITEM.get(id("laowu:cat_taunt_bell"))),
                "Creative icon is the redrawn bell");
        var marker = net.minecraft.network.chat.Component.translatable("cat_accessory.laowu.label");
        var name = net.minecraft.network.chat.Component.literal("Accessory name");
        var footer = net.minecraft.network.chat.Component.literal("example:item_id / NBT: 2 / #example:tag");
        for (var definition : CatAccessoryItems.DEFAULTS.values()) {
            Item item = BuiltInRegistries.ITEM.get(id(definition.item()));
            helper.assertTrue(tab.getDisplayItems().stream().filter(stack -> stack.is(item)).count() == 1,
                    "Exactly one tab entry for " + definition.item());
            helper.assertTrue(LaoWuMod.CAT_PROGRESSION_TAB.get().getDisplayItems().stream().noneMatch(stack -> stack.is(item)),
                    "Accessory removed from progression tab " + definition.item());
            // Simulate an earlier tooltip provider adding our marker before technical metadata.
            var lines = new java.util.ArrayList<net.minecraft.network.chat.Component>(java.util.List.of(name, marker, footer));
            CatAccessoryTooltip.append(new ItemStack(item), lines);
            helper.assertTrue(lines.get(0) == name && lines.get(lines.size() - 1) == footer,
                    "Item name and existing footers are preserved");
            helper.assertTrue(lines.size() >= 4 && !lines.get(1).getString().equals(marker.getString())
                            && lines.get(lines.size() - 2).getString().equals(marker.getString()),
                    "All effect lines precede the accessory label and technical footer");
            helper.assertTrue(lines.stream().filter(line -> line.getString().equals(marker.getString())).count() == 1,
                    "No duplicate accessory marker");
        }
        var ordinary = new java.util.ArrayList<net.minecraft.network.chat.Component>(java.util.List.of(name, footer));
        CatAccessoryTooltip.append(new ItemStack(Items.DIRT), ordinary);
        helper.assertTrue(ordinary.equals(java.util.List.of(name, footer)), "Unrelated items are untouched");
        var original = CatAccessoryRegistry.networkData();
        try {
            for (boolean enabled : new boolean[]{true, false}) {
                var payload = original.copy();
                var definition = new CompoundTag();
                definition.putString("Id", "example:tooltip_probe");
                definition.putString("Json", "{\"schema_version\":1,\"item\":\"minecraft:stick\",\"enabled\":" + enabled
                        + ",\"description\":\"Custom scripted effect\",\"required_outfit\":\"flight\","
                        + "\"charge\":{\"capacity\":8,\"initial\":3},\"effects\":{\"health\":10}}");
                payload.getList("Definitions", net.minecraft.nbt.Tag.TAG_COMPOUND).add(definition);
                CatAccessoryRegistry.receive(payload);
                var lines = new java.util.ArrayList<net.minecraft.network.chat.Component>(java.util.List.of(name, footer));
                CatAccessoryTooltip.append(new ItemStack(Items.STICK), lines);
                helper.assertTrue(lines.get(0) == name && lines.get(lines.size() - 1) == footer
                                && lines.get(lines.size() - 2).getString().equals(marker.getString()),
                        "External / disabled definition keeps the complete ordered block");
                if (enabled) {
                    helper.assertTrue(lines.get(1).getString().equals("Custom scripted effect") && lines.size() == 7,
                            "Custom description, charge, career restriction and effect are retained");
                } else {
                    helper.assertTrue(lines.size() == 4 && lines.get(1).getString().equals(
                                    net.minecraft.network.chat.Component.translatable("cat_accessory.laowu.disabled").getString()),
                            "Disabled status comes before its label; no stale effects");
                }
            }
        } finally {
            CatAccessoryRegistry.receive(original);
        }
        System.out.println("PASS: accessory presentation: 36 unique creative entries, separate progression tab, ordered normal/custom/disabled tooltips");
        helper.succeed();
    }


    @GameTest(template = "accessory_probe", batch = "accessory", timeoutTicks = 100)
    public static void previewCareersAndButter(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Cat cat = helper.spawn(EntityType.CAT, new BlockPos(1, 1, 1));
        cat.setTame(true, true);
        cat.setOwnerUUID(java.util.UUID.randomUUID());
        CatClothesData.unequip(cat);
        for (CatOutfitType outfit : new CatOutfitType[]{CatOutfitType.ENGINEERING, CatOutfitType.MEDICAL, CatOutfitType.MUSIC}) {
            Item suit = BuiltInRegistries.ITEM.get(id("laowu:" + outfit.id() + "_suit"));
            helper.assertTrue(suit instanceof cn.laowu.mod.item.TerminatorSuitItem, "Registered usable suit " + outfit);
            helper.assertTrue(BuiltInRegistries.ITEM.get(id("laowu:incomplete_" + outfit.id() + "_suit")) != Items.AIR,
                    "Registered intermediate " + outfit);
            CatClothesData.equip(cat, outfit);
            CareerCatBehavior.tick(cat);
            boolean activeCareer = outfit == CatOutfitType.ENGINEERING;
            helper.assertTrue(CareerCatBehavior.canParticipateInCombat(cat) == activeCareer, "Only engineering combat enabled " + outfit);
            helper.assertTrue(ServerConfig.CAREERS.contains(outfit) == !outfit.isPreviewOnly(), "Active combat/support career config " + outfit);
            helper.assertTrue(ServerConfig.careerSetting(outfit, CatSuitSetting.HEALTH) == (activeCareer ? 30 : 6), "Career bonus " + outfit);
            ItemStack pancake = cn.laowu.mod.item.CatPancakeItem.capture(cat);
            helper.assertTrue(cn.laowu.mod.item.CatPancakeItem.getOutfit(pancake) == outfit, "Pancake preserves outfit " + outfit);
            CompoundTag entityData = cat.saveWithoutId(new CompoundTag());
            Cat restored = EntityType.CAT.create(level);
            restored.load(entityData);
            helper.assertTrue(CatClothesData.getOutfit(restored) == outfit, "Entity NBT preserves outfit " + outfit);
        }
        CatClothesData.unequip(cat);
        cat.setNoAi(true);
        var inventory = CatProfileData.openContainer(cat);
        Item butter = CatAccessoryItems.butterReward();
        helper.assertTrue(butter == BuiltInRegistries.ITEM.get(id("laowu:cat_butter_cube")), "Boss reward registry ID");
        inventory.setItem(0, new ItemStack(butter));
        helper.assertTrue(CatAccessoryApi.effectValue(cat, "speed") == 10, "Butter speed bonus");
        var attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 1));
        attacker.setNoAi(true);
        var source = cat.damageSources().mobAttack(attacker);
        cat.setDeltaMovement(0, 0, 0);
        helper.assertTrue(mitigated(cat, source, 10) == 10, "Standing has no mitigation");
        cat.setDeltaMovement(0.1, 0, 0);
        helper.assertTrue(mitigated(cat, source, 10) == 7.5F, "Moving attack damage reduced 25%");
        helper.assertTrue(mitigated(cat, cat.damageSources().fellOutOfWorld(), 10) == 10, "Void not reduced");
        helper.assertTrue(mitigated(cat, cat.damageSources().fall(), 10) == 10, "Environment not reduced");
        cat.setOrderedToSit(true);
        helper.assertTrue(mitigated(cat, source, 10) == 10, "Sitting excludes residual velocity");
        cat.setOrderedToSit(false);
        inventory.setItem(0, ItemStack.EMPTY);
        helper.assertTrue(CatAccessoryApi.effectValue(cat, "speed") == 0
                && mitigated(cat, source, 10) == 10, "Removal clears both effects");
        helper.succeed();
    }

@GameTest(template = "accessory_probe", batch = "accessory", timeoutTicks = 100)
    public static void engineeringCrankWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // Reused GameTest worlds retain entities even when structure blocks are
        // reset. Old engineer/seat occupants can re-seat and drive this crank
        // after the test cat dismounts. Isolate ONLY this five-block fixture.
        var fixture = new net.minecraft.world.phys.AABB(
                net.minecraft.world.phys.Vec3.atLowerCornerOf(helper.absolutePos(BlockPos.ZERO)),
                net.minecraft.world.phys.Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(5, 5, 5))));
        var leftovers = level.getEntities((net.minecraft.world.entity.Entity)null, fixture,
                entity -> !(entity instanceof net.minecraft.world.entity.player.Player));
        System.out.println("Crank fixture stale non-player entities cleared: " + leftovers.size());
        for (var leftover : leftovers) leftover.discard();
        // Keep the crank in the guaranteed ticking origin chunk, even when the
        // test structure starts at x/z=15 and straddles the next chunk.
        BlockPos seatPos = new BlockPos(0, 1, 0), crankPos = seatPos.above();
        // The wider artillery fixtures can place this small fixture across a chunk edge.
        // Forge's GameTest ticket may cover only its origin; keep the actual crank ticking.
        var crankChunk = new net.minecraft.world.level.ChunkPos(helper.absolutePos(crankPos));
        boolean alreadyForced = level.getForcedChunks().contains(crankChunk.toLong());
        level.setChunkForced(crankChunk.x, crankChunk.z, true);
        helper.setBlock(seatPos.below(), Blocks.STONE);
        helper.setBlock(seatPos, BuiltInRegistries.BLOCK.get(id("create:white_seat")).defaultBlockState());
        var block = com.simibubi.create.AllBlocks.HAND_CRANK.get();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            helper.setBlock(crankPos.relative(direction.getOpposite()), Blocks.STONE);
        }
        helper.setBlock(crankPos, block.defaultBlockState().setValue(
                com.simibubi.create.content.kinetics.crank.HandCrankBlock.FACING, Direction.SOUTH));
        Cat cat = helper.spawn(EntityType.CAT, new BlockPos(1, 1, 1));
        cat.setNoAi(true);
        cat.setTame(true, false);
        cat.setOwnerUUID(java.util.UUID.randomUUID());
        CatClothesData.equip(cat, CatOutfitType.ENGINEERING);
        helper.assertTrue(CatEngineeringBehavior.findCrank(cat) == null, "Standing engineer must not operate a crank");
        com.simibubi.create.content.contraptions.actors.seat.SeatBlock.sitDown(level, helper.absolutePos(seatPos), cat);
        var crank = (com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity)
                level.getBlockEntity(helper.absolutePos(crankPos));
        float ordinaryCapacity = crank.calculateAddedStressCapacity();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            helper.setBlock(crankPos, block.defaultBlockState().setValue(
                    com.simibubi.create.content.kinetics.crank.HandCrankBlock.FACING, direction));
            helper.assertTrue(CatEngineeringBehavior.findCrank(cat) == crank, "All four wall-mounted directions");
            helper.assertTrue(CatEngineeringBehavior.facingYaw(crank) == direction.getClockWise().toYRot(),
                    "Side-on working direction");
        }
        helper.setBlock(crankPos, block.defaultBlockState().setValue(
                com.simibubi.create.content.kinetics.crank.HandCrankBlock.FACING, Direction.UP));
        helper.assertTrue(CatEngineeringBehavior.findCrank(cat) == null, "Floor-mounted crank is not the pictured arrangement");
        helper.setBlock(crankPos, block.defaultBlockState().setValue(
                com.simibubi.create.content.kinetics.crank.HandCrankBlock.FACING, Direction.SOUTH));
        CatPoseData.setPose(cat, CatPoseData.PANCAKE);
        helper.assertTrue(CatEngineeringBehavior.findCrank(cat) == null, "Pancakes cannot work");
        CatPoseData.setPose(cat, CatPoseData.NORMAL);
        CatClothesData.equip(cat, CatOutfitType.MEDICAL);
        helper.assertTrue(CatEngineeringBehavior.findCrank(cat) == null, "Other new careers remain inactive");
        CatClothesData.equip(cat, CatOutfitType.ENGINEERING);
        CareerCatBehavior.tick(cat);
        helper.assertTrue(crank.getGeneratedSpeed() == 0, "Reach transition before generating power");
        helper.runAtTickTime(18, () -> {
            helper.assertTrue(crank.inUse > 0, "Real Create crank operated");
            helper.assertTrue(Math.abs(crank.getGeneratedSpeed()) == block.getRotationSpeed(), "Ordinary hand-crank RPM");
            helper.assertTrue(Math.abs(crank.getSpeed()) == block.getRotationSpeed(), "Real kinetic network driven");
            double expectedCapacity = CatCrankPower.stressCapacity(cn.laowu.mod.genetics.CatAttributeEffects.effectiveValue(
                    cat, cn.laowu.mod.genetics.CatStat.STAMINA));
            helper.assertTrue(Math.abs(crank.calculateAddedStressCapacity() * Math.abs(crank.getGeneratedSpeed())
                    - expectedCapacity) < 0.01, "Total SU is 128 per effective Stamina, not per RPM");
            helper.assertTrue(Math.abs(crank.getOrCreateNetwork().calculateCapacity() - expectedCapacity) < 0.01,
                    "Real Create network uses the engineer capacity");
            helper.assertTrue(!CareerCatBehavior.canParticipateInCombat(cat), "Work does not enable career combat");
            System.out.println("Engineer rider Y above seat: " + (cat.getY() - helper.absolutePos(seatPos).getY()));
            crank.turn(true);
        });
        helper.runAtTickTime(23, () -> {
            helper.assertTrue(crank.backwards && crank.inUse > 0, "Retain player-selected reverse direction");
            cat.stopRiding();
            helper.assertTrue(CatEngineeringBehavior.findCrank(cat) == null, "Dismount releases the crank");
            // Move out of the cushion so Create cannot automatically seat the animal again.
            var away = helper.absolutePos(new BlockPos(1, 1, 1));
            cat.teleportTo(away.getX()+0.5, away.getY(), away.getZ()+0.5);
        });
        // Block-entity ticking can catch up a few ticks after a GameTest batch / reload boundary.
        helper.runAtTickTime(38, () -> helper.succeedWhen(() -> {
            helper.assertTrue(crank.calculateAddedStressCapacity() == ordinaryCapacity,
                    "Dismount restores ordinary hand-crank capacity: expected=" + ordinaryCapacity
                            + ", actual=" + crank.calculateAddedStressCapacity() + ", passenger=" + cat.isPassenger());
            helper.assertTrue(crank.getGeneratedSpeed() == 0 && crank.getSpeed() == 0, "Crank naturally stops after release: inUse="
                    +crank.inUse+", generated="+crank.getGeneratedSpeed()+", speed="+crank.getSpeed()+", passenger="+cat.isPassenger()
                    +", sameBE="+(crank==level.getBlockEntity(helper.absolutePos(crankPos)))+", pos="+crank.getBlockPos());
            com.simibubi.create.content.contraptions.actors.seat.SeatBlock.sitDown(level, helper.absolutePos(seatPos), cat);
            CareerCatBehavior.tick(cat);
            helper.setBlock(crankPos, Blocks.AIR);
            helper.assertTrue(CatEngineeringBehavior.findCrank(cat) == null, "Removed crank clears work target");
            helper.assertTrue(CatClothesData.getOutfit(cat) == CatOutfitType.ENGINEERING, "Outfit is not consumed");
            var testSeat = cat.getVehicle();
            cat.stopRiding();
            cat.discard();
            if (testSeat != null) testSeat.discard();
            if (!alreadyForced) level.setChunkForced(crankChunk.x, crankChunk.z, false);
        }));
    }


    @BeforeBatch(batch = "accessory")
    public static void prepare(ServerLevel level) {
        level.getStructureManager().getOrCreate(id("laowu:accessory_probe"))
                .fillFromWorld(level, new BlockPos(0, 319, 0), new Vec3i(5, 5, 5), false, Blocks.STRUCTURE_VOID);
    }
    @GameTest(template = "accessory_probe", batch = "accessory", timeoutTicks = 600)
    public static void scriptsAndPersistence(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        if (!net.neoforged.fml.ModList.get().isLoaded("kubejs")) {
            Cat plain = helper.spawn(EntityType.CAT, new BlockPos(2, 1, 2));
            plain.setNoAi(true);
            var plainInventory = CatProfileData.openContainer(plain);
            plainInventory.setItem(0, new ItemStack(BuiltInRegistries.ITEM.get(id("laowu:cat_attack_badge"))));
            var badge = CatAccessoryApi.accessory(plain, "laowu:cat_attack_badge");
            helper.assertTrue(badge != null && CatAccessoryApi.effectValue(plain, "attack") == 10, "Built-in without KubeJS");
            badge.setStatBonus("attack", 3);
            helper.assertTrue(CatAccessoryApi.effectValue(plain, "attack") == 13, "Core state without script classes");
            plainInventory.setItem(0, ItemStack.EMPTY);
            helper.assertTrue(CatAccessoryApi.effectValue(plain, "attack") == 0, "Removal without KubeJS");
            System.out.println("PASS: accessory real Minecraft integration without KubeJS or Rhino");
            helper.succeed(); return;
        }
        Item item = BuiltInRegistries.ITEM.get(id("kubejs:accessory_probe"));
        helper.assertTrue(item != Items.AIR, "KubeJS startup item registered");
        Cat cat = helper.spawn(EntityType.CAT, new BlockPos(2, 1, 2));
        cat.setNoAi(true);
        var inventory = CatProfileData.openContainer(cat);
        inventory.setItem(0, new ItemStack(item));
        var handle = CatAccessoryApi.accessory(cat, "kubejs:accessory_probe");
        helper.assertTrue(handle != null, "Datapack recognizes custom item");
        helper.assertTrue(handle.number("probe:equip") == 1, "Real KubeJS equip callback exactly once");
        helper.assertTrue(handle.getCharge() == 10, "Initial charge");
        helper.assertTrue(handle.tryActivate("probe:test", 100, 2), "Atomic first activation");
        helper.assertTrue(!handle.tryActivate("probe:test", 100, 2), "Cooldown blocks repeated activation");
        helper.assertTrue(handle.getCharge() == 8, "Failed activation does not consume charge");
        handle.setText("probe:state", "persisted");
        handle.setStatBonus("attack", 8);
        helper.assertTrue(CatAccessoryApi.effectValue(cat, "attack") == 8, "Dynamic stat enters effective attributes");
        ItemStack saved = inventory.removeItemNoUpdate(0);
        inventory.setChanged();
        helper.assertTrue(cat.getTags().contains("probe_unequipped"), "Unequip callback");
        helper.assertTrue(CatAccessoryApi.effectValue(cat, "attack") == 0, "Unequip removes dynamic bonus");
        inventory.setItem(0, saved);
        handle = CatAccessoryApi.accessory(cat, "kubejs:accessory_probe");
        helper.assertTrue(handle.getCharge() == 8 && handle.cooldownRemaining("probe:test") > 0,
                "Unequip/re-equip preserves charge and cooldown");
        helper.assertTrue(handle.text("probe:state").equals("persisted"), "Custom state survives re-equip");
        var victim = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 1, 2));
        victim.setNoAi(true);
        float modified = CatAccessoryHooks.beforeDamage(victim, cat.damageSources().mobAttack(cat), 4);
        helper.assertTrue(modified == 12, "Before attack mutation delivered through KubeJS");
        float incoming = CatAccessoryHooks.beforeDamage(cat, cat.damageSources().mobAttack(victim), 4);
        helper.assertTrue(incoming == 1, "Before hurt mutation delivered through KubeJS");
        double before = handle.number("probe:after_attack");
        victim.hurt(cat.damageSources().mobAttack(cat), 3);
        CatAccessoryHooks.flushAfterDamage();
        helper.assertTrue(handle.number("probe:after_attack") == before + 1, "Actual loader damage event and no recursion");
        MechanicalLaserProjectile projectile = new MechanicalLaserProjectile(level, cat, 2);
        projectile.setDeltaMovement(2, 0, 0);
        helper.assertTrue(CatAccessoryHooks.projectile(cat, victim, projectile), "Projectile event not canceled");
        helper.assertTrue(projectile.getAccessoryDamage() == 7 && projectile.getDeltaMovement().x == 1,
                "Projectile damage and speed setters");
        var gear = new cn.laowu.mod.entity.EngineeringCogwheelProjectile(level, cat, 20);
        gear.setDeltaMovement(2, 0, 0);
        helper.assertTrue(CatAccessoryHooks.projectile(cat, victim, gear)
                        && gear.getAccessoryDamage() == 7 && gear.getDeltaMovement().x == 1,
                "The same KubeJS methods can edit engineer cogwheel damage and speed");
        CatClothesData.equip(cat, CatOutfitType.ENGINEERING);
        var cannon = new cn.laowu.mod.entity.EngineeringCannon(LaoWuMod.ENGINEERING_CANNON.get(), level);
        cannon.setPos(cat.position()); cannon.aimAt(victim);
        level.addFreshEntity(cannon);
        helper.assertTrue(cat.startRiding(cannon, true), "Script probe can mount cannon");
        handle.setText("probe:state", "cancel_shot");
        double shots = handle.number("probe:projectile");
        long gearsBefore = level.getEntitiesOfClass(cn.laowu.mod.entity.EngineeringCogwheelProjectile.class,
                cat.getBoundingBox().inflate(4)).size();
        helper.assertTrue(cannon.fire(cat, victim) && handle.number("probe:projectile") == shots + 1
                        && level.getEntitiesOfClass(cn.laowu.mod.entity.EngineeringCogwheelProjectile.class,
                        cat.getBoundingBox().inflate(4)).size() == gearsBefore,
                "Script-canceled cannon shot consumes reload but adds no projectile");
        handle.setText("probe:state", "persisted");
        cannon.release(); CatClothesData.unequip(cat);
        helper.assertTrue(CatAccessoryHooks.beforeExplosion(cat, 5) == 13, "Explosion event");
        helper.assertTrue(!CatAccessoryApi.damage(cat, cat, 5), "Team-safe helper refuses self damage");
        int chargeBeforeSave = handle.getCharge();
        CompoundTag entityData = cat.saveWithoutId(new CompoundTag());
        Cat copy = EntityType.CAT.create(level);
        copy.load(entityData);
        copy.setUUID(java.util.UUID.randomUUID());
        level.addFreshEntity(copy);
        var restored = CatAccessoryApi.accessory(copy, "kubejs:accessory_probe");
        helper.assertTrue(restored.getCharge() == chargeBeforeSave && restored.text("probe:state").equals("persisted"),
                "Full entity NBT roundtrip preserves charge and state");
        CatClothesData.equip(copy, CatOutfitType.TERMINATOR);
        CatProfileData.dropOnDeath(copy);
        helper.assertTrue(CatAccessoryApi.accessory(copy, "kubejs:accessory_probe") != null,
                "Career death inventory stays available for pancake capture");
        helper.runAfterDelay(45, () -> {
            var current = CatAccessoryApi.accessory(cat, "kubejs:accessory_probe");
            helper.assertTrue(current.number("probe:tick") >= 1, "Real periodic event fires");
            double calls = current.number("probe:before_attack");
            level.getServer().reloadResources(level.getServer().getPackRepository().getSelectedIds())
                    .whenComplete((ignored, error) -> level.getServer().execute(() -> {
                        helper.assertTrue(error == null, "Real server /reload succeeded");
                        var live = CatAccessoryApi.accessory(cat, "kubejs:accessory_probe");
                        float postReload = CatAccessoryHooks.beforeDamage(victim, cat.damageSources().mobAttack(cat), 4);
                        helper.assertTrue(postReload == 12 && live.number("probe:before_attack") == calls + 1,
                                "Reload replaces listeners, never doubles callbacks");
                        helper.assertTrue(live.text("probe:state").equals("persisted"), "Reload preserves per-item state");
                        System.out.println("PASS: accessory real KubeJS/GameTest integration: callbacks, damage, no recursion, charge, persistence, reload");
                        helper.succeed();
                    }));
        });
    }
}
