package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class DoorBlock extends Block {
   public static final MapCodec<DoorBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422106_ -> p_422106_.group(BlockSetType.CODEC.fieldOf("block_set_type").forGetter(DoorBlock::type), propertiesCodec()).apply(p_422106_, DoorBlock::new)
   );
   public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
   public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
   public static final EnumProperty<DoorHingeSide> HINGE = BlockStateProperties.DOOR_HINGE;
   public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Block.boxZ(16.0, 13.0, 16.0));
   private final BlockSetType type;

   @Override
   public MapCodec<? extends DoorBlock> codec() {
      return CODEC;
   }

   protected DoorBlock(BlockSetType p_272854_, BlockBehaviour.Properties p_273303_) {
      super(p_273303_.sound(p_272854_.soundType()));
      this.type = p_272854_;
      this.registerDefaultState(
         this.stateDefinition
            .any()
            .setValue(FACING, Direction.NORTH)
            .setValue(OPEN, false)
            .setValue(HINGE, DoorHingeSide.LEFT)
            .setValue(POWERED, false)
            .setValue(HALF, DoubleBlockHalf.LOWER)
      );
   }

   public BlockSetType type() {
      return this.type;
   }

   @Override
   protected VoxelShape getShape(BlockState p_52807_, BlockGetter p_52808_, BlockPos p_52809_, CollisionContext p_52810_) {
      Direction direction = p_52807_.getValue(FACING);
      Direction direction1 = p_52807_.getValue(OPEN)
         ? (p_52807_.getValue(HINGE) == DoorHingeSide.RIGHT ? direction.getCounterClockWise() : direction.getClockWise())
         : direction;
      return SHAPES.get(direction1);
   }

   @Override
   protected BlockState updateShape(
      BlockState p_52796_,
      LevelReader p_360873_,
      ScheduledTickAccess p_361225_,
      BlockPos p_52800_,
      Direction p_52797_,
      BlockPos p_52801_,
      BlockState p_52798_,
      RandomSource p_367859_
   ) {
      DoubleBlockHalf doubleblockhalf = p_52796_.getValue(HALF);
      if (p_52797_.getAxis() == Direction.Axis.Y && doubleblockhalf == DoubleBlockHalf.LOWER == (p_52797_ == Direction.UP)) {
         return p_52798_.getBlock() instanceof DoorBlock && p_52798_.getValue(HALF) != doubleblockhalf
            ? p_52798_.setValue(HALF, doubleblockhalf)
            : Blocks.AIR.defaultBlockState();
      } else {
         return doubleblockhalf == DoubleBlockHalf.LOWER && p_52797_ == Direction.DOWN && !p_52796_.canSurvive(p_360873_, p_52800_)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(p_52796_, p_360873_, p_361225_, p_52800_, p_52797_, p_52801_, p_52798_, p_367859_);
      }
   }

   @Override
   protected void onExplosionHit(
      BlockState p_312768_, ServerLevel p_367125_, BlockPos p_309806_, Explosion p_309956_, BiConsumer<ItemStack, BlockPos> p_311447_
   ) {
      if (p_309956_.canTriggerBlocks() && p_312768_.getValue(HALF) == DoubleBlockHalf.LOWER && this.type.canOpenByWindCharge() && !p_312768_.getValue(POWERED)) {
         this.setOpen(null, p_367125_, p_312768_, p_309806_, !this.isOpen(p_312768_));
      }

      super.onExplosionHit(p_312768_, p_367125_, p_309806_, p_309956_, p_311447_);
   }

   @Override
   public BlockState playerWillDestroy(Level p_52755_, BlockPos p_52756_, BlockState p_52757_, Player p_52758_) {
      if (!p_52755_.isClientSide() && (p_52758_.preventsBlockDrops() || !p_52758_.hasCorrectToolForDrops(p_52757_))) {
         DoublePlantBlock.preventDropFromBottomPart(p_52755_, p_52756_, p_52757_, p_52758_);
      }

      return super.playerWillDestroy(p_52755_, p_52756_, p_52757_, p_52758_);
   }

   @Override
   protected boolean isPathfindable(BlockState p_52764_, PathComputationType p_52767_) {
      return switch (p_52767_) {
         case LAND, AIR -> p_52764_.getValue(OPEN);
         case WATER -> false;
      };
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_52739_) {
      BlockPos blockpos = p_52739_.getClickedPos();
      Level level = p_52739_.getLevel();
      if (blockpos.getY() < level.getMaxY() && level.getBlockState(blockpos.above()).canBeReplaced(p_52739_)) {
         boolean flag = level.hasNeighborSignal(blockpos) || level.hasNeighborSignal(blockpos.above());
         return this.defaultBlockState()
            .setValue(FACING, p_52739_.getHorizontalDirection())
            .setValue(HINGE, this.getHinge(p_52739_))
            .setValue(POWERED, flag)
            .setValue(OPEN, flag)
            .setValue(HALF, DoubleBlockHalf.LOWER);
      } else {
         return null;
      }
   }

   @Override
   public void setPlacedBy(Level p_52749_, BlockPos p_52750_, BlockState p_52751_, @Nullable LivingEntity p_52752_, ItemStack p_52753_) {
      p_52749_.setBlock(p_52750_.above(), p_52751_.setValue(HALF, DoubleBlockHalf.UPPER), 3);
   }

   private DoorHingeSide getHinge(BlockPlaceContext p_52805_) {
      BlockGetter blockgetter = p_52805_.getLevel();
      BlockPos blockpos = p_52805_.getClickedPos();
      Direction direction = p_52805_.getHorizontalDirection();
      BlockPos blockpos1 = blockpos.above();
      Direction direction1 = direction.getCounterClockWise();
      BlockPos blockpos2 = blockpos.relative(direction1);
      BlockState blockstate = blockgetter.getBlockState(blockpos2);
      BlockPos blockpos3 = blockpos1.relative(direction1);
      BlockState blockstate1 = blockgetter.getBlockState(blockpos3);
      Direction direction2 = direction.getClockWise();
      BlockPos blockpos4 = blockpos.relative(direction2);
      BlockState blockstate2 = blockgetter.getBlockState(blockpos4);
      BlockPos blockpos5 = blockpos1.relative(direction2);
      BlockState blockstate3 = blockgetter.getBlockState(blockpos5);
      int i = (blockstate.isCollisionShapeFullBlock(blockgetter, blockpos2) ? -1 : 0)
         + (blockstate1.isCollisionShapeFullBlock(blockgetter, blockpos3) ? -1 : 0)
         + (blockstate2.isCollisionShapeFullBlock(blockgetter, blockpos4) ? 1 : 0)
         + (blockstate3.isCollisionShapeFullBlock(blockgetter, blockpos5) ? 1 : 0);
      boolean flag = blockstate.getBlock() instanceof DoorBlock && blockstate.getValue(HALF) == DoubleBlockHalf.LOWER;
      boolean flag1 = blockstate2.getBlock() instanceof DoorBlock && blockstate2.getValue(HALF) == DoubleBlockHalf.LOWER;
      if ((!flag || flag1) && i <= 0) {
         if ((!flag1 || flag) && i >= 0) {
            int j = direction.getStepX();
            int k = direction.getStepZ();
            Vec3 vec3 = p_52805_.getClickLocation();
            double d0 = vec3.x - blockpos.getX();
            double d1 = vec3.z - blockpos.getZ();
            return j < 0 && d1 < 0.5 || j > 0 && d1 > 0.5 || k < 0 && d0 > 0.5 || k > 0 && d0 < 0.5 ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT;
         } else {
            return DoorHingeSide.LEFT;
         }
      } else {
         return DoorHingeSide.RIGHT;
      }
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_52769_, Level p_52770_, BlockPos p_52771_, Player p_52772_, BlockHitResult p_52774_) {
      if (!this.type.canOpenByHand()) {
         return InteractionResult.PASS;
      }

      p_52769_ = p_52769_.cycle(OPEN);
      p_52770_.setBlock(p_52771_, p_52769_, 10);
      this.playSound(p_52772_, p_52770_, p_52771_, p_52769_.getValue(OPEN));
      p_52770_.gameEvent(p_52772_, this.isOpen(p_52769_) ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, p_52771_);
      return InteractionResult.SUCCESS;
   }

   public boolean isOpen(BlockState p_52816_) {
      return p_52816_.getValue(OPEN);
   }

   public void setOpen(@Nullable Entity p_153166_, Level p_153167_, BlockState p_153168_, BlockPos p_153169_, boolean p_153170_) {
      if (p_153168_.is(this) && p_153168_.getValue(OPEN) != p_153170_) {
         p_153167_.setBlock(p_153169_, p_153168_.setValue(OPEN, p_153170_), 10);
         this.playSound(p_153166_, p_153167_, p_153169_, p_153170_);
         p_153167_.gameEvent(p_153166_, p_153170_ ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, p_153169_);
      }
   }

   @Override
   protected void neighborChanged(BlockState p_52776_, Level p_52777_, BlockPos p_52778_, Block p_52779_, @Nullable Orientation p_369522_, boolean p_52781_) {
      boolean flag = p_52777_.hasNeighborSignal(p_52778_)
         || p_52777_.hasNeighborSignal(p_52778_.relative(p_52776_.getValue(HALF) == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN));
      if (!this.defaultBlockState().is(p_52779_) && flag != p_52776_.getValue(POWERED)) {
         if (flag != p_52776_.getValue(OPEN)) {
            this.playSound(null, p_52777_, p_52778_, flag);
            p_52777_.gameEvent(null, flag ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, p_52778_);
         }

         p_52777_.setBlock(p_52778_, p_52776_.setValue(POWERED, flag).setValue(OPEN, flag), 2);
      }
   }

   @Override
   protected boolean canSurvive(BlockState p_52783_, LevelReader p_52784_, BlockPos p_52785_) {
      BlockPos blockpos = p_52785_.below();
      BlockState blockstate = p_52784_.getBlockState(blockpos);
      return p_52783_.getValue(HALF) == DoubleBlockHalf.LOWER ? blockstate.isFaceSturdy(p_52784_, blockpos, Direction.UP) : blockstate.is(this);
   }

   private void playSound(@Nullable Entity p_251616_, Level p_249656_, BlockPos p_249439_, boolean p_251628_) {
      p_249656_.playSound(
         p_251616_,
         p_249439_,
         p_251628_ ? this.type.doorOpen() : this.type.doorClose(),
         SoundSource.BLOCKS,
         1.0F,
         p_249656_.getRandom().nextFloat() * 0.1F + 0.9F
      );
   }

   @Override
   protected BlockState rotate(BlockState p_52790_, Rotation p_52791_) {
      return p_52790_.setValue(FACING, p_52791_.rotate(p_52790_.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState p_52787_, Mirror p_52788_) {
      return p_52788_ == Mirror.NONE ? p_52787_ : p_52787_.rotate(p_52788_.getRotation(p_52787_.getValue(FACING))).cycle(HINGE);
   }

   @Override
   protected long getSeed(BlockState p_52793_, BlockPos p_52794_) {
      return Mth.getSeed(p_52794_.getX(), p_52794_.below(p_52793_.getValue(HALF) == DoubleBlockHalf.LOWER ? 0 : 1).getY(), p_52794_.getZ());
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_52803_) {
      p_52803_.add(HALF, FACING, OPEN, HINGE, POWERED);
   }

   public static boolean isWoodenDoor(Level p_52746_, BlockPos p_52747_) {
      return isWoodenDoor(p_52746_.getBlockState(p_52747_));
   }

   public static boolean isWoodenDoor(BlockState p_52818_) {
      return p_52818_.getBlock() instanceof DoorBlock doorblock && doorblock.type().canOpenByHand();
   }
}
