package net.minecraft.world.item;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

public class StandingAndWallBlockItem extends BlockItem {
   protected final Block wallBlock;
   private final Direction attachmentDirection;

   public StandingAndWallBlockItem(Block p_248873_, Block p_251044_, Direction p_250800_, Item.Properties p_249308_) {
      super(p_248873_, p_249308_);
      this.wallBlock = p_251044_;
      this.attachmentDirection = p_250800_;
   }

   protected boolean canPlace(LevelReader p_250350_, BlockState p_249311_, BlockPos p_250328_) {
      return p_249311_.canSurvive(p_250350_, p_250328_);
   }

   @Override
   protected @Nullable BlockState getPlacementState(BlockPlaceContext p_43255_) {
      BlockState blockstate = this.wallBlock.getStateForPlacement(p_43255_);
      BlockState blockstate1 = null;
      LevelReader levelreader = p_43255_.getLevel();
      BlockPos blockpos = p_43255_.getClickedPos();

      for (Direction direction : p_43255_.getNearestLookingDirections()) {
         if (direction != this.attachmentDirection.getOpposite()) {
            BlockState blockstate2 = direction == this.attachmentDirection ? this.getBlock().getStateForPlacement(p_43255_) : blockstate;
            if (blockstate2 != null && this.canPlace(levelreader, blockstate2, blockpos)) {
               blockstate1 = blockstate2;
               break;
            }
         }
      }

      return blockstate1 != null && levelreader.isUnobstructed(blockstate1, blockpos, CollisionContext.empty()) ? blockstate1 : null;
   }

   @Override
   public void registerBlocks(Map<Block, Item> p_43252_, Item p_43253_) {
      super.registerBlocks(p_43252_, p_43253_);
      p_43252_.put(this.wallBlock, p_43253_);
   }
}
