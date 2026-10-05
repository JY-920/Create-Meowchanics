package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.entity.GiantCatBoss;
import cn.laowu.mod.genetics.CatGenomeData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import java.nio.file.Files;
import java.nio.file.Path;

/** Real registered boss renderer/texture and GPU frames; never included in runtime jars. */
final class GiantCatBossVisualProbe {
    @SuppressWarnings("unchecked")
    private static <T> void synced(GiantCatBoss boss,String name,T value) throws Exception {
        var field=GiantCatBoss.class.getDeclaredField(name);field.setAccessible(true);
        boss.getEntityData().set((net.minecraft.network.syncher.EntityDataAccessor<T>)field.get(null),value);
    }
    static void verify(Minecraft mc) throws Exception {
        var boss=new GiantCatBoss(LaoWuMod.GIANT_CAT_BOSS.get(),mc.level);
        boss.setId(97501);boss.setOnGround(true);
        var renderer=mc.getEntityRenderDispatcher().getRenderer(boss);
        CatSixWayClientProbe.check(renderer instanceof GiantCatBossRenderer,"Registered giant boss uses its dedicated renderer");
        var original=new Cat(EntityType.CAT,mc.level);
        original.setVariant(BuiltInRegistries.CAT_VARIANT.get(ResourceLocation.fromNamespaceAndPath("minecraft","calico")));
        boss.setInheritedGenome(CatGenomeData.getOrFallback(original));
        var texture=renderer.getTextureLocation(boss);
        CatSixWayClientProbe.check(mc.getResourceManager().getResource(texture).isPresent(),"Inherited boss coat resolves a real texture");
        // Invoke the actual client body-update entry point. Ordinary boss head
        // bones are not independently aimed, so the look controller cannot
        // give the visible body a second direction while it turns in place.
        var bodyTick = net.minecraft.world.entity.Mob.class.getDeclaredMethod("tickHeadTurn", float.class, float.class);
        bodyTick.setAccessible(true);
        for (float heading : new float[]{170, 179, -179, -170}) {
            boss.setYRot(heading); boss.yHeadRot = 0; boss.yBodyRot = 75;
            bodyTick.invoke(boss, heading, 0F);
            CatSixWayClientProbe.check(Math.abs(net.minecraft.util.Mth.wrapDegrees(boss.yBodyRot - heading)) < .001F,
                    "Client ordinary body follows synchronized smooth yaw, not stationary head look: " + heading);
        }
        boss.setYRot(0); boss.yRotO = 0; boss.yBodyRot = 0; boss.yBodyRotO = 0;
        System.out.println("PASS: GIANT BOSS CLIENT PURSUIT HEADING");
        Files.createDirectories(Path.of("giant-boss-frames"));
        byte[] phases={GiantCatBoss.SUMMON,GiantCatBoss.IDLE,GiantCatBoss.MELEE,GiantCatBoss.ROLL_WINDUP,
                GiantCatBoss.ROLL,GiantCatBoss.ROLL_RECOVERY,GiantCatBoss.JUMP_WINDUP,GiantCatBoss.JUMP,GiantCatBoss.SLAM_RECOVERY,
                GiantCatBoss.SLAM_RECOVERY,GiantCatBoss.ROLL_RECOVERY,GiantCatBoss.SLAM_RECOVERY,
                GiantCatBoss.ROLL_RECOVERY,GiantCatBoss.ROLL_RECOVERY,GiantCatBoss.ROLL_RECOVERY};
        float[][] times={{0,30,59},{0,10,20},{0,5,10},{0,8,16},
                {0,5,10,15,20,40,42,44,46,48,50,52,54,56,57,58,59,60},
                {0,1,2,4,8,15,30},{0,12,24},{0,2,5,10,20},{0,10,25,40},
                {0,2,5,8,20,40},{0,5,15,30},{0,5,15,40},
                {0,1,2,4,6,8,10,12,16,20,30},
                {0,1,2,4,6,8,10,12,16,20,30},
                {0,1,2,4,6,8,10,12,16,20,30}};
        var capture=GiantCatVisualProbe.class.getDeclaredMethod("capture",Minecraft.class,String.class,Runnable.class);
        capture.setAccessible(true);
        var hashes=new java.util.HashSet<Long>();
        int frames=0;
        for(int i=0;i<phases.length;i++) for(float at:times[i]){
            synced(boss,"DATA_PHASE",phases[i]);
            synced(boss,"DATA_PHASE_START",mc.level.getGameTime()-(long)at);
            synced(boss,"DATA_RECOVERY_FROM_PHASE",(byte)(i==10?GiantCatBoss.ROLL_WINDUP
                    :i==11?GiantCatBoss.JUMP_WINDUP:phases[i]==GiantCatBoss.ROLL_RECOVERY?GiantCatBoss.ROLL:GiantCatBoss.JUMP));
            synced(boss,"DATA_RECOVERY_FROM_TICKS",i==9?2:i==10?8:i==11?10:i==12?27:i==13?7:i==14?48
                    :phases[i]==GiantCatBoss.ROLL_RECOVERY?60:20);
            boss.tickCount=(int)at+100;
            Object frame=capture.invoke(null,mc,"giant-boss-frames/"+i+"-"+(int)at+".png",(Runnable)()->{
                var pose=new PoseStack();pose.translate(320,350,-11000);
                pose.scale(42,-42,42);pose.mulPose(Axis.XP.rotationDegrees(20));pose.mulPose(Axis.YP.rotationDegrees(-55));
                var buffers=mc.renderBuffers().bufferSource();
                renderer.render(boss,0,.5F,pose,buffers,LightTexture.FULL_BRIGHT);buffers.endBatch();
            });
            var count=frame.getClass().getDeclaredMethod("pixels");count.setAccessible(true);
            var clipped=frame.getClass().getDeclaredMethod("clipped");clipped.setAccessible(true);
            var hash=frame.getClass().getDeclaredMethod("hash");hash.setAccessible(true);
            CatSixWayClientProbe.check((int)count.invoke(frame)>150,"Boss phase emits real geometry: "+i+"/"+at);
            CatSixWayClientProbe.check(!(boolean)clipped.invoke(frame),"Complete boss silhouette: "+i+"/"+at);
            hashes.add((long)hash.invoke(frame));frames++;
        }
        // Orthographic front/side keys show grounded rump/rear paws, level
        // muzzle, vertical torso, and front paws suspended at mid-chest.
        synced(boss,"DATA_PHASE",GiantCatBoss.SLAM_RECOVERY);
        synced(boss,"DATA_PHASE_START",mc.level.getGameTime());
        synced(boss,"DATA_RECOVERY_FROM_PHASE",GiantCatBoss.JUMP);
        synced(boss,"DATA_RECOVERY_FROM_TICKS",20);
        boss.setYRot(0);boss.yRotO=0;boss.yBodyRot=0;boss.yBodyRotO=0;
        for(int view=0;view<2;view++) {
            final int angle=view*90;
            Object frame=capture.invoke(null,mc,"giant-boss-frames/seated-"+(view==0?"front":"side")+".png",(Runnable)()->{
                var pose=new PoseStack();pose.translate(320,350,-11000);pose.scale(42,-42,42);
                pose.mulPose(Axis.YP.rotationDegrees(angle));
                var buffers=mc.renderBuffers().bufferSource();
                renderer.render(boss,0,0,pose,buffers,LightTexture.FULL_BRIGHT);buffers.endBatch();
            });
            var count=frame.getClass().getDeclaredMethod("pixels");count.setAccessible(true);
            var clipped=frame.getClass().getDeclaredMethod("clipped");clipped.setAccessible(true);
            CatSixWayClientProbe.check((int)count.invoke(frame)>150&&!(boolean)clipped.invoke(frame),
                    "Seated orthographic silhouette is complete: "+view);
            frames++;
        }
        // BodyRotationControl must not overwrite the server's side-facing roll
        // heading. Change only its body fields and compare actual rendered pixels.
        synced(boss,"DATA_PHASE",GiantCatBoss.ROLL);
        synced(boss,"DATA_PHASE_START",mc.level.getGameTime()-5);
        boss.setYRot(90);boss.yRotO=90;
        Long sideRollHash=null;
        for(int vanillaHeading:new int[]{0,90}) {
            boss.yBodyRot=vanillaHeading;boss.yBodyRotO=vanillaHeading;
            Object frame=capture.invoke(null,mc,"giant-boss-frames/side-roll-body-"+vanillaHeading+".png",(Runnable)()->{
                var pose=new PoseStack();pose.translate(320,350,-11000);pose.scale(42,-42,42);
                pose.mulPose(Axis.XP.rotationDegrees(20));pose.mulPose(Axis.YP.rotationDegrees(-55));
                var buffers=mc.renderBuffers().bufferSource();
                renderer.render(boss,0,0,pose,buffers,LightTexture.FULL_BRIGHT);buffers.endBatch();
            });
            var hash=frame.getClass().getDeclaredMethod("hash");hash.setAccessible(true);
            long current=(long)hash.invoke(frame);
            if(sideRollHash!=null)CatSixWayClientProbe.check(sideRollHash==current,
                    "Side roll ignores vanilla body-yaw controller and follows synchronized entity heading");
            sideRollHash=current;frames++;
        }
        CatSixWayClientProbe.check(hashes.size()>20,"Boss attack states produce distinct actual GPU animation frames");
        System.out.println("PASS: giant boss registered renderer, inherited coat, summon prop and "+frames+" GPU frames across nine phases and interrupted-recovery cases");
    }
}
