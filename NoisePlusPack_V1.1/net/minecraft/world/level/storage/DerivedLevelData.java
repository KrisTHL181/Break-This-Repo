package net.minecraft.world.level.storage;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.CrashReportCategory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.timers.TimerQueue;

public class DerivedLevelData implements ServerLevelData {
   private final WorldData worldData;
   private final ServerLevelData wrapped;

   public DerivedLevelData(WorldData p_78079_, ServerLevelData p_78080_) {
      this.worldData = p_78079_;
      this.wrapped = p_78080_;
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
   public long getDayTime() {
      return this.wrapped.getDayTime();
   }

   @Override
   public String getLevelName() {
      return this.worldData.getLevelName();
   }

   @Override
   public int getClearWeatherTime() {
      return this.wrapped.getClearWeatherTime();
   }

   @Override
   public void setClearWeatherTime(int p_78085_) {
   }

   @Override
   public boolean isThundering() {
      return this.wrapped.isThundering();
   }

   @Override
   public int getThunderTime() {
      return this.wrapped.getThunderTime();
   }

   @Override
   public boolean isRaining() {
      return this.wrapped.isRaining();
   }

   @Override
   public int getRainTime() {
      return this.wrapped.getRainTime();
   }

   @Override
   public GameType getGameType() {
      return this.worldData.getGameType();
   }

   @Override
   public void setGameTime(long p_78087_) {
   }

   @Override
   public void setDayTime(long p_78105_) {
   }

   @Override
   public void setSpawn(LevelData.RespawnData p_428129_) {
      this.wrapped.setSpawn(p_428129_);
   }

   @Override
   public void setThundering(boolean p_78100_) {
   }

   @Override
   public void setThunderTime(int p_78118_) {
   }

   @Override
   public void setRaining(boolean p_78107_) {
   }

   @Override
   public void setRainTime(int p_78121_) {
   }

   @Override
   public void setGameType(GameType p_78089_) {
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
   public boolean isInitialized() {
      return this.wrapped.isInitialized();
   }

   @Override
   public void setInitialized(boolean p_78112_) {
   }

   @Override
   public GameRules getGameRules() {
      return this.worldData.getGameRules();
   }

   @Override
   public Optional<WorldBorder.Settings> getLegacyWorldBorderSettings() {
      return this.wrapped.getLegacyWorldBorderSettings();
   }

   @Override
   public void setLegacyWorldBorderSettings(Optional<WorldBorder.Settings> p_428220_) {
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
   public TimerQueue<MinecraftServer> getScheduledEvents() {
      return this.wrapped.getScheduledEvents();
   }

   @Override
   public int getWanderingTraderSpawnDelay() {
      return 0;
   }

   @Override
   public void setWanderingTraderSpawnDelay(int p_78124_) {
   }

   @Override
   public int getWanderingTraderSpawnChance() {
      return 0;
   }

   @Override
   public void setWanderingTraderSpawnChance(int p_78127_) {
   }

   @Override
   public UUID getWanderingTraderId() {
      return null;
   }

   @Override
   public void setWanderingTraderId(UUID p_78096_) {
   }

   @Override
   public void fillCrashReportCategory(CrashReportCategory p_164852_, LevelHeightAccessor p_164853_) {
      p_164852_.setDetail("Derived", true);
      this.wrapped.fillCrashReportCategory(p_164852_, p_164853_);
   }
}
