package net.minecraft.world.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.component.TooltipDisplay;

public class DiscFragmentItem extends Item {
   public DiscFragmentItem(Item.Properties p_220029_) {
      super(p_220029_);
   }

   @Override
   public void appendHoverText(
      ItemStack p_220031_, Item.TooltipContext p_327830_, TooltipDisplay p_391187_, Consumer<Component> p_395377_, TooltipFlag p_220034_
   ) {
      p_395377_.accept(this.getDisplayName().withStyle(ChatFormatting.GRAY));
   }

   public MutableComponent getDisplayName() {
      return Component.translatable(this.descriptionId + ".desc");
   }
}
