package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.configurations.LayerConfiguration;

public class FillLayerFeature extends Feature<LayerConfiguration> {
    public FillLayerFeature(final Codec<LayerConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<LayerConfiguration> context) {
        BlockPos origin = context.origin();
        LayerConfiguration config = context.config();
        WorldGenLevel level = context.level();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int y = level.getMinY() + config.height;
                pos.set(x, y, z);
                if (level.getBlockState(pos).isAir()) {
                    level.setBlock(pos, config.state, 2);
                }
            }
        }

        return true;
    }
}