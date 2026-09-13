package com.mojang.blaze3d.textures;

import com.mojang.blaze3d.DontObfuscate;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public abstract class GpuTexture implements AutoCloseable {
   public static final int USAGE_COPY_DST = 1;
   public static final int USAGE_COPY_SRC = 2;
   public static final int USAGE_TEXTURE_BINDING = 4;
   public static final int USAGE_RENDER_ATTACHMENT = 8;
   public static final int USAGE_CUBEMAP_COMPATIBLE = 16;
   private final TextureFormat format;
   private final int width;
   private final int height;
   private final int depthOrLayers;
   private final int mipLevels;
   @GpuTexture.Usage
   private final int usage;
   private final String label;

   public GpuTexture(@GpuTexture.Usage int p_393042_, String p_395679_, TextureFormat p_392008_, int p_394574_, int p_397229_, int p_406893_, int p_405806_) {
      this.usage = p_393042_;
      this.label = p_395679_;
      this.format = p_392008_;
      this.width = p_394574_;
      this.height = p_397229_;
      this.depthOrLayers = p_406893_;
      this.mipLevels = p_405806_;
   }

   public int getWidth(int p_397572_) {
      return this.width >> p_397572_;
   }

   public int getHeight(int p_394674_) {
      return this.height >> p_394674_;
   }

   public int getDepthOrLayers() {
      return this.depthOrLayers;
   }

   public int getMipLevels() {
      return this.mipLevels;
   }

   public TextureFormat getFormat() {
      return this.format;
   }

   @GpuTexture.Usage
   public int usage() {
      return this.usage;
   }

   public String getLabel() {
      return this.label;
   }

   @Override
   public abstract void close();

   public abstract boolean isClosed();

   @Retention(RetentionPolicy.CLASS)
   @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.METHOD, ElementType.TYPE_USE})
   @OnlyIn(Dist.CLIENT)
   public @interface Usage {
   }
}
