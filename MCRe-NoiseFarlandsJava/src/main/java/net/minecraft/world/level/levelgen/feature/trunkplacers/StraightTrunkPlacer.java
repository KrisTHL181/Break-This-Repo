package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class StraightTrunkPlacer extends TrunkPlacer {
    public static final MapCodec<StraightTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(i -> trunkPlacerParts(i).apply(i, StraightTrunkPlacer::new));

    public StraightTrunkPlacer(final int baseHeight, final int heightRandA, final int heightRandB) {
        super(baseHeight, heightRandA, heightRandB);
    }

    @Override
    protected TrunkPlacerType<?> type() {
        return TrunkPlacerType.STRAIGHT_TRUNK_PLACER;
    }

    @Override
    public List<FoliagePlacer.FoliageAttachment> placeTrunk(
        final WorldGenLevel level,
        final BiConsumer<BlockPos, BlockState> trunkSetter,
        final RandomSource random,
        final int treeHeight,
        final BlockPos origin,
        final TreeConfiguration config
    ) {
        placeBelowTrunkBlock(level, trunkSetter, random, origin.below(), config);

        for (int y = 0; y < treeHeight; y++) {
            this.placeLog(level, trunkSetter, random, origin.above(y), config);
        }

        return ImmutableList.of(new FoliagePlacer.FoliageAttachment(origin.above(treeHeight), 0, false));
    }
}