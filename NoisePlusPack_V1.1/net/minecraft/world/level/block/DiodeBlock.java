package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;
import org.jspecify.annotations.Nullable;

public abstract class DiodeBlock extends HorizontalDirectionalBlock {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 2.0);

   protected DiodeBlock(BlockBehaviour.Properties p_52499_) {
      super(p_52499_);
   }

   @Override
   protected abstract MapCodec<? extends DiodeBlock> codec();

   @Override
   protected VoxelShape getShape(BlockState p_52556_, BlockGetter p_52557_, BlockPos p_52558_, CollisionContext p_52559_) {
      return SHAPE;
   }

   @Override
   protected boolean canSurvive(BlockState p_52538_, LevelReader p_52539_, BlockPos p_52540_) {
      BlockPos blockpos = p_52540_.below();
      return this.canSurviveOn(p_52539_, blockpos, p_52539_.getBlockState(blockpos));
   }

   protected boolean canSurviveOn(LevelReader p_299987_, BlockPos p_298116_, BlockState p_297597_) {
      return p_297597_.isFaceSturdy(p_299987_, p_298116_, Direction.UP, SupportType.RIGID);
   }

   @Override
   protected void tick(BlockState p_221065_, ServerLevel p_221066_, BlockPos p_221067_, RandomSource p_221068_) {
      if (!this.isLocked(p_221066_, p_221067_, p_221065_)) {
         boolean flag = p_221065_.getValue(POWERED);
         boolean flag1 = this.shouldTurnOn(p_221066_, p_221067_, p_221065_);
         if (flag && !flag1) {
            p_221066_.setBlock(p_221067_, p_221065_.setValue(POWERED, false), 2);
         } else if (!flag) {
            p_221066_.setBlock(p_221067_, p_221065_.setValue(POWERED, true), 2);
            if (!flag1) {
               p_221066_.scheduleTick(p_221067_, this, this.getDelay(p_221065_), TickPriority.VERY_HIGH);
            }
         }
      }
   }

   @Override
   protected int getDirectSignal(BlockState p_52561_, BlockGetter p_52562_, BlockPos p_52563_, Direction p_52564_) {
      return p_52561_.getSignal(p_52562_, p_52563_, p_52564_);
   }

   @Override
   protected int getSignal(BlockState p_52520_, BlockGetter p_52521_, BlockPos p_52522_, Direction p_52523_) {
      if (!p_52520_.getValue(POWERED)) {
         return 0;
      } else {
         return p_52520_.getValue(FACING) == p_52523_ ? this.getOutputSignal(p_52521_, p_52522_, p_52520_) : 0;
      }
   }

   @Override
   protected void neighborChanged(BlockState p_52525_, Level p_52526_, BlockPos p_52527_, Block p_52528_, @Nullable Orientation p_363702_, boolean p_52530_) {
      if (p_52525_.canSurvive(p_52526_, p_52527_)) {
         this.checkTickOnNeighbor(p_52526_, p_52527_, p_52525_);
      } else {
         BlockEntity blockentity = p_52525_.hasBlockEntity() ? p_52526_.getBlockEntity(p_52527_) : null;
         dropResources(p_52525_, p_52526_, p_52527_, blockentity);
         p_52526_.removeBlock(p_52527_, false);

         for (Direction direction : Direction.values()) {
            p_52526_.updateNeighborsAt(p_52527_.relative(direction), this);
         }
      }
   }

   protected void checkTickOnNeighbor(Level p_52577_, BlockPos p_52578_, BlockState p_52579_) {
      if (!this.isLocked(p_52577_, p_52578_, p_52579_)) {
         boolean flag = p_52579_.getValue(POWERED);
         boolean flag1 = this.shouldTurnOn(p_52577_, p_52578_, p_52579_);
         if (flag != flag1 && !p_52577_.getBlockTicks().willTickThisTick(p_52578_, this)) {
            TickPriority tickpriority = TickPriority.HIGH;
            if (this.shouldPrioritize(p_52577_, p_52578_, p_52579_)) {
               tickpriority = TickPriority.EXTREMELY_HIGH;
            } else if (flag) {
               tickpriority = TickPriority.VERY_HIGH;
            }

            p_52577_.scheduleTick(p_52578_, this, this.getDelay(p_52579_), tickpriority);
         }
      }
   }

   public boolean isLocked(LevelReader p_52511_, BlockPos p_52512_, BlockState p_52513_) {
      return false;
   }

   protected boolean shouldTurnOn(Level p_52502_, BlockPos p_52503_, BlockState p_52504_) {
      return this.getInputSignal(p_52502_, p_52503_, p_52504_) > 0;
   }

   protected int getInputSignal(Level p_52544_, BlockPos p_52545_, BlockState p_52546_) {
      Direction direction = p_52546_.getValue(FACING);
      BlockPos blockpos = p_52545_.relative(direction);
      int i = p_52544_.getSignal(blockpos, direction);
      if (i >= 15) {
         return i;
      }

      BlockState blockstate = p_52544_.getBlockState(blockpos);
      return Math.max(i, blockstate.is(Blocks.REDSTONE_WIRE) ? blockstate.getValue(RedStoneWireBlock.POWER) : 0);
   }

   protected int getAlternateSignal(SignalGetter p_277358_, BlockPos p_277763_, BlockState p_277604_) {
      Direction direction = p_277604_.getValue(FACING);
      Direction direction1 = direction.getClockWise();
      Direction direction2 = direction.getCounterClockWise();
      boolean flag = this.sideInputDiodesOnly();
      return Math.max(
         p_277358_.getControlInputSignal(p_277763_.relative(direction1), direction1, flag),
         p_277358_.getControlInputSignal(p_277763_.relative(direction2), direction2, flag)
      );
   }

   @Override
   protected boolean isSignalSource(BlockState p_52572_) {
      return true;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_52501_) {
      return this.defaultBlockState().setValue(FACING, p_52501_.getHorizontalDirection().getOpposite());
   }

   @Override
   public void setPlacedBy(Level p_52506_, BlockPos p_52507_, BlockState p_52508_, @Nullable LivingEntity p_52509_, ItemStack p_52510_) {
      if (this.shouldTurnOn(p_52506_, p_52507_, p_52508_)) {
         p_52506_.scheduleTick(p_52507_, this, 1);
      }
   }

   @Override
   protected void onPlace(BlockState p_52566_, Level p_52567_, BlockPos p_52568_, BlockState p_52569_, boolean p_52570_) {
      this.updateNeighborsInFront(p_52567_, p_52568_, p_52566_);
   }

   @Override
   protected void affectNeighborsAfterRemoval(BlockState p_397978_, ServerLevel p_391595_, BlockPos p_392425_, boolean p_397263_) {
      if (!p_397263_) {
         this.updateNeighborsInFront(p_391595_, p_392425_, p_397978_);
      }
   }

   protected void updateNeighborsInFront(Level p_52581_, BlockPos p_52582_, BlockState p_52583_) {
      Direction direction = p_52583_.getValue(FACING);
      BlockPos blockpos = p_52582_.relative(direction.getOpposite());
      Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(p_52581_, direction.getOpposite(), Direction.UP);
      p_52581_.neighborChanged(blockpos, this, orientation);
      p_52581_.updateNeighborsAtExceptFromFacing(blockpos, this, direction, orientation);
   }

   protected boolean sideInputDiodesOnly() {
      return false;
   }

   protected int getOutputSignal(BlockGetter p_52541_, BlockPos p_52542_, BlockState p_52543_) {
      return 15;
   }

   public static boolean isDiode(BlockState p_52587_) {
      return p_52587_.getBlock() instanceof DiodeBlock;
   }

   public boolean shouldPrioritize(BlockGetter p_52574_, BlockPos p_52575_, BlockState p_52576_) {
      Direction direction = p_52576_.getValue(FACING).getOpposite();
      BlockState blockstate = p_52574_.getBlockState(p_52575_.relative(direction));
      return isDiode(blockstate) && blockstate.getValue(FACING) != direction;
   }

   protected abstract int getDelay(BlockState var1);
}
