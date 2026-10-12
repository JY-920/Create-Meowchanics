package cn.laowu.mod.client;

/** Loaded only in optional Curios fixtures; exercises its registered production renderer. */
final class StandaloneGogglesCuriosRegression {
    @SuppressWarnings({"rawtypes","unchecked"})
    static void render(net.minecraft.client.Minecraft mc,net.minecraft.world.item.ItemStack stack) {
        var handler=top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(mc.player).resolve().orElseThrow(
                ()->new AssertionError("Player has no real Curios inventory"));
        var slots=handler.getStacksHandler("head").orElseThrow(
                ()->new AssertionError("Curios head slot unavailable without Create")).getStacks();
        if(slots.getSlots()!=1||!slots.isItemValid(0,stack))throw new AssertionError("Default fixture needs one valid head slot, never a duplicated Create slot");
        var oldHead=mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        var oldCurio=slots.getStackInSlot(0).copy();
        slots.setStackInSlot(0,net.minecraft.world.item.ItemStack.EMPTY);
        if(!slots.insertItem(0,stack.copy(),false).isEmpty())throw new AssertionError("Real Curios inventory rejected goggles");
        mc.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.item.ItemStack.EMPTY);
        if(!CatStatsGoggleOverlay.isWearingCatGoggles(mc.player))throw new AssertionError("Curios-only goggles cannot activate cat stats");
        OptionalCreateClientRegression.verifyStatsPanel(mc);
        var renderer=top.theillusivec4.curios.api.client.CuriosRendererRegistry.getRenderer(stack.getItem()).orElseThrow(
                ()->new AssertionError("Optional Curios goggles renderer not registered"));
        renderer.render(stack,new top.theillusivec4.curios.api.SlotContext("head",mc.player,0,false,true),
                new com.mojang.blaze3d.vertex.PoseStack(),
                (net.minecraft.client.renderer.entity.RenderLayerParent)mc.getEntityRenderDispatcher().getRenderer(mc.player),
                mc.renderBuffers().bufferSource(),net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,0,0,0,0,0,0);
        mc.renderBuffers().bufferSource().endBatch();
        // Exercise the normal player renderer as well, including its Curios layer.
        mc.getEntityRenderDispatcher().getRenderer(mc.player).render(mc.player,0,0,
                new com.mojang.blaze3d.vertex.PoseStack(),mc.renderBuffers().bufferSource(),
                net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
        mc.renderBuffers().bufferSource().endBatch();
        slots.setStackInSlot(0,oldCurio);
        mc.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,oldHead);
        System.out.println("PASS: ACTUAL CURIOS HEAD INSERTION + CURIOS-ONLY STATS + NORMAL PLAYER RENDER PIPELINE");
    }
}
