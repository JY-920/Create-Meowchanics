package cn.laowu.mod.test;

import cn.laowu.mod.*;
import cn.laowu.mod.item.*;
import cn.laowu.mod.create.*;
import cn.laowu.mod.genetics.*;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.sequenced.*;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.items.*;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class WishFilterAssemblyProbe {
    private static ServerPlayer player(GameTestHelper h) {
        var level=h.getLevel();var stub=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"filter-link"));
        var p=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"filter-probe"),net.minecraft.server.level.ClientInformation.createDefault());
        p.connection=stub.connection;var pos=h.absolutePos(new BlockPos(1,2,3));
        p.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LaoWuMod.CAT_FILTER.get()));return p;
    }
    private static void offer(WishAdoptionBoxBlockEntity box,boolean max){
        var rules=List.of(new WishAdoptionOffer.Condition(CatStat.ATTACK,40,60),
                new WishAdoptionOffer.Condition(CatStat.SPEED,50,-1),
                new WishAdoptionOffer.Condition(CatStat.HEALTH,-1,70));
        var tag=box.saveWithoutMetadata(box.getLevel().registryAccess());
        tag.put("Offer",new WishAdoptionOffer(max,rules,"laowu:cat_loot_magnet").save());
        tag.putBoolean("Locked",true);box.loadAdditional(tag,box.getLevel().registryAccess());
    }
    private static PlayerInteractEvent.RightClickBlock click(ServerPlayer p,BlockPos pos){
        var event=new PlayerInteractEvent.RightClickBlock(p,InteractionHand.MAIN_HAND,pos,
                new BlockHitResult(Vec3.atCenterOf(pos),Direction.SOUTH,pos,false));
        CommonEvents.onHissingGasBucketInteract(event);return event;
    }
    private static ItemStack roundTrip(ItemStack stack){return ItemCustomData.loadStack(ItemCustomData.saveStack(stack));}
    private static void finish(GameTestHelper h,String message){System.out.println("PASS: wish filter / assembly "+message);h.succeed();}

    @GameTest(template="accessory_probe",batch="wish_filter33",timeoutTicks=30)
    public static void importResetAndExactPredicate(GameTestHelper h){
        var p=player(h);var pos=h.absolutePos(new BlockPos(1,2,1));
        h.setBlock(new BlockPos(1,2,1),LaoWuMod.WISH_ADOPTION_BOX.get());
        var box=(WishAdoptionBoxBlockEntity)h.getLevel().getBlockEntity(pos);
        var stack=p.getMainHandItem();stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,Component.literal("Keep item name"));
        var dirty=CatFilterRules.fromValues(null,null,null,null,List.of(CatTrait.LOLI),
                CatFilterRules.GrowthFilter.KITTEN,CatFilterRules.OwnershipFilter.UNOWNED,
                CatFilterRules.CareerFilter.AGENT,"old cat name").withLogic(new CatFilterLogic(63,63,31));
        dirty.write(stack);offer(box,false);
        var event=click(p,pos);var rules=CatFilterRules.read(stack);
        h.assertTrue(event.isCanceled()&&event.getCancellationResult().consumesAction(),"Right-click intercepts before box/filter screens");
        h.assertTrue(rules.baseCurrent()&&rules.logic().limitMask()==0&&rules.logic().flags()==0,"NOW uses raw base values; old MAX / OR / inversion reset");
        h.assertTrue(rules.requiredTraits().isEmpty()&&rules.catName().isEmpty()
                &&rules.growth()==CatFilterRules.GrowthFilter.ANY&&rules.ownership()==CatFilterRules.OwnershipFilter.ANY
                &&rules.career()==CatFilterRules.CareerFilter.ANY,"Traits, age, owner, career and old cat-name conditions cleared");
        h.assertTrue(stack.getHoverName().getString().equals("Keep item name")&&stack.getCount()==1,"Item custom name and count preserved");
        for(var stat:CatStat.values()){
            h.assertTrue(!rules.enabled(1,stat),"Unused page disabled");
            h.assertTrue(rules.enabled(0,stat)==Set.of(CatStat.ATTACK,CatStat.SPEED,CatStat.HEALTH).contains(stat),"Only copied stats enabled");
        }
        var rng=RandomSource.create(3300);
        for(int i=0;i<1500;i++){
            var profile=CatAttributeProfile.founder(rng);
            h.assertTrue(rules.matches(profile,CatTraitProfile.EMPTY)==box.offer().matches(profile),"NOW exact numeric predicate");
        }
        var menu=new CatFilterMenu(71,p.getInventory(),stack);
        h.assertTrue(menu.baseCurrent()&&menu.rules().baseCurrent(),"Held menu preserves base mode");
        menu.clickMenuButton(p,CatFilterMenu.rangeButton(0,CatStat.ATTACK,false,40));
        h.assertTrue(menu.rules().baseCurrent(),"Editing bounds does not silently change NOW semantics");
        menu.clearContents();h.assertTrue(menu.rules().isDefault()&&!menu.baseCurrent(),"Manual reset returns ordinary filter defaults");
        h.assertTrue(CatFilterRules.read(roundTrip(stack)).baseCurrent(),"Saved item round trip retains base mode");

        // Trait boost cannot turn a base-stat failure into an adoption match.
        var boosted=CatTraitProfile.EMPTY.withLevel(CatTrait.SELECTED_ELDER,1);
        var profile=CatAttributeProfile.founder(rng);
        for(var stat:CatStat.values())profile=profile.withValues(stat,50,100);
        profile=profile.withValues(CatStat.ATTACK,39,100);
        h.assertTrue(rules.withBaseCurrent(false).matches(profile,boosted),"Control: ordinary effective filter accepts trait-boosted stats");
        var pancake=new ItemStack(LaoWuMod.CAT_PANCAKE.get());
        CatAttributeData.set(pancake,profile);CatTraitData.set(pancake,boosted);
        h.assertTrue(!new CatFilterItemStack(stack).test(h.getLevel(),pancake,false),"Create wrapper rejects boosted but base-ineligible cat");

        offer(box,true);click(p,pos);rules=CatFilterRules.read(stack);
        h.assertTrue(!rules.baseCurrent()&&rules.logic().currentMask()==0&&rules.logic().limitMask()!=0,"Reimport replaces NOW with MAX, never combines them");
        for(int i=0;i<1500;i++){
            profile=CatAttributeProfile.founder(rng);
            h.assertTrue(rules.matches(profile,boosted,true,false)==box.offer().matches(profile),"MAX exact predicate regardless of time/traits");
        }
        h.assertTrue(box.locked()&&box.offer().maximum(),"Copy never unlocks/rerolls a valid box");
        p.discard();h.setBlock(new BlockPos(1,2,1),Blocks.AIR);finish(h,"reset all old predicates, exact NOW/MAX, item name/count, menu and saved-data retention");
    }

    @GameTest(template="accessory_probe",batch="wish_filter33",timeoutTicks=20)
    public static void importServerGuards(GameTestHelper h){
        var p=player(h);var pos=h.absolutePos(new BlockPos(1,2,1));
        h.setBlock(new BlockPos(1,2,1),LaoWuMod.WISH_ADOPTION_BOX.get());
        var box=(WishAdoptionBoxBlockEntity)h.getLevel().getBlockEntity(pos);offer(box,false);
        var filter=p.getMainHandItem();
        p.setGameMode(GameType.SPECTATOR);click(p,pos);
        h.assertTrue(CatFilterRules.read(filter).isDefault(),"Spectator cannot overwrite a filter");
        p.setGameMode(GameType.SURVIVAL);var start=p.position();p.setPos(start.add(20,0,0));click(p,pos);
        h.assertTrue(CatFilterRules.read(filter).isDefault(),"Remote copy rejected");
        p.setPos(start);p.containerMenu=new CatFilterMenu(72,p.getInventory(),filter);click(p,pos);
        h.assertTrue(CatFilterRules.read(filter).isDefault(),"Open draft cannot race with copying");
        p.containerMenu=p.inventoryMenu;
        h.setBlock(new BlockPos(1,2,2),Blocks.STONE);h.setBlock(new BlockPos(1,3,2),Blocks.STONE);click(p,pos);
        h.assertTrue(CatFilterRules.read(filter).isDefault(),"Solid wall blocks copying");
        h.setBlock(new BlockPos(1,2,2),Blocks.AIR);h.setBlock(new BlockPos(1,3,2),Blocks.AIR);click(p,pos);
        h.assertTrue(!CatFilterRules.read(filter).isDefault(),"Valid right-click succeeds after guards clear");
        h.setBlock(new BlockPos(1,2,1),Blocks.AIR);p.discard();finish(h,"server-only, spectator/range/wall/open-draft guards");
    }

    @GameTest(template="accessory_probe",batch="suit_assembly33",timeoutTicks=40)
    public static void allIntermediateAssemblyChains(GameTestHelper h){
        var names=new ArrayList<String>();
        for(var outfit:CatOutfitType.values())if(outfit!=CatOutfitType.NONE)names.add(outfit.id()+"_suit");
        names.add("cat_component");names.add("cat_grenade");int steps=0;
        for(String name:names){
            var holder=h.getLevel().getRecipeManager().byKey(LaoWuMod.id(name+"_sequenced_assembly")).orElseThrow();
            h.assertTrue(holder.value() instanceof SequencedAssemblyRecipe,"Loaded actual Create sequence "+name);
            var recipe=(SequencedAssemblyRecipe)holder.value();
            var half=recipe.getTransitionalItem();
            h.assertTrue(BuiltInRegistries.ITEM.getKey(half.getItem()).equals(LaoWuMod.id("incomplete_"+name)),"Distinct correct intermediate "+name);
            h.assertTrue(half.getItem() instanceof SequencedAssemblyItem,"Native progress display "+name);
            h.assertTrue(recipe.getLoops()==1&&Math.abs(recipe.getOutputChance()-(name.equals("cat_component")?.8F:1F))<1e-6,"Original success rate preserved "+name);
            ItemStack input=recipe.getIngredient().getItems()[0].copy();
            for(int i=0;i<recipe.getSequence().size();i++){
                var declared=(DeployerApplicationRecipe)recipe.getSequence().get(i).getRecipe();
                var held=declared.getRequiredHeldItem().getItems()[0].copy();
                h.assertTrue(!held.isEmpty(),"Real registered assembly ingredient");
                var items=new ItemStackHandler(2);items.setStackInSlot(0,input);items.setStackInSlot(1,held);
                var wrapper=new RecipeWrapper(items);
                var step=SequencedAssemblyRecipe.getRecipe(h.getLevel(),wrapper,
                        AllRecipeTypes.DEPLOYING.getType(),DeployerApplicationRecipe.class).orElseThrow().value();
                h.assertTrue(step.matches(wrapper,h.getLevel()),"Actual deployer lookup accepts next stage");
                var outputs=step.rollResults(h.getLevel().random);
                h.assertTrue(outputs.size()==1&&outputs.get(0).getCount()==1,"No duplicate or missing assembly output");
                input=roundTrip(outputs.get(0));steps++;
                if(i+1<recipe.getSequence().size()){
                    h.assertTrue(input.is(half.getItem()),"Actual next stage remains artist's intermediate "+name);
                    float progress=((SequencedAssemblyItem)input.getItem()).getProgress(input);
                    h.assertTrue(progress>0&&progress<1&&input.getItem().isBarVisible(input),"Serialized stage and visible progress survive item transfer");
                    h.assertTrue(input.getItem().getBarWidth(input)>0&&input.getItem().getBarWidth(input)<=13,"Valid progress bar");
                }else {
                    var id=BuiltInRegistries.ITEM.getKey(input.getItem()).toString();
                    h.assertTrue(id.equals("laowu:"+name)||(name.equals("cat_component")
                            &&Set.of("create:iron_sheet","laowu:cat_pellet").contains(id)),"Final actual output or original component byproduct "+name);
                }
            }
        }
        h.assertTrue(names.size()==15&&steps==31,"All thirteen suits plus component/grenade, 31 real Create stages");
        finish(h,"15 intermediate types, 31 real deployer stages, save/transfer progress and correct final output");
    }
}
