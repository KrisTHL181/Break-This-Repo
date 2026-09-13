package net.minecraft.world.item;

import java.util.stream.Stream;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class ItemUtils {
   public static InteractionResult startUsingInstantly(final Level level, final Player player, final InteractionHand hand) {
      player.startUsingItem(hand);
      return InteractionResult.CONSUME;
   }

   public static ItemStack createFilledResult(
      final ItemStack itemStack, final Player player, final ItemStack newItemStack, final boolean limitCreativeStackSize
   ) {
      boolean isCreative = player.hasInfiniteMaterials();
      if (limitCreativeStackSize && isCreative) {
         if (!player.getInventory().contains(newItemStack)) {
            player.getInventory().add(newItemStack);
         }

         return itemStack;
      } else {
         itemStack.consume(1, player);
         if (itemStack.isEmpty()) {
            return newItemStack;
         }

         if (!player.getInventory().add(newItemStack)) {
            player.drop(newItemStack, false);
         }

         return itemStack;
      }
   }

   public static ItemStack createFilledResult(final ItemStack itemStack, final Player player, final ItemStack newItemStack) {
      return createFilledResult(itemStack, player, newItemStack, true);
   }

   public static void onContainerDestroyed(final ItemEntity container, final Stream<ItemStack> contents) {
      Level level = container.level();
      if (!level.isClientSide()) {
         contents.forEach(stack -> level.addFreshEntity(new ItemEntity(level, container.getX(), container.getY(), container.getZ(), stack)));
      }
   }
}
