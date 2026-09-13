package net.minecraft.realms;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsLabel implements Renderable {
   private final Component text;
   private final int x;
   private final int y;
   private final int color;

   public RealmsLabel(Component p_120736_, int p_120737_, int p_120738_, int p_120739_) {
      this.text = p_120736_;
      this.x = p_120737_;
      this.y = p_120738_;
      this.color = p_120739_;
   }

   @Override
   public void render(GuiGraphics p_281597_, int p_282874_, int p_281694_, float p_282363_) {
      p_281597_.drawCenteredString(Minecraft.getInstance().font, this.text, this.x, this.y, this.color);
   }

   public Component getText() {
      return this.text;
   }
}
