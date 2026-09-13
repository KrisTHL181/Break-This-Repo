package net.minecraft.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ItemDisplayWidget extends AbstractWidget {
   private final Minecraft minecraft;
   private final int offsetX;
   private final int offsetY;
   private final ItemStack itemStack;
   private final boolean decorations;
   private final boolean tooltip;

   public ItemDisplayWidget(
      final Minecraft minecraft,
      final int offsetX,
      final int offsetY,
      final int width,
      final int height,
      final Component message,
      final ItemStack itemStack,
      final boolean decorations,
      final boolean tooltip
   ) {
      super(0, 0, width, height, message);
      this.minecraft = minecraft;
      this.offsetX = offsetX;
      this.offsetY = offsetY;
      this.itemStack = itemStack;
      this.decorations = decorations;
      this.tooltip = tooltip;
   }

   @Override
   protected void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      graphics.item(this.itemStack, this.getX() + this.offsetX, this.getY() + this.offsetY, 0);
      if (this.decorations) {
         graphics.itemDecorations(this.minecraft.font, this.itemStack, this.getX() + this.offsetX, this.getY() + this.offsetY, null);
      }

      if (this.isFocused()) {
         graphics.outline(this.getX(), this.getY(), this.getWidth(), this.getHeight(), -1);
      }

      if (this.tooltip && this.isHovered()) {
         this.extractTooltip(graphics, mouseX, mouseY);
      }
   }

   protected void extractTooltip(final GuiGraphicsExtractor graphics, final int x, final int y) {
      graphics.setTooltipForNextFrame(this.minecraft.font, this.itemStack, x, y);
   }

   @Override
   protected void updateWidgetNarration(final NarrationElementOutput output) {
      output.add(NarratedElementType.TITLE, Component.translatable("narration.item", this.itemStack.getHoverName()));
   }
}
