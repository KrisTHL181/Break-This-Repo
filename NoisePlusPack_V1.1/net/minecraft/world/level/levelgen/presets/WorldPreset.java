package net.minecraft.world.level.levelgen.presets;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

public class WorldPreset {
   public static final Codec<WorldPreset> DIRECT_CODEC = RecordCodecBuilder.create(
         p_259011_ -> p_259011_.group(
               Codec.unboundedMap(ResourceKey.codec(Registries.LEVEL_STEM), LevelStem.CODEC).fieldOf("dimensions").forGetter(p_226430_ -> p_226430_.dimensions)
            )
            .apply(p_259011_, WorldPreset::new)
      )
      .validate(WorldPreset::requireOverworld);
   public static final Codec<Holder<WorldPreset>> CODEC = RegistryFileCodec.create(Registries.WORLD_PRESET, DIRECT_CODEC);
   private final Map<ResourceKey<LevelStem>, LevelStem> dimensions;

   public WorldPreset(Map<ResourceKey<LevelStem>, LevelStem> p_226419_) {
      this.dimensions = p_226419_;
   }

   private ImmutableMap<ResourceKey<LevelStem>, LevelStem> dimensionsInOrder() {
      Builder<ResourceKey<LevelStem>, LevelStem> builder = ImmutableMap.builder();
      WorldDimensions.keysInOrder(this.dimensions.keySet().stream()).forEach(p_327474_ -> {
         LevelStem levelstem = this.dimensions.get(p_327474_);
         if (levelstem != null) {
            builder.put(p_327474_, levelstem);
         }
      });
      return builder.build();
   }

   public WorldDimensions createWorldDimensions() {
      return new WorldDimensions(this.dimensionsInOrder());
   }

   public Optional<LevelStem> overworld() {
      return Optional.ofNullable(this.dimensions.get(LevelStem.OVERWORLD));
   }

   private static DataResult<WorldPreset> requireOverworld(WorldPreset p_238379_) {
      return p_238379_.overworld().isEmpty() ? DataResult.error(() -> "Missing overworld dimension") : DataResult.success(p_238379_, Lifecycle.stable());
   }
}
