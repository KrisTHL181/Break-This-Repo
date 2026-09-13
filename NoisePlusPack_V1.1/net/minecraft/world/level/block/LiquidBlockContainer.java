package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

public interface LiquidBlockContainer {
   boolean canPlaceLiquid(@Nullable LivingEntity var1, BlockGetter var2, BlockPos var3, BlockState var4, Fluid var5);

   boolean placeLiquid(LevelAccessor var1, BlockPos var2, BlockState var3, FluidState var4);
}
