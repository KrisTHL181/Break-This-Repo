package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

public class RandomSpreadFoliagePlacer extends FoliagePlacer {
    public static final MapCodec<RandomSpreadFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(
        i -> foliagePlacerParts(i)
            .and(
                i.group(
                    IntProviders.codec(1, 512).fieldOf("foliage_height").forGetter(c -> c.foliageHeight),
                    Codec.intRange(0, 256).fieldOf("leaf_placement_attempts").forGetter(c -> c.leafPlacementAttempts)
                )
            )
            .apply(i, RandomSpreadFoliagePlacer::new)
    );
    private final IntProvider foliageHeight;
    private final int leafPlacementAttempts;

    public RandomSpreadFoliagePlacer(final IntProvider radius, final IntProvider offset, final IntProvider foliageHeight, final int leafPlacementAttempts) {
        super(radius, offset);
        this.foliageHeight = foliageHeight;
        this.leafPlacementAttempts = leafPlacementAttempts;
    }

    @Override
    protected FoliagePlacerType<?> type() {
        return FoliagePlacerType.RANDOM_SPREAD_FOLIAGE_PLACER;
    }

    @Override
    protected void createFoliage(
        final WorldGenLevel level,
        final FoliagePlacer.FoliageSetter foliageSetter,
        final RandomSource random,
        final TreeConfiguration config,
        final int treeHeight,
        final FoliagePlacer.FoliageAttachment foliageAttachment,
        final int foliageHeight,
        final int leafRadius,
        final int offset
    ) {
        BlockPos origin = foliageAttachment.pos();
        BlockPos.MutableBlockPos pos = origin.mutable();

        for (int i = 0; i < this.leafPlacementAttempts; i++) {
            pos.setWithOffset(
                origin,
                random.nextInt(leafRadius) - random.nextInt(leafRadius),
                random.nextInt(foliageHeight) - random.nextInt(foliageHeight),
                random.nextInt(leafRadius) - random.nextInt(leafRadius)
            );
            tryPlaceLeaf(level, foliageSetter, random, config, pos);
        }
    }

    @Override
    public int foliageHeight(final RandomSource random, final int treeHeight, final TreeConfiguration config) {
        return this.foliageHeight.sample(random);
    }

    @Override
    protected boolean shouldSkipLocation(final RandomSource random, final int dx, final int y, final int dz, final int currentRadius, final boolean doubleTrunk) {
        return false;
    }
}