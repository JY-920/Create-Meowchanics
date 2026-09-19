package cn.laowu.mod.client;

import cn.laowu.mod.CatClothesData;
import cn.laowu.mod.CatOutfitType;
import cn.laowu.mod.CatPoseData;
import cn.laowu.mod.DynamiteCatLastStand;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.CatAttributeData;
import cn.laowu.mod.genetics.CatGenomeData;
import cn.laowu.mod.genetics.CatTraitData;
import cn.laowu.mod.network.AudioSessionPacket;
import cn.laowu.mod.network.CatPackageLoadPacket;
import cn.laowu.mod.network.LogisticsSoundPacket;
import cn.laowu.mod.network.SyncCatChestPacket;
import cn.laowu.mod.network.SyncCatClothesPacket;
import cn.laowu.mod.network.SyncCatPosePacket;
import cn.laowu.mod.network.SyncCatGenomePacket;
import cn.laowu.mod.network.SyncCatAttributesPacket;
import cn.laowu.mod.network.SyncCatTraitsPacket;
import cn.laowu.mod.network.SyncCatTraitStatePacket;
import cn.laowu.mod.network.SyncDynamiteCatLastStandPacket;
import cn.laowu.mod.genetics.CatTraitEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Physical-client endpoint for packets whose payload classes are shared with a dedicated server. */
@OnlyIn(Dist.CLIENT)
public final class ClientPacketHandler {
    public static void handleAgentWatch(cn.laowu.mod.network.AgentWatchPacket packet) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != null) CatAgentWatchClient.receive(level, packet.entityId(), packet.uuid(), packet.duration());
    }
    public static void handleAgentMelee(cn.laowu.mod.network.AgentMeleePacket packet) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != null && level.getEntity(packet.entityId()) instanceof net.minecraft.world.entity.animal.Cat cat
                && cat.getUUID().equals(packet.uuid()))
            cn.laowu.mod.CatAgentMeleeMotion.receive(cat, packet.move(), packet.duration(), packet.age());
    }
    public static void handleCockroachState(cn.laowu.mod.network.CockroachStatePacket packet) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != null && level.getEntity(packet.entityId()) instanceof net.minecraft.world.entity.animal.Cat cat
                && cat.getUUID().equals(packet.uuid()))
            cn.laowu.mod.CatCockroachSwarm.receive(cat, packet.allies(), packet.mode(), packet.age());
    }
    public static void handleMusicSupport(cn.laowu.mod.network.MusicSupportPacket packet) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(packet.entityId()) instanceof net.minecraft.world.entity.animal.Cat cat
                && cat.getUUID().equals(packet.uuid()))
            cn.laowu.mod.CatMusicSupport.receive(cat, packet.pose(), packet.age(), packet.bonus(), packet.radius());
    }
    public static void handleMusicRecord(cn.laowu.mod.network.MusicRecordPacket packet) {
        CatMusicRecordClient.receive(packet);
    }

    public static void handleMedical(cn.laowu.mod.network.MedicalHealingPacket p) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(p.entityId()) instanceof net.minecraft.world.entity.LivingEntity patient && patient.getUUID().equals(p.uuid()))
            cn.laowu.mod.CatMedicalHealing.receive(patient, p.casting(), p.aura(), p.age(), p.radius(), p.stationed());
    }

    public static void handle(SyncCatPosePacket packet) {
        if (Minecraft.getInstance().level == null) return;
        Entity entity = Minecraft.getInstance().level.getEntity(packet.entityId());
        if (entity instanceof Cat cat) CatPoseData.setPose(cat, packet.pose());
    }

    public static void handleAudio(AudioSessionPacket packet) {
        if (packet.active()) HissingAudioManager.start(packet.entityId());
        else HissingAudioManager.stop(packet.entityId());
    }

    public static void handleChest(SyncCatChestPacket packet) {
        if (Minecraft.getInstance().level == null) return;
        Entity entity = Minecraft.getInstance().level.getEntity(packet.entityId());
        if (entity instanceof Cat cat) {
            cat.getPersistentData().putBoolean("LaoWuHasChest", packet.hasChest());
        }
    }

    public static void handleClothes(SyncCatClothesPacket packet) {
        if (Minecraft.getInstance().level == null) return;
        Entity entity = Minecraft.getInstance().level.getEntity(packet.entityId());
        if (entity instanceof Cat cat) {
            CatOutfitType outfit = CatOutfitType.byOrdinal(packet.outfit());
            if (outfit == CatOutfitType.NONE) {
                cat.getPersistentData().remove(CatClothesData.EQUIPPED_TAG);
                cat.getPersistentData().remove(CatClothesData.OUTFIT_TAG);
            } else {
                cat.getPersistentData().putBoolean(CatClothesData.EQUIPPED_TAG, true);
                cat.getPersistentData().putString(CatClothesData.OUTFIT_TAG, outfit.id());
            }
        }
    }

    public static void handleGenome(SyncCatGenomePacket packet) {
        if (Minecraft.getInstance().level == null) return;
        Entity entity = Minecraft.getInstance().level.getEntity(packet.entityId());
        if (entity instanceof Cat cat) CatGenomeData.setSerialized(cat, packet.genome());
    }

    public static void handleAttributes(SyncCatAttributesPacket packet) {
        if (Minecraft.getInstance().level == null) return;
        Entity entity = Minecraft.getInstance().level.getEntity(packet.entityId());
        if (entity instanceof Cat cat) {
            CatAttributeData.setSerialized(cat, packet.attributes());
        }
    }

    public static void handleTraits(SyncCatTraitsPacket packet) {
        if (packet.entityId() == -1) {
            cn.laowu.mod.genetics.CatTraitRegistry.receive(packet.traits());
            return;
        }
        if (Minecraft.getInstance().level == null) return;
        Entity entity = Minecraft.getInstance().level.getEntity(packet.entityId());
        if (entity instanceof Cat cat) {
            CatTraitData.setSerialized(cat, packet.traits());
        }
    }

    public static void handleTraitState(SyncCatTraitStatePacket packet) {
        if (Minecraft.getInstance().level == null) return;
        Entity entity = Minecraft.getInstance().level.getEntity(packet.entityId());
        if (entity instanceof Cat cat) {
            CatTraitEffects.setClientState(cat, packet.rageActive(),
                    packet.luBuOutnumbered(), packet.timidOutnumbered(),
                    packet.combatActive());
        }
    }

    public static void handleDynamiteLastStand(
            SyncDynamiteCatLastStandPacket packet) {
        if (Minecraft.getInstance().level == null) return;
        Entity entity = Minecraft.getInstance().level.getEntity(packet.entityId());
        if (entity instanceof Cat cat) {
            DynamiteCatLastStand.setClientState(cat, packet.active(), packet.fuseTicks());
        }
    }

    public static void handleLogisticsSound(LogisticsSoundPacket packet) {
        HissingAudioManager.playLogisticsSound(packet.x(), packet.y(), packet.z(), packet.arrival());
    }

    public static void handlePackageLoad(CatPackageLoadPacket packet) {
        CatPackageLoadAnimationManager.start(packet);
    }

    public static void handleCatTotemActivation() {
        Minecraft.getInstance().gameRenderer.displayItemActivation(
                LaoWuMod.CAT_TOTEM.get().getDefaultInstance());
    }

    private ClientPacketHandler() {}
}
