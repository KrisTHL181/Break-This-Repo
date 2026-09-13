package com.mojang.blaze3d.pipeline;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import org.jspecify.annotations.Nullable;

public class TextureTarget extends RenderTarget {
   public TextureTarget(
      final @Nullable String label, final int width, final int height, final @Nullable GpuFormat colorFormat, final @Nullable GpuFormat depthFormat
   ) {
      super(label, colorFormat, depthFormat);
      RenderSystem.assertOnRenderThread();
      this.resize(width, height);
   }
}
