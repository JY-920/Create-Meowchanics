package cn.laowu.mod.genetics;

import net.minecraft.ChatFormatting;

/** Quality, generation weight and pixel-card style are independent of level. */
public enum CatTraitRarity {
    DEFECT("defect", 3, 30, ChatFormatting.RED, 0xFFF8F8EC),
    COMMON("common", 0, 35, ChatFormatting.GRAY, 0xFF4A2D31),
    GOOD("good", 1, 30, ChatFormatting.AQUA, 0xFFFFFFFF),
    EXCELLENT("excellent", 2, 5, ChatFormatting.GOLD, 0xFF603D39);

    private final String serializedName;
    private final int frameIndex;
    private final int generationWeight;
    private final ChatFormatting textFormatting;
    private final int cardTextColour;

    CatTraitRarity(String serializedName, int frameIndex, int generationWeight,
                   ChatFormatting textFormatting, int cardTextColour) {
        this.serializedName = serializedName;
        this.frameIndex = frameIndex;
        this.generationWeight = generationWeight;
        this.textFormatting = textFormatting;
        this.cardTextColour = cardTextColour;
    }

    public String serializedName() {
        return serializedName;
    }

    public int frameIndex() {
        return frameIndex;
    }

    public int generationWeight() {
        return generationWeight;
    }

    public ChatFormatting textFormatting() {
        return textFormatting;
    }

    public int cardTextColour() {
        return cardTextColour;
    }
}
