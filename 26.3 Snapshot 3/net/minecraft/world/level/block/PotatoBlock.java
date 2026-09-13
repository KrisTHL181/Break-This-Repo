package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PotatoBlock extends CropBlock {
   private static final VoxelShape[] SHAPES = Block.boxes(7, age -> Block.column(16.0, 0.0, 2 + age));

   public PotatoBlock(final BlockBehaviour.Properties properties) {
      super(properties);
   }

   @Override
   protected ItemLike getBaseSeedId() {
      return Items.POTATO;
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return SHAPES[this.getAge(state)];
   }
}
