package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.entity.GiantCatBoss;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Artist model and coat, driven by the real hostile entity's synchronized combat state. */
public final class GiantCatBossRenderer extends EntityRenderer<GiantCatBoss> {
    private static final ResourceLocation MODEL = LaoWuMod.id("models/entity/giant_cat_mount.bbmodel");
    private static final ResourceLocation FALLBACK = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/cat/tabby.png");
    private final ItemRenderer itemRenderer;
    // These cats never enter a world or renderer. Only the existing authored
    // idle/walk sampler sees them; boss health, names, death, and gear do not.
    private final Map<GiantCatBoss, Cat> samplers = new WeakHashMap<>();
    private final Map<RuntimeBlockbenchModel, List<ContactCube>> contactGeometry = new WeakHashMap<>();

    public GiantCatBossRenderer(EntityRendererProvider.Context context) {
        super(context);
        itemRenderer = context.getItemRenderer();
        shadowRadius = 1.25F;
    }

    @Override public ResourceLocation getTextureLocation(GiantCatBoss boss) {
        return boss.getInheritedGenome().map(genome -> CatGenomeTextureManager.resolve(genome, FALLBACK))
                .orElse(FALLBACK);
    }

    @Override public void render(GiantCatBoss boss, float yaw, float partialTick,
            PoseStack pose, MultiBufferSource buffers, int light) {
        Minecraft mc = Minecraft.getInstance();
        if (!boss.isInvisible() || mc.player != null && !boss.isInvisibleTo(mc.player)
                || mc.shouldEntityAppearGlowing(boss)) {
            Cat sampler = samplers.computeIfAbsent(boss, entity -> EntityType.CAT.create(entity.level()));
            Map<String, RuntimeBlockbenchModel.GroupTransform> base = Map.of();
            if (sampler != null) {
                sampler.tickCount = boss.tickCount;
                sampler.setOnGround(true);
                sampler.setDeltaMovement(boss.getPhase() == GiantCatBoss.IDLE
                        && boss.isAlive() ? boss.getDeltaMovement() : net.minecraft.world.phys.Vec3.ZERO);
                sampler.setYRot(boss.getYRot()); sampler.yRotO = boss.yRotO;
                base = GiantCatAnimation.sample(sampler, partialTick);
            }
            var animation = GiantCatBossAnimation.sample(boss.getPhase(),
                    boss.getPhaseTicks(partialTick), boss.getRecoveryFromPhase(), boss.getRecoveryFromTicks(), base);
            if (boss.deathTime > 0) {
                animation = new HashMap<>(animation);
                var root = animation.getOrDefault("group2", RuntimeBlockbenchModel.GroupTransform.IDENTITY);
                float death = Math.min(1, (float) Math.sqrt(Math.max(0,
                        (boss.deathTime + partialTick - 1) / 20.0F * 1.6F)));
                animation.put("group2", RuntimeBlockbenchModel.GroupTransform.scaled(
                        root.x(),root.y(),root.z(),root.xRot(),root.yRot(),root.zRot()+death*Mth.HALF_PI,
                        root.scaleX(),root.scaleY(),root.scaleZ()));
            }
            float scale = boss.isSummoning() ? 1.0F : GiantCatBoss.MODEL_SCALE;
            shadowRadius = scale * .3F;
            boolean sideRoll = boss.getPhase() == GiantCatBoss.ROLL_WINDUP
                    || boss.getPhase() == GiantCatBoss.ROLL || boss.getPhase() == GiantCatBoss.ROLL_RECOVERY;
            // Server yRot is the side-facing body heading during a roll;
            // vanilla's client body controller may aim yBodyRot along travel.
            float bodyYaw = sideRoll ? Mth.rotLerp(partialTick, boss.yRotO, boss.getYRot())
                    : Mth.rotLerp(partialTick, boss.yBodyRotO, boss.yBodyRot);
            // Combat bones already aim the head and body; no proxy AI, accessories,
            // mount targeting, or second nameplate can leak into this renderer.
            var head = RuntimeBlockbenchModel.HeadMotion.NONE;
            RuntimeBlockbenchModel model = RuntimeBlockbenchModel.get(MODEL);
            float floor = lowestPoint(model, animation, head, scale);
            pose.pushPose();
            pose.translate(0, -floor, 0);
            pose.mulPose(Axis.YP.rotationDegrees(180 - bodyYaw));
            pose.scale(-scale, -scale, scale);
            pose.translate(0, -1.5, 0);
            ResourceLocation texture = getTextureLocation(boss);
            RenderType type = boss.isInvisible()
                    ? (mc.shouldEntityAppearGlowing(boss) ? RenderType.outline(texture)
                    : RenderType.itemEntityTranslucentCull(texture)) : RenderType.entityCutoutNoCull(texture);
            model.render(pose, buffers.getBuffer(type), light,
                    LivingEntityRenderer.getOverlayCoords(boss, 0),
                    RuntimeBlockbenchModel.GroupSelection.ALL_GROUPS, head, animation);
            pose.popPose();
        }
        super.render(boss, yaw, partialTick, pose, buffers, light);
        if (boss.isSummoning()) renderSummonItem(boss, partialTick, pose, buffers, light);
    }

    private void renderSummonItem(GiantCatBoss boss, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light) {
        float progress = boss.getSummonProgress(partialTick);
        float eased = progress * progress * (3 - 2 * progress);
        float age = boss.tickCount + partialTick;
        float pulse = 2.35F + Mth.sin(age * .18F) * .06F;
        pose.pushPose();
        pose.translate(0, Mth.lerp(eased, 5.2D, .72D), 0);
        pose.mulPose(Axis.YP.rotationDegrees(age * 5));
        pose.mulPose(Axis.XP.rotationDegrees(18));
        pose.scale(pulse, pulse, pulse);
        itemRenderer.renderStatic(new ItemStack(LaoWuMod.GIANT_CAT_TREAT.get()),
                ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY,
                pose, buffers, boss.level(), boss.getId());
        pose.popPose();
    }

    /** Exact animated cube support plane, using the runtime's own bone transform chain.
     * This lets the side/back/rump touch the ground without sinking or using a
     * fixed body-radius approximation. Rebuilt when the runtime model reloads.
     */
    private float lowestPoint(RuntimeBlockbenchModel model,
            Map<String, RuntimeBlockbenchModel.GroupTransform> animation,
            RuntimeBlockbenchModel.HeadMotion head, float scale) {
        float minimum = Float.POSITIVE_INFINITY;
        for (ContactCube cube : contactGeometry.computeIfAbsent(model, ignored -> loadContactGeometry())) {
            PoseStack local = new PoseStack();
            local.scale(-scale, -scale, scale);
            local.translate(0, -1.5, 0);
            if (!model.translateToGroup(local, cube.group, head, animation)) continue;
            local.translate((cube.origin.x-cube.groupOrigin.x)/16,
                    (cube.groupOrigin.y-cube.origin.y)/16,(cube.origin.z-cube.groupOrigin.z)/16);
            local.mulPose(new Quaternionf().rotationZYX(-cube.rotation.z*Mth.DEG_TO_RAD,
                    cube.rotation.y*Mth.DEG_TO_RAD,-cube.rotation.x*Mth.DEG_TO_RAD));
            for (int corner=0;corner<8;corner++) {
                Vector3f point = new Vector3f(
                        ((corner&1)==0?cube.from.x:cube.to.x)-cube.origin.x,
                        cube.origin.y-((corner&2)==0?cube.from.y:cube.to.y),
                        ((corner&4)==0?cube.from.z:cube.to.z)-cube.origin.z).div(16);
                minimum = Math.min(minimum, local.last().pose().transformPosition(point).y);
            }
        }
        return Float.isFinite(minimum) ? minimum : 0;
    }

    private static List<ContactCube> loadContactGeometry() {
        try (var reader = Minecraft.getInstance().getResourceManager().getResourceOrThrow(MODEL).openAsReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            Map<String, JsonObject> groups = new HashMap<>(), elements = new HashMap<>();
            for (JsonElement raw : root.getAsJsonArray("groups")) {
                JsonObject group = raw.getAsJsonObject(); groups.put(group.get("uuid").getAsString(),group);
            }
            for (JsonElement raw : root.getAsJsonArray("elements")) {
                JsonObject element = raw.getAsJsonObject(); elements.put(element.get("uuid").getAsString(),element);
            }
            List<ContactCube> cubes = new ArrayList<>();
            for (JsonElement raw : root.getAsJsonArray("outliner")) collect(raw, null, groups, elements, cubes);
            return List.copyOf(cubes);
        } catch (Exception exception) {
            com.mojang.logging.LogUtils.getLogger().warn("Could not read giant boss contact geometry", exception);
            return List.of();
        }
    }

    private static void collect(JsonElement raw, JsonObject parent, Map<String,JsonObject> groups,
            Map<String,JsonObject> elements, List<ContactCube> cubes) {
        if (raw.isJsonObject()) {
            JsonObject node = raw.getAsJsonObject(), group = groups.get(node.get("uuid").getAsString());
            if (group == null) group = node;
            for (JsonElement child : node.getAsJsonArray("children")) collect(child,group,groups,elements,cubes);
        } else if (parent != null) {
            JsonObject element = elements.get(raw.getAsString());
            if (element != null && element.has("from") && element.has("to"))
                cubes.add(new ContactCube(parent.get("name").getAsString(), vector(parent,"origin"),
                        vector(element,"origin"),vector(element,"rotation"),
                        vector(element,"from"),vector(element,"to")));
        }
    }
    private static Vector3f vector(JsonObject object,String key) {
        if (!object.has(key)) return new Vector3f();
        var values = object.getAsJsonArray(key);
        return new Vector3f(values.get(0).getAsFloat(),values.get(1).getAsFloat(),values.get(2).getAsFloat());
    }
    private record ContactCube(String group, Vector3f groupOrigin, Vector3f origin,
                               Vector3f rotation, Vector3f from, Vector3f to) {}
}
