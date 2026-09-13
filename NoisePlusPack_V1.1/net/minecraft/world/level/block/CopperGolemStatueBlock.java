package net.minecraft.world.level.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CopperGolemStatueBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class CopperGolemStatueBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
   public static final MapCodec<CopperGolemStatueBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_424761_ -> p_424761_.group(
            WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state").forGetter(CopperGolemStatueBlock::getWeatheringState), propertiesCodec()
         )
         .apply(p_424761_, CopperGolemStatueBlock::new)
   );
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   public static final EnumProperty<CopperGolemStatueBlock.Pose> POSE = BlockStateProperties.COPPER_GOLEM_POSE;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private static final VoxelShape SHAPE = Block.column(10.0, 0.0, 14.0);
   private final WeatheringCopper.WeatherState weatheringState;

   @Override
   public MapCodec<? extends CopperGolemStatueBlock> codec() {
      return CODEC;
   }

   public CopperGolemStatueBlock(WeatheringCopper.WeatherState p_429713_, BlockBehaviour.Properties p_426193_) {
      super(p_426193_);
      this.weatheringState = p_429713_;
      this.registerDefaultState(
         this.defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POSE, CopperGolemStatueBlock.Pose.STANDING).setValue(WATERLOGGED, false)
      );
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_429573_) {
      super.createBlockStateDefinition(p_429573_);
      p_429573_.add(FACING, POSE, WATERLOGGED);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_425305_) {
      FluidState fluidstate = p_425305_.getLevel().getFluidState(p_425305_.getClickedPos());
      return this.defaultBlockState()
         .setValue(FACING, p_425305_.getHorizontalDirection().getOpposite())
         .setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
   }

   @Override
   protected BlockState rotate(BlockState p_426141_, Rotation p_430230_) {
      return p_426141_.setValue(FACING, p_430230_.rotate(p_426141_.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState p_424860_, Mirror p_428564_) {
      return p_424860_.rotate(p_428564_.getRotation(p_424860_.getValue(FACING)));
   }

   @Override
   protected VoxelShape getShape(BlockState p_431473_, BlockGetter p_423539_, BlockPos p_426613_, CollisionContext p_430572_) {
      return SHAPE;
   }

   public WeatheringCopper.WeatherState getWeatheringState() {
      return this.weatheringState;
   }

   @Override
   protected InteractionResult useItemOn(
      ItemStack p_429817_, BlockState p_427867_, Level p_431353_, BlockPos p_423862_, Player p_423611_, InteractionHand p_425206_, BlockHitResult p_424242_
   ) {
      if (p_429817_.is(ItemTags.AXES)) {
         return InteractionResult.PASS;
      }

      this.updatePose(p_431353_, p_427867_, p_423862_, p_423611_);
      return InteractionResult.SUCCESS;
   }

   void updatePose(Level p_430461_, BlockState p_422442_, BlockPos p_424918_, Player p_431272_) {
      p_430461_.playSound(null, p_424918_, SoundEvents.COPPER_GOLEM_BECOME_STATUE, SoundSource.BLOCKS);
      p_430461_.setBlock(p_424918_, p_422442_.setValue(POSE, p_422442_.getValue(POSE).getNextPose()), 3);
      p_430461_.gameEvent(p_431272_, GameEvent.BLOCK_CHANGE, p_424918_);
   }

   @Override
   protected boolean isPathfindable(BlockState p_428257_, PathComputationType p_428347_) {
      return p_428347_ == PathComputationType.WATER && p_428257_.getFluidState().is(FluidTags.WATER);
   }

   @Override
   public @Nullable BlockEntity newBlockEntity(BlockPos p_429618_, BlockState p_425641_) {
      return new CopperGolemStatueBlockEntity(p_429618_, p_425641_);
   }

   @Override
   public boolean shouldChangedStateKeepBlockEntity(BlockState p_424048_) {
      return p_424048_.is(BlockTags.COPPER_GOLEM_STATUES);
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState p_431445_) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState p_430439_, Level p_423233_, BlockPos p_427299_, Direction p_422568_) {
      return p_430439_.getValue(POSE).ordinal() + 1;
   }

   @Override
   protected ItemStack getCloneItemStack(LevelReader p_430732_, BlockPos p_428180_, BlockState p_429456_, boolean p_428930_) {
      return p_430732_.getBlockEntity(p_428180_) instanceof CopperGolemStatueBlockEntity coppergolemstatueblockentity
         ? coppergolemstatueblockentity.getItem(this.asItem().getDefaultInstance(), p_429456_.getValue(POSE))
         : super.getCloneItemStack(p_430732_, p_428180_, p_429456_, p_428930_);
   }

   @Override
   protected void affectNeighborsAfterRemoval(BlockState p_423766_, ServerLevel p_423214_, BlockPos p_429218_, boolean p_423964_) {
      p_423214_.updateNeighbourForOutputSignal(p_429218_, p_423766_.getBlock());
   }

   @Override
   protected FluidState getFluidState(BlockState p_422841_) {
      return p_422841_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_422841_);
   }

   @Override
   protected BlockState updateShape(
      BlockState p_429586_,
      LevelReader p_429646_,
      ScheduledTickAccess p_425760_,
      BlockPos p_424733_,
      Direction p_425600_,
      BlockPos p_431588_,
      BlockState p_430838_,
      RandomSource p_425690_
   ) {
      if (p_429586_.getValue(WATERLOGGED)) {
         p_425760_.scheduleTick(p_424733_, Fluids.WATER, Fluids.WATER.getTickDelay(p_429646_));
      }

      return super.updateShape(p_429586_, p_429646_, p_425760_, p_424733_, p_425600_, p_431588_, p_430838_, p_425690_);
   }

   public enum Pose implements StringRepresentable {
      STANDING("standing"),
      SITTING("sitting"),
      RUNNING("running"),
      STAR("star");

      public static final IntFunction<CopperGolemStatueBlock.Pose> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
      public static final Codec<CopperGolemStatueBlock.Pose> CODEC = StringRepresentable.fromEnum(CopperGolemStatueBlock.Pose::values);
      private final String name;

      Pose(final String p_426055_) {
         this.name = p_426055_;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      public CopperGolemStatueBlock.Pose getNextPose() {
         return BY_ID.apply(this.ordinal() + 1);
      }
   }
}
