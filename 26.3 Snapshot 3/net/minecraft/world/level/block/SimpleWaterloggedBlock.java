package net.minecraft.world.level.block;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public interface SimpleWaterloggedBlock extends BucketPickup, LiquidBlockContainer {
   @Override
   default boolean canPlaceLiquid(final @Nullable LivingEntity user, final BlockGetter level, final BlockPos pos, final BlockState state, final Fluid type) {
      return type == Fluids.WATER;
   }

   @Override
   default boolean placeLiquid(final LevelAccessor level, final BlockPos pos, final BlockState state, final FluidState fluidState) {
      if (!state.getValue(BlockStateProperties.WATERLOGGED) && fluidState.is(Fluids.WATER)) {
         if (!level.isClientSide()) {
            level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.WATERLOGGED, true));
            level.scheduleTick(pos, fluidState.getType(), fluidState.getType().getTickDelay(level));
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   default ItemStack pickupBlock(final @Nullable LivingEntity user, final LevelAccessor level, final BlockPos pos, final BlockState state) {
      if (state.getValue(BlockStateProperties.WATERLOGGED)) {
         level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.WATERLOGGED, false));
         if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
         }

         return new ItemStack(Items.WATER_BUCKET);
      } else {
         return ItemStack.EMPTY;
      }
   }

   @Override
   default Optional<SoundEvent> getPickupSound() {
      return Fluids.WATER.getPickupSound();
   }
}
