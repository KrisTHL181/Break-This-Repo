package net.minecraft.world.level.storage;

import net.minecraft.CrashReportCategory;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelHeightAccessor;

public class DerivedLevelData implements ServerLevelData {
   private final WorldData worldData;
   private final ServerLevelData wrapped;

   public DerivedLevelData(final WorldData worldData, final ServerLevelData wrapped) {
      this.worldData = worldData;
      this.wrapped = wrapped;
   }

   @Override
   public LevelData.RespawnData getRespawnData() {
      return this.wrapped.getRespawnData();
   }

   @Override
   public long getGameTime() {
      return this.wrapped.getGameTime();
   }

   @Override
   public String getLevelName() {
      return this.worldData.getLevelName();
   }

   @Override
   public GameType getGameType() {
      return this.worldData.getGameType();
   }

   @Override
   public void setGameTime(final long time) {
   }

   @Override
   public void setSpawn(final LevelData.RespawnData respawnData) {
      this.wrapped.setSpawn(respawnData);
   }

   @Override
   public void setGameType(final GameType gameType) {
   }

   @Override
   public boolean isHardcore() {
      return this.worldData.isHardcore();
   }

   @Override
   public boolean isAllowCommands() {
      return this.worldData.isAllowCommands();
   }

   @Override
   public void setAllowCommands(final boolean allowCommands) {
   }

   @Override
   public boolean isInitialized() {
      return this.wrapped.isInitialized();
   }

   @Override
   public void setInitialized(final boolean initialized) {
   }

   @Override
   public Difficulty getDifficulty() {
      return this.worldData.getDifficulty();
   }

   @Override
   public boolean isDifficultyLocked() {
      return this.worldData.isDifficultyLocked();
   }

   @Override
   public void fillCrashReportCategory(final CrashReportCategory category, final LevelHeightAccessor levelHeightAccessor) {
      category.setDetail("Derived", true);
      this.wrapped.fillCrashReportCategory(category, levelHeightAccessor);
   }
}
