package net.minecraft.client.gui.render.pip;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class PictureInPictureRenderer<T extends PictureInPictureRenderState> implements AutoCloseable {
   protected final MultiBufferSource.BufferSource bufferSource;
   private @Nullable GpuTexture texture;
   private @Nullable GpuTextureView textureView;
   private @Nullable GpuTexture depthTexture;
   private @Nullable GpuTextureView depthTextureView;
   private final CachedOrthoProjectionMatrixBuffer projectionMatrixBuffer = new CachedOrthoProjectionMatrixBuffer(
      "PIP - " + this.getClass().getSimpleName(), -1000.0F, 1000.0F, true
   );

   protected PictureInPictureRenderer(MultiBufferSource.BufferSource p_406498_) {
      this.bufferSource = p_406498_;
   }

   public void prepare(T p_409544_, GuiRenderState p_408696_, int p_408229_) {
      int i = (p_409544_.x1() - p_409544_.x0()) * p_408229_;
      int j = (p_409544_.y1() - p_409544_.y0()) * p_408229_;
      boolean flag = this.texture == null || this.texture.getWidth(0) != i || this.texture.getHeight(0) != j;
      if (!flag && this.textureIsReadyToBlit(p_409544_)) {
         this.blitTexture(p_409544_, p_408696_);
      } else {
         this.prepareTexturesAndProjection(flag, i, j);
         RenderSystem.outputColorTextureOverride = this.textureView;
         RenderSystem.outputDepthTextureOverride = this.depthTextureView;
         PoseStack posestack = new PoseStack();
         posestack.translate(i / 2.0F, this.getTranslateY(j, p_408229_), 0.0F);
         float f = p_408229_ * p_409544_.scale();
         posestack.scale(f, f, -f);
         this.renderToTexture(p_409544_, posestack);
         this.bufferSource.endBatch();
         RenderSystem.outputColorTextureOverride = null;
         RenderSystem.outputDepthTextureOverride = null;
         this.blitTexture(p_409544_, p_408696_);
      }
   }

   protected void blitTexture(T p_407638_, GuiRenderState p_409825_) {
      p_409825_.submitBlitToCurrentLayer(
         new BlitRenderState(
            RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
            TextureSetup.singleTexture(this.textureView, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
            p_407638_.pose(),
            p_407638_.x0(),
            p_407638_.y0(),
            p_407638_.x1(),
            p_407638_.y1(),
            0.0F,
            1.0F,
            1.0F,
            0.0F,
            -1,
            p_407638_.scissorArea(),
            null
         )
      );
   }

   private void prepareTexturesAndProjection(boolean p_405826_, int p_409682_, int p_409450_) {
      if (this.texture != null && p_405826_) {
         this.texture.close();
         this.texture = null;
         this.textureView.close();
         this.textureView = null;
         this.depthTexture.close();
         this.depthTexture = null;
         this.depthTextureView.close();
         this.depthTextureView = null;
      }

      GpuDevice gpudevice = RenderSystem.getDevice();
      if (this.texture == null) {
         this.texture = gpudevice.createTexture(() -> "UI " + this.getTextureLabel() + " texture", 12, TextureFormat.RGBA8, p_409682_, p_409450_, 1, 1);
         this.textureView = gpudevice.createTextureView(this.texture);
         this.depthTexture = gpudevice.createTexture(
            () -> "UI " + this.getTextureLabel() + " depth texture", 8, TextureFormat.DEPTH32, p_409682_, p_409450_, 1, 1
         );
         this.depthTextureView = gpudevice.createTextureView(this.depthTexture);
      }

      gpudevice.createCommandEncoder().clearColorAndDepthTextures(this.texture, 0, this.depthTexture, 1.0);
      RenderSystem.setProjectionMatrix(this.projectionMatrixBuffer.getBuffer(p_409682_, p_409450_), ProjectionType.ORTHOGRAPHIC);
   }

   protected boolean textureIsReadyToBlit(T p_405809_) {
      return false;
   }

   protected float getTranslateY(int p_407832_, int p_407310_) {
      return p_407832_;
   }

   @Override
   public void close() {
      if (this.texture != null) {
         this.texture.close();
      }

      if (this.textureView != null) {
         this.textureView.close();
      }

      if (this.depthTexture != null) {
         this.depthTexture.close();
      }

      if (this.depthTextureView != null) {
         this.depthTextureView.close();
      }

      this.projectionMatrixBuffer.close();
   }

   public abstract Class<T> getRenderStateClass();

   protected abstract void renderToTexture(T var1, PoseStack var2);

   protected abstract String getTextureLabel();
}
