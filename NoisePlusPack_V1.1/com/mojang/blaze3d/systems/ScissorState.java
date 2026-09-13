package com.mojang.blaze3d.systems;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ScissorState {
   private boolean enabled;
   private int x;
   private int y;
   private int width;
   private int height;

   public void enable(int p_392490_, int p_395265_, int p_398025_, int p_397710_) {
      this.enabled = true;
      this.x = p_392490_;
      this.y = p_395265_;
      this.width = p_398025_;
      this.height = p_397710_;
   }

   public void disable() {
      this.enabled = false;
   }

   public boolean enabled() {
      return this.enabled;
   }

   public int x() {
      return this.x;
   }

   public int y() {
      return this.y;
   }

   public int width() {
      return this.width;
   }

   public int height() {
      return this.height;
   }
}
