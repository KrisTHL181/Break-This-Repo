package com.mojang.blaze3d.pipeline;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class TextureTarget extends RenderTarget {
   public TextureTarget(@Nullable String p_397583_, int p_166213_, int p_166214_, boolean p_166215_) {
      super(p_397583_, p_166215_);
      RenderSystem.assertOnRenderThread();
      this.resize(p_166213_, p_166214_);
   }
}
