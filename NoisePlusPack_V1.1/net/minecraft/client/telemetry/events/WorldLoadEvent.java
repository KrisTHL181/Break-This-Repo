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

   public WorldLoadEvent(@Nullable String p_286661_) {
      this.minigameName = p_286661_;
   }

   public void addProperties(TelemetryPropertyMap.Builder p_261869_) {
      if (this.serverBrand != null) {
         p_261869_.put(TelemetryProperty.SERVER_MODDED, !this.serverBrand.equals("vanilla"));
      }

      p_261869_.put(TelemetryProperty.SERVER_TYPE, this.getServerType());
   }

   private TelemetryProperty.ServerType getServerType() {
      ServerData serverdata = Minecraft.getInstance().getCurrentServer();
      if (serverdata != null && serverdata.isRealm()) {
         return TelemetryProperty.ServerType.REALM;
      } else {
         return Minecraft.getInstance().hasSingleplayerServer() ? TelemetryProperty.ServerType.LOCAL : TelemetryProperty.ServerType.OTHER;
      }
   }

   public boolean send(TelemetryEventSender p_263325_) {
      if (!this.eventSent && this.gameMode != null && this.serverBrand != null) {
         this.eventSent = true;
         p_263325_.send(TelemetryEventType.WORLD_LOADED, p_286185_ -> {
            p_286185_.put(TelemetryProperty.GAME_MODE, this.gameMode);
            if (this.minigameName != null) {
               p_286185_.put(TelemetryProperty.REALMS_MAP_CONTENT, this.minigameName);
            }
         });
         return true;
      } else {
         return false;
      }
   }

   public void setGameMode(GameType p_261852_, boolean p_261831_) {
      this.gameMode = switch (p_261852_) {
         case SURVIVAL -> p_261831_ ? TelemetryProperty.GameMode.HARDCORE : TelemetryProperty.GameMode.SURVIVAL;
         case CREATIVE -> TelemetryProperty.GameMode.CREATIVE;
         case ADVENTURE -> TelemetryProperty.GameMode.ADVENTURE;
         case SPECTATOR -> TelemetryProperty.GameMode.SPECTATOR;
      };
   }

   public void setServerBrand(String p_261964_) {
      this.serverBrand = p_261964_;
   }
}
