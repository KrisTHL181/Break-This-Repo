package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuQuery;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.util.ARGB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL33C;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class GlCommandEncoder implements CommandEncoder {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final GlDevice device;
   private final int readFbo;
   private final int drawFbo;
   private @Nullable RenderPipeline lastPipeline;
   private boolean inRenderPass;
   private @Nullable GlProgram lastProgram;
   private @Nullable GlTimerQuery activeTimerQuery;

   protected GlCommandEncoder(GlDevice p_396674_) {
      this.device = p_396674_;
      this.readFbo = p_396674_.directStateAccess().createFrameBufferObject();
      this.drawFbo = p_396674_.directStateAccess().createFrameBufferObject();
   }

   @Override
   public RenderPass createRenderPass(Supplier<String> p_407771_, GpuTextureView p_406560_, OptionalInt p_395809_) {
      return this.createRenderPass(p_407771_, p_406560_, p_395809_, null, OptionalDouble.empty());
   }

   @Override
   public RenderPass createRenderPass(
      Supplier<String> p_406861_, GpuTextureView p_408235_, OptionalInt p_391728_, @Nullable GpuTextureView p_409576_, OptionalDouble p_395171_
   ) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before creating a new one!");
      }

      if (p_395171_.isPresent() && p_409576_ == null) {
         LOGGER.warn("Depth clear value was provided but no depth texture is being used");
      }

      if (p_408235_.isClosed()) {
         throw new IllegalStateException("Color texture is closed");
      }

      if ((p_408235_.texture().usage() & 8) == 0) {
         throw new IllegalStateException("Color texture must have USAGE_RENDER_ATTACHMENT");
      }

      if (p_408235_.texture().getDepthOrLayers() > 1) {
         throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported as an attachment");
      }

      if (p_409576_ != null) {
         if (p_409576_.isClosed()) {
            throw new IllegalStateException("Depth texture is closed");
         }

         if ((p_409576_.texture().usage() & 8) == 0) {
            throw new IllegalStateException("Depth texture must have USAGE_RENDER_ATTACHMENT");
         }

         if (p_409576_.texture().getDepthOrLayers() > 1) {
            throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported as an attachment");
         }
      }

      this.inRenderPass = true;
      this.device.debugLabels().pushDebugGroup(p_406861_);
      int i = ((GlTextureView)p_408235_).getFbo(this.device.directStateAccess(), p_409576_ == null ? null : p_409576_.texture());
      GlStateManager._glBindFramebuffer(36160, i);
      int j = 0;
      if (p_391728_.isPresent()) {
         int k = p_391728_.getAsInt();
         GL11.glClearColor(ARGB.redFloat(k), ARGB.greenFloat(k), ARGB.blueFloat(k), ARGB.alphaFloat(k));
         j |= 16384;
      }

      if (p_409576_ != null && p_395171_.isPresent()) {
         GL11.glClearDepth(p_395171_.getAsDouble());
         j |= 256;
      }

      if (j != 0) {
         GlStateManager._disableScissorTest();
         GlStateManager._depthMask(true);
         GlStateManager._colorMask(true, true, true, true);
         GlStateManager._clear(j);
      }

      GlStateManager._viewport(0, 0, p_408235_.getWidth(0), p_408235_.getHeight(0));
      this.lastPipeline = null;
      return new GlRenderPass(this, p_409576_ != null);
   }

   @Override
   public void clearColorTexture(GpuTexture p_394273_, int p_393834_) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before creating a new one!");
      }

      this.verifyColorTexture(p_394273_);
      this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, ((GlTexture)p_394273_).id, 0, 0, 36160);
      GL11.glClearColor(ARGB.redFloat(p_393834_), ARGB.greenFloat(p_393834_), ARGB.blueFloat(p_393834_), ARGB.alphaFloat(p_393834_));
      GlStateManager._disableScissorTest();
      GlStateManager._colorMask(true, true, true, true);
      GlStateManager._clear(16384);
      GlStateManager._glFramebufferTexture2D(36160, 36064, 3553, 0, 0);
      GlStateManager._glBindFramebuffer(36160, 0);
   }

   @Override
   public void clearColorAndDepthTextures(GpuTexture p_393527_, int p_391700_, GpuTexture p_391582_, double p_393930_) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before creating a new one!");
      }

      this.verifyColorTexture(p_393527_);
      this.verifyDepthTexture(p_391582_);
      int i = ((GlTexture)p_393527_).getFbo(this.device.directStateAccess(), p_391582_);
      GlStateManager._glBindFramebuffer(36160, i);
      GlStateManager._disableScissorTest();
      GL11.glClearDepth(p_393930_);
      GL11.glClearColor(ARGB.redFloat(p_391700_), ARGB.greenFloat(p_391700_), ARGB.blueFloat(p_391700_), ARGB.alphaFloat(p_391700_));
      GlStateManager._depthMask(true);
      GlStateManager._colorMask(true, true, true, true);
      GlStateManager._clear(16640);
      GlStateManager._glBindFramebuffer(36160, 0);
   }

   @Override
   public void clearColorAndDepthTextures(
      GpuTexture p_409650_, int p_409075_, GpuTexture p_409914_, double p_409885_, int p_408011_, int p_410279_, int p_410459_, int p_409150_
   ) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before creating a new one!");
      }

      this.verifyColorTexture(p_409650_);
      this.verifyDepthTexture(p_409914_);
      this.verifyRegion(p_409650_, p_408011_, p_410279_, p_410459_, p_409150_);
      int i = ((GlTexture)p_409650_).getFbo(this.device.directStateAccess(), p_409914_);
      GlStateManager._glBindFramebuffer(36160, i);
      GlStateManager._scissorBox(p_408011_, p_410279_, p_410459_, p_409150_);
      GlStateManager._enableScissorTest();
      GL11.glClearDepth(p_409885_);
      GL11.glClearColor(ARGB.redFloat(p_409075_), ARGB.greenFloat(p_409075_), ARGB.blueFloat(p_409075_), ARGB.alphaFloat(p_409075_));
      GlStateManager._depthMask(true);
      GlStateManager._colorMask(true, true, true, true);
      GlStateManager._clear(16640);
      GlStateManager._glBindFramebuffer(36160, 0);
   }

   private void verifyRegion(GpuTexture p_407248_, int p_405805_, int p_409035_, int p_406966_, int p_407297_) {
      if (p_405805_ < 0 || p_405805_ >= p_407248_.getWidth(0)) {
         throw new IllegalArgumentException("regionX should not be outside of the texture");
      }

      if (p_409035_ < 0 || p_409035_ >= p_407248_.getHeight(0)) {
         throw new IllegalArgumentException("regionY should not be outside of the texture");
      }

      if (p_406966_ <= 0) {
         throw new IllegalArgumentException("regionWidth should be greater than 0");
      }

      if (p_405805_ + p_406966_ > p_407248_.getWidth(0)) {
         throw new IllegalArgumentException("regionWidth + regionX should be less than the texture width");
      }

      if (p_407297_ <= 0) {
         throw new IllegalArgumentException("regionHeight should be greater than 0");
      }

      if (p_409035_ + p_407297_ > p_407248_.getHeight(0)) {
         throw new IllegalArgumentException("regionWidth + regionX should be less than the texture height");
      }
   }

   @Override
   public void clearDepthTexture(GpuTexture p_397307_, double p_397388_) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before creating a new one!");
      }

      this.verifyDepthTexture(p_397307_);
      this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, 0, ((GlTexture)p_397307_).id, 0, 36160);
      GL11.glDrawBuffer(0);
      GL11.glClearDepth(p_397388_);
      GlStateManager._depthMask(true);
      GlStateManager._disableScissorTest();
      GlStateManager._clear(256);
      GL11.glDrawBuffer(36064);
      GlStateManager._glFramebufferTexture2D(36160, 36096, 3553, 0, 0);
      GlStateManager._glBindFramebuffer(36160, 0);
   }

   private void verifyColorTexture(GpuTexture p_409818_) {
      if (!p_409818_.getFormat().hasColorAspect()) {
         throw new IllegalStateException("Trying to clear a non-color texture as color");
      }

      if (p_409818_.isClosed()) {
         throw new IllegalStateException("Color texture is closed");
      }

      if ((p_409818_.usage() & 8) == 0) {
         throw new IllegalStateException("Color texture must have USAGE_RENDER_ATTACHMENT");
      }

      if (p_409818_.getDepthOrLayers() > 1) {
         throw new UnsupportedOperationException("Clearing a texture with multiple layers or depths is not yet supported");
      }
   }

   private void verifyDepthTexture(GpuTexture p_407968_) {
      if (!p_407968_.getFormat().hasDepthAspect()) {
         throw new IllegalStateException("Trying to clear a non-depth texture as depth");
      }

      if (p_407968_.isClosed()) {
         throw new IllegalStateException("Depth texture is closed");
      }

      if ((p_407968_.usage() & 8) == 0) {
         throw new IllegalStateException("Depth texture must have USAGE_RENDER_ATTACHMENT");
      }

      if (p_407968_.getDepthOrLayers() > 1) {
         throw new UnsupportedOperationException("Clearing a texture with multiple layers or depths is not yet supported");
      }
   }

   @Override
   public void writeToBuffer(GpuBufferSlice p_405833_, ByteBuffer p_397249_) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      }

      GlBuffer glbuffer = (GlBuffer)p_405833_.buffer();
      if (glbuffer.closed) {
         throw new IllegalStateException("Buffer already closed");
      }

      if ((glbuffer.usage() & 8) == 0) {
         throw new IllegalStateException("Buffer needs USAGE_COPY_DST to be a destination for a copy");
      }

      int i = p_397249_.remaining();
      if (i > p_405833_.length()) {
         throw new IllegalArgumentException(
            "Cannot write more data than the slice allows (attempting to write " + i + " bytes into a slice of length " + p_405833_.length() + ")"
         );
      }

      if (p_405833_.length() + p_405833_.offset() > glbuffer.size()) {
         throw new IllegalArgumentException(
            "Cannot write more data than this buffer can hold (attempting to write "
               + i
               + " bytes at offset "
               + p_405833_.offset()
               + " to "
               + glbuffer.size()
               + " size buffer)"
         );
      }

      this.device.directStateAccess().bufferSubData(glbuffer.handle, p_405833_.offset(), p_397249_, glbuffer.usage());
   }

   @Override
   public GpuBuffer.MappedView mapBuffer(GpuBuffer p_410372_, boolean p_409631_, boolean p_409789_) {
      return this.mapBuffer(p_410372_.slice(), p_409631_, p_409789_);
   }

   @Override
   public GpuBuffer.MappedView mapBuffer(GpuBufferSlice p_410020_, boolean p_409612_, boolean p_409669_) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      }

      GlBuffer glbuffer = (GlBuffer)p_410020_.buffer();
      if (glbuffer.closed) {
         throw new IllegalStateException("Buffer already closed");
      }

      if (!p_409612_ && !p_409669_) {
         throw new IllegalArgumentException("At least read or write must be true");
      }

      if (p_409612_ && (glbuffer.usage() & 1) == 0) {
         throw new IllegalStateException("Buffer is not readable");
      }

      if (p_409669_ && (glbuffer.usage() & 2) == 0) {
         throw new IllegalStateException("Buffer is not writable");
      }

      if (p_410020_.offset() + p_410020_.length() > glbuffer.size()) {
         throw new IllegalArgumentException(
            "Cannot map more data than this buffer can hold (attempting to map "
               + p_410020_.length()
               + " bytes at offset "
               + p_410020_.offset()
               + " from "
               + glbuffer.size()
               + " size buffer)"
         );
      }

      int i = 0;
      if (p_409612_) {
         i |= 1;
      }

      if (p_409669_) {
         i |= 34;
      }

      return this.device.getBufferStorage().mapBuffer(this.device.directStateAccess(), glbuffer, p_410020_.offset(), p_410020_.length(), i);
   }

   @Override
   public void copyToBuffer(GpuBufferSlice p_410810_, GpuBufferSlice p_410813_) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      }

      GlBuffer glbuffer = (GlBuffer)p_410810_.buffer();
      if (glbuffer.closed) {
         throw new IllegalStateException("Source buffer already closed");
      }

      if ((glbuffer.usage() & 16) == 0) {
         throw new IllegalStateException("Source buffer needs USAGE_COPY_SRC to be a source for a copy");
      }

      GlBuffer glbuffer1 = (GlBuffer)p_410813_.buffer();
      if (glbuffer1.closed) {
         throw new IllegalStateException("Target buffer already closed");
      }

      if ((glbuffer1.usage() & 8) == 0) {
         throw new IllegalStateException("Target buffer needs USAGE_COPY_DST to be a destination for a copy");
      }

      if (p_410810_.length() != p_410813_.length()) {
         throw new IllegalArgumentException(
            "Cannot copy from slice of size " + p_410810_.length() + " to slice of size " + p_410813_.length() + ", they must be equal"
         );
      }

      if (p_410810_.offset() + p_410810_.length() > glbuffer.size()) {
         throw new IllegalArgumentException(
            "Cannot copy more data than the source buffer holds (attempting to copy "
               + p_410810_.length()
               + " bytes at offset "
               + p_410810_.offset()
               + " from "
               + glbuffer.size()
               + " size buffer)"
         );
      }

      if (p_410813_.offset() + p_410813_.length() > glbuffer1.size()) {
         throw new IllegalArgumentException(
            "Cannot copy more data than the target buffer can hold (attempting to copy "
               + p_410813_.length()
               + " bytes at offset "
               + p_410813_.offset()
               + " to "
               + glbuffer1.size()
               + " size buffer)"
         );
      }

      this.device.directStateAccess().copyBufferSubData(glbuffer.handle, glbuffer1.handle, p_410810_.offset(), p_410813_.offset(), p_410810_.length());
   }

   @Override
   public void writeToTexture(GpuTexture p_394020_, NativeImage p_396595_) {
      int i = p_394020_.getWidth(0);
      int j = p_394020_.getHeight(0);
      if (p_396595_.getWidth() != i || p_396595_.getHeight() != j) {
         throw new IllegalArgumentException(
            "Cannot replace texture of size " + i + "x" + j + " with image of size " + p_396595_.getWidth() + "x" + p_396595_.getHeight()
         );
      }

      if (p_394020_.isClosed()) {
         throw new IllegalStateException("Destination texture is closed");
      }

      if ((p_394020_.usage() & 1) == 0) {
         throw new IllegalStateException("Color texture must have USAGE_COPY_DST to be a destination for a write");
      }

      this.writeToTexture(p_394020_, p_396595_, 0, 0, 0, 0, i, j, 0, 0);
   }

   @Override
   public void writeToTexture(
      GpuTexture p_395107_,
      NativeImage p_392321_,
      int p_394222_,
      int p_396221_,
      int p_392746_,
      int p_391501_,
      int p_397458_,
      int p_397527_,
      int p_392683_,
      int p_409501_
   ) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      }

      if (p_394222_ >= 0 && p_394222_ < p_395107_.getMipLevels()) {
         if (p_392683_ + p_397458_ > p_392321_.getWidth() || p_409501_ + p_397527_ > p_392321_.getHeight()) {
            throw new IllegalArgumentException(
               "Copy source ("
                  + p_392321_.getWidth()
                  + "x"
                  + p_392321_.getHeight()
                  + ") is not large enough to read a rectangle of "
                  + p_397458_
                  + "x"
                  + p_397527_
                  + " from "
                  + p_392683_
                  + "x"
                  + p_409501_
            );
         }

         if (p_392746_ + p_397458_ > p_395107_.getWidth(p_394222_) || p_391501_ + p_397527_ > p_395107_.getHeight(p_394222_)) {
            throw new IllegalArgumentException(
               "Dest texture ("
                  + p_397458_
                  + "x"
                  + p_397527_
                  + ") is not large enough to write a rectangle of "
                  + p_397458_
                  + "x"
                  + p_397527_
                  + " at "
                  + p_392746_
                  + "x"
                  + p_391501_
                  + " (at mip level "
                  + p_394222_
                  + ")"
            );
         }

         if (p_395107_.isClosed()) {
            throw new IllegalStateException("Destination texture is closed");
         }

         if ((p_395107_.usage() & 1) == 0) {
            throw new IllegalStateException("Color texture must have USAGE_COPY_DST to be a destination for a write");
         }

         if (p_396221_ >= p_395107_.getDepthOrLayers()) {
            throw new UnsupportedOperationException("Depth or layer is out of range, must be >= 0 and < " + p_395107_.getDepthOrLayers());
         }

         int i;
         if ((p_395107_.usage() & 16) != 0) {
            i = GlConst.CUBEMAP_TARGETS[p_396221_ % 6];
            GL11.glBindTexture(34067, ((GlTexture)p_395107_).id);
         } else {
            i = 3553;
            GlStateManager._bindTexture(((GlTexture)p_395107_).id);
         }

         GlStateManager._pixelStore(3314, p_392321_.getWidth());
         GlStateManager._pixelStore(3316, p_392683_);
         GlStateManager._pixelStore(3315, p_409501_);
         GlStateManager._pixelStore(3317, p_392321_.format().components());
         GlStateManager._texSubImage2D(i, p_394222_, p_392746_, p_391501_, p_397458_, p_397527_, GlConst.toGl(p_392321_.format()), 5121, p_392321_.getPointer());
      } else {
         throw new IllegalArgumentException("Invalid mipLevel " + p_394222_ + ", must be >= 0 and < " + p_395107_.getMipLevels());
      }
   }

   @Override
   public void writeToTexture(
      GpuTexture p_396389_,
      ByteBuffer p_428720_,
      NativeImage.Format p_392785_,
      int p_394994_,
      int p_395915_,
      int p_394993_,
      int p_393355_,
      int p_396347_,
      int p_408032_
   ) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      }

      if (p_394994_ >= 0 && p_394994_ < p_396389_.getMipLevels()) {
         if (p_396347_ * p_408032_ * p_392785_.components() > p_428720_.remaining()) {
            throw new IllegalArgumentException(
               "Copy would overrun the source buffer (remaining length of "
                  + p_428720_.remaining()
                  + ", but copy is "
                  + p_396347_
                  + "x"
                  + p_408032_
                  + " of format "
                  + p_392785_
                  + ")"
            );
         }

         if (p_394993_ + p_396347_ > p_396389_.getWidth(p_394994_) || p_393355_ + p_408032_ > p_396389_.getHeight(p_394994_)) {
            throw new IllegalArgumentException(
               "Dest texture ("
                  + p_396389_.getWidth(p_394994_)
                  + "x"
                  + p_396389_.getHeight(p_394994_)
                  + ") is not large enough to write a rectangle of "
                  + p_396347_
                  + "x"
                  + p_408032_
                  + " at "
                  + p_394993_
                  + "x"
                  + p_393355_
            );
         }

         if (p_396389_.isClosed()) {
            throw new IllegalStateException("Destination texture is closed");
         }

         if ((p_396389_.usage() & 1) == 0) {
            throw new IllegalStateException("Color texture must have USAGE_COPY_DST to be a destination for a write");
         }

         if (p_395915_ >= p_396389_.getDepthOrLayers()) {
            throw new UnsupportedOperationException("Depth or layer is out of range, must be >= 0 and < " + p_396389_.getDepthOrLayers());
         }

         int i;
         if ((p_396389_.usage() & 16) != 0) {
            i = GlConst.CUBEMAP_TARGETS[p_395915_ % 6];
            GL11.glBindTexture(34067, ((GlTexture)p_396389_).id);
         } else {
            i = 3553;
            GlStateManager._bindTexture(((GlTexture)p_396389_).id);
         }

         GlStateManager._pixelStore(3314, p_396347_);
         GlStateManager._pixelStore(3316, 0);
         GlStateManager._pixelStore(3315, 0);
         GlStateManager._pixelStore(3317, p_392785_.components());
         GlStateManager._texSubImage2D(i, p_394994_, p_394993_, p_393355_, p_396347_, p_408032_, GlConst.toGl(p_392785_), 5121, p_428720_);
      } else {
         throw new IllegalArgumentException("Invalid mipLevel, must be >= 0 and < " + p_396389_.getMipLevels());
      }
   }

   @Override
   public void copyTextureToBuffer(GpuTexture p_397941_, GpuBuffer p_395918_, long p_458777_, Runnable p_397559_, int p_391975_) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      }

      this.copyTextureToBuffer(p_397941_, p_395918_, p_458777_, p_397559_, p_391975_, 0, 0, p_397941_.getWidth(p_391975_), p_397941_.getHeight(p_391975_));
   }

   @Override
   public void copyTextureToBuffer(
      GpuTexture p_391871_, GpuBuffer p_395502_, long p_454956_, Runnable p_397589_, int p_395739_, int p_391264_, int p_393748_, int p_396780_, int p_391271_
   ) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      }

      if (p_395739_ >= 0 && p_395739_ < p_391871_.getMipLevels()) {
         if (p_391871_.getWidth(p_395739_) * p_391871_.getHeight(p_395739_) * p_391871_.getFormat().pixelSize() + p_454956_ > p_395502_.size()) {
            throw new IllegalArgumentException(
               "Buffer of size "
                  + p_395502_.size()
                  + " is not large enough to hold "
                  + p_396780_
                  + "x"
                  + p_391271_
                  + " pixels ("
                  + p_391871_.getFormat().pixelSize()
                  + " bytes each) starting from offset "
                  + p_454956_
            );
         }

         if ((p_391871_.usage() & 2) == 0) {
            throw new IllegalArgumentException("Texture needs USAGE_COPY_SRC to be a source for a copy");
         }

         if ((p_395502_.usage() & 8) == 0) {
            throw new IllegalArgumentException("Buffer needs USAGE_COPY_DST to be a destination for a copy");
         }

         if (p_391264_ + p_396780_ > p_391871_.getWidth(p_395739_) || p_393748_ + p_391271_ > p_391871_.getHeight(p_395739_)) {
            throw new IllegalArgumentException(
               "Copy source texture ("
                  + p_391871_.getWidth(p_395739_)
                  + "x"
                  + p_391871_.getHeight(p_395739_)
                  + ") is not large enough to read a rectangle of "
                  + p_396780_
                  + "x"
                  + p_391271_
                  + " from "
                  + p_391264_
                  + ","
                  + p_393748_
            );
         }

         if (p_391871_.isClosed()) {
            throw new IllegalStateException("Source texture is closed");
         }

         if (p_395502_.isClosed()) {
            throw new IllegalStateException("Destination buffer is closed");
         }

         if (p_391871_.getDepthOrLayers() > 1) {
            throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported for copying");
         }

         GlStateManager.clearGlErrors();
         this.device.directStateAccess().bindFrameBufferTextures(this.readFbo, ((GlTexture)p_391871_).glId(), 0, p_395739_, 36008);
         GlStateManager._glBindBuffer(35051, ((GlBuffer)p_395502_).handle);
         GlStateManager._pixelStore(3330, p_396780_);
         GlStateManager._readPixels(
            p_391264_, p_393748_, p_396780_, p_391271_, GlConst.toGlExternalId(p_391871_.getFormat()), GlConst.toGlType(p_391871_.getFormat()), p_454956_
         );
         RenderSystem.queueFencedTask(p_397589_);
         GlStateManager._glFramebufferTexture2D(36008, 36064, 3553, 0, p_395739_);
         GlStateManager._glBindFramebuffer(36008, 0);
         GlStateManager._glBindBuffer(35051, 0);
         int i = GlStateManager._getError();
         if (i != 0) {
            throw new IllegalStateException("Couldn't perform copyTobuffer for texture " + p_391871_.getLabel() + ": GL error " + i);
         }
      } else {
         throw new IllegalArgumentException("Invalid mipLevel " + p_395739_ + ", must be >= 0 and < " + p_391871_.getMipLevels());
      }
   }

   @Override
   public void copyTextureToTexture(
      GpuTexture p_394155_, GpuTexture p_394461_, int p_396176_, int p_393684_, int p_394159_, int p_394139_, int p_396698_, int p_394668_, int p_397937_
   ) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      }

      if (p_396176_ >= 0 && p_396176_ < p_394155_.getMipLevels() && p_396176_ < p_394461_.getMipLevels()) {
         if (p_393684_ + p_394668_ > p_394461_.getWidth(p_396176_) || p_394159_ + p_397937_ > p_394461_.getHeight(p_396176_)) {
            throw new IllegalArgumentException(
               "Dest texture ("
                  + p_394461_.getWidth(p_396176_)
                  + "x"
                  + p_394461_.getHeight(p_396176_)
                  + ") is not large enough to write a rectangle of "
                  + p_394668_
                  + "x"
                  + p_397937_
                  + " at "
                  + p_393684_
                  + "x"
                  + p_394159_
            );
         }

         if (p_394139_ + p_394668_ > p_394155_.getWidth(p_396176_) || p_396698_ + p_397937_ > p_394155_.getHeight(p_396176_)) {
            throw new IllegalArgumentException(
               "Source texture ("
                  + p_394155_.getWidth(p_396176_)
                  + "x"
                  + p_394155_.getHeight(p_396176_)
                  + ") is not large enough to read a rectangle of "
                  + p_394668_
                  + "x"
                  + p_397937_
                  + " at "
                  + p_394139_
                  + "x"
                  + p_396698_
            );
         }

         if (p_394155_.isClosed()) {
            throw new IllegalStateException("Source texture is closed");
         }

         if (p_394461_.isClosed()) {
            throw new IllegalStateException("Destination texture is closed");
         }

         if ((p_394155_.usage() & 2) == 0) {
            throw new IllegalArgumentException("Texture needs USAGE_COPY_SRC to be a source for a copy");
         }

         if ((p_394461_.usage() & 1) == 0) {
            throw new IllegalArgumentException("Texture needs USAGE_COPY_DST to be a destination for a copy");
         }

         if (p_394155_.getDepthOrLayers() > 1) {
            throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported for copying");
         }

         if (p_394461_.getDepthOrLayers() > 1) {
            throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported for copying");
         }

         GlStateManager.clearGlErrors();
         GlStateManager._disableScissorTest();
         boolean flag = p_394155_.getFormat().hasDepthAspect();
         int i = ((GlTexture)p_394155_).glId();
         int j = ((GlTexture)p_394461_).glId();
         this.device.directStateAccess().bindFrameBufferTextures(this.readFbo, flag ? 0 : i, flag ? i : 0, 0, 0);
         this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, flag ? 0 : j, flag ? j : 0, 0, 0);
         this.device
            .directStateAccess()
            .blitFrameBuffers(
               this.readFbo, this.drawFbo, p_394139_, p_396698_, p_394668_, p_397937_, p_393684_, p_394159_, p_394668_, p_397937_, flag ? 256 : 16384, 9728
            );
         int k = GlStateManager._getError();
         if (k != 0) {
            throw new IllegalStateException(
               "Couldn't perform copyToTexture for texture " + p_394155_.getLabel() + " to " + p_394461_.getLabel() + ": GL error " + k
            );
         }
      } else {
         throw new IllegalArgumentException(
            "Invalid mipLevel " + p_396176_ + ", must be >= 0 and < " + p_394155_.getMipLevels() + " and < " + p_394461_.getMipLevels()
         );
      }
   }

   @Override
   public void presentTexture(GpuTextureView p_406941_) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      }

      if (!p_406941_.texture().getFormat().hasColorAspect()) {
         throw new IllegalStateException("Cannot present a non-color texture!");
      }

      if ((p_406941_.texture().usage() & 8) == 0) {
         throw new IllegalStateException("Color texture must have USAGE_RENDER_ATTACHMENT to presented to the screen");
      }

      if (p_406941_.texture().getDepthOrLayers() > 1) {
         throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported for presentation");
      }

      GlStateManager._disableScissorTest();
      GlStateManager._viewport(0, 0, p_406941_.getWidth(0), p_406941_.getHeight(0));
      GlStateManager._depthMask(true);
      GlStateManager._colorMask(true, true, true, true);
      this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, ((GlTexture)p_406941_.texture()).glId(), 0, 0, 0);
      this.device
         .directStateAccess()
         .blitFrameBuffers(
            this.drawFbo, 0, 0, 0, p_406941_.getWidth(0), p_406941_.getHeight(0), 0, 0, p_406941_.getWidth(0), p_406941_.getHeight(0), 16384, 9728
         );
   }

   @Override
   public GpuFence createFence() {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      } else {
         return new GlFence();
      }
   }

   protected <T> void executeDrawMultiple(
      GlRenderPass p_396459_,
      Collection<RenderPass.Draw<T>> p_398042_,
      @Nullable GpuBuffer p_391308_,
      VertexFormat.@Nullable IndexType p_395864_,
      Collection<String> p_407124_,
      T p_409520_
   ) {
      if (this.trySetup(p_396459_, p_407124_)) {
         if (p_395864_ == null) {
            p_395864_ = VertexFormat.IndexType.SHORT;
         }

         for (RenderPass.Draw<T> draw : p_398042_) {
            VertexFormat.IndexType vertexformat$indextype = draw.indexType() == null ? p_395864_ : draw.indexType();
            p_396459_.setIndexBuffer(draw.indexBuffer() == null ? p_391308_ : draw.indexBuffer(), vertexformat$indextype);
            p_396459_.setVertexBuffer(draw.slot(), draw.vertexBuffer());
            if (GlRenderPass.VALIDATION) {
               if (p_396459_.indexBuffer == null) {
                  throw new IllegalStateException("Missing index buffer");
               }

               if (p_396459_.indexBuffer.isClosed()) {
                  throw new IllegalStateException("Index buffer has been closed!");
               }

               if (p_396459_.vertexBuffers[0] == null) {
                  throw new IllegalStateException("Missing vertex buffer at slot 0");
               }

               if (p_396459_.vertexBuffers[0].isClosed()) {
                  throw new IllegalStateException("Vertex buffer at slot 0 has been closed!");
               }
            }

            BiConsumer<T, RenderPass.UniformUploader> biconsumer = draw.uniformUploaderConsumer();
            if (biconsumer != null) {
               biconsumer.accept(p_409520_, (p_404740_, p_404741_) -> {
                  if (p_396459_.pipeline.program().getUniform(p_404740_) instanceof Uniform.Ubo(int i)) {
                     GL32.glBindBufferRange(35345, i, ((GlBuffer)p_404741_.buffer()).handle, p_404741_.offset(), p_404741_.length());
                  }
               });
            }

            this.drawFromBuffers(p_396459_, 0, draw.firstIndex(), draw.indexCount(), vertexformat$indextype, p_396459_.pipeline, 1);
         }
      }
   }

   protected void executeDraw(GlRenderPass p_391991_, int p_395477_, int p_392599_, int p_409900_, VertexFormat.@Nullable IndexType p_391193_, int p_408977_) {
      if (this.trySetup(p_391991_, Collections.emptyList())) {
         if (GlRenderPass.VALIDATION) {
            if (p_391193_ != null) {
               if (p_391991_.indexBuffer == null) {
                  throw new IllegalStateException("Missing index buffer");
               }

               if (p_391991_.indexBuffer.isClosed()) {
                  throw new IllegalStateException("Index buffer has been closed!");
               }

               if ((p_391991_.indexBuffer.usage() & 64) == 0) {
                  throw new IllegalStateException("Index buffer must have GpuBuffer.USAGE_INDEX!");
               }
            }

            GlRenderPipeline glrenderpipeline = p_391991_.pipeline;
            if (p_391991_.vertexBuffers[0] == null && glrenderpipeline != null && !glrenderpipeline.info().getVertexFormat().getElements().isEmpty()) {
               throw new IllegalStateException("Vertex format contains elements but vertex buffer at slot 0 is null");
            }

            if (p_391991_.vertexBuffers[0] != null && p_391991_.vertexBuffers[0].isClosed()) {
               throw new IllegalStateException("Vertex buffer at slot 0 has been closed!");
            }

            if (p_391991_.vertexBuffers[0] != null && (p_391991_.vertexBuffers[0].usage() & 32) == 0) {
               throw new IllegalStateException("Vertex buffer must have GpuBuffer.USAGE_VERTEX!");
            }
         }

         this.drawFromBuffers(p_391991_, p_395477_, p_392599_, p_409900_, p_391193_, p_391991_.pipeline, p_408977_);
      }
   }

   private void drawFromBuffers(
      GlRenderPass p_409055_,
      int p_407852_,
      int p_409156_,
      int p_406701_,
      VertexFormat.@Nullable IndexType p_407030_,
      GlRenderPipeline p_410289_,
      int p_409245_
   ) {
      this.device.vertexArrayCache().bindVertexArray(p_410289_.info().getVertexFormat(), (GlBuffer)p_409055_.vertexBuffers[0]);
      if (p_407030_ != null) {
         GlStateManager._glBindBuffer(34963, ((GlBuffer)p_409055_.indexBuffer).handle);
         if (p_409245_ > 1) {
            if (p_407852_ > 0) {
               GL32.glDrawElementsInstancedBaseVertex(
                  GlConst.toGl(p_410289_.info().getVertexFormatMode()),
                  p_406701_,
                  GlConst.toGl(p_407030_),
                  (long)p_409156_ * p_407030_.bytes,
                  p_409245_,
                  p_407852_
               );
            } else {
               GL31.glDrawElementsInstanced(
                  GlConst.toGl(p_410289_.info().getVertexFormatMode()), p_406701_, GlConst.toGl(p_407030_), (long)p_409156_ * p_407030_.bytes, p_409245_
               );
            }
         } else if (p_407852_ > 0) {
            GL32.glDrawElementsBaseVertex(
               GlConst.toGl(p_410289_.info().getVertexFormatMode()), p_406701_, GlConst.toGl(p_407030_), (long)p_409156_ * p_407030_.bytes, p_407852_
            );
         } else {
            GlStateManager._drawElements(
               GlConst.toGl(p_410289_.info().getVertexFormatMode()), p_406701_, GlConst.toGl(p_407030_), (long)p_409156_ * p_407030_.bytes
            );
         }
      } else if (p_409245_ > 1) {
         GL31.glDrawArraysInstanced(GlConst.toGl(p_410289_.info().getVertexFormatMode()), p_407852_, p_406701_, p_409245_);
      } else {
         GlStateManager._drawArrays(GlConst.toGl(p_410289_.info().getVertexFormatMode()), p_407852_, p_406701_);
      }
   }

   private boolean trySetup(GlRenderPass p_396081_, Collection<String> p_408722_) {
      if (GlRenderPass.VALIDATION) {
         if (p_396081_.pipeline == null) {
            throw new IllegalStateException("Can't draw without a render pipeline");
         }

         if (p_396081_.pipeline.program() == GlProgram.INVALID_PROGRAM) {
            throw new IllegalStateException("Pipeline contains invalid shader program");
         }

         for (RenderPipeline.UniformDescription renderpipeline$uniformdescription : p_396081_.pipeline.info().getUniforms()) {
            GpuBufferSlice gpubufferslice = p_396081_.uniforms.get(renderpipeline$uniformdescription.name());
            if (!p_408722_.contains(renderpipeline$uniformdescription.name())) {
               if (gpubufferslice == null) {
                  throw new IllegalStateException(
                     "Missing uniform " + renderpipeline$uniformdescription.name() + " (should be " + renderpipeline$uniformdescription.type() + ")"
                  );
               }

               if (renderpipeline$uniformdescription.type() == UniformType.UNIFORM_BUFFER) {
                  if (gpubufferslice.buffer().isClosed()) {
                     throw new IllegalStateException("Uniform buffer " + renderpipeline$uniformdescription.name() + " is already closed");
                  }

                  if ((gpubufferslice.buffer().usage() & 128) == 0) {
                     throw new IllegalStateException("Uniform buffer " + renderpipeline$uniformdescription.name() + " must have GpuBuffer.USAGE_UNIFORM");
                  }
               }

               if (renderpipeline$uniformdescription.type() == UniformType.TEXEL_BUFFER) {
                  if (gpubufferslice.offset() != 0L || gpubufferslice.length() != gpubufferslice.buffer().size()) {
                     throw new IllegalStateException("Uniform texel buffers do not support a slice of a buffer, must be entire buffer");
                  }

                  if (renderpipeline$uniformdescription.textureFormat() == null) {
                     throw new IllegalStateException("Invalid uniform texel buffer " + renderpipeline$uniformdescription.name() + " (missing a texture format)");
                  }
               }
            }
         }

         for (Entry<String, Uniform> entry : p_396081_.pipeline.program().getUniforms().entrySet()) {
            if (entry.getValue() instanceof Uniform.Sampler) {
               String s1 = entry.getKey();
               GlRenderPass.TextureViewAndSampler glrenderpass$textureviewandsampler = p_396081_.samplers.get(s1);
               if (glrenderpass$textureviewandsampler == null) {
                  throw new IllegalStateException("Missing sampler " + s1);
               }

               GlTextureView gltextureview = glrenderpass$textureviewandsampler.view();
               if (gltextureview.isClosed()) {
                  throw new IllegalStateException("Texture view " + s1 + " (" + gltextureview.texture().getLabel() + ") has been closed!");
               }

               if ((gltextureview.texture().usage() & 4) == 0) {
                  throw new IllegalStateException("Texture view " + s1 + " (" + gltextureview.texture().getLabel() + ") must have USAGE_TEXTURE_BINDING!");
               }

               if (glrenderpass$textureviewandsampler.sampler().isClosed()) {
                  throw new IllegalStateException("Sampler for " + s1 + " (" + gltextureview.texture().getLabel() + ") has been closed!");
               }
            }
         }

         if (p_396081_.pipeline.info().wantsDepthTexture() && !p_396081_.hasDepthTexture()) {
            LOGGER.warn("Render pipeline {} wants a depth texture but none was provided - this is probably a bug", p_396081_.pipeline.info().getLocation());
         }
      } else if (p_396081_.pipeline == null || p_396081_.pipeline.program() == GlProgram.INVALID_PROGRAM) {
         return false;
      }

      RenderPipeline renderpipeline = p_396081_.pipeline.info();
      GlProgram glprogram = p_396081_.pipeline.program();
      this.applyPipelineState(renderpipeline);
      boolean flag1 = this.lastProgram != glprogram;
      if (flag1) {
         GlStateManager._glUseProgram(glprogram.getProgramId());
         this.lastProgram = glprogram;
      }

      for (Entry<String, Uniform> entry1 : glprogram.getUniforms().entrySet()) {
         String s = entry1.getKey();
         boolean flag = p_396081_.dirtyUniforms.contains(s);
         switch ((Uniform)entry1.getValue()) {
            case Uniform.Ubo(int j2):
               int k = j2;
               if (flag) {
                  GpuBufferSlice gpubufferslice1 = p_396081_.uniforms.get(s);
                  GL32.glBindBufferRange(35345, k, ((GlBuffer)gpubufferslice1.buffer()).handle, gpubufferslice1.offset(), gpubufferslice1.length());
               }
               break;
            case Uniform.Utb(int l, int i1, TextureFormat textureformat, int i2):
               int j1 = i2;
               if (flag1 || flag) {
                  GlStateManager._glUniform1i(l, i1);
               }

               GlStateManager._activeTexture(33984 + i1);
               GL11C.glBindTexture(35882, j1);
               if (flag) {
                  GpuBufferSlice gpubufferslice2 = p_396081_.uniforms.get(s);
                  GL31.glTexBuffer(35882, GlConst.toGlInternalId(textureformat), ((GlBuffer)gpubufferslice2.buffer()).handle);
               }
               break;
            case Uniform.Sampler(int $$23, int l1):
               int k1 = l1;
               GlRenderPass.TextureViewAndSampler glrenderpass$textureviewandsampler1 = p_396081_.samplers.get(s);
               if (glrenderpass$textureviewandsampler1 == null) {
                  break;
               }

               GlTextureView gltextureview1 = glrenderpass$textureviewandsampler1.view();
               if (flag1 || flag) {
                  GlStateManager._glUniform1i($$23, k1);
               }

               GlStateManager._activeTexture(33984 + k1);
               GlTexture gltexture = gltextureview1.texture();
               int j;
               if ((gltexture.usage() & 16) != 0) {
                  j = 34067;
                  GL11.glBindTexture(34067, gltexture.id);
               } else {
                  j = 3553;
                  GlStateManager._bindTexture(gltexture.id);
               }

               GL33C.glBindSampler(k1, glrenderpass$textureviewandsampler1.sampler().getId());
               GlStateManager._texParameter(j, 33084, gltextureview1.baseMipLevel());
               GlStateManager._texParameter(j, 33085, gltextureview1.baseMipLevel() + gltextureview1.mipLevels() - 1);
               break;
            default:
               throw new MatchException(null, null);
         }
      }

      p_396081_.dirtyUniforms.clear();
      if (p_396081_.isScissorEnabled()) {
         GlStateManager._enableScissorTest();
         GlStateManager._scissorBox(p_396081_.getScissorX(), p_396081_.getScissorY(), p_396081_.getScissorWidth(), p_396081_.getScissorHeight());
      } else {
         GlStateManager._disableScissorTest();
      }

      return true;
   }

   private void applyPipelineState(RenderPipeline p_394271_) {
      if (this.lastPipeline != p_394271_) {
         this.lastPipeline = p_394271_;
         if (p_394271_.getDepthTestFunction() != DepthTestFunction.NO_DEPTH_TEST) {
            GlStateManager._enableDepthTest();
            GlStateManager._depthFunc(GlConst.toGl(p_394271_.getDepthTestFunction()));
         } else {
            GlStateManager._disableDepthTest();
         }

         if (p_394271_.isCull()) {
            GlStateManager._enableCull();
         } else {
            GlStateManager._disableCull();
         }

         if (p_394271_.getBlendFunction().isPresent()) {
            GlStateManager._enableBlend();
            BlendFunction blendfunction = p_394271_.getBlendFunction().get();
            GlStateManager._blendFuncSeparate(
               GlConst.toGl(blendfunction.sourceColor()),
               GlConst.toGl(blendfunction.destColor()),
               GlConst.toGl(blendfunction.sourceAlpha()),
               GlConst.toGl(blendfunction.destAlpha())
            );
         } else {
            GlStateManager._disableBlend();
         }

         GlStateManager._polygonMode(1032, GlConst.toGl(p_394271_.getPolygonMode()));
         GlStateManager._depthMask(p_394271_.isWriteDepth());
         GlStateManager._colorMask(p_394271_.isWriteColor(), p_394271_.isWriteColor(), p_394271_.isWriteColor(), p_394271_.isWriteAlpha());
         if (p_394271_.getDepthBiasConstant() == 0.0F && p_394271_.getDepthBiasScaleFactor() == 0.0F) {
            GlStateManager._disablePolygonOffset();
         } else {
            GlStateManager._polygonOffset(p_394271_.getDepthBiasScaleFactor(), p_394271_.getDepthBiasConstant());
            GlStateManager._enablePolygonOffset();
         }

         switch (p_394271_.getColorLogic()) {
            case NONE:
               GlStateManager._disableColorLogicOp();
               break;
            case OR_REVERSE:
               GlStateManager._enableColorLogicOp();
               GlStateManager._logicOp(5387);
         }
      }
   }

   public void finishRenderPass() {
      this.inRenderPass = false;
      GlStateManager._glBindFramebuffer(36160, 0);
      this.device.debugLabels().popDebugGroup();
   }

   protected GlDevice getDevice() {
      return this.device;
   }

   @Override
   public GpuQuery timerQueryBegin() {
      RenderSystem.assertOnRenderThread();
      if (this.activeTimerQuery != null) {
         throw new IllegalStateException("A GL_TIME_ELAPSED query is already active");
      }

      int i = GL32C.glGenQueries();
      GL32C.glBeginQuery(35007, i);
      this.activeTimerQuery = new GlTimerQuery(i);
      return this.activeTimerQuery;
   }

   @Override
   public void timerQueryEnd(GpuQuery p_455716_) {
      RenderSystem.assertOnRenderThread();
      if (p_455716_ != this.activeTimerQuery) {
         throw new IllegalStateException("Mismatched or duplicate GpuQuery when ending timerQuery");
      }

      GL32C.glEndQuery(35007);
      this.activeTimerQuery = null;
   }
}
