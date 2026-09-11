package cn.laowu.mod.recipe;
/** Retains workbench positions after 1.21 trims the empty borders of a crafting input. */
public interface CraftingGridOrigin {
    int laowu$width();
    int laowu$height();
    int laowu$left();
    int laowu$top();
}
