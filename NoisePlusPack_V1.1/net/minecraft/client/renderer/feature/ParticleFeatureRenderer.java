package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Queue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ParticleFeatureRenderer implements AutoCloseable {
   private final Queue<ParticleFeatureRenderer.ParticleBufferCache> availableBuffers = new ArrayDeque<>();
   private final List<ParticleFeatureRenderer.ParticleBufferCache> usedBuffers = new ArrayList<>();

   public void render(SubmitNodeCollection p_427383_) {
      if (!p_427383_.getParticleGroupRenderers().isEmpty()) {
         GpuDevice gpudevice = RenderSystem.getDevice();
         Minecraft minecraft = Minecraft.getInstance();
         TextureManager texturemanager = minecraft.getTextureManager();
         RenderTarget rendertarget = minecraft.getMainRenderTarget();
         RenderTarget rendertarget1 = minecraft.levelRenderer.getParticlesTarget();

         for (SubmitNodeCollector.ParticleGroupRenderer submitnodecollector$particlegrouprenderer : p_427383_.getParticleGroupRenderers()) {
            ParticleFeatureRenderer.ParticleBufferCache particlefeaturerenderer$particlebuffercache = this.availableBuffers.poll();
            if (particlefeaturerenderer$particlebuffercache == null) {
               particlefeaturerenderer$particlebuffercache = new ParticleFeatureRenderer.ParticleBufferCache();
            }

            this.usedBuffers.add(particlefeaturerenderer$particlebuffercache);
            QuadParticleRenderState.PreparedBuffers quadparticlerenderstate$preparedbuffers = submitnodecollector$particlegrouprenderer.prepare(
               particlefeaturerenderer$particlebuffercache
            );
            if (quadparticlerenderstate$preparedbuffers != null) {
               try (RenderPass renderpass = gpudevice.createCommandEncoder()
                     .createRenderPass(
                        () -> "Particles - Main",
                        rendertarget.getColorTextureView(),
                        OptionalInt.empty(),
                        rendertarget.getDepthTextureView(),
                        OptionalDouble.empty()
                     )) {
                  this.prepareRenderPass(renderpass);
                  submitnodecollector$particlegrouprenderer.render(
                     quadparticlerenderstate$preparedbuffers, particlefeaturerenderer$particlebuffercache, renderpass, texturemanager, false
                  );
                  if (rendertarget1 == null) {
                     submitnodecollector$particlegrouprenderer.render(
                        quadparticlerenderstate$preparedbuffers, particlefeaturerenderer$particlebuffercache, renderpass, texturemanager, true
                     );
                  }
               }

               if (rendertarget1 != null) {
                  try (RenderPass renderpass1 = gpudevice.createCommandEncoder()
                        .createRenderPass(
                           () -> "Particles - Transparent",
                           rendertarget1.getColorTextureView(),
                           OptionalInt.empty(),
                           rendertarget1.getDepthTextureView(),
                           OptionalDouble.empty()
                        )) {
                     this.prepareRenderPass(renderpass1);
                     submitnodecollector$particlegrouprenderer.render(
                        quadparticlerenderstate$preparedbuffers, particlefeaturerenderer$particlebuffercache, renderpass1, texturemanager, true
                     );
                  }
               }
            }
         }
      }
   }

   public void endFrame() {
      for (ParticleFeatureRenderer.ParticleBufferCache particlefeaturerenderer$particlebuffercache : this.usedBuffers) {
         particlefeaturerenderer$particlebuffercache.rotate();
      }

      this.availableBuffers.addAll(this.usedBuffers);
      this.usedBuffers.clear();
   }

   private void prepareRenderPass(RenderPass p_427034_) {
      p_427034_.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
      p_427034_.setUniform("Fog", RenderSystem.getShaderFog());
      p_427034_.bindTexture(
         "Sampler2", Minecraft.getInstance().gameRenderer.lightTexture().getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
      );
   }

   @Override
   public void close() {
      this.availableBuffers.forEach(ParticleFeatureRenderer.ParticleBufferCache::close);
   }

   @OnlyIn(Dist.CLIENT)
   public static class ParticleBufferCache implements AutoCloseable {
      private @Nullable MappableRingBuffer ringBuffer;

      public void write(ByteBuffer p_423992_) {
         if (this.ringBuffer == null || this.ringBuffer.size() < p_423992_.remaining()) {
            if (this.ringBuffer != null) {
               this.ringBuffer.close();
            }

            this.ringBuffer = new MappableRingBuffer(() -> "Particle Vertices", 34, p_423992_.remaining());
         }

         try (GpuBuffer.MappedView gpubuffer$mappedview = RenderSystem.getDevice()
               .createCommandEncoder()
               .mapBuffer(this.ringBuffer.currentBuffer().slice(), false, true)) {
            gpubuffer$mappedview.data().put(p_423992_);
         }
      }

      public GpuBuffer get() {
         if (this.ringBuffer == null) {
            throw new IllegalStateException("Can't get buffer before it's made");
         } else {
            return this.ringBuffer.currentBuffer();
         }
      }

      void rotate() {
         if (this.ringBuffer != null) {
            this.ringBuffer.rotate();
         }
      }

      @Override
      public void close() {
         if (this.ringBuffer != null) {
            this.ringBuffer.close();
         }
      }
   }
}
