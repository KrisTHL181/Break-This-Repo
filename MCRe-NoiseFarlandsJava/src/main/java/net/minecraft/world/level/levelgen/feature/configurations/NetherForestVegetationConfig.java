package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class NetherForestVegetationConfig extends BlockPileConfiguration {
    public static final Codec<NetherForestVegetationConfig> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                BlockStateProvider.CODEC.fieldOf("state_provider").forGetter(c -> c.stateProvider),
                ExtraCodecs.POSITIVE_INT.fieldOf("spread_width").forGetter(c -> c.spreadWidth),
                ExtraCodecs.POSITIVE_INT.fieldOf("spread_height").forGetter(c -> c.spreadHeight)
            )
            .apply(i, NetherForestVegetationConfig::new)
    );
    public final int spreadWidth;
    public final int spreadHeight;

    public NetherForestVegetationConfig(final BlockStateProvider stateProvider, final int spreadWidth, final int spreadHeight) {
        super(stateProvider);
        this.spreadWidth = spreadWidth;
        this.spreadHeight = spreadHeight;
    }
}