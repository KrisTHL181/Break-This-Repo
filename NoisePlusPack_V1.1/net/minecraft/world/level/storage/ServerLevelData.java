package net.minecraft.world.level.storage;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.CrashReportCategory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.timers.TimerQueue;
import org.jspecify.annotations.Nullable;

public interface ServerLevelData extends WritableLevelData {
   String getLevelName();

   void setThundering(boolean var1);

   int getRainTime();

   void setRainTime(int var1);

   void setThunderTime(int var1);

   int getThunderTime();

   @Override
   default void fillCrashReportCategory(CrashReportCategory p_164976_, LevelHeightAccessor p_164977_) {
      WritableLevelData.super.fillCrashReportCategory(p_164976_, p_164977_);
      p_164976_.setDetail("Level name", this::getLevelName);
      p_164976_.setDetail(
         "Level game mode",
         () -> String.format(
            Locale.ROOT,
            "Game mode: %s (ID %d). Hardcore: %b. Commands: %b",
            this.getGameType().getName(),
            this.getGameType().getId(),
            this.isHardcore(),
            this.isAllowCommands()
         )
      );
      p_164976_.setDetail(
         "Level weather",
         () -> String.format(
            Locale.ROOT,
            "Rain time: %d (now: %b), thunder time: %d (now: %b)",
            this.getRainTime(),
            this.isRaining(),
            this.getThunderTime(),
            this.isThundering()
         )
      );
   }

   int getClearWeatherTime();

   void setClearWeatherTime(int var1);

   int getWanderingTraderSpawnDelay();

   void setWanderingTraderSpawnDelay(int var1);

   int getWanderingTraderSpawnChance();

   void setWanderingTraderSpawnChance(int var1);

   @Nullable UUID getWanderingTraderId();

   void setWanderingTraderId(UUID var1);

   GameType getGameType();

   @Deprecated
   Optional<WorldBorder.Settings> getLegacyWorldBorderSettings();

   @Deprecated
   void setLegacyWorldBorderSettings(Optional<WorldBorder.Settings> var1);

   boolean isInitialized();

   void setInitialized(boolean var1);

   boolean isAllowCommands();

   void setGameType(GameType var1);

   TimerQueue<MinecraftServer> getScheduledEvents();

   void setGameTime(long var1);

   void setDayTime(long var1);

   GameRules getGameRules();
}
