package net.minecraft.world.item;

import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class WrittenBookItem extends Item {
   public WrittenBookItem(Item.Properties p_43455_) {
      super(p_43455_);
   }

   @Override
   public InteractionResult use(Level p_43468_, Player p_43469_, InteractionHand p_43470_) {
      ItemStack itemstack = p_43469_.getItemInHand(p_43470_);
      p_43469_.openItemGui(itemstack, p_43470_);
      p_43469_.awardStat(Stats.ITEM_USED.get(this));
      return InteractionResult.SUCCESS;
   }
}
