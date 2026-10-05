package cn.laowu.mod;

import cn.laowu.mod.entity.ButterCatBoss;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = LaoWuMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModEntityEvents {
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(LaoWuMod.BUTTER_CAT.get(), ButterCatBoss.createAttributes().build());
        event.put(LaoWuMod.GIANT_CAT_BOSS.get(), cn.laowu.mod.entity.GiantCatBoss.createAttributes().build());
    }

    private ModEntityEvents() { }
}
