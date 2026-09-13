package net.minecraft.world.item;

import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class WritableBookItem extends Item {
   public WritableBookItem(Item.Properties p_43445_) {
      super(p_43445_);
   }

   @Override
   public InteractionResult use(Level p_43449_, Player p_43450_, InteractionHand p_43451_) {
      ItemStack itemstack = p_43450_.getItemInHand(p_43451_);
      p_43450_.openItemGui(itemstack, p_43451_);
      p_43450_.awardStat(Stats.ITEM_USED.get(this));
      return InteractionResult.SUCCESS;
   }
}
