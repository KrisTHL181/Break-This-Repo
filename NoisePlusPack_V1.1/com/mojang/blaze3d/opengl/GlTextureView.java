package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class GlTextureView extends GpuTextureView {
   private static final int EMPTY = -1;
   private boolean closed;
   private int firstFboId = -1;
   private int firstFboDepthId = -1;
   private @Nullable Int2IntMap fboCache;

   protected GlTextureView(GlTexture p_409590_, int p_406886_, int p_408540_) {
      super(p_409590_, p_406886_, p_408540_);
      p_409590_.addViews();
   }

   @Override
   public boolean isClosed() {
      return this.closed;
   }

   @Override
   public void close() {
      if (!this.closed) {
         this.closed = true;
         this.texture().removeViews();
         if (this.firstFboId != -1) {
            GlStateManager._glDeleteFramebuffers(this.firstFboId);
         }

         if (this.fboCache != null) {
            IntIterator var1 = this.fboCache.values().iterator();

            while (var1.hasNext()) {
               int i = (Integer)var1.next();
               GlStateManager._glDeleteFramebuffers(i);
            }
         }
      }
   }

   public int getFbo(DirectStateAccess p_457836_, @Nullable GpuTexture p_459514_) {
      int i = p_459514_ == null ? 0 : ((GlTexture)p_459514_).id;
      if (this.firstFboDepthId == i) {
         return this.firstFboId;
      }

      if (this.firstFboId == -1) {
         this.firstFboId = this.createFbo(p_457836_, i);
         this.firstFboDepthId = i;
         return this.firstFboId;
      }

      if (this.fboCache == null) {
         this.fboCache = new Int2IntArrayMap();
      }

      return this.fboCache.computeIfAbsent(i, p_455261_ -> this.createFbo(p_457836_, p_455261_));
   }

   private int createFbo(DirectStateAccess p_453035_, int p_453170_) {
      int i = p_453035_.createFrameBufferObject();
      p_453035_.bindFrameBufferTextures(i, this.texture().id, p_453170_, this.baseMipLevel(), 0);
      return i;
   }

   public GlTexture texture() {
      return (GlTexture)super.texture();
   }
}
