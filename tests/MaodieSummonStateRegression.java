import cn.laowu.mod.encounter.MaodieSummonState;
import java.util.UUID;

/** No Minecraft runtime needed: exercise the actual server-clock summon state. */
public final class MaodieSummonStateRegression {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        var state = new MaodieSummonState();
        UUID cat = UUID.randomUUID();
        check(state.stroke(cat, 600) == MaodieSummonState.StrokeResult.ACCEPTED, "first stroke");
        check(state.stroke(cat, 600) == MaodieSummonState.StrokeResult.IGNORED, "same-tick duplicate");
        state.stroke(cat, 602);
        check(state.stroke(cat, 604) == MaodieSummonState.StrokeResult.READY, "third quick stroke");
        state.begin(604);
        check(!state.due(603) && !state.due(613), "third hiss must finish before summon");
        check(state.due(614), "summon immediately after ten-tick third hiss, without aura delay");
        check(state.stroke(cat, 607) == MaodieSummonState.StrokeResult.IGNORED, "pending clicks cannot restart summon");
        check(state.due(614), "ignored extra click must not extend final hiss");
        state.reset();
        state.stroke(cat, 700); state.stroke(cat, 701); state.stroke(cat, 702);
        state.rejectThirdStroke();
        check(state.count() == 2 && state.stroke(cat, 703) == MaodieSummonState.StrokeResult.READY,
                "blocked front spawn keeps third-stroke retry");
        state.reset(); state.stroke(cat, 800); state.stroke(cat, 960);
        check(state.count() == 1, "160 ticks expire previous strokes");
        state.stroke(UUID.randomUUID(), 961);
        check(state.count() == 1, "replacement passenger cannot inherit strokes");
        System.out.println("MaodieSummonStateRegression PASS");
    }
}
