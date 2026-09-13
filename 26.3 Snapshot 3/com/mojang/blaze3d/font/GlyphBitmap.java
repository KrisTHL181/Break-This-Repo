package com.mojang.blaze3d.font;

import com.mojang.renderpearl.api.textures.GpuTexture;

public interface GlyphBitmap {
   int getPixelWidth();

   int getPixelHeight();

   void upload(int x, int y, GpuTexture texture);

   boolean isColored();

   float getOversample();

   default float getLeft() {
      return this.getBearingLeft();
   }

   default float getRight() {
      return this.getLeft() + this.getPixelWidth() / this.getOversample();
   }

   default float getTop() {
      return 7.0F - this.getBearingTop();
   }

   default float getBottom() {
      return this.getTop() + this.getPixelHeight() / this.getOversample();
   }

   default float getBearingLeft() {
      return 0.0F;
   }

   default float getBearingTop() {
      return 7.0F;
   }
}
