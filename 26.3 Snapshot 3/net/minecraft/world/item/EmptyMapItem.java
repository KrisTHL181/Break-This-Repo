package net.minecraft.world.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class EmptyMapItem extends Item {
   public EmptyMapItem(final Item.Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      if (level instanceof ServerLevel serverLevel) {
         itemStack.consume(1, player);
         player.awardStat(Stats.ITEM_USED.get(this));
         serverLevel.playSound(null, player, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, player.getSoundSource(), 1.0F, 1.0F);
         ItemStack map = MapItem.create(serverLevel, player.getBlockX(), player.getBlockZ(), (byte)0, true, false);
         if (itemStack.isEmpty()) {
            return InteractionResult.SUCCESS.heldItemTransformedTo(map);
         }

         if (!player.getInventory().add(map.copy())) {
            player.drop(map, false);
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.SUCCESS;
      }
   }
}
