package net.minecraft.world.level.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class WeightedPressurePlateBlock extends BasePressurePlateBlock {
    public static final MapCodec<WeightedPressurePlateBlock> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
                Codec.intRange(1, 1024).fieldOf("max_weight").forGetter(b -> b.maxWeight),
                BlockSetType.CODEC.fieldOf("block_set_type").forGetter(b -> b.type),
                propertiesCodec()
            )
            .apply(i, WeightedPressurePlateBlock::new)
    );
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    private final int maxWeight;

    @Override
    public MapCodec<WeightedPressurePlateBlock> codec() {
        return CODEC;
    }

    protected WeightedPressurePlateBlock(final int maxWeight, final BlockSetType type, final BlockBehaviour.Properties properties) {
        super(properties, type);
        this.registerDefaultState(this.stateDefinition.any().setValue(POWER, 0));
        this.maxWeight = maxWeight;
    }

    @Override
    protected int getSignalStrength(final Level level, final BlockPos pos) {
        int count = Math.min(getEntityCount(level, TOUCH_AABB.move(pos), Entity.class), this.maxWeight);
        if (count > 0) {
            float percent = (float)Math.min(this.maxWeight, count) / this.maxWeight;
            return Mth.ceil(percent * 15.0F);
        } else {
            return 0;
        }
    }

    @Override
    protected int getSignalForState(final BlockState state) {
        return state.getValue(POWER);
    }

    @Override
    protected BlockState setSignalForState(final BlockState state, final int signal) {
        return state.setValue(POWER, signal);
    }

    @Override
    protected int getPressedTime() {
        return 10;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER);
    }
}