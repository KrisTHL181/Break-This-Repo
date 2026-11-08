package net.minecraft.client.gui.screens;

import net.minecraft.client.gui.components.Renderable;

public abstract class Overlay implements Renderable {
   public boolean isPausing() {
      return true;
   }

   public void tick() {
   }
}
