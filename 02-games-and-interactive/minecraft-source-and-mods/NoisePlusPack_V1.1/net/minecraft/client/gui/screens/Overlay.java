package net.minecraft.client.gui.screens;

import net.minecraft.client.gui.components.Renderable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class Overlay implements Renderable {
   public boolean isPauseScreen() {
      return true;
   }

   public void tick() {
   }
}
