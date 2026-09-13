package net.minecraft.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemDisplayWidget extends AbstractWidget {
   private final Minecraft minecraft;
   private final int offsetX;
   private final int offsetY;
   private final ItemStack itemStack;
   private final boolean decorations;
   private final boolean tooltip;

   public ItemDisplayWidget(
      Minecraft p_409984_,
      int p_407702_,
      int p_406955_,
      int p_408670_,
      int p_407909_,
      Component p_405828_,
      ItemStack p_407541_,
      boolean p_406789_,
      boolean p_408752_
   ) {
      super(0, 0, p_408670_, p_407909_, p_405828_);
      this.minecraft = p_409984_;
      this.offsetX = p_407702_;
      this.offsetY = p_406955_;
      this.itemStack = p_407541_;
      this.decorations = p_406789_;
      this.tooltip = p_408752_;
   }

   @Override
   protected void renderWidget(GuiGraphics p_406193_, int p_406793_, int p_407747_, float p_407971_) {
      p_406193_.renderItem(this.itemStack, this.getX() + this.offsetX, this.getY() + this.offsetY, 0);
      if (this.decorations) {
         p_406193_.renderItemDecorations(this.minecraft.font, this.itemStack, this.getX() + this.offsetX, this.getY() + this.offsetY, null);
      }

      if (this.isFocused()) {
         p_406193_.renderOutline(this.getX(), this.getY(), this.getWidth(), this.getHeight(), -1);
      }

      if (this.tooltip && this.isHoveredOrFocused()) {
         this.renderTooltip(p_406193_, p_406793_, p_407747_);
      }
   }

   protected void renderTooltip(GuiGraphics p_426118_, int p_430049_, int p_425795_) {
      p_426118_.setTooltipForNextFrame(this.minecraft.font, this.itemStack, p_430049_, p_425795_);
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput p_406080_) {
      p_406080_.add(NarratedElementType.TITLE, Component.translatable("narration.item", this.itemStack.getHoverName()));
   }
}
