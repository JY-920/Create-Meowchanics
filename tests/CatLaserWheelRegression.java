package cn.laowu.mod;

import cn.laowu.mod.client.CatHealthBarLayout;
import cn.laowu.mod.client.CatLaserWheelLayout;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.UUID;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** Real saved-data serialization, production geometry and production atlas drawing plan; no live game required. */
public final class CatLaserWheelRegression {
    private static int checks;
    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        Path atlasPath = Path.of(args[0]), output = Path.of(args[1]);
        Files.createDirectories(output);
        savedData();
        geometry();
        health();
        colours();
        BufferedImage atlas = ImageIO.read(atlasPath.toFile());
        check(atlas.getWidth() == 64 && atlas.getHeight() == 64, "atlas dimensions");
        check(atlas.getRGB(17, 28) == 0xFFF8F8EC, "fill samples the compensated opaque light pixel");
        for (char c : "0123456789%".toCharArray()) {
            var glyph = CatHealthBarLayout.glyph(c);
            check(glyph.u() >= 0 && glyph.v() >= 0 && glyph.u() + glyph.width() <= 64 && glyph.v() + 7 <= 64, "glyph bounds");
            int foreground = 0;
            for (int y = 0; y < 7; y++) for (int x = 0; x < glyph.width(); x++)
                if ((atlas.getRGB(glyph.u() + x, glyph.v() + y) & 0xFFFFFF) == 0xF8F8EC) foreground++;
            check(foreground > 3, "visible glyph " + c);
        }
        pixelPreview(atlas, output);
        wheelPreview(output);
        System.out.println("PASS: " + checks + " laser-wheel checks; per-owner/world saved data, selection pixels, health clamping, continuous pastel colours, unchanged borders/digits and shared rendering plan.");
    }

    private static CompoundTag save(CatCombatPreferences data) throws Exception {
        for (var method : CatCombatPreferences.class.getDeclaredMethods()) {
            if (method.getName().equals("save") && method.getParameterTypes()[0] == CompoundTag.class)
                return (CompoundTag) (method.getParameterCount() == 1
                        ? method.invoke(data, new CompoundTag()) : method.invoke(data, new CompoundTag(), null));
        }
        throw new AssertionError("save method");
    }

    private static void savedData() throws Exception {
        UUID first = new UUID(12, 1), second = new UUID(12, 2);
        CatCombatPreferences worldA = new CatCombatPreferences(), worldB = new CatCombatPreferences();
        check(!worldA.aggressive(first) && !worldA.aggressive(second), "default is orders / defence");
        worldA.setAggressive(first, true);
        check(worldA.aggressive(first) && !worldA.aggressive(second) && !worldB.aggressive(first), "owner and world isolation");
        check(worldA.isDirty(), "marks changed saved data dirty");
        worldA.setDirty(false);
        worldA.setAggressive(first, true);
        check(!worldA.isDirty(), "no-op does not resave");
        worldA = CatCombatPreferences.load(save(worldA));
        check(worldA.aggressive(new UUID(12, 1)) && !worldA.aggressive(second), "UUID persists without live player/cat/item");
        worldA.setAggressive(first, false);
        check(!CatCombatPreferences.load(save(worldA)).aggressive(first), "off persists");
        worldA.setAggressive(null, true);
        check(!worldA.aggressive(null), "missing owner safe");
        CompoundTag corrupt = new CompoundTag();
        ListTag entries = new ListTag();
        entries.add(new IntArrayTag(new int[] {1}));
        entries.add(net.minecraft.nbt.NbtUtils.createUUID(second));
        corrupt.put("AggressiveOwners", entries);
        worldA = CatCombatPreferences.load(corrupt);
        check(worldA.aggressive(second) && !worldA.aggressive(first), "malformed entry does not erase valid entries");
    }

    private static void geometry() {
        for (int radius : new int[] {24, 48, 71, 90, 96}) {
            int side = radius * 2 + 3;
            int[][] drawn = new int[side][side];
            for (int[] row : drawn) Arrays.fill(row, -1);
            for (var span : CatLaserWheelLayout.spans(radius)) {
                check(span.x0() < span.x1() && span.section() >= 0 && span.section() < 3, "valid cached span");
                for (int x = span.x0(); x < span.x1(); x++) {
                    check(drawn[span.y()+radius+1][x+radius+1] == -1, "no overlapping sectors");
                    drawn[span.y()+radius+1][x+radius+1] = span.section();
                }
            }
            for (int y = -radius; y <= radius; y++) for (int x = -radius; x <= radius; x++)
                check(drawn[y+radius+1][x+radius+1] == CatLaserWheelLayout.sectionAt(x, y, radius), "draw and click alignment");
            for (int section = 0; section < 3; section++) {
                double angle = CatLaserWheelLayout.labelAngle(section);
                check(CatLaserWheelLayout.sectionAt(Math.cos(angle)*radius*.66, Math.sin(angle)*radius*.66, radius) == section,
                        "label lies in its own sector");
            }
            check(CatLaserWheelLayout.sectionAt(0, 0, radius) == -1, "center closes, never toggles");
            check(CatLaserWheelLayout.sectionAt(0, radius+1, radius) == -1, "outside is inactive");
        }
        check(CatLaserWheelLayout.sectionAt(Double.NaN, 0, 96) == -1, "nonfinite mouse safe");
    }

    private static void health() {
        for (float maximum : new float[] {1, 10, 40, 100, 1024, 999999}) {
            int lastPercent = -1, lastFill = -1;
            for (int i = 0; i <= 1000; i++) {
                float health = maximum * i / 1000;
                int percent = CatHealthBarLayout.percent(health, maximum), fill = CatHealthBarLayout.fillPixels(health, maximum);
                check(percent >= lastPercent && percent <= 100, "monotone bounded percent");
                check(fill >= lastFill && fill <= 34, "monotone bounded fill");
                lastPercent = percent; lastFill = fill;
                CatHealthBarLayout.draw(health, maximum, (x,y,w,h,u,v,sw,sh,z,tint) -> {
                    check(x >= 0 && y >= 0 && x+w <= 64 && y+h <= 12, "destination never clips frame");
                    check(u >= 0 && v >= 0 && u+sw <= 64 && v+sh <= 64, "UV never clips atlas");
                    if (tint != -1) check(x == CatHealthBarLayout.width(health, maximum) - 38
                            && y == 4 && h == 3 && u == 17 && v == 28 && sw == 1 && sh == 1,
                            "Only progress fill receives colour, never frame/background/digits");
                });
            }
        }
        check(CatHealthBarLayout.percent(200, 100) == 100 && CatHealthBarLayout.fillPixels(200,100) == 34, "over-max clamps");
        check(CatHealthBarLayout.percent(0.001F, 100) == 1, "living sliver does not show zero");
        for (float bad : new float[] {0, -1, Float.NaN, Float.POSITIVE_INFINITY}) {
            check(CatHealthBarLayout.percent(1, bad) == 0, "invalid max safe");
            check(CatHealthBarLayout.fillPixels(bad, 100) == 0, "invalid health safe");
        }
    }

    private static void colours() {
        check(CatHealthBarLayout.fillColour(0, 100) == 0xFFF2A6A6, "Low health is pastel red");
        check(CatHealthBarLayout.fillColour(50, 100) == 0xFFF2DEA0, "Half health is pastel yellow");
        check(CatHealthBarLayout.fillColour(100, 100) == 0xFFA8E6B0, "Full health is pastel green");
        check(CatHealthBarLayout.fillColour(200, 100) == CatHealthBarLayout.FULL_COLOUR,
                "Over-max colour clamped");
        check(CatHealthBarLayout.fillColour(-1, 100) == CatHealthBarLayout.LOW_COLOUR,
                "Negative colour clamped");
        for (float invalid : new float[]{0, -1, Float.NaN, Float.POSITIVE_INFINITY})
            check(CatHealthBarLayout.fillColour(1, invalid) == CatHealthBarLayout.LOW_COLOUR,
                    "Invalid max has safe colour");
        java.util.Set<Integer> observed = new java.util.HashSet<>();
        int previous = CatHealthBarLayout.fillColour(0, 10000);
        for (int i = 0; i <= 10000; i++) {
            int colour = CatHealthBarLayout.fillColour(i, 10000);
            check(colour >>> 24 == 255, "Fill remains opaque");
            for (int shift : new int[]{16, 8, 0}) {
                int channel = colour >> shift & 255, prior = previous >> shift & 255;
                check(channel >= 160 && channel <= 242, "Soft pastel channel bounds");
                check(Math.abs(channel - prior) <= 1, "Continuous transitions, including half health");
            }
            observed.add(colour);
            previous = colour;
        }
        check(observed.size() > 100, "Interpolated colours, not three discrete bands");
        check(CatHealthBarLayout.fillColour(25, 100) == CatHealthBarLayout.fillColour(250, 1000),
                "Colour follows health ratio, not absolute hit points");
    }

    /** Same per-channel texture multiplication as the in-game vertex colour. */
    private static int tinted(int source, int tint) {
        int result = 0;
        for (int shift : new int[]{24, 16, 8, 0})
            result |= (((source >>> shift & 255) * (tint >>> shift & 255) + 127) / 255) << shift;
        return result;
    }

    private static BufferedImage bar(BufferedImage atlas, int percent) {
        int width = CatHealthBarLayout.width(percent, 100);
        int atlasU = width == 64 ? 0 : 4, atlasV = width == 64 ? 0 : 13;
        BufferedImage image = new BufferedImage(width, 12, BufferedImage.TYPE_INT_ARGB);
        BufferedImage untinted = new BufferedImage(width, 12, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        Graphics2D plain = untinted.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        CatHealthBarLayout.draw(percent, 100, (x,y,w,h,u,v,sw,sh,z,tint) -> {
            plain.drawImage(atlas, x,y,x+w,y+h,u,v,u+sw,v+sh,null);
            if (tint == -1) {
                g.drawImage(atlas, x,y,x+w,y+h,u,v,u+sw,v+sh,null);
            } else {
                BufferedImage sample = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
                for (int sy = 0; sy < sh; sy++) for (int sx = 0; sx < sw; sx++)
                    sample.setRGB(sx, sy, tinted(atlas.getRGB(u+sx, v+sy), tint));
                g.drawImage(sample, x, y, w, h, null);
            }
        });
        g.dispose();
        plain.dispose();
        int fill = CatHealthBarLayout.fillPixels(percent, 100);
        for (int y = 0; y < 12; y++) for (int x = 0; x < width; x++) {
            if (x >= width - 38 && x < width - 38 + fill && y >= 4 && y < 7) {
                int expected = CatHealthBarLayout.fillColour(percent, 100), actual = image.getRGB(x, y);
                for (int shift : new int[]{16, 8, 0}) check(
                        Math.abs((actual >> shift & 255) - (expected >> shift & 255)) <= 1,
                        "Atlas tint compensation matches requested pastel within rounding");
            } else check(image.getRGB(x,y) == untinted.getRGB(x,y),
                    "Every non-fill pixel, including digits, remains unchanged");
        }
        for (int x = 0; x < width; x++) {
            check(image.getRGB(x,0) == atlas.getRGB(atlasU+x,atlasV), "top black border retained");
            check(image.getRGB(x,11) == atlas.getRGB(atlasU+x,atlasV+11), "bottom black border retained");
        }
        for (int y = 0; y < 12; y++)
            check(image.getRGB(0,y) == atlas.getRGB(atlasU,atlasV+y)
                    && image.getRGB(width-1,y) == atlas.getRGB(63,atlasV+y), "side borders retained");
        return image;
    }

    private static void pixelPreview(BufferedImage atlas, Path output) throws Exception {
        BufferedImage sheet = new BufferedImage(384, 12*6*8, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        int row = 0;
        for (int percent : new int[] {100, 89, 75, 50, 25, 10, 1, 0}) {
            BufferedImage bar = bar(atlas, percent);
            g.drawImage(bar, (64-bar.getWidth())*3, row++*72, bar.getWidth()*6, 72, null);
        }
        g.dispose();
        ImageIO.write(sheet, "png", output.resolve("health-bars.png").toFile());
    }

    /** Geometry preview only: platform text here is not a Minecraft/shader integration test. */
    private static void wheelPreview(Path output) throws Exception {
        BufferedImage image = new BufferedImage(360,260,BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(0x30343A)); g.fillRect(0,0,360,260);
        int cx=180, cy=130, radius=96;
        for (var span : CatLaserWheelLayout.spans(radius)) {
            g.setColor(new Color(span.section()==2 ? 0xE6BA75 : 0x9B7050));
            g.fillRect(cx+span.x0(),cy+span.y(),span.x1()-span.x0(),1);
        }
        g.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
        g.setColor(new Color(0xFFF3D9));
        centered(g,"猫咪指挥",180,cy-radius-8);
        String[] titles={"猫咪血条","团队显示","战斗模式"}, states={"开启","关闭","仅指示或保护"};
        for(int i=0;i<3;i++){
            double angle=CatLaserWheelLayout.labelAngle(i);
            int x=cx+(int)Math.round(Math.cos(angle)*radius*.66), y=cy+(int)Math.round(Math.sin(angle)*radius*.66);
            centered(g,titles[i],x,y-2); centered(g,states[i],x,y+12);
        }
        centered(g,"关闭",cx,cy+6);
        centered(g,"左键 / 1–3 切换 · 右键 / Esc 关闭",cx,cy+radius+19);
        g.dispose();
        ImageIO.write(image,"png",output.resolve("wheel-geometry.png").toFile());
    }

    private static void centered(Graphics2D g,String s,int x,int y){g.drawString(s,x-g.getFontMetrics().stringWidth(s)/2,y);}
}
