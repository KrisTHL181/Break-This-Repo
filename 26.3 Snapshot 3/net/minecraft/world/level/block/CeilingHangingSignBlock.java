package net.minecraft.world.level.block;

import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class CeilingHangingSignBlock extends SignBlock implements HangingSignBlock {
   public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;
   public static final BooleanProperty ATTACHED = BlockStateProperties.ATTACHED;
   private static final VoxelShape SHAPE_DEFAULT = Block.column(10.0, 0.0, 16.0);
   private static final Map<Integer, VoxelShape> SHAPES = Shapes.rotateHorizontal(Block.column(14.0, 2.0, 0.0, 10.0))
      .entrySet()
      .stream()
      .collect(Collectors.toMap(e -> RotationSegment.convertToSegment(e.getKey()), Entry::getValue));

   public CeilingHangingSignBlock(final WoodType type, final BlockBehaviour.Properties properties) {
      super(type, properties.sound(type.hangingSignSoundType()));
      this.registerDefaultState(this.stateDefinition.any().setValue(ROTATION, 8).setValue(ATTACHED, false).setValue(WATERLOGGED, false));
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
      return level.getBlockEntity(pos) instanceof SignBlockEntity signEntity
            && this.shouldTryToChainAnotherHangingSign(player, hitResult, signEntity, itemStack)
         ? InteractionResult.PASS
         : super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
   }

   private boolean shouldTryToChainAnotherHangingSign(
      final Player player, final BlockHitResult hitResult, final SignBlockEntity signEntity, final ItemStack itemStack
   ) {
      return !signEntity.canExecuteClickCommands(signEntity.isFacingFrontText(player), player)
         && itemStack.getItem() instanceof HangingSignItem
         && hitResult.getDirection().equals(Direction.DOWN);
   }

   @Override
   protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
      return level.getBlockState(pos.above()).isFaceSturdy(level, pos.above(), Direction.DOWN, SupportType.CENTER);
   }

   @Override
   public BlockState getStateForPlacement(final BlockPlaceContext context) {
      Level level = context.getLevel();
      FluidState replacedFluidState = level.getFluidState(context.getClickedPos());
      BlockPos above = context.getClickedPos().above();
      BlockState stateAbove = level.getBlockState(above);
      boolean isBelowHangingSign = stateAbove.is(BlockTags.ALL_HANGING_SIGNS);
      Direction direction = Direction.fromYRot(context.getRotation());
      boolean attachedToMiddle = !Block.isFaceFull(stateAbove.getCollisionShape(level, above), Direction.DOWN) || context.isSecondaryUseActive();
      if (isBelowHangingSign && !context.isSecondaryUseActive()) {
         if (stateAbove.hasProperty(WallHangingSignBlock.FACING)) {
            Direction aboveDirection = stateAbove.getValue(WallHangingSignBlock.FACING);
            if (aboveDirection.getAxis().test(direction)) {
               attachedToMiddle = false;
            }
         } else if (stateAbove.hasProperty(ROTATION)) {
            Optional<Direction> aboveDirection = RotationSegment.convertToDirection(stateAbove.getValue(ROTATION));
            if (aboveDirection.isPresent() && aboveDirection.get().getAxis().test(direction)) {
               attachedToMiddle = false;
            }
         }
      }

      int rotationSegment = !attachedToMiddle
         ? RotationSegment.convertToSegment(direction.getOpposite())
         : RotationSegment.convertToSegment(context.getRotation() + 180.0F);
      return this.defaultBlockState()
         .setValue(ATTACHED, attachedToMiddle)
         .setValue(ROTATION, rotationSegment)
         .setValue(WATERLOGGED, replacedFluidState.is(Fluids.WATER));
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return SHAPES.getOrDefault(state.getValue(ROTATION), SHAPE_DEFAULT);
   }

   @Override
   protected VoxelShape getBlockSupportShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
      return this.getShape(state, level, pos, CollisionContext.empty());
   }

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
      return directionToNeighbour == Direction.UP && !this.canSurvive(state, level, pos)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
   }

   @Override
   public float getYRotationDegrees(final BlockState state) {
      return RotationSegment.convertToDegrees(state.getValue(ROTATION));
   }

   @Override
   protected BlockState rotate(final BlockState state, final Rotation rotation) {
      return state.setValue(ROTATION, rotation.rotate(state.getValue(ROTATION), 16));
   }

   @Override
   protected BlockState mirror(final BlockState state, final Mirror mirror) {
      return state.setValue(ROTATION, mirror.mirror(state.getValue(ROTATION), 16));
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(ROTATION, ATTACHED, WATERLOGGED);
   }

   @Override
   public BlockEntity newBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      return new HangingSignBlockEntity(worldPosition, blockState);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState blockState, final BlockEntityType<T> type) {
      return createTickerHelper(type, BlockEntityTypes.HANGING_SIGN, SignBlockEntity::tick);
   }

   @Override
   public HangingSignBlock.Attachment attachmentPoint(final BlockState state) {
      return getAttachmentPoint(state.getValue(BlockStateProperties.ATTACHED));
   }

   public static HangingSignBlock.Attachment getAttachmentPoint(final boolean isAttached) {
      return isAttached ? HangingSignBlock.Attachment.CEILING_MIDDLE : HangingSignBlock.Attachment.CEILING;
   }
}
