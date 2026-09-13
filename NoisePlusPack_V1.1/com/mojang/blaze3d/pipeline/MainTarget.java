package com.mojang.blaze3d.pipeline;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.GpuOutOfMemoryException;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import java.util.List;
import java.util.Objects;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class MainTarget extends RenderTarget {
   public static final int DEFAULT_WIDTH = 854;
   public static final int DEFAULT_HEIGHT = 480;
   static final MainTarget.Dimension DEFAULT_DIMENSIONS = new MainTarget.Dimension(854, 480);

   public MainTarget(int p_166137_, int p_166138_) {
      super("Main", true);
      this.createFrameBuffer(p_166137_, p_166138_);
   }

   private void createFrameBuffer(int p_166142_, int p_166143_) {
      MainTarget.Dimension maintarget$dimension = this.allocateAttachments(p_166142_, p_166143_);
      if (this.colorTexture != null && this.depthTexture != null) {
         this.width = maintarget$dimension.width;
         this.height = maintarget$dimension.height;
      } else {
         throw new IllegalStateException("Missing color and/or depth textures");
      }
   }

   private MainTarget.Dimension allocateAttachments(int p_166147_, int p_166148_) {
      RenderSystem.assertOnRenderThread();

      for (MainTarget.Dimension maintarget$dimension : MainTarget.Dimension.listWithFallback(p_166147_, p_166148_)) {
         if (this.colorTexture != null) {
            this.colorTexture.close();
            this.colorTexture = null;
         }

         if (this.colorTextureView != null) {
            this.colorTextureView.close();
            this.colorTextureView = null;
         }

         if (this.depthTexture != null) {
            this.depthTexture.close();
            this.depthTexture = null;
         }

         if (this.depthTextureView != null) {
            this.depthTextureView.close();
            this.depthTextureView = null;
         }

         this.colorTexture = this.allocateColorAttachment(maintarget$dimension);
         this.depthTexture = this.allocateDepthAttachment(maintarget$dimension);
         if (this.colorTexture != null && this.depthTexture != null) {
            this.colorTextureView = RenderSystem.getDevice().createTextureView(this.colorTexture);
            this.depthTextureView = RenderSystem.getDevice().createTextureView(this.depthTexture);
            return maintarget$dimension;
         }
      }

      throw new RuntimeException(
         "Unrecoverable GL_OUT_OF_MEMORY ("
            + (this.colorTexture == null ? "missing color" : "have color")
            + ", "
            + (this.depthTexture == null ? "missing depth" : "have depth")
            + ")"
      );
   }

   private @Nullable GpuTexture allocateColorAttachment(MainTarget.Dimension p_166140_) {
      try {
         return RenderSystem.getDevice().createTexture(() -> this.label + " / Color", 15, TextureFormat.RGBA8, p_166140_.width, p_166140_.height, 1, 1);
      } catch (GpuOutOfMemoryException gpuoutofmemoryexception) {
         return null;
      }
   }

   private @Nullable GpuTexture allocateDepthAttachment(MainTarget.Dimension p_166145_) {
      try {
         return RenderSystem.getDevice().createTexture(() -> this.label + " / Depth", 15, TextureFormat.DEPTH32, p_166145_.width, p_166145_.height, 1, 1);
      } catch (GpuOutOfMemoryException gpuoutofmemoryexception) {
         return null;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class Dimension {
      public final int width;
      public final int height;

      Dimension(int p_166171_, int p_166172_) {
         this.width = p_166171_;
         this.height = p_166172_;
      }

      static List<MainTarget.Dimension> listWithFallback(int p_166174_, int p_166175_) {
         RenderSystem.assertOnRenderThread();
         int i = RenderSystem.getDevice().getMaxTextureSize();
         return p_166174_ > 0 && p_166174_ <= i && p_166175_ > 0 && p_166175_ <= i
            ? ImmutableList.of(new MainTarget.Dimension(p_166174_, p_166175_), MainTarget.DEFAULT_DIMENSIONS)
            : ImmutableList.of(MainTarget.DEFAULT_DIMENSIONS);
      }

      @Override
      public boolean equals(Object p_166177_) {
         if (this == p_166177_) {
            return true;
         } else if (p_166177_ != null && this.getClass() == p_166177_.getClass()) {
            MainTarget.Dimension maintarget$dimension = (MainTarget.Dimension)p_166177_;
            return this.width == maintarget$dimension.width && this.height == maintarget$dimension.height;
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.width, this.height);
      }

      @Override
      public String toString() {
         return this.width + "x" + this.height;
      }
   }
}
