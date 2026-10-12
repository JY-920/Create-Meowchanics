package cn.laowu.mod.test;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.CatEngineerGogglesItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class StandaloneExtrasProbe {
    @GameTest(template="artillery_probe",timeoutTicks=30)
    public static void giantEggSpawnsTheExistingBoss(GameTestHelper h) {
        var id=LaoWuMod.id("giant_cat_boss_spawn_egg");
        h.assertTrue(BuiltInRegistries.ITEM.containsKey(id),"Giant cat spawn egg remains available with or without Create");
        var item=BuiltInRegistries.ITEM.get(id);
        h.assertTrue(item instanceof SpawnEggItem,"Boss egg uses vanilla spawning semantics");
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"boss-egg"));
        var pos=h.absolutePos(new BlockPos(8,0,6));
        h.getLevel().setBlock(pos,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));
        var result=item.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false)));
        h.assertTrue(result.consumesAction(),"Right-click actually uses the egg");
        var bosses=h.getLevel().getEntitiesOfClass(cn.laowu.mod.entity.GiantCatBoss.class,new AABB(pos).inflate(5));
        h.assertTrue(bosses.size()==1&&bosses.get(0).getType()==LaoWuMod.GIANT_CAT_BOSS.get(),"Egg spawns exactly the existing giant boss, not a mount carrier");
        bosses.forEach(net.minecraft.world.entity.Entity::discard);player.discard();h.succeed();
    }

    @GameTest(template="artillery_probe",timeoutTicks=30)
    public static void engineerGogglesEquipWithoutCreateInheritance(GameTestHelper h) {
        var id=LaoWuMod.id("cat_engineer_goggles");
        h.assertTrue(BuiltInRegistries.ITEM.containsKey(id),"Engineer goggles must remain registered");
        var item=BuiltInRegistries.ITEM.get(id);
        h.assertTrue(item instanceof Equipable&&((Equipable)item).getEquipmentSlot()==EquipmentSlot.HEAD,"Goggles use the vanilla head slot");
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"core-goggles"));
        player.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));
        h.assertTrue(item.use(h.getLevel(),player,InteractionHand.MAIN_HAND).getResult().consumesAction(),"Right-click equips goggles");
        h.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(item)&&CatEngineerGogglesItem.isWornBy(player),"Equipped core goggles are recognized");
        if(cn.laowu.mod.compat.create.CreateIntegration.isLoaded())
            h.assertTrue(com.simibubi.create.content.equipment.goggles.GogglesItem.isWearingGoggles(player),"Create still recognizes custom goggles");
        player.discard();h.succeed();
    }
}
