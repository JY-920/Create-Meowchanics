package cn.laowu.mod.client;

import cn.laowu.mod.CatEditorMenu;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.create.CatEditorBlock;
import cn.laowu.mod.item.CatTraitTokenItem;
import cn.laowu.mod.genetics.*;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.List;

/** GPU screenshots of the real screen/model and vanilla menu data, isolated from user saves. */
final class CatEditorClientProbe {
    static void verify(Minecraft mc) throws Exception {
        check(mc.level != null && mc.player != null, "Isolated client world required");
        var previousLanguage = net.minecraft.locale.Language.getInstance();
        Cat source = EntityType.CAT.create(mc.level);
        check(source != null, "Fixture cat creation");
        source.setId(-97003);
        source.setPos(mc.player.position());
        source.setAge(0);
        source.setOrderedToSit(true);
        source.setInSittingPose(true);
        source.setYRot(73);
        source.setXRot(12);
        source.yBodyRot = 39;
        source.yHeadRot = 21;
        // A normal vanilla phenotype is used first; the applied stone phenotype follows below.
        CatGenomeData.set(source, CatGenome.uniform(net.minecraft.resources.ResourceLocation.tryParse("minecraft:red")));
        CatTraitData.set(source, CatTraitProfile.EMPTY.withLevel(CatTrait.THORNS, 3).withLevel(CatTrait.NIGHT_OWL, 2));
        mc.level.putNonPlayerEntity(source.getId(), source);
        try {
            net.minecraft.locale.Language.inject(net.minecraft.client.resources.language.ClientLanguage.loadFrom(
                    mc.getResourceManager(), List.of("en_us", "zh_cn"), false));
            check(Component.translatable("gui.laowu.cat_editor.appearance").getString().equals("外观"),
                    "Actual supplied Chinese language must be loaded");
            for (var entry : new Object[][]{{"textures/block/cat_editor.png", 64, 64}, {"textures/gui/cat_editor.png", 630, 512}, {"textures/item/cat_trait_token.png", 16, 16}})
                try (var stream = mc.getResourceManager().getResource(LaoWuMod.id((String)entry[0])).orElseThrow().open();
                     var image = NativeImage.read(stream)) {
                    check(image.getWidth() == (int)entry[1] && image.getHeight() == (int)entry[2], "Source texture dimensions");
                }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                var state = LaoWuMod.CAT_EDITOR.get().defaultBlockState().setValue(CatEditorBlock.FACING, direction);
                model(mc, mc.getBlockRenderer().getBlockModel(state), state, "block " + direction);
            }
            var item = new ItemStack(LaoWuMod.CAT_EDITOR_ITEM.get());
            model(mc, mc.getItemRenderer().getModel(item, mc.level, null, 0), null, "complete item");
            scene(mc);
            var buf = new FriendlyByteBuf(Unpooled.buffer());
            CatEditorMenu.writeOpeningData(buf, source, BlockPos.ZERO);
            var menu = new CatEditorMenu(97, mc.player.getInventory(), buf);
            buf.release();
            var loadingScreen=new CatEditorScreen(menu,mc.player.getInventory(),Component.literal("Loading"));
            loadingScreen.init(mc,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight());
            gui(mc,loadingScreen,menu,"cat-editor-before-sync.png",true);
            check(buttons(loadingScreen,"拆出")==0&&buttons(loadingScreen,"装入词条")==0,
                    "Before initial server sync no misleading install/extract button is shown");
            sync(menu, source);
            gui(mc,loadingScreen,menu,"cat-editor-first-synced-frame.png",true);
            check(buttons(loadingScreen,"拆出")==2&&buttons(loadingScreen,"装入词条")==1,
                    "The first synchronized frame refreshes controls without waiting for a container tick");
            check(menu.page() == 0 && menu.slots.size() == 52, "Default traits page and 16 + 36 slots");
            var token = CatTraitTokenItem.create(CatTrait.THORNS, 3);
            check(CatTraitTokenItem.traitId(token).equals(CatTrait.THORNS.id()) && CatTraitTokenItem.level(token) == 3,
                    "Lossless level-3 bottle");
            var tooltip = new ArrayList<Component>();
            token.getItem().appendHoverText(token, mc.level, tooltip, net.minecraft.world.item.TooltipFlag.NORMAL);
            var tooltipText = tooltip.stream().map(Component::getString).toList();
            check(tooltipText.stream().anyMatch(s -> s.contains(CatTrait.THORNS.title().getString()) && s.contains("3")),
                    "Localized bottle tooltip includes full level and title");
            check(tooltipText.stream().anyMatch(s -> s.contains(CatTrait.THORNS.id().toString())),
                    "Bottle tooltip retains public trait ID");
            check(tooltipText.contains(CatTrait.THORNS.description(3).getString()), "Bottle tooltip retains real effect");
            var screen = new CatEditorScreen(menu, mc.player.getInventory(), Component.translatable("block.laowu.cat_editor"));
            screen.init(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            check(menu.getSlot(4).x == 151 && menu.getSlot(4).y == 18,
                    "Appearance sample slots must stay on the supplied whole-page artwork");
            check(menu.getSlot(16).y >= 220, "Inventory must not compress or overlap the complete brick footer");
            var previewField = CatEditorScreen.class.getDeclaredField("previewCat");
            previewField.setAccessible(true);
            Cat preview = (Cat)previewField.get(screen);
            check(preview != null && preview != source && !preview.isPassenger(), "Detached real cat clone");
            check(CatTraitData.read(preview).orElseThrow().rawLevel(CatTrait.THORNS) == 3, "Clone preserves visual traits");
            check(buttons(screen, "拆出") == 2 && buttons(screen, "装入词条") == 1, "Compact installed rows and exactly one install row");
            check(menu.displayedTraits().traits().get(0).trait() == CatTrait.THORNS, "Ordered synced cards");
            var traits = gui(mc, screen, menu, "cat-editor-traits.png", true);
            authoredRect(mc,traits,294,80,3,60,342,49,"noticeboard full right vertical border");
            authoredRect(mc,traits,503,234,28,27,181,86,"disabled install complete right edge");
            authoredRect(mc,traits,244,73,43,7,292,42,"NOW MAX complete bottom row");
            double panelScale=mc.getWindow().getGuiScale()*fit(mc);
            for(int px=286;px<341;px++) {
                int ix=(int)((left(mc)+px+.5)*panelScale),iy=mc.getWindow().getHeight()-1-(int)((top(mc)+104.5)*panelScale);
                check(traits[iy*mc.getWindow().getWidth()+ix]==0xFFB3DBF5,"Stat value background matches the noticeboard, not the main sheet");
            }
            menu.getSlot(0).set(CatTraitTokenItem.create(CatTrait.THORNS,1));screen.containerTick();
            var blockedExtract=gui(mc,screen,menu,"cat-editor-disabled-extract.png",true);
            authoredRect(mc,blockedExtract,471,234,28,27,181,27,"disabled extract complete right edge");
            menu.getSlot(0).set(ItemStack.EMPTY);screen.containerTick();
            authoredRect(mc,traits,35,201,201,31,83,170,"traits full-height bricks");
            authoredRect(mc,traits,233,53,64,3,281,22,"traits noticeboard header");
            authoredRect(mc,traits,133,58,28,27,181,27,"complete extract button");
            for (int i=0;i<menu.slots.size();i++) {
                var slot=menu.getSlot(i);
                check(slot.x>=0&&slot.x+16<=408&&slot.y>=0&&slot.y+16<=303, "All actual slots fit panel: "+i);
                if(i>=16)check(slot.y>=225,"Player inventory below editor");
            }
            // Four installed cards fill four rows; there must be no fifth install row.
            CatTraitData.set(source, CatTraitData.read(source).orElseThrow().withLevel(CatTrait.LONG_FUR, 1).withLevel(CatTrait.TOUGH, 1));
            sync(menu,source);screen.containerTick();
            check(menu.installedCount()==4 && buttons(screen,"拆出")==4 && buttons(screen,"装入词条")==0,"Four trait maximum");
            gui(mc,screen,menu,"cat-editor-four-traits.png",true);
            // Compare actual hover screenshots only inside NOW / MAX numeric columns.
            CatTraitData.set(source,CatTraitProfile.EMPTY);sync(menu,source);
            CatAttributeData.set(source,CatAttributeProfile.founder(RandomSource.create(42)));
            var rarityPixels = new ArrayList<int[]>();
            for(var rarity:CatTraitRarity.values()) {
                var trait=java.util.Arrays.stream(CatTrait.values()).filter(t->t.rarity()==rarity).findFirst().orElseThrow();
                menu.getSlot(0).set(CatTraitTokenItem.create(trait,1));screen.containerTick();
                var install=screen.children().stream().filter(c->c instanceof Button b&&b.visible&&b.getMessage().getString().equals("装入词条")).map(c->(Button)c).findFirst().orElseThrow();
                check(install.active,"Valid "+rarity+" bottle enables installation");
                var normal=gui(mc,screen,menu,"cat-editor-"+rarity.serializedName()+".png",true);
                for(var earlier:rarityPixels)check(diff(mc,earlier,normal,233,34,249,50)>0,"All four runtime bottle colors differ");
                rarityPixels.add(normal);
                var hover=gui(mc,screen,menu,"cat-editor-"+rarity.serializedName()+"-hover.png",true,install.getX()+5,install.getY()+5,false);
                int now=diff(mc,normal,hover,292,49,307,102), max=diff(mc,normal,hover,317,49,332,102);
                check((now>0)==(rarity==CatTraitRarity.GOOD||rarity==CatTraitRarity.EXCELLENT),"NOW hover mask "+rarity);
                check((max>0)==(rarity==CatTraitRarity.EXCELLENT),"MAX hover mask "+rarity);
                check(diff(mc,normal,hover,309,49,315,102)==0&&diff(mc,normal,hover,335,49,341,102)==0,"Hover never alters stat tier icons");
            }
            CatTraitData.set(source,CatTraitProfile.EMPTY.withLevel(CatTrait.THORNS,3).withLevel(CatTrait.NIGHT_OWL,2));
            sync(menu,source);screen.containerTick();
            int left=left(mc),top=top(mc);
            var cardPlain=gui(mc,screen,menu,"cat-editor-card.png",true);
            var cardTooltip=gui(mc,screen,menu,"cat-editor-card-tooltip.png",true,left+114,top+34,true);
            check(diff(mc,cardPlain,cardTooltip,0,0,408,303)>100,"Shared scanner card tooltip emits pixels");
            menu.getSlot(4).set(new ItemStack(Items.STONE,2));
            menu.clickMenuButton(mc.player,1);screen.containerTick();
            check(screen.children().stream().filter(c->c instanceof Button b&&b.visible&&b.getClass().getSimpleName().equals("ChoiceButton")).count()==12,"Twelve appearance choices");
            var original=CatGenomeData.getOrFallback(source);
            var sample=CatMaterialRegistry.blockMaterial(new ItemStack(Items.STONE)).orElseThrow();
            check(menu.pendingGenome().material(CatRegion.values()[0]).equals(sample),"Sample preview before commit");
            var appearance=gui(mc,screen,menu,"cat-editor-stone-preview.png",true);
            previewField.set(screen,null);
            var empty=gui(mc,screen,menu,"cat-editor-without-preview.png",true);
            previewField.set(screen,preview);
            check(diff(mc,appearance,empty,321,38,404,123)>150*mc.getWindow().getGuiScale()*mc.getWindow().getGuiScale(),"Real clone emits substantial GPU pixels");
            menu.getSlot(4).set(ItemStack.EMPTY);
            menu.clickMenuButton(mc.player,menu.selectionAction(0,0));screen.containerTick();
            check(CatGenomeData.getOrFallback((Cat)previewField.get(screen)).equals(menu.pendingGenome()),"Draft choice updates clone immediately");
            var draft=gui(mc,screen,menu,"cat-editor-appearance.png",true);
            var lookLeft=gui(mc,screen,menu,"cat-editor-look-left.png",true,left+325,top+76,false);
            var lookRight=gui(mc,screen,menu,"cat-editor-look-right.png",true,left+400,top+76,false);
            check(diff(mc,lookLeft,lookRight,321,38,404,123)>100,"Preview cat follows horizontal pointer motion");
            var lookUp=gui(mc,screen,menu,"cat-editor-look-up.png",true,left+363,top+42,false);
            var lookDown=gui(mc,screen,menu,"cat-editor-look-down.png",true,left+363,top+115,false);
            check(diff(mc,lookUp,lookDown,321,38,404,123)>100,"Preview cat follows vertical pointer motion");
            authoredRect(mc,draft,42,436,301,30,21,171,"appearance full-height bricks");
            authoredRect(mc,draft,340,286,89,3,319,21,"appearance noticeboard header");
            authoredRect(mc,draft,21,303,23,59,0,38,"authored selected tabs");
            authoredRect(mc,draft,82,279,83,5,61,14,"selector complete upper border");
            authoredRect(mc,draft,352,403,28,26,331,138,"reset complete border");
            authoredRect(mc,draft,388,403,28,26,367,138,"confirm complete border");
            var actualScreen=capture(mc,mc.getWindow().getWidth(),mc.getWindow().getHeight(),"cat-editor-actual-screen.png",()->{
                RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight(),0,1000,21000),VertexSorting.ORTHOGRAPHIC_Z);
                var graphics=new GuiGraphics(mc,mc.renderBuffers().bufferSource());
                graphics.pose().translate(0,0,-11000);
                screen.render(graphics,-100,-100,0);graphics.flush();
            });
            authoredRect(mc,actualScreen,42,436,301,30,21,171,"actual screen footer after global scaling");
            var expectedHelp=widgetHelp(mc,screen,true,false,"cat-editor-help-expected.png");
            var actualHelp=widgetHelp(mc,screen,false,true,"cat-editor-help-actual.png");
            check(java.util.Arrays.equals(expectedHelp,actualHelp),"Deferred keyboard button tooltip must share the UI scale and logical bounds");
            check(diff(mc,appearance,draft,321,38,404,123)>100,"Draft changes real clone pixels");
            check(CatGenomeData.getOrFallback(source).equals(original),"Draft does not mutate source genome");
            check(source.getYRot()==73&&source.getXRot()==12&&source.yBodyRot==39&&source.yHeadRot==21&&source.isInSittingPose(),"Preview leaves source pose unchanged");
            menu.clickMenuButton(mc.player,0);screen.containerTick();
            check(menu.getSlot(0).hasItem()&&!menu.getSlot(4).isActive(),"Tab switch retains bottle but hides material slots");
            menu.clickMenuButton(mc.player,1);screen.containerTick();
            check(!menu.getSlot(0).isActive()&&menu.getSlot(4).isActive(),"Appearance hides bottle slots");
            check(menu.clickMenuButton(mc.player,CatEditorMenu.COMMIT),"Client sends confirmation");
            check(!menu.clickMenuButton(mc.player,menu.selectionAction(0,1)),
                    "Awaiting confirmation must reject newer draft edits until its acknowledgement");
            menu.setData(menu.catalog().size()+5,1);
            screen.containerTick();
            check(menu.selection(0)==-1&&menu.pendingGenome().equals(original),
                    "Server confirmation epoch clears client draft choices without exposing old overrides");
            var labels=gui(mc,screen,menu,"cat-editor-labels.png",false);
            int width=mc.getWindow().getWidth(),height=mc.getWindow().getHeight(),count=0;
            double scale=mc.getWindow().getGuiScale()*fit(mc);
            for(int y=0;y<height;y++)for(int x=0;x<width;x++)if((labels[y*width+x]>>>24)!=0) {
                count++;double px=x/scale-left,py=(height-1-y)/scale-top;
                check(px>=-1&&px<409&&py>=-1&&py<304,"Chinese widget pixels stay inside 408x303");
            }
            check(count>1000,"Real Chinese widgets emitted pixels");
            screen.init(mc,320,240);
            float smallScale=Math.min((320-8)/408F,(240-8)/303F);
            var tab=screen.children().stream().filter(c->c instanceof Button b&&b.getMessage().getString().equals("词条")).map(c->(Button)c).findFirst().orElseThrow();
            check(screen.mouseClicked((tab.getX()+5)*smallScale,(tab.getY()+5)*smallScale,0)&&menu.page()==0,
                    "Scaled-down page tab hitbox must follow its artwork");
            screen.mouseReleased((tab.getX()+5)*smallScale,(tab.getY()+5)*smallScale,0);
            check(screen.width*smallScale<=321&&screen.height*smallScale<=241,"Whole composition fits small GUI");
            System.out.println("PASS: CAT EDITOR CLIENT - source-pixel footer/header/tab/button checks; small-GUI scaled hitboxes; 16 temporary slots; scanner tooltip; four rarity hover masks; real clone preview; 408x303 bounds");
        } finally {
            source.discard();
            net.minecraft.locale.Language.inject(previousLanguage);
        }
    }


    private static int[] widgetHelp(Minecraft mc,CatEditorScreen screen,boolean expected,boolean queued,String name) throws Exception {
        var button=screen.children().stream().filter(c->c instanceof Button b&&b.visible&&b.getMessage().getString().equals("确认修改")).map(c->(Button)c).findFirst().orElseThrow();
        var lines=List.of(Component.literal("Confirm appearance").getVisualOrderText());
        var positioner=new net.minecraft.client.gui.screens.inventory.tooltip.BelowOrAboveWidgetTooltipPositioner(button);
        return capture(mc,mc.getWindow().getWidth(),mc.getWindow().getHeight(),name,()->{
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight(),0,1000,21000),VertexSorting.ORTHOGRAPHIC_Z);
            var g=new GuiGraphics(mc,mc.renderBuffers().bufferSource());g.pose().translate(0,0,-11000);
            if(queued)screen.setTooltipForNextRenderPass(lines,positioner,true);
            screen.renderWithTooltip(g,-100,-100,0);
            if(expected){g.pose().pushPose();g.pose().scale(fit(mc),fit(mc),1);
                g.renderTooltip(mc.font,lines,(w,h,x,y,tw,th)->positioner.positionTooltip(screen.width,screen.height,x,y,tw,th),(int)(-100/fit(mc)),(int)(-100/fit(mc)));
                g.pose().popPose();}
            g.flush();
        });
    }
    private static float fit(Minecraft mc) {
        return Math.min(1F,Math.min((mc.getWindow().getGuiScaledWidth()-8)/408F,(mc.getWindow().getGuiScaledHeight()-8)/303F));
    }
    private static int left(Minecraft mc){return ((int)Math.ceil(mc.getWindow().getGuiScaledWidth()/fit(mc))-408)/2;}
    private static int top(Minecraft mc){return ((int)Math.ceil(mc.getWindow().getGuiScaledHeight()/fit(mc))-303)/2;}
    private static void authoredRect(Minecraft mc,int[] actual,int u,int v,int w,int h,int dx,int dy,String name) throws Exception {
        try(var stream=mc.getResourceManager().getResource(LaoWuMod.id("textures/gui/cat_editor.png")).orElseThrow().open();
            var atlas=NativeImage.read(stream)) {
            double scale=mc.getWindow().getGuiScale()*fit(mc);
            int tested=0,mismatch=0,screenWidth=mc.getWindow().getWidth(),screenHeight=mc.getWindow().getHeight();
            for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                int expected=atlas.getPixelRGBA(u+x,v+y);
                if((expected>>>24)!=255)continue;
                int px=(int)((left(mc)+dx+x+.5)*scale),py=screenHeight-1-(int)((top(mc)+dy+y+.5)*scale);
                int found=actual[py*screenWidth+px];
                tested++;if(found!=expected)mismatch++;
            }
            check(tested>20&&mismatch==0,name+" retains every authored pixel: "+mismatch+"/"+tested);
        }
    }

    private static void sync(CatEditorMenu menu,Cat source) {
        menu.setData(menu.catalog().size()+5,0);
        var profile=CatTraitData.read(source).orElse(CatTraitProfile.EMPTY);
        for(int i=0;i<menu.catalog().size();i++)menu.setData(i,profile.rawLevel(menu.catalog().get(i)));
        for(int row=0;row<4;row++)menu.setData(menu.catalog().size()+1+row,row<profile.traits().size()?menu.catalog().indexOf(profile.traits().get(row).trait())+1:0);
    }
    private static long buttons(CatEditorScreen screen,String text) {
        return screen.children().stream().filter(c->c instanceof Button b&&b.visible&&b.getMessage().getString().equals(text)).count();
    }
    private static int diff(Minecraft mc,int[] a,int[] b,int x0,int y0,int x1,int y1) {
        int width=mc.getWindow().getWidth(),height=mc.getWindow().getHeight(),n=0;
        double scale=mc.getWindow().getGuiScale()*fit(mc);
        int left=left(mc),top=top(mc);
        for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
            double px=x/scale-left,py=(height-1-y)/scale-top;
            if(px>=x0&&px<x1&&py>=y0&&py<y1&&a[y*width+x]!=b[y*width+x])n++;
        }
        return n;
    }

    private static void model(Minecraft mc, BakedModel model, BlockState state, String name) {
        check(model != mc.getModelManager().getMissingModel() && !model.isCustomRenderer(), "Static baked OBJ missing: " + name);
        var expected = LaoWuMod.id("block/cat_editor");
        check(model.getParticleIcon().contents().name().equals(expected), "Authored particle not stitched");
        var quads = new ArrayList<net.minecraft.client.renderer.block.model.BakedQuad>();
        quads.addAll(model.getQuads(state, null, RandomSource.create(1)));
        for (var direction : Direction.values()) quads.addAll(model.getQuads(state, direction, RandomSource.create(1)));
        check(quads.size() == 62, "All ten cubes and both paper faces must bake: " + name + " quads=" + quads.size());
        for (var quad : quads) {
            check(!quad.getSprite().contents().name().equals(MissingTextureAtlasSprite.getLocation()) && quad.getSprite().contents().name().equals(expected), "Wrong or missing face sprite");
            var data = quad.getVertices();
            for (int v = 0; v < 4; v++) for (int axis = 0; axis < 3; axis++)
                check(Float.isFinite(Float.intBitsToFloat(data[v * (data.length / 4) + axis])), "Degenerate baked position");
        }
    }

    private static int[] gui(Minecraft mc, CatEditorScreen screen, CatEditorMenu menu, String name, boolean complete) throws Exception {
        return gui(mc,screen,menu,name,complete,-100,-100,false);
    }
    private static int[] gui(Minecraft mc,CatEditorScreen screen,CatEditorMenu menu,String name,boolean complete,int mouseX,int mouseY,boolean tooltip) throws Exception {
        int guiWidth = mc.getWindow().getGuiScaledWidth(), guiHeight = mc.getWindow().getGuiScaledHeight();
        int left = left(mc), top = top(mc);
        // InventoryScreen's scissor is in the real Window pixel coordinate system.
        // Match that exact system, rather than drawing it into a mismatched synthetic FBO.
        return capture(mc, mc.getWindow().getWidth(), mc.getWindow().getHeight(), name, () -> {
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, guiWidth, guiHeight, 0, 1000, 21000), VertexSorting.ORTHOGRAPHIC_Z);
            var g = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
            g.pose().translate(0, 0, -11000);
            g.pose().scale(fit(mc),fit(mc),1);
            if (complete) {
                g.fill(0, 0, guiWidth, guiHeight, 0xFF24282D);
                screen.renderBg(g, 0, mouseX, mouseY);
            }
            for (var child : screen.children()) if (child instanceof Button b) b.render(g, mouseX, mouseY, 0);
            g.pose().pushPose(); g.pose().translate(left, top, 0);
            screen.renderLabels(g, mouseX, mouseY); g.pose().popPose();
            if (complete) for (var slot : menu.slots) if (slot.isActive() && slot.hasItem()) {
                g.renderItem(slot.getItem(), left + slot.x, top + slot.y);
                g.renderItemDecorations(mc.font, slot.getItem(), left + slot.x, top + slot.y);
            }
            if(tooltip)check(CatTraitCardRenderer.renderTooltip(g,mc.font,menu.displayedTraits(),left+104,top+27,29,mouseX,mouseY),"Scanner tooltip hit");
            g.flush();
        });
    }

    private static void scene(Minecraft mc) throws Exception {
        var pixels = capture(mc, 800, 360, "cat-editor-models.png", () -> {
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 800, 360, 0, 1000, 21000), VertexSorting.ORTHOGRAPHIC_Z);
            com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
            var buffers = mc.renderBuffers().bufferSource();
            int column = 0;
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                var poses = new PoseStack();
                poses.translate(100 + 200 * column++, 285, -11000);
                poses.scale(170, -170, 170);
                poses.mulPose(Axis.XP.rotationDegrees(25)); poses.mulPose(Axis.YP.rotationDegrees(225)); poses.translate(-.5, 0, -.5);
                mc.getBlockRenderer().renderSingleBlock(LaoWuMod.CAT_EDITOR.get().defaultBlockState().setValue(CatEditorBlock.FACING, facing),
                        poses, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
            buffers.endBatch();
        });
        for (int column = 0; column < 4; column++) {
            int visible = 0;
            for (int y = 0; y < 360; y++) for (int x = column * 200; x < (column + 1) * 200; x++)
                if ((pixels[y * 800 + x] >>> 24) != 0) visible++;
            check(visible > 5000, "Facing " + column + " body is transparent/missing despite baked quads: pixels=" + visible);
        }
    }

    private static int[] capture(Minecraft mc, int width, int height, String name, Runnable draw) throws Exception {
        var projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        var view = RenderSystem.getModelViewStack();
        view.pushPose(); view.setIdentity(); RenderSystem.applyModelViewMatrix();
        TextureTarget output = null;
        float[] clear = new float[4]; org.lwjgl.opengl.GL11.glGetFloatv(org.lwjgl.opengl.GL11.GL_COLOR_CLEAR_VALUE, clear);
        try (var guard = new CatPerformanceOutline.State(); var target = new PerformanceSceneSnapshot.Target()) {
            output = new TextureTarget(width, height, true, Minecraft.ON_OSX); output.bindWrite(true);
            RenderSystem.clearColor(0, 0, 0, 0); RenderSystem.clearDepth(1); RenderSystem.depthMask(true);
            RenderSystem.clear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT | org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            RenderSystem.setShaderColor(1, 1, 1, 1); RenderSystem.enableDepthTest(); RenderSystem.enableCull();
            draw.run();
            try (var image = new NativeImage(width, height, false)) {
                RenderSystem.bindTexture(output.getColorTextureId()); image.downloadTexture(0, false);
                var pixels = new int[width * height];
                for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) pixels[y * width + x] = image.getPixelRGBA(x, y);
                image.flipY(); image.writeToFile(java.nio.file.Path.of(name));
                return pixels;
            }
        } finally {
            if (output != null) output.destroyBuffers();
            view.popPose(); RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection, sorting);
            RenderSystem.clearColor(clear[0], clear[1], clear[2], clear[3]);
        }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
