package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.SpikeConfiguration;

public class SpikeFeature extends Feature<SpikeConfiguration> {
    public SpikeFeature(final Codec<SpikeConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<SpikeConfiguration> context) {
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        WorldGenLevel level = context.level();

        while (level.isEmptyBlock(origin) && origin.getY() > level.getMinY() + 2) {
            origin = origin.below();
        }

        SpikeConfiguration config = context.config();
        if (!config.canPlaceOn().test(level, origin)) {
            return false;
        }

        origin = origin.above(random.nextInt(4));
        int height = random.nextInt(4) + 7;
        int width = height / 4 + random.nextInt(2);
        if (width > 1 && random.nextInt(60) == 0) {
            origin = origin.above(10 + random.nextInt(30));
        }

        for (int yOff = 0; yOff < height; yOff++) {
            float scale = (1.0F - (float)yOff / height) * width;
            int newWidth = Mth.ceil(scale);

            for (int xo = -newWidth; xo <= newWidth; xo++) {
                float dx = Mth.abs(xo) - 0.25F;

                for (int zo = -newWidth; zo <= newWidth; zo++) {
                    float dz = Mth.abs(zo) - 0.25F;
                    if ((xo == 0 && zo == 0 || !(dx * dx + dz * dz > scale * scale))
                        && (xo != -newWidth && xo != newWidth && zo != -newWidth && zo != newWidth || !(random.nextFloat() > 0.75F))) {
                        BlockPos positiveOffset = origin.offset(xo, yOff, zo);
                        BlockState state = level.getBlockState(positiveOffset);
                        if (state.isAir() || config.canReplace().test(level, positiveOffset)) {
                            this.setBlock(level, positiveOffset, config.state());
                        }

                        if (yOff != 0 && newWidth > 1) {
                            BlockPos negativeOffset = origin.offset(xo, -yOff, zo);
                            state = level.getBlockState(negativeOffset);
                            if (state.isAir() || config.canReplace().test(level, negativeOffset)) {
                                this.setBlock(level, negativeOffset, config.state());
                            }
                        }
                    }
                }
            }
        }

        int pillarWidth = width - 1;
        if (pillarWidth < 0) {
            pillarWidth = 0;
        } else if (pillarWidth > 1) {
            pillarWidth = 1;
        }

        for (int xo = -pillarWidth; xo <= pillarWidth; xo++) {
            for (int zo = -pillarWidth; zo <= pillarWidth; zo++) {
                BlockPos cursor = origin.offset(xo, -1, zo);
                int runLength = 50;
                if (Math.abs(xo) == 1 && Math.abs(zo) == 1) {
                    runLength = random.nextInt(5);
                }

                while (cursor.getY() > 50) {
                    BlockState state = level.getBlockState(cursor);
                    if (!state.isAir() && !config.canReplace().test(level, cursor) && state != config.state()) {
                        break;
                    }

                    this.setBlock(level, cursor, config.state());
                    cursor = cursor.below();
                    if (--runLength <= 0) {
                        cursor = cursor.below(random.nextInt(5) + 1);
                        runLength = random.nextInt(5);
                    }
                }
            }
        }

        return true;
    }
}