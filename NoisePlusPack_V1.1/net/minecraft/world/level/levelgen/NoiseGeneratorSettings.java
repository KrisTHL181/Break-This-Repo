package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public record NoiseGeneratorSettings(
   NoiseSettings noiseSettings,
   BlockState defaultBlock,
   BlockState defaultFluid,
   NoiseRouter noiseRouter,
   SurfaceRules.RuleSource surfaceRule,
   List<Climate.ParameterPoint> spawnTarget,
   int seaLevel,
   @Deprecated boolean disableMobGeneration,
   boolean aquifersEnabled,
   boolean oreVeinsEnabled,
   boolean useLegacyRandomSource
) {
   public static final Codec<NoiseGeneratorSettings> DIRECT_CODEC = RecordCodecBuilder.create(
      p_64475_ -> p_64475_.group(
            NoiseSettings.CODEC.fieldOf("noise").forGetter(NoiseGeneratorSettings::noiseSettings),
            BlockState.CODEC.fieldOf("default_block").forGetter(NoiseGeneratorSettings::defaultBlock),
            BlockState.CODEC.fieldOf("default_fluid").forGetter(NoiseGeneratorSettings::defaultFluid),
            NoiseRouter.CODEC.fieldOf("noise_router").forGetter(NoiseGeneratorSettings::noiseRouter),
            SurfaceRules.RuleSource.CODEC.fieldOf("surface_rule").forGetter(NoiseGeneratorSettings::surfaceRule),
            Climate.ParameterPoint.CODEC.listOf().fieldOf("spawn_target").forGetter(NoiseGeneratorSettings::spawnTarget),
            Codec.INT.fieldOf("sea_level").forGetter(NoiseGeneratorSettings::seaLevel),
            Codec.BOOL.fieldOf("disable_mob_generation").forGetter(NoiseGeneratorSettings::disableMobGeneration),
            Codec.BOOL.fieldOf("aquifers_enabled").forGetter(NoiseGeneratorSettings::isAquifersEnabled),
            Codec.BOOL.fieldOf("ore_veins_enabled").forGetter(NoiseGeneratorSettings::oreVeinsEnabled),
            Codec.BOOL.fieldOf("legacy_random_source").forGetter(NoiseGeneratorSettings::useLegacyRandomSource)
         )
         .apply(p_64475_, NoiseGeneratorSettings::new)
   );
   public static final Codec<Holder<NoiseGeneratorSettings>> CODEC = RegistryFileCodec.create(Registries.NOISE_SETTINGS, DIRECT_CODEC);
   public static final ResourceKey<NoiseGeneratorSettings> OVERWORLD = ResourceKey.create(
      Registries.NOISE_SETTINGS, Identifier.withDefaultNamespace("overworld")
   );
   public static final ResourceKey<NoiseGeneratorSettings> LARGE_BIOMES = ResourceKey.create(
      Registries.NOISE_SETTINGS, Identifier.withDefaultNamespace("large_biomes")
   );
   public static final ResourceKey<NoiseGeneratorSettings> AMPLIFIED = ResourceKey.create(
      Registries.NOISE_SETTINGS, Identifier.withDefaultNamespace("amplified")
   );
   public static final ResourceKey<NoiseGeneratorSettings> NETHER = ResourceKey.create(Registries.NOISE_SETTINGS, Identifier.withDefaultNamespace("nether"));
   public static final ResourceKey<NoiseGeneratorSettings> END = ResourceKey.create(Registries.NOISE_SETTINGS, Identifier.withDefaultNamespace("end"));
   public static final ResourceKey<NoiseGeneratorSettings> CAVES = ResourceKey.create(Registries.NOISE_SETTINGS, Identifier.withDefaultNamespace("caves"));
   public static final ResourceKey<NoiseGeneratorSettings> FLOATING_ISLANDS = ResourceKey.create(
      Registries.NOISE_SETTINGS, Identifier.withDefaultNamespace("floating_islands")
   );

   public boolean isAquifersEnabled() {
      return this.aquifersEnabled && !SharedConstants.DEBUG_DISABLE_AQUIFERS;
   }

   public boolean oreVeinsEnabled() {
      return this.oreVeinsEnabled && !SharedConstants.DEBUG_DISABLE_ORE_VEINS;
   }

   public WorldgenRandom.Algorithm getRandomSource() {
      return this.useLegacyRandomSource ? WorldgenRandom.Algorithm.LEGACY : WorldgenRandom.Algorithm.XOROSHIRO;
   }

   public static void bootstrap(BootstrapContext<NoiseGeneratorSettings> p_334698_) {
      p_334698_.register(OVERWORLD, overworld(p_334698_, false, false));
      p_334698_.register(LARGE_BIOMES, overworld(p_334698_, false, true));
      p_334698_.register(AMPLIFIED, overworld(p_334698_, true, false));
      p_334698_.register(NETHER, nether(p_334698_));
      p_334698_.register(END, end(p_334698_));
      p_334698_.register(CAVES, caves(p_334698_));
      p_334698_.register(FLOATING_ISLANDS, floatingIslands(p_334698_));
   }

   private static NoiseGeneratorSettings end(BootstrapContext<?> p_330746_) {
      return new NoiseGeneratorSettings(
         NoiseSettings.END_NOISE_SETTINGS,
         Blocks.END_STONE.defaultBlockState(),
         Blocks.AIR.defaultBlockState(),
         NoiseRouterData.end(p_330746_.lookup(Registries.DENSITY_FUNCTION)),
         SurfaceRuleData.end(),
         List.of(),
         0,
         true,
         false,
         false,
         true
      );
   }

   private static NoiseGeneratorSettings nether(BootstrapContext<?> p_329279_) {
      return new NoiseGeneratorSettings(
         NoiseSettings.NETHER_NOISE_SETTINGS,
         Blocks.NETHERRACK.defaultBlockState(),
         Blocks.LAVA.defaultBlockState(),
         NoiseRouterData.nether(p_329279_.lookup(Registries.DENSITY_FUNCTION), p_329279_.lookup(Registries.NOISE)),
         SurfaceRuleData.nether(),
         List.of(),
         32,
         false,
         false,
         false,
         true
      );
   }

   private static NoiseGeneratorSettings overworld(BootstrapContext<?> p_332236_, boolean p_256427_, boolean p_256318_) {
      return new NoiseGeneratorSettings(
         NoiseSettings.OVERWORLD_NOISE_SETTINGS,
         Blocks.STONE.defaultBlockState(),
         Blocks.WATER.defaultBlockState(),
         NoiseRouterData.overworld(p_332236_.lookup(Registries.DENSITY_FUNCTION), p_332236_.lookup(Registries.NOISE), p_256318_, p_256427_),
         SurfaceRuleData.overworld(),
         new OverworldBiomeBuilder().spawnTarget(),
         63,
         false,
         true,
         true,
         false
      );
   }

   private static NoiseGeneratorSettings caves(BootstrapContext<?> p_330713_) {
      return new NoiseGeneratorSettings(
         NoiseSettings.CAVES_NOISE_SETTINGS,
         Blocks.STONE.defaultBlockState(),
         Blocks.WATER.defaultBlockState(),
         NoiseRouterData.caves(p_330713_.lookup(Registries.DENSITY_FUNCTION), p_330713_.lookup(Registries.NOISE)),
         SurfaceRuleData.overworldLike(false, true, true),
         List.of(),
         32,
         false,
         false,
         false,
         true
      );
   }

   private static NoiseGeneratorSettings floatingIslands(BootstrapContext<?> p_335454_) {
      return new NoiseGeneratorSettings(
         NoiseSettings.FLOATING_ISLANDS_NOISE_SETTINGS,
         Blocks.STONE.defaultBlockState(),
         Blocks.WATER.defaultBlockState(),
         NoiseRouterData.floatingIslands(p_335454_.lookup(Registries.DENSITY_FUNCTION), p_335454_.lookup(Registries.NOISE)),
         SurfaceRuleData.overworldLike(false, false, false),
         List.of(),
         -64,
         false,
         false,
         false,
         true
      );
   }

   public static NoiseGeneratorSettings dummy() {
      return new NoiseGeneratorSettings(
         NoiseSettings.OVERWORLD_NOISE_SETTINGS,
         Blocks.STONE.defaultBlockState(),
         Blocks.AIR.defaultBlockState(),
         NoiseRouterData.none(),
         SurfaceRuleData.air(),
         List.of(),
         63,
         true,
         false,
         false,
         false
      );
   }
}
