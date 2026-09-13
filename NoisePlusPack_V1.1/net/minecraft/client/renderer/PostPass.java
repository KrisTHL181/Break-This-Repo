package net.minecraft.client.renderer;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.SamplerCache;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Map.Entry;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.system.MemoryStack;

@OnlyIn(Dist.CLIENT)
public class PostPass implements AutoCloseable {
   private static final int UBO_SIZE_PER_SAMPLER = new Std140SizeCalculator().putVec2().get();
   private final String name;
   private final RenderPipeline pipeline;
   private final Identifier outputTargetId;
   private final Map<String, GpuBuffer> customUniforms = new HashMap<>();
   private final MappableRingBuffer infoUbo;
   private final List<PostPass.Input> inputs;

   public PostPass(RenderPipeline p_395322_, Identifier p_450426_, Map<String, List<UniformValue>> p_410093_, List<PostPass.Input> p_361905_) {
      this.pipeline = p_395322_;
      this.name = p_395322_.getLocation().toString();
      this.outputTargetId = p_450426_;
      this.inputs = p_361905_;

      for (Entry<String, List<UniformValue>> entry : p_410093_.entrySet()) {
         List<UniformValue> list = entry.getValue();
         if (!list.isEmpty()) {
            Std140SizeCalculator std140sizecalculator = new Std140SizeCalculator();

            for (UniformValue uniformvalue : list) {
               uniformvalue.addSize(std140sizecalculator);
            }

            int i = std140sizecalculator.get();
            MemoryStack memorystack = MemoryStack.stackPush();

            try {
               Std140Builder std140builder = Std140Builder.onStack(memorystack, i);

               for (UniformValue uniformvalue1 : list) {
                  uniformvalue1.writeTo(std140builder);
               }

               this.customUniforms
                  .put(entry.getKey(), RenderSystem.getDevice().createBuffer(() -> this.name + " / " + entry.getKey(), 128, std140builder.get()));
            } catch (Throwable var15) {
               if (memorystack != null) {
                  try {
                     memorystack.close();
                  } catch (Throwable var14) {
                     var15.addSuppressed(var14);
                  }
               }

               throw var15;
            }

            if (memorystack != null) {
               memorystack.close();
            }
         }
      }

      this.infoUbo = new MappableRingBuffer(() -> this.name + " SamplerInfo", 130, (p_361905_.size() + 1) * UBO_SIZE_PER_SAMPLER);
   }

   public void addToFrame(FrameGraphBuilder p_369714_, Map<Identifier, ResourceHandle<RenderTarget>> p_365909_, GpuBufferSlice p_406688_) {
      FramePass framepass = p_369714_.addPass(this.name);

      for (PostPass.Input postpass$input : this.inputs) {
         postpass$input.addToPass(framepass, p_365909_);
      }

      ResourceHandle<RenderTarget> resourcehandle = p_365909_.computeIfPresent(
         this.outputTargetId, (p_453255_, p_363433_) -> framepass.readsAndWrites((ResourceHandle<RenderTarget>)p_363433_)
      );
      if (resourcehandle == null) {
         throw new IllegalStateException("Missing handle for target " + this.outputTargetId);
      }

      framepass.executes(
         () -> {
            RenderTarget rendertarget = resourcehandle.get();
            RenderSystem.backupProjectionMatrix();
            RenderSystem.setProjectionMatrix(p_406688_, ProjectionType.ORTHOGRAPHIC);
            CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();
            SamplerCache samplercache = RenderSystem.getSamplerCache();
            List<PostPass.InputTexture> list = this.inputs
               .stream()
               .map(
                  p_448187_ -> new PostPass.InputTexture(
                     p_448187_.samplerName(),
                     p_448187_.texture(p_365909_),
                     samplercache.getClampToEdge(p_448187_.bilinear() ? FilterMode.LINEAR : FilterMode.NEAREST)
                  )
               )
               .toList();

            try (GpuBuffer.MappedView gpubuffer$mappedview = commandencoder.mapBuffer(this.infoUbo.currentBuffer(), false, true)) {
               Std140Builder std140builder = Std140Builder.intoBuffer(gpubuffer$mappedview.data());
               std140builder.putVec2(rendertarget.width, rendertarget.height);

               for (PostPass.InputTexture postpass$inputtexture : list) {
                  std140builder.putVec2(postpass$inputtexture.view.getWidth(0), postpass$inputtexture.view.getHeight(0));
               }
            }

            try (RenderPass renderpass = commandencoder.createRenderPass(
                  () -> "Post pass " + this.name,
                  rendertarget.getColorTextureView(),
                  OptionalInt.empty(),
                  rendertarget.useDepth ? rendertarget.getDepthTextureView() : null,
                  OptionalDouble.empty()
               )) {
               renderpass.setPipeline(this.pipeline);
               RenderSystem.bindDefaultUniforms(renderpass);
               renderpass.setUniform("SamplerInfo", this.infoUbo.currentBuffer());

               for (Entry<String, GpuBuffer> entry : this.customUniforms.entrySet()) {
                  renderpass.setUniform(entry.getKey(), entry.getValue());
               }

               for (PostPass.InputTexture postpass$inputtexture1 : list) {
                  renderpass.bindTexture(postpass$inputtexture1.samplerName() + "Sampler", postpass$inputtexture1.view(), postpass$inputtexture1.sampler());
               }

               renderpass.draw(0, 3);
            }

            this.infoUbo.rotate();
            RenderSystem.restoreProjectionMatrix();

            for (PostPass.Input postpass$input1 : this.inputs) {
               postpass$input1.cleanup(p_365909_);
            }
         }
      );
   }

   @Override
   public void close() {
      for (GpuBuffer gpubuffer : this.customUniforms.values()) {
         gpubuffer.close();
      }

      this.infoUbo.close();
   }

   @OnlyIn(Dist.CLIENT)
   public interface Input {
      void addToPass(FramePass var1, Map<Identifier, ResourceHandle<RenderTarget>> var2);

      default void cleanup(Map<Identifier, ResourceHandle<RenderTarget>> p_366914_) {
      }

      GpuTextureView texture(Map<Identifier, ResourceHandle<RenderTarget>> var1);

      String samplerName();

      boolean bilinear();
   }

   @OnlyIn(Dist.CLIENT)
   record InputTexture(String samplerName, GpuTextureView view, GpuSampler sampler) {
   }

   @OnlyIn(Dist.CLIENT)
   public record TargetInput(String samplerName, Identifier targetId, boolean depthBuffer, boolean bilinear) implements PostPass.Input {
      private ResourceHandle<RenderTarget> getHandle(Map<Identifier, ResourceHandle<RenderTarget>> p_369908_) {
         ResourceHandle<RenderTarget> resourcehandle = p_369908_.get(this.targetId);
         if (resourcehandle == null) {
            throw new IllegalStateException("Missing handle for target " + this.targetId);
         } else {
            return resourcehandle;
         }
      }

      @Override
      public void addToPass(FramePass p_369983_, Map<Identifier, ResourceHandle<RenderTarget>> p_369342_) {
         p_369983_.reads(this.getHandle(p_369342_));
      }

      @Override
      public GpuTextureView texture(Map<Identifier, ResourceHandle<RenderTarget>> p_363476_) {
         ResourceHandle<RenderTarget> resourcehandle = this.getHandle(p_363476_);
         RenderTarget rendertarget = resourcehandle.get();
         GpuTextureView gputextureview = this.depthBuffer ? rendertarget.getDepthTextureView() : rendertarget.getColorTextureView();
         if (gputextureview == null) {
            throw new IllegalStateException("Missing " + (this.depthBuffer ? "depth" : "color") + "texture for target " + this.targetId);
         } else {
            return gputextureview;
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record TextureInput(String samplerName, AbstractTexture texture, int width, int height, boolean bilinear) implements PostPass.Input {
      @Override
      public void addToPass(FramePass p_364568_, Map<Identifier, ResourceHandle<RenderTarget>> p_370060_) {
      }

      @Override
      public GpuTextureView texture(Map<Identifier, ResourceHandle<RenderTarget>> p_408443_) {
         return this.texture.getTextureView();
      }
   }
}
