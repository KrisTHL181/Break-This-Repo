package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LavaCauldronBlock extends AbstractCauldronBlock {
   public static final MapCodec<LavaCauldronBlock> CODEC = simpleCodec(LavaCauldronBlock::new);
   private static final VoxelShape SHAPE_INSIDE = Block.column(12.0, 4.0, 15.0);
   private static final VoxelShape FILLED_SHAPE = Shapes.or(AbstractCauldronBlock.SHAPE, SHAPE_INSIDE);

   @Override
   public MapCodec<LavaCauldronBlock> codec() {
      return CODEC;
   }

   public LavaCauldronBlock(BlockBehaviour.Properties p_153498_) {
      super(p_153498_, CauldronInteraction.LAVA);
   }

   @Override
   protected double getContentHeight(BlockState p_153500_) {
      return 0.9375;
   }

   @Override
   public boolean isFull(BlockState p_153511_) {
      return true;
   }

   @Override
   protected VoxelShape getEntityInsideCollisionShape(BlockState p_406571_, BlockGetter p_408197_, BlockPos p_409119_, Entity p_407100_) {
      return FILLED_SHAPE;
   }

   @Override
   protected void entityInside(
      BlockState p_153506_, Level p_153507_, BlockPos p_153508_, Entity p_153509_, InsideBlockEffectApplier p_394329_, boolean p_432040_
   ) {
      p_394329_.apply(InsideBlockEffectType.CLEAR_FREEZE);
      p_394329_.apply(InsideBlockEffectType.LAVA_IGNITE);
      p_394329_.runAfter(InsideBlockEffectType.LAVA_IGNITE, Entity::lavaHurt);
   }

   @Override
   protected int getAnalogOutputSignal(BlockState p_153502_, Level p_153503_, BlockPos p_153504_, Direction p_428271_) {
      return 3;
   }
}
