package net.minecraft.world.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class GameMasterBlockItem extends BlockItem {
   public GameMasterBlockItem(final Block block, final Item.Properties properties) {
      super(block, properties);
   }

   @Override
   protected @Nullable BlockState getPlacementState(final BlockPlaceContext context) {
      Player player = context.getPlayer();
      return player != null && !player.canUseGameMasterBlocks() ? null : super.getPlacementState(context);
   }
}
