package net.minecraft.world.level.block.piston;

import com.mojang.serialization.MapCodec;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class MovingPistonBlock extends BaseEntityBlock {
   public static final MapCodec<MovingPistonBlock> CODEC = simpleCodec(MovingPistonBlock::new);
   public static final EnumProperty<Direction> FACING = PistonHeadBlock.FACING;
   public static final EnumProperty<PistonType> TYPE = PistonHeadBlock.TYPE;

   @Override
   public MapCodec<MovingPistonBlock> codec() {
      return CODEC;
   }

   public MovingPistonBlock(BlockBehaviour.Properties p_60050_) {
      super(p_60050_);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TYPE, PistonType.DEFAULT));
   }

   @Override
   public @Nullable BlockEntity newBlockEntity(BlockPos p_155879_, BlockState p_155880_) {
      return null;
   }

   public static BlockEntity newMovingBlockEntity(
      BlockPos p_155882_, BlockState p_155883_, BlockState p_155884_, Direction p_155885_, boolean p_155886_, boolean p_155887_
   ) {
      return new PistonMovingBlockEntity(p_155882_, p_155883_, p_155884_, p_155885_, p_155886_, p_155887_);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level p_155875_, BlockState p_155876_, BlockEntityType<T> p_155877_) {
      return createTickerHelper(p_155877_, BlockEntityType.PISTON, PistonMovingBlockEntity::tick);
   }

   @Override
   public void destroy(LevelAccessor p_60061_, BlockPos p_60062_, BlockState p_60063_) {
      BlockPos blockpos = p_60062_.relative(p_60063_.getValue(FACING).getOpposite());
      BlockState blockstate = p_60061_.getBlockState(blockpos);
      if (blockstate.getBlock() instanceof PistonBaseBlock && blockstate.getValue(PistonBaseBlock.EXTENDED)) {
         p_60061_.removeBlock(blockpos, false);
      }
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_60070_, Level p_60071_, BlockPos p_60072_, Player p_60073_, BlockHitResult p_60075_) {
      if (!p_60071_.isClientSide() && p_60071_.getBlockEntity(p_60072_) == null) {
         p_60071_.removeBlock(p_60072_, false);
         return InteractionResult.CONSUME;
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   protected List<ItemStack> getDrops(BlockState p_287650_, LootParams.Builder p_287754_) {
      PistonMovingBlockEntity pistonmovingblockentity = this.getBlockEntity(
         p_287754_.getLevel(), BlockPos.containing(p_287754_.getParameter(LootContextParams.ORIGIN))
      );
      return pistonmovingblockentity == null ? Collections.emptyList() : pistonmovingblockentity.getMovedState().getDrops(p_287754_);
   }

   @Override
   protected VoxelShape getShape(BlockState p_60099_, BlockGetter p_60100_, BlockPos p_60101_, CollisionContext p_60102_) {
      return Shapes.empty();
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState p_60104_, BlockGetter p_60105_, BlockPos p_60106_, CollisionContext p_60107_) {
      PistonMovingBlockEntity pistonmovingblockentity = this.getBlockEntity(p_60105_, p_60106_);
      return pistonmovingblockentity != null ? pistonmovingblockentity.getCollisionShape(p_60105_, p_60106_) : Shapes.empty();
   }

   private @Nullable PistonMovingBlockEntity getBlockEntity(BlockGetter p_60054_, BlockPos p_60055_) {
      BlockEntity blockentity = p_60054_.getBlockEntity(p_60055_);
      return blockentity instanceof PistonMovingBlockEntity ? (PistonMovingBlockEntity)blockentity : null;
   }

   @Override
   protected RenderShape getRenderShape(BlockState p_377730_) {
      return RenderShape.INVISIBLE;
   }

   @Override
   protected ItemStack getCloneItemStack(LevelReader p_309808_, BlockPos p_60058_, BlockState p_60059_, boolean p_376538_) {
      return ItemStack.EMPTY;
   }

   @Override
   protected BlockState rotate(BlockState p_60086_, Rotation p_60087_) {
      return p_60086_.setValue(FACING, p_60087_.rotate(p_60086_.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState p_60083_, Mirror p_60084_) {
      return p_60083_.rotate(p_60084_.getRotation(p_60083_.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_60097_) {
      p_60097_.add(FACING, TYPE);
   }

   @Override
   protected boolean isPathfindable(BlockState p_60065_, PathComputationType p_60068_) {
      return false;
   }
}
