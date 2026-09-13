package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class WallHangingSignBlock extends SignBlock {
   public static final MapCodec<WallHangingSignBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422136_ -> p_422136_.group(WoodType.CODEC.fieldOf("wood_type").forGetter(SignBlock::type), propertiesCodec())
         .apply(p_422136_, WallHangingSignBlock::new)
   );
   public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
   private static final Map<Direction.Axis, VoxelShape> SHAPES_PLANK = Shapes.rotateHorizontalAxis(Block.column(16.0, 4.0, 14.0, 16.0));
   private static final Map<Direction.Axis, VoxelShape> SHAPES = Shapes.rotateHorizontalAxis(
      Shapes.or(SHAPES_PLANK.get(Direction.Axis.Z), Block.column(14.0, 2.0, 0.0, 10.0))
   );

   @Override
   public MapCodec<WallHangingSignBlock> codec() {
      return CODEC;
   }

   public WallHangingSignBlock(WoodType p_252140_, BlockBehaviour.Properties p_251606_) {
      super(p_252140_, p_251606_.sound(p_252140_.hangingSignSoundType()));
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
   }

   @Override
   protected InteractionResult useItemOn(
      ItemStack p_331007_, BlockState p_336183_, Level p_331789_, BlockPos p_329016_, Player p_329833_, InteractionHand p_330634_, BlockHitResult p_333867_
   ) {
      return p_331789_.getBlockEntity(p_329016_) instanceof SignBlockEntity signblockentity
            && this.shouldTryToChainAnotherHangingSign(p_336183_, p_329833_, p_333867_, signblockentity, p_331007_)
         ? InteractionResult.PASS
         : super.useItemOn(p_331007_, p_336183_, p_331789_, p_329016_, p_329833_, p_330634_, p_333867_);
   }

   private boolean shouldTryToChainAnotherHangingSign(
      BlockState p_278346_, Player p_278263_, BlockHitResult p_278269_, SignBlockEntity p_278290_, ItemStack p_278238_
   ) {
      return !p_278290_.canExecuteClickCommands(p_278290_.isFacingFrontText(p_278263_), p_278263_)
         && p_278238_.getItem() instanceof HangingSignItem
         && !this.isHittingEditableSide(p_278269_, p_278346_);
   }

   private boolean isHittingEditableSide(BlockHitResult p_278339_, BlockState p_278302_) {
      return p_278339_.getDirection().getAxis() == p_278302_.getValue(FACING).getAxis();
   }

   @Override
   protected VoxelShape getShape(BlockState p_250980_, BlockGetter p_251012_, BlockPos p_251391_, CollisionContext p_251875_) {
      return SHAPES.get(p_250980_.getValue(FACING).getAxis());
   }

   @Override
   protected VoxelShape getBlockSupportShape(BlockState p_253927_, BlockGetter p_254149_, BlockPos p_253805_) {
      return this.getShape(p_253927_, p_254149_, p_253805_, CollisionContext.empty());
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState p_249963_, BlockGetter p_248542_, BlockPos p_252224_, CollisionContext p_251891_) {
      return SHAPES_PLANK.get(p_249963_.getValue(FACING).getAxis());
   }

   public boolean canPlace(BlockState p_249472_, LevelReader p_249453_, BlockPos p_251235_) {
      Direction direction = p_249472_.getValue(FACING).getClockWise();
      Direction direction1 = p_249472_.getValue(FACING).getCounterClockWise();
      return this.canAttachTo(p_249453_, p_249472_, p_251235_.relative(direction), direction1)
         || this.canAttachTo(p_249453_, p_249472_, p_251235_.relative(direction1), direction);
   }

   public boolean canAttachTo(LevelReader p_249746_, BlockState p_251128_, BlockPos p_250583_, Direction p_250567_) {
      BlockState blockstate = p_249746_.getBlockState(p_250583_);
      return blockstate.is(BlockTags.WALL_HANGING_SIGNS)
         ? blockstate.getValue(FACING).getAxis().test(p_251128_.getValue(FACING))
         : blockstate.isFaceSturdy(p_249746_, p_250583_, p_250567_, SupportType.FULL);
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_251399_) {
      BlockState blockstate = this.defaultBlockState();
      FluidState fluidstate = p_251399_.getLevel().getFluidState(p_251399_.getClickedPos());
      LevelReader levelreader = p_251399_.getLevel();
      BlockPos blockpos = p_251399_.getClickedPos();

      for (Direction direction : p_251399_.getNearestLookingDirections()) {
         if (direction.getAxis().isHorizontal() && !direction.getAxis().test(p_251399_.getClickedFace())) {
            Direction direction1 = direction.getOpposite();
            blockstate = blockstate.setValue(FACING, direction1);
            if (blockstate.canSurvive(levelreader, blockpos) && this.canPlace(blockstate, levelreader, blockpos)) {
               return blockstate.setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
            }
         }
      }

      return null;
   }

   @Override
   protected BlockState updateShape(
      BlockState p_249879_,
      LevelReader p_362268_,
      ScheduledTickAccess p_365983_,
      BlockPos p_252327_,
      Direction p_249939_,
      BlockPos p_251853_,
      BlockState p_250767_,
      RandomSource p_368431_
   ) {
      return p_249939_.getAxis() == p_249879_.getValue(FACING).getClockWise().getAxis() && !p_249879_.canSurvive(p_362268_, p_252327_)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(p_249879_, p_362268_, p_365983_, p_252327_, p_249939_, p_251853_, p_250767_, p_368431_);
   }

   @Override
   public float getYRotationDegrees(BlockState p_278073_) {
      return p_278073_.getValue(FACING).toYRot();
   }

   @Override
   protected BlockState rotate(BlockState p_249292_, Rotation p_249867_) {
      return p_249292_.setValue(FACING, p_249867_.rotate(p_249292_.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState p_250446_, Mirror p_249494_) {
      return p_250446_.rotate(p_249494_.getRotation(p_250446_.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_251029_) {
      p_251029_.add(FACING, WATERLOGGED);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos p_250745_, BlockState p_250905_) {
      return new HangingSignBlockEntity(p_250745_, p_250905_);
   }

   @Override
   protected boolean isPathfindable(BlockState p_253755_, PathComputationType p_253687_) {
      return false;
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level p_279316_, BlockState p_279345_, BlockEntityType<T> p_279384_) {
      return createTickerHelper(p_279384_, BlockEntityType.HANGING_SIGN, SignBlockEntity::tick);
   }
}
