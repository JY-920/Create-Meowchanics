package cn.laowu.mod.recipe;

import cn.laowu.mod.item.KimiDyePalette;

/** Logical cells: -2 empty, -1 armour, 0..15 dye, any other value invalid. */
public final class KimiDyeCrafting {
    public static int result(int[] cells, int width, int height, int previous) {
        return result(cells,width,height,previous,true);
    }
    public static int result(int[] cells, int width, int height, int previous, boolean helmet) {
        if (width * height != cells.length) return -1;
        int occupied=0, armor=-1;
        for(int i=0;i<cells.length;i++) {
            if(cells[i]==-2) continue;
            occupied++;
            if(cells[i]==-1) { if(armor!=-1) return -1; armor=i; }
        }
        if(armor<0) return -1;
        if(occupied==1) return previous==0 ? -1 : 0;
        if(width!=3 || height!=3 || armor!=4) return -1;
        if(!helmet && cells[2]>=0) return -1;
        int code=previous;
        for(int i=0;i<cells.length;i++) {
            if(cells[i]==-2 || i==armor) continue;
            if(i>2 || cells[i]<0 || cells[i]>15) return -1;
            code=KimiDyePalette.replace(code,i,cells[i]);
        }
        return code;
    }
    private KimiDyeCrafting() {}
}
