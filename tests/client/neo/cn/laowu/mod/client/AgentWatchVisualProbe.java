package cn.laowu.mod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.*;
import net.minecraft.world.scores.Scoreboard;
import java.util.*;

/** Real transformed Minecraft/Entity methods in an in-memory client level; no save or connection. */
public final class AgentWatchVisualProbe {
    public static void verify(Minecraft mc) {
        var world=new ProbeLevel();
        var target=new ProbeHusk(world);
        target.setId(91801);
        var team=world.scoreboard.addPlayerTeam("original_blue");
        team.setColor(net.minecraft.ChatFormatting.BLUE);
        world.scoreboard.addPlayerToTeam(target.getScoreboardName(),team);
        check(!mc.shouldEntityAppearGlowing(target),"Unmarked target must not glow");
        check(target.getTeamColor()==net.minecraft.ChatFormatting.BLUE.getColor(),"Original blue team");
        var uuid=target.getUUID();
        CatAgentWatchClient.receive(world,target.getId(),uuid,40);
        check(CatAgentWatchClient.visible(target)&&mc.shouldEntityAppearGlowing(target),"Watch glow mixin");
        check(target.getTeamColor()==CatAgentWatchClient.RED,"Watch colour mixin");
        check(!target.isCurrentlyGlowing()&&!target.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING),
                "Overlay must not mutate vanilla flags or effects");
        check(target.getTeam()==team&&team.getColor()==net.minecraft.ChatFormatting.BLUE,"Team untouched");
        target.setInvisible(true);
        check(mc.shouldEntityAppearGlowing(target),"Invisible hostiles still outlined");
        target.setId(91802);
        check(!CatAgentWatchClient.visible(target),"Reused UUID but wrong runtime ID");
        target.setId(91801);
        target.setUUID(UUID.randomUUID());
        check(!CatAgentWatchClient.visible(target),"Reused runtime ID but wrong UUID");
        target.setUUID(uuid);
        var another=new Husk(EntityType.HUSK,new ProbeLevel());
        another.setId(target.getId());another.setUUID(uuid);
        check(!CatAgentWatchClient.visible(another),"World isolation");
        world.clock=39;
        check(CatAgentWatchClient.visible(target),"Valid until exclusive expiry");
        world.clock=40;
        check(!mc.shouldEntityAppearGlowing(target),"Expiry restores vanilla visibility");
        check(target.getTeamColor()==net.minecraft.ChatFormatting.BLUE.getColor(),"Expiry restores blue team");
        CatAgentWatchClient.receive(world,target.getId(),uuid,40);
        CatAgentWatchClient.receive(world,target.getId(),uuid,0);
        check(!mc.shouldEntityAppearGlowing(target),"Explicit clear");
        CatAgentWatchClient.receive(world,target.getId(),uuid,41);
        CatAgentWatchClient.receive(world,target.getId(),uuid,-1);
        check(!CatAgentWatchClient.visible(target),"Invalid durations ignored");
        target.receiveVanillaGlow();
        CatAgentWatchClient.receive(world,target.getId(),uuid,40);
        CatAgentWatchClient.receive(world,target.getId(),uuid,0);
        check(mc.shouldEntityAppearGlowing(target),"Keep pre-existing vanilla glow");
        check(target.getTeamColor()==net.minecraft.ChatFormatting.BLUE.getColor(),"Keep pre-existing colour");
        renderOutline(mc);
        System.out.println("PASS: agent watch real client glow/colour mixins, ID+UUID+world safety, expiry, clear, invisible target, vanilla team/effect preservation");
    }
    private static void renderOutline(Minecraft mc) {
        com.mojang.blaze3d.pipeline.TextureTarget output=null;
        com.mojang.blaze3d.systems.RenderSystem.backupProjectionMatrix();
        var view=com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        view.pushMatrix();view.identity();com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        try(var state=new CatPerformanceOutline.State();var framebuffer=new PerformanceSceneSnapshot.Target()) {
            output=new com.mojang.blaze3d.pipeline.TextureTarget(448,448,true,Minecraft.ON_OSX);
            try(var chain=new net.minecraft.client.renderer.PostChain(mc.getTextureManager(),mc.getResourceManager(),output,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft","shaders/post/entity_outline.json"))) {
                chain.resize(448,448);
                var mask=chain.getTempTarget("final");
                com.mojang.blaze3d.systems.RenderSystem.colorMask(true,true,true,true);
                com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
                com.mojang.blaze3d.systems.RenderSystem.disableScissor();
                mask.setClearColor(0,0,0,0);mask.clear(Minecraft.ON_OSX);mask.bindWrite(true);
                com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(
                        new org.joml.Matrix4f().setOrtho(-2,2,-2,2,-10,10),com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);
                com.mojang.blaze3d.systems.RenderSystem.disableBlend();
                com.mojang.blaze3d.systems.RenderSystem.disableCull();
                com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);
                com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getRendertypeOutlineShader);
                com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0,
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft","textures/entity/zombie/husk.png"));
                var model=new net.minecraft.client.model.HumanoidModel<Husk>(
                        net.minecraft.client.model.geom.builders.LayerDefinition.create(
                        net.minecraft.client.model.HumanoidModel.createMesh(net.minecraft.client.model.geom.builders.CubeDeformation.NONE,0),64,64).bakeRoot());
                var pose=new com.mojang.blaze3d.vertex.PoseStack();
                pose.translate(0,1,0);pose.scale(1,-1,-1);
                pose.mulPose(new org.joml.Quaternionf().rotationY(.4F));
                var buffer=com.mojang.blaze3d.vertex.Tesselator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,net.minecraft.client.renderer.RenderType.outline(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft","textures/entity/zombie/husk.png")).format());
                model.renderToBuffer(pose,new RedVertices(buffer),net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,0xffffffff);
                com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(buffer.buildOrThrow());
                try(var pixels=new com.mojang.blaze3d.platform.NativeImage(448,448,false)){
                    com.mojang.blaze3d.systems.RenderSystem.bindTexture(mask.getColorTextureId());
                    pixels.downloadTexture(0,false);
                    pixels.flipY();pixels.writeToFile(java.nio.file.Path.of("agent-watch-mask.png"));
                }
                chain.process(0);
                int red=0,maxRed=0;
                try(var pixels=new com.mojang.blaze3d.platform.NativeImage(448,448,false)){
                    com.mojang.blaze3d.systems.RenderSystem.bindTexture(mask.getColorTextureId());
                    pixels.downloadTexture(0,false);
                    for(int x=0;x<448;x++)for(int y=0;y<448;y++){
                        int pixel=pixels.getPixelRGBA(x,y),r=pixel&255,g=(pixel>>>8)&255,b=(pixel>>>16)&255,a=(pixel>>>24)&255;
                        maxRed=Math.max(maxRed,r);
                        // 1.21's linear-filtered box blur attenuates edge RGB (the mask remains full red).
                        // Verify hue, visible contrast and edge coverage rather than 1.20's peak brightness.
                        if(a>10&&r>32&&r>g*4&&r>b*4)red++;
                    }
                    pixels.flipY();pixels.writeToFile(java.nio.file.Path.of("agent-watch-red.png"));
                    check(red>300&&red<12000&&maxRed>=64,"Vanilla outline GPU output must contain a hollow red silhouette: "+red+", peak="+maxRed);
                }
                System.out.println("PASS: agent watch vanilla outline shader/post-chain GPU output, "+red+" red edge pixels");
            }
        }catch(Exception error){throw new IllegalStateException(error);}
        finally{
            if(output!=null)output.destroyBuffers();
            view.popMatrix();com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
            com.mojang.blaze3d.systems.RenderSystem.restoreProjectionMatrix();
        }
    }
    private static final class RedVertices implements com.mojang.blaze3d.vertex.VertexConsumer {
        private final com.mojang.blaze3d.vertex.VertexConsumer delegate;

        RedVertices(com.mojang.blaze3d.vertex.VertexConsumer delegate){this.delegate=delegate;}
        public com.mojang.blaze3d.vertex.VertexConsumer addVertex(float x,float y,float z){delegate.addVertex(x,y,z).setColor(255,48,48,255);return this;}
        public com.mojang.blaze3d.vertex.VertexConsumer setColor(int r,int g,int b,int a){return this;}
        public com.mojang.blaze3d.vertex.VertexConsumer setUv(float u,float v){delegate.setUv(u,v);return this;}
        public com.mojang.blaze3d.vertex.VertexConsumer setUv1(int u,int v){return this;}
        public com.mojang.blaze3d.vertex.VertexConsumer setUv2(int u,int v){return this;}
        public com.mojang.blaze3d.vertex.VertexConsumer setNormal(float x,float y,float z){return this;}
    }
    private static void check(boolean condition,String message) {
        if(!condition)throw new AssertionError(message);
    }
    private static final class ProbeHusk extends Husk {
        ProbeHusk(Level level){super(EntityType.HUSK,level);}
        // Client glow arrives in synchronized entity flags, not the server-only glowingTag field.
        void receiveVanillaGlow(){setSharedFlag(6,true);}
    }
    static final class ProbeLevel extends Level {
        long clock;
        final Scoreboard scoreboard=new Scoreboard();
        private static final RegistryAccess REGISTRIES=registries();
        private static RegistryAccess registries(){
            var source=net.minecraft.data.registries.VanillaRegistries.createLookup();
            return new RegistryAccess.ImmutableRegistryAccess(List.of(copy(source,net.minecraft.core.registries.Registries.DAMAGE_TYPE),
                    copy(source,net.minecraft.core.registries.Registries.DIMENSION_TYPE))).freeze();
        }
        private static <T> Registry<T> copy(HolderLookup.Provider source,net.minecraft.resources.ResourceKey<Registry<T>> key){
            var registry=new MappedRegistry<T>(key,com.mojang.serialization.Lifecycle.stable());
            source.lookupOrThrow(key).listElements().forEach(holder->
                    registry.register(holder.key(),holder.value(),RegistrationInfo.BUILT_IN));
            return registry.freeze();
        }
        ProbeLevel() {
            super(new net.minecraft.client.multiplayer.ClientLevel.ClientLevelData(net.minecraft.world.Difficulty.NORMAL,false,false),
                    Level.OVERWORLD,REGISTRIES,REGISTRIES.registryOrThrow(net.minecraft.core.registries.Registries.DIMENSION_TYPE)
                            .getHolderOrThrow(net.minecraft.world.level.dimension.BuiltinDimensionTypes.OVERWORLD),
                    ()->net.minecraft.util.profiling.InactiveProfiler.INSTANCE,true,false,0,1);
        }
        public net.minecraft.world.TickRateManager tickRateManager(){return new net.minecraft.world.TickRateManager();}
        public net.minecraft.world.item.alchemy.PotionBrewing potionBrewing(){throw new AssertionError("No brewing in probe");}
        public void setDayTimeFraction(float value){}
        public float getDayTimeFraction(){return 0;}
        public float getDayTimePerTick(){return 0;}
        public void setDayTimePerTick(float value){}
        public long getGameTime(){return clock;}
        public Scoreboard getScoreboard(){return scoreboard;}
        public BlockState getBlockState(BlockPos pos){return Blocks.AIR.defaultBlockState();}
        public FluidState getFluidState(BlockPos pos){return Fluids.EMPTY.defaultFluidState();}
        public List<? extends Player> players(){return List.of();}
        public void sendBlockUpdated(BlockPos p,BlockState a,BlockState b,int flags){}
        public void playSeededSound(Player p,double x,double y,double z,Holder<net.minecraft.sounds.SoundEvent> e,net.minecraft.sounds.SoundSource s,float v,float pitch,long seed){}
        public void playSeededSound(Player p,Entity entity,Holder<net.minecraft.sounds.SoundEvent> e,net.minecraft.sounds.SoundSource s,float v,float pitch,long seed){}
        public String gatherChunkSourceStats(){return "agent watch client probe";}
        public Entity getEntity(int id){return null;}
        public net.minecraft.world.level.saveddata.maps.MapItemSavedData getMapData(net.minecraft.world.level.saveddata.maps.MapId id){return null;}
        public void setMapData(net.minecraft.world.level.saveddata.maps.MapId id,net.minecraft.world.level.saveddata.maps.MapItemSavedData data){}
        public net.minecraft.world.level.saveddata.maps.MapId getFreeMapId(){return new net.minecraft.world.level.saveddata.maps.MapId(0);}
        public void destroyBlockProgress(int id,BlockPos pos,int stage){}
        public net.minecraft.world.item.crafting.RecipeManager getRecipeManager(){throw new AssertionError("No recipes in probe");}
        protected net.minecraft.world.level.entity.LevelEntityGetter<Entity> getEntities(){throw new AssertionError("No loaded chunks in probe");}
        public net.minecraft.world.level.chunk.ChunkSource getChunkSource(){throw new AssertionError("No chunks in probe");}
        public long nextSubTickCount(){return 0;}
        public net.minecraft.world.ticks.LevelTickAccess<Block> getBlockTicks(){return net.minecraft.world.ticks.BlackholeTickAccess.emptyLevelList();}
        public net.minecraft.world.ticks.LevelTickAccess<Fluid> getFluidTicks(){return net.minecraft.world.ticks.BlackholeTickAccess.emptyLevelList();}
        public void levelEvent(Player player,int type,BlockPos pos,int data){}
        public void gameEvent(Holder<net.minecraft.world.level.gameevent.GameEvent> event,net.minecraft.world.phys.Vec3 position,net.minecraft.world.level.gameevent.GameEvent.Context context){}
        public Holder<net.minecraft.world.level.biome.Biome> getUncachedNoiseBiome(int x,int y,int z){throw new AssertionError("No biomes in probe");}
        public float getShade(Direction direction,boolean shade){return 1;}
        public int getBlockTint(BlockPos pos,ColorResolver resolver){return 0xffffff;}
        public net.minecraft.world.flag.FeatureFlagSet enabledFeatures(){return net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS;}
    }
}
