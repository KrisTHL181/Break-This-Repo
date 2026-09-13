package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record SpeleothemConfiguration(
    BlockState baseBlock,
    BlockState pointedBlock,
    HolderSet<Block> replaceableBlocks,
    float chanceOfTallerGeneration,
    float chanceOfDirectionalSpread,
    float chanceOfSpreadRadius2,
    float chanceOfSpreadRadius3
) implements FeatureConfiguration {
    public static final Codec<SpeleothemConfiguration> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                BlockState.CODEC.fieldOf("base_block").forGetter(c -> c.baseBlock),
                BlockState.CODEC.fieldOf("pointed_block").forGetter(c -> c.pointedBlock),
                RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("replaceable_blocks").forGetter(c -> c.replaceableBlocks),
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("chance_of_taller_generation", 0.2F).forGetter(c -> c.chanceOfTallerGeneration),
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("chance_of_directional_spread", 0.7F).forGetter(c -> c.chanceOfDirectionalSpread),
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("chance_of_spread_radius2", 0.5F).forGetter(c -> c.chanceOfSpreadRadius2),
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("chance_of_spread_radius3", 0.5F).forGetter(c -> c.chanceOfSpreadRadius3)
            )
            .apply(i, SpeleothemConfiguration::new)
    );
}