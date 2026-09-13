package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CarrotBlock extends CropBlock {
   public static final MapCodec<CarrotBlock> CODEC = simpleCodec(CarrotBlock::new);
   private static final VoxelShape[] SHAPES = Block.boxes(7, p_395626_ -> Block.column(16.0, 0.0, 2 + p_395626_));

   @Override
   public MapCodec<CarrotBlock> codec() {
      return CODEC;
   }

   public CarrotBlock(BlockBehaviour.Properties p_51328_) {
      super(p_51328_);
   }

   @Override
   protected ItemLike getBaseSeedId() {
      return Items.CARROT;
   }

   @Override
   protected VoxelShape getShape(BlockState p_51330_, BlockGetter p_51331_, BlockPos p_51332_, CollisionContext p_51333_) {
      return SHAPES[this.getAge(p_51330_)];
   }
}
