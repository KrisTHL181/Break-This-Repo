package net.minecraft.client.gui.screens.inventory.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientTextTooltip implements ClientTooltipComponent {
   private final FormattedCharSequence text;

   public ClientTextTooltip(FormattedCharSequence p_169938_) {
      this.text = p_169938_;
   }

   @Override
   public int getWidth(Font p_169941_) {
      return p_169941_.width(this.text);
   }

   @Override
   public int getHeight(Font p_360905_) {
      return 10;
   }

   @Override
   public void renderText(GuiGraphics p_406088_, Font p_254285_, int p_254192_, int p_253697_) {
      p_406088_.drawString(p_254285_, this.text, p_254192_, p_253697_, -1, true);
   }
}
