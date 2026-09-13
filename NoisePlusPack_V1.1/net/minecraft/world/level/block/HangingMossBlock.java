package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
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
   public static final MapCodec<HangingMossBlock> CODEC = simpleCodec(HangingMossBlock::new);
   private static final VoxelShape SHAPE_BASE = Block.column(14.0, 0.0, 16.0);
   private static final VoxelShape SHAPE_TIP = Block.column(14.0, 2.0, 16.0);
   public static final BooleanProperty TIP = BlockStateProperties.TIP;

   @Override
   public MapCodec<HangingMossBlock> codec() {
      return CODEC;
   }

   public HangingMossBlock(BlockBehaviour.Properties p_369795_) {
      super(p_369795_);
      this.registerDefaultState(this.stateDefinition.any().setValue(TIP, true));
   }

   @Override
   protected VoxelShape getShape(BlockState p_365701_, BlockGetter p_361960_, BlockPos p_364640_, CollisionContext p_365299_) {
      return p_365701_.getValue(TIP) ? SHAPE_TIP : SHAPE_BASE;
   }

   @Override
   public void animateTick(BlockState p_362034_, Level p_368572_, BlockPos p_366897_, RandomSource p_361415_) {
      if (p_361415_.nextInt(500) == 0) {
         BlockState blockstate = p_368572_.getBlockState(p_366897_.above());
         if (blockstate.is(BlockTags.PALE_OAK_LOGS) || blockstate.is(Blocks.PALE_OAK_LEAVES)) {
            p_368572_.playLocalSound(
               p_366897_.getX(), p_366897_.getY(), p_366897_.getZ(), SoundEvents.PALE_HANGING_MOSS_IDLE, SoundSource.AMBIENT, 1.0F, 1.0F, false
            );
         }
      }
   }

   @Override
   protected boolean propagatesSkylightDown(BlockState p_370166_) {
      return true;
   }

   @Override
   protected boolean canSurvive(BlockState p_361782_, LevelReader p_367417_, BlockPos p_366634_) {
      return this.canStayAtPosition(p_367417_, p_366634_);
   }

   private boolean canStayAtPosition(BlockGetter p_365484_, BlockPos p_362413_) {
      BlockPos blockpos = p_362413_.relative(Direction.UP);
      BlockState blockstate = p_365484_.getBlockState(blockpos);
      return MultifaceBlock.canAttachTo(p_365484_, Direction.UP, blockpos, blockstate) || blockstate.is(Blocks.PALE_HANGING_MOSS);
   }

   @Override
   protected BlockState updateShape(
      BlockState p_361250_,
      LevelReader p_370189_,
      ScheduledTickAccess p_362194_,
      BlockPos p_366744_,
      Direction p_367183_,
      BlockPos p_366942_,
      BlockState p_369003_,
      RandomSource p_363174_
   ) {
      if (!this.canStayAtPosition(p_370189_, p_366744_)) {
         p_362194_.scheduleTick(p_366744_, this, 1);
      }

      return p_361250_.setValue(TIP, !p_370189_.getBlockState(p_366744_.below()).is(this));
   }

   @Override
   protected void tick(BlockState p_367034_, ServerLevel p_368909_, BlockPos p_361251_, RandomSource p_363153_) {
      if (!this.canStayAtPosition(p_368909_, p_361251_)) {
         p_368909_.destroyBlock(p_361251_, true);
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_361400_) {
      p_361400_.add(TIP);
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader p_362841_, BlockPos p_362408_, BlockState p_365198_) {
      return this.canGrowInto(p_362841_.getBlockState(this.getTip(p_362841_, p_362408_).below()));
   }

   private boolean canGrowInto(BlockState p_369343_) {
      return p_369343_.isAir();
   }

   public BlockPos getTip(BlockGetter p_363698_, BlockPos p_367170_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_367170_.mutable();

      BlockState blockstate;
      do {
         blockpos$mutableblockpos.move(Direction.DOWN);
         blockstate = p_363698_.getBlockState(blockpos$mutableblockpos);
      } while (blockstate.is(this));

      return blockpos$mutableblockpos.relative(Direction.UP).immutable();
   }

   @Override
   public boolean isBonemealSuccess(Level p_369749_, RandomSource p_362047_, BlockPos p_361385_, BlockState p_361113_) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel p_362461_, RandomSource p_362879_, BlockPos p_361813_, BlockState p_362206_) {
      BlockPos blockpos = this.getTip(p_362461_, p_361813_).below();
      if (this.canGrowInto(p_362461_.getBlockState(blockpos))) {
         p_362461_.setBlockAndUpdate(blockpos, p_362206_.setValue(TIP, true));
      }
   }
}
