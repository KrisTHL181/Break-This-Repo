package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class FrostedIceBlock extends IceBlock {
   public static final MapCodec<FrostedIceBlock> CODEC = simpleCodec(FrostedIceBlock::new);
   public static final int MAX_AGE = 3;
   public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
   private static final int NEIGHBORS_TO_AGE = 4;
   private static final int NEIGHBORS_TO_MELT = 2;

   @Override
   public MapCodec<FrostedIceBlock> codec() {
      return CODEC;
   }

   public FrostedIceBlock(BlockBehaviour.Properties p_53564_) {
      super(p_53564_);
      this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
   }

   @Override
   public void onPlace(BlockState p_342717_, Level p_343709_, BlockPos p_344929_, BlockState p_344499_, boolean p_344789_) {
      p_343709_.scheduleTick(p_344929_, this, Mth.nextInt(p_343709_.getRandom(), 60, 120));
   }

   @Override
   protected void tick(BlockState p_221233_, ServerLevel p_221234_, BlockPos p_221235_, RandomSource p_221236_) {
      if (p_221236_.nextInt(3) == 0 || this.fewerNeigboursThan(p_221234_, p_221235_, 4)) {
         int i = p_221234_.dimension() == Level.END ? p_221234_.getBrightness(LightLayer.BLOCK, p_221235_) : p_221234_.getMaxLocalRawBrightness(p_221235_);
         if (i > 11 - p_221233_.getValue(AGE) - p_221233_.getLightBlock() && this.slightlyMelt(p_221233_, p_221234_, p_221235_)) {
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

            for (Direction direction : Direction.values()) {
               blockpos$mutableblockpos.setWithOffset(p_221235_, direction);
               BlockState blockstate = p_221234_.getBlockState(blockpos$mutableblockpos);
               if (blockstate.is(this) && !this.slightlyMelt(blockstate, p_221234_, blockpos$mutableblockpos)) {
                  p_221234_.scheduleTick(blockpos$mutableblockpos, this, Mth.nextInt(p_221236_, 20, 40));
               }
            }

            return;
         }
      }

      p_221234_.scheduleTick(p_221235_, this, Mth.nextInt(p_221236_, 20, 40));
   }

   private boolean slightlyMelt(BlockState p_53593_, Level p_53594_, BlockPos p_53595_) {
      int i = p_53593_.getValue(AGE);
      if (i < 3) {
         p_53594_.setBlock(p_53595_, p_53593_.setValue(AGE, i + 1), 2);
         return false;
      } else {
         this.melt(p_53593_, p_53594_, p_53595_);
         return true;
      }
   }

   @Override
   protected void neighborChanged(BlockState p_53579_, Level p_53580_, BlockPos p_53581_, Block p_53582_, @Nullable Orientation p_368711_, boolean p_53584_) {
      if (p_53582_.defaultBlockState().is(this) && this.fewerNeigboursThan(p_53580_, p_53581_, 2)) {
         this.melt(p_53579_, p_53580_, p_53581_);
      }

      super.neighborChanged(p_53579_, p_53580_, p_53581_, p_53582_, p_368711_, p_53584_);
   }

   private boolean fewerNeigboursThan(BlockGetter p_53566_, BlockPos p_53567_, int p_53568_) {
      int i = 0;
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for (Direction direction : Direction.values()) {
         blockpos$mutableblockpos.setWithOffset(p_53567_, direction);
         if (p_53566_.getBlockState(blockpos$mutableblockpos).is(this)) {
            if (++i >= p_53568_) {
               return false;
            }
         }
      }

      return true;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_53586_) {
      p_53586_.add(AGE);
   }

   @Override
   protected ItemStack getCloneItemStack(LevelReader p_310839_, BlockPos p_53571_, BlockState p_53572_, boolean p_376558_) {
      return ItemStack.EMPTY;
   }
}
