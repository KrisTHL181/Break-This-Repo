package net.minecraft.client.gui.screens.worldselection;

import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.screens.CreateBuffetWorldScreen;
import net.minecraft.client.gui.screens.CreateFlatWorldScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.StructureSet;

public interface PresetEditor {
   Map<Optional<ResourceKey<WorldPreset>>, PresetEditor> EDITORS = Map.of(
      Optional.of(WorldPresets.FLAT),
      (parent, settings) -> {
         ChunkGenerator overworld = settings.selectedDimensions().overworld();
         RegistryAccess registryAccess = settings.worldgenLoadContext();
         HolderGetter<Biome> biomes = registryAccess.lookupOrThrow(Registries.BIOME);
         HolderGetter<StructureSet> structureSets = registryAccess.lookupOrThrow(Registries.STRUCTURE_SET);
         HolderGetter<PlacedFeature> placedFeatures = registryAccess.lookupOrThrow(Registries.PLACED_FEATURE);
         return new CreateFlatWorldScreen(
            parent,
            flatWorldSettings -> parent.getUiState().updateDimensions(flatWorldConfigurator(flatWorldSettings)),
            overworld instanceof FlatLevelSource flatLevelSource
               ? flatLevelSource.settings()
               : FlatLevelGeneratorSettings.getDefault(biomes, structureSets, placedFeatures)
         );
      },
      Optional.of(WorldPresets.SINGLE_BIOME_SURFACE),
      (parent, settings) -> new CreateBuffetWorldScreen(parent, settings, biome -> parent.getUiState().updateDimensions(fixedBiomeConfigurator(biome)))
   );

   Screen createEditScreen(final CreateWorldScreen parent, final WorldCreationContext settings);

   static WorldCreationContext.DimensionsUpdater flatWorldConfigurator(final FlatLevelGeneratorSettings generatorSettings) {
      return (registryAccess, dimensions) -> {
         ChunkGenerator generator = new FlatLevelSource(generatorSettings);
         return dimensions.replaceOverworldGenerator(registryAccess, generator);
      };
   }

   private static WorldCreationContext.DimensionsUpdater fixedBiomeConfigurator(final Holder<Biome> biome) {
      return (registryAccess, dimensions) -> {
         Registry<NoiseGeneratorSettings> noiseGeneratorSettings = registryAccess.lookupOrThrow(Registries.NOISE_SETTINGS);
         Holder<NoiseGeneratorSettings> noiseSettings = noiseGeneratorSettings.getOrThrow(NoiseGeneratorSettings.OVERWORLD);
         BiomeSource biomeSource = new FixedBiomeSource(biome);
         ChunkGenerator generator = new NoiseBasedChunkGenerator(biomeSource, noiseSettings);
         return dimensions.replaceOverworldGenerator(registryAccess, generator);
      };
   }
}
