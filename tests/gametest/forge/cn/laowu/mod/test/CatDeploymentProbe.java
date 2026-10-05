package cn.laowu.mod.test;

import cn.laowu.mod.CatClothesData;
import cn.laowu.mod.CatOutfitType;
import cn.laowu.mod.CatPoseData;
import cn.laowu.mod.CatProfileData;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.CatAttributeData;
import cn.laowu.mod.genetics.CatStat;
import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.genetics.CatTraitData;
import cn.laowu.mod.genetics.CatTraitProfile;
import cn.laowu.mod.item.CatPancakeItem;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.UUID;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class CatDeploymentProbe {
    private static final String NORMAL = "cat_deployment_platform";
    private static final String EJECTING = "cat_ejecting_deployment_platform";
    private static final UUID OWNER = UUID.fromString("6f9a2ef4-6025-41d0-8b24-36d7b10f3751");
    private static final BlockPos SOURCE = new BlockPos(2, 1, 2);

    // Registry lookup/reflection intentionally let the tests compile before the RED implementation exists.
    private static Block block(GameTestHelper h, String id) {
        Block result = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("laowu", id));
        h.assertTrue(result != Blocks.AIR, "Missing deployment platform registration: laowu:" + id);
        h.assertTrue(result.asItem() instanceof BlockItem, "Platform must have its matching BlockItem: " + id);
        return result;
    }

    private static BlockEntity machine(GameTestHelper h, String id) {
        h.setBlock(SOURCE.below(), Blocks.STONE);
        h.setBlock(SOURCE, block(h, id));
        BlockEntity result = h.getLevel().getBlockEntity(h.absolutePos(SOURCE));
        h.assertTrue(result != null && result.getType().isValid(h.getBlockState(SOURCE)),
                "Platform must own a registered valid block entity: " + id);
        h.assertTrue("laowu".equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(result.getType()).getNamespace()),
                "Platform must persist its own block entity type");
        return result;
    }

    private static Object call(BlockEntity machine, String name, Class<?>[] types, Object... values) {
        try {
            return machine.getClass().getMethod(name, types).invoke(machine, values);
        } catch (InvocationTargetException error) {
            if (error.getCause() instanceof RuntimeException cause) throw cause;
            if (error.getCause() instanceof Error cause) throw cause;
            throw new AssertionError("Deployment operation failed: " + name, error.getCause());
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Missing public deployment contract: " + name, error);
        }
    }

    private static Object field(BlockEntity machine, String name) {
        try {
            return machine.getClass().getField(name).get(machine);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Missing public deployment state: " + name, error);
        }
    }

    private static FluidTank tank(BlockEntity machine) { return (FluidTank) field(machine, "tank"); }
    private static ItemStackHandler inventory(BlockEntity machine) { return (ItemStackHandler) field(machine, "inventory"); }
    private static ItemStack catStack(BlockEntity machine) { return (ItemStack) call(machine, "catStack", new Class<?>[0]); }
    private static boolean deploy(BlockEntity machine) { return (boolean) call(machine, "tryDeploy", new Class<?>[0]); }
    private static boolean target(BlockEntity machine, BlockPos pos) {
        return (boolean) call(machine, "setTarget", new Class<?>[] {BlockPos.class}, pos);
    }
    private static BlockPos target(BlockEntity machine) { return (BlockPos) call(machine, "target", new Class<?>[0]); }
    private static float progress(BlockEntity machine) {
        return (float) call(machine, "launchProgress", new Class<?>[] {float.class}, 0f);
    }
    private static ItemStack portable(BlockEntity machine) {
        return (ItemStack) call(machine, "portableStack", new Class<?>[0]);
    }
    private static IFluidHandler fluids(GameTestHelper h, BlockEntity machine) {
        return machine.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH).orElseThrow(
                () -> new AssertionError("Platform must expose pipe fluid capability"));
    }
    private static IItemHandler items(GameTestHelper h, BlockEntity machine) {
        return machine.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElseThrow(
                () -> new AssertionError("Platform must expose automation item capability"));
    }
    private static void fuel(GameTestHelper h, BlockEntity machine, int amount) {
        h.assertTrue(tank(machine).fill(new FluidStack(LaoWuMod.HISSING_GAS.get(), amount),
                IFluidHandler.FluidAction.EXECUTE) == amount, "Fixture could not fill hissing fuel");
    }
    private static void input(GameTestHelper h, BlockEntity machine, ItemStack pancake) {
        h.assertTrue(inventory(machine).insertItem(0, pancake, false).isEmpty(), "Fixture pancake insertion failed");
    }
    private static AABB fixture(GameTestHelper h) {
        return new AABB(h.absolutePos(BlockPos.ZERO), h.absolutePos(new BlockPos(5, 5, 5)));
    }
    private static List<Cat> cats(GameTestHelper h) {
        return h.getLevel().getEntitiesOfClass(Cat.class, fixture(h), Cat::isAlive);
    }
    private static ItemStack pancake() { return new ItemStack(LaoWuMod.CAT_PANCAKE.get()); }
    private static void retained(GameTestHelper h, BlockEntity machine, int amount) {
        h.assertTrue(tank(machine).getFluidAmount() == amount && catStack(machine).getCount() == 1,
                "Failed deployment consumed fuel or the pending pancake");
    }

    @GameTest(template = "accessory_probe", batch = "cat_deployment", timeoutTicks = 20)
    public static void registrationAutomationAndSimulationAreSafe(GameTestHelper h) {
        for (String id : new String[] {NORMAL, EJECTING}) {
            BlockEntity machine = machine(h, id);
            IFluidHandler fluid = fluids(h, machine);
            IItemHandler item = items(h, machine);
            h.assertTrue(fluid.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000),
                    IFluidHandler.FluidAction.EXECUTE) == 0, "Water must never enter a deployment tank");
            h.assertTrue(fluid.fill(new FluidStack(net.minecraft.world.level.material.Fluids.LAVA, 1000),
                    IFluidHandler.FluidAction.SIMULATE) == 0, "Simulated lava must be rejected");
            h.assertTrue(fluid.fill(new FluidStack(LaoWuMod.HISSING_GAS.get(), 9000),
                    IFluidHandler.FluidAction.SIMULATE) == 4000 && tank(machine).isEmpty(),
                    "Simulated hissing fill must report the 4000 mB capacity without changing state");
            h.assertTrue(fluid.fill(new FluidStack(LaoWuMod.HISSING_GAS.get(), 9000),
                    IFluidHandler.FluidAction.EXECUTE) == 4000 && tank(machine).getFluidAmount() == 4000,
                    "Committed fill must cap hissing fuel at 4000 mB");
            h.assertTrue(fluid.fill(new FluidStack(LaoWuMod.HISSING_GAS.get(), 1),
                    IFluidHandler.FluidAction.EXECUTE) == 0, "A full tank must reject excess fuel");
            h.assertTrue(fluid.drain(250, IFluidHandler.FluidAction.SIMULATE).getAmount() == 250
                    && tank(machine).getFluidAmount() == 4000, "Simulated fluid extraction must not consume fuel");
            h.assertTrue(item.getSlots() == 1, "Platform must expose exactly one pancake slot");
            h.assertTrue(item.insertItem(0, new ItemStack(Items.STONE), false).getCount() == 1
                    && catStack(machine).isEmpty(), "Automation must reject non-pancake items");
            ItemStack pair = new ItemStack(LaoWuMod.CAT_PANCAKE.get(), 2);
            h.assertTrue(item.insertItem(0, pair, true).getCount() == 1 && catStack(machine).isEmpty()
                    && pair.getCount() == 2, "Simulated insertion must accept only one and mutate neither stack");
            h.assertTrue(item.insertItem(0, pair, false).getCount() == 1 && catStack(machine).getCount() == 1,
                    "Committed insertion must accept exactly one pancake");
            h.assertTrue(item.insertItem(0, pancake(), false).getCount() == 1,
                    "Occupied platform must not merge another pancake");
            h.assertTrue(item.extractItem(0, 1, true).getCount() == 1 && catStack(machine).getCount() == 1,
                    "Simulated extraction must not remove the pending pancake");
            h.getLevel().removeBlock(machine.getBlockPos(), false);
        }
        h.succeed();
    }

    private static void playerClick(GameTestHelper h, net.minecraft.server.level.ServerPlayer player, BlockEntity machine) {
        BlockPos pos = machine.getBlockPos();
        player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos).add(0, .4, 0), Direction.UP, pos, false));
    }

    @GameTest(template = "accessory_probe", batch = "cat_deployment", timeoutTicks = 20)
    public static void actualPlayerBucketsAndPendingPancakeExchange(GameTestHelper h) {
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "deployment-manual"));
        player.setGameMode(GameType.SURVIVAL);
        player.setShiftKeyDown(false);
        Vec3 position = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(4, 1, 4)));
        player.moveTo(position.x, position.y, position.z, 0f, 0f);
        for (String id : new String[] {NORMAL, EJECTING}) {
            player.getInventory().clearContent();
            BlockEntity machine = machine(h, id);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(LaoWuMod.HISSING_GAS_BUCKET.get()));
            playerClick(h, player, machine);
            h.assertTrue(tank(machine).getFluidAmount() == 1000 && catStack(machine).isEmpty()
                    && player.getMainHandItem().is(Items.BUCKET),
                    "Real gas-bucket click must store 1000 mB and return one empty bucket");
            playerClick(h, player, machine);
            h.assertTrue(tank(machine).isEmpty() && player.getMainHandItem().is(LaoWuMod.HISSING_GAS_BUCKET.get()),
                    "Real empty-bucket click must extract all 1000 mB as one hissing bucket");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            playerClick(h, player, machine);
            h.assertTrue(tank(machine).isEmpty() && catStack(machine).isEmpty(),
                    "A water bucket must enter neither the hissing tank nor the pancake slot");
            // Vanilla bucket fallback may place water outside the rejected tank.
            h.setBlock(SOURCE.above(), Blocks.AIR);
            fuel(h, machine, 249);
            player.setItemInHand(InteractionHand.MAIN_HAND, pancake());
            playerClick(h, player, machine);
            retained(h, machine, 249);
            h.assertTrue(player.getMainHandItem().isEmpty() && cats(h).isEmpty(),
                    "Manual pancake insertion must store one pending cat without deploying below 250 mB");
            playerClick(h, player, machine);
            h.assertTrue(catStack(machine).isEmpty() && tank(machine).getFluidAmount() == 249
                    && player.getInventory().countItem(LaoWuMod.CAT_PANCAKE.get()) == 1,
                    "Real empty-hand click must return the pending pancake exactly once without burning fuel");
            h.getLevel().removeBlock(machine.getBlockPos(), false);
        }
        player.getInventory().clearContent();
        player.discard();
        h.succeed();
    }

    @GameTest(template = "accessory_probe", batch = "cat_deployment", timeoutTicks = 20)
    public static void airborneCaptureRestoresOriginalGravityWithoutOldLaunch(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 0, 2), Blocks.STONE);
        for (boolean originalNoGravity : new boolean[] {false, true}) {
            Cat original = EntityType.CAT.create(h.getLevel());
            h.assertTrue(original != null, "Airborne snapshot fixture creation failed");
            original.setTame(true);
            original.setOwnerUUID(OWNER);
            original.setNoAi(false);
            original.setNoGravity(originalNoGravity);
            original.setOnGround(false);
            original.setDeltaMovement(1.2, 1, 0);
            cn.laowu.mod.create.CatDeploymentFlight.begin(original);
            h.assertTrue(original.getPersistentData().contains("laowu:deployment_flight") && !original.isNoGravity(),
                    "Fixture must capture a genuinely active launch with temporarily enabled gravity");
            ItemStack captured = CatPancakeItem.capture(original);
            var saved = captured.getOrCreateTag().getCompound(CatPancakeItem.CAT_DATA_TAG);
            h.assertTrue(!saved.getCompound("ForgeData").contains("laowu:deployment_flight")
                    && saved.getBoolean("NoGravity") == originalNoGravity,
                    "Captured pancake must remove launch bookkeeping and save the pre-flight gravity setting");
            h.assertTrue(original.getPersistentData().contains("laowu:deployment_flight"),
                    "Sanitizing the saved snapshot must not mutate the still-airborne original cat");
            original.discard();
            Vec3 position = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2, 2, 2)));
            Cat restored = CatPancakeItem.deployCat(h.getLevel(), captured, position, 0f);
            h.assertTrue(restored != null && restored.isAlive() && captured.getCount() == 1,
                    "Ordinary restoration must create a live cat without consuming caller-owned input");
            h.assertTrue(!restored.getPersistentData().contains("laowu:deployment_flight")
                    && restored.isNoGravity() == originalNoGravity && !restored.isNoAi()
                    && restored.getDeltaMovement().lengthSqr() < .000001,
                    "Ordinary restoration must recover original gravity/AI without replaying airborne state or velocity");
            h.assertTrue(!cn.laowu.mod.create.CatDeploymentFlight.tick(restored)
                    && restored.isNoGravity() == originalNoGravity,
                    "A restored pancake must not resume or finish a stale launch");
            restored.discard();
        }
        h.succeed();
    }

    @GameTest(template = "accessory_probe", batch = "cat_deployment", timeoutTicks = 20)
    public static void insufficientHissingNeverConsumesInput(GameTestHelper h) {
        for (String id : new String[] {NORMAL, EJECTING}) {
            BlockEntity machine = machine(h, id);
            if (id.equals(EJECTING)) h.assertTrue(target(machine, h.absolutePos(new BlockPos(4, 0, 2))),
                    "Aligned nearby ejector target should be accepted");
            input(h, machine, pancake());
            fuel(h, machine, 249);
            h.assertTrue(!deploy(machine), "249 mB must not deploy a cat");
            retained(h, machine, 249);
            h.assertTrue(cats(h).isEmpty(), "Insufficient fuel must not spawn a cat");
            h.getLevel().removeBlock(machine.getBlockPos(), false);
        }
        h.succeed();
    }

    @GameTest(template = "accessory_probe", batch = "cat_deployment", timeoutTicks = 20)
    public static void successfulDeploymentPreservesCompleteCatSnapshot(GameTestHelper h) {
        BlockEntity machine = machine(h, NORMAL);
        Cat original = EntityType.CAT.create(h.getLevel());
        h.assertTrue(original != null, "Cat fixture creation failed");
        original.setTame(true);
        original.setOwnerUUID(OWNER);
        original.setAge(-12000);
        original.setCustomName(Component.literal("Deployment snapshot"));
        original.setNoAi(true);
        CatClothesData.equip(original, CatOutfitType.FIRE);
        var profile = CatAttributeData.ensure(original);
        for (CatStat stat : CatStat.values()) profile = profile.withValues(stat, 50, 100);
        CatAttributeData.set(original, profile);
        CatTraitData.set(original, CatTraitProfile.EMPTY.withLevel(CatTrait.LONG_FUR, 1));
        var cargo = CatProfileData.openContainer(original);
        var badge = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("laowu", "cat_speed_badge"));
        h.assertTrue(badge != Items.AIR, "Snapshot fixture accessory must be registered");
        cargo.setItem(0, new ItemStack(badge));
        cargo.setItem(4, new ItemStack(Items.DIAMOND, 3));
        cargo.setItem(12, new ItemStack(Items.APPLE, 2));
        cargo.setChanged();
        ItemStack captured = CatPancakeItem.capture(original);
        original.discard();
        input(h, machine, captured);
        fuel(h, machine, 750);
        h.assertTrue(deploy(machine), "A clear unpowered regular platform with sufficient hissing must deploy");
        h.assertTrue(tank(machine).getFluidAmount() == 500 && catStack(machine).isEmpty(),
                "One successful deployment must consume exactly 250 mB and one pancake");
        List<Cat> spawned = cats(h);
        h.assertTrue(spawned.size() == 1, "Deployment must spawn exactly one living cat");
        Cat cat = spawned.get(0);
        h.assertTrue(cat.isTame() && OWNER.equals(cat.getOwnerUUID()) && cat.getAge() == -12000
                && cat.getName().getString().equals("Deployment snapshot")
                && CatClothesData.getOutfit(cat) == CatOutfitType.FIRE && !CatPoseData.isPancake(cat),
                "Owner, exact growth age, name, career and normal pose must survive deployment");
        h.assertTrue(CatTraitData.ensure(cat).level(CatTrait.LONG_FUR) == 1, "Deployment lost the saved trait level");
        for (CatStat stat : CatStat.values()) h.assertTrue(CatAttributeData.ensure(cat).current(stat) == 50
                && CatAttributeData.ensure(cat).potential(stat) == 100, "Deployment changed saved genes: " + stat);
        var restoredCargo = CatProfileData.openContainer(cat);
        h.assertTrue(restoredCargo.getItem(0).is(badge) && restoredCargo.getItem(0).getCount() == 1
                && restoredCargo.getItem(4).is(Items.DIAMOND) && restoredCargo.getItem(4).getCount() == 3
                && restoredCargo.getItem(12).is(Items.APPLE) && restoredCargo.getItem(12).getCount() == 2,
                "Deployment must preserve exact accessory and cargo slots/counts");
        h.assertTrue(!deploy(machine) && tank(machine).getFluidAmount() == 500 && cats(h).size() == 1,
                "Repeating deployment after consuming the pancake must not duplicate a cat or burn fuel");
        cat.discard();
        h.succeed();
    }

    @GameTest(template = "accessory_probe", batch = "cat_deployment", timeoutTicks = 20)
    public static void blockedSpawnRetainsResourcesAndAllowsRetry(GameTestHelper h) {
        BlockEntity machine = machine(h, NORMAL);
        input(h, machine, pancake());
        fuel(h, machine, 1000);
        for (int x = 1; x <= 3; x++) for (int z = 1; z <= 3; z++) for (int y = 2; y <= 3; y++)
            h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
        h.assertTrue(!deploy(machine), "A solid blocked deployment area must reject spawning");
        retained(h, machine, 1000);
        h.assertTrue(cats(h).isEmpty(), "Blocked spawn must not create a suffocating cat");
        for (int x = 1; x <= 3; x++) for (int z = 1; z <= 3; z++) for (int y = 2; y <= 3; y++)
            h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        h.assertTrue(deploy(machine) && tank(machine).getFluidAmount() == 750 && catStack(machine).isEmpty(),
                "Removing the obstruction must deploy the retained pancake and charge exactly once");
        h.assertTrue(cats(h).size() == 1, "A successful retry must create exactly one cat");
        cats(h).forEach(Cat::discard);
        h.succeed();
    }

    @GameTest(template = "accessory_probe", batch = "cat_deployment", timeoutTicks = 20)
    public static void blockedTargetSpaceRetainsResourcesAndAllowsRetry(GameTestHelper h) {
        BlockEntity machine = machine(h, EJECTING);
        BlockPos selected = new BlockPos(4, 0, 2);
        h.setBlock(selected, Blocks.STONE);
        h.setBlock(selected.above(), Blocks.STONE);
        h.setBlock(selected.above(2), Blocks.STONE);
        h.assertTrue(h.getBlockState(SOURCE.above()).isAir()
                && h.getBlockState(SOURCE.above(2)).isAir(), "Source cat space must remain clear");
        h.assertTrue(target(machine, h.absolutePos(selected)), "Blocked target must still be geometrically valid");
        input(h, machine, pancake());
        fuel(h, machine, 1000);
        for (int attempt = 0; attempt < 2; attempt++) {
            h.assertTrue(!deploy(machine), "An ejector must reject blocked cat space above the selected landing block");
            retained(h, machine, 1000);
            h.assertTrue(cats(h).isEmpty() && progress(machine) == 0,
                    "A blocked destination must neither spawn a cat nor trigger launch animation");
        }
        h.setBlock(selected.above(), Blocks.AIR);
        h.setBlock(selected.above(2), Blocks.AIR);
        h.assertTrue(deploy(machine) && tank(machine).getFluidAmount() == 750 && catStack(machine).isEmpty(),
                "Clearing only the target cat space must deploy the retained pancake and charge exactly 250 mB");
        List<Cat> spawned = cats(h);
        h.assertTrue(spawned.size() == 1 && spawned.get(0).getDeltaMovement().x > 0,
                "Retry must restore exactly one cat and eject it east toward the same selected block");
        spawned.forEach(Cat::discard);
        h.succeed();
    }

    @GameTest(template = "accessory_probe", batch = "cat_deployment", timeoutTicks = 20)
    public static void actualBreakAndPlacementPreservePortableFuelAndPancake(GameTestHelper h) {
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "deployment-portable"));
        player.setGameMode(GameType.SURVIVAL);
        Vec3 playerPos = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(4, 1, 4)));
        player.moveTo(playerPos.x, playerPos.y, playerPos.z, 0f, 0f);
        for (String id : new String[] {NORMAL, EJECTING}) {
            BlockEntity machine = machine(h, id);
            if (id.equals(EJECTING)) h.assertTrue(target(machine, h.absolutePos(new BlockPos(4, 0, 2))),
                    "Portable ejector target setup failed");
            ItemStack waiting = pancake();
            waiting.setHoverName(Component.literal("Portable waiting cat"));
            input(h, machine, waiting);
            fuel(h, machine, 1750);
            for (int cycle = 0; cycle < 2; cycle++) {
                h.assertTrue(portable(machine).is(block(h, id).asItem()), "Portable snapshot must use the proper platform item");
                BlockPos absolute = machine.getBlockPos();
                h.assertTrue(h.getLevel().destroyBlock(absolute, true, player), "Actual platform destruction failed");
                List<ItemEntity> drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(absolute).inflate(1.25));
                int platforms = 0;
                ItemStack dropped = ItemStack.EMPTY;
                for (ItemEntity drop : drops) {
                    h.assertTrue(drop.getItem().is(block(h, id).asItem()), "Breaking must not duplicate loose pancake cargo");
                    platforms += drop.getItem().getCount();
                    dropped = drop.getItem().copy();
                    drop.discard();
                }
                h.assertTrue(platforms == 1 && dropped.getCount() == 1, "Breaking must drop exactly one portable platform");
                player.setItemInHand(InteractionHand.MAIN_HAND, dropped);
                BlockPos floor = absolute.below();
                var hit = new BlockHitResult(Vec3.atCenterOf(floor), Direction.UP, floor, false);
                var placed = ((BlockItem) dropped.getItem()).place(
                        new BlockPlaceContext(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)));
                h.assertTrue(placed.consumesAction() && player.getMainHandItem().isEmpty(),
                        "Real survival placement must consume the portable platform exactly once");
                machine = h.getLevel().getBlockEntity(absolute);
                h.assertTrue(machine != null, "Placed portable platform must restore its block entity");
                retained(h, machine, 1750);
                h.assertTrue(catStack(machine).getHoverName().getString().equals("Portable waiting cat"),
                        "Actual break/place must preserve the pending pancake's saved data");
                if (id.equals(EJECTING)) h.assertTrue(h.absolutePos(new BlockPos(4, 0, 2)).equals(target(machine)),
                        "Portable ejector must retain its selected target");
            }
            h.getLevel().removeBlock(machine.getBlockPos(), false);
        }
        player.getInventory().clearContent();
        player.discard();
        h.succeed();
    }

    @GameTest(template = "accessory_probe", batch = "cat_deployment", timeoutTicks = 20)
    public static void targetValidationCannotConsumeInputOrLoadDistantChunks(GameTestHelper h) {
        BlockEntity machine = machine(h, EJECTING);
        input(h, machine, pancake());
        fuel(h, machine, 1000);
        h.assertTrue(!deploy(machine), "An ejector without a selected target must retain its input");
        retained(h, machine, 1000);
        BlockPos source = machine.getBlockPos();
        int max = AllConfigs.server().kinetics.maxEjectorDistance.get();
        h.assertTrue(!target(machine, source) && !target(machine, source.above()),
                "Zero horizontal displacement must not be a valid target");
        h.assertTrue(!target(machine, source.offset(1, 0, 1)), "Diagonal target must be rejected");
        h.assertTrue(!target(machine, source.offset(max + 1, 0, 0)), "Target beyond Create's server limit must be rejected");
        h.assertTrue(!target(machine, source.offset(1, max + 1, 0)), "Vertical target beyond the server limit must be rejected");
        retained(h, machine, 1000);
        BlockPos distant = source.offset(1000000, 0, 0);
        h.assertTrue(!h.getLevel().hasChunkAt(distant), "Distant target fixture must start unloaded");
        h.assertTrue(!target(machine, distant) && !h.getLevel().hasChunkAt(distant),
                "Rejecting an unloaded distant target must never force-load its chunk");
        // Also exercise an in-range unloaded destination when the surrounding test chunks permit one.
        outer: for (Direction direction : new Direction[] {Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH}) {
            for (int distance = 1; distance <= max; distance++) {
                BlockPos candidate = source.relative(direction, distance);
                if (h.getLevel().hasChunkAt(candidate)) continue;
                target(machine, candidate);
                h.assertTrue(!deploy(machine), "An in-range unloaded destination must block deployment");
                retained(h, machine, 1000);
                h.assertTrue(!h.getLevel().hasChunkAt(candidate), "Destination validation must not load an in-range chunk");
                break outer;
            }
        }
        h.succeed();
    }

    // Flight fixtures stay inside a small corridor of artillery_probe; never touch a gameplay world's forced chunks.
    private static Runnable flightArena(GameTestHelper h, BlockPos source, BlockPos selected) {
        int minX = Math.min(source.getX(), selected.getX()) - 2;
        int maxX = Math.max(source.getX(), selected.getX()) + 3;
        int minZ = Math.min(source.getZ(), selected.getZ()) - 2;
        int maxZ = Math.max(source.getZ(), selected.getZ()) + 2;
        java.util.List<net.minecraft.world.level.ChunkPos> addedChunks = new java.util.ArrayList<>();
        if (h.getLevel().getServer() instanceof net.minecraft.gametest.framework.GameTestServer) {
            BlockPos first = h.absolutePos(new BlockPos(minX, 0, minZ));
            BlockPos last = h.absolutePos(new BlockPos(maxX, 11, maxZ));
            for (int x = Math.floorDiv(first.getX(), 16); x <= Math.floorDiv(last.getX(), 16); x++)
                for (int z = Math.floorDiv(first.getZ(), 16); z <= Math.floorDiv(last.getZ(), 16); z++) {
                    var chunk = new net.minecraft.world.level.ChunkPos(x, z);
                    if (h.getLevel().getForcedChunks().contains(chunk.toLong())) continue;
                    h.getLevel().setChunkForced(x, z, true);
                    addedChunks.add(chunk);
                }
        }
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            for (int y = 1; y <= 11; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        }
        // The selected coordinate is the solid landing block, not the cat's feet or its eye position.
        for (int x = selected.getX() - 2; x <= selected.getX() + 2; x++)
            for (int z = selected.getZ() - 2; z <= selected.getZ() + 2; z++)
                for (int y = 1; y <= selected.getY(); y++) h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
        for (int y = 1; y < source.getY(); y++) h.setBlock(new BlockPos(source.getX(), y, source.getZ()), Blocks.STONE);
        return () -> addedChunks.forEach(chunk -> h.getLevel().setChunkForced(chunk.x, chunk.z, false));
    }

    private static void actualFlight(GameTestHelper h, Direction direction, int sourceHeight, int landingHeight,
                                     int distance, boolean skyTrait) {
        BlockPos source = direction == Direction.NORTH ? new BlockPos(10, sourceHeight, 11)
                : new BlockPos(4, sourceHeight, 6);
        BlockPos selected = source.relative(direction, distance).atY(landingHeight);
        Runnable releaseChunks = flightArena(h, source, selected);
        h.setBlock(source, block(h, EJECTING));
        BlockEntity machine = h.getLevel().getBlockEntity(h.absolutePos(source));
        h.assertTrue(machine != null && target(machine, h.absolutePos(selected)), "Flight target setup failed");
        Cat[] airborne = {null};
        Vec3[] launchPosition = {null};
        float[] launchHealth = {0};
        boolean[] finished = {false};
        int[] landedAt = {-1};
        Vec3 expectedLanding = Vec3.atBottomCenterOf(h.absolutePos(selected).above());
        // Register every callback before execution: adding tasks from a running GameTest callback
        // can resize its task map before iterator.remove(), leaving the launch callback registered.
        // Let the isolated server make the corridor entity-ticking before launching the real cat.
        h.runAfterDelay(5, () -> {
            Cat original = EntityType.CAT.create(h.getLevel());
            h.assertTrue(original != null, "Flight fixture cat creation failed");
            original.setTame(true);
            original.setOwnerUUID(OWNER);
            original.setCustomName(Component.literal(skyTrait ? "Airborne sky cat" : "Airborne AI cat"));
            original.setNoAi(false);
            original.setNoGravity(false);
            var profile = CatAttributeData.ensure(original);
            for (CatStat stat : CatStat.values()) profile = profile.withValues(stat, 50, 100);
            CatAttributeData.set(original, profile);
            CatTraitData.set(original, skyTrait ? CatTraitProfile.EMPTY.withLevel(CatTrait.SKY_CAT, 1)
                    : CatTraitProfile.EMPTY);
            original.setHealth(original.getMaxHealth());
            ItemStack captured = CatPancakeItem.capture(original);
            original.discard();
            input(h, machine, captured);
            fuel(h, machine, 750);
            h.assertTrue(deploy(machine), "Clear loaded flight corridor must allow unpowered ejection");
            BlockPos absoluteSource = h.absolutePos(source);
            List<Cat> spawned = h.getLevel().getEntitiesOfClass(Cat.class, new AABB(absoluteSource).inflate(2),
                    cat -> cat.isAlive() && cat.getName().getString().startsWith("Airborne"));
            h.assertTrue(spawned.size() == 1, "Flight fixture must launch exactly one restored cat");
            Cat cat = spawned.get(0);
            airborne[0] = cat;
            launchPosition[0] = cat.position();
            launchHealth[0] = cat.getHealth();
        });
        for (int tick = 1; tick <= 70; tick++) {
            final int elapsed = tick;
            h.runAfterDelay(5 + tick, () -> {
                if (finished[0]) return;
                Cat cat = airborne[0];
                h.assertTrue(cat != null, "Flight setup must complete before scheduled trajectory checks");
                h.assertTrue(cat.isAlive() && !cat.isRemoved(), "Launched cat vanished during real world ticks");
                if (landedAt[0] >= 0) {
                    if (elapsed >= landedAt[0] + 5) {
                        h.assertTrue(!cat.isNoAi() && cat.isNoGravity(),
                                "The preserved sky trait's autonomous AI must resume after landing");
                        finished[0] = true;
                        cat.discard();
                        releaseChunks.run();
                        h.succeed();
                    }
                    return;
                }
                Vec3 start = launchPosition[0];
                Vec3 travelled = cat.position().subtract(start);
                double forward = travelled.x * direction.getStepX() + travelled.z * direction.getStepZ();
                double lateral = Math.abs(travelled.x * direction.getStepZ() - travelled.z * direction.getStepX());
                if (elapsed == 8) {
                    h.assertTrue(cat.tickCount >= 6, "Flight test requires a genuinely ticking entity, not frozen chunks");
                    h.assertTrue(forward >= distance * .38 && lateral < .8 && cat.getY() > start.y + .8,
                            "Active cat AI must not cancel/reverse Create's airborne launch; forward=" + forward
                                    + ", lateral=" + lateral + ", height=" + (cat.getY() - start.y));
                    h.assertTrue(cat.getDeltaMovement().x * direction.getStepX()
                            + cat.getDeltaMovement().z * direction.getStepZ() > .15,
                            "Launched cat must retain positive travel velocity after eight real AI ticks");
                }
                if (elapsed >= 8 && cat.onGround()) {
                    double horizontalError = Math.hypot(cat.getX() - expectedLanding.x, cat.getZ() - expectedLanding.z);
                    h.assertTrue(horizontalError <= 2.5 && Math.abs(cat.getY() - expectedLanding.y) < .1,
                            "Create trajectory must land near the selected block's TOP; position=" + cat.position()
                                    + ", expected feet=" + expectedLanding + ", horizontal error=" + horizontalError);
                    h.assertTrue(cat.getHealth() >= launchHealth[0] - .01f,
                            "Vanilla cat fall immunity must survive deployment without fall damage");
                    h.assertTrue(!cat.isNoAi(), "Any temporary airborne AI suppression must be released on landing");
                    h.assertTrue(tank(machine).getFluidAmount() == 500 && catStack(machine).isEmpty(),
                            "World flight ticks must not repeat fuel/input consumption");
                    if (skyTrait) h.assertTrue(CatTraitData.ensure(cat).level(CatTrait.SKY_CAT) == 1,
                            "Airborne launch must not erase the sky cat's saved trait");
                    if (skyTrait) {
                        landedAt[0] = elapsed;
                    } else {
                        finished[0] = true;
                        cat.discard();
                        releaseChunks.run();
                        h.succeed();
                    }
                } else if (elapsed == 65) {
                    h.assertTrue(false, "Launched cat did not land after 65 actual ticks; position=" + cat.position()
                            + ", expected feet=" + expectedLanding + ", motion=" + cat.getDeltaMovement());
                }
            });
        }
    }

    @GameTest(template = "artillery_probe", batch = "cat_deployment_flight", timeoutTicks = 100)
    public static void activeAiEastFlightReachesSelectedBlockTop(GameTestHelper h) {
        actualFlight(h, Direction.EAST, 1, 0, 12, false);
    }

    @GameTest(template = "artillery_probe", batch = "cat_deployment_flight", timeoutTicks = 100)
    public static void activeAiNorthFlightUsesCorrectCreateDirection(GameTestHelper h) {
        actualFlight(h, Direction.NORTH, 1, 0, 8, false);
    }

    @GameTest(template = "artillery_probe", batch = "cat_deployment_flight", timeoutTicks = 100)
    public static void activeAiRaisedTargetLandsAtSelectedHeight(GameTestHelper h) {
        actualFlight(h, Direction.EAST, 1, 3, 12, false);
    }

    @GameTest(template = "artillery_probe", batch = "cat_deployment_flight", timeoutTicks = 100)
    public static void activeAiLowerTargetRetainsVanillaFallImmunity(GameTestHelper h) {
        actualFlight(h, Direction.EAST, 5, 0, 12, false);
    }

    @GameTest(template = "artillery_probe", batch = "cat_deployment_flight", timeoutTicks = 100)
    public static void skyTraitCannotOverrideLaunchAndResumesAfterLanding(GameTestHelper h) {
        actualFlight(h, Direction.EAST, 1, 0, 12, true);
    }


    @GameTest(template = "accessory_probe", batch = "cat_deployment", timeoutTicks = 60)
    public static void ejectorTargetsBeforePlacementAndLaunchesWithoutRotation(GameTestHelper h) {
        Block platform = block(h, EJECTING);
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "deployment-target"));
        player.setGameMode(GameType.SURVIVAL);
        Vec3 playerPos = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(4, 1, 4)));
        player.moveTo(playerPos.x, playerPos.y, playerPos.z, 0f, 0f);
        BlockPos selected = h.absolutePos(new BlockPos(4, 0, 2));
        h.getLevel().setBlock(selected, Blocks.STONE.defaultBlockState(), 3);
        ItemStack stack = new ItemStack(platform);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setShiftKeyDown(true);
        var selectHit = new BlockHitResult(Vec3.atCenterOf(selected), Direction.UP, selected, false);
        h.assertTrue(stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, selectHit)).consumesAction(),
                "Sneak-right-click must select a target before placement");
        h.assertTrue(stack.getCount() == 1 && h.getLevel().getBlockState(selected.above()).isAir(),
                "Selecting a target must neither place nor consume the platform");
        player.setShiftKeyDown(false);
        BlockPos floor = h.absolutePos(SOURCE.below());
        h.getLevel().setBlock(floor, Blocks.STONE.defaultBlockState(), 3);
        var placeHit = new BlockHitResult(Vec3.atCenterOf(floor), Direction.UP, floor, false);
        h.assertTrue(((BlockItem) stack.getItem()).place(
                new BlockPlaceContext(new UseOnContext(player, InteractionHand.MAIN_HAND, placeHit))).consumesAction(),
                "Normal placement after selection must place the platform");
        BlockEntity machine = h.getLevel().getBlockEntity(h.absolutePos(SOURCE));
        h.assertTrue(machine != null && selected.equals(target(machine)), "Server placement must apply the selected target");
        input(h, machine, pancake());
        fuel(h, machine, 500);
        h.assertTrue(deploy(machine), "An ejector needs hissing, not a rotational power source");
        h.assertTrue(tank(machine).getFluidAmount() == 250 && catStack(machine).isEmpty(), "Ejection must charge exactly once");
        List<Cat> spawned = cats(h);
        h.assertTrue(spawned.size() == 1, "Ejection must restore one live cat before launching");
        Cat cat = spawned.get(0);
        Vec3 motion = cat.getDeltaMovement();
        h.assertTrue(motion.x > 0 && motion.y > 0 && Math.abs(motion.z) < 0.001,
                "Create launch velocity must point upward and east toward the selected target");
        float initial = progress(machine);
        h.assertTrue(initial > 0 && initial <= 1, "Successful ejection must trigger visible bounded tray/rod animation");
        cat.discard();
        player.getInventory().clearContent();
        player.discard();
        h.runAfterDelay(40, () -> {
            h.assertTrue(progress(machine) == 0, "Ejector animation must return to its resting state");
            h.assertTrue(tank(machine).getFluidAmount() == 250 && catStack(machine).isEmpty(),
                    "Animation ticks must not repeat resource consumption");
            h.succeed();
        });
    }
}
