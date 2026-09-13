package net.minecraft.world.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class ScaffoldingBlockItem extends BlockItem {
   public ScaffoldingBlockItem(Block p_43060_, Item.Properties p_43061_) {
      super(p_43060_, p_43061_);
   }

   @Override
   public @Nullable BlockPlaceContext updatePlacementContext(BlockPlaceContext p_43063_) {
      BlockPos blockpos = p_43063_.getClickedPos();
      Level level = p_43063_.getLevel();
      BlockState blockstate = level.getBlockState(blockpos);
      Block block = this.getBlock();
      if (!blockstate.is(block)) {
         return ScaffoldingBlock.getDistance(level, blockpos) == 7 ? null : p_43063_;
      }

      Direction direction;
      if (p_43063_.isSecondaryUseActive()) {
         direction = p_43063_.isInside() ? p_43063_.getClickedFace().getOpposite() : p_43063_.getClickedFace();
      } else {
         direction = p_43063_.getClickedFace() == Direction.UP ? p_43063_.getHorizontalDirection() : Direction.UP;
      }

      int i = 0;
      BlockPos.MutableBlockPos blockpos$mutableblockpos = blockpos.mutable().move(direction);

      while (i < 7) {
         if (!level.isClientSide() && !level.isInWorldBounds(blockpos$mutableblockpos)) {
            Player player = p_43063_.getPlayer();
            int j = level.getMaxY();
            if (player instanceof ServerPlayer && blockpos$mutableblockpos.getY() > j) {
               ((ServerPlayer)player).sendSystemMessage(Component.translatable("build.tooHigh", j).withStyle(ChatFormatting.RED), true);
            }
            break;
         }

         blockstate = level.getBlockState(blockpos$mutableblockpos);
         if (!blockstate.is(this.getBlock())) {
            if (blockstate.canBeReplaced(p_43063_)) {
               return BlockPlaceContext.at(p_43063_, blockpos$mutableblockpos, direction);
            }
            break;
         }

         blockpos$mutableblockpos.move(direction);
         if (direction.getAxis().isHorizontal()) {
            i++;
         }
      }

      return null;
   }

   @Override
   protected boolean mustSurvive() {
      return false;
   }
}
