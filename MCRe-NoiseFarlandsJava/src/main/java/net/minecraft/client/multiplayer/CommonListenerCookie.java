package net.minecraft.client.multiplayer;

import com.mojang.authlib.GameProfile;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerLinks;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record CommonListenerCookie(
    LevelLoadTracker levelLoadTracker,
    GameProfile localGameProfile,
    WorldSessionTelemetryManager telemetryManager,
    RegistryAccess.Frozen receivedRegistries,
    FeatureFlagSet enabledFeatures,
    @Nullable String serverBrand,
    @Nullable ServerData serverData,
    @Nullable Screen postDisconnectScreen,
    Map<Identifier, byte[]> serverCookies,
    ChatComponent.@Nullable State chatState,
    Map<String, String> customReportDetails,
    ServerLinks serverLinks,
    Map<UUID, PlayerInfo> seenPlayers,
    boolean seenInsecureChatWarning
) {
}