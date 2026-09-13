package net.minecraft.world.level.block;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CandleBlock extends AbstractCandleBlock implements SimpleWaterloggedBlock {
   public static final int MIN_CANDLES = 1;
   public static final int MAX_CANDLES = 4;
   public static final IntegerProperty CANDLES = BlockStateProperties.CANDLES;
   public static final BooleanProperty LIT = AbstractCandleBlock.LIT;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final ToIntFunction<BlockState> LIGHT_EMISSION = state -> state.getValue(LIT) ? 3 * state.getValue(CANDLES) : 0;
   private static final Int2ObjectMap<List<Vec3>> PARTICLE_OFFSETS = Util.make(
      new Int2ObjectOpenHashMap(4),
      map -> {
         float s = 0.0625F;
         map.put(1, List.of(new Vec3(8.0, 8.0, 8.0).scale(0.0625)));
         map.put(2, List.of(new Vec3(6.0, 7.0, 8.0).scale(0.0625), new Vec3(10.0, 8.0, 7.0).scale(0.0625)));
         map.put(3, List.of(new Vec3(8.0, 5.0, 10.0).scale(0.0625), new Vec3(6.0, 7.0, 8.0).scale(0.0625), new Vec3(9.0, 8.0, 7.0).scale(0.0625)));
         map.put(
            4,
            List.of(
               new Vec3(7.0, 5.0, 9.0).scale(0.0625),
               new Vec3(10.0, 7.0, 9.0).scale(0.0625),
               new Vec3(6.0, 7.0, 6.0).scale(0.0625),
               new Vec3(9.0, 8.0, 6.0).scale(0.0625)
            )
         );
      }
   );
   private static final VoxelShape[] SHAPES = new VoxelShape[]{
      Block.column(2.0, 0.0, 6.0),
      Block.box(5.0, 0.0, 6.0, 11.0, 6.0, 9.0),
      Block.box(5.0, 0.0, 6.0, 10.0, 6.0, 11.0),
      Block.box(5.0, 0.0, 5.0, 11.0, 6.0, 10.0)
   };

   public CandleBlock(final BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(CANDLES, 1).setValue(LIT, false).setValue(WATERLOGGED, false));
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
      if (itemStack.isEmpty() && player.getAbilities().mayBuild && state.getValue(LIT)) {
         extinguish(player, state, level, pos);
         return InteractionResult.SUCCESS;
      } else {
         return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
      }
   }

   @Override
   protected boolean canBeReplaced(final BlockState state, final BlockPlaceContext context) {
      return !context.isSecondaryUseActive() && context.getItemInHand().getItem() == this.asItem() && state.getValue(CANDLES) < 4
         ? true
         : super.canBeReplaced(state, context);
   }

   @Override
   public BlockState getStateForPlacement(final BlockPlaceContext context) {
      BlockState state = context.getLevel().getBlockState(context.getClickedPos());
      if (state.is(this)) {
         return state.cycle(CANDLES);
      }

      FluidState replacedFluidState = context.getLevel().getFluidState(context.getClickedPos());
      boolean isWaterSource = replacedFluidState.is(Fluids.WATER);
      return super.getStateForPlacement(context).setValue(WATERLOGGED, isWaterSource);
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
      if (state.getValue(WATERLOGGED)) {
         ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
      }

      return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
   }

   @Override
   protected FluidState getFluidState(final BlockState state) {
      return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return SHAPES[state.getValue(CANDLES) - 1];
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(CANDLES, LIT, WATERLOGGED);
   }

   @Override
   public boolean placeLiquid(final LevelAccessor level, final BlockPos pos, final BlockState state, final FluidState fluidState) {
      if (!state.getValue(WATERLOGGED) && fluidState.is(Fluids.WATER)) {
         BlockState newState = state.setValue(WATERLOGGED, true);
         if (state.getValue(LIT)) {
            extinguish(null, newState, level, pos);
         } else {
            level.setBlockAndUpdate(pos, newState);
         }

         level.scheduleTick(pos, fluidState.getType(), fluidState.getType().getTickDelay(level));
         return true;
      } else {
         return false;
      }
   }

   public static boolean canLight(final BlockState state) {
      return state.is(BlockTags.CANDLES, s -> s.hasProperty(LIT) && s.hasProperty(WATERLOGGED)) && !state.getValue(LIT) && !state.getValue(WATERLOGGED);
   }

   @Override
   protected Iterable<Vec3> getParticleOffsets(final BlockState state) {
      return (Iterable<Vec3>)PARTICLE_OFFSETS.get(state.getValue(CANDLES));
   }

   @Override
   protected boolean canBeLit(final BlockState state) {
      return !state.getValue(WATERLOGGED) && super.canBeLit(state);
   }

   @Override
   protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
      return Block.canSupportCenter(level, pos.below(), Direction.UP);
   }
}
