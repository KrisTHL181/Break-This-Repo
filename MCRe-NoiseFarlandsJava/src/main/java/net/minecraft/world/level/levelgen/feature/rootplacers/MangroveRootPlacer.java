package net.minecraft.world.level.levelgen.feature.rootplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class MangroveRootPlacer extends RootPlacer {
    public static final int ROOT_WIDTH_LIMIT = 8;
    public static final int ROOT_LENGTH_LIMIT = 15;
    public static final MapCodec<MangroveRootPlacer> CODEC = RecordCodecBuilder.mapCodec(
        i -> rootPlacerParts(i)
            .and(MangroveRootPlacement.CODEC.fieldOf("mangrove_root_placement").forGetter(c -> c.mangroveRootPlacement))
            .apply(i, MangroveRootPlacer::new)
    );
    private final MangroveRootPlacement mangroveRootPlacement;

    public MangroveRootPlacer(
        final IntProvider trunkOffsetY,
        final BlockStateProvider rootProvider,
        final Optional<AboveRootPlacement> aboveRootPlacement,
        final MangroveRootPlacement mangroveRootPlacement
    ) {
        super(trunkOffsetY, rootProvider, aboveRootPlacement);
        this.mangroveRootPlacement = mangroveRootPlacement;
    }

    @Override
    public boolean placeRoots(
        final WorldGenLevel level,
        final BiConsumer<BlockPos, BlockState> rootSetter,
        final RandomSource random,
        final BlockPos origin,
        final BlockPos trunkOrigin,
        final TreeConfiguration config
    ) {
        List<BlockPos> rootPositions = Lists.newArrayList();
        BlockPos.MutableBlockPos columnPos = origin.mutable();

        while (columnPos.getY() < trunkOrigin.getY()) {
            if (!this.canPlaceRoot(level, columnPos)) {
                return false;
            }

            columnPos.move(Direction.UP);
        }

        rootPositions.add(trunkOrigin.below());

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos pos = trunkOrigin.relative(dir);
            List<BlockPos> positionsInDirection = Lists.newArrayList();
            if (!this.simulateRoots(level, random, pos, dir, trunkOrigin, positionsInDirection, 0)) {
                return false;
            }

            rootPositions.addAll(positionsInDirection);
            rootPositions.add(trunkOrigin.relative(dir));
        }

        for (BlockPos rootPos : rootPositions) {
            this.placeRoot(level, rootSetter, random, rootPos, config);
        }

        return true;
    }

    private boolean simulateRoots(
        final LevelSimulatedReader level,
        final RandomSource random,
        final BlockPos rootPos,
        final Direction dir,
        final BlockPos rootOrigin,
        final List<BlockPos> rootPositions,
        final int layer
    ) {
        int maxRootLength = this.mangroveRootPlacement.maxRootLength();
        if (layer != maxRootLength && rootPositions.size() <= maxRootLength) {
            for (BlockPos pos : this.potentialRootPositions(rootPos, dir, random, rootOrigin)) {
                if (this.canPlaceRoot(level, pos)) {
                    rootPositions.add(pos);
                    if (!this.simulateRoots(level, random, pos, dir, rootOrigin, rootPositions, layer + 1)) {
                        return false;
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }

    protected List<BlockPos> potentialRootPositions(final BlockPos pos, final Direction prevDir, final RandomSource random, final BlockPos rootOrigin) {
        BlockPos below = pos.below();
        BlockPos nextTo = pos.relative(prevDir);
        int width = pos.distManhattan(rootOrigin);
        int maxRootWidth = this.mangroveRootPlacement.maxRootWidth();
        float randomSkewChance = this.mangroveRootPlacement.randomSkewChance();
        if (width > maxRootWidth - 3 && width <= maxRootWidth) {
            return random.nextFloat() < randomSkewChance ? List.of(below, nextTo.below()) : List.of(below);
        } else if (width > maxRootWidth) {
            return List.of(below);
        } else if (random.nextFloat() < randomSkewChance) {
            return List.of(below);
        } else {
            return random.nextBoolean() ? List.of(nextTo) : List.of(below);
        }
    }

    @Override
    protected boolean canPlaceRoot(final LevelSimulatedReader level, final BlockPos pos) {
        return super.canPlaceRoot(level, pos) || level.isStateAtPosition(pos, state -> state.is(this.mangroveRootPlacement.canGrowThrough()));
    }

    @Override
    protected void placeRoot(
        final WorldGenLevel level,
        final BiConsumer<BlockPos, BlockState> rootSetter,
        final RandomSource random,
        final BlockPos pos,
        final TreeConfiguration config
    ) {
        if (level.isStateAtPosition(pos, s -> s.is(this.mangroveRootPlacement.muddyRootsIn()))) {
            BlockState muddyRoots = this.mangroveRootPlacement.muddyRootsProvider().getState(level, random, pos);
            rootSetter.accept(pos, this.getPotentiallyWaterloggedState(level, pos, muddyRoots));
        } else {
            super.placeRoot(level, rootSetter, random, pos, config);
        }
    }

    @Override
    protected RootPlacerType<?> type() {
        return RootPlacerType.MANGROVE_ROOT_PLACER;
    }
}