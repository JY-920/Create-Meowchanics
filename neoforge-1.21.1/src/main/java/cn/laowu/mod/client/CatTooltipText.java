package cn.laowu.mod.client;

import cn.laowu.mod.compat.create.CreateIntegration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

/** Independent formatted descriptions; installed Create keeps its native line wrapping. */
public final class CatTooltipText {
    public record Palette(net.minecraft.network.chat.Style primary, net.minecraft.network.chat.Style highlight) {
        public static final Palette STANDARD_CREATE = new Palette(
                net.minecraft.network.chat.Style.EMPTY.withColor(13211468),
                net.minecraft.network.chat.Style.EMPTY.withColor(15850873));
    }
    public static List<Component> cutStringTextComponent(String text, Palette palette) {
        if (CreateIntegration.isLoaded()) return cn.laowu.mod.compat.create.CreateClientEvents.tooltip(text);
        return cutStringTextComponent(text,palette.primary(),palette.highlight(),0);
    }
    public static List<Component> cutStringTextComponent(String text, net.minecraft.network.chat.Style primary, net.minecraft.network.chat.Style highlight, int indent) {
        if (CreateIntegration.isLoaded()) return cn.laowu.mod.compat.create.CreateClientEvents.tooltip(text,primary,highlight,indent);
        var result=new ArrayList<Component>();
        var font=Minecraft.getInstance().font;
        var line=Component.literal(" ".repeat(indent));boolean highlighted=false;int width=font.width(line);
        for(int offset=0;offset<text.length();) {
            int point=text.codePointAt(offset);offset+=Character.charCount(point);
            if(point=='_'){highlighted=!highlighted;continue;}
            String glyph=new String(Character.toChars(point));int advance=font.width(glyph);
            if(point=='\n'||width+advance>200){result.add(line);line=Component.literal(" ".repeat(indent));width=font.width(line);if(point=='\n')continue;}
            line.append(Component.literal(glyph).withStyle(highlighted?highlight:primary));width+=advance;
        }
        result.add(line);
        return result;
    }
    private CatTooltipText() {}
}
