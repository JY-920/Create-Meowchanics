package cn.laowu.mod.test;

import cn.laowu.mod.CatEditorTraits;
import cn.laowu.mod.genetics.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.gametest.*;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationTargetException;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class CatEditorRiskProbe {
    @GameTest(template = "artillery_probe", batch = "cat_editor_risk", timeoutTicks = 30)
    public static void installOnlyRiskAndNonmutatingAvailability(GameTestHelper h) throws Exception {
        var cat = net.minecraft.world.entity.EntityType.CAT.create(h.getLevel());
        try {
            CatTraitData.set(cat, CatTraitProfile.EMPTY);
            CatAttributeData.set(cat, profile(100, 100));
            var token = cn.laowu.mod.item.CatTraitTokenItem.create(CatTrait.THORNS, 1);
            var before = CatAttributeData.read(cat).orElseThrow().save();
            var canInstall = CatEditorTraits.class.getMethod("canInstall",
                    net.minecraft.world.entity.animal.Cat.class, net.minecraft.world.item.ItemStack.class);
            h.assertTrue((boolean) canInstall.invoke(null, cat, token), "Valid input is available");
            h.assertTrue(before.equals(CatAttributeData.read(cat).orElseThrow().save())
                    && token.getCount() == 1 && CatTraitData.read(cat).orElseThrow().traits().isEmpty(),
                    "Availability does not mutate or consume");
            // Find a seed selecting the NOW branch, then derive its independently calculated literal loss.
            long seed = 0;
            RandomSource expected;
            do { expected = RandomSource.create(seed++); } while (expected.nextInt(100) >= 50);
            int stat = expected.nextInt(6), loss = 10 + expected.nextInt(41);
            cat.getRandom().setSeed(seed - 1);
            h.assertTrue(CatEditorTraits.install(cat, token), "Valid install succeeds");
            check(h, CatAttributeData.read(cat).orElseThrow(), stat, 100 - loss, -1, 100);
            var after = CatAttributeData.read(cat).orElseThrow().save();
            var duplicate = cn.laowu.mod.item.CatTraitTokenItem.create(CatTrait.THORNS, 1);
            h.assertTrue(!(boolean) canInstall.invoke(null, cat, duplicate)
                    && !CatEditorTraits.install(cat, duplicate) && duplicate.getCount() == 1,
                    "Duplicate is unavailable and rejected without consumption");
            h.assertTrue(after.equals(CatAttributeData.read(cat).orElseThrow().save()), "Rejected install has no risk");
            h.assertTrue(!CatEditorTraits.extract(cat, CatTrait.THORNS.id()).isEmpty(), "Extraction succeeds");
            h.assertTrue(after.equals(CatAttributeData.read(cat).orElseThrow().save()), "Extraction has no risk");
            h.succeed();
        } finally { cat.discard(); }
    }


    private static CatAttributeProfile profile(int now, int max) {
        var p = CatAttributeProfile.founder(RandomSource.create(1));
        for (var stat : CatStat.values()) p = p.withValues(stat, now, max);
        return p;
    }

    private static CatAttributeProfile risk(GameTestHelper h, CatAttributeProfile p, CatTraitRarity rarity, int... draws) {
        int[] index = {0};
        RandomSource random = (RandomSource) Proxy.newProxyInstance(RandomSource.class.getClassLoader(),
                new Class<?>[]{RandomSource.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("nextInt") || args == null || args.length != 1)
                        throw new AssertionError("Unexpected random operation: " + method.getName());
                    if (index[0] >= draws.length) throw new AssertionError("Unexpected extra random draw");
                    int value = draws[index[0]++];
                    if (value < 0 || value >= (int) args[0]) throw new AssertionError("Invalid random bound");
                    return value;
                });
        try {
            var result = (CatAttributeProfile) CatEditorTraits.class.getMethod("applyInstallRisk",
                    CatAttributeProfile.class, CatTraitRarity.class, RandomSource.class).invoke(null, p, rarity, random);
            h.assertTrue(index[0] == draws.length, "Every expected risk draw was consumed");
            return result;
        } catch (NoSuchMethodException missing) {
            h.assertTrue(false, "Missing installation risk behavior");
            throw new AssertionError(missing);
        } catch (InvocationTargetException error) { throw new RuntimeException(error.getCause()); }
        catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
    }

    private static void check(GameTestHelper h, CatAttributeProfile p, int nowIndex, int now,
                              int maxIndex, int max) {
        for (int i = 0; i < CatStat.values().length; i++) {
            CatStat stat = CatStat.values()[i];
            int expectedMax = i == maxIndex ? max : 100;
            int expectedNow = Math.min(i == nowIndex ? now : 100, expectedMax);
            h.assertTrue(p.current(stat) == expectedNow && p.potential(stat) == expectedMax,
                    "Only independently selected loci change: " + stat + " got " + p.current(stat) + "/" + p.potential(stat));
        }
    }

    @GameTest(template = "artillery_probe", batch = "cat_editor_risk", timeoutTicks = 30)
    public static void exactBucketsInclusiveRangesAndIndependentStats(GameTestHelper h) {
        var p = profile(100, 100);
        check(h, risk(h, p, CatTraitRarity.EXCELLENT, 0, 0, 0), 0, 90, -1, 100);
        check(h, risk(h, p, CatTraitRarity.EXCELLENT, 49, 5, 40), 5, 50, -1, 100);
        check(h, risk(h, p, CatTraitRarity.EXCELLENT, 50, 1, 0), -1, 100, 1, 90);
        check(h, risk(h, p, CatTraitRarity.EXCELLENT, 59, 4, 10), -1, 100, 4, 80);
        check(h, risk(h, p, CatTraitRarity.EXCELLENT, 60), -1, 100, -1, 100);
        check(h, risk(h, p, CatTraitRarity.EXCELLENT, 99), -1, 100, -1, 100);
        check(h, risk(h, p, CatTraitRarity.GOOD, 0, 3, 0), 3, 90, -1, 100);
        check(h, risk(h, p, CatTraitRarity.GOOD, 49, 4, 40), 4, 50, -1, 100);
        check(h, risk(h, p, CatTraitRarity.GOOD, 50), -1, 100, -1, 100);
        check(h, risk(h, p, CatTraitRarity.GOOD, 99), -1, 100, -1, 100);
        check(h, risk(h, p, CatTraitRarity.COMMON), -1, 100, -1, 100);
        check(h, risk(h, p, CatTraitRarity.DEFECT), -1, 100, -1, 100);
        int base=0,cap=0,none=0;
        var countsProfile=profile(80,100);
        for(int roll=0;roll<100;roll++) {
            var result=roll<60?risk(h,countsProfile,CatTraitRarity.EXCELLENT,roll,0,0)
                    :risk(h,countsProfile,CatTraitRarity.EXCELLENT,roll);
            boolean baseLoss=result.current(CatStat.values()[0])<80;
            boolean capLoss=result.potential(CatStat.values()[0])<100;
            h.assertTrue(!(baseLoss&&capLoss),"No combined-loss branch remains");
            if(baseLoss)base++;else if(capLoss)cap++;else none++;
        }
        h.assertTrue(base==50&&cap==10&&none==40,"Excellent risk is exactly 50/10/40");
        check(h, p, -1, 100, -1, 100);
        h.succeed();
    }

    @GameTest(template = "artillery_probe", batch = "cat_editor_risk", timeoutTicks = 30)
    public static void lossesClampAtZeroAndCurrentNeverExceedsMaximum(GameTestHelper h) {
        var p = profile(5, 8);
        var result = risk(h, p, CatTraitRarity.EXCELLENT, 0, 0, 40);
        h.assertTrue(result.current(CatStat.values()[0]) == 0 && result.potential(CatStat.values()[0]) == 8,
                "NOW loss floors at zero without changing MAX");
        result = risk(h, p, CatTraitRarity.EXCELLENT, 50, 1, 10);
        h.assertTrue(result.current(CatStat.values()[1]) == 0 && result.potential(CatStat.values()[1]) == 0,
                "MAX loss floors at zero and clamps NOW");
        h.assertTrue(p.current(CatStat.values()[0]) == 5, "Helper is immutable");
        h.succeed();
    }
}
