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
    public static final Codec<WorldPreset> DIRECT_CODEC = RecordCodecBuilder.<WorldPreset>create(
            i -> i.group(Codec.unboundedMap(ResourceKey.codec(Registries.LEVEL_STEM), LevelStem.CODEC).fieldOf("dimensions").forGetter(e -> e.dimensions))
                .apply(i, WorldPreset::new)
        )
        .validate(WorldPreset::requireOverworld);
    public static final Codec<Holder<WorldPreset>> CODEC = RegistryFileCodec.create(Registries.WORLD_PRESET, DIRECT_CODEC);
    private final Map<ResourceKey<LevelStem>, LevelStem> dimensions;

    public WorldPreset(final Map<ResourceKey<LevelStem>, LevelStem> dimensions) {
        this.dimensions = dimensions;
    }

    private ImmutableMap<ResourceKey<LevelStem>, LevelStem> dimensionsInOrder() {
        Builder<ResourceKey<LevelStem>, LevelStem> builder = ImmutableMap.builder();
        WorldDimensions.keysInOrder(this.dimensions.keySet()).forEach(key -> {
            LevelStem levelStem = this.dimensions.get(key);
            if (levelStem != null) {
                builder.put((ResourceKey<LevelStem>)key, levelStem);
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

    private static DataResult<WorldPreset> requireOverworld(final WorldPreset preset) {
        return preset.overworld().isEmpty() ? DataResult.error(() -> "Missing overworld dimension") : DataResult.success(preset, Lifecycle.stable());
    }
}