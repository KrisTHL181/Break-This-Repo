package net.minecraft.client.gui.spectator;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface SpectatorMenuItem {
   void selectItem(SpectatorMenu var1);

   Component getName();

   void renderIcon(GuiGraphics var1, float var2, float var3);

   boolean isEnabled();
}
