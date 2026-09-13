package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
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
   public static final MapCodec<CandleBlock> CODEC = simpleCodec(CandleBlock::new);
   public static final int MIN_CANDLES = 1;
   public static final int MAX_CANDLES = 4;
   public static final IntegerProperty CANDLES = BlockStateProperties.CANDLES;
   public static final BooleanProperty LIT = AbstractCandleBlock.LIT;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final ToIntFunction<BlockState> LIGHT_EMISSION = p_152848_ -> p_152848_.getValue(LIT) ? 3 * p_152848_.getValue(CANDLES) : 0;
   private static final Int2ObjectMap<List<Vec3>> PARTICLE_OFFSETS = Util.make(
      new Int2ObjectOpenHashMap(4),
      p_390929_ -> {
         float f = 0.0625F;
         p_390929_.put(1, List.of(new Vec3(8.0, 8.0, 8.0).scale(0.0625)));
         p_390929_.put(2, List.of(new Vec3(6.0, 7.0, 8.0).scale(0.0625), new Vec3(10.0, 8.0, 7.0).scale(0.0625)));
         p_390929_.put(3, List.of(new Vec3(8.0, 5.0, 10.0).scale(0.0625), new Vec3(6.0, 7.0, 8.0).scale(0.0625), new Vec3(9.0, 8.0, 7.0).scale(0.0625)));
         p_390929_.put(
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

   @Override
   public MapCodec<CandleBlock> codec() {
      return CODEC;
   }

   public CandleBlock(BlockBehaviour.Properties p_152801_) {
      super(p_152801_);
      this.registerDefaultState(this.stateDefinition.any().setValue(CANDLES, 1).setValue(LIT, false).setValue(WATERLOGGED, false));
   }

   @Override
   protected InteractionResult useItemOn(
      ItemStack p_333640_, BlockState p_329233_, Level p_330828_, BlockPos p_332080_, Player p_327941_, InteractionHand p_333741_, BlockHitResult p_331416_
   ) {
      if (p_333640_.isEmpty() && p_327941_.getAbilities().mayBuild && p_329233_.getValue(LIT)) {
         extinguish(p_327941_, p_329233_, p_330828_, p_332080_);
         return InteractionResult.SUCCESS;
      } else {
         return super.useItemOn(p_333640_, p_329233_, p_330828_, p_332080_, p_327941_, p_333741_, p_331416_);
      }
   }

   @Override
   protected boolean canBeReplaced(BlockState p_152814_, BlockPlaceContext p_152815_) {
      return !p_152815_.isSecondaryUseActive() && p_152815_.getItemInHand().getItem() == this.asItem() && p_152814_.getValue(CANDLES) < 4
         ? true
         : super.canBeReplaced(p_152814_, p_152815_);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_152803_) {
      BlockState blockstate = p_152803_.getLevel().getBlockState(p_152803_.getClickedPos());
      if (blockstate.is(this)) {
         return blockstate.cycle(CANDLES);
      }

      FluidState fluidstate = p_152803_.getLevel().getFluidState(p_152803_.getClickedPos());
      boolean flag = fluidstate.getType() == Fluids.WATER;
      return super.getStateForPlacement(p_152803_).setValue(WATERLOGGED, flag);
   }

   @Override
   protected BlockState updateShape(
      BlockState p_152833_,
      LevelReader p_364051_,
      ScheduledTickAccess p_366701_,
      BlockPos p_152837_,
      Direction p_152834_,
      BlockPos p_152838_,
      BlockState p_152835_,
      RandomSource p_365341_
   ) {
      if (p_152833_.getValue(WATERLOGGED)) {
         p_366701_.scheduleTick(p_152837_, Fluids.WATER, Fluids.WATER.getTickDelay(p_364051_));
      }

      return super.updateShape(p_152833_, p_364051_, p_366701_, p_152837_, p_152834_, p_152838_, p_152835_, p_365341_);
   }

   @Override
   protected FluidState getFluidState(BlockState p_152844_) {
      return p_152844_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_152844_);
   }

   @Override
   protected VoxelShape getShape(BlockState p_152817_, BlockGetter p_152818_, BlockPos p_152819_, CollisionContext p_152820_) {
      return SHAPES[p_152817_.getValue(CANDLES) - 1];
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_152840_) {
      p_152840_.add(CANDLES, LIT, WATERLOGGED);
   }

   @Override
   public boolean placeLiquid(LevelAccessor p_152805_, BlockPos p_152806_, BlockState p_152807_, FluidState p_152808_) {
      if (!p_152807_.getValue(WATERLOGGED) && p_152808_.getType() == Fluids.WATER) {
         BlockState blockstate = p_152807_.setValue(WATERLOGGED, true);
         if (p_152807_.getValue(LIT)) {
            extinguish(null, blockstate, p_152805_, p_152806_);
         } else {
            p_152805_.setBlock(p_152806_, blockstate, 3);
         }

         p_152805_.scheduleTick(p_152806_, p_152808_.getType(), p_152808_.getType().getTickDelay(p_152805_));
         return true;
      } else {
         return false;
      }
   }

   public static boolean canLight(BlockState p_152846_) {
      return p_152846_.is(BlockTags.CANDLES, p_152810_ -> p_152810_.hasProperty(LIT) && p_152810_.hasProperty(WATERLOGGED))
         && !p_152846_.getValue(LIT)
         && !p_152846_.getValue(WATERLOGGED);
   }

   @Override
   protected Iterable<Vec3> getParticleOffsets(BlockState p_152812_) {
      return (Iterable<Vec3>)PARTICLE_OFFSETS.get(p_152812_.getValue(CANDLES));
   }

   @Override
   protected boolean canBeLit(BlockState p_152842_) {
      return !p_152842_.getValue(WATERLOGGED) && super.canBeLit(p_152842_);
   }

   @Override
   protected boolean canSurvive(BlockState p_152829_, LevelReader p_152830_, BlockPos p_152831_) {
      return Block.canSupportCenter(p_152830_, p_152831_.below(), Direction.UP);
   }
}
