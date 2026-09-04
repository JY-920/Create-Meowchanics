package cn.laowu.mod.create;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;

/** The three Create-style parent replacement modes of the advanced breeding box. */
public enum BreedingReplacementMode implements INamedIconOptions {
    LOCKED(AllIcons.I_CONFIG_LOCKED,
            "gui.laowu.breeding_box.replacement.locked"),
    AUTOMATIC(AllIcons.I_REFRESH,
            "gui.laowu.breeding_box.replacement.automatic"),
    REDSTONE(AllIcons.I_ACTIVE,
            "gui.laowu.breeding_box.replacement.redstone");

    private final AllIcons icon;
    private final String translationKey;

    BreedingReplacementMode(AllIcons icon, String translationKey) {
        this.icon = icon;
        this.translationKey = translationKey;
    }

    @Override
    public AllIcons getIcon() {
        return icon;
    }

    @Override
    public String getTranslationKey() {
        return translationKey;
    }
}
