package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.create.CatMachineBlocks;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogRenderer;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogCTBehaviour;
import com.simibubi.create.content.processing.basin.BasinRenderer;
import com.simibubi.create.foundation.block.connected.*;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;

public final class CatMachinesClient {
    private static final CTSpriteShiftEntry CASING = CTSpriteShifter.getCT(AllCTTypes.OMNIDIRECTIONAL,
            LaoWuMod.id("block/cat_casing"), LaoWuMod.id("block/cat_casing_connected"));
    private static final CTSpriteShiftEntry COG_SIDE = CTSpriteShifter.getCT(AllCTTypes.VERTICAL,
            LaoWuMod.id("block/cat_encased_cogwheel_side"), LaoWuMod.id("block/cat_encased_cogwheel_side_connected"));
    private static final CTSpriteShiftEntry COG_OTHERSIDE = CTSpriteShifter.getCT(AllCTTypes.HORIZONTAL,
            LaoWuMod.id("block/cat_encased_cogwheel_side"), LaoWuMod.id("block/cat_encased_cogwheel_side_connected"));

    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer.builder(CatMachineBlocks.SHAFT_BE.get())
                    .factory(com.simibubi.create.content.kinetics.base.ShaftVisual::new).apply();
            dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer.builder(CatMachineBlocks.COG_BE.get())
                    .factory(com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogVisual::small).apply();
            dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer.builder(CatMachineBlocks.LARGE_COG_BE.get())
                    .factory(com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogVisual::large).apply();
            var connectivity=CreateClient.CASING_CONNECTIVITY;
            connectivity.makeCasing(CatMachineBlocks.CAT_CASING.get(),CASING);
            connectivity.make(CatMachineBlocks.CAT_ENCASED_SHAFT.get(),CASING,
                    (state,face)->state.getValue(BlockStateProperties.AXIS)!=face.getAxis());
            // Only axis faces use full casing tiles; side faces contain cogwheel slots.
            connectivity.make(CatMachineBlocks.CAT_ENCASED_COGWHEEL.get(),CASING,
                    CatMachinesClient::closedCogFace);
            connectivity.make(CatMachineBlocks.CAT_ENCASED_LARGE_COGWHEEL.get(),CASING,
                    CatMachinesClient::closedCogFace);
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(CatMachineBlocks.HAJI_BASIN.get(),
                    net.minecraft.client.renderer.RenderType.cutout());
        });
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(cn.laowu.mod.create.CreatureTransmitterRegistration.ENTITY.get(),CreatureTransmitterRenderer::new);
        event.registerBlockEntityRenderer(CatMachineBlocks.AUTO_LASER_BE.get(),CatAutoLaserRenderer::new);
        event.registerBlockEntityRenderer(CatMachineBlocks.SHAFT_BE.get(),ShaftRenderer::new);
        event.registerBlockEntityRenderer(CatMachineBlocks.COG_BE.get(),EncasedCogRenderer::small);
        event.registerBlockEntityRenderer(CatMachineBlocks.LARGE_COG_BE.get(),EncasedCogRenderer::large);
        event.registerBlockEntityRenderer(CatMachineBlocks.BASIN_BE.get(),CatBasinRenderer::new);
        event.registerBlockEntityRenderer(CatMachineBlocks.PRESS_BE.get(),CatProcessorRenderer::new);
        event.registerBlockEntityRenderer(CatMachineBlocks.MIXER_BE.get(),CatProcessorRenderer::new);
    }
    @SubscribeEvent public static void models(ModelEvent.ModifyBakingResult event) {
        wrap(event,CatMachineBlocks.CAT_CASING.get(),new EncasedCTBehaviour(CASING));
        wrap(event,CatMachineBlocks.CAT_ENCASED_SHAFT.get(),new EncasedCTBehaviour(CASING));
        wrap(event,CatMachineBlocks.CAT_ENCASED_COGWHEEL.get(),new EncasedCogCTBehaviour(CASING,
                net.createmod.catnip.data.Couple.create(
                        COG_SIDE, COG_OTHERSIDE)));
        wrap(event,CatMachineBlocks.CAT_ENCASED_LARGE_COGWHEEL.get(),new EncasedCogCTBehaviour(CASING));
    }
    private static void wrap(ModelEvent.ModifyBakingResult event,Block block,ConnectedTextureBehaviour behaviour) {
        for(var state:block.getStateDefinition().getPossibleStates()) {
            var key=BlockModelShaper.stateToModelLocation(state);
            var model=event.getModels().get(key);
            if(model!=null)event.getModels().put(key,new CTModel(model,behaviour));
        }
    }
    private static boolean closedCogFace(net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.Direction face) {
        return state.getValue(BlockStateProperties.AXIS)==face.getAxis() && !state.getValue(
                face.getAxisDirection()==net.minecraft.core.Direction.AxisDirection.POSITIVE
                        ?com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock.TOP_SHAFT
                        :com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock.BOTTOM_SHAFT);
    }
    private CatMachinesClient() {}
}
