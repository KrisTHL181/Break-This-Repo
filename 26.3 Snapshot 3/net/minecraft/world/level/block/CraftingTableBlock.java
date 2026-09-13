package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class CraftingTableBlock extends Block {
   private static final Component CONTAINER_TITLE = Component.translatable("container.crafting");

   protected CraftingTableBlock(final BlockBehaviour.Properties properties) {
      super(properties);
   }

   @Override
   protected InteractionResult useWithoutItem(
      final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult
   ) {
      if (!level.isClientSide()) {
         player.openMenu(state.getMenuProvider(level, pos));
         player.awardStat(Stats.INTERACT_WITH_CRAFTING_TABLE);
      }

      return InteractionResult.SUCCESS;
   }

   @Override
   protected MenuProvider getMenuProvider(final BlockState state, final Level level, final BlockPos pos) {
      return new SimpleMenuProvider(
         (containerId, inventory, player) -> new CraftingMenu(containerId, inventory, ContainerLevelAccess.create(level, pos)), CONTAINER_TITLE
      );
   }
}
