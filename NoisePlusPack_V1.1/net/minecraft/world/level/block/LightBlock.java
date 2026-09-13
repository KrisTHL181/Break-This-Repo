package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LightBlock extends Block implements SimpleWaterloggedBlock {
   public static final MapCodec<LightBlock> CODEC = simpleCodec(LightBlock::new);
   public static final int MAX_LEVEL = 15;
   public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final ToIntFunction<BlockState> LIGHT_EMISSION = p_153701_ -> p_153701_.getValue(LEVEL);

   @Override
   public MapCodec<LightBlock> codec() {
      return CODEC;
   }

   public LightBlock(BlockBehaviour.Properties p_153662_) {
      super(p_153662_);
      this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 15).setValue(WATERLOGGED, false));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_153687_) {
      p_153687_.add(LEVEL, WATERLOGGED);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_153673_, Level p_153674_, BlockPos p_153675_, Player p_153676_, BlockHitResult p_153678_) {
      if (!p_153674_.isClientSide() && p_153676_.canUseGameMasterBlocks()) {
         p_153674_.setBlock(p_153675_, p_153673_.cycle(LEVEL), 2);
         return InteractionResult.SUCCESS_SERVER;
      } else {
         return InteractionResult.CONSUME;
      }
   }

   @Override
   protected VoxelShape getShape(BlockState p_153668_, BlockGetter p_153669_, BlockPos p_153670_, CollisionContext p_153671_) {
      return p_153671_.isHoldingItem(Items.LIGHT) ? Shapes.block() : Shapes.empty();
   }

   @Override
   protected boolean propagatesSkylightDown(BlockState p_153695_) {
      return p_153695_.getFluidState().isEmpty();
   }

   @Override
   protected RenderShape getRenderShape(BlockState p_153693_) {
      return RenderShape.INVISIBLE;
   }

   @Override
   protected float getShadeBrightness(BlockState p_153689_, BlockGetter p_153690_, BlockPos p_153691_) {
      return 1.0F;
   }

   @Override
   protected BlockState updateShape(
      BlockState p_153680_,
      LevelReader p_367699_,
      ScheduledTickAccess p_366283_,
      BlockPos p_153684_,
      Direction p_153681_,
      BlockPos p_153685_,
      BlockState p_153682_,
      RandomSource p_361423_
   ) {
      if (p_153680_.getValue(WATERLOGGED)) {
         p_366283_.scheduleTick(p_153684_, Fluids.WATER, Fluids.WATER.getTickDelay(p_367699_));
      }

      return super.updateShape(p_153680_, p_367699_, p_366283_, p_153684_, p_153681_, p_153685_, p_153682_, p_361423_);
   }

   @Override
   protected FluidState getFluidState(BlockState p_153699_) {
      return p_153699_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_153699_);
   }

   @Override
   protected ItemStack getCloneItemStack(LevelReader p_310734_, BlockPos p_153665_, BlockState p_153666_, boolean p_375572_) {
      return setLightOnStack(super.getCloneItemStack(p_310734_, p_153665_, p_153666_, p_375572_), p_153666_.getValue(LEVEL));
   }

   public static ItemStack setLightOnStack(ItemStack p_259339_, int p_259353_) {
      p_259339_.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(LEVEL, p_259353_));
      return p_259339_;
   }
}
