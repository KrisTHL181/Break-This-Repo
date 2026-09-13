package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class CoralTreeFeature extends CoralFeature {
    public CoralTreeFeature(final Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    protected boolean placeFeature(final LevelAccessor level, final RandomSource random, final BlockPos origin, final BlockState state) {
        BlockPos.MutableBlockPos mutPos = origin.mutable();
        int trunckHeight = random.nextInt(3) + 1;

        for (int i = 0; i < trunckHeight; i++) {
            if (!this.placeCoralBlock(level, random, mutPos, state)) {
                return true;
            }

            mutPos.move(Direction.UP);
        }

        BlockPos trunckTopPos = mutPos.immutable();
        int nBranches = random.nextInt(3) + 2;
        List<Direction> directions = Direction.Plane.HORIZONTAL.shuffledCopy(random);

        for (Direction branchDirection : directions.subList(0, nBranches)) {
            mutPos.set(trunckTopPos);
            mutPos.move(branchDirection);
            int branchHeight = random.nextInt(5) + 2;
            int segmentLength = 0;

            for (int j = 0; j < branchHeight && this.placeCoralBlock(level, random, mutPos, state); j++) {
                segmentLength++;
                mutPos.move(Direction.UP);
                if (j == 0 || segmentLength >= 2 && random.nextFloat() < 0.25F) {
                    mutPos.move(branchDirection);
                    segmentLength = 0;
                }
            }
        }

        return true;
    }
}