package com.mojang.renderpearl.backend.vulkan;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.buffers.TransientMemory;
import com.mojang.renderpearl.api.commands.GpuFence;
import com.mojang.renderpearl.api.commands.GpuQueryPool;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.backend.api.CommandEncoderBackend;
import com.mojang.renderpearl.backend.api.RenderPassBackend;
import com.mojang.renderpearl.backend.vulkan.checkpoints.CheckpointExtension;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRDynamicRendering;
import org.lwjgl.vulkan.KHRSynchronization2;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkBufferImageCopy;
import org.lwjgl.vulkan.VkClearAttachment;
import org.lwjgl.vulkan.VkClearColorValue;
import org.lwjgl.vulkan.VkClearDepthStencilValue;
import org.lwjgl.vulkan.VkClearRect;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkDependencyInfo;
import org.lwjgl.vulkan.VkImageCopy;
import org.lwjgl.vulkan.VkImageSubresourceLayers;
import org.lwjgl.vulkan.VkImageSubresourceRange;
import org.lwjgl.vulkan.VkMemoryBarrier2;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;
import org.lwjgl.vulkan.VkRenderingInfo;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreTypeCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreWaitInfo;
import org.lwjgl.vulkan.VkMemoryBarrier2.Buffer;

public class VulkanCommandEncoder implements CommandEncoderBackend, Destroyable {
   public static final int MAX_SUBMITS_IN_FLIGHT = 2;
   private final VulkanDevice device;
   private final VulkanTransientMemory transientMemory;
   private final long submitSemaphore;
   private long currentSubmitIndex = 2L;
   private long completedSubmitIndex = 0L;
   private final CheckpointExtension.CheckpointStorage checkpointStorage;
   private VulkanQueue.Submission submissionBuilder;
   private final DestructionQueue<Destroyable> destroyQueue = new DestructionQueue<>(2, Destroyable::destroy);
   private final VulkanCommandPool[] commandPools = new VulkanCommandPool[2];
   private @Nullable VkCommandBuffer currentCommandBuffer;
   private @Nullable VulkanRenderPass currentRenderPass;

   public VulkanCommandEncoder(final VulkanDevice device) {
      this.device = device;
      this.transientMemory = new VulkanTransientMemory(device, this);
      MemoryStack baseStack = MemoryStack.stackGet();
      MemoryStack stack = baseStack.push();

      try {
         VkSemaphoreTypeCreateInfo semaphoreTypeCreateInfo = VkSemaphoreTypeCreateInfo.calloc(stack).sType$Default();
         semaphoreTypeCreateInfo.semaphoreType(1);
         semaphoreTypeCreateInfo.initialValue(this.currentSubmitIndex - 1L);
         VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(stack).sType$Default();
         semaphoreCreateInfo.pNext(semaphoreTypeCreateInfo);
         LongBuffer semaphoreHandlePtr = stack.callocLong(1);
         VulkanUtils.crashIfFailure(
            device, VK12.vkCreateSemaphore(device.vkDevice(), semaphoreCreateInfo, null, semaphoreHandlePtr), "Failed to create submit VkSemaphore"
         );
         this.submitSemaphore = semaphoreHandlePtr.get(0);
      } catch (Throwable var8) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }
         }

         throw var8;
      }

      if (stack != null) {
         stack.close();
      }

      for (int i = 0; i < 2; i++) {
         this.commandPools[i] = new VulkanCommandPool(device, device.graphicsQueue());
      }

      this.checkpointStorage = device.checkpointExtension().createStorage(device, device.graphicsQueue(), 2);
      this.submissionBuilder = device.graphicsQueue().beginSubmit();
      this.transientMemory.beginSubmit();
   }

   @Override
   public void destroy() {
      this.transientMemory.endSubmit();
      this.submissionBuilder.close();
      this.device.graphicsQueue().waitIdle();
      this.destroyQueue.close();
      this.transientMemory.destroy();
      this.destroyQueue.close();

      for (int i = 0; i < 2; i++) {
         this.commandPools[i].destroy();
      }

      VK12.vkDestroySemaphore(this.device.vkDevice(), this.submitSemaphore, null);
   }

   public void queueForDestroy(final Destroyable destroyable) {
      this.destroyQueue.add(destroyable);
   }

   private VulkanCommandPool currentCommandPool() {
      return this.commandPools[(int)(this.currentSubmitIndex % 2L)];
   }

   public VkCommandBuffer allocateAndBeginTransientCommandBuffer() {
      MemoryStack stack = MemoryStack.stackPush();

      VkCommandBuffer var4;
      try {
         VkCommandBuffer commandBuffer = this.currentCommandPool().allocateBuffer();
         VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack).sType$Default();
         beginInfo.flags(1);
         VulkanUtils.crashIfFailure(this.device, VK12.vkBeginCommandBuffer(commandBuffer, beginInfo), "Failed to begin VkCommandBuffer");
         var4 = commandBuffer;
      } catch (Throwable var6) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (stack != null) {
         stack.close();
      }

      return var4;
   }

   private VkCommandBuffer commandBuffer() {
      if (this.currentCommandBuffer != null) {
         return this.currentCommandBuffer;
      }

      if (this.currentRenderPass != null) {
         throw new IllegalStateException("Cannot start command buffer while inside RenderPass");
      }

      this.currentCommandBuffer = this.allocateAndBeginTransientCommandBuffer();
      this.submissionBuilder.executeCommands(this.currentCommandBuffer);
      return this.currentCommandBuffer;
   }

   VkCommandBuffer textureInitCommandBuffer() {
      return this.commandBuffer();
   }

   private void endCommandBuffer() {
      if (this.currentCommandBuffer != null) {
         if (this.currentRenderPass != null) {
            throw new IllegalStateException("Cannot end command buffer while inside RenderPass");
         }

         VulkanUtils.crashIfFailure(this.device, VK12.vkEndCommandBuffer(this.currentCommandBuffer), "Failed to end VkCommandBuffer");
         this.currentCommandBuffer = null;
      }
   }

   public void waitSemaphore(final long vkSemaphore, final long value, final long stageMask) {
      if (this.currentRenderPass != null) {
         throw new IllegalStateException("Cannot add semaphore operation while inside RenderPass");
      }

      this.endCommandBuffer();
      this.submissionBuilder.waitSemaphore(vkSemaphore, value, stageMask);
   }

   public void execute(final VkCommandBuffer commandBuffer) {
      if (this.currentRenderPass != null) {
         throw new IllegalStateException("Cannot execute command buffer while inside RenderPass");
      }

      this.endCommandBuffer();
      this.submissionBuilder.executeCommands(commandBuffer);
   }

   public void signalSemaphore(final long vkSemaphore, final long value, final long stageMask) {
      if (this.currentRenderPass != null) {
         throw new IllegalStateException("Cannot add semaphore operation while inside RenderPass");
      }

      this.endCommandBuffer();
      this.submissionBuilder.signalSemaphore(vkSemaphore, value, stageMask);
   }

   private void memoryBarrier(final MemoryStack stack) {
      memoryBarrier(this.commandBuffer(), stack);
   }

   public static void memoryBarrier(final VkCommandBuffer commandBuffer, final MemoryStack stack) {
      Buffer memoryBarrier = VkMemoryBarrier2.calloc(1, stack).sType$Default();
      memoryBarrier.srcStageMask(65536L);
      memoryBarrier.srcAccessMask(98304L);
      memoryBarrier.dstStageMask(65536L);
      memoryBarrier.dstAccessMask(98304L);
      VkDependencyInfo depInfo = VkDependencyInfo.calloc(stack).sType$Default();
      depInfo.pMemoryBarriers(memoryBarrier);
      KHRSynchronization2.vkCmdPipelineBarrier2KHR(commandBuffer, depInfo);
   }

   @Override
   public void submit() {
      this.endCommandBuffer();
      this.transientMemory.endSubmit();
      this.signalSemaphore(this.submitSemaphore, this.currentSubmitIndex, 65536L);
      this.submissionBuilder.close();
      this.submissionBuilder = this.device.graphicsQueue().beginSubmit();
      this.currentSubmitIndex++;
      if (!this.awaitSubmitCompletion(this.currentSubmitIndex - 2L, 5000000000L)) {
         List<CheckpointExtension.QueueCheckpoints> checkpoints = this.device.checkpointExtension().retrieveCheckpoints(false);
         throw new IllegalStateException("5s timeout reached when waiting for VK semaphore: " + VulkanUtils.formatCheckpoints(checkpoints));
      }

      this.currentCommandPool().reset();
      this.destroyQueue.rotate();
      this.checkpointStorage.rotate();
      this.transientMemory.beginSubmit();
   }

   @Override
   public TransientMemory transientMemory() {
      return this.transientMemory;
   }

   @Override
   public RenderPassBackend createRenderPass(final RenderPassDescriptor descriptor) {
      List<RenderPassDescriptor.Attachment<Optional<Vector4fc>>> colorAttachments = descriptor.colorAttachments();
      VulkanGpuTextureView[] colorTextures = new VulkanGpuTextureView[colorAttachments.size()];

      for (int i = 0; i < colorAttachments.size(); i++) {
         RenderPassDescriptor.Attachment<Optional<Vector4fc>> attachment = colorAttachments.get(i);
         colorTextures[i] = attachment != null ? (VulkanGpuTextureView)attachment.textureView() : null;
      }

      RenderPassDescriptor.Attachment<OptionalDouble> depthAttachment = descriptor.depthAttachment();
      this.device.instance().debug().beginDebugGroup(this.commandBuffer(), descriptor.label());
      this.checkpointStorage.recordCheckpoint(this.commandBuffer(), CheckpointExtension.CheckpointType.BEGIN_RENDER_PASS, descriptor.label());
      MemoryStack stack = MemoryStack.stackPush();

      try {
         int width = 0;
         int height = 0;
         if (!colorAttachments.isEmpty()) {
            for (RenderPassDescriptor.Attachment<Optional<Vector4fc>> colorAttachment : colorAttachments) {
               if (colorAttachment != null) {
                  GpuTextureView colorTexture = colorAttachment.textureView();
                  width = colorTexture.getWidth(0);
                  height = colorTexture.getHeight(0);
               }
            }
         } else if (depthAttachment != null) {
            width = depthAttachment.textureView().getWidth(0);
            height = depthAttachment.textureView().getHeight(0);
         }

         VkRect2D vkRenderArea = VkRect2D.calloc(stack);
         RenderPass.RenderArea renderArea = descriptor.renderArea();
         vkRenderArea.extent().set(renderArea.width(), renderArea.height());
         vkRenderArea.offset().set(renderArea.x(), renderArea.y());
         org.lwjgl.vulkan.VkRenderingAttachmentInfo.Buffer colorAttachmentInfo = VkRenderingAttachmentInfo.calloc(colorAttachments.size(), stack);

         for (int i = 0; i < colorAttachments.size(); i++) {
            ((org.lwjgl.vulkan.VkRenderingAttachmentInfo.Buffer)colorAttachmentInfo.position(i)).sType$Default();
            VulkanGpuTextureView colorTexture = colorTextures[i];
            if (colorTexture != null) {
               colorAttachmentInfo.imageView(colorTexture.vkImageView());
               colorAttachmentInfo.imageLayout(1);
               colorAttachmentInfo.storeOp(0);
               RenderPassDescriptor.Attachment<Optional<Vector4fc>> attachment = colorAttachments.get(i);
               Optional<Vector4fc> clearValue = attachment.clearValue();
               if (clearValue.isPresent()) {
                  Vector4fc color = clearValue.get();
                  VkClearColorValue vkClearColor = VulkanUtils.putArgb(VkClearColorValue.calloc(stack), color);
                  colorAttachmentInfo.loadOp(1);
                  colorAttachmentInfo.clearValue(VkClearValue.calloc(stack).color(vkClearColor));
               } else {
                  colorAttachmentInfo.loadOp(0);
               }
            } else {
               colorAttachmentInfo.imageView(0L);
               colorAttachmentInfo.imageLayout(0);
               colorAttachmentInfo.storeOp(1);
               colorAttachmentInfo.loadOp(2);
            }
         }

         colorAttachmentInfo.position(0);
         VkRenderingInfo renderingInfo = VkRenderingInfo.calloc(stack).sType$Default();
         renderingInfo.renderArea(vkRenderArea);
         renderingInfo.layerCount(1);
         renderingInfo.viewMask(0);
         renderingInfo.pColorAttachments(colorAttachmentInfo);
         if (depthAttachment != null) {
            VkRenderingAttachmentInfo depthAttachmentInfo = VkRenderingAttachmentInfo.calloc(stack).sType$Default();
            VulkanGpuTextureView vulkanDepthAttachment = (VulkanGpuTextureView)depthAttachment.textureView();
            depthAttachmentInfo.imageView(vulkanDepthAttachment.vkImageView());
            depthAttachmentInfo.imageLayout(1);
            depthAttachmentInfo.storeOp(0);
            OptionalDouble clearValue = depthAttachment.clearValue();
            if (clearValue.isPresent()) {
               double color = clearValue.getAsDouble();
               VkClearDepthStencilValue vkClearColor = VkClearDepthStencilValue.calloc(stack).depth((float)color);
               depthAttachmentInfo.loadOp(1);
               depthAttachmentInfo.clearValue(VkClearValue.calloc(stack).depthStencil(vkClearColor));
            } else {
               depthAttachmentInfo.loadOp(0);
            }

            renderingInfo.pDepthAttachment(depthAttachmentInfo);
         }

         KHRDynamicRendering.vkCmdBeginRenderingKHR(this.commandBuffer(), renderingInfo);
         this.currentRenderPass = new VulkanRenderPass(
            this.device, this, this.commandBuffer(), this.checkpointStorage, renderArea, width, height, depthAttachment != null, descriptor.label()
         );
      } catch (Throwable var19) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var18) {
               var19.addSuppressed(var18);
            }
         }

         throw var19;
      }

      if (stack != null) {
         stack.close();
      }

      return this.currentRenderPass;
   }

   @Override
   public void submitRenderPass() {
      if (this.currentRenderPass == null) {
         throw new IllegalStateException("Cannot submit a renderpass if one hasn't been started!");
      }

      KHRDynamicRendering.vkCmdEndRenderingKHR(this.commandBuffer());
      this.device.instance().debug().endDebugGroup(this.commandBuffer());
      this.checkpointStorage.recordCheckpoint(this.commandBuffer(), CheckpointExtension.CheckpointType.END_RENDER_PASS, this.currentRenderPass.getLabel());
      this.currentRenderPass = null;
      MemoryStack stack = MemoryStack.stackPush();

      try {
         this.memoryBarrier(stack);
      } catch (Throwable var5) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }
         }

         throw var5;
      }

      if (stack != null) {
         stack.close();
      }
   }

   private void clearColorTextureUnsynced(final MemoryStack stack, final GpuTexture colorTexture, final Vector4fc clearColor) {
      VkClearColorValue vkClearColor = VulkanUtils.putArgb(VkClearColorValue.calloc(stack), clearColor);
      VkImageSubresourceRange subresourceRange = VkImageSubresourceRange.calloc(stack);
      subresourceRange.baseMipLevel(0);
      subresourceRange.levelCount(colorTexture.getMipLevels());
      subresourceRange.baseArrayLayer(0);
      subresourceRange.layerCount(1);
      subresourceRange.aspectMask(1);
      VK12.vkCmdClearColorImage(this.commandBuffer(), ((VulkanGpuTexture)colorTexture).vkImage(), 1, vkClearColor, subresourceRange);
   }

   public void clearDepthTextureUnsynced(final MemoryStack stack, final GpuTexture depthTexture, final double clearDepth) {
      VkClearDepthStencilValue vkClearDepth = VkClearDepthStencilValue.calloc(stack).depth((float)clearDepth);
      VkImageSubresourceRange subresourceRange = VkImageSubresourceRange.calloc(stack);
      subresourceRange.baseMipLevel(0);
      subresourceRange.levelCount(depthTexture.getMipLevels());
      subresourceRange.baseArrayLayer(0);
      subresourceRange.layerCount(1);
      subresourceRange.aspectMask(2);
      VK12.vkCmdClearDepthStencilImage(this.commandBuffer(), ((VulkanGpuTexture)depthTexture).vkImage(), 1, vkClearDepth, subresourceRange);
   }

   @Override
   public void clearColorTexture(final GpuTexture colorTexture, final Vector4fc clearColor) {
      MemoryStack stack = MemoryStack.stackPush();

      try {
         this.clearColorTextureUnsynced(stack, colorTexture, clearColor);
         this.memoryBarrier(stack);
      } catch (Throwable var7) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }
         }

         throw var7;
      }

      if (stack != null) {
         stack.close();
      }
   }

   @Override
   public void clearColorAndDepthTextures(final GpuTexture colorTexture, final Vector4fc clearColor, final GpuTexture depthTexture, final double clearDepth) {
      MemoryStack stack = MemoryStack.stackPush();

      try {
         this.clearColorTextureUnsynced(stack, colorTexture, clearColor);
         this.clearDepthTextureUnsynced(stack, depthTexture, clearDepth);
         this.memoryBarrier(stack);
      } catch (Throwable var10) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var9) {
               var10.addSuppressed(var9);
            }
         }

         throw var10;
      }

      if (stack != null) {
         stack.close();
      }
   }

   @Override
   public void clearColorAndDepthTextures(
      final GpuTexture colorTexture,
      final Vector4fc clearColor,
      final GpuTexture depthTexture,
      final double clearDepth,
      final int regionX,
      final int regionY,
      final int regionWidth,
      final int regionHeight
   ) {
      try (
         GpuTextureView colorTextureView = this.device.createTextureView(colorTexture);
         GpuTextureView depthTextureView = this.device.createTextureView(depthTexture);
      ) {
         MemoryStack stack = MemoryStack.stackPush();

         try {
            this.createRenderPass(
               RenderPassDescriptor.builder(() -> "ClearColorDepthTextures")
                  .withColorAttachment(colorTextureView)
                  .withDepthAttachment(depthTextureView)
                  .withRenderArea(new RenderPass.RenderArea(0, 0, colorTexture.getWidth(0), colorTexture.getHeight(0)))
                  .build()
            );
            assert this.currentRenderPass != null;
            org.lwjgl.vulkan.VkClearRect.Buffer rects = VkClearRect.calloc(1, stack);
            rects.baseArrayLayer(0);
            rects.layerCount(1);
            rects.rect().offset().set(regionX, regionY);
            rects.rect().extent().set(regionWidth, regionHeight);
            org.lwjgl.vulkan.VkClearAttachment.Buffer attachments = VkClearAttachment.calloc(2, stack);
            VkClearValue colorClearValue = VkClearValue.calloc(stack);
            VulkanUtils.putArgb(colorClearValue.color(), clearColor);
            attachments.aspectMask(1);
            attachments.clearValue(colorClearValue);
            VkClearValue depthClearValue = VkClearValue.calloc(stack);
            VkClearDepthStencilValue clearValue = depthClearValue.depthStencil();
            clearValue.depth((float)clearDepth);
            attachments.position(1);
            attachments.aspectMask(2);
            attachments.clearValue(depthClearValue);
            attachments.position(0);
            VK12.vkCmdClearAttachments(this.commandBuffer(), attachments, rects);
            this.submitRenderPass();
         } catch (Throwable var21) {
            if (stack != null) {
               try {
                  stack.close();
               } catch (Throwable var20) {
                  var21.addSuppressed(var20);
               }
            }

            throw var21;
         }

         if (stack != null) {
            stack.close();
         }
      }
   }

   @Override
   public void clearDepthTexture(final GpuTexture depthTexture, final double clearDepth) {
      MemoryStack stack = MemoryStack.stackPush();

      try {
         this.clearDepthTextureUnsynced(stack, depthTexture, clearDepth);
         this.memoryBarrier(stack);
      } catch (Throwable var8) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }
         }

         throw var8;
      }

      if (stack != null) {
         stack.close();
      }
   }

   @Override
   public void writeToBuffer(final GpuBufferSlice destination, final ByteBuffer data) {
      VulkanGpuBuffer destBuffer = (VulkanGpuBuffer)destination.buffer();
      GpuBufferSlice stagingBuffer = this.transientMemory.uploadStaging(data, 1L, 16);
      MemoryStack stack = MemoryStack.stackPush();

      try {
         org.lwjgl.vulkan.VkBufferCopy.Buffer regions = VkBufferCopy.calloc(1, stack)
            .srcOffset(stagingBuffer.offset())
            .dstOffset(destination.offset())
            .size(data.remaining());
         VK12.vkCmdCopyBuffer(this.commandBuffer(), ((VulkanGpuBuffer)stagingBuffer.buffer()).vkBuffer(), destBuffer.vkBuffer(), regions);
         this.memoryBarrier(stack);
      } catch (Throwable var9) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }
         }

         throw var9;
      }

      if (stack != null) {
         stack.close();
      }
   }

   @Override
   public void copyToBuffer(final GpuBufferSlice source, final GpuBufferSlice target) {
      MemoryStack stack = MemoryStack.stackPush();

      try {
         org.lwjgl.vulkan.VkBufferCopy.Buffer copyInfo = VkBufferCopy.calloc(1, stack);
         copyInfo.srcOffset(source.offset());
         copyInfo.dstOffset(target.offset());
         copyInfo.size(source.length());
         VK12.vkCmdCopyBuffer(this.commandBuffer(), ((VulkanGpuBuffer)source.buffer()).vkBuffer(), ((VulkanGpuBuffer)target.buffer()).vkBuffer(), copyInfo);
         this.memoryBarrier(stack);
      } catch (Throwable var7) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }
         }

         throw var7;
      }

      if (stack != null) {
         stack.close();
      }
   }

   @Override
   public void writeToTexture(
      final GpuTexture destination,
      final ByteBuffer source,
      final int mipLevel,
      final int depthOrLayer,
      final int destX,
      final int destY,
      final int width,
      final int height
   ) {
      GpuBufferSlice stagingBuffer = this.transientMemory.uploadStaging(source, 1L, 16);
      MemoryStack stack = MemoryStack.stackPush();

      try {
         org.lwjgl.vulkan.VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack);
         region.bufferOffset(stagingBuffer.offset());
         region.bufferRowLength(width);
         region.bufferImageHeight(height);
         VkImageSubresourceLayers imageSubresource = region.imageSubresource();
         imageSubresource.aspectMask(1);
         imageSubresource.mipLevel(mipLevel);
         imageSubresource.baseArrayLayer(depthOrLayer);
         imageSubresource.layerCount(1);
         region.imageOffset().set(destX, destY, 0);
         region.imageExtent().set(width, height, 1);
         VK12.vkCmdCopyBufferToImage(
            this.commandBuffer(), ((VulkanGpuBuffer)stagingBuffer.buffer()).vkBuffer(), ((VulkanGpuTexture)destination).vkImage(), 1, region
         );
         this.memoryBarrier(stack);
      } catch (Throwable var14) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var13) {
               var14.addSuppressed(var13);
            }
         }

         throw var14;
      }

      if (stack != null) {
         stack.close();
      }
   }

   @Override
   public void copyBufferToTexture(
      final GpuBufferSlice source,
      final int sourceX,
      final int sourceY,
      final int sourceWidth,
      final int sourceHeight,
      final GpuTexture destination,
      final int destinationX,
      final int destinationY,
      final int copyWidth,
      final int copyHeight,
      final int mipLevel,
      final int arrayLayer
   ) {
      int texelSize = destination.getFormat().blockSize();
      long skipTexels = sourceX + (long)sourceY * sourceWidth;
      long skipBytes = skipTexels * texelSize;
      MemoryStack stack = MemoryStack.stackPush();

      try {
         org.lwjgl.vulkan.VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack);
         region.bufferOffset(source.offset() + skipBytes);
         region.bufferRowLength(sourceWidth);
         region.bufferImageHeight(sourceHeight);
         VkImageSubresourceLayers imageSubresource = region.imageSubresource();
         imageSubresource.aspectMask(1);
         imageSubresource.mipLevel(mipLevel);
         imageSubresource.baseArrayLayer(arrayLayer);
         imageSubresource.layerCount(1);
         region.imageOffset().set(destinationX, destinationY, 0);
         region.imageExtent().set(copyWidth, copyHeight, 1);
         VK12.vkCmdCopyBufferToImage(this.commandBuffer(), ((VulkanGpuBuffer)source.buffer()).vkBuffer(), ((VulkanGpuTexture)destination).vkImage(), 1, region);
         this.memoryBarrier(stack);
      } catch (Throwable var22) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var21) {
               var22.addSuppressed(var21);
            }
         }

         throw var22;
      }

      if (stack != null) {
         stack.close();
      }
   }

   @Override
   public void copyTextureToBuffer(final GpuTexture source, final GpuBuffer destination, final long offset, final Runnable callback, final int mipLevel) {
      this.copyTextureToBuffer(source, destination, offset, callback, mipLevel, 0, 0, source.getWidth(mipLevel), source.getHeight(mipLevel));
   }

   @Override
   public void copyTextureToBuffer(
      final GpuTexture source,
      final GpuBuffer destination,
      final long offset,
      final Runnable callback,
      final int mipLevel,
      final int x,
      final int y,
      final int width,
      final int height
   ) {
      MemoryStack stack = MemoryStack.stackPush();

      try {
         org.lwjgl.vulkan.VkBufferImageCopy.Buffer copy = VkBufferImageCopy.calloc(1, stack);
         copy.bufferOffset(offset);
         VkImageSubresourceLayers subresource = copy.imageSubresource();
         subresource.aspectMask(VulkanConst.formatAspectMask(source.getFormat()));
         subresource.mipLevel(mipLevel);
         subresource.baseArrayLayer(0);
         subresource.layerCount(1);
         copy.imageOffset().set(x, y, 0);
         copy.imageExtent().set(width, height, 1);
         copy.bufferRowLength(width);
         copy.bufferImageHeight(height);
         VK12.vkCmdCopyImageToBuffer(this.commandBuffer(), ((VulkanGpuTexture)source).vkImage(), 1, ((VulkanGpuBuffer)destination).vkBuffer(), copy);
         this.memoryBarrier(stack);
      } catch (Throwable var15) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var14) {
               var15.addSuppressed(var14);
            }
         }

         throw var15;
      }

      if (stack != null) {
         stack.close();
      }

      this.queueForDestroy(callback::run);
   }

   @Override
   public void copyTextureToTexture(
      final GpuTexture source,
      final GpuTexture destination,
      final int mipLevel,
      final int destX,
      final int destY,
      final int sourceX,
      final int sourceY,
      final int width,
      final int height
   ) {
      VulkanGpuTexture vulkanSrc = (VulkanGpuTexture)source;
      VulkanGpuTexture vulkanDst = (VulkanGpuTexture)destination;
      MemoryStack stack = MemoryStack.stackPush();

      try {
         VkImageSubresourceLayers subresourceLayers = VkImageSubresourceLayers.calloc(stack);
         subresourceLayers.mipLevel(mipLevel);
         subresourceLayers.baseArrayLayer(0);
         subresourceLayers.layerCount(1);
         subresourceLayers.aspectMask(VulkanConst.formatAspectMask(source.getFormat()));
         org.lwjgl.vulkan.VkImageCopy.Buffer regions = VkImageCopy.calloc(1, stack);
         regions.srcOffset().set(sourceX, sourceY, 0);
         regions.dstOffset().set(destX, destY, 0);
         regions.extent().set(width, height, 1);
         regions.srcSubresource(subresourceLayers);
         regions.dstSubresource(subresourceLayers);
         VK12.vkCmdCopyImage(this.commandBuffer(), vulkanSrc.vkImage(), 1, vulkanDst.vkImage(), 1, regions);
         this.memoryBarrier(stack);
      } catch (Throwable var16) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var15) {
               var16.addSuppressed(var15);
            }
         }

         throw var16;
      }

      if (stack != null) {
         stack.close();
      }
   }

   private boolean awaitSubmitCompletion(final long submitIndex, final long timeoutNS) {
      if (this.completedSubmitIndex >= submitIndex) {
         return true;
      }

      if (submitIndex == this.currentSubmitIndex) {
         if (timeoutNS == 0L) {
            return false;
         } else {
            throw new IllegalStateException("Cannot wait on a fence for the current submit");
         }
      } else {
         MemoryStack stack = MemoryStack.stackPush();

         boolean var9;
         try {
            VkSemaphoreWaitInfo waitInfo = VkSemaphoreWaitInfo.calloc(stack).sType$Default();
            waitInfo.pSemaphores(stack.longs(this.submitSemaphore));
            waitInfo.pValues(stack.longs(submitIndex));
            waitInfo.semaphoreCount(1);
            int result = VK12.vkWaitSemaphores(this.device.vkDevice(), waitInfo, timeoutNS);
            VulkanUtils.crashIfFailure(this.device, result, "Failed to wait for semaphore");
            boolean completed = result == 0;
            if (completed) {
               this.completedSubmitIndex = submitIndex;
            }

            var9 = completed;
         } catch (Throwable var11) {
            if (stack != null) {
               try {
                  stack.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }
            }

            throw var11;
         }

         if (stack != null) {
            stack.close();
         }

         return var9;
      }
   }

   @Override
   public GpuFence createFence() {
      return new GpuFence() {
         private final long submitIndex = VulkanCommandEncoder.this.currentSubmitIndex;
         private boolean completed = false;

         @Override
         public boolean awaitCompletion(final long timeoutMs) {
            if (!this.completed) {
               this.completed = VulkanCommandEncoder.this.awaitSubmitCompletion(this.submitIndex, timeoutMs);
            }

            return this.completed;
         }

         @Override
         public void close() {
            this.completed = true;
         }
      };
   }

   @Override
   public void writeTimestamp(final GpuQueryPool pool, final int index) {
      long queryPool = ((VulkanQueryPool)pool).vkQueryPool();
      VK12.vkResetQueryPool(this.device.vkDevice(), queryPool, index, 1);
      KHRSynchronization2.vkCmdWriteTimestamp2KHR(this.commandBuffer(), 65536L, queryPool, index);
   }

   public long getTimestampNow() {
      MemoryStack stack = MemoryStack.stackPush();

      long var5;
      try (VulkanQueryPool queryPool = (VulkanQueryPool)this.device.createTimestampQueryPool(1)) {
         VkCommandBuffer commandBuffer = this.allocateAndBeginTransientCommandBuffer();
         KHRSynchronization2.vkCmdWriteTimestamp2KHR(commandBuffer, 0L, queryPool.vkQueryPool(), 0);
         VulkanUtils.crashIfFailure(this.device, VK12.vkEndCommandBuffer(commandBuffer), "Failed to end VkCommandBuffer");

         try (VulkanQueue.Submission submit = this.device.graphicsQueue().beginSubmit()) {
            submit.executeCommands(commandBuffer);
         }

         LongBuffer timestampPtr = stack.callocLong(1);
         VulkanUtils.crashIfFailure(
            this.device,
            VK12.vkGetQueryPoolResults(this.device.vkDevice(), queryPool.vkQueryPool(), 0, 1, timestampPtr, 0L, 3),
            "Cannot fetch current timestamp"
         );
         var5 = timestampPtr.get(0);
      } catch (Throwable var12) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var7) {
               var12.addSuppressed(var7);
            }
         }

         throw var12;
      }

      if (stack != null) {
         stack.close();
      }

      return var5;
   }
}
