package net.minecraft.client.gui.components;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface Renderable {
   void extractRenderState(final GuiGraphicsExtractor graphics, int mouseX, int mouseY, final float a);
}
