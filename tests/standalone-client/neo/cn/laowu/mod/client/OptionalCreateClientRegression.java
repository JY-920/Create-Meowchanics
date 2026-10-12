package cn.laowu.mod.client;
import cn.laowu.mod.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
/** Exercises production item rendering and actual localized suit tooltip output. */
final class OptionalCreateClientRegression {
    static void verifyStandaloneExtras(Minecraft mc) {
        for(var id:java.util.List.of("giant_cat_boss_spawn_egg","cat_engineer_goggles"))
            if(!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(LaoWuMod.id(id)))
                throw new AssertionError("Core item missing: "+id);
        var oldHead=mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        var oldHit=mc.hitResult;
        var stack=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(LaoWuMod.id("cat_engineer_goggles")));
        mc.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,stack);
        if(!CatStatsGoggleOverlay.isWearingCatGoggles(mc.player))
            throw new AssertionError("Core goggles cannot activate the cat stats overlay");
        var worn=mc.getModelManager().getModel(net.minecraft.client.resources.model.ModelResourceLocation.standalone(LaoWuMod.id("item/cat_engineer_goggles_worn")));
        var itemModel=mc.getItemRenderer().getModel(stack,mc.level,mc.player,0);
        if(worn==mc.getModelManager().getMissingModel()||!(itemModel instanceof CatEngineerGogglesModel))
            throw new AssertionError("Worn goggles model or HEAD wrapper missing");
        mc.getItemRenderer().renderStatic(stack,ItemDisplayContext.HEAD,net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,new com.mojang.blaze3d.vertex.PoseStack(),
                mc.renderBuffers().bufferSource(),mc.level,0);
        var cat=net.minecraft.world.entity.EntityType.CAT.create(mc.level);
        cat.setPos(mc.player.position());
        cn.laowu.mod.genetics.CatAttributeData.set(cat,cn.laowu.mod.genetics.CatAttributeProfile.founder(cat.getRandom()));
        mc.hitResult=new net.minecraft.world.phys.EntityHitResult(cat);
        if(CatWorldTarget.find(mc,5)==null)throw new AssertionError("Overlay fixture must have a real cat target");
        verifyStatsPanel(mc);
        if(net.neoforged.fml.ModList.get().isLoaded("curios"))
            StandaloneGogglesCuriosRegression.render(mc,stack);
        mc.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,oldHead);
        mc.hitResult=oldHit;
        cat.discard();
        System.out.println("PASS: GIANT EGG + WORN GOGGLES MODEL + ACTUAL CAT STATS OVERLAY; Create="+cn.laowu.mod.compat.create.CreateIntegration.isLoaded());
    }
    static void renderCoreItems(Minecraft mc) {
        var output=mc.renderBuffers().bufferSource();
        for(var item:net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            if(!id.getNamespace().equals("laowu"))continue;
            var stack=new ItemStack(item);
            if(mc.getItemRenderer().getModel(stack,mc.level,null,0)==mc.getModelManager().getMissingModel())
                throw new AssertionError("Missing registered item model: "+id);
            var contexts=id.getPath().contains("breeding_box")?ItemDisplayContext.values():new ItemDisplayContext[]{ItemDisplayContext.GUI};
            for(var context:contexts) {
                mc.getItemRenderer().renderStatic(stack,context,net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,new com.mojang.blaze3d.vertex.PoseStack(),output,mc.level,0);
                output.endBatch();
            }
        }
        System.out.println("PASS: ACTUAL ALL CORE ITEMS + ALL BREEDING ITEM DISPLAY CONTEXTS");
    }
    static void verifyTooltipsAndIcon(Minecraft mc) {
        boolean createLoaded=cn.laowu.mod.compat.create.CreateIntegration.isLoaded();
        if(!LaoWuMod.LAOWU_TAB.get().getIconItem().is(LaoWuMod.CAT_INGOT.get()))
            throw new AssertionError("Creative tab icon must be cat ingot");
        var independent=java.util.EnumSet.of(CatOutfitType.FLIGHT,CatOutfitType.DIVING,CatOutfitType.MUSIC,CatOutfitType.COCKROACH,CatOutfitType.TRANSPORT);
        for(var outfit:CatOutfitType.values()) {
            if(outfit==CatOutfitType.NONE)continue;
            var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(LaoWuMod.id(outfit.id()+"_suit"));
            var stack=new ItemStack(item);
            for(int mode=0;mode<3;mode++) {
                var lines=new java.util.ArrayList<Component>();lines.add(stack.getHoverName());
                var event=new net.neoforged.neoforge.event.entity.player.ItemTooltipEvent(stack,null,lines,TooltipFlag.Default.NORMAL,Item.TooltipContext.EMPTY);
                CareerSuitTooltip.modify(event,item,outfit,mode==2,mode==1);
                var text=lines.stream().map(Component::getString).collect(java.util.stream.Collectors.joining(""));
                var content=compact(text);
                if(text.contains(".tooltip.")||text.contains("item.laowu.")||text.contains("screen.laowu."))
                    throw new AssertionError("Untranslated tooltip: "+outfit+" mode="+mode+": "+text);
                if(createLoaded) {
                    var unavailable=Component.translatable("item.laowu.career_suit.work",Component.translatable("item.laowu.career_suit.no_work_without_create")).getString();
                    if(mode==0&&content.contains(compact(unavailable)))throw new AssertionError("Create work incorrectly disabled: "+outfit);
                    if(mode!=2) {
                        var work=Component.translatable(item.getDescriptionId()+(mode==0?".tooltip.work":".tooltip.behaviour2")).getString().replace("_","");
                        if(!content.contains(compact(work)))throw new AssertionError("Original Create work missing: "+outfit+": "+text);
                    }
                    continue;
                }
                if(independent.contains(outfit)&&mode!=2) {
                    String suffix=outfit==CatOutfitType.TRANSPORT?".tooltip.standalone"
                            :mode==1?".tooltip.behaviour2"
                            :outfit==CatOutfitType.FLIGHT||outfit==CatOutfitType.DIVING?".tooltip.standalone":".tooltip.work";
                    String expected=Component.translatable(item.getDescriptionId()+suffix).getString();
                    if(!content.contains(compact(expected)))throw new AssertionError("Independent ability instructions missing: "+outfit+" mode="+mode+": "+text);
                    if(mode==1&&!content.contains(compact(Component.translatable("item.laowu.career_suit.exclusive_details").getString())))
                        throw new AssertionError("Independent Ctrl section misclassified: "+outfit);
                }
                if(mode==0) {
                    var disabled=Component.translatable("item.laowu.career_suit.work",Component.translatable("item.laowu.career_suit.no_work_without_create")).getString();
                    if(!content.contains(compact(disabled)))throw new AssertionError("No-Create suit must report no industrial work: "+outfit+": "+text);
                    var exclusive=Component.translatable("item.laowu.career_suit.exclusive","").getString().trim();
                    if(content.contains(compact(exclusive))!=independent.contains(outfit))throw new AssertionError("Independent ability classified incorrectly: "+outfit+": "+text);
                } else if(mode==1&&!independent.contains(outfit)) {
                    if(!content.contains(compact(Component.translatable("item.laowu.career_suit.no_work_without_create").getString())))
                        throw new AssertionError("Ctrl details still claim unavailable work: "+outfit+": "+text);
                }
                if((outfit==CatOutfitType.FLIGHT||outfit==CatOutfitType.DIVING)&&mode!=2&&text.toLowerCase(java.util.Locale.ROOT).contains("wrench"))throw new AssertionError("Standalone riding tooltip still requires wrench");
                if(!independent.contains(outfit)&&mode!=2) {
                    var old=Component.translatable(item.getDescriptionId()+(mode==0?".tooltip.work":".tooltip.behaviour2")).getString().replace("_","");
                    if(old.length()>20&&content.contains(compact(old)))throw new AssertionError("Unavailable work instructions leaked: "+outfit);
                }
                if(outfit==CatOutfitType.AGENT&&mode==2&&content.contains(compact(Component.translatable("item.laowu.career_suit.snapshot.watch_radius","#").getString().split("#")[0])))
                    throw new AssertionError("Shift preview advertises unavailable seat watch");
            }
        }
        System.out.println("PASS: ALL 13 SUIT NORMAL/CTRL/SHIFT TOOLTIPS + CAT INGOT TAB; Create="+createLoaded);
    }
    private static String compact(String text) {
        return text.replaceAll("\\s+","").replace("_","");
    }
    static void verifyStatsPanel(Minecraft mc) {
        var graphics=new OverlayRecordingGraphics(mc);
        CatStatsGoggleOverlay.OVERLAY.render(graphics,mc.getTimer());
        graphics.flush();
        if(graphics.panelDraws!=1)throw new AssertionError("Goggles HUD must actually draw exactly one stats panel, not return early");
    }
    private static final class OverlayRecordingGraphics extends net.minecraft.client.gui.GuiGraphics {
        int panelDraws;
        OverlayRecordingGraphics(Minecraft mc) {super(mc,mc.renderBuffers().bufferSource());}
        @Override public void blit(net.minecraft.resources.ResourceLocation texture,int x,int y,float u,float v,int w,int h,int tw,int th) {
            if(texture.equals(LaoWuMod.id("textures/gui/cat_stats_panel_limits.png")))panelDraws++;
            super.blit(texture,x,y,u,v,w,h,tw,th);
        }
    }
}
