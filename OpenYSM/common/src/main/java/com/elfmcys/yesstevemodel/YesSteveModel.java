package com.elfmcys.yesstevemodel;

import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.config.ModSoundEvents;
import com.elfmcys.yesstevemodel.config.ServerConfig;
import com.elfmcys.yesstevemodel.event.YsmEventBootstrap;
import com.elfmcys.yesstevemodel.util.obfuscate.Keep;
import dev.architectury.platform.Platform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rip.ysm.api.PlatformAPI;
import rip.ysm.api.config.ConfigRegistration;

import java.io.File;
import java.io.IOException;

/**
 * TODO:
 * 默认模型应该就在模组架加载的时候就预加载了
 * 其它模型统统都是进入世界后加载
 */
public class YesSteveModel {
    public static final String MOD_ID = "yes_steve_model";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private YesSteveModel() {
    }

    public static void init() {
        LOGGER.info("Initializing YesSteveModel, platform: " + PlatformAPI.getPlatformName());
        try {
            NativeLibLoader.init();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize native lib", e);
        }
        if (!NativeLibLoader.isAvailable()) {
            LOGGER.error(getErrorMessage());
        } else {
            initConfig();
        }
        YsmEventBootstrap.register();
    }

    private static void initConfig() {
        File oldConfig = Platform.getConfigFolder().resolve("yes_steve_model-common.toml").toFile();
        if (oldConfig.isFile()) {
            File file2 = Platform.getConfigFolder().resolve("yes_steve_model-client.toml").toFile();
            if (!file2.isFile()) {
                oldConfig.renameTo(file2);
            } else {
                oldConfig.delete();
            }
        }
        ConfigRegistration.register(MOD_ID, ModConfig.Type.CLIENT, GeneralConfig.buildSpec());
        ConfigRegistration.register(MOD_ID, ModConfig.Type.SERVER, ServerConfig.buildSpec());
        if (!PlatformAPI.isServer()) {
            ModSoundEvents.REGISTER.register();
        }
    }

    @Keep
    public static boolean isAvailable() {
        return NativeLibLoader.isAvailable();
    }

    public static boolean isOnAndroid() {
        return NativeLibLoader.isOnAndroid();
    }

    @Environment(EnvType.CLIENT)
    public static void sendUnavailableMessage() {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer != null) {
            localPlayer.sendSystemMessage(getUnavailableComponent());
        }
    }

    public static Component getUnavailableComponent() {
        return NativeLibLoader.getErrorComponent();
    }

    public static String getErrorMessage() {
        return NativeLibLoader.getErrorMessage();
    }
}
