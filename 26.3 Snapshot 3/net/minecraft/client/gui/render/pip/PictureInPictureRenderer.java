package net.minecraft.client.gui.render.pip;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Matrix4fStack;
import org.jspecify.annotations.Nullable;

public abstract class PictureInPictureRenderer<T extends PictureInPictureRenderState> implements AutoCloseable {
   private @Nullable GpuTexture texture;
   private @Nullable GpuTextureView textureView;
   private @Nullable GpuTexture depthTexture;
   private @Nullable GpuTextureView depthTextureView;
   private final Projection projection = new Projection();
   private final ProjectionMatrixBuffer projectionMatrixBuffer = new ProjectionMatrixBuffer("PIP - " + this.getClass().getSimpleName());
   private final SubmitNodeStorage submitNodeStorage = new SubmitNodeStorage();

   public void prepare(final T renderState, final GuiRenderState guiRenderState, final FeatureRenderDispatcher featureRenderDispatcher, final int guiScale) {
      int width = (renderState.x1() - renderState.x0()) * guiScale;
      int height = (renderState.y1() - renderState.y0()) * guiScale;
      boolean needsAResize = this.texture == null || this.texture.getWidth(0) != width || this.texture.getHeight(0) != height;
      if (!needsAResize && this.textureIsReadyToBlit(renderState)) {
         this.blitTexture(renderState, guiRenderState);
      } else {
         this.prepareTexturesAndProjection(needsAResize, width, height);
         Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
         modelViewStack.pushMatrix();
         PoseStack poseStack = new PoseStack();
         poseStack.translate(width / 2.0F, this.getTranslateY(height, guiScale), 0.0F);
         float scale = guiScale * renderState.scale();
         poseStack.scale(scale, scale, -scale);
         this.renderToTexture(renderState, poseStack, this.submitNodeStorage);

         try (
            FeatureRenderDispatcher.PreparedFrame frame = featureRenderDispatcher.prepareFrame(this.submitNodeStorage);
            RenderPass renderPass = RenderSystem.getDevice()
               .createCommandEncoder()
               .createRenderPass(() -> "Picture in picture", this.textureView, Optional.empty(), this.depthTextureView, OptionalDouble.empty());
         ) {
            RenderSystem.bindDefaultUniforms(renderPass);
            FeatureRenderDispatcher.renderAllFeatures(renderPass, frame);
         }

         modelViewStack.popMatrix();
         this.blitTexture(renderState, guiRenderState);
      }
   }

   protected void blitTexture(final T renderState, final GuiRenderState guiRenderState) {
      guiRenderState.addBlitToCurrentLayer(
         new BlitRenderState(
            RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
            TextureSetup.singleTexture(this.textureView, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
            renderState.pose(),
            renderState.x0(),
            renderState.y0(),
            renderState.x1(),
            renderState.y1(),
            0.0F,
            1.0F,
            1.0F,
            0.0F,
            -1,
            renderState.scissorArea(),
            null
         )
      );
   }

   private void prepareTexturesAndProjection(final boolean needsAResize, final int width, final int height) {
      if (this.texture != null && needsAResize) {
         this.texture.close();
         this.texture = null;
         this.textureView.close();
         this.textureView = null;
         this.depthTexture.close();
         this.depthTexture = null;
         this.depthTextureView.close();
         this.depthTextureView = null;
      }

      GpuDevice device = RenderSystem.getDevice();
      if (this.texture == null) {
         this.texture = device.createTexture(() -> "UI " + this.getTextureLabel() + " texture", 13, GpuFormat.RGBA8_UNORM, width, height, 1, 1);
         this.textureView = device.createTextureView(this.texture);
         this.depthTexture = device.createTexture(() -> "UI " + this.getTextureLabel() + " depth texture", 9, GpuFormat.D32_FLOAT, width, height, 1, 1);
         this.depthTextureView = device.createTextureView(this.depthTexture);
      }

      device.createCommandEncoder().clearColorAndDepthTextures(this.texture, GuiRenderer.CLEAR_COLOR, this.depthTexture, 0.0);
      this.projection.setupOrtho(-1000.0F, 1000.0F, width, height, true);
      RenderSystem.setProjectionMatrix(this.projectionMatrixBuffer.getBuffer(this.projection), ProjectionType.ORTHOGRAPHIC);
   }

   protected boolean textureIsReadyToBlit(final T renderState) {
      return false;
   }

   protected float getTranslateY(final int height, final int guiScale) {
      return height;
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

   protected abstract void renderToTexture(T renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector);

   protected abstract String getTextureLabel();
}
