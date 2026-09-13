package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.CountConfiguration;

public class SeaPickleFeature extends Feature<CountConfiguration> {
    public SeaPickleFeature(final Codec<CountConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<CountConfiguration> context) {
        int placed = 0;
        RandomSource random = context.random();
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        int count = context.config().count().sample(random);

        for (int i = 0; i < count; i++) {
            int x = random.nextInt(8) - random.nextInt(8);
            int z = random.nextInt(8) - random.nextInt(8);
            int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR, origin.getX() + x, origin.getZ() + z);
            BlockPos picklePos = new BlockPos(origin.getX() + x, y, origin.getZ() + z);
            BlockState pickleState = Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES, random.nextInt(4) + 1);
            if (level.getBlockState(picklePos).is(Blocks.WATER) && pickleState.canSurvive(level, picklePos)) {
                level.setBlock(picklePos, pickleState, 2);
                placed++;
            }
        }

        return placed > 0;
    }
}