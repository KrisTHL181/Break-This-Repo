package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class CoralMushroomFeature extends CoralFeature {
    public CoralMushroomFeature(final Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    protected boolean placeFeature(final LevelAccessor level, final RandomSource random, final BlockPos origin, final BlockState state) {
        int height = random.nextInt(3) + 3;
        int width = random.nextInt(3) + 3;
        int length = random.nextInt(3) + 3;
        int sinkValue = random.nextInt(3) + 1;
        BlockPos.MutableBlockPos mutPos = origin.mutable();

        for (int x = 0; x <= width; x++) {
            for (int y = 0; y <= height; y++) {
                for (int z = 0; z <= length; z++) {
                    mutPos.set(x + origin.getX(), y + origin.getY(), z + origin.getZ());
                    mutPos.move(Direction.DOWN, sinkValue);
                    if ((x != 0 && x != width || y != 0 && y != height)
                        && (z != 0 && z != length || y != 0 && y != height)
                        && (x != 0 && x != width || z != 0 && z != length)
                        && (x == 0 || x == width || y == 0 || y == height || z == 0 || z == length)
                        && !(random.nextFloat() < 0.1F)
                        && !this.placeCoralBlock(level, random, mutPos, state)) {
                    }
                }
            }
        }

        return true;
    }
}