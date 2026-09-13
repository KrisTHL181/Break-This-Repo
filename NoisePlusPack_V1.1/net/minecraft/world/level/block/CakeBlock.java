package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
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
   public static final MapCodec<CakeBlock> CODEC = simpleCodec(CakeBlock::new);
   public static final int MAX_BITES = 6;
   public static final IntegerProperty BITES = BlockStateProperties.BITES;
   public static final int FULL_CAKE_SIGNAL = getOutputSignal(0);
   private static final VoxelShape[] SHAPES = Block.boxes(6, p_398001_ -> Block.box(1 + p_398001_ * 2, 0.0, 1.0, 15.0, 8.0, 15.0));

   @Override
   public MapCodec<CakeBlock> codec() {
      return CODEC;
   }

   protected CakeBlock(BlockBehaviour.Properties p_51184_) {
      super(p_51184_);
      this.registerDefaultState(this.stateDefinition.any().setValue(BITES, 0));
   }

   @Override
   protected VoxelShape getShape(BlockState p_51222_, BlockGetter p_51223_, BlockPos p_51224_, CollisionContext p_51225_) {
      return SHAPES[p_51222_.getValue(BITES)];
   }

   @Override
   protected InteractionResult useItemOn(
      ItemStack p_332983_, BlockState p_333266_, Level p_328017_, BlockPos p_332811_, Player p_327926_, InteractionHand p_330281_, BlockHitResult p_332277_
   ) {
      Item item = p_332983_.getItem();
      if (p_332983_.is(ItemTags.CANDLES) && p_333266_.getValue(BITES) == 0 && Block.byItem(item) instanceof CandleBlock candleblock) {
         p_332983_.consume(1, p_327926_);
         p_328017_.playSound(null, p_332811_, SoundEvents.CAKE_ADD_CANDLE, SoundSource.BLOCKS, 1.0F, 1.0F);
         p_328017_.setBlockAndUpdate(p_332811_, CandleCakeBlock.byCandle(candleblock));
         p_328017_.gameEvent(p_327926_, GameEvent.BLOCK_CHANGE, p_332811_);
         p_327926_.awardStat(Stats.ITEM_USED.get(item));
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      }
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_331745_, Level p_334119_, BlockPos p_330552_, Player p_332095_, BlockHitResult p_329702_) {
      if (p_334119_.isClientSide()) {
         if (eat(p_334119_, p_330552_, p_331745_, p_332095_).consumesAction()) {
            return InteractionResult.SUCCESS;
         }

         if (p_332095_.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
            return InteractionResult.CONSUME;
         }
      }

      return eat(p_334119_, p_330552_, p_331745_, p_332095_);
   }

   protected static InteractionResult eat(LevelAccessor p_51186_, BlockPos p_51187_, BlockState p_51188_, Player p_51189_) {
      if (!p_51189_.canEat(false)) {
         return InteractionResult.PASS;
      }

      p_51189_.awardStat(Stats.EAT_CAKE_SLICE);
      p_51189_.getFoodData().eat(2, 0.1F);
      int i = p_51188_.getValue(BITES);
      p_51186_.gameEvent(p_51189_, GameEvent.EAT, p_51187_);
      if (i < 6) {
         p_51186_.setBlock(p_51187_, p_51188_.setValue(BITES, i + 1), 3);
      } else {
         p_51186_.removeBlock(p_51187_, false);
         p_51186_.gameEvent(p_51189_, GameEvent.BLOCK_DESTROY, p_51187_);
      }

      return InteractionResult.SUCCESS;
   }

   @Override
   protected BlockState updateShape(
      BlockState p_51213_,
      LevelReader p_366089_,
      ScheduledTickAccess p_363263_,
      BlockPos p_51217_,
      Direction p_51214_,
      BlockPos p_51218_,
      BlockState p_51215_,
      RandomSource p_363935_
   ) {
      return p_51214_ == Direction.DOWN && !p_51213_.canSurvive(p_366089_, p_51217_)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(p_51213_, p_366089_, p_363263_, p_51217_, p_51214_, p_51218_, p_51215_, p_363935_);
   }

   @Override
   protected boolean canSurvive(BlockState p_51209_, LevelReader p_51210_, BlockPos p_51211_) {
      return p_51210_.getBlockState(p_51211_.below()).isSolid();
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_51220_) {
      p_51220_.add(BITES);
   }

   @Override
   protected int getAnalogOutputSignal(BlockState p_51198_, Level p_51199_, BlockPos p_51200_, Direction p_425879_) {
      return getOutputSignal(p_51198_.getValue(BITES));
   }

   public static int getOutputSignal(int p_152747_) {
      return (7 - p_152747_) * 2;
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState p_51191_) {
      return true;
   }

   @Override
   protected boolean isPathfindable(BlockState p_51193_, PathComputationType p_51196_) {
      return false;
   }
}
