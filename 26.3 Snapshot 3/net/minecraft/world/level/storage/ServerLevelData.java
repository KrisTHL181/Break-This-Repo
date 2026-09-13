package net.minecraft.world.level.storage;

import java.util.Locale;
import net.minecraft.CrashReportCategory;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelHeightAccessor;

public interface ServerLevelData extends WritableLevelData {
   String getLevelName();

   @Override
   default void fillCrashReportCategory(final CrashReportCategory category, final LevelHeightAccessor levelHeightAccessor) {
      WritableLevelData.super.fillCrashReportCategory(category, levelHeightAccessor);
      category.setDetail("Level name", this::getLevelName);
      category.setDetail(
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
   }

   GameType getGameType();

   boolean isInitialized();

   void setInitialized(boolean initialized);

   boolean isAllowCommands();

   void setAllowCommands(boolean allowCommands);

   void setGameType(GameType gameType);

   void setGameTime(final long time);
}
