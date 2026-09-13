package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;

public class DiskFeature extends Feature<DiskConfiguration> {
    public DiskFeature(final Codec<DiskConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<DiskConfiguration> context) {
        DiskConfiguration config = context.config();
        BlockPos origin = context.origin();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        boolean placedAny = false;
        int originY = origin.getY();
        int top = originY + config.halfHeight();
        int bottom = originY - config.halfHeight() - 1;
        int r = config.radius().sample(random);
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (BlockPos columnPos : BlockPos.betweenClosed(origin.offset(-r, 0, -r), origin.offset(r, 0, r))) {
            int xd = columnPos.getX() - origin.getX();
            int zd = columnPos.getZ() - origin.getZ();
            if (xd * xd + zd * zd <= r * r) {
                placedAny |= this.placeColumn(config, level, random, top, bottom, mutablePos.set(columnPos));
            }
        }

        return placedAny;
    }

    protected boolean placeColumn(
        final DiskConfiguration config,
        final WorldGenLevel level,
        final RandomSource random,
        final int top,
        final int bottom,
        final BlockPos.MutableBlockPos pos
    ) {
        boolean placedAny = false;
        boolean placedAbove = false;

        for (int y = top; y > bottom; y--) {
            pos.setY(y);
            if (config.target().test(level, pos)) {
                BlockState state = config.stateProvider().getOptionalState(level, random, pos);
                if (state != null) {
                    level.setBlock(pos, state, 2);
                    if (!placedAbove) {
                        this.markAboveForPostProcessing(level, pos);
                    }

                    placedAny = true;
                    placedAbove = true;
                }
            } else {
                placedAbove = false;
            }
        }

        return placedAny;
    }
}