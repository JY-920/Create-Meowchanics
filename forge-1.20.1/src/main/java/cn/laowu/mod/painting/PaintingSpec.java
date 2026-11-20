package cn.laowu.mod.painting;
public record PaintingSpec(int widthBlocks, int heightBlocks, Mode mode, Crop crop) {
    public enum Mode { ORIGINAL, PIXEL }
    public enum Crop { KEEP_WIDTH, KEEP_HEIGHT }
    public PaintingSpec {
        if (widthBlocks<1 || widthBlocks>8 || heightBlocks<1 || heightBlocks>8 || mode==null || crop==null)
            throw new IllegalArgumentException("Invalid painting dimensions or mode");
    }
}
