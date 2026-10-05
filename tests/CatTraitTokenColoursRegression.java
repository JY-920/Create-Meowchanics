import cn.laowu.mod.client.CatTraitTokenColours;

public final class CatTraitTokenColoursRegression {
    public static void main(String[] args) throws Exception {
        // A broad yellow mask would destroy the glass and cork in the supplied sprite.
        for (int frame = 0; frame < 4; frame++) {
            for (int pixel : new int[]{0, 0xFFFFFFFF, 0xFFFFDE88, 0xFF7A0C00, 0xFFFFF8A2}) {
                require(CatTraitTokenColours.recolour(pixel, frame) == pixel, "glass/cork/alpha changed");
            }
        }
        int gold = CatTraitTokenColours.recolour(0xFFE38C2C, 2);
        int blue = CatTraitTokenColours.recolour(0xFFE38C2C, 1);
        int brown = CatTraitTokenColours.recolour(0xFFE38C2C, 0);
        int grey = CatTraitTokenColours.recolour(0xFFE38C2C, 3);
        require(gold == 0xFFE38C2C, "excellent must retain authored gold");
        require((blue & 255) > ((blue >> 16) & 255), "good must become blue");
        require(((brown >> 16) & 255) > (brown & 255) && brown != gold, "common must become brown");
        require((grey & 255) == ((grey >> 8) & 255) && (grey & 255) < 120, "defect must become dark grey");
        require(CatTraitTokenColours.recolour(0x80E38C2C, 1) >>> 24 == 128, "alpha lost");
        require(CatTraitTokenColours.recolour(0xFFF4B446, 1) != blue, "liquid shading flattened");
        var sprite = javax.imageio.ImageIO.read(new java.io.File("assets-source/cat-editor/bottled-trait.png"));
        var liquid = java.util.Set.of("8,7", "7,8", "8,8", "7,9", "8,9", "10,9", "13,9",
                "7,10", "8,10", "10,10", "13,10", "7,11", "8,11", "11,11", "12,11",
                "7,12", "8,12", "7,13", "8,13");
        require(sprite.getWidth() == 16 && sprite.getHeight() == 16, "unexpected source size");
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int pixel = sprite.getRGB(x, y);
                for (int frame = 0; frame < 4; frame++) {
                    boolean changed = CatTraitTokenColours.recolour(pixel, frame) != pixel;
                    require(changed == (frame != 2 && liquid.contains(x + "," + y)),
                            "incorrect source pixel mask at " + x + "," + y + " frame " + frame);
                }
            }
        }
        System.out.println("CatTraitTokenColoursRegression PASS");
    }
    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
