package cn.laowu.mod.client;

import cn.laowu.mod.DynamiteCatLastStand;
import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.genetics.CatTraitData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Cat;

public final class HissingCatRenderer extends MobRenderer<Cat, CatModel<Cat>> {
    private final GiantCatRenderer giant;
    private final HissingCatModel resourceModel;
    private final HissingCatModel nativeModel;
    public HissingCatRenderer(EntityRendererProvider.Context context) {
        super(context, CatResourceModelCompatibility.create(context), 0.4F);
        resourceModel = (HissingCatModel) model;
        nativeModel = resourceModel.usesResourcePackModel()
                ? new HissingCatModel(context.bakeLayer(HissingCatModel.LAYER)) : resourceModel;
        giant = new GiantCatRenderer(context);
        addLayer(new HissingCatGeometryLayer(this));
        addLayer(new PancakeCatGeometryLayer(this));
        addLayer(new CatClothesLayer(this));
        addLayer(new CatPilotHarnessLayer(this));
        addLayer(new CatAppearanceLayer(this,
                new CatAppearanceModel(context.bakeLayer(CatAppearanceModel.LAYER))));
        addLayer(new AdaptiveCatCollarLayer(this, context.getModelSet()));
        addLayer(new CatChestLayer(this));
        addLayer(new CatPerformanceLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(Cat cat) {
        return usesNativeGeometry(cat) ? CatGenomeTextureManager.resolveNativeGeometry(cat)
                : resourceModel.usesResourcePackModel() ? CatGenomeTextureManager.resolveResourceGeometry(cat)
                : CatGenomeTextureManager.resolve(cat);
    }

    private boolean usesNativeGeometry(Cat cat) {
        if (!resourceModel.usesResourcePackModel()) return false;
        if (cn.laowu.mod.CatPoseData.isHissing(cat) || cn.laowu.mod.CatPoseData.isPancake(cat)
                || cat.getVehicle() instanceof cn.laowu.mod.entity.BoguShelfSeat
                || cn.laowu.mod.CatClothesData.isEquipped(cat)
                || cn.laowu.mod.CatMedicalHealing.casting(cat)
                || cn.laowu.mod.CatMusicSupport.performing(cat) || CatMusicRecordClient.performing(cat)) return true;
        return CatTraitData.read(cat).map(traits -> traits.has(CatTrait.NEKOMATA)
                || traits.has(CatTrait.HIM) || traits.has(CatTrait.ISAAC)
                || traits.has(CatTrait.PUSS_IN_BOOTS)
                || (traits.has(CatTrait.ROUND_HEAD) && cn.laowu.mod.genetics.CatTraitEffects.isCombatActive(cat))
                || (HissingCatModel.idlePerformanceEligible(cat,cat.walkAnimation.speed())
                    && (traits.has(CatTrait.STREET_DANCE) && CatStreetDanceAnimation.isAvailable()
                        || traits.has(CatTrait.PIPA_PERFORMANCE) && CatPipaAnimation.isAvailable()))).orElse(false);
    }

    @Override
    public void render(Cat cat,float yaw,float partialTick,PoseStack pose,
                       net.minecraft.client.renderer.MultiBufferSource buffers,int light) {
        model = usesNativeGeometry(cat) ? nativeModel : resourceModel;
        try {
        if (cat.getVehicle() instanceof cn.laowu.mod.entity.BoguShelfSeat) {
            shadowRadius=.256F;
            pose.pushPose();
            try {
                super.render(cat,yaw,partialTick,pose,buffers,light);
            } finally { pose.popPose(); }
        } else if (cn.laowu.mod.CatGiantMount.active(cat)) {
            shadowRadius=1.1F*cn.laowu.mod.CatGiantMount.sizeFactor(cat);
            giant.render(cat,yaw,partialTick,pose,buffers,light);
        } else {
            shadowRadius=0.4F;
            super.render(cat,yaw,partialTick,pose,buffers,light);
        }
        } finally { model = resourceModel; }
    }

    @Override
    protected void scale(Cat cat, PoseStack pose, float partialTick) {
        super.scale(cat, pose, partialTick);
        int level = CatTraitData.read(cat)
                .map(profile -> profile.level(CatTrait.BIG_CHONKY_CAT))
                .orElse(0);
        if (cat.getVehicle() instanceof cn.laowu.mod.entity.BoguShelfSeat) {
            float traitScale=level>0?CatTrait.BIG_CHONKY_CAT.bigCatScalePercent(level)/100.0F:1F;
            pose.scale(.64F/traitScale,.64F/traitScale,.64F/traitScale);
        }
        if (level > 0) {
            float scale = CatTrait.BIG_CHONKY_CAT.bigCatScalePercent(level) / 100.0F;
            pose.scale(scale, scale, scale);
        }

        // Vanilla CreeperRenderer performs this entirely in the renderer; it
        // is not a separate model animation. Preserve its pulse and p^4
        // late-fuse swelling curve on the complete cat and every render layer.
        float swelling = DynamiteCatLastStand.swelling(cat, partialTick);
        if (swelling <= 0.0F) return;
        float pulse = 1.0F + Mth.sin(swelling * 100.0F)
                * swelling * 0.01F;
        float eased = Mth.clamp(swelling, 0.0F, 1.0F);
        eased *= eased;
        eased *= eased;
        float horizontal = (1.0F + eased * 0.4F) * pulse;
        float vertical = (1.0F + eased * 0.1F) / pulse;
        pose.scale(horizontal, vertical, horizontal);
    }

    @Override
    protected float getWhiteOverlayProgress(Cat cat, float partialTick) {
        if (!DynamiteCatLastStand.isActive(cat)) {
            return super.getWhiteOverlayProgress(cat, partialTick);
        }
        return DynamiteCatLastStand.whiteOverlayProgress(cat, partialTick);
    }

    @Override
    protected void setupRotations(Cat cat, PoseStack pose, float ageInTicks,
                                  float bodyYaw, float partialTick, float scale) {
        if(cat.getVehicle() instanceof cn.laowu.mod.entity.BoguShelfSeat seat){
            super.setupRotations(cat,pose,ageInTicks,seat.getYRot(),partialTick, scale);
            return;
        }
        boolean carried = cn.laowu.mod.CatPilotFlight.carried(cat) || cn.laowu.mod.CatDivingMount.carried(cat);
        bodyYaw = carried ? CatPilotFlightClient.viewYaw(cat.getVehicle(), partialTick)
                : cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? CatEngineeringAnimation.prepare(cat, bodyYaw, partialTick) : bodyYaw;
        super.setupRotations(cat, pose, ageInTicks, bodyYaw, partialTick, scale);
        if (cn.laowu.mod.compat.create.CreateIntegration.isLoaded() && CatEngineeringAnimation.isPosing(cat)) return;
        if (carried) return;
        if (cn.laowu.mod.CatEngineeringCombat.deployed(cat)) return;
        if (CatTraitData.read(cat)
                .map(profile -> profile.has(CatTrait.OIIAI)).orElse(false)) {
            pose.mulPose(Axis.YP.rotationDegrees(ageInTicks * 20.0F));
        }
    }
}
