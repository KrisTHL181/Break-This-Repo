package net.minecraft.world.level.levelgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class EndPodiumFeature extends Feature<NoneFeatureConfiguration> {
    public static final int PODIUM_RADIUS = 4;
    public static final int PODIUM_PILLAR_HEIGHT = 4;
    public static final int RIM_RADIUS = 1;
    public static final float CORNER_ROUNDING = 0.5F;
    private static final BlockPos END_PODIUM_LOCATION = BlockPos.ZERO;
    private final boolean active;

    public static BlockPos getLocation(final BlockPos offset) {
        return END_PODIUM_LOCATION.offset(offset);
    }

    public EndPodiumFeature(final boolean active) {
        super(NoneFeatureConfiguration.CODEC);
        this.active = active;
    }

    @Override
    public boolean place(final FeaturePlaceContext<NoneFeatureConfiguration> context) {
        BlockPos origin = context.origin();
        WorldGenLevel level = context.level();

        for (BlockPos pos : BlockPos.betweenClosed(
            new BlockPos(origin.getX() - 4, origin.getY() - 1, origin.getZ() - 4), new BlockPos(origin.getX() + 4, origin.getY() + 32, origin.getZ() + 4)
        )) {
            boolean insideRim = pos.closerThan(origin, 2.5);
            if (insideRim || pos.closerThan(origin, 3.5)) {
                if (pos.getY() < origin.getY()) {
                    if (insideRim) {
                        this.setBlock(level, pos, Blocks.BEDROCK.defaultBlockState());
                    } else if (pos.getY() < origin.getY()) {
                        if (this.active) {
                            this.dropPreviousAndSetBlock(level, pos, Blocks.END_STONE);
                        } else {
                            this.setBlock(level, pos, Blocks.END_STONE.defaultBlockState());
                        }
                    }
                } else if (pos.getY() > origin.getY()) {
                    if (this.active) {
                        this.dropPreviousAndSetBlock(level, pos, Blocks.AIR);
                    } else {
                        this.setBlock(level, pos, Blocks.AIR.defaultBlockState());
                    }
                } else if (!insideRim) {
                    this.setBlock(level, pos, Blocks.BEDROCK.defaultBlockState());
                } else if (this.active) {
                    this.dropPreviousAndSetBlock(level, new BlockPos(pos), Blocks.END_PORTAL);
                } else {
                    this.setBlock(level, new BlockPos(pos), Blocks.AIR.defaultBlockState());
                }
            }
        }

        for (int y = 0; y < 4; y++) {
            this.setBlock(level, origin.above(y), Blocks.BEDROCK.defaultBlockState());
        }

        BlockPos centerOfPillar = origin.above(2);

        for (Direction face : Direction.Plane.HORIZONTAL) {
            this.setBlock(level, centerOfPillar.relative(face), Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, face));
        }

        return true;
    }

    private void dropPreviousAndSetBlock(final WorldGenLevel level, final BlockPos pos, final Block block) {
        if (!level.getBlockState(pos).is(block)) {
            level.destroyBlock(pos, true, null);
            this.setBlock(level, pos, block.defaultBlockState());
        }
    }
}