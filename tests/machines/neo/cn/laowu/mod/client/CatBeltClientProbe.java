package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.create.CatBeltStyle;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.kinetics.belt.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.client.renderer.RenderType;
import java.lang.reflect.Proxy;
import java.util.Arrays;

/** Real baked native belt model and native BE, with a world-view adapter for model data. */
public final class CatBeltClientProbe {
    public static void verify(Minecraft mc) {
        if(mc.level!=null) {
            int[] refreshes={0};
            var predicted=new BeltBlockEntity(AllBlockEntityTypes.BELT.get(),BlockPos.ZERO,AllBlocks.BELT.getDefaultState()) {
                @Override public void requestModelDataUpdate(){refreshes[0]++;super.requestModelDataUpdate();}
            };
            predicted.setLevel(mc.level);
            predicted.casing=BeltBlockEntity.CasingType.ANDESITE;
            ((CatBeltStyle)predicted).laowu$setCatBelt(true);
            CatMachinesClientProbe.check(refreshes[0]>0,"Predicted cat style must invalidate client model data even with unchanged native casing");
            int before=refreshes[0];
            predicted.setCasingType(BeltBlockEntity.CasingType.ANDESITE);
            CatMachinesClientProbe.check(refreshes[0]>before,"Predicted andesite overwrite must immediately invalidate cat model data");
        }
        var state=AllBlocks.BELT.getDefaultState().setValue(BeltBlock.CASING,true);
        var be=new BeltBlockEntity(AllBlockEntityTypes.BELT.get(),BlockPos.ZERO,state);
        be.casing=BeltBlockEntity.CasingType.ANDESITE;
        ((CatBeltStyle)be).laowu$setCatBelt(true);
        var world=(BlockAndTintGetter)Proxy.newProxyInstance(CatBeltClientProbe.class.getClassLoader(),
                new Class<?>[]{BlockAndTintGetter.class},(p,m,a)->switch(m.getName()) {
                    case "getBlockEntity" -> be;
                    case "getBlockState" -> state;
                    case "getFluidState" -> state.getFluidState();
                    case "getHeight" -> 256;
                    case "getMinBuildHeight" -> 0;
                    case "getShade" -> 1F;
                    case "getBlockTint" -> 0xffffff;
                    case "toString" -> "CatBeltModelFixture";
                    default -> null;
                });
        var model=mc.getBlockRenderer().getBlockModel(state);
        var data=model.getModelData(world,BlockPos.ZERO,state,be.getModelData());
        var cat=model.getQuads(state,Direction.NORTH,RandomSource.create(1),data,RenderType.solid());
        if(cat.isEmpty())cat=model.getQuads(state,null,RandomSource.create(1),data,RenderType.solid());
        CatMachinesClientProbe.check(!cat.isEmpty(),"Native encased belt must have baked casing geometry");
        boolean custom=cat.stream().anyMatch(q->q.getSprite().contents().name().equals(LaoWuMod.id("block/cat_belt_casing")));
        CatMachinesClientProbe.check(custom,"Cat belt casing must use the supplied atlas, not Create's original");
        ((CatBeltStyle)be).laowu$setCatBelt(false);
        var plainData=model.getModelData(world,BlockPos.ZERO,state,be.getModelData());
        var plain=model.getQuads(state,null,RandomSource.create(1),plainData,RenderType.solid());
        CatMachinesClientProbe.check(plain.stream().noneMatch(q->q.getSprite().contents().name().equals(LaoWuMod.id("block/cat_belt_casing"))),
                "Ordinary Create belts must retain their original textures");
        System.out.println("PASS: cat belt uses custom atlas and native belts remain unchanged");
    }
}
