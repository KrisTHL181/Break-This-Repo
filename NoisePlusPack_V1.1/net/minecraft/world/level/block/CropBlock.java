package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CropBlock extends VegetationBlock implements BonemealableBlock {
   public static final MapCodec<CropBlock> CODEC = simpleCodec(CropBlock::new);
   public static final int MAX_AGE = 7;
   public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
   private static final VoxelShape[] SHAPES = Block.boxes(7, p_397339_ -> Block.column(16.0, 0.0, 2 + p_397339_ * 2));

   @Override
   public MapCodec<? extends CropBlock> codec() {
      return CODEC;
   }

   protected CropBlock(BlockBehaviour.Properties p_52247_) {
      super(p_52247_);
      this.registerDefaultState(this.stateDefinition.any().setValue(this.getAgeProperty(), 0));
   }

   @Override
   protected VoxelShape getShape(BlockState p_52297_, BlockGetter p_52298_, BlockPos p_52299_, CollisionContext p_52300_) {
      return SHAPES[this.getAge(p_52297_)];
   }

   @Override
   protected boolean mayPlaceOn(BlockState p_52302_, BlockGetter p_52303_, BlockPos p_52304_) {
      return p_52302_.is(Blocks.FARMLAND);
   }

   protected IntegerProperty getAgeProperty() {
      return AGE;
   }

   public int getMaxAge() {
      return 7;
   }

   public int getAge(BlockState p_52306_) {
      return p_52306_.getValue(this.getAgeProperty());
   }

   public BlockState getStateForAge(int p_52290_) {
      return this.defaultBlockState().setValue(this.getAgeProperty(), p_52290_);
   }

   public final boolean isMaxAge(BlockState p_52308_) {
      return this.getAge(p_52308_) >= this.getMaxAge();
   }

   @Override
   protected boolean isRandomlyTicking(BlockState p_52288_) {
      return !this.isMaxAge(p_52288_);
   }

   @Override
   protected void randomTick(BlockState p_221050_, ServerLevel p_221051_, BlockPos p_221052_, RandomSource p_221053_) {
      if (p_221051_.getRawBrightness(p_221052_, 0) >= 9) {
         int i = this.getAge(p_221050_);
         if (i < this.getMaxAge()) {
            float f = getGrowthSpeed(this, p_221051_, p_221052_);
            if (p_221053_.nextInt((int)(25.0F / f) + 1) == 0) {
               p_221051_.setBlock(p_221052_, this.getStateForAge(i + 1), 2);
            }
         }
      }
   }

   public void growCrops(Level p_52264_, BlockPos p_52265_, BlockState p_52266_) {
      int i = Math.min(this.getMaxAge(), this.getAge(p_52266_) + this.getBonemealAgeIncrease(p_52264_));
      p_52264_.setBlock(p_52265_, this.getStateForAge(i), 2);
   }

   protected int getBonemealAgeIncrease(Level p_52262_) {
      return Mth.nextInt(p_52262_.random, 2, 5);
   }

   protected static float getGrowthSpeed(Block p_52273_, BlockGetter p_52274_, BlockPos p_52275_) {
      float f = 1.0F;
      BlockPos blockpos = p_52275_.below();

      for (int i = -1; i <= 1; i++) {
         for (int j = -1; j <= 1; j++) {
            float f1 = 0.0F;
            BlockState blockstate = p_52274_.getBlockState(blockpos.offset(i, 0, j));
            if (blockstate.is(Blocks.FARMLAND)) {
               f1 = 1.0F;
               if (blockstate.getValue(FarmBlock.MOISTURE) > 0) {
                  f1 = 3.0F;
               }
            }

            if (i != 0 || j != 0) {
               f1 /= 4.0F;
            }

            f += f1;
         }
      }

      BlockPos blockpos1 = p_52275_.north();
      BlockPos blockpos2 = p_52275_.south();
      BlockPos blockpos3 = p_52275_.west();
      BlockPos blockpos4 = p_52275_.east();
      boolean flag = p_52274_.getBlockState(blockpos3).is(p_52273_) || p_52274_.getBlockState(blockpos4).is(p_52273_);
      boolean flag1 = p_52274_.getBlockState(blockpos1).is(p_52273_) || p_52274_.getBlockState(blockpos2).is(p_52273_);
      if (flag && flag1) {
         f /= 2.0F;
      } else {
         boolean flag2 = p_52274_.getBlockState(blockpos3.north()).is(p_52273_)
            || p_52274_.getBlockState(blockpos4.north()).is(p_52273_)
            || p_52274_.getBlockState(blockpos4.south()).is(p_52273_)
            || p_52274_.getBlockState(blockpos3.south()).is(p_52273_);
         if (flag2) {
            f /= 2.0F;
         }
      }

      return f;
   }

   @Override
   protected boolean canSurvive(BlockState p_52282_, LevelReader p_52283_, BlockPos p_52284_) {
      return hasSufficientLight(p_52283_, p_52284_) && super.canSurvive(p_52282_, p_52283_, p_52284_);
   }

   protected static boolean hasSufficientLight(LevelReader p_300321_, BlockPos p_300219_) {
      return p_300321_.getRawBrightness(p_300219_, 0) >= 8;
   }

   @Override
   protected void entityInside(BlockState p_52277_, Level p_52278_, BlockPos p_52279_, Entity p_52280_, InsideBlockEffectApplier p_391246_, boolean p_432051_) {
      if (p_52278_ instanceof ServerLevel serverlevel && p_52280_ instanceof Ravager && serverlevel.getGameRules().get(GameRules.MOB_GRIEFING)) {
         serverlevel.destroyBlock(p_52279_, true, p_52280_);
      }

      super.entityInside(p_52277_, p_52278_, p_52279_, p_52280_, p_391246_, p_432051_);
   }

   protected ItemLike getBaseSeedId() {
      return Items.WHEAT_SEEDS;
   }

   @Override
   protected ItemStack getCloneItemStack(LevelReader p_310461_, BlockPos p_52255_, BlockState p_52256_, boolean p_377901_) {
      return new ItemStack(this.getBaseSeedId());
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader p_255715_, BlockPos p_52259_, BlockState p_52260_) {
      return !this.isMaxAge(p_52260_);
   }

   @Override
   public boolean isBonemealSuccess(Level p_221045_, RandomSource p_221046_, BlockPos p_221047_, BlockState p_221048_) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel p_221040_, RandomSource p_221041_, BlockPos p_221042_, BlockState p_221043_) {
      this.growCrops(p_221040_, p_221042_, p_221043_);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_52286_) {
      p_52286_.add(AGE);
   }
}
