package com.mojang.blaze3d.opengl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL33C;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class GlProgram implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Set<String> BUILT_IN_UNIFORMS = Sets.newHashSet("Projection", "Lighting", "Fog", "Globals");
    public static final GlProgram INVALID_PROGRAM = new GlProgram(-1, "invalid");
    private final Map<String, Uniform> uniformsByName = new HashMap<>();
    private final int programId;
    private final String debugLabel;

    private GlProgram(final int programId, final String debugLabel) {
        this.programId = programId;
        this.debugLabel = debugLabel;
    }

    public static GlProgram link(
        final GlShaderModule vertexShader, final GlShaderModule fragmentShader, final @Nullable VertexFormat[] vertexBindings, final String debugLabel
    ) throws ShaderManager.CompilationException {
        int programId = GlStateManager.glCreateProgram();
        if (programId <= 0) {
            throw new ShaderManager.CompilationException("Could not create shader program (returned program ID " + programId + ")");
        }

        int attributeLocation = 0;
        String previousName = null;

        for (VertexFormat vertexFormat : vertexBindings) {
            if (vertexFormat != null) {
                for (VertexFormatElement attribute : vertexFormat.getElements()) {
                    String attributeName = attribute.name();
                    if (!attributeName.equals(previousName)) {
                        GlStateManager._glBindAttribLocation(programId, attributeLocation, attributeName);
                    }

                    previousName = attributeName;
                    attributeLocation++;
                }
            }
        }

        GlStateManager.glAttachShader(programId, vertexShader.getShaderId());
        GlStateManager.glAttachShader(programId, fragmentShader.getShaderId());
        GlStateManager.glLinkProgram(programId);
        int linkStatus = GlStateManager.glGetProgrami(programId, 35714);
        String linkMessage = GlStateManager.glGetProgramInfoLog(programId, 32768);
        if (linkStatus != 0 && !linkMessage.contains("Failed for unknown reason")) {
            if (!linkMessage.isEmpty()) {
                LOGGER.info(
                    "Info log when linking program containing VS {} and FS {}. Log output: {}", vertexShader.getId(), fragmentShader.getId(), linkMessage
                );
            }

            return new GlProgram(programId, debugLabel);
        } else {
            throw new ShaderManager.CompilationException(
                "Error encountered when linking program containing VS "
                    + vertexShader.getId()
                    + " and FS "
                    + fragmentShader.getId()
                    + ". Log output: "
                    + linkMessage
            );
        }
    }

    public void setupBindGroupLayouts(final List<BindGroupLayout> bindGroupLayouts) {
        BindGroupLayout.ensureCompatible(bindGroupLayouts);
        List<BindGroupLayout.UniformDescription> uniforms = BindGroupLayout.flattenUniforms(bindGroupLayouts);
        List<String> samplers = BindGroupLayout.flattenSamplers(bindGroupLayouts);
        int nextUboBinding = 0;
        int nextSamplerIndex = 0;

        for (BindGroupLayout.UniformDescription uniformDescription : uniforms) {
            String uniformName = uniformDescription.name();

            Uniform uniform = switch (uniformDescription.type()) {
                case UNIFORM_BUFFER -> {
                    int index = GL33C.glGetUniformBlockIndex(this.programId, uniformName);
                    if (index == -1) {
                        yield null;
                    } else {
                        int uboBinding = nextUboBinding++;
                        GL33C.glUniformBlockBinding(this.programId, index, uboBinding);
                        yield new Uniform.Ubo(uboBinding);
                    }
                }
                case TEXEL_BUFFER -> {
                    int location = GlStateManager._glGetUniformLocation(this.programId, uniformName);
                    if (location == -1) {
                        LOGGER.warn("{} shader program does not use utb {} defined in the pipeline. This might be a bug.", this.debugLabel, uniformName);
                        yield null;
                    } else {
                        int samplerIndex = nextSamplerIndex++;
                        yield new Uniform.Utb(location, samplerIndex, Objects.requireNonNull(uniformDescription.gpuFormat()));
                    }
                }
            };
            if (uniform != null) {
                this.uniformsByName.put(uniformName, uniform);
            }
        }

        for (String sampler : samplers) {
            int location = GlStateManager._glGetUniformLocation(this.programId, sampler);
            if (location == -1) {
                LOGGER.warn("{} shader program does not use sampler {} defined in the pipeline. This might be a bug.", this.debugLabel, sampler);
            } else {
                int samplerIndex = nextSamplerIndex++;
                this.uniformsByName.put(sampler, new Uniform.Sampler(location, samplerIndex));
            }
        }

        int totalDefinedBlocks = GlStateManager.glGetProgrami(this.programId, 35382);

        for (int i = 0; i < totalDefinedBlocks; i++) {
            String name = GL33C.glGetActiveUniformBlockName(this.programId, i);
            if (!this.uniformsByName.containsKey(name)) {
                if (!samplers.contains(name) && BUILT_IN_UNIFORMS.contains(name)) {
                    int uboBinding = nextUboBinding++;
                    GL33C.glUniformBlockBinding(this.programId, i, uboBinding);
                    this.uniformsByName.put(name, new Uniform.Ubo(uboBinding));
                } else {
                    LOGGER.warn("Found unknown and unsupported uniform {} in {}", name, this.debugLabel);
                }
            }
        }
    }

    @Override
    public void close() {
        this.uniformsByName.values().forEach(Uniform::close);
        GlStateManager.glDeleteProgram(this.programId);
    }

    public @Nullable Uniform getUniform(final String name) {
        RenderSystem.assertOnRenderThread();
        return this.uniformsByName.get(name);
    }

    @VisibleForTesting
    public int getProgramId() {
        return this.programId;
    }

    @Override
    public String toString() {
        return this.debugLabel;
    }

    public String getDebugLabel() {
        return this.debugLabel;
    }

    public Map<String, Uniform> getUniforms() {
        return this.uniformsByName;
    }
}