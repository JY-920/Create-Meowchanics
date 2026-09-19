package cn.laowu.mod.compat.kubejs;

import cn.laowu.mod.accessory.CatAccessoryHooks;
import cn.laowu.mod.api.CatAccessoryContext;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.event.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** Discovered ONLY by KubeJS through kubejs.plugins.txt, not loaded by the core mod. */
public final class CatAccessoriesKubePlugin implements KubeJSPlugin {
    public static final EventGroup GROUP = EventGroup.of("CatAccessoryEvents");
    private static final Map<String, EventHandler> HANDLERS = new LinkedHashMap<>();
    static {
        for (String name : CatAccessoryHooks.EVENT_NAMES) {
            EventHandler handler = GROUP.server(name, () -> AccessoryEvent.class);
            if (name.startsWith("before") || name.equals("projectile")) handler.hasResult();
            HANDLERS.put(name, handler);
        }
    }
    @Override public void registerEvents(EventGroupRegistry registry) {
        registry.register(GROUP);
        CatAccessoryHooks.installBridge(name -> HANDLERS.get(name).hasListeners(), (name, context) -> {
            EventResult result = HANDLERS.get(name).post(new AccessoryEvent(context));
            if (result.interruptFalse() && context.isMutable()) context.cancel();
        });
    }
    public static final class AccessoryEvent implements KubeEvent {
        private final CatAccessoryContext context;
        public AccessoryEvent(CatAccessoryContext context) { this.context = context; }
        public CatAccessoryContext getContext() { return context; }
        public net.minecraft.world.entity.animal.Cat getCat() { return context.getCat(); }
    }
}
