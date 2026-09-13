package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.modifier.AttributeModifier;
import net.minecraft.world.level.DryFoliageColor;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public final class Biome {
   public static final Codec<Biome> DIRECT_CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Biome.ClimateSettings.CODEC.forGetter(b -> b.climateSettings),
            EnvironmentAttributeMap.CODEC_ONLY_POSITIONAL.optionalFieldOf("attributes", EnvironmentAttributeMap.EMPTY).forGetter(b -> b.attributes),
            BiomeSpecialEffects.CODEC.fieldOf("effects").forGetter(b -> b.specialEffects),
            BiomeGenerationSettings.CODEC.forGetter(b -> b.generationSettings),
            MobSpawnSettings.CODEC.forGetter(b -> b.mobSettings)
         )
         .apply(i, Biome::new)
   );
   public static final Codec<Biome> NETWORK_CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Biome.ClimateSettings.CODEC.forGetter(b -> b.climateSettings),
            EnvironmentAttributeMap.NETWORK_CODEC.optionalFieldOf("attributes", EnvironmentAttributeMap.EMPTY).forGetter(b -> b.attributes),
            BiomeSpecialEffects.CODEC.fieldOf("effects").forGetter(b -> b.specialEffects)
         )
         .apply(
            i,
            (climateSettings, attributes, specialEffects) -> new Biome(
               climateSettings, attributes, specialEffects, BiomeGenerationSettings.EMPTY, MobSpawnSettings.EMPTY
            )
         )
   );
   public static final Codec<Holder<Biome>> CODEC = RegistryFileCodec.create(Registries.BIOME, DIRECT_CODEC);
   public static final Codec<HolderSet<Biome>> LIST_CODEC = RegistryCodecs.homogeneousList(Registries.BIOME, DIRECT_CODEC);
   private static final PerlinSimplexNoise TEMPERATURE_NOISE = new PerlinSimplexNoise(new WorldgenRandom(new LegacyRandomSource(1234L)), ImmutableList.of(0));
   private static final PerlinSimplexNoise FROZEN_TEMPERATURE_NOISE = new PerlinSimplexNoise(
      new WorldgenRandom(new LegacyRandomSource(3456L)), ImmutableList.of(-2, -1, 0)
   );
   @Deprecated(forRemoval = true)
   public static final PerlinSimplexNoise BIOME_INFO_NOISE = new PerlinSimplexNoise(new WorldgenRandom(new LegacyRandomSource(2345L)), ImmutableList.of(0));
   private static final int TEMPERATURE_CACHE_SIZE = 1024;
   private final Biome.ClimateSettings climateSettings;
   private final BiomeGenerationSettings generationSettings;
   private final MobSpawnSettings mobSettings;
   private final EnvironmentAttributeMap attributes;
   private final BiomeSpecialEffects specialEffects;
   private final ThreadLocal<Long2FloatLinkedOpenHashMap> temperatureCache = ThreadLocal.withInitial(() -> {
      Long2FloatLinkedOpenHashMap map = new Long2FloatLinkedOpenHashMap(1024, 0.25F) {
         protected void rehash(final int newN) {
         }
      };
      map.defaultReturnValue(Float.NaN);
      return map;
   });

   private Biome(
      final Biome.ClimateSettings climateSettings,
      final EnvironmentAttributeMap attributes,
      final BiomeSpecialEffects specialEffects,
      final BiomeGenerationSettings generationSettings,
      final MobSpawnSettings mobSettings
   ) {
      this.climateSettings = climateSettings;
      this.generationSettings = generationSettings;
      this.mobSettings = mobSettings;
      this.attributes = attributes;
      this.specialEffects = specialEffects;
   }

   public MobSpawnSettings getMobSettings() {
      return this.mobSettings;
   }

   public boolean hasPrecipitation() {
      return this.climateSettings.hasPrecipitation();
   }

   public Biome.Precipitation getPrecipitationAt(final BlockPos pos, final int seaLevel) {
      if (!this.hasPrecipitation()) {
         return Biome.Precipitation.NONE;
      } else {
         return this.coldEnoughToSnow(pos, seaLevel) ? Biome.Precipitation.SNOW : Biome.Precipitation.RAIN;
      }
   }

   private float getHeightAdjustedTemperature(final BlockPos pos, final int seaLevel) {
      float adjustedTemperature = this.climateSettings.temperatureModifier.modifyTemperature(pos, this.getBaseTemperature());
      int snowLevel = seaLevel + 17;
      if (pos.getY() > snowLevel) {
         float v = (float)(TEMPERATURE_NOISE.getValue(pos.getX() / 8.0F, pos.getZ() / 8.0F, false) * 8.0);
         return adjustedTemperature - (v + pos.getY() - snowLevel) * 0.05F / 40.0F;
      } else {
         return adjustedTemperature;
      }
   }

   @Deprecated
   private float getTemperature(final BlockPos pos, final int seaLevel) {
      long key = pos.asLong();
      Long2FloatLinkedOpenHashMap cache = this.temperatureCache.get();
      float cached = cache.get(key);
      if (!Float.isNaN(cached)) {
         return cached;
      }

      float temp = this.getHeightAdjustedTemperature(pos, seaLevel);
      if (cache.size() == 1024) {
         cache.removeFirstFloat();
      }

      cache.put(key, temp);
      return temp;
   }

   public boolean shouldFreeze(final LevelReader level, final BlockPos pos) {
      return this.shouldFreeze(level, pos, true);
   }

   public boolean shouldFreeze(final LevelReader level, final BlockPos pos, final boolean checkNeighbors) {
      if (this.warmEnoughToRain(pos, level.getSeaLevel())) {
         return false;
      }

      if (level.isInsideBuildHeight(pos.getY()) && level.getBrightness(LightLayer.BLOCK, pos) < 10) {
         BlockState blockState = level.getBlockState(pos);
         FluidState fluidState = level.getFluidState(pos);
         if (fluidState.is(Fluids.WATER) && blockState.getBlock() instanceof LiquidBlock) {
            if (!checkNeighbors) {
               return true;
            }

            boolean surroundedByWater = level.isWaterAt(pos.west())
               && level.isWaterAt(pos.east())
               && level.isWaterAt(pos.north())
               && level.isWaterAt(pos.south());
            if (!surroundedByWater) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean coldEnoughToSnow(final BlockPos pos, final int seaLevel) {
      return !this.warmEnoughToRain(pos, seaLevel);
   }

   public boolean warmEnoughToRain(final BlockPos pos, final int seaLevel) {
      return this.getTemperature(pos, seaLevel) >= 0.15F;
   }

   public boolean shouldMeltFrozenOceanIcebergSlightly(final BlockPos pos, final int seaLevel) {
      return this.getTemperature(pos, seaLevel) > 0.1F;
   }

   public boolean shouldSnow(final LevelReader level, final BlockPos pos) {
      if (this.getPrecipitationAt(pos, level.getSeaLevel()) != Biome.Precipitation.SNOW) {
         return false;
      }

      if (level.isInsideBuildHeight(pos.getY()) && level.getBrightness(LightLayer.BLOCK, pos) < 10) {
         BlockState state = level.getBlockState(pos);
         if ((state.isAir() || state.is(Blocks.SNOW)) && Blocks.SNOW.defaultBlockState().canSurvive(level, pos)) {
            return true;
         }
      }

      return false;
   }

   public BiomeGenerationSettings getGenerationSettings() {
      return this.generationSettings;
   }

   public int getGrassColor(final double x, final double z) {
      int baseGrassColor = this.getBaseGrassColor();
      return this.specialEffects.grassColorModifier().modifyColor(x, z, baseGrassColor);
   }

   private int getBaseGrassColor() {
      Optional<Integer> colorOverride = this.specialEffects.grassColorOverride();
      return colorOverride.isPresent() ? colorOverride.get() : this.getGrassColorFromTexture();
   }

   private int getGrassColorFromTexture() {
      double temp = Mth.clamp(this.climateSettings.temperature, 0.0F, 1.0F);
      double rain = Mth.clamp(this.climateSettings.downfall, 0.0F, 1.0F);
      return GrassColor.get(temp, rain);
   }

   public int getFoliageColor() {
      return this.specialEffects.foliageColorOverride().orElseGet(this::getFoliageColorFromTexture);
   }

   private int getFoliageColorFromTexture() {
      double temp = Mth.clamp(this.climateSettings.temperature, 0.0F, 1.0F);
      double rain = Mth.clamp(this.climateSettings.downfall, 0.0F, 1.0F);
      return FoliageColor.get(temp, rain);
   }

   public int getDryFoliageColor() {
      return this.specialEffects.dryFoliageColorOverride().orElseGet(this::getDryFoliageColorFromTexture);
   }

   private int getDryFoliageColorFromTexture() {
      double temp = Mth.clamp(this.climateSettings.temperature, 0.0F, 1.0F);
      double rain = Mth.clamp(this.climateSettings.downfall, 0.0F, 1.0F);
      return DryFoliageColor.get(temp, rain);
   }

   public float getBaseTemperature() {
      return this.climateSettings.temperature;
   }

   public EnvironmentAttributeMap getAttributes() {
      return this.attributes;
   }

   public BiomeSpecialEffects getSpecialEffects() {
      return this.specialEffects;
   }

   public int getWaterColor() {
      return this.specialEffects.waterColor();
   }

   public static class BiomeBuilder {
      private boolean hasPrecipitation = true;
      private @Nullable Float temperature;
      private Biome.TemperatureModifier temperatureModifier = Biome.TemperatureModifier.NONE;
      private @Nullable Float downfall;
      private final EnvironmentAttributeMap.Builder attributes = EnvironmentAttributeMap.builder();
      private @Nullable BiomeSpecialEffects specialEffects;
      private @Nullable MobSpawnSettings mobSpawnSettings;
      private @Nullable BiomeGenerationSettings generationSettings;

      public Biome.BiomeBuilder hasPrecipitation(final boolean hasPrecipitation) {
         this.hasPrecipitation = hasPrecipitation;
         return this;
      }

      public Biome.BiomeBuilder temperature(final float temperature) {
         this.temperature = temperature;
         return this;
      }

      public Biome.BiomeBuilder downfall(final float downfall) {
         this.downfall = downfall;
         return this;
      }

      public Biome.BiomeBuilder putAttributes(final EnvironmentAttributeMap attributes) {
         this.attributes.putAll(attributes);
         return this;
      }

      public Biome.BiomeBuilder putAttributes(final EnvironmentAttributeMap.Builder attributes) {
         return this.putAttributes(attributes.build());
      }

      public <Value> Biome.BiomeBuilder setAttribute(final EnvironmentAttribute<Value> attribute, final Value value) {
         this.attributes.set(attribute, value);
         return this;
      }

      public <Value, Parameter> Biome.BiomeBuilder modifyAttribute(
         final EnvironmentAttribute<Value> attribute, final AttributeModifier<Value, Parameter> modifier, final Parameter value
      ) {
         this.attributes.modify(attribute, modifier, value);
         return this;
      }

      public Biome.BiomeBuilder specialEffects(final BiomeSpecialEffects specialEffects) {
         this.specialEffects = specialEffects;
         return this;
      }

      public Biome.BiomeBuilder mobSpawnSettings(final MobSpawnSettings mobSpawnSettings) {
         this.mobSpawnSettings = mobSpawnSettings;
         return this;
      }

      public Biome.BiomeBuilder generationSettings(final BiomeGenerationSettings generationSettings) {
         this.generationSettings = generationSettings;
         return this;
      }

      public Biome.BiomeBuilder temperatureAdjustment(final Biome.TemperatureModifier temperatureModifier) {
         this.temperatureModifier = temperatureModifier;
         return this;
      }

      public Biome build() {
         if (this.temperature != null
            && this.downfall != null
            && this.specialEffects != null
            && this.mobSpawnSettings != null
            && this.generationSettings != null) {
            return new Biome(
               new Biome.ClimateSettings(this.hasPrecipitation, this.temperature, this.temperatureModifier, this.downfall),
               this.attributes.build(),
               this.specialEffects,
               this.generationSettings,
               this.mobSpawnSettings
            );
         } else {
            throw new IllegalStateException("You are missing parameters to build a proper biome\n" + this);
         }
      }

      @Override
      public String toString() {
         return "BiomeBuilder{\nhasPrecipitation="
            + this.hasPrecipitation
            + ",\ntemperature="
            + this.temperature
            + ",\ntemperatureModifier="
            + this.temperatureModifier
            + ",\ndownfall="
            + this.downfall
            + ",\nspecialEffects="
            + this.specialEffects
            + ",\nmobSpawnSettings="
            + this.mobSpawnSettings
            + ",\ngenerationSettings="
            + this.generationSettings
            + ",\n}";
      }
   }

   private record ClimateSettings(boolean hasPrecipitation, float temperature, Biome.TemperatureModifier temperatureModifier, float downfall) {
      public static final MapCodec<Biome.ClimateSettings> CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               Codec.BOOL.fieldOf("has_precipitation").forGetter(b -> b.hasPrecipitation),
               Codec.FLOAT.fieldOf("temperature").forGetter(b -> b.temperature),
               Biome.TemperatureModifier.CODEC.optionalFieldOf("temperature_modifier", Biome.TemperatureModifier.NONE).forGetter(b -> b.temperatureModifier),
               Codec.FLOAT.fieldOf("downfall").forGetter(b -> b.downfall)
            )
            .apply(i, Biome.ClimateSettings::new)
      );
   }

   public enum Precipitation implements StringRepresentable {
      NONE("none"),
      RAIN("rain"),
      SNOW("snow");

      public static final Codec<Biome.Precipitation> CODEC = StringRepresentable.fromEnum(Biome.Precipitation::values);
      private final String name;

      Precipitation(final String name) {
         this.name = name;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }

   public enum TemperatureModifier implements StringRepresentable {
      NONE("none") {
         @Override
         public float modifyTemperature(final BlockPos pos, final float baseTemperature) {
            return baseTemperature;
         }
      },
      FROZEN("frozen") {
         @Override
         public float modifyTemperature(final BlockPos pos, final float baseTemperature) {
            double groundValueLargeVariation = Biome.FROZEN_TEMPERATURE_NOISE.getValue(pos.getX() * 0.05, pos.getZ() * 0.05, false) * 7.0;
            double groundValueEdgeVariation = Biome.BIOME_INFO_NOISE.getValue(pos.getX() * 0.2, pos.getZ() * 0.2, false);
            double icePatches = groundValueLargeVariation + groundValueEdgeVariation;
            if (icePatches < 0.3) {
               double groundValueSmallVariation = Biome.BIOME_INFO_NOISE.getValue(pos.getX() * 0.09, pos.getZ() * 0.09, false);
               if (groundValueSmallVariation < 0.8) {
                  return 0.2F;
               }
            }

            return baseTemperature;
         }
      };

      private final String name;
      public static final Codec<Biome.TemperatureModifier> CODEC = StringRepresentable.fromEnum(Biome.TemperatureModifier::values);

      public abstract float modifyTemperature(final BlockPos pos, final float baseTemperature);

      TemperatureModifier(final String name) {
         this.name = name;
      }

      public String getName() {
         return this.name;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }
}
