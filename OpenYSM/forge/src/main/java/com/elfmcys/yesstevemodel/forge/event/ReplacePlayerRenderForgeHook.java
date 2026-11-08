package com.elfmcys.yesstevemodel.forge.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.event.ReplacePlayerRenderEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT)
public final class ReplacePlayerRenderForgeHook {

    private ReplacePlayerRenderForgeHook() {
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (ReplacePlayerRenderEvent.onRenderPlayerPre(event.getEntity(), event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight())) {
            event.setCanceled(true);
        }
    }
}
