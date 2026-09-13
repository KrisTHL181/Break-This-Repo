package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class BarrelBlock extends BaseEntityBlock {
   public static final MapCodec<BarrelBlock> CODEC = simpleCodec(BarrelBlock::new);
   public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
   public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

   @Override
   public MapCodec<BarrelBlock> codec() {
      return CODEC;
   }

   public BarrelBlock(BlockBehaviour.Properties p_49046_) {
      super(p_49046_);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false));
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_49069_, Level p_49070_, BlockPos p_49071_, Player p_49072_, BlockHitResult p_49074_) {
      if (p_49070_ instanceof ServerLevel serverlevel && p_49070_.getBlockEntity(p_49071_) instanceof BarrelBlockEntity barrelblockentity) {
         p_49072_.openMenu(barrelblockentity);
         p_49072_.awardStat(Stats.OPEN_BARREL);
         PiglinAi.angerNearbyPiglins(serverlevel, p_49072_, true);
      }

      return InteractionResult.SUCCESS;
   }

   @Override
   protected void affectNeighborsAfterRemoval(BlockState p_392973_, ServerLevel p_395733_, BlockPos p_396792_, boolean p_391385_) {
      Containers.updateNeighboursAfterDestroy(p_392973_, p_395733_, p_396792_);
   }

   @Override
   protected void tick(BlockState p_220758_, ServerLevel p_220759_, BlockPos p_220760_, RandomSource p_220761_) {
      BlockEntity blockentity = p_220759_.getBlockEntity(p_220760_);
      if (blockentity instanceof BarrelBlockEntity) {
         ((BarrelBlockEntity)blockentity).recheckOpen();
      }
   }

   @Override
   public @Nullable BlockEntity newBlockEntity(BlockPos p_152102_, BlockState p_152103_) {
      return new BarrelBlockEntity(p_152102_, p_152103_);
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState p_49058_) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState p_49065_, Level p_49066_, BlockPos p_49067_, Direction p_428298_) {
      return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(p_49066_.getBlockEntity(p_49067_));
   }

   @Override
   protected BlockState rotate(BlockState p_49085_, Rotation p_49086_) {
      return p_49085_.setValue(FACING, p_49086_.rotate(p_49085_.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState p_49082_, Mirror p_49083_) {
      return p_49082_.rotate(p_49083_.getRotation(p_49082_.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_49088_) {
      p_49088_.add(FACING, OPEN);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_49048_) {
      return this.defaultBlockState().setValue(FACING, p_49048_.getNearestLookingDirection().getOpposite());
   }
}
