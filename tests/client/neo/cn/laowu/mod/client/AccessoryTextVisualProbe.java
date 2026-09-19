package cn.laowu.mod.client;
import cn.laowu.mod.*;
import cn.laowu.mod.accessory.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.ChatFormatting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import org.joml.Matrix4f;
import java.util.*;

/** Uses Minecraft's actual translation parser, not a String.format approximation. */
public final class AccessoryTextVisualProbe {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.get(LaoWuMod.id(id)));}
    private static List<Component> tooltip(String id){
        var stack=item(id);var lines=new ArrayList<Component>();
        lines.add(stack.getHoverName());CatAccessoryTooltip.append(stack,lines);return lines;
    }
    public static void verify(Minecraft mc)throws Exception{
        var previous=net.minecraft.locale.Language.getInstance();
        try{
            for(String language:List.of("en_us","zh_cn")){
                net.minecraft.locale.Language.inject(net.minecraft.client.resources.language.ClientLanguage.loadFrom(
                        mc.getResourceManager(),language.equals("zh_cn")?List.of("en_us","zh_cn"):List.of("en_us"),false));
                int count=0;
                for(String id:CatAccessoryItems.DEFAULTS.keySet()){
                    var lines=tooltip(id.substring(6));
                    for(var line:lines){
                        String text=line.getString();
                        check(!text.contains("%s")&&!text.contains("%1$")&&!text.contains("%%")
                                &&!text.contains("cat_accessory.laowu."),"No placeholder or missing translation: "+language+" "+id+" "+text);
                    }
                    count++;
                }
                check(count==36,"All accessory tooltips checked in "+language);
                var pilot=tooltip("cat_ace_feather").stream().map(Component::getString).toList();
                check(pilot.stream().anyMatch(t->t.contains("0.15%")&&t.contains("80%")),"Dodge percentage and cap are actually substituted");
                for(String guard:List.of("cat_cork_vest","cat_roly_poly")){
                    var durable=item(guard);check(durable.getMaxDamage()==50,"Guard has native 50 durability");
                    durable.setDamageValue(17);var info=new ArrayList<Component>();info.add(durable.getHoverName());CatAccessoryTooltip.append(durable,info);
                    check(info.stream().anyMatch(t->t.getString().contains("33/50")),"Remaining guard durability is visible");
                }
                var bone=item("cat_chew_bone");
                check(bone.getRarity()==Rarity.EPIC&&CatAccessoryRarity.requirements(bone)==4,"Native Epic rarity and Wish cost");
                if(language.equals("zh_cn")){
                    var expected=new LinkedHashMap<String,String>();
                    expected.put("cat_ace_feather","每点速度属性提供0.15%闪避率，免除一次攻击伤害；最高80%。");
                    expected.put("cat_reel_hook","钓鱼钩命中后改为将敌人拉向自身。");
                    expected.put("cat_blue_flame_nozzle","喷射蓝色超级火焰，喷火伤害×1.5。");
                    expected.put("cat_honey_stamp","命中后在敌人脚底留下蜂蜜渍，持续5秒，减速接触到的生物。");
                    expected.put("cat_mouse_plush","攻击血量高于90%的敌人，伤害+35%。");
                    expected.put("cat_spiked_collar","近身受击后反弹实际伤害的50%。");
                    expected.put("cat_roly_poly","收到一次性超过最大生命30%的攻击降低至30%，冷却15s，每次触发消耗1点耐久，与软木护胸互斥。");
                    expected.put("cat_cork_vest","血量跌至35%及以下且存活时，获得最大生命15%的护盾，持续6秒；冷却15s，每次触发消耗1点耐久，与金基咪互斥。");
                    expected.put("cat_tracking_tag","主人在4格范围内时，战斗力属性+30。");
                    expected.put("cat_sorting_pouch","只自动拾取自身9格物品栏内已有同类物品的掉落物；可搭配磁铁。");
                    expected.put("cat_rebirth_ootheca","死亡时分裂为两只幼年蟑螂猫，各继承基础六维属性的50%；");
                    for(var entry:expected.entrySet())check(tooltip(entry.getKey()).stream()
                            .anyMatch(line->line.getString().equals(entry.getValue())),"Exact requested Chinese text "+entry.getKey());
                    check(item("cat_roly_poly").getHoverName().getString().equals("金基咪"),"Canonical item name agrees with incompatibility text");
                    check(Component.translatable("cat_accessory.laowu.effect.heavy_hit_cap","25").getString()
                            .equals("收到一次性超过最大生命25%的攻击降低至25%，冷却15s，每次触发消耗1点耐久，与软木护胸互斥。"),"Repeated indexed substitution respects custom balance");
                    capture(mc,new ArrayList<>(expected.keySet()));
                }
            }
            System.out.println("PASS: all 36 accessory tooltips in both languages; exact eleven requested lines, no raw placeholders, Epic bone and custom amount substitution");
        }finally{net.minecraft.locale.Language.inject(previous);}
    }
    private static void capture(Minecraft mc,List<String> ids)throws Exception{
        var all=new ArrayList<net.minecraft.util.FormattedCharSequence>();
        for(String id:ids){
            var lines=tooltip(id);
            all.addAll(mc.font.split(lines.get(0).copy().withStyle(ChatFormatting.GOLD),360));
            for(int i=1;i<lines.size();i++)all.addAll(mc.font.split(lines.get(i),360));
            all.add(Component.empty().getVisualOrderText());
        }
        int height=all.size()*11+24;
        var pixels=WishAdoptionVisualProbe.capture(mc,768,height*2,"accessory-text36.png",()->{
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,384,height,0,1000,21000),VertexSorting.ORTHOGRAPHIC_Z);
            var g=new GuiGraphics(mc,mc.renderBuffers().bufferSource());g.pose().translate(0,0,-11000);
            g.fill(0,0,384,height,0xFF24282D);
            for(int i=0;i<all.size();i++)g.drawString(mc.font,all.get(i),12,12+i*11,0xFFFFFF,false);
            g.flush();
        });
        int changed=0;
        for(int c:pixels)if(c!=pixels[0])changed++;
        check(changed>5000,"Requested text renders visible GPU glyphs");
    }
    private AccessoryTextVisualProbe(){}
}
