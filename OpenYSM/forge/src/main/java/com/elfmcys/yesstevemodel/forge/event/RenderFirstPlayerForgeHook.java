package com.elfmcys.yesstevemodel.forge.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.event.RenderFirstPlayerBackground;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT)
public final class RenderFirstPlayerForgeHook {

    private RenderFirstPlayerForgeHook() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            RenderFirstPlayerBackground.resetFrame();
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        RenderFirstPlayerBackground.onRenderHand(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), event.getPartialTick());
    }
}
