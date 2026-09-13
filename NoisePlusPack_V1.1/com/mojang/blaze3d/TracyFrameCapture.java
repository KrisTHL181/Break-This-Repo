package com.mojang.blaze3d;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.jtracy.TracyClient;
import java.util.OptionalInt;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TracyFrameCapture implements AutoCloseable {
   private static final int MAX_WIDTH = 320;
   private static final int MAX_HEIGHT = 180;
   private static final long BYTES_PER_PIXEL = 4L;
   private int targetWidth;
   private int targetHeight;
   private int width;
   private int height;
   private GpuTexture frameBuffer;
   private GpuTextureView frameBufferView;
   private GpuBuffer pixelbuffer;
   private int lastCaptureDelay;
   private boolean capturedThisFrame;
   private TracyFrameCapture.Status status = TracyFrameCapture.Status.WAITING_FOR_CAPTURE;

   public TracyFrameCapture() {
      this.width = 320;
      this.height = 180;
      GpuDevice gpudevice = RenderSystem.getDevice();
      this.frameBuffer = gpudevice.createTexture("Tracy Frame Capture", 10, TextureFormat.RGBA8, this.width, this.height, 1, 1);
      this.frameBufferView = gpudevice.createTextureView(this.frameBuffer);
      this.pixelbuffer = gpudevice.createBuffer(() -> "Tracy Frame Capture buffer", 9, this.width * this.height * 4L);
   }

   private void resize(int p_361808_, int p_365044_) {
      float f = (float)p_361808_ / p_365044_;
      if (p_361808_ > 320) {
         p_361808_ = 320;
         p_365044_ = (int)(320.0F / f);
      }

      if (p_365044_ > 180) {
         p_361808_ = (int)(180.0F * f);
         p_365044_ = 180;
      }

      p_361808_ = p_361808_ / 4 * 4;
      p_365044_ = p_365044_ / 4 * 4;
      if (this.width != p_361808_ || this.height != p_365044_) {
         this.width = p_361808_;
         this.height = p_365044_;
         GpuDevice gpudevice = RenderSystem.getDevice();
         this.frameBuffer.close();
         this.frameBuffer = gpudevice.createTexture("Tracy Frame Capture", 10, TextureFormat.RGBA8, p_361808_, p_365044_, 1, 1);
         this.frameBufferView.close();
         this.frameBufferView = gpudevice.createTextureView(this.frameBuffer);
         this.pixelbuffer.close();
         this.pixelbuffer = gpudevice.createBuffer(() -> "Tracy Frame Capture buffer", 9, p_361808_ * p_365044_ * 4L);
      }
   }

   public void capture(RenderTarget p_367460_) {
      if (this.status == TracyFrameCapture.Status.WAITING_FOR_CAPTURE && !this.capturedThisFrame && p_367460_.getColorTexture() != null) {
         this.capturedThisFrame = true;
         if (p_367460_.width != this.targetWidth || p_367460_.height != this.targetHeight) {
            this.targetWidth = p_367460_.width;
            this.targetHeight = p_367460_.height;
            this.resize(this.targetWidth, this.targetHeight);
         }

         this.status = TracyFrameCapture.Status.WAITING_FOR_COPY;
         CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();

         try (RenderPass renderpass = RenderSystem.getDevice()
               .createCommandEncoder()
               .createRenderPass(() -> "Tracy blit", this.frameBufferView, OptionalInt.empty())) {
            renderpass.setPipeline(RenderPipelines.TRACY_BLIT);
            renderpass.bindTexture("InSampler", p_367460_.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            renderpass.draw(0, 3);
         }

         commandencoder.copyTextureToBuffer(this.frameBuffer, this.pixelbuffer, 0L, () -> this.status = TracyFrameCapture.Status.WAITING_FOR_UPLOAD, 0);
         this.lastCaptureDelay = 0;
      }
   }

   public void upload() {
      if (this.status == TracyFrameCapture.Status.WAITING_FOR_UPLOAD) {
         this.status = TracyFrameCapture.Status.WAITING_FOR_CAPTURE;

         try (GpuBuffer.MappedView gpubuffer$mappedview = RenderSystem.getDevice().createCommandEncoder().mapBuffer(this.pixelbuffer, true, false)) {
            TracyClient.frameImage(gpubuffer$mappedview.data(), this.width, this.height, this.lastCaptureDelay, true);
         }
      }
   }

   public void endFrame() {
      this.lastCaptureDelay++;
      this.capturedThisFrame = false;
      TracyClient.markFrame();
   }

   @Override
   public void close() {
      this.frameBuffer.close();
      this.frameBufferView.close();
      this.pixelbuffer.close();
   }

   @OnlyIn(Dist.CLIENT)
   enum Status {
      WAITING_FOR_CAPTURE,
      WAITING_FOR_COPY,
      WAITING_FOR_UPLOAD;
   }
}
