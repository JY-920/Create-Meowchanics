package cn.laowu.mod.client;
import cn.laowu.mod.*;
import cn.laowu.mod.entity.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.pipeline.TextureTarget;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;
import java.util.*;

/** Real resource bake, particle provider, restored-pose blends and registered honey renderer. */
public final class CareerAccessoryVisualProbe {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("laowu",path);}
    public static void verify(Minecraft mc){
        try{
            int count=0;
            for(var entry:cn.laowu.mod.accessory.CatAccessoryItems.DEFAULTS.values()){
                var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.item()));
                var stack=new ItemStack(item);
                var model=mc.getItemRenderer().getModel(stack,null,null,0);
                check(!model.getParticleIcon().contents().name().equals(MissingTextureAtlasSprite.getLocation()),
                        "Baked accessory has a real texture: "+entry.item());
                var expected=ResourceLocation.parse(entry.item().replace("laowu:","laowu:item/"));
                check(model.getParticleIcon().contents().name().equals(expected),"Exact artist sprite on the actual baked model: "+entry.item());
                check(!stack.getHoverName().getString().startsWith("item.laowu."),"Translated accessory "+entry.item());
                count++;
            }
            check(count==36,"All 36 accessories baked");
            var texture=((TextureAtlas)mc.getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES)).getSprite(ResourceLocation.fromNamespaceAndPath("minecraft","big_smoke_0"));
            check(!texture.contents().name().equals(MissingTextureAtlasSprite.getLocation()),"Vanilla smoke sprite loaded");
            var sprites=new SpriteSet(){
                public TextureAtlasSprite get(int age,int max){return texture;}
                public TextureAtlasSprite get(net.minecraft.util.RandomSource random){return texture;}
            };
            // A smoke constructor only reads Level.random; no client connection, world or loaded chunks.
            // Allocate a test-only level shell to run the real factory without opening a game save.
            var unsafeField=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);
            var unsafe=(sun.misc.Unsafe)unsafeField.get(null);
            var shell=(net.minecraft.client.multiplayer.ClientLevel)unsafe.allocateInstance(net.minecraft.client.multiplayer.ClientLevel.class);
            var random=net.minecraft.world.level.Level.class.getDeclaredField("random");random.setAccessible(true);
            random.set(shell,net.minecraft.util.RandomSource.create(26));
            var particle=new CatHealingSmokeParticle.Provider(sprites).createParticle(LaoWuMod.CAT_HEALING_SMOKE.get(),shell,0,0,0,0,0,0);
            float r=field(particle,Particle.class,"rCol"),g=field(particle,Particle.class,"gCol"),b=field(particle,Particle.class,"bCol");
            check(g>r*2&&g>b*2,"Actual campfire particle factory creates green, not grey smoke");
            var smoke=new CatHealingSmokeParticle.SmokeProvider(sprites).createParticle(LaoWuMod.CAT_AGENT_SMOKE.get(),shell,0,0,0,0,0,0);
            for(var puff:List.of(particle,smoke)){
                check(puff instanceof CatHealingSmokeParticle,"Local campfire-style puff; no vanilla particle modification");
                check(puff.getRenderType()==ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT,"Both smoke types use translucent blending");
                check(Math.abs(field(puff,Particle.class,"alpha")-.32F)<.001,"Both puffs are only 32 percent opaque");
            }
            check(field(smoke,Particle.class,"rCol")==field(smoke,Particle.class,"gCol"),"Normal cover smoke stays neutral grey");
            verifySpeedStatState();
            verifyTransitions();
            honey(mc);
            System.out.println("PASS: 36 artist icons actually baked, client music +20 Speed, green-smoke factory, per-cat continuous original-height poses and real honey GPU decal");
        }catch(Exception e){throw new IllegalStateException(e);}
    }
    private static void verifySpeedStatState(){
        var level=new AgentWatchVisualProbe.ProbeLevel();
        var cat=new Cat(EntityType.CAT,level);cat.setId(94391);
        cn.laowu.mod.genetics.CatTraitData.set(cat,cn.laowu.mod.genetics.CatTraitProfile.EMPTY);
        var genes=cn.laowu.mod.genetics.CatAttributeData.ensure(cat);
        var speed=cn.laowu.mod.genetics.CatStat.SPEED;
        cn.laowu.mod.genetics.CatAttributeData.set(cat,genes.withValues(speed,50,100));
        int before=cn.laowu.mod.genetics.CatAttributeEffects.effectiveValue(cat,speed);
        var state=new net.minecraft.nbt.CompoundTag();state.putInt("MusicSpeedBonus",20);
        cat.getPersistentData().put(cn.laowu.mod.accessory.CatAccessories.CLIENT_STATE,state);
        check(cn.laowu.mod.genetics.CatAttributeEffects.effectiveValue(cat,speed)==before+20,"Real client attribute panel path includes music +20 Speed");
        check(cn.laowu.mod.genetics.CatAttributeData.ensure(cat).current(speed)==50,"Displayed temporary boost never mutates client genes");
        state.putInt("MusicSpeedBonus",0);
        check(cn.laowu.mod.genetics.CatAttributeEffects.effectiveValue(cat,speed)==before,"Client panel removes expired music Speed");
    }
    private static float field(Object target,Class<?> type,String name)throws Exception{
        var f=type.getDeclaredField(name);f.setAccessible(true);return f.getFloat(target);
    }
    private static void verifyTransitions(){
        var level=new AgentWatchVisualProbe.ProbeLevel();
        for(int mode=0;mode<3;mode++){
            var cat=new Cat(EntityType.CAT,level);cat.setId(94400+mode);
            var other=new Cat(EntityType.CAT,level);other.setId(cat.getId());other.setUUID(cat.getUUID());
            var root=HissingCatModel.createLayer().bakeRoot();
            ModelPart[] parts=List.of("head","body","left_hind_leg","right_hind_leg","left_front_leg","right_front_leg","tail1","tail2").stream().map(root::getChild).toArray(ModelPart[]::new);
            float previous=0;
            for(int frame=0;frame<=80;frame++){
                level.clock=1000+frame/4;
                float partial=(frame%4)/4F;
                boolean active=frame>=4&&frame<40;
                var blend=CatPoseTransitions.sample(cat,active&&mode<2,active&&mode==2?2:0,partial);
                check(blend.equals(CatPoseTransitions.sample(cat,active&&mode<2,active&&mode==2?2:0,partial)),"Same-frame render passes must not advance twice");
                check(CatPoseTransitions.sample(other,false,0,partial).ride()==0&&CatPoseTransitions.sample(other,false,0,partial).dash()==0,
                        "Even a matching-ID preview has independent transition state");
                for(var p:parts)p.resetPose();parts[1].xRot=(float)Math.PI/2;
                if(mode<2)CatPoseTransitions.apply(blend.ride(),()->CatRideAnimation.apply(10,parts[0],parts[1],parts[2],parts[3],parts[4],parts[5],parts[6],parts[7]),parts);
                else CatPoseTransitions.apply(blend.dash(),()->CatCockroachAnimation.dash(blend.age(),parts[2],parts[3],parts[4],parts[5]),parts);
                check(Math.abs(parts[4].xRot-previous)<.16,"Quarter-tick pose transitions remain continuous, mode "+mode+" frame "+frame);
                check(Math.abs(parts[4].y-parts[4].getInitialPose().y-(mode==2?3*blend.dash():0))<.001,"Restored original forelimb height");
                previous=parts[4].xRot;
                if(frame==32)check(parts[4].xRot<-1.5,"Full old extended pose reached");
                if(frame==80)check(Math.abs(parts[4].xRot)<.001&&blend.wings()==0,"Exit returns fully to idle");
            }
        }
        // Exercise the registered model's actual receive -> setupAnim path for cockroach transitions.
        var cat=new Cat(EntityType.CAT,level);cat.setId(94450);
        cat.getPersistentData().putBoolean(CatClothesData.EQUIPPED_TAG,true);
        cat.getPersistentData().putString(CatClothesData.OUTFIT_TAG,"cockroach");
        var root=HissingCatModel.createLayer().bakeRoot();var model=new HissingCatModel(root);
        float previous=0;
        for(int frame=0;frame<30;frame++){
            level.clock=2000+frame;CatCockroachSwarm.receive(cat,0,frame>0&&frame<14?2:0,frame);
            model.prepareMobModel(cat,0,0,0);model.setupAnim(cat,0,0,cat.tickCount,0,0);
            float angle=root.getChild("left_front_leg").xRot;
            check(Math.abs(angle-previous)<.6,"Actual model cockroach animation enters/exits without a hard snap");
            previous=angle;
        }
    }
    private static void honey(Minecraft mc)throws Exception{
        TextureTarget output=null;
        var projection=new Matrix4f(RenderSystem.getProjectionMatrix());var sorting=RenderSystem.getVertexSorting();
        var stack=RenderSystem.getModelViewStack();stack.pushPose();stack.setIdentity();RenderSystem.applyModelViewMatrix();
        float[] clear=new float[4];GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE,clear);
        double depth=GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
        try(var guard=new CatPerformanceOutline.State();var framebuffer=new PerformanceSceneSnapshot.Target()){
            output=new TextureTarget(384,384,true,Minecraft.ON_OSX);output.bindWrite(true);
            RenderSystem.clearColor(0,0,0,0);RenderSystem.clearDepth(1);RenderSystem.depthMask(true);
            RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT,Minecraft.ON_OSX);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(-2,2,-2,2,-10,10),VertexSorting.ORTHOGRAPHIC_Z);
            RenderSystem.setShaderColor(1,1,1,1);RenderSystem.enableDepthTest();RenderSystem.enableCull();
            var entity=new CatHoneyPatch(LaoWuMod.CAT_HONEY_PATCH.get(),new AgentWatchVisualProbe.ProbeLevel());
            var renderer=mc.getEntityRenderDispatcher().getRenderer(entity);
            check(renderer instanceof CatHoneyPatchRenderer,"Registered honey entity renderer");
            var poses=new PoseStack();poses.mulPose(new Quaternionf().rotationX((float)Math.PI/2));
            var buffers=mc.renderBuffers().bufferSource();renderer.render(entity,0,0,poses,buffers,LightTexture.FULL_BRIGHT);buffers.endBatch();
            int gold=0;
            try(var pixels=new NativeImage(384,384,false)){
                RenderSystem.bindTexture(output.getColorTextureId());pixels.downloadTexture(0,false);
                for(int x=0;x<384;x++)for(int y=0;y<384;y++){
                    int c=pixels.getPixelRGBA(x,y),r=c&255,g=(c>>>8)&255,b=(c>>>16)&255;
                    if(r>40&&g>20&&r>b*1.5&&g>b*1.2)gold++;
                }
                pixels.flipY();pixels.writeToFile(java.nio.file.Path.of("career-accessories-honey.png"));
            }
            check(gold>10000,"Honey decal visible from above with culling enabled: "+gold);
            System.out.println("PASS: registered temporary honey decal has "+gold+" golden GPU pixels using vanilla honey texture");
        }finally{
            if(output!=null)output.destroyBuffers();
            stack.popPose();RenderSystem.applyModelViewMatrix();RenderSystem.setProjectionMatrix(projection,sorting);
            RenderSystem.clearColor(clear[0],clear[1],clear[2],clear[3]);RenderSystem.clearDepth(depth);
        }
    }
    private CareerAccessoryVisualProbe(){}
}
