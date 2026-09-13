package net.minecraft.data.worldgen;

import java.util.Optional;
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
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.timeline.Timeline;
import net.minecraft.world.timeline.Timelines;

public class DimensionTypes {
   public static void bootstrap(final BootstrapContext<DimensionType> context) {
      HolderGetter<Block> blocks = context.lookup(Registries.BLOCK);
      HolderGetter<Timeline> timelines = context.lookup(Registries.TIMELINE);
      HolderGetter<WorldClock> clocks = context.lookup(Registries.WORLD_CLOCK);
      EnvironmentAttributeMap overworldAttributes = EnvironmentAttributeMap.builder()
         .set(EnvironmentAttributes.FOG_COLOR, -4138753)
         .set(EnvironmentAttributes.SKY_COLOR, OverworldBiomes.calculateSkyColor(0.8F))
         .set(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, -16119286)
         .set(EnvironmentAttributes.CLOUD_COLOR, ARGB.white(0.8F))
         .set(EnvironmentAttributes.CLOUD_HEIGHT, 192.33F)
         .set(EnvironmentAttributes.BACKGROUND_MUSIC, BackgroundMusic.OVERWORLD)
         .set(EnvironmentAttributes.BED_RULE, BedRule.CAN_SLEEP_WHEN_DARK)
         .set(EnvironmentAttributes.STRAW_BED_RULE, BedRule.DESTROY_ON_LEAVE)
         .set(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, false)
         .set(EnvironmentAttributes.NETHER_PORTAL_SPAWNS_PIGLINS, true)
         .set(EnvironmentAttributes.AMBIENT_SOUNDS, AmbientSounds.LEGACY_CAVE_SETTINGS)
         .build();
      context.register(
         BuiltinDimensionTypes.OVERWORLD,
         new DimensionType(
            false,
            true,
            false,
            false,
            1.0,
            -64,
            384,
            384,
            blocks.getOrThrow(BlockTags.INFINIBURN_OVERWORLD),
            0.0F,
            new DimensionType.MonsterSettings(UniformInt.of(0, 7), 0),
            DimensionType.Skybox.OVERWORLD,
            CardinalLighting.Type.DEFAULT,
            overworldAttributes,
            timelines.getOrThrow(TimelineTags.IN_OVERWORLD),
            Optional.of(clocks.getOrThrow(WorldClocks.OVERWORLD))
         )
      );
      context.register(
         BuiltinDimensionTypes.NETHER,
         new DimensionType(
            true,
            false,
            true,
            false,
            8.0,
            0,
            256,
            128,
            blocks.getOrThrow(BlockTags.INFINIBURN_NETHER),
            0.1F,
            new DimensionType.MonsterSettings(ConstantInt.of(7), 15),
            DimensionType.Skybox.NONE,
            CardinalLighting.Type.NETHER,
            EnvironmentAttributeMap.builder()
               .set(EnvironmentAttributes.FOG_START_DISTANCE, 10.0F)
               .set(EnvironmentAttributes.FOG_END_DISTANCE, 96.0F)
               .set(EnvironmentAttributes.SKY_LIGHT_COLOR, Timelines.NIGHT_SKY_LIGHT_COLOR)
               .set(EnvironmentAttributes.SKY_LIGHT_LEVEL, 4.0F)
               .set(EnvironmentAttributes.SKY_LIGHT_FACTOR, 0.0F)
               .set(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, -13621215)
               .set(EnvironmentAttributes.DEFAULT_DRIPSTONE_PARTICLE, ParticleTypes.DRIPPING_DRIPSTONE_LAVA)
               .set(EnvironmentAttributes.BED_RULE, BedRule.DESTROY_ON_USE)
               .set(EnvironmentAttributes.STRAW_BED_RULE, BedRule.DESTROY_ON_USE)
               .set(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, true)
               .set(EnvironmentAttributes.WATER_EVAPORATES, true)
               .set(EnvironmentAttributes.FAST_LAVA, true)
               .set(EnvironmentAttributes.PIGLINS_ZOMBIFY, false)
               .set(EnvironmentAttributes.CAN_START_RAID, false)
               .set(EnvironmentAttributes.SNOW_GOLEM_MELTS, true)
               .build(),
            timelines.getOrThrow(TimelineTags.IN_NETHER),
            Optional.empty()
         )
      );
      context.register(
         BuiltinDimensionTypes.END,
         new DimensionType(
            true,
            true,
            false,
            true,
            1.0,
            0,
            256,
            256,
            blocks.getOrThrow(BlockTags.INFINIBURN_END),
            0.25F,
            new DimensionType.MonsterSettings(ConstantInt.of(15), 0),
            DimensionType.Skybox.END,
            CardinalLighting.Type.DEFAULT,
            EnvironmentAttributeMap.builder()
               .set(EnvironmentAttributes.FOG_COLOR, -15199464)
               .set(EnvironmentAttributes.SKY_LIGHT_COLOR, -5480243)
               .set(EnvironmentAttributes.SKY_COLOR, -16777216)
               .set(EnvironmentAttributes.SKY_LIGHT_FACTOR, 0.0F)
               .set(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, -12630209)
               .set(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(Musics.END))
               .set(EnvironmentAttributes.AMBIENT_SOUNDS, AmbientSounds.LEGACY_CAVE_SETTINGS)
               .set(EnvironmentAttributes.BED_RULE, BedRule.DESTROY_ON_USE)
               .set(EnvironmentAttributes.STRAW_BED_RULE, BedRule.DESTROY_ON_USE)
               .set(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, false)
               .build(),
            timelines.getOrThrow(TimelineTags.IN_END),
            Optional.of(clocks.getOrThrow(WorldClocks.THE_END))
         )
      );
      context.register(
         BuiltinDimensionTypes.OVERWORLD_CAVES,
         new DimensionType(
            false,
            true,
            true,
            false,
            1.0,
            -64,
            384,
            384,
            blocks.getOrThrow(BlockTags.INFINIBURN_OVERWORLD),
            0.0F,
            new DimensionType.MonsterSettings(UniformInt.of(0, 7), 0),
            DimensionType.Skybox.OVERWORLD,
            CardinalLighting.Type.DEFAULT,
            overworldAttributes,
            timelines.getOrThrow(TimelineTags.IN_OVERWORLD),
            Optional.of(clocks.getOrThrow(WorldClocks.OVERWORLD))
         )
      );
   }
}
