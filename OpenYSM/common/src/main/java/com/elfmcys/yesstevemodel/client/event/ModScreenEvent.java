package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.client.gui.DownloadScreen;
import com.elfmcys.yesstevemodel.client.gui.PlayerModelScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@Environment(EnvType.CLIENT)
public class ModScreenEvent {

    public static final String IMC_METHOD = "DownloadScreen";

    @Nullable
    private static Screen receivedScreen;

    private ModScreenEvent() {
    }

    public static void setReceivedScreen(@Nullable Screen screen) {
        receivedScreen = screen;
    }

    public static void openScreen(PlayerModelScreen modelScreen) {
        Minecraft.getInstance().setScreen(Objects.requireNonNullElseGet(receivedScreen, () -> {
            return new DownloadScreen(modelScreen);
        }));
    }
}
