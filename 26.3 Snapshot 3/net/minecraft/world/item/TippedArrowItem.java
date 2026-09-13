package net.minecraft.world.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

public class TippedArrowItem extends ArrowItem {
   public TippedArrowItem(final Item.Properties properties) {
      super(properties);
   }

   @Override
   public ItemStack getDefaultInstance() {
      ItemStack itemStack = super.getDefaultInstance();
      itemStack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.POISON));
      return itemStack;
   }

   @Override
   public Component getName(final ItemStack itemStack) {
      PotionContents potion = itemStack.get(DataComponents.POTION_CONTENTS);
      return potion != null ? potion.getName(this.descriptionId + ".effect.") : super.getName(itemStack);
   }
}
