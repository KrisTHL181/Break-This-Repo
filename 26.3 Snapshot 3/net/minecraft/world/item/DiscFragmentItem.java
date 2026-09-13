package net.minecraft.world.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.component.TooltipDisplay;

public class DiscFragmentItem extends Item {
   public DiscFragmentItem(final Item.Properties properties) {
      super(properties);
   }

   @Override
   public void appendHoverText(
      final ItemStack itemStack,
      final Item.TooltipContext context,
      final TooltipDisplay display,
      final Consumer<Component> builder,
      final TooltipFlag tooltipFlag
   ) {
      builder.accept(this.getDisplayName().withStyle(ChatFormatting.GRAY));
   }

   public MutableComponent getDisplayName() {
      return Component.translatable(this.descriptionId + ".desc");
   }
}
