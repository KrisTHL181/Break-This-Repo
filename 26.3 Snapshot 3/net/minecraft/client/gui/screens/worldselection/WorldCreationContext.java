package net.minecraft.client.gui.screens.worldselection;

import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;

public record WorldCreationContext(
   WorldOptions options,
   Registry<LevelStem> datapackDimensions,
   WorldDimensions selectedDimensions,
   LayeredRegistryAccess<RegistryLayer> worldgenRegistries,
   ReloadableServerResources dataPackResources,
   WorldDataConfiguration dataConfiguration,
   InitialWorldCreationOptions initialWorldCreationOptions
) {
   public WorldCreationContext(
      final WorldGenSettings worldGenSettings,
      final LayeredRegistryAccess<RegistryLayer> loadedRegistries,
      final ReloadableServerResources dataPackResources,
      final WorldDataConfiguration dataConfiguration
   ) {
      this(
         worldGenSettings.options(),
         worldGenSettings.dimensions(),
         loadedRegistries,
         dataPackResources,
         dataConfiguration,
         new InitialWorldCreationOptions(WorldCreationUiState.SelectedGameMode.SURVIVAL, GameRuleMap.of(), null)
      );
   }

   public WorldCreationContext(
      final WorldOptions worldOptions,
      final WorldDimensions worldDimensions,
      final LayeredRegistryAccess<RegistryLayer> loadedRegistries,
      final ReloadableServerResources dataPackResources,
      final WorldDataConfiguration dataConfiguration,
      final InitialWorldCreationOptions initialWorldCreationOptions
   ) {
      this(
         worldOptions,
         loadedRegistries.getLayer(RegistryLayer.DIMENSIONS).lookupOrThrow(Registries.LEVEL_STEM),
         worldDimensions,
         loadedRegistries.replaceFrom(RegistryLayer.DIMENSIONS),
         dataPackResources,
         dataConfiguration,
         initialWorldCreationOptions
      );
   }

   public WorldCreationContext withSettings(final WorldOptions options, final WorldDimensions dimensions) {
      return new WorldCreationContext(
         options,
         this.datapackDimensions,
         dimensions,
         this.worldgenRegistries,
         this.dataPackResources,
         this.dataConfiguration,
         this.initialWorldCreationOptions
      );
   }

   public WorldCreationContext withOptions(final WorldCreationContext.OptionsModifier modifier) {
      return new WorldCreationContext(
         modifier.apply(this.options),
         this.datapackDimensions,
         this.selectedDimensions,
         this.worldgenRegistries,
         this.dataPackResources,
         this.dataConfiguration,
         this.initialWorldCreationOptions
      );
   }

   public WorldCreationContext withDimensions(final WorldCreationContext.DimensionsUpdater modifier) {
      return new WorldCreationContext(
         this.options,
         this.datapackDimensions,
         modifier.apply(this.worldgenLoadContext(), this.selectedDimensions),
         this.worldgenRegistries,
         this.dataPackResources,
         this.dataConfiguration,
         this.initialWorldCreationOptions
      );
   }

   public RegistryAccess.Frozen worldgenLoadContext() {
      return this.worldgenRegistries.compositeAccess();
   }

   public void validate() {
      for (LevelStem stem : this.datapackDimensions()) {
         stem.generator().validate();
      }
   }

   @FunctionalInterface
   public interface DimensionsUpdater extends BiFunction<RegistryAccess.Frozen, WorldDimensions, WorldDimensions> {
   }

   public interface OptionsModifier extends UnaryOperator<WorldOptions> {
   }
}
