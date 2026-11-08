package net.minecraft.world.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

public class AirItem extends Item {
   public AirItem(Block p_40368_, Item.Properties p_40369_) {
      super(p_40369_);
   }

   @Override
   public Component getName(ItemStack p_365938_) {
      return this.getName();
   }
}
