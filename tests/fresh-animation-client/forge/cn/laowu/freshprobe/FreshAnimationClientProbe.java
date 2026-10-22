package cn.laowu.freshprobe;

import cn.laowu.mod.CatPoseData;
import cn.laowu.mod.client.HissingCatModel;
import cn.laowu.mod.client.HissingCatRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.*;

@Mod.EventBusSubscriber(modid="laowu_fresh_probe",value=Dist.CLIENT)
public final class FreshAnimationClientProbe {
    private static int ticks;
    private static boolean started, done;
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(done)return;
        if(!Boolean.getBoolean("laowu.fresh_probe"))return;
        if(ticks==200&&!started&&mc.level==null&&mc.getSingleplayerServer()==null&&mc.getOverlay()==null)
            mc.createWorldOpenFlows().loadLevel(mc.screen,"Standalone");
        if(++ticks>1600){fail(mc,new AssertionError("Fresh client timeout"));return;}
        if(!started&&mc.level!=null&&mc.player!=null&&mc.getOverlay()==null){
            started=true;
            org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow());
            org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),1280,720);
            mc.options.guiScale().set(2);
            mc.setScreen(new Preview());
        }
    }
    private static void check(boolean value,String text){if(!value)throw new AssertionError(text);}
    private static void fail(Minecraft mc,Throwable failure){done=true;failure.printStackTrace();System.err.println("FAIL: FRESH ANIMATION CLIENT "+failure);mc.stop();}
    private static HissingCatModel liveModel(Minecraft mc,Cat cat) {
        // Use the vanilla owner so the separate probe remaps inherited methods to SRG.
        return (HissingCatModel)((net.minecraft.client.renderer.entity.LivingEntityRenderer<?,?>)
                mc.getEntityRenderDispatcher().getRenderer(cat)).getModel();
    }
    private static net.minecraft.resources.ResourceLocation liveTexture(Minecraft mc,Cat cat) {
        return ((net.minecraft.client.renderer.entity.EntityRenderer<Cat>)
                mc.getEntityRenderDispatcher().getRenderer(cat)).getTextureLocation(cat);
    }
    private static void verifyNativePoseEyes(Minecraft mc,Cat cat)throws Exception {
        var location=liveTexture(mc,cat);
        var texture=mc.getTextureManager().getTexture(location);
        boolean borrowed=texture instanceof net.minecraft.client.renderer.texture.DynamicTexture;
        com.mojang.blaze3d.platform.NativeImage image;
        if(borrowed)image=((net.minecraft.client.renderer.texture.DynamicTexture)texture).getPixels();
        else try(var stream=mc.getResourceManager().getResource(location).orElseThrow().open()){
            image=com.mojang.blaze3d.platform.NativeImage.read(stream);
        }
        var nativeResource=mc.getResourceManager().getResourceStack(cat.getVariant().texture()).stream()
                .filter(resource->resource.sourcePackId().equals("vanilla")).findFirst().orElseThrow();
        try(var stream=nativeResource.open();var base=com.mojang.blaze3d.platform.NativeImage.read(stream)){
            for(int x:new int[]{5,6,8,9})check(image.getPixelRGBA(x,6)==base.getPixelRGBA(x,6),"Native hissing geometry sampled missing resource-pack eye pixels at "+x+",6");
        }finally{if(!borrowed)image.close();}
    }
    private static final class Preview extends Screen {
        private final Cat standing,walking;
        private int frames;
        private Object root;
        private ModelPart pupil,head;
        private float initialPupil,initialHead;
        private boolean moved;
        private java.util.concurrent.CompletableFuture<Void> reload;
        private int reloadStage;
        Preview(){
            super(Component.literal("Fresh Animations compatibility"));
            var mc=Minecraft.getInstance();
            standing=EntityType.CAT.create(mc.level);
            walking=EntityType.CAT.create(mc.level);
            standing.setId(31901);walking.setId(31902);
            standing.setTame(true);
            var genome=cn.laowu.mod.genetics.CatGenome.uniform(new net.minecraft.resources.ResourceLocation("minecraft","black"))
                    .withMaterial(cn.laowu.mod.genetics.CatRegion.LEFT_EYE,new net.minecraft.resources.ResourceLocation("minecraft","siamese"))
                    .withMaterial(cn.laowu.mod.genetics.CatRegion.RIGHT_EYE,new net.minecraft.resources.ResourceLocation("minecraft","red"));
            cn.laowu.mod.genetics.CatGenomeData.set(walking,genome);
            cn.laowu.mod.genetics.CatTraitData.set(walking,cn.laowu.mod.genetics.CatTraitProfile.EMPTY
                    .withLevel(cn.laowu.mod.genetics.CatTrait.STREET_DANCE,1));
            walking.setOnGround(true);
            for(var cat:List.of(standing,walking)) {
                cat.setYRot(180);cat.yBodyRot=cat.yBodyRotO=180;cat.yHeadRot=cat.yHeadRotO=180;
            }
        }
        @Override public boolean isPauseScreen(){return false;}
        @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial){
            var mc=Minecraft.getInstance();
            try{
                if (reload != null) {
                    if (!reload.isDone() || mc.getOverlay() != null) return;
                    reload.join();
                    var liveModel=liveModel(mc,standing);
                    check(liveModel.usesResourcePackModel() == (reloadStage == 2),
                            "Resource reload did not switch native / CEM model");
                    reload=null;
                    if(reloadStage==1) {
                        mc.getResourcePackRepository().setSelected(List.of("vanilla","file/FreshAnimations_v1.10.4.zip"));
                        reloadStage=2; reload=mc.reloadResourcePacks(); return;
                    }
                    System.out.println("PASS: FRESH ANIMATION CLIENT - live pupils, animation, native pose eyes, outfit, collar and pack reload");
                    done=true;mc.stop();return;
                }
                graphics.fill(0,0,width,height,0xff282d35);
                graphics.drawString(mc.font,"Fresh Animations: standing / walking",18,18,0xffffffff);
                standing.tickCount=walking.tickCount=frames;
                walking.walkAnimation.update(.7F,1F);
                var rotation=new Quaternionf().rotateZ((float)Math.PI).rotateY(.35F);
                InventoryScreen.renderEntityInInventory(graphics,width/3,(int)(height*.75F),100,rotation,null,standing);
                InventoryScreen.renderEntityInInventory(graphics,width*2/3,(int)(height*.75F),100,rotation,null,walking);
                graphics.flush();
                if(frames==5)net.minecraft.client.Screenshot.grab(mc.gameDirectory,"fresh-standing-walking.png",mc.getMainRenderTarget(),message->{});
                if(frames++==8){
                    var model=liveModel(mc,standing);
                    root=model.getClass().getMethod("emf$getEMFRootModel").invoke(model);
                    check(root!=null,"Cat renderer has no EMF model root; only resource-pack textures were used");
                    check(root.getClass().getField("containsCustomModel").getBoolean(root),"Renderer bypassed Fresh cat.jem geometry");
                    check(root.getClass().getField("containsCustomAnims").getBoolean(root),"Fresh cat animations were not attached");
                    // EMF restores the public vanilla child map after drawing. Inspect
                    // the active CEM state's animation map rather than that restored map.
                    var animationMap=(Map<?,?>)root.getClass().getMethod("getAllChildPartsAsAnimationMap",String.class,int.class,Map.class)
                            .invoke(root,"root",1,new HashMap<String,String>());
                    var parts=animationMap.values();
                    Map<String,ModelPart> ids=new HashMap<>();
                    for(var part:parts)try{
                        String id=(String)part.getClass().getField("id").get(part);
                        id=id.substring(id.lastIndexOf(':')+1).replace("EMF_","");
                        ids.put(id,(ModelPart)part);
                    }catch(NoSuchFieldException ignored){}
                    System.out.println("FRESH LIVE IDS: "+ids.keySet()+"; keys="+animationMap.keySet());
                    check(ids.containsKey("l_pupil")&&ids.containsKey("r_pupil"),"Fresh eye pupils missing from live renderer");
                    pupil=ids.get("l_pupil");head=ids.get("head2");
                    check(head!=null,"Fresh animated head missing");
                    initialPupil=pupil.x+pupil.y+pupil.xScale;initialHead=head.xRot+head.yRot+head.y;
                    System.out.println("FRESH MODEL: "+root+"; eye parts="+ids.keySet());
                }
                if(head!=null){
                    moved|=Math.abs(initialHead-(head.xRot+head.yRot+head.y))>.001F||Math.abs(initialPupil-(pupil.x+pupil.y+pupil.xScale))>.001F;
                }
                if(frames==100){
                    var texture=mc.getTextureManager().getTexture(liveTexture(mc,walking));
                    check(texture instanceof net.minecraft.client.renderer.texture.DynamicTexture,"Mixed genome texture was not composed");
                    var pixels=((net.minecraft.client.renderer.texture.DynamicTexture)texture).getPixels();
                    for(int[] xy:new int[][]{{1,3},{3,3},{0,4},{1,4},{3,4},{4,4}})
                        check((pixels.getPixelRGBA(xy[0],xy[1])>>>24)!=0,"Mixed genome erased CEM eye plane at "+Arrays.toString(xy));
                    var renderer=(HissingCatRenderer)mc.getEntityRenderDispatcher().getRenderer(walking);
                    var nativePredicate=HissingCatRenderer.class.getDeclaredMethod("usesNativeGeometry",Cat.class);
                    nativePredicate.setAccessible(true);
                    check(!(boolean)nativePredicate.invoke(renderer,walking),"Inactive dance trait disabled walking resource animation");
                    var leftTexture=new net.minecraft.resources.ResourceLocation("minecraft","textures/entity/cat/siamese.png");
                    var rightTexture=new net.minecraft.resources.ResourceLocation("minecraft","textures/entity/cat/red.png");
                    try(var ls=mc.getResourceManager().open(leftTexture);var rs=mc.getResourceManager().open(rightTexture);
                        var left=com.mojang.blaze3d.platform.NativeImage.read(ls);var right=com.mojang.blaze3d.platform.NativeImage.read(rs)) {
                        check(pixels.getPixelRGBA(3,3)==left.getPixelRGBA(3,3),"CEM left pupil lost eye material");
                        check(pixels.getPixelRGBA(1,3)==right.getPixelRGBA(1,3),"CEM right pupil lost eye material");
                    }
                    check(moved,"Resource-pack animation was frozen across rendered frames");
                    net.minecraft.client.Screenshot.grab(mc.gameDirectory,"fresh-animation-later.png",mc.getMainRenderTarget(),message->{});
                    CatPoseData.setPose(standing,CatPoseData.HISSING);
                }
                if(frames==120){
                    net.minecraft.client.Screenshot.grab(mc.gameDirectory,"fresh-hissing.png",mc.getMainRenderTarget(),message->{});
                    verifyNativePoseEyes(mc,standing);
                    CatPoseData.setPose(standing,CatPoseData.PANCAKE);
                }
                if(frames==145){
                    net.minecraft.client.Screenshot.grab(mc.gameDirectory,"fresh-pancake.png",mc.getMainRenderTarget(),message->{});
                    CatPoseData.setPose(standing,CatPoseData.NORMAL);
                }
                if(frames==155)standing.getPersistentData().putString(cn.laowu.mod.CatClothesData.OUTFIT_TAG,cn.laowu.mod.CatOutfitType.TERMINATOR.id());
                if(frames==170) {
                    verifyNativePoseEyes(mc,standing);
                    net.minecraft.client.Screenshot.grab(mc.gameDirectory,"fresh-outfit.png",mc.getMainRenderTarget(),message->{});
                    standing.getPersistentData().remove(cn.laowu.mod.CatClothesData.OUTFIT_TAG);
                }
                if(frames==180){
                    net.minecraft.client.Screenshot.grab(mc.gameDirectory,"fresh-return-to-normal.png",mc.getMainRenderTarget(),message->{});
                    check(liveModel(mc,standing).usesResourcePackModel(),"Normal cat did not return to resource-pack model");
                    mc.getResourcePackRepository().setSelected(List.of("vanilla"));
                    reloadStage=1;reload=mc.reloadResourcePacks();
                }
            }catch(Throwable failure){fail(mc,failure);}
        }
    }
}
