package com.mojang.blaze3d.platform;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWVidMode.Buffer;

public final class VideoMode {
   private final int width;
   private final int height;
   private final int redBits;
   private final int greenBits;
   private final int blueBits;
   private final int refreshRate;
   private static final Pattern PATTERN = Pattern.compile("(\\d+)x(\\d+)(?:@(\\d+)(?::(\\d+))?)?");

   public VideoMode(final int width, final int height, final int redBits, final int greenBits, final int blueBits, final int refreshRate) {
      this.width = width;
      this.height = height;
      this.redBits = redBits;
      this.greenBits = greenBits;
      this.blueBits = blueBits;
      this.refreshRate = refreshRate;
   }

   public VideoMode(final Buffer buffer) {
      this.width = buffer.width();
      this.height = buffer.height();
      this.redBits = buffer.redBits();
      this.greenBits = buffer.greenBits();
      this.blueBits = buffer.blueBits();
      this.refreshRate = buffer.refreshRate();
   }

   public VideoMode(final GLFWVidMode mode) {
      this.width = mode.width();
      this.height = mode.height();
      this.redBits = mode.redBits();
      this.greenBits = mode.greenBits();
      this.blueBits = mode.blueBits();
      this.refreshRate = mode.refreshRate();
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public int getRedBits() {
      return this.redBits;
   }

   public int getGreenBits() {
      return this.greenBits;
   }

   public int getBlueBits() {
      return this.blueBits;
   }

   public int getRefreshRate() {
      return this.refreshRate;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         VideoMode videoMode = (VideoMode)o;
         return this.width == videoMode.width
            && this.height == videoMode.height
            && this.redBits == videoMode.redBits
            && this.greenBits == videoMode.greenBits
            && this.blueBits == videoMode.blueBits
            && this.refreshRate == videoMode.refreshRate;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.width, this.height, this.redBits, this.greenBits, this.blueBits, this.refreshRate);
   }

   @Override
   public String toString() {
      return String.format(Locale.ROOT, "%sx%s@%s (%sbit)", this.width, this.height, this.refreshRate, this.redBits + this.greenBits + this.blueBits);
   }

   public static Optional<VideoMode> read(final @Nullable String s) {
      if (s == null) {
         return Optional.empty();
      }

      try {
         Matcher m = PATTERN.matcher(s);
         if (m.matches()) {
            int width = Integer.parseInt(m.group(1));
            int height = Integer.parseInt(m.group(2));
            String rateString = m.group(3);
            int rate;
            if (rateString == null) {
               rate = 60;
            } else {
               rate = Integer.parseInt(rateString);
            }

            String bitString = m.group(4);
            int bits;
            if (bitString == null) {
               bits = 24;
            } else {
               bits = Integer.parseInt(bitString);
            }

            int componentBits = bits / 3;
            return Optional.of(new VideoMode(width, height, componentBits, componentBits, componentBits, rate));
         }
      } catch (Exception var9) {
      }

      return Optional.empty();
   }

   public String write() {
      return String.format(Locale.ROOT, "%sx%s@%s:%s", this.width, this.height, this.refreshRate, this.redBits + this.greenBits + this.blueBits);
   }
}
