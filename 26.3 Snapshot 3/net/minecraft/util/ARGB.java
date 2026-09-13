package net.minecraft.util;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class ARGB {
   private static final int LINEAR_CHANNEL_DEPTH = 1024;
   private static final short[] SRGB_TO_LINEAR = Util.make(new short[256], lookup -> {
      for (int i = 0; i < lookup.length; i++) {
         float channel = i / 255.0F;
         lookup[i] = (short)Math.round(computeSrgbToLinear(channel) * 1023.0F);
      }
   });
   private static final byte[] LINEAR_TO_SRGB = Util.make(new byte[1024], lookup -> {
      for (int i = 0; i < lookup.length; i++) {
         float channel = i / 1023.0F;
         lookup[i] = (byte)Math.round(computeLinearToSrgb(channel) * 255.0F);
      }
   });

   private static float computeSrgbToLinear(final float x) {
      return x >= 0.04045F ? (float)Math.pow((x + 0.055) / 1.055, 2.4) : x / 12.92F;
   }

   private static float computeLinearToSrgb(final float x) {
      return x >= 0.0031308F ? (float)(1.055 * Math.pow(x, 0.4166666666666667) - 0.055) : 12.92F * x;
   }

   public static float srgbToLinearChannel(final int srgb) {
      return SRGB_TO_LINEAR[srgb] / 1023.0F;
   }

   public static int linearToSrgbChannel(final float linear) {
      return LINEAR_TO_SRGB[Mth.floor(linear * 1023.0F)] & 0xFF;
   }

   public static int meanLinear(final int srgb1, final int srgb2, final int srgb3, final int srgb4) {
      return color(
         (alpha(srgb1) + alpha(srgb2) + alpha(srgb3) + alpha(srgb4)) / 4,
         linearChannelMean(red(srgb1), red(srgb2), red(srgb3), red(srgb4)),
         linearChannelMean(green(srgb1), green(srgb2), green(srgb3), green(srgb4)),
         linearChannelMean(blue(srgb1), blue(srgb2), blue(srgb3), blue(srgb4))
      );
   }

   private static int linearChannelMean(final int c1, final int c2, final int c3, final int c4) {
      int linear = (SRGB_TO_LINEAR[c1] + SRGB_TO_LINEAR[c2] + SRGB_TO_LINEAR[c3] + SRGB_TO_LINEAR[c4]) / 4;
      return LINEAR_TO_SRGB[linear] & 0xFF;
   }

   public static int alpha(final int color) {
      return color >>> 24;
   }

   public static int red(final int color) {
      return color >> 16 & 0xFF;
   }

   public static int green(final int color) {
      return color >> 8 & 0xFF;
   }

   public static int blue(final int color) {
      return color & 0xFF;
   }

   public static int color(final int alpha, final int red, final int green, final int blue) {
      return (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
   }

   public static int color(final int red, final int green, final int blue) {
      return color(255, red, green, blue);
   }

   public static int color(final Vec3 vec) {
      return color(as8BitChannel((float)vec.x()), as8BitChannel((float)vec.y()), as8BitChannel((float)vec.z()));
   }

   public static int multiply(final int lhs, final int rhs) {
      if (lhs == -1) {
         return rhs;
      } else {
         return rhs == -1 ? lhs : color(alpha(lhs) * alpha(rhs) / 255, red(lhs) * red(rhs) / 255, green(lhs) * green(rhs) / 255, blue(lhs) * blue(rhs) / 255);
      }
   }

   public static int addRgb(final int lhs, final int rhs) {
      return color(alpha(lhs), Math.min(red(lhs) + red(rhs), 255), Math.min(green(lhs) + green(rhs), 255), Math.min(blue(lhs) + blue(rhs), 255));
   }

   public static int subtractRgb(final int lhs, final int rhs) {
      return color(alpha(lhs), Math.max(red(lhs) - red(rhs), 0), Math.max(green(lhs) - green(rhs), 0), Math.max(blue(lhs) - blue(rhs), 0));
   }

   public static int multiplyAlpha(final int color, final float alphaMultiplier) {
      if (color == 0 || alphaMultiplier <= 0.0F) {
         return 0;
      } else {
         return alphaMultiplier >= 1.0F ? color : color(alphaFloat(color) * alphaMultiplier, color);
      }
   }

   public static int scaleRGB(final int color, final float scale) {
      return scaleRGB(color, scale, scale, scale);
   }

   public static int scaleRGB(final int color, final float scaleR, final float scaleG, final float scaleB) {
      return color(
         alpha(color),
         Math.clamp((int)(red(color) * scaleR), 0, 255),
         Math.clamp((int)(green(color) * scaleG), 0, 255),
         Math.clamp((int)(blue(color) * scaleB), 0, 255)
      );
   }

   public static int scaleRGB(final int color, final int scale) {
      return color(
         alpha(color),
         Math.clamp((long)red(color) * scale / 255L, 0, 255),
         Math.clamp((long)green(color) * scale / 255L, 0, 255),
         Math.clamp((long)blue(color) * scale / 255L, 0, 255)
      );
   }

   public static int greyscale(final int color) {
      int greyscale = (int)(red(color) * 0.3F + green(color) * 0.59F + blue(color) * 0.11F);
      return color(alpha(color), greyscale, greyscale, greyscale);
   }

   public static int alphaBlend(final int destination, final int source) {
      int destinationAlpha = alpha(destination);
      int sourceAlpha = alpha(source);
      if (sourceAlpha == 255) {
         return source;
      }

      if (sourceAlpha == 0) {
         return destination;
      }

      int alpha = sourceAlpha + destinationAlpha * (255 - sourceAlpha) / 255;
      return color(
         alpha,
         alphaBlendChannel(alpha, sourceAlpha, red(destination), red(source)),
         alphaBlendChannel(alpha, sourceAlpha, green(destination), green(source)),
         alphaBlendChannel(alpha, sourceAlpha, blue(destination), blue(source))
      );
   }

   private static int alphaBlendChannel(final int resultAlpha, final int sourceAlpha, final int destination, final int source) {
      return (source * sourceAlpha + destination * (resultAlpha - sourceAlpha)) / resultAlpha;
   }

   public static int srgbLerp(final float alpha, final int p0, final int p1) {
      int a = Mth.lerpInt(alpha, alpha(p0), alpha(p1));
      int red = Mth.lerpInt(alpha, red(p0), red(p1));
      int green = Mth.lerpInt(alpha, green(p0), green(p1));
      int blue = Mth.lerpInt(alpha, blue(p0), blue(p1));
      return color(a, red, green, blue);
   }

   public static int linearLerp(final float alpha, final int p0, final int p1) {
      return color(
         Mth.lerpInt(alpha, alpha(p0), alpha(p1)),
         LINEAR_TO_SRGB[Mth.lerpInt(alpha, SRGB_TO_LINEAR[red(p0)], SRGB_TO_LINEAR[red(p1)])] & 0xFF,
         LINEAR_TO_SRGB[Mth.lerpInt(alpha, SRGB_TO_LINEAR[green(p0)], SRGB_TO_LINEAR[green(p1)])] & 0xFF,
         LINEAR_TO_SRGB[Mth.lerpInt(alpha, SRGB_TO_LINEAR[blue(p0)], SRGB_TO_LINEAR[blue(p1)])] & 0xFF
      );
   }

   public static int opaque(final int color) {
      return color | 0xFF000000;
   }

   public static int transparent(final int color) {
      return color & 16777215;
   }

   public static int color(final int alpha, final int rgb) {
      return alpha << 24 | rgb & 16777215;
   }

   public static int color(final float alpha, final int rgb) {
      return as8BitChannel(alpha) << 24 | rgb & 16777215;
   }

   public static int white(final float alpha) {
      return as8BitChannel(alpha) << 24 | 16777215;
   }

   public static int white(final int alpha) {
      return alpha << 24 | 16777215;
   }

   public static int black(final float alpha) {
      return as8BitChannel(alpha) << 24;
   }

   public static int black(final int alpha) {
      return alpha << 24;
   }

   public static int gray(final float brightness) {
      int channel = as8BitChannel(brightness);
      return color(channel, channel, channel);
   }

   public static int colorFromFloat(final float alpha, final float red, final float green, final float blue) {
      return color(as8BitChannel(alpha), as8BitChannel(red), as8BitChannel(green), as8BitChannel(blue));
   }

   public static Vector3f vector3fFromRGB24(final int color) {
      return new Vector3f(redFloat(color), greenFloat(color), blueFloat(color));
   }

   public static Vector4f vector4fFromARGB32(final int color) {
      return new Vector4f(redFloat(color), greenFloat(color), blueFloat(color), alphaFloat(color));
   }

   public static Vector4f setVector4fFromARGB32(final Vector4f dest, final int color) {
      return dest.set(redFloat(color), greenFloat(color), blueFloat(color), alphaFloat(color));
   }

   public static int average(final int lhs, final int rhs) {
      return color((alpha(lhs) + alpha(rhs)) / 2, (red(lhs) + red(rhs)) / 2, (green(lhs) + green(rhs)) / 2, (blue(lhs) + blue(rhs)) / 2);
   }

   public static int as8BitChannel(final float value) {
      return Mth.floor(value * 255.0F);
   }

   public static float alphaFloat(final int color) {
      return from8BitChannel(alpha(color));
   }

   public static float redFloat(final int color) {
      return from8BitChannel(red(color));
   }

   public static float greenFloat(final int color) {
      return from8BitChannel(green(color));
   }

   public static float blueFloat(final int color) {
      return from8BitChannel(blue(color));
   }

   private static float from8BitChannel(final int value) {
      return value / 255.0F;
   }

   public static int toABGR(final int color) {
      return color & -16711936 | (color & 0xFF0000) >> 16 | (color & 0xFF) << 16;
   }

   public static int fromABGR(final int color) {
      return toABGR(color);
   }

   public static int setBrightness(final int color, final float brightness) {
      int red = red(color);
      int green = green(color);
      int blue = blue(color);
      int alpha = alpha(color);
      int rgbMax = Math.max(Math.max(red, green), blue);
      int rgbMin = Math.min(Math.min(red, green), blue);
      float rgbConstantRange = rgbMax - rgbMin;
      float saturation;
      if (rgbMax != 0) {
         saturation = rgbConstantRange / rgbMax;
      } else {
         saturation = 0.0F;
      }

      float hue;
      if (saturation == 0.0F) {
         hue = 0.0F;
      } else {
         float constantRed = (rgbMax - red) / rgbConstantRange;
         float constantGreen = (rgbMax - green) / rgbConstantRange;
         float constantBlue = (rgbMax - blue) / rgbConstantRange;
         if (red == rgbMax) {
            hue = constantBlue - constantGreen;
         } else if (green == rgbMax) {
            hue = 2.0F + constantRed - constantBlue;
         } else {
            hue = 4.0F + constantGreen - constantRed;
         }

         hue /= 6.0F;
         if (hue < 0.0F) {
            hue++;
         }
      }

      if (saturation == 0.0F) {
         red = green = blue = Math.round(brightness * 255.0F);
         return color(alpha, red, green, blue);
      }

      float colorWheelSegment = (hue - (float)Math.floor(hue)) * 6.0F;
      float colorWheelOffset = colorWheelSegment - (float)Math.floor(colorWheelSegment);
      float primaryColor = brightness * (1.0F - saturation);
      float secondaryColor = brightness * (1.0F - saturation * colorWheelOffset);
      float tertiaryColor = brightness * (1.0F - saturation * (1.0F - colorWheelOffset));
      switch ((int)colorWheelSegment) {
         case 0:
            red = Math.round(brightness * 255.0F);
            green = Math.round(tertiaryColor * 255.0F);
            blue = Math.round(primaryColor * 255.0F);
            break;
         case 1:
            red = Math.round(secondaryColor * 255.0F);
            green = Math.round(brightness * 255.0F);
            blue = Math.round(primaryColor * 255.0F);
            break;
         case 2:
            red = Math.round(primaryColor * 255.0F);
            green = Math.round(brightness * 255.0F);
            blue = Math.round(tertiaryColor * 255.0F);
            break;
         case 3:
            red = Math.round(primaryColor * 255.0F);
            green = Math.round(secondaryColor * 255.0F);
            blue = Math.round(brightness * 255.0F);
            break;
         case 4:
            red = Math.round(tertiaryColor * 255.0F);
            green = Math.round(primaryColor * 255.0F);
            blue = Math.round(brightness * 255.0F);
            break;
         case 5:
            red = Math.round(brightness * 255.0F);
            green = Math.round(primaryColor * 255.0F);
            blue = Math.round(secondaryColor * 255.0F);
      }

      return color(alpha, red, green, blue);
   }
}
