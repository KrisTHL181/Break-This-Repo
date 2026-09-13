package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.sounds.AmbientDesertBlockSoundsPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ShortDryGrassBlock extends DryVegetationBlock implements BonemealableBlock {
    public static final MapCodec<ShortDryGrassBlock> CODEC = simpleCodec(ShortDryGrassBlock::new);
    private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 10.0);

    @Override
    public MapCodec<ShortDryGrassBlock> codec() {
        return CODEC;
    }

    protected ShortDryGrassBlock(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
        AmbientDesertBlockSoundsPlayer.playAmbientDryGrassSounds(level, pos, random);
    }

    @Override
    public boolean isValidBonemealTarget(final LevelReader level, final BlockPos pos, final BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(final Level level, final RandomSource random, final BlockPos pos, final BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(final ServerLevel level, final RandomSource random, final BlockPos pos, final BlockState state) {
        level.setBlockAndUpdate(pos, Blocks.TALL_DRY_GRASS.defaultBlockState());
    }
}