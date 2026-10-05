package cn.laowu.mod.client;

import cn.laowu.mod.entity.CatGiantCarrier;
import cn.laowu.mod.network.ModNetwork;
import net.minecraft.client.Minecraft;

/** Sends controls at 20 Hz; collision and movement are resolved by the server. */
public final class CatGiantMountClient {
    private static CatGiantCarrier previousCarrier;
    public static void tick(Minecraft minecraft) {
        var player=minecraft.player;
        if (player==null || !(player.getVehicle() instanceof CatGiantCarrier carrier)) {
            previousCarrier=null;
            return;
        }
        // KeyMapping retains a complete press/release between client ticks.
        // Discard pre-mount clicks, and never consume another vehicle's jump input.
        boolean pressed=false;
        while(minecraft.options.keyJump.consumeClick())pressed=true;
        if(previousCarrier!=carrier)pressed=false;
        previousCarrier=carrier;
        boolean active=minecraft.screen==null;
        ModNetwork.giantCatInput(active?player.input.forwardImpulse:0,active?player.input.leftImpulse:0,
                player.getYRot(),active&&(pressed||minecraft.options.keyJump.isDown()),active&&minecraft.options.keySprint.isDown());
    }
    private CatGiantMountClient() {}
}
