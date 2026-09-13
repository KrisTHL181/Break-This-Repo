package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TorchflowerCropBlock extends CropBlock {
    public static final MapCodec<TorchflowerCropBlock> CODEC = simpleCodec(TorchflowerCropBlock::new);
    public static final int MAX_AGE = 1;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_1;
    private static final VoxelShape[] SHAPES = Block.boxes(1, age -> Block.column(6.0, 0.0, 6 + age * 4));
    private static final int BONEMEAL_INCREASE = 1;

    @Override
    public MapCodec<TorchflowerCropBlock> codec() {
        return CODEC;
    }

    public TorchflowerCropBlock(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SHAPES[this.getAge(state)];
    }

    @Override
    protected IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return 2;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return Items.TORCHFLOWER_SEEDS;
    }

    @Override
    public BlockState getStateForAge(final int age) {
        return age == 2 ? Blocks.TORCHFLOWER.defaultBlockState() : super.getStateForAge(age);
    }

    @Override
    public void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        if (random.nextInt(3) != 0) {
            super.randomTick(state, level, pos, random);
        }
    }

    @Override
    protected int getBonemealAgeIncrease(final Level level) {
        return 1;
    }
}