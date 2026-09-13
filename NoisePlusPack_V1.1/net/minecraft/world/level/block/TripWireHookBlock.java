package net.minecraft.world.level.block;

import com.google.common.base.MoreObjects;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class TripWireHookBlock extends Block {
   public static final MapCodec<TripWireHookBlock> CODEC = simpleCodec(TripWireHookBlock::new);
   public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final BooleanProperty ATTACHED = BlockStateProperties.ATTACHED;
   protected static final int WIRE_DIST_MIN = 1;
   protected static final int WIRE_DIST_MAX = 42;
   private static final int RECHECK_PERIOD = 10;
   private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Block.boxZ(6.0, 0.0, 10.0, 10.0, 16.0));

   @Override
   public MapCodec<TripWireHookBlock> codec() {
      return CODEC;
   }

   public TripWireHookBlock(BlockBehaviour.Properties p_57676_) {
      super(p_57676_);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false).setValue(ATTACHED, false));
   }

   @Override
   protected VoxelShape getShape(BlockState p_57740_, BlockGetter p_57741_, BlockPos p_57742_, CollisionContext p_57743_) {
      return SHAPES.get(p_57740_.getValue(FACING));
   }

   @Override
   protected boolean canSurvive(BlockState p_57721_, LevelReader p_57722_, BlockPos p_57723_) {
      Direction direction = p_57721_.getValue(FACING);
      BlockPos blockpos = p_57723_.relative(direction.getOpposite());
      BlockState blockstate = p_57722_.getBlockState(blockpos);
      return direction.getAxis().isHorizontal() && blockstate.isFaceSturdy(p_57722_, blockpos, direction);
   }

   @Override
   protected BlockState updateShape(
      BlockState p_57731_,
      LevelReader p_368766_,
      ScheduledTickAccess p_365650_,
      BlockPos p_57735_,
      Direction p_57732_,
      BlockPos p_57736_,
      BlockState p_57733_,
      RandomSource p_361546_
   ) {
      return p_57732_.getOpposite() == p_57731_.getValue(FACING) && !p_57731_.canSurvive(p_368766_, p_57735_)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(p_57731_, p_368766_, p_365650_, p_57735_, p_57732_, p_57736_, p_57733_, p_361546_);
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_57678_) {
      BlockState blockstate = this.defaultBlockState().setValue(POWERED, false).setValue(ATTACHED, false);
      LevelReader levelreader = p_57678_.getLevel();
      BlockPos blockpos = p_57678_.getClickedPos();
      Direction[] adirection = p_57678_.getNearestLookingDirections();

      for (Direction direction : adirection) {
         if (direction.getAxis().isHorizontal()) {
            Direction direction1 = direction.getOpposite();
            blockstate = blockstate.setValue(FACING, direction1);
            if (blockstate.canSurvive(levelreader, blockpos)) {
               return blockstate;
            }
         }
      }

      return null;
   }

   @Override
   public void setPlacedBy(Level p_57680_, BlockPos p_57681_, BlockState p_57682_, @Nullable LivingEntity p_57683_, ItemStack p_57684_) {
      calculateState(p_57680_, p_57681_, p_57682_, false, false, -1, null);
   }

   public static void calculateState(
      Level p_57686_, BlockPos p_57687_, BlockState p_57688_, boolean p_57689_, boolean p_57690_, int p_57691_, @Nullable BlockState p_57692_
   ) {
      Optional<Direction> optional = p_57688_.getOptionalValue(FACING);
      if (optional.isPresent()) {
         Direction direction = optional.get();
         boolean flag = p_57688_.getOptionalValue(ATTACHED).orElse(false);
         boolean flag1 = p_57688_.getOptionalValue(POWERED).orElse(false);
         Block block = p_57688_.getBlock();
         boolean flag2 = !p_57689_;
         boolean flag3 = false;
         int i = 0;
         BlockState[] ablockstate = new BlockState[42];

         for (int j = 1; j < 42; j++) {
            BlockPos blockpos = p_57687_.relative(direction, j);
            BlockState blockstate = p_57686_.getBlockState(blockpos);
            if (blockstate.is(Blocks.TRIPWIRE_HOOK)) {
               if (blockstate.getValue(FACING) == direction.getOpposite()) {
                  i = j;
               }
               break;
            }

            if (!blockstate.is(Blocks.TRIPWIRE) && j != p_57691_) {
               ablockstate[j] = null;
               flag2 = false;
            } else {
               if (j == p_57691_) {
                  blockstate = (BlockState)MoreObjects.firstNonNull(p_57692_, blockstate);
               }

               boolean flag4 = !blockstate.getValue(TripWireBlock.DISARMED);
               boolean flag5 = blockstate.getValue(TripWireBlock.POWERED);
               flag3 |= flag4 && flag5;
               ablockstate[j] = blockstate;
               if (j == p_57691_) {
                  p_57686_.scheduleTick(p_57687_, block, 10);
                  flag2 &= flag4;
               }
            }
         }

         flag2 &= i > 1;
         flag3 &= flag2;
         BlockState blockstate1 = block.defaultBlockState().trySetValue(ATTACHED, flag2).trySetValue(POWERED, flag3);
         if (i > 0) {
            BlockPos blockpos1 = p_57687_.relative(direction, i);
            Direction direction1 = direction.getOpposite();
            p_57686_.setBlock(blockpos1, blockstate1.setValue(FACING, direction1), 3);
            notifyNeighbors(block, p_57686_, blockpos1, direction1);
            emitState(p_57686_, blockpos1, flag2, flag3, flag, flag1);
         }

         emitState(p_57686_, p_57687_, flag2, flag3, flag, flag1);
         if (!p_57689_) {
            p_57686_.setBlock(p_57687_, blockstate1.setValue(FACING, direction), 3);
            if (p_57690_) {
               notifyNeighbors(block, p_57686_, p_57687_, direction);
            }
         }

         if (flag != flag2) {
            for (int k = 1; k < i; k++) {
               BlockPos blockpos2 = p_57687_.relative(direction, k);
               BlockState blockstate2 = ablockstate[k];
               if (blockstate2 != null) {
                  BlockState blockstate3 = p_57686_.getBlockState(blockpos2);
                  if (blockstate3.is(Blocks.TRIPWIRE) || blockstate3.is(Blocks.TRIPWIRE_HOOK)) {
                     p_57686_.setBlock(blockpos2, blockstate2.trySetValue(ATTACHED, flag2), 3);
                  }
               }
            }
         }
      }
   }

   @Override
   protected void tick(BlockState p_222610_, ServerLevel p_222611_, BlockPos p_222612_, RandomSource p_222613_) {
      calculateState(p_222611_, p_222612_, p_222610_, false, true, -1, null);
   }

   private static void emitState(Level p_222603_, BlockPos p_222604_, boolean p_222605_, boolean p_222606_, boolean p_222607_, boolean p_222608_) {
      if (p_222606_ && !p_222608_) {
         p_222603_.playSound(null, p_222604_, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 0.4F, 0.6F);
         p_222603_.gameEvent(null, GameEvent.BLOCK_ACTIVATE, p_222604_);
      } else if (!p_222606_ && p_222608_) {
         p_222603_.playSound(null, p_222604_, SoundEvents.TRIPWIRE_CLICK_OFF, SoundSource.BLOCKS, 0.4F, 0.5F);
         p_222603_.gameEvent(null, GameEvent.BLOCK_DEACTIVATE, p_222604_);
      } else if (p_222605_ && !p_222607_) {
         p_222603_.playSound(null, p_222604_, SoundEvents.TRIPWIRE_ATTACH, SoundSource.BLOCKS, 0.4F, 0.7F);
         p_222603_.gameEvent(null, GameEvent.BLOCK_ATTACH, p_222604_);
      } else if (!p_222605_ && p_222607_) {
         p_222603_.playSound(null, p_222604_, SoundEvents.TRIPWIRE_DETACH, SoundSource.BLOCKS, 0.4F, 1.2F / (p_222603_.random.nextFloat() * 0.2F + 0.9F));
         p_222603_.gameEvent(null, GameEvent.BLOCK_DETACH, p_222604_);
      }
   }

   private static void notifyNeighbors(Block p_312237_, Level p_57694_, BlockPos p_57695_, Direction p_57696_) {
      Direction direction = p_57696_.getOpposite();
      Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(p_57694_, direction, Direction.UP);
      p_57694_.updateNeighborsAt(p_57695_, p_312237_, orientation);
      p_57694_.updateNeighborsAt(p_57695_.relative(direction), p_312237_, orientation);
   }

   @Override
   protected void affectNeighborsAfterRemoval(BlockState p_394362_, ServerLevel p_392228_, BlockPos p_392478_, boolean p_391414_) {
      if (!p_391414_) {
         boolean flag = p_394362_.getValue(ATTACHED);
         boolean flag1 = p_394362_.getValue(POWERED);
         if (flag || flag1) {
            calculateState(p_392228_, p_392478_, p_394362_, true, false, -1, null);
         }

         if (flag1) {
            notifyNeighbors(this, p_392228_, p_392478_, p_394362_.getValue(FACING));
         }
      }
   }

   @Override
   protected int getSignal(BlockState p_57710_, BlockGetter p_57711_, BlockPos p_57712_, Direction p_57713_) {
      return p_57710_.getValue(POWERED) ? 15 : 0;
   }

   @Override
   protected int getDirectSignal(BlockState p_57745_, BlockGetter p_57746_, BlockPos p_57747_, Direction p_57748_) {
      if (!p_57745_.getValue(POWERED)) {
         return 0;
      } else {
         return p_57745_.getValue(FACING) == p_57748_ ? 15 : 0;
      }
   }

   @Override
   protected boolean isSignalSource(BlockState p_57750_) {
      return true;
   }

   @Override
   protected BlockState rotate(BlockState p_57728_, Rotation p_57729_) {
      return p_57728_.setValue(FACING, p_57729_.rotate(p_57728_.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState p_57725_, Mirror p_57726_) {
      return p_57725_.rotate(p_57726_.getRotation(p_57725_.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_57738_) {
      p_57738_.add(FACING, POWERED, ATTACHED);
   }
}
