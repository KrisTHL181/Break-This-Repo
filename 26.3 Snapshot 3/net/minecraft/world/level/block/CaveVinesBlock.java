package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

public class CaveVinesBlock extends GrowingPlantHeadBlock implements CaveVines {
   private static final float CHANCE_OF_BERRIES_ON_GROWTH = 0.11F;

   public CaveVinesBlock(final BlockBehaviour.Properties properties) {
      super(properties, Direction.DOWN, SHAPE, false, 0.1);
      this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0).setValue(BERRIES, false));
   }

   @Override
   protected int getBlocksToGrowWhenBonemealed(final RandomSource random) {
      return 1;
   }

   @Override
   protected boolean canGrowInto(final BlockState state) {
      return state.isAir();
   }

   @Override
   protected Block getBodyBlock() {
      return Blocks.CAVE_VINES_PLANT;
   }

   @Override
   protected BlockState updateBodyAfterConvertedFromHead(final BlockState headState, final BlockState bodyState) {
      return bodyState.setValue(BERRIES, headState.getValue(BERRIES));
   }

   @Override
   protected BlockState getGrowIntoState(final BlockState growFromState, final RandomSource random) {
      return super.getGrowIntoState(growFromState, random).setValue(BERRIES, random.nextFloat() < 0.11F);
   }

   @Override
   protected ItemStack getCloneItemStack(final LevelReader level, final BlockPos pos, final BlockState state, final boolean includeData) {
      return new ItemStack(Items.GLOW_BERRIES);
   }

   @Override
   protected InteractionResult useWithoutItem(
      final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult
   ) {
      return CaveVines.use(player, state, level, pos);
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(BERRIES);
   }

   @Override
   public boolean isValidBonemealTarget(final LevelReader level, final BlockPos pos, final BlockState state) {
      return !state.getValue(BERRIES);
   }

   @Override
   public boolean isBonemealSuccess(final Level level, final RandomSource random, final BlockPos pos, final BlockState state) {
      return true;
   }

   @Override
   public void performBonemeal(final ServerLevel level, final RandomSource random, final BlockPos pos, final BlockState state) {
      level.setBlock(pos, state.setValue(BERRIES, true), 2);
   }
}
