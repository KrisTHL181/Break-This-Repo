package net.minecraft.world.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;

public class ShieldItem extends Item {
   public ShieldItem(Item.Properties p_43089_) {
      super(p_43089_);
   }

   @Override
   public Component getName(ItemStack p_360971_) {
      DyeColor dyecolor = p_360971_.get(DataComponents.BASE_COLOR);
      return dyecolor != null ? Component.translatable(this.descriptionId + "." + dyecolor.getName()) : super.getName(p_360971_);
   }
}
