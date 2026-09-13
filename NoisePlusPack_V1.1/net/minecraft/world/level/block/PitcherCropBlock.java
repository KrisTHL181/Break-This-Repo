package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class PitcherCropBlock extends DoublePlantBlock implements BonemealableBlock {
   public static final MapCodec<PitcherCropBlock> CODEC = simpleCodec(PitcherCropBlock::new);
   public static final int MAX_AGE = 4;
   public static final IntegerProperty AGE = BlockStateProperties.AGE_4;
   public static final EnumProperty<DoubleBlockHalf> HALF = DoublePlantBlock.HALF;
   private static final int DOUBLE_PLANT_AGE_INTERSECTION = 3;
   private static final int BONEMEAL_INCREASE = 1;
   private static final VoxelShape SHAPE_BULB = Block.column(6.0, -1.0, 3.0);
   private static final VoxelShape SHAPE_CROP = Block.column(10.0, -1.0, 5.0);
   private final Function<BlockState, VoxelShape> shapes = this.makeShapes();

   @Override
   public MapCodec<PitcherCropBlock> codec() {
      return CODEC;
   }

   public PitcherCropBlock(BlockBehaviour.Properties p_277780_) {
      super(p_277780_);
   }

   private Function<BlockState, VoxelShape> makeShapes() {
      int[] aint = new int[]{0, 9, 11, 22, 26};
      return this.getShapeForEachState(p_397354_ -> {
         int i = (p_397354_.getValue(AGE) == 0 ? 4 : 6) + aint[p_397354_.getValue(AGE)];
         int j = p_397354_.getValue(AGE) == 0 ? 6 : 10;

         return switch ((DoubleBlockHalf)p_397354_.getValue(HALF)) {
            case LOWER -> Block.column(j, -1.0, Math.min(16, -1 + i));
            case UPPER -> Block.column(j, 0.0, Math.max(0, -1 + i - 16));
         };
      });
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_277448_) {
      return this.defaultBlockState();
   }

   @Override
   public VoxelShape getShape(BlockState p_277602_, BlockGetter p_277617_, BlockPos p_278005_, CollisionContext p_277514_) {
      return this.shapes.apply(p_277602_);
   }

   @Override
   public VoxelShape getCollisionShape(BlockState p_277609_, BlockGetter p_277398_, BlockPos p_278042_, CollisionContext p_277995_) {
      if (p_277609_.getValue(HALF) == DoubleBlockHalf.LOWER) {
         return p_277609_.getValue(AGE) == 0 ? SHAPE_BULB : SHAPE_CROP;
      } else {
         return Shapes.empty();
      }
   }

   @Override
   public BlockState updateShape(
      BlockState p_277518_,
      LevelReader p_363837_,
      ScheduledTickAccess p_369493_,
      BlockPos p_277982_,
      Direction p_277700_,
      BlockPos p_278106_,
      BlockState p_277660_,
      RandomSource p_368843_
   ) {
      if (isDouble(p_277518_.getValue(AGE))) {
         return super.updateShape(p_277518_, p_363837_, p_369493_, p_277982_, p_277700_, p_278106_, p_277660_, p_368843_);
      } else {
         return p_277518_.canSurvive(p_363837_, p_277982_) ? p_277518_ : Blocks.AIR.defaultBlockState();
      }
   }

   @Override
   public boolean canSurvive(BlockState p_277671_, LevelReader p_277477_, BlockPos p_278085_) {
      return isLower(p_277671_) && !sufficientLight(p_277477_, p_278085_) ? false : super.canSurvive(p_277671_, p_277477_, p_278085_);
   }

   @Override
   protected boolean mayPlaceOn(BlockState p_277418_, BlockGetter p_277461_, BlockPos p_277608_) {
      return p_277418_.is(Blocks.FARMLAND);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_277573_) {
      p_277573_.add(AGE);
      super.createBlockStateDefinition(p_277573_);
   }

   @Override
   public void entityInside(BlockState p_279266_, Level p_279469_, BlockPos p_279119_, Entity p_279372_, InsideBlockEffectApplier p_395453_, boolean p_432035_) {
      if (p_279469_ instanceof ServerLevel serverlevel && p_279372_ instanceof Ravager && serverlevel.getGameRules().get(GameRules.MOB_GRIEFING)) {
         serverlevel.destroyBlock(p_279119_, true, p_279372_);
      }
   }

   @Override
   public boolean canBeReplaced(BlockState p_277627_, BlockPlaceContext p_277759_) {
      return false;
   }

   @Override
   public void setPlacedBy(Level p_277432_, BlockPos p_277632_, BlockState p_277479_, @Nullable LivingEntity p_277805_, ItemStack p_277663_) {
   }

   @Override
   public boolean isRandomlyTicking(BlockState p_277483_) {
      return p_277483_.getValue(HALF) == DoubleBlockHalf.LOWER && !this.isMaxAge(p_277483_);
   }

   @Override
   public void randomTick(BlockState p_277950_, ServerLevel p_277589_, BlockPos p_277937_, RandomSource p_277887_) {
      float f = CropBlock.getGrowthSpeed(this, p_277589_, p_277937_);
      boolean flag = p_277887_.nextInt((int)(25.0F / f) + 1) == 0;
      if (flag) {
         this.grow(p_277589_, p_277950_, p_277937_, 1);
      }
   }

   private void grow(ServerLevel p_277975_, BlockState p_277349_, BlockPos p_277585_, int p_277498_) {
      int i = Math.min(p_277349_.getValue(AGE) + p_277498_, 4);
      if (this.canGrow(p_277975_, p_277585_, p_277349_, i)) {
         BlockState blockstate = p_277349_.setValue(AGE, i);
         p_277975_.setBlock(p_277585_, blockstate, 2);
         if (isDouble(i)) {
            p_277975_.setBlock(p_277585_.above(), blockstate.setValue(HALF, DoubleBlockHalf.UPPER), 3);
         }
      }
   }

   private static boolean canGrowInto(LevelReader p_290010_, BlockPos p_277823_) {
      BlockState blockstate = p_290010_.getBlockState(p_277823_);
      return blockstate.isAir() || blockstate.is(Blocks.PITCHER_CROP);
   }

   private static boolean sufficientLight(LevelReader p_290018_, BlockPos p_290011_) {
      return CropBlock.hasSufficientLight(p_290018_, p_290011_);
   }

   private static boolean isLower(BlockState p_279488_) {
      return p_279488_.is(Blocks.PITCHER_CROP) && p_279488_.getValue(HALF) == DoubleBlockHalf.LOWER;
   }

   private static boolean isDouble(int p_298533_) {
      return p_298533_ >= 3;
   }

   private boolean canGrow(LevelReader p_290007_, BlockPos p_290014_, BlockState p_290017_, int p_290008_) {
      return !this.isMaxAge(p_290017_) && sufficientLight(p_290007_, p_290014_) && (!isDouble(p_290008_) || canGrowInto(p_290007_, p_290014_.above()));
   }

   private boolean isMaxAge(BlockState p_277387_) {
      return p_277387_.getValue(AGE) >= 4;
   }

   private PitcherCropBlock.@Nullable PosAndState getLowerHalf(LevelReader p_290009_, BlockPos p_290016_, BlockState p_290015_) {
      if (isLower(p_290015_)) {
         return new PitcherCropBlock.PosAndState(p_290016_, p_290015_);
      }

      BlockPos blockpos = p_290016_.below();
      BlockState blockstate = p_290009_.getBlockState(blockpos);
      return isLower(blockstate) ? new PitcherCropBlock.PosAndState(blockpos, blockstate) : null;
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader p_277380_, BlockPos p_277500_, BlockState p_277715_) {
      PitcherCropBlock.PosAndState pitchercropblock$posandstate = this.getLowerHalf(p_277380_, p_277500_, p_277715_);
      return pitchercropblock$posandstate == null
         ? false
         : this.canGrow(p_277380_, pitchercropblock$posandstate.pos, pitchercropblock$posandstate.state, pitchercropblock$posandstate.state.getValue(AGE) + 1);
   }

   @Override
   public boolean isBonemealSuccess(Level p_277920_, RandomSource p_277594_, BlockPos p_277401_, BlockState p_277434_) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel p_277717_, RandomSource p_277870_, BlockPos p_277836_, BlockState p_278034_) {
      PitcherCropBlock.PosAndState pitchercropblock$posandstate = this.getLowerHalf(p_277717_, p_277836_, p_278034_);
      if (pitchercropblock$posandstate != null) {
         this.grow(p_277717_, pitchercropblock$posandstate.state, pitchercropblock$posandstate.pos, 1);
      }
   }

   record PosAndState(BlockPos pos, BlockState state) {
   }
}
