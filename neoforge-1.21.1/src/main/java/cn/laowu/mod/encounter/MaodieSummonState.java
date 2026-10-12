package cn.laowu.mod.encounter;

import java.util.UUID;

/** Per-seat, transient server-clock state; a new passenger cannot inherit strokes. */
public final class MaodieSummonState {
    public enum StrokeResult { IGNORED, ACCEPTED, READY }
    private UUID catId;
    private int count;
    private long lastStroke = Long.MIN_VALUE, startTick = -1;

    public StrokeResult stroke(UUID id, long now) {
        if (active()) return StrokeResult.IGNORED;
        if (!id.equals(catId)) { reset(); catId = id; }
        if (lastStroke != Long.MIN_VALUE) {
            if (now < lastStroke || now - lastStroke >= 160) count = 0;
            // Deduplicate one server tick, not separate intentional right clicks.
            else if (now == lastStroke) return StrokeResult.IGNORED;
        }
        lastStroke = now;
        count = Math.min(3, count + 1);
        return count == 3 ? StrokeResult.READY : StrokeResult.ACCEPTED;
    }
    public int count() { return count; }
    public boolean active() { return startTick >= 0; }
    public long startTick() { return startTick; }
    public void begin(long now) { if (count == 3 && !active()) startTick = now; }
    /** The third stroke has the same half-second hiss as the preceding strokes. */
    public boolean due(long now) { return active() && now - startTick >= 10; }
    public void rejectThirdStroke() { if (!active() && count == 3) count = 2; }
    public void reset() { catId = null; count = 0; lastStroke = Long.MIN_VALUE; startTick = -1; }
}
