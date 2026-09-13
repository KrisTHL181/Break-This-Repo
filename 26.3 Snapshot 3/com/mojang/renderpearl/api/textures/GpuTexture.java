package com.mojang.renderpearl.api.textures;

import com.mojang.renderpearl.api.GpuFormat;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public abstract class GpuTexture implements AutoCloseable {
   public static final int USAGE_COPY_DST = 1;
   public static final int USAGE_COPY_SRC = 2;
   public static final int USAGE_TEXTURE_BINDING = 4;
   public static final int USAGE_RENDER_ATTACHMENT = 8;
   public static final int USAGE_CUBEMAP_COMPATIBLE = 16;
   private final GpuFormat format;
   private final int width;
   private final int height;
   private final int depthOrLayers;
   private final int mipLevels;
   private final @GpuTexture.Usage int usage;
   private final String label;

   public GpuTexture(
      final @GpuTexture.Usage int usage,
      final String label,
      final GpuFormat format,
      final int width,
      final int height,
      final int depthOrLayers,
      final int mipLevels
   ) {
      this.usage = usage;
      this.label = label;
      this.format = format;
      this.width = width;
      this.height = height;
      this.depthOrLayers = depthOrLayers;
      this.mipLevels = mipLevels;
   }

   public int getWidth(final int mipLevel) {
      return this.width >> mipLevel;
   }

   public int getHeight(final int mipLevel) {
      return this.height >> mipLevel;
   }

   public int getDepthOrLayers() {
      return this.depthOrLayers;
   }

   public int getMipLevels() {
      return this.mipLevels;
   }

   public GpuFormat getFormat() {
      return this.format;
   }

   public @GpuTexture.Usage int usage() {
      return this.usage;
   }

   public String getLabel() {
      return this.label;
   }

   @Override
   public abstract void close();

   public abstract boolean isClosed();

   @Retention(RetentionPolicy.CLASS)
   @Target(ElementType.TYPE_USE)
   public @interface Usage {
   }
}
