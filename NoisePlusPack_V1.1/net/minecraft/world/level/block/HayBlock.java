package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class HayBlock extends RotatedPillarBlock {
   public static final MapCodec<HayBlock> CODEC = simpleCodec(HayBlock::new);

   @Override
   public MapCodec<HayBlock> codec() {
      return CODEC;
   }

   public HayBlock(BlockBehaviour.Properties p_53976_) {
      super(p_53976_);
      this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.Y));
   }

   @Override
   public void fallOn(Level p_153362_, BlockState p_153363_, BlockPos p_153364_, Entity p_153365_, double p_396854_) {
      p_153365_.causeFallDamage(p_396854_, 0.2F, p_153362_.damageSources().fall());
   }
}
