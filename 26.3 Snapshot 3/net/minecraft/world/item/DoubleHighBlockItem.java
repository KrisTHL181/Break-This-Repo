package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class DoubleHighBlockItem extends BlockItem {
   public DoubleHighBlockItem(final Block block, final Item.Properties properties) {
      super(block, properties);
   }

   @Override
   protected boolean placeBlock(final BlockPlaceContext context, final BlockState placementState) {
      Level level = context.getLevel();
      BlockPos above = context.getClickedPos().above();
      BlockState aboveState = level.isWaterAt(above) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
      level.setBlock(above, aboveState, 27);
      return super.placeBlock(context, placementState);
   }
}
