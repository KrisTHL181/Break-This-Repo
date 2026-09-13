package net.minecraft.world.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class ItemUtils {
   public static InteractionResult startUsingInstantly(Level p_150960_, Player p_150961_, InteractionHand p_150962_) {
      p_150961_.startUsingItem(p_150962_);
      return InteractionResult.CONSUME;
   }

   public static ItemStack createFilledResult(ItemStack p_41818_, Player p_41819_, ItemStack p_41820_, boolean p_41821_) {
      boolean flag = p_41819_.hasInfiniteMaterials();
      if (p_41821_ && flag) {
         if (!p_41819_.getInventory().contains(p_41820_)) {
            p_41819_.getInventory().add(p_41820_);
         }

         return p_41818_;
      } else {
         p_41818_.consume(1, p_41819_);
         if (p_41818_.isEmpty()) {
            return p_41820_;
         }

         if (!p_41819_.getInventory().add(p_41820_)) {
            p_41819_.drop(p_41820_, false);
         }

         return p_41818_;
      }
   }

   public static ItemStack createFilledResult(ItemStack p_41814_, Player p_41815_, ItemStack p_41816_) {
      return createFilledResult(p_41814_, p_41815_, p_41816_, true);
   }

   public static void onContainerDestroyed(ItemEntity p_150953_, Iterable<ItemStack> p_330873_) {
      Level level = p_150953_.level();
      if (!level.isClientSide()) {
         p_330873_.forEach(p_449801_ -> level.addFreshEntity(new ItemEntity(level, p_150953_.getX(), p_150953_.getY(), p_150953_.getZ(), p_449801_)));
      }
   }
}
