import cn.laowu.mod.recipe.KimiDyeCrafting;
import cn.laowu.mod.item.KimiDyePalette;
import java.util.Arrays;

public final class KimiArmorDyeRegression {
    static void check(boolean value) { if(!value) throw new AssertionError(); }
    static int[] grid() { int[] a=new int[9]; Arrays.fill(a,-2); a[4]=-1; return a; }
    public static void main(String[] args) {
        for (int scale : new int[]{1,2,4}) {
            for (int x : new int[]{1,8}) for (int y : new int[]{1,2}) {
                check(KimiDyePalette.region("textures/models/armor/kimi_helmet.png",
                        x*scale,y*scale,64*scale,64*scale,0xEAEAEA)==-1);
                check(KimiDyePalette.region("textures/models/armor/kimi_armor.png",
                        (x+10)*scale,(y+10)*scale,64*scale,64*scale,0xEAEAEA)==-1);
                check(KimiDyePalette.region("textures/models/armor/kimi_armor_slim.png",
                        (x+10)*scale,(y+10)*scale,64*scale,64*scale,0xFFFFFF)==-1);
            }
            for (int x : new int[]{5,10}) for(int y:new int[]{5,6})
                check(KimiDyePalette.region("textures/item/cat_helmet.png",
                        x*scale,y*scale,16*scale,16*scale,0xEAEAEA)==-1);
        }
        check(KimiDyePalette.region("textures/models/armor/kimi_helmet.png",2,4,64,64,0xEAEAEA)==1);
        check(KimiDyePalette.region("textures/models/armor/kimi_helmet.png",2,1,64,64,0x4D890D)==2);
        int cases=0;
        for(int a=-1;a<16;a++) for(int b=-1;b<16;b++) for(int c=-1;c<16;c++) {
            int[] cells=grid();
            int[] dyes={a,b,c};
            int previous=KimiDyePalette.replace(KimiDyePalette.replace(0,0,14),2,13);
            for(int i=0;i<3;i++) if(dyes[i]>=0) cells[i]=dyes[i];
            int actual=KimiDyeCrafting.result(cells,3,3,previous);
            if(a<0 && b<0 && c<0) check(actual==0);
            else for(int i=0;i<3;i++) check(KimiDyePalette.channel(actual,i)
                ==(dyes[i]<0?KimiDyePalette.channel(previous,i):dyes[i]));
            cases++;
        }
        int[] cells=grid(); cells[3]=1;
        check(KimiDyeCrafting.result(cells,3,3,0)==-1);
        cells=grid();cells[0]=16;check(KimiDyeCrafting.result(cells,3,3,0)==-1);
        cells=grid();cells[0]=1;cells[5]=-1;check(KimiDyeCrafting.result(cells,3,3,0)==-1);
        check(KimiDyeCrafting.result(new int[]{1,-2,-1,-2},2,2,0)==-1);
        check(KimiDyeCrafting.result(new int[]{-1,-2,-2,-2},2,2,1)==0);
        check(KimiDyePalette.region(0xE79696)==-1); // nose stays pink
        check(KimiDyePalette.region(0xFFFFFF)==-1); // eye whites never take secondary dye
        cells=grid();cells[2]=5;
        check(KimiDyeCrafting.result(cells,3,3,0,false)==-1);
        check(KimiDyeCrafting.result(cells,3,3,0,true)>=0);
        cells=grid();cells[1]=5;
        check(KimiDyeCrafting.result(cells,3,3,0,false)>=0);
        check(KimiDyePalette.region(0x65B213)==2);
        check(KimiDyePalette.region(0xEAD4AE)==1);
        check(KimiDyePalette.region(0xF4B446)==0);
        for(int colour:new int[]{0xFFFFFF,0x1D1D21,0x3C44AA,0xB02E26,0x80C71F}) {
            int dark=KimiDyePalette.shade(0xDB7920,colour,0);
            int base=KimiDyePalette.shade(0xEAA939,colour,0);
            int light=KimiDyePalette.shade(0xF4B446,colour,0);
            check(base==colour);
            for(int bit:new int[]{0,8,16}) {
                check(((dark>>bit)&255)<=((base>>bit)&255));
                check(((light>>bit)&255)>=((base>>bit)&255));
            }
        }
        System.out.println("PASS: "+cases+" dye/empty combinations, invalid layouts, reset, palette and shading");
    }
}
