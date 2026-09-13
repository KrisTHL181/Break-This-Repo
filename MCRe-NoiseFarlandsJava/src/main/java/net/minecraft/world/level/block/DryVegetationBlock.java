package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.sounds.AmbientDesertBlockSoundsPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DryVegetationBlock extends VegetationBlock {
    public static final MapCodec<DryVegetationBlock> CODEC = simpleCodec(DryVegetationBlock::new);
    private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 13.0);

    @Override
    public MapCodec<? extends DryVegetationBlock> codec() {
        return CODEC;
    }

    protected DryVegetationBlock(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return state.is(BlockTags.SUPPORTS_DRY_VEGETATION);
    }

    @Override
    public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
        AmbientDesertBlockSoundsPlayer.playAmbientDeadBushSounds(level, pos, random);
    }
}