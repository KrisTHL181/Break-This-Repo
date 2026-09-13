package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.TrapezoidHeight;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;

public class HeightRangePlacement extends PlacementModifier {
    public static final MapCodec<HeightRangePlacement> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(HeightProvider.CODEC.fieldOf("height").forGetter(c -> c.height)).apply(i, HeightRangePlacement::new)
    );
    private final HeightProvider height;

    private HeightRangePlacement(final HeightProvider height) {
        this.height = height;
    }

    public static HeightRangePlacement of(final HeightProvider height) {
        return new HeightRangePlacement(height);
    }

    public static HeightRangePlacement uniform(final VerticalAnchor minInclusive, final VerticalAnchor maxInclusive) {
        return of(UniformHeight.of(minInclusive, maxInclusive));
    }

    public static HeightRangePlacement triangle(final VerticalAnchor minInclusive, final VerticalAnchor maxInclusive) {
        return of(TrapezoidHeight.of(minInclusive, maxInclusive));
    }

    @Override
    public Stream<BlockPos> getPositions(final PlacementContext context, final RandomSource random, final BlockPos origin) {
        return Stream.of(origin.atY(this.height.sample(random, context)));
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.HEIGHT_RANGE;
    }
}