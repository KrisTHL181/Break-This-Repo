package net.minecraft.client.renderer;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.SamplerCache;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Map.Entry;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryStack;

public class PostPass implements AutoCloseable {
   private static final int UBO_SIZE_PER_SAMPLER = new Std140SizeCalculator().putVec2().get();
   private final String name;
   private final RenderPipeline pipeline;
   private final Identifier outputTargetId;
   private final Map<String, GpuBuffer> customUniforms = new HashMap<>();
   private final MappableRingBuffer infoUbo;
   private final List<PostPass.Input> inputs;

   public PostPass(
      final RenderPipeline pipeline, final Identifier outputTargetId, final Map<String, List<UniformValue>> uniformGroups, final List<PostPass.Input> inputs
   ) {
      this.pipeline = pipeline;
      this.name = pipeline.getLocation().toString();
      this.outputTargetId = outputTargetId;
      this.inputs = inputs;

      for (Entry<String, List<UniformValue>> uniformGroup : uniformGroups.entrySet()) {
         List<UniformValue> uniforms = uniformGroup.getValue();
         if (!uniforms.isEmpty()) {
            Std140SizeCalculator calculator = new Std140SizeCalculator();

            for (UniformValue uniform : uniforms) {
               uniform.addSize(calculator);
            }

            int size = calculator.get();
            MemoryStack stack = MemoryStack.stackPush();

            try {
               Std140Builder builder = Std140Builder.onStack(stack, size);

               for (UniformValue uniform : uniforms) {
                  uniform.writeTo(builder);
               }

               this.customUniforms
                  .put(uniformGroup.getKey(), RenderSystem.getDevice().createBuffer(() -> this.name + " / " + uniformGroup.getKey(), 128, builder.get()));
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
         }
      }

      this.infoUbo = new MappableRingBuffer(() -> this.name + " SamplerInfo", 130, (inputs.size() + 1) * UBO_SIZE_PER_SAMPLER);
   }

   public void addToFrame(final FrameGraphBuilder frame, final Map<Identifier, ResourceHandle<RenderTarget>> targets, final GpuBufferSlice shaderOrthoMatrix) {
      FramePass pass = frame.addPass(this.name);

      for (PostPass.Input input : this.inputs) {
         input.addToPass(pass, targets);
      }

      ResourceHandle<RenderTarget> outputHandle = targets.computeIfPresent(
         this.outputTargetId, (id, handle) -> pass.readsAndWrites((ResourceHandle<RenderTarget>)handle)
      );
      if (outputHandle == null) {
         throw new IllegalStateException("Missing handle for target " + this.outputTargetId);
      }

      pass.executes(
         () -> {
            RenderTarget outputTarget = outputHandle.get();
            RenderSystem.backupProjectionMatrix();
            RenderSystem.setProjectionMatrix(shaderOrthoMatrix, ProjectionType.ORTHOGRAPHIC);
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            SamplerCache samplerCache = RenderSystem.getSamplerCache();
            List<PostPass.InputTexture> inputTextures = this.inputs
               .stream()
               .map(
                  i -> new PostPass.InputTexture(
                     i.samplerName(), i.texture(targets), samplerCache.getClampToEdge(i.bilinear() ? FilterMode.LINEAR : FilterMode.NEAREST)
                  )
               )
               .toList();

            try (GpuBufferSlice.MappedView view = this.infoUbo.currentBuffer().map(false, true)) {
               Std140Builder builder = Std140Builder.intoBuffer(view.data());
               builder.putVec2(outputTarget.width, outputTarget.height);

               for (PostPass.InputTexture inputxxx : inputTextures) {
                  builder.putVec2(inputxxx.view.getWidth(0), inputxxx.view.getHeight(0));
               }
            }

            try (RenderPass renderPass = commandEncoder.createRenderPass(
                  () -> "Post pass " + this.name,
                  outputTarget.getColorTextureView(),
                  Optional.empty(),
                  outputTarget.hasDepth() ? outputTarget.getDepthTextureView() : null,
                  OptionalDouble.empty()
               )) {
               renderPass.setPipeline(RenderSystem.getCompiledPipeline(this.pipeline));
               RenderSystem.bindDefaultUniforms(renderPass);
               renderPass.setUniform("SamplerInfo", this.infoUbo.currentBuffer());

               for (Entry<String, GpuBuffer> entry : this.customUniforms.entrySet()) {
                  renderPass.setUniform(entry.getKey(), entry.getValue());
               }

               for (PostPass.InputTexture inputx : inputTextures) {
                  renderPass.bindTexture(inputx.samplerName() + "Sampler", inputx.view(), inputx.sampler());
               }

               renderPass.draw(3, 1, 0, 0);
            }

            this.infoUbo.rotate();
            RenderSystem.restoreProjectionMatrix();

            for (PostPass.Input inputxx : this.inputs) {
               inputxx.cleanup(targets);
            }
         }
      );
   }

   @Override
   public void close() {
      for (GpuBuffer buffer : this.customUniforms.values()) {
         buffer.close();
      }

      this.infoUbo.close();
   }

   public interface Input {
      void addToPass(FramePass pass, Map<Identifier, ResourceHandle<RenderTarget>> targets);

      default void cleanup(final Map<Identifier, ResourceHandle<RenderTarget>> targets) {
      }

      GpuTextureView texture(final Map<Identifier, ResourceHandle<RenderTarget>> targets);

      String samplerName();

      boolean bilinear();
   }

   private record InputTexture(String samplerName, GpuTextureView view, GpuSampler sampler) {
   }

   public record TargetInput(String samplerName, Identifier targetId, boolean depthBuffer, boolean bilinear) implements PostPass.Input {
      private ResourceHandle<RenderTarget> getHandle(final Map<Identifier, ResourceHandle<RenderTarget>> targets) {
         ResourceHandle<RenderTarget> handle = targets.get(this.targetId);
         if (handle == null) {
            throw new IllegalStateException("Missing handle for target " + this.targetId);
         } else {
            return handle;
         }
      }

      @Override
      public void addToPass(final FramePass pass, final Map<Identifier, ResourceHandle<RenderTarget>> targets) {
         pass.reads(this.getHandle(targets));
      }

      @Override
      public GpuTextureView texture(final Map<Identifier, ResourceHandle<RenderTarget>> targets) {
         ResourceHandle<RenderTarget> handle = this.getHandle(targets);
         RenderTarget target = handle.get();
         GpuTextureView textureView = this.depthBuffer ? target.getDepthTextureView() : target.getColorTextureView();
         if (textureView == null) {
            throw new IllegalStateException("Missing " + (this.depthBuffer ? "depth" : "color") + "texture for target " + this.targetId);
         } else {
            return textureView;
         }
      }
   }

   public record TextureInput(String samplerName, AbstractTexture texture, int width, int height, boolean bilinear) implements PostPass.Input {
      @Override
      public void addToPass(final FramePass pass, final Map<Identifier, ResourceHandle<RenderTarget>> targets) {
      }

      @Override
      public GpuTextureView texture(final Map<Identifier, ResourceHandle<RenderTarget>> targets) {
         return this.texture.getTextureView();
      }
   }
}
