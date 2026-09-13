package net.minecraft.client.telemetry.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.telemetry.TelemetryEventSender;
import net.minecraft.client.telemetry.TelemetryEventType;
import net.minecraft.client.telemetry.TelemetryProperty;
import net.minecraft.client.telemetry.TelemetryPropertyMap;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class WorldLoadEvent {
    private boolean eventSent;
    private TelemetryProperty.@Nullable GameMode gameMode;
    private @Nullable String serverBrand;
    private final @Nullable String minigameName;

    public WorldLoadEvent(final @Nullable String minigameName) {
        this.minigameName = minigameName;
    }

    public void addProperties(final TelemetryPropertyMap.Builder properties) {
        if (this.serverBrand != null) {
            properties.put(TelemetryProperty.SERVER_MODDED, !this.serverBrand.equals("vanilla"));
        }

        properties.put(TelemetryProperty.SERVER_TYPE, this.getServerType());
    }

    private TelemetryProperty.ServerType getServerType() {
        ServerData server = Minecraft.getInstance().getCurrentServer();
        if (server != null && server.isRealm()) {
            return TelemetryProperty.ServerType.REALM;
        } else {
            return Minecraft.getInstance().hasSingleplayerServer() ? TelemetryProperty.ServerType.LOCAL : TelemetryProperty.ServerType.OTHER;
        }
    }

    public boolean send(final TelemetryEventSender eventSender, final boolean lastChance) {
        if (!this.eventSent && this.gameMode != null && (this.serverBrand != null || lastChance)) {
            this.eventSent = true;
            eventSender.send(TelemetryEventType.WORLD_LOADED, properties -> {
                properties.put(TelemetryProperty.GAME_MODE, this.gameMode);
                if (this.minigameName != null) {
                    properties.put(TelemetryProperty.REALMS_MAP_CONTENT, this.minigameName);
                }
            });
            return true;
        } else {
            return false;
        }
    }

    public boolean wasSent() {
        return this.eventSent;
    }

    public void setGameMode(final GameType type, final boolean hardcore) {
        this.gameMode = switch (type) {
            case SURVIVAL -> hardcore ? TelemetryProperty.GameMode.HARDCORE : TelemetryProperty.GameMode.SURVIVAL;
            case CREATIVE -> TelemetryProperty.GameMode.CREATIVE;
            case ADVENTURE -> TelemetryProperty.GameMode.ADVENTURE;
            case SPECTATOR -> TelemetryProperty.GameMode.SPECTATOR;
        };
    }

    public void setServerBrand(final String serverBrand) {
        this.serverBrand = serverBrand;
    }
}