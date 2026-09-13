package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
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
   public static final int MAX_AGE = 7;
   public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
   private static final VoxelShape[] SHAPES = Block.boxes(7, age -> Block.column(16.0, 0.0, 2 + age * 2));

   protected CropBlock(final BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(this.getAgeProperty(), 0));
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return SHAPES[this.getAge(state)];
   }

   @Override
   protected boolean mayPlaceOn(final BlockState state, final BlockGetter level, final BlockPos pos) {
      return state.is(BlockTags.SUPPORTS_CROPS);
   }

   protected IntegerProperty getAgeProperty() {
      return AGE;
   }

   public int getMaxAge() {
      return 7;
   }

   public int getAge(final BlockState state) {
      return state.getValue(this.getAgeProperty());
   }

   public BlockState getStateForAge(final int age) {
      return this.defaultBlockState().setValue(this.getAgeProperty(), age);
   }

   public final boolean isMaxAge(final BlockState state) {
      return this.getAge(state) >= this.getMaxAge();
   }

   @Override
   protected boolean isRandomlyTicking(final BlockState state) {
      return !this.isMaxAge(state);
   }

   @Override
   protected void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
      if (level.getRawBrightness(pos, 0) >= 9) {
         int age = this.getAge(state);
         if (age < this.getMaxAge()) {
            float growthSpeed = getGrowthSpeed(this, level, pos);
            if (random.nextInt((int)(25.0F / growthSpeed) + 1) == 0) {
               level.setBlock(pos, this.getStateForAge(age + 1), 2);
            }
         }
      }
   }

   public void growCrops(final Level level, final BlockPos pos, final BlockState state) {
      int age = Math.min(this.getMaxAge(), this.getAge(state) + this.getBonemealAgeIncrease(level));
      level.setBlock(pos, this.getStateForAge(age), 2);
   }

   protected int getBonemealAgeIncrease(final Level level) {
      return Mth.nextInt(level.getRandom(), 2, 5);
   }

   protected static float getGrowthSpeed(final Block type, final BlockGetter level, final BlockPos pos) {
      float speed = 1.0F;
      BlockPos below = pos.below();

      for (int xx = -1; xx <= 1; xx++) {
         for (int zz = -1; zz <= 1; zz++) {
            float blockSpeed = 0.0F;
            BlockState blockState = level.getBlockState(below.offset(xx, 0, zz));
            if (blockState.is(BlockTags.GROWS_CROPS)) {
               blockSpeed = 1.0F;
               if (blockState.getValueOrElse(FarmlandBlock.MOISTURE, 0) > 0) {
                  blockSpeed = 3.0F;
               }
            }

            if (xx != 0 || zz != 0) {
               blockSpeed /= 4.0F;
            }

            speed += blockSpeed;
         }
      }

      BlockPos north = pos.north();
      BlockPos south = pos.south();
      BlockPos west = pos.west();
      BlockPos east = pos.east();
      boolean horizontal = level.getBlockState(west).is(type) || level.getBlockState(east).is(type);
      boolean vertical = level.getBlockState(north).is(type) || level.getBlockState(south).is(type);
      if (horizontal && vertical) {
         speed /= 2.0F;
      } else {
         boolean diagonal = level.getBlockState(west.north()).is(type)
            || level.getBlockState(east.north()).is(type)
            || level.getBlockState(east.south()).is(type)
            || level.getBlockState(west.south()).is(type);
         if (diagonal) {
            speed /= 2.0F;
         }
      }

      return speed;
   }

   @Override
   protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
      return hasSufficientLight(level, pos) && super.canSurvive(state, level, pos);
   }

   protected static boolean hasSufficientLight(final LevelReader level, final BlockPos pos) {
      return level.getRawBrightness(pos, 0) >= 8;
   }

   @Override
   protected void entityInside(
      final BlockState state, final Level level, final BlockPos pos, final Entity entity, final InsideBlockEffectApplier effectApplier, final boolean isPrecise
   ) {
      if (level instanceof ServerLevel serverLevel && entity instanceof Ravager && serverLevel.getGameRules().get(GameRules.MOB_GRIEFING)) {
         serverLevel.destroyBlock(pos, true, entity);
      }

      super.entityInside(state, level, pos, entity, effectApplier, isPrecise);
   }

   protected ItemLike getBaseSeedId() {
      return Items.WHEAT_SEEDS;
   }

   @Override
   protected ItemStack getCloneItemStack(final LevelReader level, final BlockPos pos, final BlockState state, final boolean includeData) {
      return new ItemStack(this.getBaseSeedId());
   }

   @Override
   public boolean isValidBonemealTarget(final LevelReader level, final BlockPos pos, final BlockState state) {
      return !this.isMaxAge(state);
   }

   @Override
   public boolean isBonemealSuccess(final Level level, final RandomSource random, final BlockPos pos, final BlockState state) {
      return true;
   }

   @Override
   public void performBonemeal(final ServerLevel level, final RandomSource random, final BlockPos pos, final BlockState state) {
      this.growCrops(level, pos, state);
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(AGE);
   }
}
