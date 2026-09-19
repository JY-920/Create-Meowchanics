package cn.laowu.mod.test;

import cn.laowu.mod.*;
import cn.laowu.mod.create.*;
import cn.laowu.mod.genetics.*;
import cn.laowu.mod.item.WishAdoptionBoxBlockItem;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class PortableWishProbe {
    static ServerPlayer player(GameTestHelper h) {
        var level=h.getLevel();
        var stub=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"card-connection"));
        var p=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"card-probe"),net.minecraft.server.level.ClientInformation.createDefault());
        p.connection=stub.connection;
        var pos=Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,1,3)));
        p.moveTo(pos.x,pos.y,pos.z,0,0);return p;
    }
    static ItemStack[] ingredients(int count) {
        ItemStack[] grid=new ItemStack[9];Arrays.fill(grid,ItemStack.EMPTY);
        grid[1]=new ItemStack(Items.PAPER,count);
        grid[3]=new ItemStack(Items.EMERALD,count);grid[5]=grid[3].copy();
        grid[4]=new ItemStack(LaoWuMod.ADOPTION_BOX_ITEM.get(),count);
        grid[7]=new ItemStack(BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create","cardboard")),count);
        return grid;
    }
    static void fill(CraftingMenu menu,int count) {
        var grid=ingredients(count);for(int i=0;i<9;i++)menu.getSlot(i+1).set(grid[i]);
    }
    static void bound(GameTestHelper h,ItemStack stack) {
        var card=WishAdoptionBoxBlockItem.offer(stack);
        h.assertTrue(stack.is(LaoWuMod.WISH_ADOPTION_BOX_ITEM.get())&&card!=null,"Crafted box carries a real offer before placement");
        h.assertTrue(!cn.laowu.mod.accessory.CatAccessoryRarity.bossOnly(card.rewardStack()),"No boss accessory");
        h.assertTrue(!WishAdoptionBoxBlockItem.cardData(stack).contains("Inventory"),"Card never embeds inventory");
    }
    @GameTest(template="accessory_probe",batch="portable35",timeoutTicks=20)
    public static void manualShiftAndCreateCrafting(GameTestHelper h) {
        var p=player(h);var pos=h.absolutePos(new BlockPos(1,1,1));
        h.getLevel().setBlock(pos,Blocks.CRAFTING_TABLE.defaultBlockState(),3);
        var menu=new CraftingMenu(135,p.getInventory(),ContainerLevelAccess.create(h.getLevel(),pos));p.containerMenu=menu;
        fill(menu,1);
        h.assertTrue(menu.getSlot(0).getItem().is(LaoWuMod.WISH_ADOPTION_BOX_ITEM.get()),"Custom recipe remains a normal shaped recipe");
        for(int i=0;i<8;i++){menu.slotsChanged(new SimpleContainer(0));
            h.assertTrue(WishAdoptionBoxBlockItem.offer(menu.getSlot(0).getItem())==null,"Preview does not roll a reward");}
        menu.clicked(0,0,ClickType.PICKUP,p);
        bound(h,menu.getCarried());var snapshot=WishAdoptionBoxBlockItem.offer(menu.getCarried());
        var copied=ItemStack.parseOptional(h.getLevel().registryAccess(),(CompoundTag)menu.getCarried().save(h.getLevel().registryAccess()));
        h.assertTrue(snapshot.equals(WishAdoptionBoxBlockItem.offer(copied)),"Item NBT round trip");
        menu.setCarried(ItemStack.EMPTY);
        fill(menu,3);for(int i=0;i<3;i++)menu.quickMoveStack(p,0);
        int total=0;for(var stack:p.getInventory().items)if(stack.is(LaoWuMod.WISH_ADOPTION_BOX_ITEM.get())){bound(h,stack);total+=stack.getCount();}
        h.assertTrue(total==3,"Shift crafting binds every result before copying to inventory");
        var input=ingredients(1);var data=new CompoundTag();var list=new ListTag();
        for(int i=0;i<9;i++){if(input[i].isEmpty())continue;var entry=new CompoundTag();
            entry.putInt("x",i%3);entry.putInt("y",2-i/3);entry.put("item",input[i].save(h.getLevel().registryAccess()));list.add(entry);}
        data.put("Grid",list);
        bound(h,RecipeGridHandler.tryToApplyRecipe(h.getLevel(),RecipeGridHandler.GroupedItems.read(data,h.getLevel().registryAccess())));
        var crafterResult=new ItemStack(LaoWuMod.WISH_ADOPTION_BOX_ITEM.get());
        crafterResult.getItem().onCraftedPostProcess(crafterResult,h.getLevel());bound(h,crafterResult);
        var recipe=(cn.laowu.mod.recipe.WishAdoptionBoxRecipe)h.getLevel().getRecipeManager()
                .byKey(LaoWuMod.id("wish_adoption_box_crafting")).orElseThrow().value();
        var serializer=(cn.laowu.mod.recipe.WishAdoptionBoxRecipe.Serializer)LaoWuMod.WISH_ADOPTION_BOX_RECIPE.get();
        var packet=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try {
            serializer.streamCodec().encode(packet,recipe);
            var decoded=serializer.streamCodec().decode(packet);
            h.assertTrue(!packet.isReadable()&&decoded.getWidth()==3&&decoded.getHeight()==3
                    &&decoded.getSerializer()==serializer&&decoded.getResultItem(h.getLevel().registryAccess()).is(LaoWuMod.WISH_ADOPTION_BOX_ITEM.get()),
                    "Custom recipe network synchronization preserves the shaped recipe");
            var json=serializer.codec().codec().encodeStart(com.mojang.serialization.JsonOps.INSTANCE,recipe).getOrThrow();
            h.assertTrue(serializer.codec().codec().parse(com.mojang.serialization.JsonOps.INSTANCE,json).getOrThrow().getWidth()==3,
                    "Recipe JSON codec can re-encode the original shaped pattern");
        }finally{packet.release();}
        p.containerMenu=p.inventoryMenu;p.discard();
        System.out.println("PASS: portable Wish manual + shift + Create crafting, stable preview, item and recipe network serialization");h.succeed();
    }
    static WishAdoptionBoxBlockEntity place(GameTestHelper h,ServerPlayer p,ItemStack stack) {
        var floor=h.absolutePos(new BlockPos(1,0,1));h.getLevel().setBlock(floor,Blocks.STONE.defaultBlockState(),3);
        p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var context=new BlockPlaceContext(new UseOnContext(p,InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(floor),Direction.UP,floor,false)));
        var result=((WishAdoptionBoxBlockItem)stack.getItem()).place(context);
        h.assertTrue(result.consumesAction(),"Actual BlockItem placement succeeded");
        return (WishAdoptionBoxBlockEntity)h.getLevel().getBlockEntity(floor.above());
    }
    static ItemStack breakBox(GameTestHelper h,ServerPlayer p,WishAdoptionBoxBlockEntity box,int diamonds) {
        var pos=box.getBlockPos();h.assertTrue(h.getLevel().destroyBlock(pos,true,p),"Actual destruction succeeded");
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(1));
        int contents=0,boxes=0;ItemStack result=ItemStack.EMPTY;
        for(var drop:drops){var stack=drop.getItem();
            if(stack.is(LaoWuMod.WISH_ADOPTION_BOX_ITEM.get())){boxes+=stack.getCount();result=stack.copy();}
            else if(stack.is(Items.DIAMOND))contents+=stack.getCount();
            else throw new AssertionError("Unexpected preview or duplicated item drop: "+stack);
            drop.discard();}
        h.assertTrue(boxes==1&&contents==diamonds,"One box and one separate set of contents; preview never drops");
        bound(h,result);return result;
    }
    @GameTest(template="accessory_probe",batch="portable35",timeoutTicks=20)
    public static void repeatedBreakPlaceAndTrade(GameTestHelper h) {
        var p=player(h);var stack=new ItemStack(LaoWuMod.WISH_ADOPTION_BOX_ITEM.get());
        stack.getItem().onCraftedBy(stack,h.getLevel(),p);
        var original=WishAdoptionBoxBlockItem.offer(stack);
        WishAdoptionBoxBlockItem.writeCard(stack,original,true);
        var box=place(h,p,stack);
        box.inventory().setStackInSlot(9,new ItemStack(Items.DIAMOND,3));
        for(int i=0;i<8;i++){
            h.assertTrue(original.equals(box.offer())&&box.locked(),"Reward, all requirements and lock restored after placement "+i);
            stack=breakBox(h,p,box,i==0?3:0);
            h.assertTrue(original.equals(WishAdoptionBoxBlockItem.offer(stack))&&WishAdoptionBoxBlockItem.cardData(stack).getBoolean("Locked"),"Dropped item retains exact card and lock "+i);
            box=place(h,p,stack);
        }
        // A locked successful trade retains its card; an unlocked trade rolls once, then travels with that new card.
        for(boolean locked:new boolean[]{true,false}){
            var tag=box.saveWithoutMetadata(h.getLevel().registryAccess());tag.putBoolean("Locked",locked);box.loadAdditional(tag,h.getLevel().registryAccess());
            var pancake=new ItemStack(LaoWuMod.CAT_PANCAKE.get());
            var profile=CatAttributeProfile.founder(net.minecraft.util.RandomSource.create(35));
            for(var stat:CatStat.values())profile=profile.withValues(stat,50,100);
            for(var condition:box.offer().conditions()){
                int v=condition.min()>=0?condition.min():0;
                profile=profile.withValues(condition.stat(),v,v);
            }
            CatAttributeData.set(pancake,profile);CatTraitData.set(pancake,CatTraitProfile.EMPTY);
            box.inventory().setStackInSlot(0,pancake);
            WishAdoptionBoxBlockEntity.serverTick(h.getLevel(),box.getBlockPos(),box.getBlockState(),box);
            h.assertTrue(box.inventory().getStackInSlot(0).isEmpty()&&!box.inventory().getStackInSlot(9).isEmpty(),"Matching pancake trades once");
            h.assertTrue(locked==original.equals(box.offer()),"Only unlocked successful trade changes offer");
            box.inventory().setStackInSlot(9,ItemStack.EMPTY);
        }
        var refreshed=box.offer();stack=breakBox(h,p,box,0);box=place(h,p,stack);
        h.assertTrue(refreshed.equals(box.offer())&&!box.locked(),"Post-trade card is preserved, not reset to original recipe");
        breakBox(h,p,box,0);p.discard();
        System.out.println("PASS: 8 actual mine/place cycles preserve card + lock, no cargo duplication, locked/unlocked trading");h.succeed();
    }
    @GameTest(template="accessory_probe",batch="portable35",timeoutTicks=20)
    public static void legacyBoxesInitializeOnce(GameTestHelper h) {
        var p=player(h);var stack=new ItemStack(LaoWuMod.WISH_ADOPTION_BOX_ITEM.get());
        stack.getItem().inventoryTick(stack,h.getLevel(),p,0,false);bound(h,stack);
        var card=WishAdoptionBoxBlockItem.offer(stack);
        for(int i=0;i<40;i++)stack.getItem().inventoryTick(stack,h.getLevel(),p,0,false);
        h.assertTrue(card.equals(WishAdoptionBoxBlockItem.offer(stack)),"Legacy inventory migration only initializes once");
        var box=place(h,p,new ItemStack(LaoWuMod.WISH_ADOPTION_BOX_ITEM.get()));
        bound(h,breakBox(h,p,box,0));
        var pos=h.absolutePos(new BlockPos(1,1,1));
        h.getLevel().setBlock(pos,LaoWuMod.WISH_ADOPTION_BOX.get().defaultBlockState(),3);
        box=(WishAdoptionBoxBlockEntity)h.getLevel().getBlockEntity(pos);
        bound(h,breakBox(h,p,box,0));
        p.discard();System.out.println("PASS: old inventory/placed boxes initialize once; breaking before first tick still stamps card");h.succeed();
    }
}
