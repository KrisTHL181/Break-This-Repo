package net.minecraft.world.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;

public class ShieldItem extends Item {
   public ShieldItem(final Item.Properties properties) {
      super(properties);
   }

   @Override
   public Component getName(final ItemStack itemStack) {
      DyeColor baseColor = itemStack.get(DataComponents.BASE_COLOR);
      return baseColor != null ? Component.translatable(this.descriptionId + "." + baseColor.getName()) : super.getName(itemStack);
   }
}
