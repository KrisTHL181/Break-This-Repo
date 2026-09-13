package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class SculkShriekerBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
   public static final MapCodec<SculkShriekerBlock> CODEC = simpleCodec(SculkShriekerBlock::new);
   public static final BooleanProperty SHRIEKING = BlockStateProperties.SHRIEKING;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final BooleanProperty CAN_SUMMON = BlockStateProperties.CAN_SUMMON;
   private static final VoxelShape SHAPE_COLLISION = Block.column(16.0, 0.0, 8.0);
   public static final double TOP_Y = SHAPE_COLLISION.max(Direction.Axis.Y);

   @Override
   public MapCodec<SculkShriekerBlock> codec() {
      return CODEC;
   }

   public SculkShriekerBlock(BlockBehaviour.Properties p_222159_) {
      super(p_222159_);
      this.registerDefaultState(this.stateDefinition.any().setValue(SHRIEKING, false).setValue(WATERLOGGED, false).setValue(CAN_SUMMON, false));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_222211_) {
      p_222211_.add(SHRIEKING);
      p_222211_.add(WATERLOGGED);
      p_222211_.add(CAN_SUMMON);
   }

   @Override
   public void stepOn(Level p_222177_, BlockPos p_222178_, BlockState p_222179_, Entity p_222180_) {
      if (p_222177_ instanceof ServerLevel serverlevel) {
         ServerPlayer serverplayer = SculkShriekerBlockEntity.tryGetPlayer(p_222180_);
         if (serverplayer != null) {
            serverlevel.getBlockEntity(p_222178_, BlockEntityType.SCULK_SHRIEKER).ifPresent(p_222163_ -> p_222163_.tryShriek(serverlevel, serverplayer));
         }
      }

      super.stepOn(p_222177_, p_222178_, p_222179_, p_222180_);
   }

   @Override
   protected void tick(BlockState p_222187_, ServerLevel p_222188_, BlockPos p_222189_, RandomSource p_222190_) {
      if (p_222187_.getValue(SHRIEKING)) {
         p_222188_.setBlock(p_222189_, p_222187_.setValue(SHRIEKING, false), 3);
         p_222188_.getBlockEntity(p_222189_, BlockEntityType.SCULK_SHRIEKER).ifPresent(p_222217_ -> p_222217_.tryRespond(p_222188_));
      }
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState p_222225_, BlockGetter p_222226_, BlockPos p_222227_, CollisionContext p_222228_) {
      return SHAPE_COLLISION;
   }

   @Override
   protected VoxelShape getOcclusionShape(BlockState p_222221_) {
      return SHAPE_COLLISION;
   }

   @Override
   protected boolean useShapeForLightOcclusion(BlockState p_222232_) {
      return true;
   }

   @Override
   public @Nullable BlockEntity newBlockEntity(BlockPos p_222213_, BlockState p_222214_) {
      return new SculkShriekerBlockEntity(p_222213_, p_222214_);
   }

   @Override
   protected BlockState updateShape(
      BlockState p_222204_,
      LevelReader p_360825_,
      ScheduledTickAccess p_367851_,
      BlockPos p_222208_,
      Direction p_222205_,
      BlockPos p_222209_,
      BlockState p_222206_,
      RandomSource p_367556_
   ) {
      if (p_222204_.getValue(WATERLOGGED)) {
         p_367851_.scheduleTick(p_222208_, Fluids.WATER, Fluids.WATER.getTickDelay(p_360825_));
      }

      return super.updateShape(p_222204_, p_360825_, p_367851_, p_222208_, p_222205_, p_222209_, p_222206_, p_367556_);
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_222171_) {
      return this.defaultBlockState().setValue(WATERLOGGED, p_222171_.getLevel().getFluidState(p_222171_.getClickedPos()).getType() == Fluids.WATER);
   }

   @Override
   protected FluidState getFluidState(BlockState p_222230_) {
      return p_222230_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_222230_);
   }

   @Override
   protected void spawnAfterBreak(BlockState p_222192_, ServerLevel p_222193_, BlockPos p_222194_, ItemStack p_222195_, boolean p_222196_) {
      super.spawnAfterBreak(p_222192_, p_222193_, p_222194_, p_222195_, p_222196_);
      if (p_222196_) {
         this.tryDropExperience(p_222193_, p_222194_, p_222195_, ConstantInt.of(5));
      }
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level p_222173_, BlockState p_222174_, BlockEntityType<T> p_222175_) {
      return !p_222173_.isClientSide()
         ? BaseEntityBlock.createTickerHelper(
            p_222175_,
            BlockEntityType.SCULK_SHRIEKER,
            (p_281134_, p_281135_, p_281136_, p_281137_) -> VibrationSystem.Ticker.tick(p_281134_, p_281137_.getVibrationData(), p_281137_.getVibrationUser())
         )
         : null;
   }
}
