package net.minecraft.client.gui.font;

import net.minecraft.network.chat.Style;

public interface ActiveArea {
   Style style();

   float activeLeft();

   float activeTop();

   float activeRight();

   float activeBottom();
}
