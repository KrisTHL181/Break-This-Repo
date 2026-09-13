package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class CeilingHangingSignBlock extends SignBlock {
   public static final MapCodec<CeilingHangingSignBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422091_ -> p_422091_.group(WoodType.CODEC.fieldOf("wood_type").forGetter(SignBlock::type), propertiesCodec())
         .apply(p_422091_, CeilingHangingSignBlock::new)
   );
   public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;
   public static final BooleanProperty ATTACHED = BlockStateProperties.ATTACHED;
   private static final VoxelShape SHAPE_DEFAULT = Block.column(10.0, 0.0, 16.0);
   private static final Map<Integer, VoxelShape> SHAPES = Shapes.rotateHorizontal(Block.column(14.0, 2.0, 0.0, 10.0))
      .entrySet()
      .stream()
      .collect(Collectors.toMap(p_390930_ -> RotationSegment.convertToSegment(p_390930_.getKey()), Entry::getValue));

   @Override
   public MapCodec<CeilingHangingSignBlock> codec() {
      return CODEC;
   }

   public CeilingHangingSignBlock(WoodType p_248716_, BlockBehaviour.Properties p_250481_) {
      super(p_248716_, p_250481_.sound(p_248716_.hangingSignSoundType()));
      this.registerDefaultState(this.stateDefinition.any().setValue(ROTATION, 0).setValue(ATTACHED, false).setValue(WATERLOGGED, false));
   }

   @Override
   protected InteractionResult useItemOn(
      ItemStack p_328363_, BlockState p_332889_, Level p_333165_, BlockPos p_331699_, Player p_335683_, InteractionHand p_332677_, BlockHitResult p_330587_
   ) {
      return p_333165_.getBlockEntity(p_331699_) instanceof SignBlockEntity signblockentity
            && this.shouldTryToChainAnotherHangingSign(p_335683_, p_330587_, signblockentity, p_328363_)
         ? InteractionResult.PASS
         : super.useItemOn(p_328363_, p_332889_, p_333165_, p_331699_, p_335683_, p_332677_, p_330587_);
   }

   private boolean shouldTryToChainAnotherHangingSign(Player p_278279_, BlockHitResult p_278273_, SignBlockEntity p_278236_, ItemStack p_278343_) {
      return !p_278236_.canExecuteClickCommands(p_278236_.isFacingFrontText(p_278279_), p_278279_)
         && p_278343_.getItem() instanceof HangingSignItem
         && p_278273_.getDirection().equals(Direction.DOWN);
   }

   @Override
   protected boolean canSurvive(BlockState p_248994_, LevelReader p_249061_, BlockPos p_249490_) {
      return p_249061_.getBlockState(p_249490_.above()).isFaceSturdy(p_249061_, p_249490_.above(), Direction.DOWN, SupportType.CENTER);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_252121_) {
      Level level = p_252121_.getLevel();
      FluidState fluidstate = level.getFluidState(p_252121_.getClickedPos());
      BlockPos blockpos = p_252121_.getClickedPos().above();
      BlockState blockstate = level.getBlockState(blockpos);
      boolean flag = blockstate.is(BlockTags.ALL_HANGING_SIGNS);
      Direction direction = Direction.fromYRot(p_252121_.getRotation());
      boolean flag1 = !Block.isFaceFull(blockstate.getCollisionShape(level, blockpos), Direction.DOWN) || p_252121_.isSecondaryUseActive();
      if (flag && !p_252121_.isSecondaryUseActive()) {
         if (blockstate.hasProperty(WallHangingSignBlock.FACING)) {
            Direction direction1 = blockstate.getValue(WallHangingSignBlock.FACING);
            if (direction1.getAxis().test(direction)) {
               flag1 = false;
            }
         } else if (blockstate.hasProperty(ROTATION)) {
            Optional<Direction> optional = RotationSegment.convertToDirection(blockstate.getValue(ROTATION));
            if (optional.isPresent() && optional.get().getAxis().test(direction)) {
               flag1 = false;
            }
         }
      }

      int i = !flag1 ? RotationSegment.convertToSegment(direction.getOpposite()) : RotationSegment.convertToSegment(p_252121_.getRotation() + 180.0F);
      return this.defaultBlockState().setValue(ATTACHED, flag1).setValue(ROTATION, i).setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
   }

   @Override
   protected VoxelShape getShape(BlockState p_250564_, BlockGetter p_248998_, BlockPos p_249501_, CollisionContext p_248978_) {
      return SHAPES.getOrDefault(p_250564_.getValue(ROTATION), SHAPE_DEFAULT);
   }

   @Override
   protected VoxelShape getBlockSupportShape(BlockState p_254482_, BlockGetter p_253669_, BlockPos p_253916_) {
      return this.getShape(p_254482_, p_253669_, p_253916_, CollisionContext.empty());
   }

   @Override
   protected BlockState updateShape(
      BlockState p_251270_,
      LevelReader p_363569_,
      ScheduledTickAccess p_360965_,
      BlockPos p_249685_,
      Direction p_250331_,
      BlockPos p_251506_,
      BlockState p_249591_,
      RandomSource p_366093_
   ) {
      return p_250331_ == Direction.UP && !this.canSurvive(p_251270_, p_363569_, p_249685_)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(p_251270_, p_363569_, p_360965_, p_249685_, p_250331_, p_251506_, p_249591_, p_366093_);
   }

   @Override
   public float getYRotationDegrees(BlockState p_277758_) {
      return RotationSegment.convertToDegrees(p_277758_.getValue(ROTATION));
   }

   @Override
   protected BlockState rotate(BlockState p_251162_, Rotation p_250515_) {
      return p_251162_.setValue(ROTATION, p_250515_.rotate(p_251162_.getValue(ROTATION), 16));
   }

   @Override
   protected BlockState mirror(BlockState p_249682_, Mirror p_250199_) {
      return p_249682_.setValue(ROTATION, p_250199_.mirror(p_249682_.getValue(ROTATION), 16));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_251174_) {
      p_251174_.add(ROTATION, ATTACHED, WATERLOGGED);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos p_249338_, BlockState p_250706_) {
      return new HangingSignBlockEntity(p_249338_, p_250706_);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level p_279379_, BlockState p_279390_, BlockEntityType<T> p_279231_) {
      return createTickerHelper(p_279231_, BlockEntityType.HANGING_SIGN, SignBlockEntity::tick);
   }
}
