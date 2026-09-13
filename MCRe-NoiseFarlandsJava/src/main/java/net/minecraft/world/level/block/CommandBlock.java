package net.minecraft.world.level.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class CommandBlock extends BaseEntityBlock implements GameMasterBlock {
    public static final MapCodec<CommandBlock> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(Codec.BOOL.fieldOf("automatic").forGetter(b -> b.automatic), propertiesCodec()).apply(i, CommandBlock::new)
    );
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
    public static final BooleanProperty CONDITIONAL = BlockStateProperties.CONDITIONAL;
    private final boolean automatic;

    @Override
    public MapCodec<CommandBlock> codec() {
        return CODEC;
    }

    public CommandBlock(final boolean automatic, final BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CONDITIONAL, false));
        this.automatic = automatic;
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
        CommandBlockEntity blockEntity = new CommandBlockEntity(worldPosition, blockState);
        blockEntity.setAutomatic(this.automatic);
        return blockEntity;
    }

    @Override
    protected void neighborChanged(
        final BlockState state, final Level level, final BlockPos pos, final Block block, final @Nullable Orientation orientation, final boolean movedByPiston
    ) {
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof CommandBlockEntity commandBlock) {
                this.setPoweredAndUpdate(level, pos, commandBlock, level.hasNeighborSignal(pos));
            }
        }
    }

    private void setPoweredAndUpdate(final Level level, final BlockPos pos, final CommandBlockEntity commandBlock, final boolean isPowered) {
        boolean wasPowered = commandBlock.isPowered();
        if (isPowered != wasPowered) {
            commandBlock.setPowered(isPowered);
            if (isPowered) {
                if (commandBlock.isAutomatic() || commandBlock.getMode() == CommandBlockEntity.Mode.SEQUENCE) {
                    return;
                }

                commandBlock.markConditionMet();
                level.scheduleTick(pos, this, 1);
            }
        }
    }

    @Override
    protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        if (level.getBlockEntity(pos) instanceof CommandBlockEntity commandBlock) {
            BaseCommandBlock baseCommandBlock = commandBlock.getCommandBlock();
            boolean commandSet = !StringUtil.isNullOrEmpty(baseCommandBlock.getCommand());
            CommandBlockEntity.Mode mode = commandBlock.getMode();
            boolean wasConditionMet = commandBlock.wasConditionMet();
            if (mode == CommandBlockEntity.Mode.AUTO) {
                commandBlock.markConditionMet();
                if (wasConditionMet) {
                    this.execute(state, level, pos, baseCommandBlock, commandSet);
                } else if (commandBlock.isConditional()) {
                    baseCommandBlock.setSuccessCount(0);
                }

                if (commandBlock.isPowered() || commandBlock.isAutomatic()) {
                    level.scheduleTick(pos, this, 1);
                }
            } else if (mode == CommandBlockEntity.Mode.REDSTONE) {
                if (wasConditionMet) {
                    this.execute(state, level, pos, baseCommandBlock, commandSet);
                } else if (commandBlock.isConditional()) {
                    baseCommandBlock.setSuccessCount(0);
                }
            }

            level.updateNeighbourForOutputSignal(pos, this);
        }
    }

    private void execute(final BlockState state, final ServerLevel level, final BlockPos pos, final BaseCommandBlock baseCommandBlock, final boolean commandSet) {
        if (commandSet) {
            baseCommandBlock.performCommand(level);
        } else {
            baseCommandBlock.setSuccessCount(0);
        }

        executeChain(level, pos, state.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(
        final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult
    ) {
        if (level.getBlockEntity(pos) instanceof CommandBlockEntity commandBlockEntity && player.canUseGameMasterBlocks()) {
            player.openCommandBlock(commandBlockEntity);
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(final BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos, final Direction direction) {
        return level.getBlockEntity(pos) instanceof CommandBlockEntity commandBlockEntity ? commandBlockEntity.getCommandBlock().getSuccessCount() : 0;
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state, final @Nullable LivingEntity by, final ItemStack itemStack) {
        if (level.getBlockEntity(pos) instanceof CommandBlockEntity commandBlockEntity) {
            BaseCommandBlock commandBlock = commandBlockEntity.getCommandBlock();
            if (level instanceof ServerLevel serverLevel) {
                if (!itemStack.has(DataComponents.BLOCK_ENTITY_DATA)) {
                    commandBlock.setTrackOutput(serverLevel.getGameRules().get(GameRules.SEND_COMMAND_FEEDBACK));
                    commandBlockEntity.setAutomatic(this.automatic);
                }

                boolean hasNeighborSignal = level.hasNeighborSignal(pos);
                this.setPoweredAndUpdate(level, pos, commandBlockEntity, hasNeighborSignal);
            }
        }
    }

    @Override
    protected BlockState rotate(final BlockState state, final Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(final BlockState state, final Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CONDITIONAL);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    private static void executeChain(final ServerLevel level, final BlockPos blockPos, Direction direction) {
        BlockPos.MutableBlockPos pos = blockPos.mutable();
        GameRules gameRules = level.getGameRules();
        int maxIterations = gameRules.get(GameRules.MAX_COMMAND_SEQUENCE_LENGTH);

        while (maxIterations-- > 0) {
            pos.move(direction);
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            if (!state.is(Blocks.CHAIN_COMMAND_BLOCK)
                || !(level.getBlockEntity(pos) instanceof CommandBlockEntity commandBlock)
                || commandBlock.getMode() != CommandBlockEntity.Mode.SEQUENCE) {
                break;
            }

            if (commandBlock.isPowered() || commandBlock.isAutomatic()) {
                BaseCommandBlock baseCommandBlock = commandBlock.getCommandBlock();
                if (commandBlock.markConditionMet()) {
                    if (!baseCommandBlock.performCommand(level)) {
                        break;
                    }

                    level.updateNeighbourForOutputSignal(pos, block);
                } else if (commandBlock.isConditional()) {
                    baseCommandBlock.setSuccessCount(0);
                }
            }

            direction = state.getValue(FACING);
        }

        if (maxIterations <= 0) {
            int limit = Math.max(gameRules.get(GameRules.MAX_COMMAND_SEQUENCE_LENGTH), 0);
            LOGGER.warn("Command Block chain tried to execute more than {} steps!", limit);
        }
    }
}