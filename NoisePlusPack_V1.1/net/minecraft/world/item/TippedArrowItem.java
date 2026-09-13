package net.minecraft.world.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

public class TippedArrowItem extends ArrowItem {
   public TippedArrowItem(Item.Properties p_43354_) {
      super(p_43354_);
   }

   @Override
   public ItemStack getDefaultInstance() {
      ItemStack itemstack = super.getDefaultInstance();
      itemstack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.POISON));
      return itemstack;
   }

   @Override
   public Component getName(ItemStack p_367491_) {
      PotionContents potioncontents = p_367491_.get(DataComponents.POTION_CONTENTS);
      return potioncontents != null ? potioncontents.getName(this.descriptionId + ".effect.") : super.getName(p_367491_);
   }
}
