package com.mojang.blaze3d;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.jtracy.TracyClient;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.Optional;
import net.minecraft.client.renderer.RenderPipelines;

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
      GpuDevice device = RenderSystem.getDevice();
      this.frameBuffer = device.createTexture("Tracy Frame Capture", 10, GpuFormat.RGBA8_UNORM, this.width, this.height, 1, 1);
      this.frameBufferView = device.createTextureView(this.frameBuffer);
      this.pixelbuffer = device.createBuffer(() -> "Tracy Frame Capture buffer", 9, this.width * this.height * 4L);
   }

   private void resize(int width, int height) {
      float aspectRatio = (float)width / height;
      if (width > 320) {
         width = 320;
         height = (int)(320.0F / aspectRatio);
      }

      if (height > 180) {
         width = (int)(180.0F * aspectRatio);
         height = 180;
      }

      width = width / 4 * 4;
      height = height / 4 * 4;
      if (this.width != width || this.height != height) {
         this.width = width;
         this.height = height;
         GpuDevice device = RenderSystem.getDevice();
         this.frameBuffer.close();
         this.frameBuffer = device.createTexture("Tracy Frame Capture", 10, GpuFormat.RGBA8_UNORM, width, height, 1, 1);
         this.frameBufferView.close();
         this.frameBufferView = device.createTextureView(this.frameBuffer);
         this.pixelbuffer.close();
         this.pixelbuffer = device.createBuffer(() -> "Tracy Frame Capture buffer", 9, width * height * 4L);
      }
   }

   public void capture(final RenderTarget captureTarget) {
      if (this.status == TracyFrameCapture.Status.WAITING_FOR_CAPTURE && !this.capturedThisFrame && captureTarget.getColorTexture() != null) {
         this.capturedThisFrame = true;
         if (captureTarget.width != this.targetWidth || captureTarget.height != this.targetHeight) {
            this.targetWidth = captureTarget.width;
            this.targetHeight = captureTarget.height;
            this.resize(this.targetWidth, this.targetHeight);
         }

         this.status = TracyFrameCapture.Status.WAITING_FOR_COPY;
         CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

         try (RenderPass renderPass = RenderSystem.getDevice()
               .createCommandEncoder()
               .createRenderPass(() -> "Tracy blit", this.frameBufferView, Optional.empty())) {
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setPipeline(RenderSystem.getCompiledPipeline(RenderPipelines.TRACY_BLIT));
            renderPass.bindTexture("InSampler", captureTarget.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            renderPass.draw(3, 1, 0, 0);
         }

         commandEncoder.copyTextureToBuffer(this.frameBuffer, this.pixelbuffer, 0L, () -> this.status = TracyFrameCapture.Status.WAITING_FOR_UPLOAD, 0);
         this.lastCaptureDelay = 0;
      }
   }

   public void upload() {
      if (this.status == TracyFrameCapture.Status.WAITING_FOR_UPLOAD) {
         this.status = TracyFrameCapture.Status.WAITING_FOR_CAPTURE;

         try (GpuBufferSlice.MappedView view = this.pixelbuffer.map(true, false)) {
            TracyClient.frameImage(view.data(), this.width, this.height, this.lastCaptureDelay, true);
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

   private enum Status {
      WAITING_FOR_CAPTURE,
      WAITING_FOR_COPY,
      WAITING_FOR_UPLOAD;
   }
}
