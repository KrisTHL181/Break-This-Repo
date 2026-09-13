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

   public StandingAndWallBlockItem(final Block block, final Block wallBlock, final Direction attachmentDirection, final Item.Properties properties) {
      super(block, properties);
      this.wallBlock = wallBlock;
      this.attachmentDirection = attachmentDirection;
   }

   protected boolean canPlace(final LevelReader level, final BlockState possibleState, final BlockPos pos) {
      return possibleState.canSurvive(level, pos);
   }

   @Override
   protected @Nullable BlockState getPlacementState(final BlockPlaceContext context) {
      BlockState wallState = this.wallBlock.getStateForPlacement(context);
      BlockState stateForPlacement = null;
      LevelReader level = context.getLevel();
      BlockPos pos = context.getClickedPos();

      for (Direction direction : context.getNearestLookingDirections()) {
         if (direction != this.attachmentDirection.getOpposite()) {
            BlockState possibleState = direction == this.attachmentDirection ? this.getBlock().getStateForPlacement(context) : wallState;
            if (possibleState != null && this.canPlace(level, possibleState, pos)) {
               stateForPlacement = possibleState;
               break;
            }
         }
      }

      return stateForPlacement != null && level.isUnobstructed(stateForPlacement, pos, CollisionContext.empty()) ? stateForPlacement : null;
   }

   @Override
   public void registerBlocks(final Map<Block, Item> map, final Item item) {
      super.registerBlocks(map, item);
      map.put(this.wallBlock, item);
   }
}
