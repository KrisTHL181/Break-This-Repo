package net.minecraft.client.gui.spectator;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public interface SpectatorMenuItem {
   void selectItem(SpectatorMenu menu);

   Component getName();

   void extractIcon(final GuiGraphicsExtractor graphics, float brightness, float alpha);

   boolean isEnabled();
}
