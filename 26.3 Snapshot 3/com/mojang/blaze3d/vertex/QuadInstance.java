package com.mojang.blaze3d.vertex;

import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;

public class QuadInstance {
   private int color0 = -1;
   private int color1 = -1;
   private int color2 = -1;
   private int color3 = -1;
   private int lightCoords0 = 15728880;
   private int lightCoords1 = 15728880;
   private int lightCoords2 = 15728880;
   private int lightCoords3 = 15728880;
   private int overlayCoords = OverlayTexture.NO_OVERLAY;

   public int getColor(final int vertex) {
      return switch (vertex) {
         case 0 -> this.color0;
         case 1 -> this.color1;
         case 2 -> this.color2;
         case 3 -> this.color3;
         default -> throw new IndexOutOfBoundsException();
      };
   }

   public int getLightCoords(final int vertex) {
      return switch (vertex) {
         case 0 -> this.lightCoords0;
         case 1 -> this.lightCoords1;
         case 2 -> this.lightCoords2;
         case 3 -> this.lightCoords3;
         default -> throw new IndexOutOfBoundsException();
      };
   }

   public int getLightCoordsWithEmission(final int vertex, final int lightEmission) {
      return LightCoordsUtil.lightCoordsWithEmission(this.getLightCoords(vertex), lightEmission);
   }

   public int overlayCoords() {
      return this.overlayCoords;
   }

   public void setColor(final int vertex, final int color) {
      switch (vertex) {
         case 0:
            this.color0 = color;
            break;
         case 1:
            this.color1 = color;
            break;
         case 2:
            this.color2 = color;
            break;
         case 3:
            this.color3 = color;
            break;
         default:
            throw new IndexOutOfBoundsException();
      }
   }

   public void setLightCoords(final int vertex, final int lightCoords) {
      switch (vertex) {
         case 0:
            this.lightCoords0 = lightCoords;
            break;
         case 1:
            this.lightCoords1 = lightCoords;
            break;
         case 2:
            this.lightCoords2 = lightCoords;
            break;
         case 3:
            this.lightCoords3 = lightCoords;
            break;
         default:
            throw new IndexOutOfBoundsException();
      }
   }

   public void setColor(final int color) {
      this.color0 = color;
      this.color1 = color;
      this.color2 = color;
      this.color3 = color;
   }

   public void setLightCoords(final int lightCoords) {
      this.lightCoords0 = lightCoords;
      this.lightCoords1 = lightCoords;
      this.lightCoords2 = lightCoords;
      this.lightCoords3 = lightCoords;
   }

   public void setOverlayCoords(final int overlayCoords) {
      this.overlayCoords = overlayCoords;
   }

   public void multiplyColor(final int color) {
      this.color0 = ARGB.multiply(this.color0, color);
      this.color1 = ARGB.multiply(this.color1, color);
      this.color2 = ARGB.multiply(this.color2, color);
      this.color3 = ARGB.multiply(this.color3, color);
   }

   public void scaleColor(final float scale) {
      this.color0 = ARGB.scaleRGB(this.color0, scale);
      this.color1 = ARGB.scaleRGB(this.color1, scale);
      this.color2 = ARGB.scaleRGB(this.color2, scale);
      this.color3 = ARGB.scaleRGB(this.color3, scale);
   }
}
