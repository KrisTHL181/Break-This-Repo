package net.minecraft.data.worldgen;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.biome.OverworldBiomes;
import net.minecraft.sounds.Musics;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TimelineTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.attribute.AmbientSounds;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.timeline.Timeline;
import net.minecraft.world.timeline.Timelines;

public class DimensionTypes {
   public static void bootstrap(BootstrapContext<DimensionType> p_333835_) {
      HolderGetter<Timeline> holdergetter = p_333835_.lookup(Registries.TIMELINE);
      EnvironmentAttributeMap environmentattributemap = EnvironmentAttributeMap.builder()
         .set(EnvironmentAttributes.FOG_COLOR, -4138753)
         .set(EnvironmentAttributes.SKY_COLOR, OverworldBiomes.calculateSkyColor(0.8F))
         .set(EnvironmentAttributes.CLOUD_COLOR, ARGB.white(0.8F))
         .set(EnvironmentAttributes.CLOUD_HEIGHT, 192.33F)
         .set(EnvironmentAttributes.BACKGROUND_MUSIC, BackgroundMusic.OVERWORLD)
         .set(EnvironmentAttributes.BED_RULE, BedRule.CAN_SLEEP_WHEN_DARK)
         .set(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, false)
         .set(EnvironmentAttributes.NETHER_PORTAL_SPAWNS_PIGLINS, true)
         .set(EnvironmentAttributes.AMBIENT_SOUNDS, AmbientSounds.LEGACY_CAVE_SETTINGS)
         .build();
      p_333835_.register(
         BuiltinDimensionTypes.OVERWORLD,
         new DimensionType(
            false,
            true,
            false,
            1.0,
            -64,
            384,
            384,
            BlockTags.INFINIBURN_OVERWORLD,
            0.0F,
            new DimensionType.MonsterSettings(UniformInt.of(0, 7), 0),
            DimensionType.Skybox.OVERWORLD,
            DimensionType.CardinalLightType.DEFAULT,
            environmentattributemap,
            holdergetter.getOrThrow(TimelineTags.IN_OVERWORLD)
         )
      );
      p_333835_.register(
         BuiltinDimensionTypes.NETHER,
         new DimensionType(
            true,
            false,
            true,
            8.0,
            0,
            256,
            128,
            BlockTags.INFINIBURN_NETHER,
            0.1F,
            new DimensionType.MonsterSettings(ConstantInt.of(7), 15),
            DimensionType.Skybox.NONE,
            DimensionType.CardinalLightType.NETHER,
            EnvironmentAttributeMap.builder()
               .set(EnvironmentAttributes.FOG_START_DISTANCE, 10.0F)
               .set(EnvironmentAttributes.FOG_END_DISTANCE, 96.0F)
               .set(EnvironmentAttributes.SKY_LIGHT_COLOR, Timelines.NIGHT_SKY_LIGHT_COLOR)
               .set(EnvironmentAttributes.SKY_LIGHT_LEVEL, 4.0F)
               .set(EnvironmentAttributes.SKY_LIGHT_FACTOR, 0.0F)
               .set(EnvironmentAttributes.DEFAULT_DRIPSTONE_PARTICLE, ParticleTypes.DRIPPING_DRIPSTONE_LAVA)
               .set(EnvironmentAttributes.BED_RULE, BedRule.EXPLODES)
               .set(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, true)
               .set(EnvironmentAttributes.WATER_EVAPORATES, true)
               .set(EnvironmentAttributes.FAST_LAVA, true)
               .set(EnvironmentAttributes.PIGLINS_ZOMBIFY, false)
               .set(EnvironmentAttributes.CAN_START_RAID, false)
               .set(EnvironmentAttributes.SNOW_GOLEM_MELTS, true)
               .build(),
            holdergetter.getOrThrow(TimelineTags.IN_NETHER)
         )
      );
      p_333835_.register(
         BuiltinDimensionTypes.END,
         new DimensionType(
            true,
            true,
            false,
            1.0,
            0,
            256,
            256,
            BlockTags.INFINIBURN_END,
            0.25F,
            new DimensionType.MonsterSettings(ConstantInt.of(15), 0),
            DimensionType.Skybox.END,
            DimensionType.CardinalLightType.DEFAULT,
            EnvironmentAttributeMap.builder()
               .set(EnvironmentAttributes.FOG_COLOR, -15199464)
               .set(EnvironmentAttributes.SKY_LIGHT_COLOR, -1736449)
               .set(EnvironmentAttributes.SKY_COLOR, -16777216)
               .set(EnvironmentAttributes.SKY_LIGHT_FACTOR, 0.0F)
               .set(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(Musics.END))
               .set(EnvironmentAttributes.AMBIENT_SOUNDS, AmbientSounds.LEGACY_CAVE_SETTINGS)
               .set(EnvironmentAttributes.BED_RULE, BedRule.EXPLODES)
               .set(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, false)
               .build(),
            holdergetter.getOrThrow(TimelineTags.IN_END)
         )
      );
      p_333835_.register(
         BuiltinDimensionTypes.OVERWORLD_CAVES,
         new DimensionType(
            false,
            true,
            true,
            1.0,
            -64,
            384,
            384,
            BlockTags.INFINIBURN_OVERWORLD,
            0.0F,
            new DimensionType.MonsterSettings(UniformInt.of(0, 7), 0),
            DimensionType.Skybox.OVERWORLD,
            DimensionType.CardinalLightType.DEFAULT,
            environmentattributemap,
            holdergetter.getOrThrow(TimelineTags.IN_OVERWORLD)
         )
      );
   }
}
