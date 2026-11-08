package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.event.api.SpecialPlayerRenderEvent;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import dev.architectury.event.EventResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import rip.ysm.api.PlatformAPI;

import java.util.Map;

public class PlayerSkinTextureManager {

    private static final ResourceLocation STEVE_SKIN = new ResourceLocation("textures/entity/player/wide/steve.png");

    private static final ResourceLocation ALEX_SKIN = new ResourceLocation("textures/entity/player/slim/alex.png");

    private static final String STEVE_TEXTURE_ID = "misc/2_steve";

    private static final String ALEX_TEXTURE_ID = "misc/1_alex";

    private PlayerSkinTextureManager() {
    }

    public static void register() {
        if (PlatformAPI.isServer()) {
            return;
        }
        SpecialPlayerRenderEvent.EVENT.register(PlayerSkinTextureManager::onRenderTexture);
    }

    private static EventResult onRenderTexture(SpecialPlayerRenderEvent event) {
        ResourceLocation location;
        if (!YesSteveModel.isAvailable()) {
            return EventResult.pass();
        }
        Player player = event.getPlayer();
        if (isDefaultSkin(event.getModelId()) && (player instanceof AbstractClientPlayer abstractClientPlayer)) {
            Minecraft minecraft = Minecraft.getInstance();
            Map insecureSkinInformation = minecraft.getSkinManager().getInsecureSkinInformation(abstractClientPlayer.getGameProfile());
            if (insecureSkinInformation.containsKey(MinecraftProfileTexture.Type.SKIN)) {
                location = minecraft.getSkinManager().registerTexture((MinecraftProfileTexture) insecureSkinInformation.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN);
            } else {
                location = getSkinTexture(event.getModelId());
            }
            event.setTextureLocation(location);
        }
        return EventResult.pass();
    }

    private static boolean isDefaultSkin(String str) {
        return str.equals(STEVE_TEXTURE_ID) || str.equals(ALEX_TEXTURE_ID);
    }

    private static ResourceLocation getSkinTexture(String str) {
        return str.equals(STEVE_TEXTURE_ID) ? STEVE_SKIN : ALEX_SKIN;
    }
}
