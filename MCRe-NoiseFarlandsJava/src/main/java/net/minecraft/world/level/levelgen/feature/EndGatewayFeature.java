package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.levelgen.feature.configurations.EndGatewayConfiguration;

public class EndGatewayFeature extends Feature<EndGatewayConfiguration> {
    public EndGatewayFeature(final Codec<EndGatewayConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<EndGatewayConfiguration> context) {
        BlockPos origin = context.origin();
        WorldGenLevel level = context.level();
        EndGatewayConfiguration config = context.config();

        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-1, -2, -1), origin.offset(1, 2, 1))) {
            boolean sameX = pos.getX() == origin.getX();
            boolean sameY = pos.getY() == origin.getY();
            boolean sameZ = pos.getZ() == origin.getZ();
            boolean end = Math.abs(pos.getY() - origin.getY()) == 2;
            if (sameX && sameY && sameZ) {
                BlockPos immutable = pos.immutable();
                this.setBlock(level, immutable, Blocks.END_GATEWAY.defaultBlockState());
                config.getExit().ifPresent(targetPos -> {
                    if (level.getBlockEntity(immutable) instanceof TheEndGatewayBlockEntity exitGateway) {
                        exitGateway.setExitPosition(targetPos, config.isExitExact());
                    }
                });
            } else if (sameY) {
                this.setBlock(level, pos, Blocks.AIR.defaultBlockState());
            } else if (end && sameX && sameZ) {
                this.setBlock(level, pos, Blocks.BEDROCK.defaultBlockState());
            } else if ((sameX || sameZ) && !end) {
                this.setBlock(level, pos, Blocks.BEDROCK.defaultBlockState());
            } else {
                this.setBlock(level, pos, Blocks.AIR.defaultBlockState());
            }
        }

        return true;
    }
}