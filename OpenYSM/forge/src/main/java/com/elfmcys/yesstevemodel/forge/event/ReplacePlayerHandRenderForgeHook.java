package com.elfmcys.yesstevemodel.forge.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.event.ReplacePlayerHandRenderEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT)
public final class ReplacePlayerHandRenderForgeHook {

    private ReplacePlayerHandRenderForgeHook() {
    }

    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        if (ReplacePlayerHandRenderEvent.onRenderArm(event.getPlayer(), event.getArm(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight())) {
            event.setCanceled(true);
        }
    }
}
