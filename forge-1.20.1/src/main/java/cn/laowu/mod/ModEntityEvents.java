package cn.laowu.mod;

import cn.laowu.mod.entity.ButterCatBoss;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LaoWuMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntityEvents {
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(LaoWuMod.BUTTER_CAT.get(), ButterCatBoss.createAttributes().build());
    }

    private ModEntityEvents() { }
}
