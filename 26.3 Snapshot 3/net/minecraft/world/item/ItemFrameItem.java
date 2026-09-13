package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;

public class ItemFrameItem extends HangingEntityItem {
   public ItemFrameItem(final EntityType<? extends HangingEntity> entityType, final Item.Properties properties) {
      super(entityType, properties);
   }

   @Override
   protected boolean mayPlace(final Player player, final Direction direction, final ItemStack itemStack, final BlockPos blockPos) {
      return player.level().isInsideBuildHeight(blockPos) && player.mayUseItemAt(blockPos, direction, itemStack);
   }
}
