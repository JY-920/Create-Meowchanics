package cn.laowu.mod;

import cn.laowu.mod.genetics.CatGenome;
import cn.laowu.mod.genetics.CatGenomeData;
import cn.laowu.mod.genetics.CatMaterialRegistry;
import cn.laowu.mod.genetics.CatRegion;
import cn.laowu.mod.network.ModNetwork;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

/** Server-authoritative whole/region material editor for the debug wand. */
public final class CatMaterialEditorMenu extends AbstractContainerMenu {
    public static final int COMMIT_BUTTON_ID = 10_000;
    private final UUID targetId;
    private final ResourceLocation initiallySelected;
    private final CatGenome initialGenome;
    private final List<ResourceLocation> materials;
    private CatGenome pendingGenome;
    private final Cat viewedCat;
    private boolean viewLockReleased;

    public CatMaterialEditorMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readUUID(), buffer.readResourceLocation(),
                readGenome(buffer), null);
    }

    public CatMaterialEditorMenu(int id, Inventory inventory, Cat target,
                                 ResourceLocation selected) {
        this(id, inventory, target.getUUID(), selected,
                CatGenomeData.ensure(target), target);
    }

    private CatMaterialEditorMenu(int id, Inventory inventory, UUID targetId,
                                  ResourceLocation selected, CatGenome initialGenome,
                                  Cat target) {
        super(LaoWuMod.CAT_MATERIAL_EDITOR_MENU.get(), id);
        this.targetId = targetId;
        this.initiallySelected = selected;
        this.initialGenome = initialGenome;
        this.materials = CatMaterialRegistry.selectableMaterials(selected, initialGenome);
        this.pendingGenome = initialGenome;
        this.viewedCat = target;
        if (viewedCat != null) CatProfileData.beginViewing(viewedCat);
    }

    public int materialCount() {
        return materials.size();
    }

    public int initialMaterialIndex() {
        int index = materials.indexOf(initiallySelected);
        return index < 0 ? 0 : index;
    }

    public int materialIndex(ResourceLocation material) {
        int index = materials.indexOf(material);
        return index < 0 ? 0 : index;
    }

    public Component materialName(int index) {
        return CatMaterialRegistry.displayName(materials.get(index));
    }

    public ResourceLocation material(int index) {
        return materials.get(index);
    }

    public CatGenome initialGenome() {
        return initialGenome;
    }

    public static Component regionName(int index) {
        return index == 0
                ? Component.translatable("region.laowu.whole_cat")
                : Component.translatable("region.laowu."
                + CatRegion.values()[index - 1].serializedName());
    }

    public static int regionCount() {
        return CatRegion.values().length + 1;
    }

    public int encodeSelection(int regionIndex, int materialIndex) {
        return regionIndex * materials.size() + materialIndex;
    }

    private static CatGenome readGenome(FriendlyByteBuf buffer) {
        CompoundTag serialized = buffer.readNbt();
        return CatGenome.load(serialized).orElseGet(() ->
                CatGenome.uniform(CatVariant.RED.location()));
    }

    @Override
    public boolean clickMenuButton(Player player, int encodedId) {
        if (player.level().isClientSide) return true;
        Cat target = resolveTarget(player);
        if (target == null || materials.isEmpty()) return false;

        if (encodedId == COMMIT_BUTTON_ID) {
            CatGenomeData.set(target, pendingGenome);
            BuiltInRegistries.CAT_VARIANT.getHolder(
                    pendingGenome.material(CatRegion.BODY_FRONT)).ifPresent(target::setVariant);
            ModNetwork.syncCatGenomeToTracking(target);
            ((ServerLevel) target.level()).sendParticles(ParticleTypes.WAX_ON,
                    target.getX(), target.getY() + 0.5D, target.getZ(),
                    12, 0.25D, 0.25D, 0.25D, 0.02D);
            return true;
        }

        int regionIndex = encodedId / materials.size();
        int materialIndex = encodedId % materials.size();
        if (regionIndex < 0 || regionIndex >= regionCount()
                || materialIndex < 0 || materialIndex >= materials.size()) return false;

        ResourceLocation material = materials.get(materialIndex);
        if (regionIndex == 0) {
            pendingGenome = pendingGenome.withUniformMaterial(material);
        } else {
            pendingGenome = pendingGenome.withMaterial(
                    CatRegion.values()[regionIndex - 1], material);
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.level().isClientSide) return true;
        Cat target = resolveTarget(player);
        return target != null && target.isAlive() && player.distanceToSqr(target) <= 64.0D;
    }

    private Cat resolveTarget(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return null;
        Entity entity = level.getEntity(targetId);
        return entity instanceof Cat cat ? cat : null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!viewLockReleased && viewedCat != null) {
            viewLockReleased = true;
            CatProfileData.endViewing(viewedCat);
        }
    }
}
