package cn.laowu.mod;

import cn.laowu.mod.client.ClientWorldSettings;
import net.minecraft.nbt.CompoundTag;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Exercises production draft reset and impact scopes without opening a game window. */
public final class CareerDefaultsAndKnockbackRegression {
    private static int checks;

    public static void run() throws Exception {
        resetDefaults();
        knockbackScopes();
        System.out.println("PASS: " + checks
                + " career reset / knockback checks; permissions, all global-lock combinations, "
                + "draft isolation, nested hits, opt-in, cancelled damage, exceptions and thread isolation");
    }

    private static void resetDefaults() {
        double[] expected = {0.75D, 0.55D, 2D, 0.75D, 1.2D, 0D, 1.5D, 3D, 0D, 0D, 1.75D, 1D, .85D};
        CompoundTag serverBefore = ServerConfig.snapshot();
        check(ClientWorldSettings.editableCareerDefaults(null).isEmpty(), "No unsynced reset");
        for (int mask = 0; mask < 1 << expected.length; mask++) {
            CompoundTag draft = serverBefore.copy();
            draft.putBoolean("can_edit", true);
            draft.putString("future_setting", "preserved");
            draft.putDouble("attack", 12.5D);
            draft.putBoolean("cats_hiss", false);
            CompoundTag careers = draft.getCompound(ServerConfig.CAREER_DAMAGE_TAG);
            careers.putDouble("future_career", 42D);
            CompoundTag locks = draft.getCompound(ServerConfig.LOCKS_TAG);
            for (int i = 0; i < expected.length; i++) {
                CatOutfitType outfit = ServerConfig.CAREERS.get(i);
                careers.putDouble(outfit.id(), 70D + i);
                locks.putBoolean(ServerConfig.careerDamageKey(outfit), (mask & (1 << i)) != 0);
            }
            CompoundTag before = draft.copy();
            var defaults = ClientWorldSettings.editableCareerDefaults(draft);
            check(defaults.size() == expected.length - Integer.bitCount(mask), "Reset only unlocked careers");
            for (int i = 0; i < expected.length; i++) {
                CatOutfitType outfit = ServerConfig.CAREERS.get(i);
                if ((mask & (1 << i)) == 0)
                    check(defaults.get(outfit) == expected[i], "Reset original K, not 1: " + outfit);
                else check(!defaults.containsKey(outfit), "Keep forced K: " + outfit);
            }
            check(!defaults.containsKey(CatOutfitType.NONE), "Do not introduce ordinary-cat K");
            check(draft.equals(before), "Computing reset never mutates the draft or revision");
            check(ServerConfig.snapshot().equals(serverBefore), "No implicit server save");
            defaults.clear();
            check(ClientWorldSettings.editableCareerDefaults(draft).size()
                    == expected.length - Integer.bitCount(mask), "No shared mutable defaults");
            draft.putBoolean("can_edit", false);
            check(ClientWorldSettings.editableCareerDefaults(draft).isEmpty(), "Read-only user cannot reset");
        }
    }

    private static void knockbackScopes() throws Exception {
        var scope = new CatProjectileDamage.KnockbackScope<Object>();
        Object a = new Object(), b = new Object();
        check(!scope.suppresses(a), "Ordinary damage outside impact is unchanged");
        boolean accepted = scope.call(a, false, () -> {
            check(scope.suppresses(a), "Only current victim is suppressed");
            check(!scope.suppresses(b), "Thorns on the shooter or unrelated victim is not suppressed");
            check(!scope.call(a, true, () -> {
                check(!scope.suppresses(a), "Explicit normal knockback overrides outer scope");
                return false;
            }), "Cancelled damage return value is preserved");
            check(scope.suppresses(a), "Restore outer suppressed impact");
            scope.call(b, false, () -> {
                check(scope.suppresses(b) && !scope.suppresses(a), "Nested impact uses its own victim");
                return true;
            });
            check(scope.suppresses(a) && !scope.suppresses(b), "Restore outer victim after nested hit");
            try {
                scope.call(b, false, () -> { throw new IllegalStateException("expected"); });
                throw new AssertionError("Damage exception must propagate");
            } catch (IllegalStateException expected) {
                check(scope.suppresses(a) && !scope.suppresses(b), "Exception restores outer victim");
            }
            var executor = Executors.newSingleThreadExecutor();
            try {
                boolean isolated = executor.submit(() -> !scope.suppresses(a)
                        && scope.call(a, true, () -> !scope.suppresses(a))
                        && !scope.suppresses(a)).get(5, TimeUnit.SECONDS);
                check(isolated && scope.suppresses(a), "Client/server or parallel worlds do not share scope");
            } catch (Exception ex) {
                throw new AssertionError("Thread isolation check failed", ex);
            } finally {
                executor.shutdownNow();
            }
            return true;
        });
        check(accepted && !scope.suppresses(a), "Successful impact cleanup");
        check(!scope.call(a, false, () -> false) && !scope.suppresses(a), "Blocked impact cleanup");
        try {
            scope.call(a, false, () -> { throw new IllegalArgumentException("expected"); });
            throw new AssertionError("Damage exception must propagate");
        } catch (IllegalArgumentException expected) {
            check(!scope.suppresses(a), "Top-level exception cannot leave immunity behind");
        }
        scope.call(a, true, () -> {
            check(!scope.suppresses(a), "Future accessory can retain vanilla knockback");
            scope.call(a, false, () -> {
                check(scope.suppresses(a), "Suppressed impact inside a normal impact");
                return true;
            });
            check(!scope.suppresses(a), "Normal impact restored");
            return true;
        });
        check(!scope.suppresses(a), "No persistent flags");
    }

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
}
