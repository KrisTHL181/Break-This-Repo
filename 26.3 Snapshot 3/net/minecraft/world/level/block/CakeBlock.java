package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CakeBlock extends Block {
   public static final int MAX_BITES = 6;
   public static final IntegerProperty BITES = BlockStateProperties.BITES;
   public static final int FULL_CAKE_SIGNAL = getOutputSignal(0);
   private static final VoxelShape[] SHAPES = Block.boxes(6, bite -> Block.box(1 + bite * 2, 0.0, 1.0, 15.0, 8.0, 15.0));

   protected CakeBlock(final BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(BITES, 0));
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return SHAPES[state.getValue(BITES)];
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
      Item item = itemStack.getItem();
      if (itemStack.is(ItemTags.CANDLES) && state.getValue(BITES) == 0 && Block.byItem(item) instanceof CandleBlock candleBlock) {
         itemStack.consume(1, player);
         level.playSound(null, pos, SoundEvents.CAKE_ADD_CANDLE, SoundSource.BLOCKS, 1.0F, 1.0F);
         level.setBlockAndUpdate(pos, CandleCakeBlock.byCandle(candleBlock));
         level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
         player.awardStat(Stats.ITEM_USED.get(item));
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      }
   }

   @Override
   protected InteractionResult useWithoutItem(
      final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult
   ) {
      if (level.isClientSide()) {
         if (eat(level, pos, state, player).consumesAction()) {
            return InteractionResult.SUCCESS;
         }

         if (player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
            return InteractionResult.CONSUME;
         }
      }

      return eat(level, pos, state, player);
   }

   protected static InteractionResult eat(final LevelAccessor level, final BlockPos pos, final BlockState state, final Player player) {
      if (!player.canEat(false)) {
         return InteractionResult.PASS;
      }

      player.awardStat(Stats.EAT_CAKE_SLICE);
      player.getFoodData().eat(2, 0.1F);
      int bites = state.getValue(BITES);
      level.gameEvent(player, GameEvent.EAT, pos);
      if (bites < 6) {
         level.setBlockAndUpdate(pos, state.setValue(BITES, bites + 1));
      } else {
         level.removeBlock(pos, false);
         level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
      }

      return InteractionResult.SUCCESS;
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
      return directionToNeighbour == Direction.DOWN && !state.canSurvive(level, pos)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
   }

   @Override
   protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
      return level.getBlockState(pos.below()).isSolid();
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(BITES);
   }

   @Override
   protected int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos, final Direction direction) {
      return getOutputSignal(state.getValue(BITES));
   }

   public static int getOutputSignal(final int bitesTaken) {
      return (7 - bitesTaken) * 2;
   }

   @Override
   protected boolean hasAnalogOutputSignal(final BlockState state) {
      return true;
   }

   @Override
   protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
      return false;
   }
}
