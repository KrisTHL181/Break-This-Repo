package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class ScaffoldingBlockItem extends BlockItem {
   public ScaffoldingBlockItem(final Block block, final Item.Properties properties) {
      super(block, properties);
   }

   @Override
   public @Nullable BlockPlaceContext updatePlacementContext(final BlockPlaceContext context) {
      BlockPos pos = context.getClickedPos();
      Level level = context.getLevel();
      BlockState replacedState = level.getBlockState(pos);
      Block block = this.getBlock();
      if (!replacedState.is(block)) {
         return ScaffoldingBlock.getDistance(level, pos) == 7 ? null : context;
      }

      Direction direction;
      if (context.isSecondaryUseActive()) {
         direction = context.isInside() ? context.getClickedFace().getOpposite() : context.getClickedFace();
      } else {
         direction = context.getClickedFace() == Direction.UP ? context.getHorizontalDirection() : Direction.UP;
      }

      int horizontalDistance = 0;
      BlockPos.MutableBlockPos placementPos = pos.mutable().move(direction);

      while (horizontalDistance < 7) {
         if (!level.isClientSide() && !level.isInWorldBounds(placementPos)) {
            Player player = context.getPlayer();
            int maxY = level.getMaxY();
            if (player instanceof ServerPlayer serverPlayer && placementPos.getY() > maxY) {
               serverPlayer.sendBuildLimitMessage(true, maxY);
            }
            break;
         }

         replacedState = level.getBlockState(placementPos);
         if (!replacedState.is(this.getBlock())) {
            if (replacedState.canBeReplaced(context)) {
               return BlockPlaceContext.at(context, placementPos, direction);
            }
            break;
         }

         placementPos.move(direction);
         if (direction.getAxis().isHorizontal()) {
            horizontalDistance++;
         }
      }

      return null;
   }

   @Override
   protected boolean mustSurvive() {
      return false;
   }
}
