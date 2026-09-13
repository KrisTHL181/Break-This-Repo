package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;
import org.jspecify.annotations.Nullable;

public class KelpPlantBlock extends GrowingPlantBodyBlock implements LiquidBlockContainer {
   public static final MapCodec<KelpPlantBlock> CODEC = simpleCodec(KelpPlantBlock::new);

   @Override
   public MapCodec<KelpPlantBlock> codec() {
      return CODEC;
   }

   protected KelpPlantBlock(BlockBehaviour.Properties p_54323_) {
      super(p_54323_, Direction.UP, Shapes.block(), true);
   }

   @Override
   protected GrowingPlantHeadBlock getHeadBlock() {
      return (GrowingPlantHeadBlock)Blocks.KELP;
   }

   @Override
   protected FluidState getFluidState(BlockState p_54336_) {
      return Fluids.WATER.getSource(false);
   }

   @Override
   protected boolean canAttachTo(BlockState p_153457_) {
      return this.getHeadBlock().canAttachTo(p_153457_);
   }

   @Override
   public boolean canPlaceLiquid(@Nullable LivingEntity p_396927_, BlockGetter p_54325_, BlockPos p_54326_, BlockState p_54327_, Fluid p_54328_) {
      return false;
   }

   @Override
   public boolean placeLiquid(LevelAccessor p_54330_, BlockPos p_54331_, BlockState p_54332_, FluidState p_54333_) {
      return false;
   }
}
