package com.elfmcys.yesstevemodel.client.input;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.gui.ExtraPlayerRenderScreen;
import com.elfmcys.yesstevemodel.util.InputUtil;
import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientRawInputEvent;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import rip.ysm.api.PlatformAPI;
import rip.ysm.api.client.KeyMappingFactory;

public final class ExtraPlayerRenderKey {

    public static final KeyMapping KEY_MAPPING = KeyMappingFactory.createInGameAlt("key.yes_steve_model.open_extra_player_render.desc", InputConstants.Type.KEYSYM, 80, "key.category.yes_steve_model");

    private ExtraPlayerRenderKey() {
    }

    public static void register() {
        if (PlatformAPI.isServer()) {
            return;
        }
        ClientRawInputEvent.KEY_PRESSED.register((client, keyCode, scanCode, action, modifiers) -> {
            if (YesSteveModel.isAvailable() && InputUtil.isPlayerReady() && action == 1 && InputUtil.isKeyPressed(keyCode, scanCode, KEY_MAPPING)) {
                Minecraft.getInstance().setScreen(new ExtraPlayerRenderScreen());
            }
            return EventResult.pass();
        });
    }
}