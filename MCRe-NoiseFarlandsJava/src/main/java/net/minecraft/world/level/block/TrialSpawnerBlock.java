package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

public class TrialSpawnerBlock extends BaseEntityBlock {
    public static final MapCodec<TrialSpawnerBlock> CODEC = simpleCodec(TrialSpawnerBlock::new);
    public static final EnumProperty<TrialSpawnerState> STATE = BlockStateProperties.TRIAL_SPAWNER_STATE;
    public static final BooleanProperty OMINOUS = BlockStateProperties.OMINOUS;

    @Override
    public MapCodec<TrialSpawnerBlock> codec() {
        return CODEC;
    }

    public TrialSpawnerBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(STATE, TrialSpawnerState.INACTIVE).setValue(OMINOUS, false));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STATE, OMINOUS);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
        return new TrialSpawnerBlockEntity(worldPosition, blockState);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState blockState, final BlockEntityType<T> type) {
        return level instanceof ServerLevel serverLevel
            ? createTickerHelper(
                type,
                BlockEntityTypes.TRIAL_SPAWNER,
                (innerLevel, pos, state, entity) -> entity.getTrialSpawner()
                    .tickServer(serverLevel, pos, state.getOptionalValue(BlockStateProperties.OMINOUS).orElse(false))
            )
            : createTickerHelper(
                type,
                BlockEntityTypes.TRIAL_SPAWNER,
                (innerLevel, pos, state, entity) -> entity.getTrialSpawner()
                    .tickClient(innerLevel, pos, state.getOptionalValue(BlockStateProperties.OMINOUS).orElse(false))
            );
    }
}