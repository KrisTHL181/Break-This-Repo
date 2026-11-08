package com.elfmcys.yesstevemodel.client.input;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.client.gui.AnimationRouletteScreen;
import com.elfmcys.yesstevemodel.client.model.ModelAssembly;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.config.ServerConfig;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.util.InputUtil;
import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientRawInputEvent;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import rip.ysm.api.PlatformAPI;
import rip.ysm.api.client.KeyMappingFactory;
import rip.ysm.compat.touhoulittlemaid.TouhouLittleMaidCompat;
import rip.ysm.gui.ModernAnimationRouletteScreen;

public final class AnimationRouletteKey {

    public static final KeyMapping KEY_ROULETTE = KeyMappingFactory.createInGameNone("key.yes_steve_model.animation_roulette.desc", InputConstants.Type.KEYSYM, 90, "key.category.yes_steve_model");

    public static final KeyMapping KEY_LOCK = KeyMappingFactory.createInGameAlt("key.yes_steve_model.lock_roulette.desc", InputConstants.Type.KEYSYM, 76, "key.category.yes_steve_model");

    private AnimationRouletteKey() {
    }

    public static void register() {
        if (PlatformAPI.isServer()) {
            return;
        }
        ClientRawInputEvent.KEY_PRESSED.register((client, keyCode, scanCode, action, modifiers) -> {
            if (YesSteveModel.isAvailable() && InputUtil.isPlayerReady() && action == 1 && InputUtil.isKeyPressed(keyCode, scanCode, KEY_ROULETTE)) {
                if (!NetworkHandler.isClientConnected() || ServerConfig.CAN_SWITCH_MODEL.get()) {
                    if (TouhouLittleMaidCompat.isMaidChatAvailable()) {
                        TouhouLittleMaidCompat.openMaidChat();
                    } else if (Minecraft.getInstance().player != null) {
                        PlayerCapability.get(Minecraft.getInstance().player).ifPresent(cap -> {
                            String modelId = cap.getModelId();
                            ModelAssembly modelAssembly = cap.getModelAssembly();
                            if (modelAssembly != null && !modelAssembly.getModelData().getModelProperties().getExtraAnimation().isEmpty()) {
                                if (Minecraft.getInstance().screen == null) {
                                    if (GeneralConfig.effectiveModernRoulette()) {
                                        Minecraft.getInstance().setScreen(new ModernAnimationRouletteScreen(modelId, modelAssembly, cap));
                                    } else {
                                        Minecraft.getInstance().setScreen(new AnimationRouletteScreen(modelId, modelAssembly, cap));
                                    }
                                } else if (Minecraft.getInstance().screen instanceof AnimationRouletteScreen || Minecraft.getInstance().screen instanceof ModernAnimationRouletteScreen) {
                                    Minecraft.getInstance().setScreen(null);
                                }
                            }
                        });
                    }
                }
            }
            return EventResult.pass();
        });
    }
}