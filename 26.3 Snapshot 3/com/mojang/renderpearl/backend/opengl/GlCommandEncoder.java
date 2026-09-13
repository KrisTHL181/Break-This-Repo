package com.mojang.renderpearl.backend.opengl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.logging.LogUtils;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.buffers.TransientMemory;
import com.mojang.renderpearl.api.commands.GpuFence;
import com.mojang.renderpearl.api.commands.GpuQueryPool;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.backend.api.CommandEncoderBackend;
import com.mojang.renderpearl.backend.api.RenderPassBackend;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.ARBBaseInstance;
import org.lwjgl.opengl.ARBDrawIndirect;
import org.lwjgl.opengl.ARBMultiDrawIndirect;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

class GlCommandEncoder implements CommandEncoderBackend, AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int MAX_SUBMITS_IN_FLIGHT = 2;
   private static final long NO_FENCE = 0L;
   private final GlDevice device;
   private final GlTransientMemory transientMemory;
   private final long[] fences = new long[2];
   private long currentSubmitIndex = 2L;
   private final int readFbo;
   private final int drawFbo;
   private @Nullable RenderPipeline lastPipeline;
   private @Nullable GlProgram lastProgram;
   private VertexArrayCache.@Nullable VertexArray lastVertexArray;
   private final List<@Nullable FrameBufferAttachment> renderPassColorTextures = new ArrayList<>();

   protected GlCommandEncoder(final GlDevice device) {
      this.device = device;
      this.transientMemory = device.getDeviceInfo().features().persistentMapping()
         ? new GlTransientMemory.PersistentMapping(device, this)
         : new GlTransientMemory.Fallback(device, this);
      this.readFbo = device.directStateAccess().createFrameBufferObject();
      this.drawFbo = device.directStateAccess().createFrameBufferObject();
   }

   @Override
   public void close() {
      this.transientMemory.close();
   }

   public long currentSubmitIndex() {
      return this.currentSubmitIndex;
   }

   public int currentSubmitSlot() {
      return (int)(this.currentSubmitIndex % 2L);
   }

   @Override
   public void submit() {
      this.fences[this.currentSubmitSlot()] = GL33C.glFenceSync(37143, 0);
      this.currentSubmitIndex++;
      if (!this.awaitSubmit(this.currentSubmitIndex - 2L, Long.MAX_VALUE)) {
         throw new IllegalStateException("Failed to wait for frame completion");
      }

      this.transientMemory.rotate();
   }

   public boolean awaitSubmit(final long index, final long timeoutNS) {
      if (this.currentSubmitIndex > index + 2L) {
         return true;
      }

      if (index == this.currentSubmitIndex) {
         if (timeoutNS == 0L) {
            return false;
         } else {
            throw new IllegalStateException("Cannot wait on a fence for the current submit");
         }
      } else {
         int submitSlot = (int)(index % 2L);
         long fence = this.fences[submitSlot];
         if (fence == 0L) {
            return true;
         }

         int result = GlStateManager._glClientWaitSync(fence, 1, timeoutNS);
         if (result == 37147) {
            return false;
         }

         if (result == 37149) {
            throw new IllegalStateException("Failed to complete GPU fence: " + GlStateManager._getError());
         }

         GL33C.glDeleteSync(this.fences[submitSlot]);
         this.fences[submitSlot] = 0L;
         return true;
      }
   }

   @Override
   public TransientMemory transientMemory() {
      return this.transientMemory;
   }

   @Override
   public RenderPassBackend createRenderPass(final RenderPassDescriptor descriptor) {
      this.device.debugLabels().pushDebugGroup(descriptor.label());
      List<RenderPassDescriptor.Attachment<Optional<Vector4fc>>> colorAttachments = descriptor.colorAttachments();
      this.renderPassColorTextures.clear();

      for (RenderPassDescriptor.Attachment<Optional<Vector4fc>> colorAttachment : colorAttachments) {
         this.renderPassColorTextures.add(colorAttachment != null ? (GlTextureView)colorAttachment.textureView() : null);
      }

      RenderPassDescriptor.Attachment<OptionalDouble> depthAttachment = descriptor.depthAttachment();
      int fbo = this.device
         .frameBufferCache()
         .getFbo(this.device.directStateAccess(), this.renderPassColorTextures, depthAttachment == null ? null : (GlTextureView)depthAttachment.textureView());
      GlStateManager._glBindFramebuffer(36160, fbo);
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

      RenderPass.RenderArea renderArea = descriptor.renderArea();
      GlStateManager._enableScissorTest();
      GlStateManager._scissorBox(renderArea.x(), renderArea.y(), renderArea.width(), renderArea.height());

      for (int i = 0; i < colorAttachments.size(); i++) {
         RenderPassDescriptor.Attachment<Optional<Vector4fc>> attachment = colorAttachments.get(i);
         if (attachment != null) {
            Optional<Vector4fc> clearValue = attachment.clearValue();
            if (clearValue.isPresent()) {
               GlStateManager._colorMask(i, 15);
               GlStateManager._clearBuffer(i, clearValue.get());
            }
         }
      }

      if (depthAttachment != null) {
         OptionalDouble clearValue = depthAttachment.clearValue();
         if (clearValue.isPresent()) {
            GlStateManager._depthMask(true);
            GlStateManager._clearBuffer(clearValue.getAsDouble());
         }
      }

      GlStateManager._viewport(0, 0, width, height);
      this.lastPipeline = null;
      ScissorState scissorState = new ScissorState();
      scissorState.enable(renderArea.x(), renderArea.y(), renderArea.width(), renderArea.height());
      return new GlRenderPass(this, this.device, depthAttachment != null, this.renderPassColorTextures.size(), scissorState);
   }

   @Override
   public void clearColorTexture(final GpuTexture colorTexture, final Vector4fc clearColor) {
      this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, ((GlTexture)colorTexture).id, 0, 0, 36160);
      GL33C.glClearColor(clearColor.x(), clearColor.y(), clearColor.z(), clearColor.w());
      GlStateManager._disableScissorTest();
      GlStateManager._colorMask(15);
      GlStateManager._clear(16384);
      GlStateManager._glFramebufferTexture2D(36160, 36064, 3553, 0, 0);
      GlStateManager._glBindFramebuffer(36160, 0);
   }

   @Override
   public void clearColorAndDepthTextures(final GpuTexture colorTexture, final Vector4fc clearColor, final GpuTexture depthTexture, final double clearDepth) {
      int fbo = this.device
         .frameBufferCache()
         .getFbo(this.device.directStateAccess(), Collections.singletonList((GlTexture)colorTexture), (GlTexture)depthTexture);
      GlStateManager._glBindFramebuffer(36160, fbo);
      GlStateManager._disableScissorTest();
      GL33C.glClearDepth(clearDepth);
      GL33C.glClearColor(clearColor.x(), clearColor.y(), clearColor.z(), clearColor.w());
      GlStateManager._depthMask(true);
      GlStateManager._colorMask(15);
      GlStateManager._clear(16640);
      GlStateManager._glBindFramebuffer(36160, 0);
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
      int fbo = this.device
         .frameBufferCache()
         .getFbo(this.device.directStateAccess(), Collections.singletonList((GlTexture)colorTexture), (GlTexture)depthTexture);
      GlStateManager._glBindFramebuffer(36160, fbo);
      GlStateManager._scissorBox(regionX, regionY, regionWidth, regionHeight);
      GlStateManager._enableScissorTest();
      GL33C.glClearDepth(clearDepth);
      GL33C.glClearColor(clearColor.x(), clearColor.y(), clearColor.z(), clearColor.w());
      GlStateManager._depthMask(true);
      GlStateManager._colorMask(15);
      GlStateManager._clear(16640);
      GlStateManager._glBindFramebuffer(36160, 0);
   }

   @Override
   public void clearDepthTexture(final GpuTexture depthTexture, final double clearDepth) {
      this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, 0, ((GlTexture)depthTexture).id, 0, 36160);
      GL33C.glDrawBuffer(0);
      GL33C.glClearDepth(clearDepth);
      GlStateManager._depthMask(true);
      GlStateManager._disableScissorTest();
      GlStateManager._clear(256);
      GL33C.glDrawBuffer(36064);
      GlStateManager._glFramebufferTexture2D(36160, 36096, 3553, 0, 0);
      GlStateManager._glBindFramebuffer(36160, 0);
   }

   @Override
   public void writeToBuffer(final GpuBufferSlice slice, final ByteBuffer data) {
      GlBuffer buffer = (GlBuffer)slice.buffer();
      buffer.checkCanBeUsed();
      this.device.directStateAccess().bufferSubData(buffer.handle(), slice.offset(), data, buffer.usage());
   }

   @Override
   public void copyToBuffer(final GpuBufferSlice source, final GpuBufferSlice target) {
      GlBuffer sourceBuffer = (GlBuffer)source.buffer();
      GlBuffer targetBuffer = (GlBuffer)target.buffer();
      sourceBuffer.checkCanBeUsed();
      targetBuffer.checkCanBeUsed();
      this.device.directStateAccess().copyBufferSubData(sourceBuffer.handle(), targetBuffer.handle(), source.offset(), target.offset(), source.length());
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
      int target;
      if ((destination.usage() & 16) != 0) {
         target = GlConst.CUBEMAP_TARGETS[depthOrLayer % 6];
         GL33C.glBindTexture(34067, ((GlTexture)destination).id);
      } else {
         target = 3553;
         GlStateManager._bindTexture(((GlTexture)destination).id);
      }

      GlStateManager._pixelStore(3314, width);
      GlStateManager._pixelStore(3316, 0);
      GlStateManager._pixelStore(3315, 0);
      GlStateManager._pixelStore(3317, destination.getFormat().componentCount());
      GlStateManager._texSubImage2D(target, mipLevel, destX, destY, width, height, GlConst.toGlExternalId(destination.getFormat()), 5121, source);
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
      int target;
      if ((destination.usage() & 16) != 0) {
         target = GlConst.CUBEMAP_TARGETS[arrayLayer % 6];
         GL33C.glBindTexture(34067, ((GlTexture)destination).id);
      } else {
         target = 3553;
         GlStateManager._bindTexture(((GlTexture)destination).id);
      }

      int texelSize = destination.getFormat().blockSize();
      long skipTexels = sourceX + (long)sourceY * sourceWidth;
      long skipBytes = skipTexels * texelSize;
      GlBuffer sourceGlBuffer = (GlBuffer)source.buffer();
      GlStateManager._glBindBuffer(35052, sourceGlBuffer.handle());
      GlStateManager._pixelStore(3314, sourceWidth);
      GlStateManager._pixelStore(32878, sourceHeight);
      GlStateManager._pixelStore(3316, 0);
      GlStateManager._pixelStore(3315, 0);
      GlStateManager._pixelStore(3317, destination.getFormat().byteAlignment());
      GlStateManager._texSubImage2D(
         target,
         mipLevel,
         destinationX,
         destinationY,
         copyWidth,
         copyHeight,
         GlConst.toGlExternalId(destination.getFormat()),
         GlConst.toGlType(destination.getFormat()),
         source.offset() + skipBytes
      );
      GlStateManager._glBindBuffer(35052, 0);
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
      ((GlBuffer)destination).checkCanBeUsed();
      GlStateManager.clearGlErrors();
      this.device.directStateAccess().bindFrameBufferTextures(this.readFbo, ((GlTexture)source).glId(), 0, mipLevel, 36008);
      GlStateManager._glBindBuffer(35051, ((GlBuffer)destination).handle());
      GlStateManager._pixelStore(3330, width);
      GlStateManager._readPixels(x, y, width, height, GlConst.toGlExternalId(source.getFormat()), GlConst.toGlType(source.getFormat()), offset);
      RenderSystem.queueFencedTask(callback);
      GlStateManager._glFramebufferTexture2D(36008, 36064, 3553, 0, mipLevel);
      GlStateManager._glBindFramebuffer(36008, 0);
      GlStateManager._glBindBuffer(35051, 0);
      int error = GlStateManager._getError();
      if (error != 0) {
         throw new IllegalStateException("Couldn't perform copyTobuffer for texture " + source.getLabel() + ": GL error " + error);
      }
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
      GlStateManager.clearGlErrors();
      GlStateManager._disableScissorTest();
      boolean isDepth = source.getFormat().hasDepthAspect();
      int sourceId = ((GlTexture)source).glId();
      int destId = ((GlTexture)destination).glId();
      this.device.directStateAccess().bindFrameBufferTextures(this.readFbo, isDepth ? 0 : sourceId, isDepth ? sourceId : 0, 0, 0);
      this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, isDepth ? 0 : destId, isDepth ? destId : 0, 0, 0);
      this.device
         .directStateAccess()
         .blitFrameBuffers(this.readFbo, this.drawFbo, sourceX, sourceY, width, height, destX, destY, width, height, isDepth ? 256 : 16384, 9728);
      int error = GlStateManager._getError();
      if (error != 0) {
         throw new IllegalStateException(
            "Couldn't perform copyToTexture for texture " + source.getLabel() + " to " + destination.getLabel() + ": GL error " + error
         );
      }
   }

   public void presentTexture(final GpuTextureView textureView, final int swapchainWidth, final int swapchainHeight) {
      int destY = Math.max(0, swapchainHeight - textureView.getHeight(0));
      int copyWidth = Math.min(swapchainWidth, textureView.getWidth(0));
      int copyHeight = Math.min(swapchainHeight, textureView.getHeight(0));
      GlStateManager._disableScissorTest();
      GlStateManager._viewport(0, 0, textureView.getWidth(0), textureView.getHeight(0));
      GlStateManager._depthMask(true);
      GlStateManager._colorMask(15);
      this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, ((GlTexture)textureView.texture()).glId(), 0, 0, 0);
      this.device.directStateAccess().blitFrameBuffers(this.drawFbo, 0, 0, 0, copyWidth, copyHeight, 0, destY, copyWidth, copyHeight + destY, 16384, 9728);
   }

   @Override
   public GpuFence createFence() {
      return new GlFence(this);
   }

   protected <T> void executeDrawMultiple(
      final GlRenderPass renderPass,
      final Collection<RenderPass.Draw<T>> draws,
      final @Nullable GpuBuffer defaultIndexBuffer,
      @Nullable IndexType defaultIndexType,
      final Collection<String> dynamicUniforms,
      final T uniformArgument
   ) {
      if (this.trySetup(renderPass, dynamicUniforms)) {
         if (defaultIndexType == null) {
            defaultIndexType = IndexType.SHORT;
         }

         for (RenderPass.Draw<T> draw : draws) {
            IndexType indexType = draw.indexType() == null ? defaultIndexType : draw.indexType();
            renderPass.setIndexBuffer(draw.indexBuffer() == null ? defaultIndexBuffer : draw.indexBuffer(), indexType);
            renderPass.setVertexBuffer(draw.slot(), draw.vertexBuffer().slice());
            if (GlRenderPass.VALIDATION) {
               if (renderPass.indexBuffer == null) {
                  throw new IllegalStateException("Missing index buffer");
               }

               ((GlBuffer)renderPass.indexBuffer).checkCanBeUsed();
               if (renderPass.indexBuffer.isClosed()) {
                  throw new IllegalStateException("Index buffer has been closed!");
               }

               if (draw.slot() < 0 || draw.slot() >= 16) {
                  throw new IllegalStateException("Vertex buffer slot must be between 0 and 16");
               }

               if (renderPass.vertexBuffers[draw.slot()] != null) {
                  ((GlBuffer)renderPass.vertexBuffers[draw.slot()].buffer()).checkCanBeUsed();
               }

               if (renderPass.vertexBuffers[draw.slot()] == null) {
                  throw new IllegalStateException("Missing vertex buffer at slot " + draw.slot());
               }

               if (renderPass.vertexBuffers[draw.slot()].buffer().isClosed()) {
                  throw new IllegalStateException("Vertex buffer at slot " + draw.slot() + " has been closed!");
               }
            }

            BiConsumer<T, RenderPass.UniformUploader> uniformUploaderConsumer = draw.uniformUploaderConsumer();
            if (uniformUploaderConsumer != null) {
               uniformUploaderConsumer.accept(uniformArgument, (name, buffer) -> {
                  ((GlBuffer)buffer.buffer()).checkCanBeUsed();
                  if (renderPass.pipeline.program().getUniform(name) instanceof Uniform.Ubo(int blockBinding)) {
                     GL33C.glBindBufferRange(35345, blockBinding, ((GlBuffer)buffer.buffer()).handle(), buffer.offset(), buffer.length());
                  }
               });
            }

            if (renderPass.vertexBufferDirty) {
               this.lastVertexArray = this.device
                  .vertexArrayCache()
                  .bindVertexArray(renderPass.pipeline.info().getVertexFormatBindings(), renderPass.vertexBuffers, null);
               renderPass.vertexBufferDirty = false;
            }

            this.drawFromBuffers(renderPass, draw.baseVertex(), draw.firstIndex(), draw.indexCount(), indexType, renderPass.pipeline, 1, 0);
         }
      }
   }

   private void validateDraw(final GlRenderPass renderPass, final @Nullable IndexType indexType) {
      if (GlRenderPass.VALIDATION) {
         if (indexType != null) {
            if (renderPass.indexBuffer == null) {
               throw new IllegalStateException("Missing index buffer");
            }

            ((GlBuffer)renderPass.indexBuffer).checkCanBeUsed();
            if (renderPass.indexBuffer.isClosed()) {
               throw new IllegalStateException("Index buffer has been closed!");
            }

            if ((renderPass.indexBuffer.usage() & 64) == 0) {
               throw new IllegalStateException("Index buffer must have GpuBuffer.USAGE_INDEX!");
            }
         }

         GlRenderPipeline pipeline = renderPass.pipeline;

         for (int i = 0; i < 16; i++) {
            if (renderPass.vertexBuffers[i] == null && pipeline != null && pipeline.info().getVertexFormatBindings()[i] != null) {
               throw new IllegalStateException("Vertex format contains elements but vertex buffer at slot " + i + " is null");
            }

            if (renderPass.vertexBuffers[i] != null) {
               ((GlBuffer)renderPass.vertexBuffers[i].buffer()).checkCanBeUsed();
            }
         }
      }
   }

   protected void executeDraw(
      final GlRenderPass renderPass,
      final int baseVertex,
      final int firstIndex,
      final int drawCount,
      final @Nullable IndexType indexType,
      final int instanceCount,
      final int firstInstance
   ) {
      if (this.trySetup(renderPass, Collections.emptyList())) {
         this.validateDraw(renderPass, indexType);
         this.lastVertexArray = this.device
            .vertexArrayCache()
            .bindVertexArray(
               renderPass.pipeline.info().getVertexFormatBindings(), renderPass.vertexBuffers, renderPass.vertexBufferDirty ? null : this.lastVertexArray
            );
         renderPass.vertexBufferDirty = false;
         this.drawFromBuffers(renderPass, baseVertex, firstIndex, drawCount, indexType, renderPass.pipeline, instanceCount, firstInstance);
      }
   }

   public void executeDraws(
      final GlRenderPass renderPass,
      final @Nullable IndexType indexType,
      final @Nullable PointerBuffer firstIndexOffsets,
      final IntBuffer indexCounts,
      final IntBuffer vertexOffsets,
      final int drawCount
   ) {
      if (this.trySetup(renderPass, Collections.emptyList())) {
         this.validateDraw(renderPass, indexType);
         this.lastVertexArray = this.device
            .vertexArrayCache()
            .bindVertexArray(
               renderPass.pipeline.info().getVertexFormatBindings(), renderPass.vertexBuffers, renderPass.vertexBufferDirty ? null : this.lastVertexArray
            );
         renderPass.vertexBufferDirty = false;
         if (indexType == null) {
            GL33C.nglMultiDrawArrays(
               GlConst.toGl(renderPass.pipeline.info().getPrimitiveTopology()),
               MemoryUtil.memAddress(vertexOffsets),
               MemoryUtil.memAddress(indexCounts),
               drawCount
            );
         } else {
            GlStateManager._glBindBuffer(34963, ((GlBuffer)renderPass.indexBuffer).handle());
            assert firstIndexOffsets != null;
            GL33C.nglMultiDrawElementsBaseVertex(
               GlConst.toGl(renderPass.pipeline.info().getPrimitiveTopology()),
               MemoryUtil.memAddress(indexCounts),
               GlConst.toGl(indexType),
               MemoryUtil.memAddress(firstIndexOffsets),
               drawCount,
               MemoryUtil.memAddress(vertexOffsets)
            );
         }
      }
   }

   protected void executeDrawIndirect(
      final GlRenderPass renderPass, final @Nullable IndexType indexType, final GlBuffer commands, final long offset, final int drawCount
   ) {
      if (this.trySetup(renderPass, Collections.emptyList())) {
         this.validateDraw(renderPass, indexType);
         this.lastVertexArray = this.device
            .vertexArrayCache()
            .bindVertexArray(
               renderPass.pipeline.info().getVertexFormatBindings(), renderPass.vertexBuffers, renderPass.vertexBufferDirty ? null : this.lastVertexArray
            );
         renderPass.vertexBufferDirty = false;
         GlStateManager._glBindBuffer(36671, commands.handle());
         if (indexType == null) {
            if (drawCount > 1) {
               ARBMultiDrawIndirect.glMultiDrawArraysIndirect(GlConst.toGl(renderPass.pipeline.info().getPrimitiveTopology()), offset, drawCount, 0);
            } else {
               ARBDrawIndirect.glDrawArraysIndirect(GlConst.toGl(renderPass.pipeline.info().getPrimitiveTopology()), offset);
            }
         } else {
            GlStateManager._glBindBuffer(34963, ((GlBuffer)renderPass.indexBuffer).handle());
            if (drawCount > 1) {
               ARBMultiDrawIndirect.glMultiDrawElementsIndirect(
                  GlConst.toGl(renderPass.pipeline.info().getPrimitiveTopology()), GlConst.toGl(indexType), offset, drawCount, 0
               );
            } else {
               ARBDrawIndirect.glDrawElementsIndirect(GlConst.toGl(renderPass.pipeline.info().getPrimitiveTopology()), GlConst.toGl(indexType), offset);
            }
         }
      }
   }

   private void drawFromBuffers(
      final GlRenderPass renderPass,
      final int baseVertex,
      final int firstIndex,
      final int drawCount,
      final @Nullable IndexType indexType,
      final GlRenderPipeline pipeline,
      final int instanceCount,
      final int firstInstance
   ) {
      if (indexType != null) {
         GlStateManager._glBindBuffer(34963, ((GlBuffer)renderPass.indexBuffer).handle());
         if (firstInstance > 0) {
            ARBBaseInstance.glDrawElementsInstancedBaseVertexBaseInstance(
               GlConst.toGl(pipeline.info().getPrimitiveTopology()),
               drawCount,
               GlConst.toGl(indexType),
               (long)firstIndex * indexType.bytes,
               instanceCount,
               baseVertex,
               firstInstance
            );
         } else {
            GL33C.glDrawElementsInstancedBaseVertex(
               GlConst.toGl(pipeline.info().getPrimitiveTopology()),
               drawCount,
               GlConst.toGl(indexType),
               (long)firstIndex * indexType.bytes,
               instanceCount,
               baseVertex
            );
         }
      } else if (firstInstance > 0) {
         ARBBaseInstance.glDrawArraysInstancedBaseInstance(
            GlConst.toGl(pipeline.info().getPrimitiveTopology()), baseVertex, drawCount, instanceCount, firstInstance
         );
      } else {
         GL33C.glDrawArraysInstanced(GlConst.toGl(pipeline.info().getPrimitiveTopology()), baseVertex, drawCount, instanceCount);
      }
   }

   private boolean trySetup(final GlRenderPass renderPass, final Collection<String> dynamicUniforms) {
      if (GlRenderPass.VALIDATION) {
         if (renderPass.pipeline == null) {
            throw new IllegalStateException("Can't draw without a render pipeline");
         }

         for (BindGroupLayout.UniformDescription uniform : BindGroupLayout.flattenUniforms(renderPass.pipeline.info().getBindGroupLayouts())) {
            GpuBufferSlice value = renderPass.uniforms.get(uniform.name());
            if (!dynamicUniforms.contains(uniform.name())) {
               if (value == null) {
                  throw new IllegalStateException("Missing uniform " + uniform.name() + " (should be " + uniform.type() + ")");
               }

               ((GlBuffer)value.buffer()).checkCanBeUsed();
               if (uniform.type() == UniformType.UNIFORM_BUFFER) {
                  if (value.buffer().isClosed()) {
                     throw new IllegalStateException("Uniform buffer " + uniform.name() + " is already closed");
                  }

                  if ((value.buffer().usage() & 128) == 0) {
                     throw new IllegalStateException("Uniform buffer " + uniform.name() + " must have GpuBuffer.USAGE_UNIFORM");
                  }
               }

               if (uniform.type() == UniformType.TEXEL_BUFFER) {
                  if (value.offset() != 0L || value.length() != value.buffer().size()) {
                     throw new IllegalStateException("Uniform texel buffers do not support a slice of a buffer, must be entire buffer");
                  }

                  if ((value.buffer().usage() & 256) == 0) {
                     throw new IllegalStateException("Uniform texel buffer " + uniform.name() + " must have GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER");
                  }

                  if (uniform.gpuFormat() == null) {
                     throw new IllegalStateException("Invalid uniform texel buffer " + uniform.name() + " (missing a texture format)");
                  }
               }
            }
         }

         for (Entry<String, Uniform> entry : renderPass.pipeline.program().getUniforms().entrySet()) {
            if (entry.getValue() instanceof Uniform.Sampler) {
               String name = entry.getKey();
               GlRenderPass.TextureViewAndSampler viewAndSampler = renderPass.samplers.get(name);
               if (viewAndSampler == null) {
                  throw new IllegalStateException("Missing sampler " + name);
               }

               GlTextureView textureView = viewAndSampler.view();
               if (textureView.isClosed()) {
                  throw new IllegalStateException("Texture view " + name + " (" + textureView.texture().getLabel() + ") has been closed!");
               }

               if ((textureView.texture().usage() & 4) == 0) {
                  throw new IllegalStateException("Texture view " + name + " (" + textureView.texture().getLabel() + ") must have USAGE_TEXTURE_BINDING!");
               }

               if (viewAndSampler.sampler().isClosed()) {
                  throw new IllegalStateException("Sampler for " + name + " (" + textureView.texture().getLabel() + ") has been closed!");
               }
            }
         }

         if (renderPass.pipeline.info().wantsDepthTexture() && !renderPass.hasDepthTexture()) {
            LOGGER.warn("Render pipeline {} wants a depth texture but none was provided - this is probably a bug", renderPass.pipeline.info().getLocation());
         }
      } else if (renderPass.pipeline == null) {
         return false;
      }

      RenderPipeline pipeline = renderPass.pipeline.info();
      GlProgram glProgram = renderPass.pipeline.program();
      this.applyPipelineState(pipeline);
      boolean differentProgram = this.lastProgram != glProgram;
      if (differentProgram) {
         GlStateManager._glUseProgram(glProgram.getProgramId());
         this.lastProgram = glProgram;
      }

      for (Entry<String, Uniform> entry : glProgram.getUniforms().entrySet()) {
         String name = entry.getKey();
         boolean isDirty = renderPass.dirtyUniforms.contains(name);
         switch ((Uniform)entry.getValue()) {
            case Uniform.Ubo(int blockBinding):
               if (isDirty) {
                  GpuBufferSlice bufferView = renderPass.uniforms.get(name);
                  GL33C.glBindBufferRange(35345, blockBinding, ((GlBuffer)bufferView.buffer()).handle(), bufferView.offset(), bufferView.length());
               }
               break;
            case Uniform.Utb(int location, int samplerIndex, GpuFormat format, int texture):
               if (differentProgram || isDirty) {
                  GlStateManager._glUniform1i(location, samplerIndex);
               }

               GlStateManager._activeTexture(33984 + samplerIndex);
               GL33C.glBindTexture(35882, texture);
               if (isDirty) {
                  GpuBufferSlice bufferView = renderPass.uniforms.get(name);
                  GL33C.glTexBuffer(35882, GlConst.toGlInternalId(format), ((GlBuffer)bufferView.buffer()).handle());
               }
               break;
            case Uniform.Sampler(int location, int samplerIndex):
               GlRenderPass.TextureViewAndSampler viewAndSampler = renderPass.samplers.get(name);
               if (viewAndSampler == null) {
                  break;
               }

               GlTextureView textureView = viewAndSampler.view();
               if (differentProgram || isDirty) {
                  GlStateManager._glUniform1i(location, samplerIndex);
               }

               GlStateManager._activeTexture(33984 + samplerIndex);
               GlTexture texture = textureView.texture();
               int target;
               if ((texture.usage() & 16) != 0) {
                  target = 34067;
                  GL33C.glBindTexture(34067, texture.id);
               } else {
                  target = 3553;
                  GlStateManager._bindTexture(texture.id);
               }

               GL33C.glBindSampler(samplerIndex, viewAndSampler.sampler().getId());
               GlStateManager._texParameter(target, 33084, textureView.baseMipLevel());
               GlStateManager._texParameter(target, 33085, textureView.baseMipLevel() + textureView.mipLevels() - 1);
               break;
            default:
               throw new MatchException(null, null);
         }
      }

      renderPass.dirtyUniforms.clear();
      int[] drawBuffers = new int[renderPass.colorAttachmentCount];

      for (int i = 0; i < renderPass.colorAttachmentCount; i++) {
         drawBuffers[i] = 36064 + i;
      }

      GL33C.glDrawBuffers(drawBuffers);
      if (renderPass.isScissorEnabled()) {
         GlStateManager._enableScissorTest();
         GlStateManager._scissorBox(renderPass.getScissorX(), renderPass.getScissorY(), renderPass.getScissorWidth(), renderPass.getScissorHeight());
      } else {
         GlStateManager._disableScissorTest();
      }

      return true;
   }

   private void applyPipelineState(final RenderPipeline pipeline) {
      if (this.lastPipeline != pipeline) {
         this.lastPipeline = pipeline;
         DepthStencilState depthStencilState = pipeline.getDepthStencilState();
         if (depthStencilState != null) {
            GlStateManager._enableDepthTest();
            GlStateManager._depthFunc(GlConst.toGl(depthStencilState.depthTest()));
            GlStateManager._depthMask(depthStencilState.writeDepth());
            if (depthStencilState.depthBiasConstant() == 0.0F && depthStencilState.depthBiasScaleFactor() == 0.0F) {
               GlStateManager._disablePolygonOffset();
            } else {
               GlStateManager._polygonOffset(depthStencilState.depthBiasScaleFactor(), depthStencilState.depthBiasConstant());
               GlStateManager._enablePolygonOffset();
            }
         } else {
            GlStateManager._disableDepthTest();
            GlStateManager._depthMask(false);
            GlStateManager._disablePolygonOffset();
         }

         if (pipeline.isCull()) {
            GlStateManager._enableCull();
         } else {
            GlStateManager._disableCull();
         }

         ColorTargetState[] colorTargetStates = pipeline.getColorTargetStates();

         for (int i = 0; i < colorTargetStates.length; i++) {
            ColorTargetState state = colorTargetStates[i];
            if (state != null) {
               if (state.blendFunction().isPresent()) {
                  GlStateManager._enableBlend(i);
                  BlendFunction blendFunction = state.blendFunction().get();
                  GlStateManager._blendFuncSeparate(
                     GlConst.toGl(blendFunction.color().sourceFactor()),
                     GlConst.toGl(blendFunction.color().destFactor()),
                     GlConst.toGl(blendFunction.alpha().sourceFactor()),
                     GlConst.toGl(blendFunction.alpha().destFactor())
                  );
                  GlStateManager._blendEquationSeparate(GlConst.toGl(blendFunction.color().op()), GlConst.toGl(blendFunction.alpha().op()));
               } else {
                  GlStateManager._disableBlend(i);
               }
            }
         }

         GlStateManager._polygonMode(1032, GlConst.toGl(pipeline.getPolygonMode()));

         for (int i = 0; i < colorTargetStates.length; i++) {
            ColorTargetState state = colorTargetStates[i];
            if (state != null) {
               GlStateManager._colorMask(i, state.writeMask());
            }
         }
      }
   }

   @Override
   public void submitRenderPass() {
      GlStateManager._glBindFramebuffer(36160, 0);
      this.device.debugLabels().popDebugGroup();
   }

   @Override
   public void writeTimestamp(final GpuQueryPool pool, final int index) {
      ((GlQueryPool)pool).writeTimestamp(index);
   }
}
