package net.minecraft.client.gui.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface Renderable {
   void render(GuiGraphics var1, int var2, int var3, float var4);
}
