package cn.laowu.mod.client;

import java.util.ArrayList;
import java.util.List;

/** Integer-pixel wheel geometry shared by rendering, clicking and offline regression tests. */
public final class CatLaserWheelLayout {
    public static final int SECTIONS = 3;
    private static final double TAU = Math.PI * 2;
    public static int sectionAt(double x, double y, double radius) {
        double distance = Math.hypot(x, y);
        if (!Double.isFinite(distance) || radius <= 0 || distance < radius * .31 || distance > radius) return -1;
        double angle = (Math.atan2(y, x) + Math.PI / 2 + Math.PI / 3 + TAU) % TAU;
        double sector = angle / (TAU / SECTIONS);
        double fraction = sector - Math.floor(sector);
        return fraction < .012 || fraction > .988 ? -1 : (int) sector;
    }
    public static List<Span> spans(int radius) {
        List<Span> spans = new ArrayList<>();
        for (int y = -radius; y <= radius; y++) {
            int start = -radius, previous = sectionAt(start, y, radius);
            for (int x = -radius + 1; x <= radius + 1; x++) {
                int section = x > radius ? -1 : sectionAt(x, y, radius);
                if (section != previous) {
                    if (previous >= 0) spans.add(new Span(start, x, y, previous));
                    start = x;
                    previous = section;
                }
            }
        }
        return List.copyOf(spans);
    }
    public static double labelAngle(int section) { return -Math.PI / 2 + section * TAU / SECTIONS; }
    public record Span(int x0, int x1, int y, int section) {}
    private CatLaserWheelLayout() {}
}
