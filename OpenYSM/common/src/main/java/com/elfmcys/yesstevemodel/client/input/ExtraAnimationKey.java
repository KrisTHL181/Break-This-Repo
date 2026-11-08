package com.elfmcys.yesstevemodel.client.input;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.client.event.AnimationLockEvent;
import com.elfmcys.yesstevemodel.client.gui.AnimationRouletteScreen;
import com.elfmcys.yesstevemodel.client.model.ModelAssembly;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.C2SPlayAnimationPacket;
import com.elfmcys.yesstevemodel.resource.models.ModelProperties;
import com.elfmcys.yesstevemodel.util.InputUtil;
import com.elfmcys.yesstevemodel.util.data.OrderedStringMap;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientRawInputEvent;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import rip.ysm.api.PlatformAPI;
import rip.ysm.api.client.KeyMappingFactory;

import java.util.List;

public final class ExtraAnimationKey {

    public static final List<KeyMapping> KEY_MAPPINGS = Lists.newArrayList();

    private static boolean initialized = false;

    private ExtraAnimationKey() {
    }

    public static List<KeyMapping> getKeyMappings() {
        if (!initialized) {
            initialized = true;
            if (YesSteveModel.isAvailable()) {
                for (int i = 0; i <= 7; i++) {
                    KeyMapping eventMapping = KeyMappingFactory.createInGameNone(String.format("key.yes_steve_model.extra_animation.%d.desc", Integer.valueOf(i)), InputConstants.Type.KEYSYM, -1, "key.category.yes_steve_model");
                    KEY_MAPPINGS.add(eventMapping);
                }
            }
        }
        return KEY_MAPPINGS;
    }

    public static void register() {
        if (PlatformAPI.isServer()) {
            return;
        }
        ClientRawInputEvent.KEY_PRESSED.register((client, keyCode, scanCode, action, modifiers) -> {
            onKeyInput(action, keyCode, scanCode);
            return EventResult.pass();
        });
    }

    private static void onKeyInput(int action, int keyCode, int scanCode) {
        if (!YesSteveModel.isAvailable() || !InputUtil.isPlayerReady()) {
            return;
        }
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        for (KeyMapping eventMapping : KEY_MAPPINGS) {
            if (action == 1 && InputUtil.isKeyPressed(keyCode, scanCode, eventMapping) && localPlayer != null && !AnimationLockEvent.isPlayerMoving(localPlayer)) {
                PlayerCapability.get(localPlayer).ifPresent(cap -> {
                    ModelAssembly modelAssembly = cap.getModelAssembly();
                    int index = KEY_MAPPINGS.indexOf(eventMapping);
                    ModelProperties modelProperties = modelAssembly.getModelData().getModelProperties();
                    OrderedStringMap<String, String> map = modelProperties.getExtraAnimation();
                    if (map.size() > index) {
                        String rouletteKey = map.getKeyAt(index);
                        if ("#return".equals(rouletteKey)) {
                            if (NetworkHandler.isClientConnected()) {
                                NetworkHandler.sendToServer(C2SPlayAnimationPacket.createDefault());
                            } else {
                                cap.requestModelSwitch(StringPool.EMPTY);
                            }
                            return;
                        }
                        if (rouletteKey.startsWith("#") && modelProperties.getExtraAnimationClassify().containsKey(rouletteKey.substring(1))) {
                            AnimationRouletteScreen.setInitialSubmenu(rouletteKey.substring(1));
                            Minecraft.getInstance().setScreen(new AnimationRouletteScreen(modelProperties.getExtraAnimationButtons(), modelProperties.getExtraAnimationClassify(), modelAssembly, cap));
                            return;
                        }
                        if (NetworkHandler.isClientConnected()) {
                            NetworkHandler.sendToServer(new C2SPlayAnimationPacket(index, StringPool.EMPTY));
                        } else {
                            cap.requestModelSwitch(rouletteKey);
                        }
                    }
                });
                return;
            }
        }
    }
}