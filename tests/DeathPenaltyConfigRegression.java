package cn.laowu.mod;

import com.electronwill.nightconfig.core.CommentedConfig;
import net.minecraft.nbt.CompoundTag;

/** Real loader config specs, server write guards and remote mirrors for death rules. */
public final class DeathPenaltyConfigRegression {
    private static final String ENABLED = ServerConfig.DEATH_PENALTY_ENABLED_KEY;
    private static final String LOSS = ServerConfig.DEATH_ATTRIBUTE_LOSS_KEY;
    private static int checks;

    public static void run() throws Exception {
        ServerConfig.resetWorldState();
        CommentedConfig global = CommentedConfig.inMemory();
        GlobalWorldConfigRegression.load(GlobalConfig.SPEC, global);
        GlobalWorldConfigRegression.load(ServerConfig.SPEC, CommentedConfig.inMemory());
        final String outcome = "cat_death_outcome";
        check(ServerConfig.snapshot().contains(outcome, 3) && ServerConfig.snapshot().getInt(outcome) == 1,
                "Old worlds default to a pancake item, not permanent death");
        for (int mode : new int[]{0, 1, 2}) {
            CompoundTag choice = ServerConfig.snapshot(); choice.putInt(outcome, mode);
            check(ServerConfig.valid(choice), "Accept death outcome " + mode);
            ServerConfig.applyUnlockedValues(choice);
            check(ServerConfig.snapshot().getInt(outcome) == mode, "Persist chosen death outcome");
        }
        CompoundTag choice = ServerConfig.snapshot(); choice.putInt(outcome, 1);
        ServerConfig.applyUnlockedValues(choice);
        override(global, outcome, 0);
        check(locked(outcome) && ServerConfig.snapshot().getInt(outcome) == 0, "Global outcome override locks and takes effect");
        choice.putInt(outcome, 2); choice.put(ServerConfig.LOCKS_TAG, new CompoundTag());
        ServerConfig.applyUnlockedValues(choice);
        override(global, outcome, -1);
        check(!locked(outcome) && ServerConfig.snapshot().getInt(outcome) == 1, "Forged locks cannot replace the stored mode");
        for (int invalid : new int[]{-1, 3, Integer.MAX_VALUE}) {
            choice = ServerConfig.snapshot(); choice.putInt(outcome, invalid);
            check(!ServerConfig.valid(choice), "Reject invalid death outcome");
        }
        choice = ServerConfig.snapshot(); choice.remove(outcome);
        check(!ServerConfig.valid(choice), "Reject missing death outcome");
        choice = ServerConfig.snapshot(); choice.putString(outcome, "2");
        check(!ServerConfig.valid(choice), "Reject noninteger death outcome");
        override(global, outcome, 0);
        ServerConfig.setRemoteWorld(true);
        check(ServerConfig.snapshot().getInt(outcome) == 1, "Remote client ignores local global mode");
        choice = ServerConfig.snapshot(); choice.putInt(outcome, 2);
        ServerConfig.receiveMirror(choice);
        check(ServerConfig.snapshot().getInt(outcome) == 2, "Server death outcome reaches remote mirror");
        ServerConfig.resetWorldState(); override(global, outcome, -1);
        check(ServerConfig.deathAttributePenaltyEnabled() && ServerConfig.deathAttributeLoss() == 20,
                "Old worlds missing both keys keep the enabled twenty-point default");
        check(!locked(ENABLED) && !locked(LOSS), "Default global config inherits and leaves both settings editable");
        check(global.getComment(LOSS).contains("默认20"), "Generated TOML documents the loss and inheritance");

        for (int loss : new int[]{0, 1, 7, 20, 100, Integer.MAX_VALUE}) {
            CompoundTag draft = ServerConfig.snapshot();
            draft.putInt(LOSS, loss);
            draft.putBoolean(ENABLED, false);
            check(ServerConfig.valid(draft), "Valid integer boundary " + loss);
            ServerConfig.applyUnlockedValues(draft);
            check(!ServerConfig.deathAttributePenaltyEnabled() && ServerConfig.deathAttributeLoss() == loss,
                    "Disabled setting keeps its configured amount for re-enabling");
            draft.putBoolean(ENABLED, true);
            ServerConfig.applyUnlockedValues(draft);
            check(ServerConfig.deathAttributePenaltyEnabled() && ServerConfig.deathAttributeLoss() == loss,
                    "World edits reach the death resolver");
        }
        ServerConfig.DEATH_ATTRIBUTE_LOSS.set(7);
        ServerConfig.DEATH_ATTRIBUTE_PENALTY_ENABLED.set(true);
        for (int loss : new int[]{0, 1, 20, Integer.MAX_VALUE}) {
            override(global, LOSS, loss);
            override(global, ENABLED, 0);
            check(locked(ENABLED) && locked(LOSS), "Both global overrides lock their own UI fields");
            check(!ServerConfig.deathAttributePenaltyEnabled() && ServerConfig.deathAttributeLoss() == loss,
                    "Global death values reach the resolver");
            CompoundTag forged = ServerConfig.snapshot();
            forged.put(ServerConfig.LOCKS_TAG, new CompoundTag());
            forged.putBoolean(ENABLED, true);
            forged.putInt(LOSS, 99);
            check(ServerConfig.valid(forged), "Forged-lock fixture otherwise valid");
            ServerConfig.applyUnlockedValues(forged);
            check(ServerConfig.DEATH_ATTRIBUTE_PENALTY_ENABLED.get() && ServerConfig.DEATH_ATTRIBUTE_LOSS.get() == 7,
                    "Forged client locks cannot overwrite either stored world value");
        }
        override(global, LOSS, -1);
        override(global, ENABLED, -1);
        check(ServerConfig.deathAttributePenaltyEnabled() && ServerConfig.deathAttributeLoss() == 7
                && !locked(ENABLED) && !locked(LOSS), "Removing overrides restores untouched world values");
        int before = ServerConfig.snapshot().getInt("revision");
        override(global, LOSS, 7);
        check(ServerConfig.snapshot().getInt("revision") != before, "A lock-only change invalidates old drafts");

        override(global, LOSS, 91);
        override(global, ENABLED, 0);
        ServerConfig.setRemoteWorld(true);
        check(ServerConfig.deathAttributePenaltyEnabled() && ServerConfig.deathAttributeLoss() == 7,
                "Client global file is ignored while waiting for a server mirror");
        CompoundTag remote = ServerConfig.snapshot();
        remote.putBoolean(ENABLED, true);
        remote.putInt(LOSS, 3);
        remote.getCompound(ServerConfig.LOCKS_TAG).putBoolean(LOSS, true);
        ServerConfig.receiveMirror(remote);
        remote.putBoolean(ENABLED, false);
        remote.putInt(LOSS, 800);
        check(ServerConfig.deathAttributePenaltyEnabled() && ServerConfig.deathAttributeLoss() == 3 && locked(LOSS),
                "Server values and lock are authoritative and defensively copied");
        ServerConfig.snapshot().putInt(LOSS, 900);
        check(ServerConfig.deathAttributeLoss() == 3 && ServerConfig.DEATH_ATTRIBUTE_LOSS.get() == 7,
                "Snapshots cannot mutate the mirror; mirroring never writes local world settings");
        ServerConfig.resetWorldState();
        check(!ServerConfig.deathAttributePenaltyEnabled() && ServerConfig.deathAttributeLoss() == 91,
                "Leaving a server clears both mirrored settings");

        for (Object invalid : new Object[]{-2, -0.5D, 0.5D, 2147483648L, Double.NaN,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, true, "20"}) {
            check(!GlobalConfig.validDeathAttributeLoss(invalid), "Reject invalid global amount " + invalid);
            CommentedConfig config = CommentedConfig.inMemory();
            config.set(LOSS, invalid);
            GlobalWorldConfigRegression.load(GlobalConfig.SPEC, config);
            check(!locked(LOSS), "Loader corrects invalid global value to inherit");
        }
        for (int invalid : new int[]{-1, Integer.MIN_VALUE}) {
            CompoundTag draft = ServerConfig.snapshot();
            draft.putInt(LOSS, invalid);
            check(!ServerConfig.valid(draft), "Negative world amount rejected");
            CommentedConfig config = CommentedConfig.inMemory();
            config.set(LOSS, invalid);
            GlobalWorldConfigRegression.load(ServerConfig.SPEC, config);
            check(ServerConfig.DEATH_ATTRIBUTE_LOSS.get() == 0, "Loader range correction clamps negative world amount to zero");
        }
        for (double invalid : new double[]{-1, 0.5, 2147483648D, Double.NaN, Double.POSITIVE_INFINITY})
            check(!ServerConfig.validDeathAttributeLoss(invalid), "UI/domain rejects invalid loss " + invalid);
        for (String key : new String[]{ENABLED, LOSS}) {
            CompoundTag missing = ServerConfig.snapshot();
            missing.remove(key);
            check(!ServerConfig.valid(missing), "Incomplete packet rejected: " + key);
        }
        CompoundTag wrongType = ServerConfig.snapshot();
        wrongType.putDouble(LOSS, 7);
        check(!ServerConfig.valid(wrongType), "Even an integral double is not a valid integer NBT field");
        wrongType = ServerConfig.snapshot();
        wrongType.putString(ENABLED, "false");
        check(!ServerConfig.valid(wrongType), "String switch is rejected");

        GlobalWorldConfigRegression.load(GlobalConfig.SPEC, CommentedConfig.inMemory());
        GlobalWorldConfigRegression.load(ServerConfig.SPEC, CommentedConfig.inMemory());
        ServerConfig.resetWorldState();
        System.out.println("PASS: " + checks + " death-penalty config checks; defaults, integer boundaries, global locks, server authority and validation");
    }
    private static boolean locked(String key) {
        return ServerConfig.snapshot().getCompound(ServerConfig.LOCKS_TAG).getBoolean(key);
    }
    private static void override(CommentedConfig config, String key, Number value) throws Exception {
        config.set(key, value);
        GlobalConfig.SPEC.getClass().getMethod("afterReload").invoke(GlobalConfig.SPEC);
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
