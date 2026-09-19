package cn.laowu.mod.client;

import cn.laowu.mod.entity.CatFlightCarrier;
import cn.laowu.mod.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class CatPilotFlightClient {
    public static void tick(Minecraft minecraft) {
        var player = minecraft.player;
        if (player == null) return;
        var vehicle = player.getVehicle();
        if (!(vehicle instanceof CatFlightCarrier) && !(vehicle instanceof cn.laowu.mod.entity.CatDivingCarrier)) return;
        // Every tick (20 Hz), rather than adding a two-tick delay before the server can turn.
        boolean active = minecraft.screen == null;
        ModNetwork.pilotFlightInput(active ? player.input.forwardImpulse : 0, active ? player.input.leftImpulse : 0,
                player.getYRot(), player.getXRot(), active && minecraft.options.keyJump.isDown(),
                active && ClientModEvents.PILOT_DESCEND.isDown());
        if (player.tickCount % 10 == 0) {
            if (vehicle instanceof CatFlightCarrier carrier)
                player.displayClientMessage(carrier.gliding() ? Component.translatable("message.laowu.pilot_flight.gliding")
                        : Component.translatable("message.laowu.pilot_flight.remaining", carrier.seconds()), true);
            else if (vehicle instanceof cn.laowu.mod.entity.CatDivingCarrier carrier)
                player.displayClientMessage(!carrier.swimming() ? Component.translatable("message.laowu.diving.land")
                        : carrier.surfacing() ? Component.translatable("message.laowu.diving.surfacing")
                        : Component.translatable("message.laowu.diving.remaining", carrier.seconds()), true);
        }
    }
    /** Cosmetic prediction only: movement/collision and stamina remain server-authoritative. */
    public static float viewYaw(net.minecraft.world.entity.Entity carrier, float partialTick) {
        var local = Minecraft.getInstance().player;
        return local != null && local.getVehicle() == carrier ? local.getYRot()
                : net.minecraft.util.Mth.rotLerp(partialTick, carrier.yRotO, carrier.getYRot());
    }
    private CatPilotFlightClient() {}
}
