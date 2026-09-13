package net.minecraft.world.level.storage;

import com.mojang.serialization.Lifecycle;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.CrashReportCategory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import org.jspecify.annotations.Nullable;

public interface WorldData {
   int ANVIL_VERSION_ID = 19133;
   int MCREGION_VERSION_ID = 19132;

   WorldDataConfiguration getDataConfiguration();

   void setDataConfiguration(final WorldDataConfiguration dataConfiguration);

   boolean wasModded();

   Set<String> getKnownServerBrands();

   Set<String> getRemovedFeatureFlags();

   void setModdedInfo(final String serverBrand, final boolean isModded);

   default void fillCrashReportCategory(final CrashReportCategory category) {
      category.setDetail("Known server brands", () -> String.join(", ", this.getKnownServerBrands()));
      category.setDetail("Removed feature flags", () -> String.join(", ", this.getRemovedFeatureFlags()));
      category.setDetail("Level was modded", () -> Boolean.toString(this.wasModded()));
      category.setDetail("Level storage version", () -> {
         int version = this.getVersion();
         return String.format(Locale.ROOT, "0x%05X - %s", version, this.getStorageVersionName(version));
      });
   }

   default String getStorageVersionName(final int version) {
      return switch (version) {
         case 19132 -> "McRegion";
         case 19133 -> "Anvil";
         default -> "Unknown?";
      };
   }

   ServerLevelData overworldData();

   LevelSettings getLevelSettings();

   CompoundTag createTag(@Nullable UUID singlePlayerUUID);

   boolean isHardcore();

   int getVersion();

   String getLevelName();

   GameType getGameType();

   void setGameType(GameType gameType);

   boolean isAllowCommands();

   void setAllowCommands(final boolean allowCommands);

   Difficulty getDifficulty();

   void setDifficulty(final Difficulty difficulty);

   boolean isDifficultyLocked();

   void setDifficultyLocked(final boolean difficultyLocked);

   @Nullable UUID getSinglePlayerUUID();

   boolean isFlatWorld();

   boolean isDebugWorld();

   Lifecycle worldGenSettingsLifecycle();

   default FeatureFlagSet enabledFeatures() {
      return this.getDataConfiguration().enabledFeatures();
   }
}
