package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class EndPlatformFeature extends Feature<NoneFeatureConfiguration> {
    public EndPlatformFeature(final Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<NoneFeatureConfiguration> context) {
        createEndPlatform(context.level(), context.origin(), false);
        return true;
    }

    public static void createEndPlatform(final ServerLevelAccessor newLevel, final BlockPos origin, final boolean dropResources) {
        BlockPos.MutableBlockPos pos = origin.mutable();

        for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -1; dy < 3; dy++) {
                    BlockPos blockPos = pos.set(origin).move(dx, dy, dz);
                    Block block = dy == -1 ? Blocks.OBSIDIAN : Blocks.AIR;
                    if (!newLevel.getBlockState(blockPos).is(block)) {
                        if (dropResources) {
                            newLevel.destroyBlock(blockPos, true, null);
                        }

                        newLevel.setBlock(blockPos, block.defaultBlockState(), 3);
                    }
                }
            }
        }
    }
}