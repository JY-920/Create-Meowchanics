package cn.laowu.mod.client;

import cn.laowu.mod.create.*;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.*;
import java.util.List;

/** Native Create hover visuals without adopting its ghost-item insertion semantics. */
@net.neoforged.fml.common.EventBusSubscriber(modid="laowu", value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class CatAutoLaserFilterHighlight {
    private static Object previous;
    @net.neoforged.bus.api.SubscribeEvent
    public static void onTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        tick();
    }
    public static void tick() {
        var mc = Minecraft.getInstance();
        var outliner = Outliner.getInstance();
        if (previous != null) { outliner.remove(previous); previous = null; }
        if (mc.level == null || mc.player == null || mc.screen != null || mc.player.isShiftKeyDown()
                || !(mc.hitResult instanceof BlockHitResult hit)) return;
        var state = mc.level.getBlockState(hit.getBlockPos());
        var slot=state.getBlock() instanceof CatAutoLaserBlock&&CatAutoLaserFilterSlot.hits(state,hit)?
            CatAutoLaserFilterSlot.INSTANCE:null;
        if(slot==null)return;
        var label = Component.translatable("item.laowu.creature_filter");
        var box = new ValueBox(label,new AABB(-.25,-.25,-.025,.25,.25,.025),hit.getBlockPos())
                .transform(slot).passive(false);
        previous = Pair.of("laowu_auto_laser_filter",hit.getBlockPos());
        outliner.showOutline(previous,box).lineWidth(1f/64)
                .withFaceTexture(AllSpecialTextures.THIN_CHECKERED).highlightFace(hit.getDirection());
        CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(List.of(label,
                Component.translatable("gui.laowu.creature_filter.slot_hint")));
    }
}
