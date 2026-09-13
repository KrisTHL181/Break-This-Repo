package com.mojang.renderpearl.backend.vulkan.glsl;

import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormatElement;
import com.mojang.renderpearl.backend.vulkan.VulkanBindGroupLayout;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.renderer.ShaderDefines;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;

public class GlslCompiler implements AutoCloseable {
   private final long shaderCompiler = Shaderc.shaderc_compiler_initialize();
   private final long shaderOptions = Shaderc.shaderc_compile_options_initialize();
   private final ShaderDefines globalDefines;

   public GlslCompiler() {
      Shaderc.shaderc_compile_options_set_target_env(this.shaderOptions, 0, 4202496);
      Shaderc.shaderc_compile_options_set_auto_bind_uniforms(this.shaderOptions, true);
      Shaderc.shaderc_compile_options_set_auto_map_locations(this.shaderOptions, true);
      Shaderc.shaderc_compile_options_set_generate_debug_info(this.shaderOptions);
      Shaderc.shaderc_compile_options_set_optimization_level(this.shaderOptions, 0);
      this.globalDefines = ShaderDefines.builder()
         .define("gl_VertexID", "gl_VertexIndex")
         .define("gl_InstanceID", "gl_InstanceIndex")
         .define("B3D_DEPTH_IS_ZERO_TO_ONE")
         .build();
   }

   public IntermediaryShaderModule createIntermediary(final String filename, String source, final ShaderType type) throws ShaderCompileException {
      source = GlslPreprocessor.injectDefines(source, this.globalDefines);
      int shaderType = type == ShaderType.FRAGMENT ? 1 : 0;
      ByteBuffer sourceBuffer = MemoryUtil.memUTF8(source, false);
      ByteBuffer filenameBuffer = MemoryUtil.memUTF8(filename);
      ByteBuffer entrypointBuffer = MemoryUtil.memUTF8("main");
      long result = Shaderc.shaderc_compile_into_spv(this.shaderCompiler, sourceBuffer, shaderType, filenameBuffer, entrypointBuffer, this.shaderOptions);

      try {
         int status = Shaderc.shaderc_result_get_compilation_status(result);
         if (status != 0) {
            throw new ShaderCompileException("Couldn't parse GLSL: " + Shaderc.shaderc_result_get_error_message(result));
         }

         ByteBuffer spirv = Shaderc.shaderc_result_get_bytes(result);
         ByteBuffer copy = MemoryUtil.memCalloc(spirv.remaining());
         MemoryUtil.memCopy(spirv, copy);
         return IntermediaryShaderModule.createFromSpirv(filename, copy);
      } finally {
         Shaderc.shaderc_result_release(result);
         MemoryUtil.memFree(entrypointBuffer);
         MemoryUtil.memFree(filenameBuffer);
         MemoryUtil.memFree(sourceBuffer);
      }
   }

   public GlslCompiler.CompiledModules compile(
      final VulkanDevice device, final RenderPipeline pipeline, final IntermediaryShaderModule vertex, final IntermediaryShaderModule fragment
   ) throws ShaderCompileException {
      String pipelineName = pipeline.getLocation().toString();
      List<VulkanBindGroupLayout.Entry> entries = new ArrayList<>();
      addToBindGroup(entries, vertex, pipeline);
      addToBindGroup(entries, fragment, pipeline);
      List<String> vertexOutputNames = new ArrayList<>();

      for (SpvVariable output : vertex.outputs()) {
         vertexOutputNames.add(output.name());
      }

      List<String> vertexInputNames = new ArrayList<>();

      for (VertexFormat vertexFormat : pipeline.getVertexFormatBindings()) {
         if (vertexFormat != null) {
            for (VertexFormatElement attribute : vertexFormat.getElements()) {
               vertexInputNames.add(attribute.name());
            }
         }
      }

      vertex.rebind(vertexInputNames, entries);
      fragment.rebind(vertexOutputNames, entries);
      long vertexId = vertex.createVulkanShaderModule(device);
      long fragmentId = fragment.createVulkanShaderModule(device);
      VulkanBindGroupLayout layout = VulkanBindGroupLayout.create(device, entries, pipelineName);
      return new GlslCompiler.CompiledModules(vertexId, fragmentId, layout);
   }

   @Override
   public void close() {
      Shaderc.shaderc_compile_options_release(this.shaderOptions);
      Shaderc.shaderc_compiler_release(this.shaderCompiler);
   }

   private static void addToBindGroup(final List<VulkanBindGroupLayout.Entry> entries, final IntermediaryShaderModule shader, final RenderPipeline pipeline) throws ShaderCompileException {
      for (SpvUniformBuffer buffer : shader.uniformBuffers()) {
         String name = buffer.name();
         Optional<BindGroupLayout.UniformDescription> uniformDescription = BindGroupLayout.flattenUniforms(pipeline.getBindGroupLayouts())
            .stream()
            .filter(d -> d.name().equals(name))
            .findFirst();
         if (uniformDescription.isEmpty()) {
            throw new ShaderCompileException("Unable to find shader defined uniform (" + name + ")");
         }

         if (entries.stream().noneMatch(e -> e.type() == VulkanBindGroupLayout.VulkanBindGroupEntryType.UNIFORM_BUFFER && e.name().equals(name))) {
            entries.add(new VulkanBindGroupLayout.Entry(VulkanBindGroupLayout.VulkanBindGroupEntryType.UNIFORM_BUFFER, name, null));
         }
      }

      for (SpvSampler sampler : shader.samplers()) {
         String name = sampler.name();
         Optional<BindGroupLayout.UniformDescription> uniformDescription = BindGroupLayout.flattenUniforms(pipeline.getBindGroupLayouts())
            .stream()
            .filter(d -> d.name().equals(name))
            .findFirst();
         if (uniformDescription.isPresent()) {
            if (sampler.dimensions() != 5) {
               throw new ShaderCompileException("UTB (" + name + ") must have type of SpvDimBuffer");
            }

            if (entries.stream().noneMatch(e -> e.type() == VulkanBindGroupLayout.VulkanBindGroupEntryType.TEXEL_BUFFER && e.name().equals(name))) {
               entries.add(
                  new VulkanBindGroupLayout.Entry(VulkanBindGroupLayout.VulkanBindGroupEntryType.TEXEL_BUFFER, name, uniformDescription.get().gpuFormat())
               );
            }
         } else {
            if (BindGroupLayout.flattenSamplers(pipeline.getBindGroupLayouts()).stream().noneMatch(name::equals)) {
               throw new ShaderCompileException("Unable to find shader defined uniform (" + name + ")");
            }

            if (sampler.dimensions() != 1 && sampler.dimensions() != 3) {
               throw new ShaderCompileException("Sampled texture (" + name + ") must have type of SpvDim2D or SpvDimCube");
            }

            if (entries.stream().noneMatch(e -> e.type() == VulkanBindGroupLayout.VulkanBindGroupEntryType.SAMPLED_IMAGE && e.name().equals(name))) {
               entries.add(new VulkanBindGroupLayout.Entry(VulkanBindGroupLayout.VulkanBindGroupEntryType.SAMPLED_IMAGE, name, null));
            }
         }
      }
   }

   public record CompiledModules(long vertex, long fragment, VulkanBindGroupLayout layout) {
   }
}
