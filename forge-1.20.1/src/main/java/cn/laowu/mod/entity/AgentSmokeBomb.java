package cn.laowu.mod.entity;
import cn.laowu.mod.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
/** A visible, non-pickup smoke grenade; it never damages blocks or creatures. */
public final class AgentSmokeBomb extends ThrowableItemProjectile {
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
    }
    private int fuse;
    private boolean healing;
    public AgentSmokeBomb(EntityType<? extends AgentSmokeBomb> type, Level level) { super(type, level); }
    public AgentSmokeBomb(Level level, Cat owner) { super(LaoWuMod.AGENT_SMOKE_BOMB.get(), owner, level); healing=cn.laowu.mod.accessory.CatAccessories.value(owner,"healing_smoke")>0; }
    @Override protected Item getDefaultItem() { return Items.FIRE_CHARGE; }
    @Override public void tick() { super.tick(); if (!level().isClientSide && ++fuse >= 8) burst(); }
    @Override protected void onHit(HitResult hit) { if (!level().isClientSide) burst(); }
    private void burst() {
        if (isRemoved()) return;
        if (getOwner() instanceof Cat cat) CatAgentSmoke.burst(cat, position(), healing);
        discard();
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putInt("Fuse", fuse); tag.putBoolean("HealingSmoke",healing); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); fuse = Math.max(0, tag.getInt("Fuse")); healing=tag.getBoolean("HealingSmoke"); }
}
