package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HangingMossBlock extends Block implements BonemealableBlock {
   private static final VoxelShape SHAPE_BASE = Block.column(14.0, 0.0, 16.0);
   private static final VoxelShape SHAPE_TIP = Block.column(14.0, 2.0, 16.0);
   public static final BooleanProperty TIP = BlockStateProperties.TIP;

   public HangingMossBlock(final BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(TIP, true));
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return state.getValue(TIP) ? SHAPE_TIP : SHAPE_BASE;
   }

   @Override
   public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
      if (random.nextInt(500) == 0) {
         BlockState above = level.getBlockState(pos.above());
         if (above.is(BlockTags.PALE_OAK_LOGS) || above.is(Blocks.PALE_OAK_LEAVES)) {
            level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.PALE_HANGING_MOSS_IDLE, SoundSource.AMBIENT, 1.0F, 1.0F, false);
         }
      }
   }

   @Override
   protected boolean propagatesSkylightDown(final BlockState state) {
      return true;
   }

   @Override
   protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
      return this.canStayAtPosition(level, pos);
   }

   private boolean canStayAtPosition(final BlockGetter level, final BlockPos pos) {
      BlockPos neighbourPos = pos.relative(Direction.UP);
      BlockState blockState = level.getBlockState(neighbourPos);
      return MultifaceBlock.canAttachTo(level, Direction.UP, neighbourPos, blockState) || blockState.is(this);
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
      if (!this.canStayAtPosition(level, pos)) {
         ticks.scheduleTick(pos, this, 1);
      }

      return state.setValue(TIP, !level.getBlockState(pos.below()).is(this));
   }

   @Override
   protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
      if (!this.canStayAtPosition(level, pos)) {
         level.destroyBlock(pos, true);
      }
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(TIP);
   }

   @Override
   public boolean isValidBonemealTarget(final LevelReader level, final BlockPos pos, final BlockState state) {
      BlockPos growPos = this.getTip(level, pos).below();
      return this.canGrowInto(level.getBlockState(growPos)) && level.isInsideBuildHeight(growPos);
   }

   private boolean canGrowInto(final BlockState state) {
      return state.isAir();
   }

   public BlockPos getTip(final BlockGetter level, final BlockPos pos) {
      BlockPos.MutableBlockPos forwardPos = pos.mutable();

      BlockState forwardState;
      do {
         forwardPos.move(Direction.DOWN);
         forwardState = level.getBlockState(forwardPos);
      } while (forwardState.is(this));

      return forwardPos.relative(Direction.UP).immutable();
   }

   @Override
   public boolean isBonemealSuccess(final Level level, final RandomSource random, final BlockPos pos, final BlockState state) {
      return true;
   }

   @Override
   public void performBonemeal(final ServerLevel level, final RandomSource random, final BlockPos pos, final BlockState state) {
      BlockPos tipPos = this.getTip(level, pos).below();
      if (this.canGrowInto(level.getBlockState(tipPos))) {
         level.setBlockAndUpdate(tipPos, state.setValue(TIP, true));
      }
   }
}
