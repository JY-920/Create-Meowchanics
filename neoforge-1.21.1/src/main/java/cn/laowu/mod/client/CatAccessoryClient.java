package cn.laowu.mod.client;

import cn.laowu.mod.accessory.CatAccessories;
import cn.laowu.mod.accessory.CatAccessoryRegistry;
import cn.laowu.mod.network.CatAccessoriesSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.animal.Cat;

public final class CatAccessoryClient {
    public static void receive(CatAccessoriesSyncPacket packet) {
        if (packet.entityId() < 0) {
            CatAccessoryRegistry.receive(packet.data());
            return;
        }
        var level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(packet.entityId()) instanceof Cat cat)
            cat.getPersistentData().put(CatAccessories.CLIENT_STATE, packet.data().copy());
    }
    private CatAccessoryClient() {}
}
