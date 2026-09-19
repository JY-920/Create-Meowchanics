package cn.laowu.mod.compat.kubejs;

import cn.laowu.mod.genetics.CatTraitHooks;
import cn.laowu.mod.api.CatTraitContext;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.event.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** Discovered ONLY by KubeJS through kubejs.plugins.txt, not loaded by the core mod. */
public final class CatTraitsKubePlugin extends KubeJSPlugin {
    public static final EventGroup GROUP = EventGroup.of("CatTraitEvents");
    private static final Map<String, EventHandler> HANDLERS = new LinkedHashMap<>();
    static {
        for (String name : CatTraitHooks.EVENT_NAMES) {
            EventHandler handler = GROUP.server(name, () -> TraitEvent.class);
            if (name.startsWith("before") || name.equals("projectile")) handler.hasResult();
            HANDLERS.put(name, handler);
        }
    }
    @Override public void registerEvents() {
        GROUP.register();
        CatTraitHooks.installBridge(name -> HANDLERS.get(name).hasListeners(), (name, context) -> {
            EventResult result = HANDLERS.get(name).post(new TraitEvent(context));
            if (result.interruptFalse() && context.isMutable()) context.cancel();
        });
    }
    public static final class TraitEvent extends EventJS {
        private final CatTraitContext context;
        public TraitEvent(CatTraitContext context) { this.context = context; }
        public CatTraitContext getContext() { return context; }
        public net.minecraft.world.entity.animal.Cat getCat() { return context.getCat(); }
    }
}
