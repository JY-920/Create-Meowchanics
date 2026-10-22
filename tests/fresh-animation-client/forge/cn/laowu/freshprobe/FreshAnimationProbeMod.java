package cn.laowu.freshprobe;

import net.minecraftforge.fml.common.Mod;

/** Separate test-only mod, never included in the laowu runtime jar. */
@Mod("laowu_fresh_probe")
public final class FreshAnimationProbeMod {
    public FreshAnimationProbeMod() {
        System.out.println("FRESH PROBE: separate SRG production render test mod loaded");
    }
}
