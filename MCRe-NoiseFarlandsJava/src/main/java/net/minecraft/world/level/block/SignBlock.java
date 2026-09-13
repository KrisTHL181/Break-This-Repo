package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Arrays;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignApplicator;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class SignBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.column(8.0, 0.0, 16.0);
    private final WoodType type;

    protected SignBlock(final WoodType type, final BlockBehaviour.Properties properties) {
        super(properties);
        this.type = type;
    }

    @Override
    protected abstract MapCodec<? extends SignBlock> codec();

    @Override
    protected BlockState updateShape(
        final BlockState state,
        final LevelReader level,
        final ScheduledTickAccess ticks,
        final BlockPos pos,
        final Direction directionToNeighbour,
        final BlockPos neighbourPos,
        final BlockState neighbourState,
        final RandomSource random
    ) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean isPossibleToRespawnInThis(final BlockState state) {
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
        return new SignBlockEntity(worldPosition, blockState);
    }

    @Override
    protected InteractionResult useItemOn(
        final ItemStack itemStack,
        final BlockState state,
        final Level level,
        final BlockPos pos,
        final Player player,
        final InteractionHand hand,
        final BlockHitResult hitResult
    ) {
        if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            SignApplicator signApplicator = itemStack.getItem() instanceof SignApplicator applicator ? applicator : null;
            boolean hasApplicatorToUse = signApplicator != null && player.mayBuild();
            if (level instanceof ServerLevel serverLevel) {
                if (hasApplicatorToUse && !sign.isWaxed() && !this.otherPlayerIsEditingSign(player, sign)) {
                    boolean isFrontText = sign.isFacingFrontText(player);
                    if (signApplicator.canApplyToSign(sign.getText(isFrontText), itemStack, player)
                        && signApplicator.tryApplyToSign(serverLevel, sign, isFrontText, itemStack, player)) {
                        sign.executeClickCommandsIfPresent(serverLevel, player, pos, isFrontText);
                        player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
                        serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, sign.getBlockPos(), GameEvent.Context.of(player, sign.getBlockState()));
                        itemStack.consume(1, player);
                        return InteractionResult.SUCCESS;
                    } else {
                        return InteractionResult.TRY_WITH_EMPTY_HAND;
                    }
                } else {
                    return InteractionResult.TRY_WITH_EMPTY_HAND;
                }
            } else {
                return !hasApplicatorToUse && !sign.isWaxed() ? InteractionResult.CONSUME : InteractionResult.SUCCESS;
            }
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    protected InteractionResult useWithoutItem(
        final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult
    ) {
        if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            if (level instanceof ServerLevel serverLevel) {
                boolean isFrontText = sign.isFacingFrontText(player);
                boolean executedClickCommand = sign.executeClickCommandsIfPresent(serverLevel, player, pos, isFrontText);
                if (sign.isWaxed()) {
                    serverLevel.playSound(null, sign.getBlockPos(), sign.getSignInteractionFailedSoundEvent(), SoundSource.BLOCKS);
                    return InteractionResult.SUCCESS_SERVER;
                } else if (executedClickCommand) {
                    return InteractionResult.SUCCESS_SERVER;
                } else if (!this.otherPlayerIsEditingSign(player, sign) && player.mayBuild() && this.hasEditableText(player, sign, isFrontText)) {
                    this.openTextEdit(player, sign, isFrontText);
                    return InteractionResult.SUCCESS_SERVER;
                } else {
                    return InteractionResult.PASS;
                }
            } else {
                Util.pauseInIde(new IllegalStateException("Expected to only call this on server"));
                return InteractionResult.CONSUME;
            }
        } else {
            return InteractionResult.PASS;
        }
    }

    private boolean hasEditableText(final Player player, final SignBlockEntity sign, final boolean isFrontText) {
        SignText text = sign.getText(isFrontText);
        return Arrays.stream(text.getMessages(player.isTextFilteringEnabled()))
            .allMatch(message -> message.equals(CommonComponents.EMPTY) || message.getContents() instanceof PlainTextContents);
    }

    public abstract float getYRotationDegrees(final BlockState state);

    public Vec3 getSignHitboxCenterPosition(final BlockState state) {
        return new Vec3(0.5, 0.5, 0.5);
    }

    @Override
    protected FluidState getFluidState(final BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    public WoodType type() {
        return this.type;
    }

    public static WoodType getWoodType(final Block block) {
        return block instanceof SignBlock signBlock ? signBlock.type() : WoodType.OAK;
    }

    public void openTextEdit(final Player player, final SignBlockEntity sign, final boolean isFrontText) {
        sign.setAllowedPlayerEditor(player.getUUID());
        player.openTextEdit(sign, isFrontText);
    }

    private boolean otherPlayerIsEditingSign(final Player player, final SignBlockEntity sign) {
        UUID playerWhoMayEdit = sign.getPlayerWhoMayEdit();
        return playerWhoMayEdit != null && !playerWhoMayEdit.equals(player.getUUID());
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState blockState, final BlockEntityType<T> type) {
        return createTickerHelper(type, BlockEntityTypes.SIGN, SignBlockEntity::tick);
    }
}