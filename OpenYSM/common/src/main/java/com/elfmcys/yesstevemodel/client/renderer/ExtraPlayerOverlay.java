package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.client.gui.ExtraPlayerRenderScreen;
import com.elfmcys.yesstevemodel.config.ExtraPlayerRenderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import rip.ysm.api.client.HudOverlay;

public class ExtraPlayerOverlay implements HudOverlay {
    @Override
    public void render(GuiGraphics guiGraphics, Font font, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft;
        LocalPlayer localPlayer;
        if (ExtraPlayerRenderConfig.DISABLE_PLAYER_RENDER.get() || (localPlayer = (minecraft = Minecraft.getInstance()).player) == null || (minecraft.screen instanceof ExtraPlayerRenderScreen)) {
            return;
        }
        if (ExtraPlayerRenderConfig.DISABLE_PLAYER_RENDER_THIRD_PERSON.get() && minecraft.options != null && !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        ModelPreviewRenderer.renderPlayerOverlay(guiGraphics, localPlayer, ExtraPlayerRenderConfig.PLAYER_POS_X.get(), ExtraPlayerRenderConfig.PLAYER_POS_Y.get(), ExtraPlayerRenderConfig.PLAYER_SCALE.get().floatValue(), ExtraPlayerRenderConfig.PLAYER_YAW_OFFSET.get().floatValue(), -500, minecraft.getFrameTime());
    }
}