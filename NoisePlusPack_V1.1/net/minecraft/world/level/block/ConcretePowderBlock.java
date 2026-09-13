package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ConcretePowderBlock extends FallingBlock {
   public static final MapCodec<ConcretePowderBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422101_ -> p_422101_.group(BuiltInRegistries.BLOCK.byNameCodec().fieldOf("concrete").forGetter(p_313163_ -> p_313163_.concrete), propertiesCodec())
         .apply(p_422101_, ConcretePowderBlock::new)
   );
   private final Block concrete;

   @Override
   public MapCodec<ConcretePowderBlock> codec() {
      return CODEC;
   }

   public ConcretePowderBlock(Block p_52060_, BlockBehaviour.Properties p_52061_) {
      super(p_52061_);
      this.concrete = p_52060_;
   }

   @Override
   public void onLand(Level p_52068_, BlockPos p_52069_, BlockState p_52070_, BlockState p_52071_, FallingBlockEntity p_52072_) {
      if (shouldSolidify(p_52068_, p_52069_, p_52071_)) {
         p_52068_.setBlock(p_52069_, this.concrete.defaultBlockState(), 3);
      }
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_52063_) {
      BlockGetter blockgetter = p_52063_.getLevel();
      BlockPos blockpos = p_52063_.getClickedPos();
      BlockState blockstate = blockgetter.getBlockState(blockpos);
      return shouldSolidify(blockgetter, blockpos, blockstate) ? this.concrete.defaultBlockState() : super.getStateForPlacement(p_52063_);
   }

   private static boolean shouldSolidify(BlockGetter p_52081_, BlockPos p_52082_, BlockState p_52083_) {
      return canSolidify(p_52083_) || touchesLiquid(p_52081_, p_52082_);
   }

   private static boolean touchesLiquid(BlockGetter p_52065_, BlockPos p_52066_) {
      boolean flag = false;
      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_52066_.mutable();

      for (Direction direction : Direction.values()) {
         BlockState blockstate = p_52065_.getBlockState(blockpos$mutableblockpos);
         if (direction != Direction.DOWN || canSolidify(blockstate)) {
            blockpos$mutableblockpos.setWithOffset(p_52066_, direction);
            blockstate = p_52065_.getBlockState(blockpos$mutableblockpos);
            if (canSolidify(blockstate) && !blockstate.isFaceSturdy(p_52065_, p_52066_, direction.getOpposite())) {
               flag = true;
               break;
            }
         }
      }

      return flag;
   }

   private static boolean canSolidify(BlockState p_52089_) {
      return p_52089_.getFluidState().is(FluidTags.WATER);
   }

   @Override
   protected BlockState updateShape(
      BlockState p_52074_,
      LevelReader p_361484_,
      ScheduledTickAccess p_362145_,
      BlockPos p_52078_,
      Direction p_52075_,
      BlockPos p_52079_,
      BlockState p_52076_,
      RandomSource p_369257_
   ) {
      return touchesLiquid(p_361484_, p_52078_)
         ? this.concrete.defaultBlockState()
         : super.updateShape(p_52074_, p_361484_, p_362145_, p_52078_, p_52075_, p_52079_, p_52076_, p_369257_);
   }

   @Override
   public int getDustColor(BlockState p_52085_, BlockGetter p_52086_, BlockPos p_52087_) {
      return p_52085_.getMapColor(p_52086_, p_52087_).col;
   }
}
