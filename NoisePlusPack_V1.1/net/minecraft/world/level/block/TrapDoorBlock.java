package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class TrapDoorBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
   public static final MapCodec<TrapDoorBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422132_ -> p_422132_.group(BlockSetType.CODEC.fieldOf("block_set_type").forGetter(p_311609_ -> p_311609_.type), propertiesCodec())
         .apply(p_422132_, TrapDoorBlock::new)
   );
   public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
   public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateAll(Block.boxZ(16.0, 13.0, 16.0));
   private final BlockSetType type;

   @Override
   public MapCodec<? extends TrapDoorBlock> codec() {
      return CODEC;
   }

   protected TrapDoorBlock(BlockSetType p_272964_, BlockBehaviour.Properties p_273079_) {
      super(p_273079_.sound(p_272964_.soundType()));
      this.type = p_272964_;
      this.registerDefaultState(
         this.stateDefinition
            .any()
            .setValue(FACING, Direction.NORTH)
            .setValue(OPEN, false)
            .setValue(HALF, Half.BOTTOM)
            .setValue(POWERED, false)
            .setValue(WATERLOGGED, false)
      );
   }

   @Override
   protected VoxelShape getShape(BlockState p_57563_, BlockGetter p_57564_, BlockPos p_57565_, CollisionContext p_57566_) {
      return SHAPES.get(p_57563_.getValue(OPEN) ? p_57563_.getValue(FACING) : (p_57563_.getValue(HALF) == Half.TOP ? Direction.DOWN : Direction.UP));
   }

   @Override
   protected boolean isPathfindable(BlockState p_57535_, PathComputationType p_57538_) {
      switch (p_57538_) {
         case LAND:
            return p_57535_.getValue(OPEN);
         case WATER:
            return p_57535_.getValue(WATERLOGGED);
         case AIR:
            return p_57535_.getValue(OPEN);
         default:
            return false;
      }
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_57540_, Level p_57541_, BlockPos p_57542_, Player p_57543_, BlockHitResult p_57545_) {
      if (!this.type.canOpenByHand()) {
         return InteractionResult.PASS;
      }

      this.toggle(p_57540_, p_57541_, p_57542_, p_57543_);
      return InteractionResult.SUCCESS;
   }

   @Override
   protected void onExplosionHit(
      BlockState p_312876_, ServerLevel p_365086_, BlockPos p_312697_, Explosion p_312889_, BiConsumer<ItemStack, BlockPos> p_312223_
   ) {
      if (p_312889_.canTriggerBlocks() && this.type.canOpenByWindCharge() && !p_312876_.getValue(POWERED)) {
         this.toggle(p_312876_, p_365086_, p_312697_, null);
      }

      super.onExplosionHit(p_312876_, p_365086_, p_312697_, p_312889_, p_312223_);
   }

   private void toggle(BlockState p_311901_, Level p_312039_, BlockPos p_310194_, @Nullable Player p_312003_) {
      BlockState blockstate = p_311901_.cycle(OPEN);
      p_312039_.setBlock(p_310194_, blockstate, 2);
      if (blockstate.getValue(WATERLOGGED)) {
         p_312039_.scheduleTick(p_310194_, Fluids.WATER, Fluids.WATER.getTickDelay(p_312039_));
      }

      this.playSound(p_312003_, p_312039_, p_310194_, blockstate.getValue(OPEN));
   }

   protected void playSound(@Nullable Player p_57528_, Level p_57529_, BlockPos p_57530_, boolean p_57531_) {
      p_57529_.playSound(
         p_57528_,
         p_57530_,
         p_57531_ ? this.type.trapdoorOpen() : this.type.trapdoorClose(),
         SoundSource.BLOCKS,
         1.0F,
         p_57529_.getRandom().nextFloat() * 0.1F + 0.9F
      );
      p_57529_.gameEvent(p_57528_, p_57531_ ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, p_57530_);
   }

   @Override
   protected void neighborChanged(BlockState p_57547_, Level p_57548_, BlockPos p_57549_, Block p_57550_, @Nullable Orientation p_363180_, boolean p_57552_) {
      if (!p_57548_.isClientSide()) {
         boolean flag = p_57548_.hasNeighborSignal(p_57549_);
         if (flag != p_57547_.getValue(POWERED)) {
            if (p_57547_.getValue(OPEN) != flag) {
               p_57547_ = p_57547_.setValue(OPEN, flag);
               this.playSound(null, p_57548_, p_57549_, flag);
            }

            p_57548_.setBlock(p_57549_, p_57547_.setValue(POWERED, flag), 2);
            if (p_57547_.getValue(WATERLOGGED)) {
               p_57548_.scheduleTick(p_57549_, Fluids.WATER, Fluids.WATER.getTickDelay(p_57548_));
            }
         }
      }
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_57533_) {
      BlockState blockstate = this.defaultBlockState();
      FluidState fluidstate = p_57533_.getLevel().getFluidState(p_57533_.getClickedPos());
      Direction direction = p_57533_.getClickedFace();
      if (!p_57533_.replacingClickedOnBlock() && direction.getAxis().isHorizontal()) {
         blockstate = blockstate.setValue(FACING, direction)
            .setValue(HALF, p_57533_.getClickLocation().y - p_57533_.getClickedPos().getY() > 0.5 ? Half.TOP : Half.BOTTOM);
      } else {
         blockstate = blockstate.setValue(FACING, p_57533_.getHorizontalDirection().getOpposite())
            .setValue(HALF, direction == Direction.UP ? Half.BOTTOM : Half.TOP);
      }

      if (p_57533_.getLevel().hasNeighborSignal(p_57533_.getClickedPos())) {
         blockstate = blockstate.setValue(OPEN, true).setValue(POWERED, true);
      }

      return blockstate.setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_57561_) {
      p_57561_.add(FACING, OPEN, HALF, POWERED, WATERLOGGED);
   }

   @Override
   protected FluidState getFluidState(BlockState p_57568_) {
      return p_57568_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_57568_);
   }

   @Override
   protected BlockState updateShape(
      BlockState p_57554_,
      LevelReader p_365791_,
      ScheduledTickAccess p_368436_,
      BlockPos p_57558_,
      Direction p_57555_,
      BlockPos p_57559_,
      BlockState p_57556_,
      RandomSource p_367698_
   ) {
      if (p_57554_.getValue(WATERLOGGED)) {
         p_368436_.scheduleTick(p_57558_, Fluids.WATER, Fluids.WATER.getTickDelay(p_365791_));
      }

      return super.updateShape(p_57554_, p_365791_, p_368436_, p_57558_, p_57555_, p_57559_, p_57556_, p_367698_);
   }

   protected BlockSetType getType() {
      return this.type;
   }
}
