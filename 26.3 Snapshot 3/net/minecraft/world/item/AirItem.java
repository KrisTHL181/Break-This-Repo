package net.minecraft.world.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class AirItem extends Item {
   public AirItem(final Item.Properties properties) {
      super(properties);
   }

   @Override
   public Component getName(final ItemStack itemStack) {
      return itemStack.typeHolder().components().getOrDefault(DataComponents.ITEM_NAME, CommonComponents.EMPTY);
   }
}
