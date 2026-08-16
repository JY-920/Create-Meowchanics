package cn.laowu.mod.client;

import cn.laowu.mod.CatOutfitType;
import cn.laowu.mod.LaoWuMod;
import net.minecraft.resources.ResourceLocation;

/** Client resource mapping for every cat outfit. */
final class CatOutfitModels {
    static Definition get(CatOutfitType outfit) {
        return switch (outfit) {
            case TERMINATOR -> definition("cat_clothes", true, null);
            case FISHING -> definition("fishing_suit", false, null);
            case FLIGHT -> definition("flight_suit", false, null);
            case FIRE -> definition("fire_suit", false, null);
            case HONEY -> definition("honey_suit", false, "honey_suit_translucent");
            case TRANSPORT -> definition("transport_suit", false, null);
            case NONE -> null;
        };
    }

    private static Definition definition(String name, boolean genericRootFollowsHead,
                                         String translucentTextureName) {
        return new Definition(
                LaoWuMod.id("models/entity/" + name + ".bbmodel"),
                LaoWuMod.id("textures/entity/" + name + ".png"),
                translucentTextureName == null ? null
                        : LaoWuMod.id("textures/entity/" + translucentTextureName + ".png"),
                genericRootFollowsHead);
    }

    record Definition(ResourceLocation model, ResourceLocation texture,
                      ResourceLocation translucentTexture,
                      boolean genericRootFollowsHead) {
    }

    private CatOutfitModels() {
    }
}
