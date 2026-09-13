package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;

public class EndGatewayConfiguration implements FeatureConfiguration {
    public static final Codec<EndGatewayConfiguration> CODEC = RecordCodecBuilder.create(
        i -> i.group(BlockPos.CODEC.optionalFieldOf("exit").forGetter(c -> c.exit), Codec.BOOL.fieldOf("exact").forGetter(c -> c.exact))
            .apply(i, EndGatewayConfiguration::new)
    );
    private final Optional<BlockPos> exit;
    private final boolean exact;

    private EndGatewayConfiguration(final Optional<BlockPos> exit, final boolean exact) {
        this.exit = exit;
        this.exact = exact;
    }

    public static EndGatewayConfiguration knownExit(final BlockPos exit, final boolean exact) {
        return new EndGatewayConfiguration(Optional.of(exit), exact);
    }

    public static EndGatewayConfiguration delayedExitSearch() {
        return new EndGatewayConfiguration(Optional.empty(), false);
    }

    public Optional<BlockPos> getExit() {
        return this.exit;
    }

    public boolean isExitExact() {
        return this.exact;
    }
}