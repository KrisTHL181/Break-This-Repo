package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.IdentifierException;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.FileUtil;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ShaderManager extends SimplePreparableReloadListener<ShaderManager.Configs> implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int MAX_LOG_LENGTH = 32768;
    public static final String SHADER_PATH = "shaders";
    private static final String SHADER_INCLUDE_PATH = "shaders/include/";
    private static final FileToIdConverter POST_CHAIN_ID_CONVERTER = FileToIdConverter.json("post_effect");
    private final TextureManager textureManager;
    private final Consumer<Exception> recoveryHandler;
    private ShaderManager.CompilationCache compilationCache = new ShaderManager.CompilationCache(ShaderManager.Configs.EMPTY);
    private final Projection postChainProjection = new Projection();
    private final ProjectionMatrixBuffer postChainProjectionMatrixBuffer = new ProjectionMatrixBuffer("post");

    public ShaderManager(final TextureManager textureManager, final Consumer<Exception> recoveryHandler) {
        this.textureManager = textureManager;
        this.recoveryHandler = recoveryHandler;
        this.postChainProjection.setupOrtho(0.1F, 1000.0F, 1.0F, 1.0F, false);
    }

    protected ShaderManager.Configs prepare(final ResourceManager manager, final ProfilerFiller profiler) {
        Builder<ShaderManager.ShaderSourceKey, String> shaderSources = ImmutableMap.builder();
        Map<Identifier, Resource> files = manager.listResources("shaders", ShaderManager::isShader);

        for (Entry<Identifier, Resource> entry : files.entrySet()) {
            Identifier location = entry.getKey();
            ShaderType shaderType = ShaderType.byLocation(location);
            if (shaderType != null) {
                loadShader(location, entry.getValue(), shaderType, files, shaderSources);
            }
        }

        Builder<Identifier, PostChainConfig> postChains = ImmutableMap.builder();

        for (Entry<Identifier, Resource> entry : POST_CHAIN_ID_CONVERTER.listMatchingResources(manager).entrySet()) {
            loadPostChain(entry.getKey(), entry.getValue(), postChains);
        }

        return new ShaderManager.Configs(shaderSources.build(), postChains.build());
    }

    private static void loadShader(
        final Identifier location,
        final Resource resource,
        final ShaderType type,
        final Map<Identifier, Resource> files,
        final Builder<ShaderManager.ShaderSourceKey, String> output
    ) {
        Identifier id = type.idConverter().fileToId(location);
        GlslPreprocessor preprocessor = createPreprocessor(files, location);

        try (Reader reader = resource.openAsReader()) {
            String source = IOUtils.toString(reader);
            output.put(new ShaderManager.ShaderSourceKey(id, type), String.join("", preprocessor.process(source)));
        } catch (IOException e) {
            LOGGER.error("Failed to load shader source at {}", location, e);
        }
    }

    private static GlslPreprocessor createPreprocessor(final Map<Identifier, Resource> files, final Identifier location) {
        final Identifier parentLocation = location.withPath(FileUtil::getFullResourcePath);
        return new GlslPreprocessor() {
            private final Set<Identifier> importedLocations = new ObjectArraySet<>();

            @Override
            public @Nullable String applyImport(final boolean isRelative, final String path) {
                Identifier locationx;
                try {
                    if (isRelative) {
                        locationx = parentLocation.withPath(parentPath -> FileUtil.normalizeResourcePath(parentPath + path));
                    } else {
                        locationx = Identifier.parse(path).withPrefix("shaders/include/");
                    }
                } catch (IdentifierException e) {
                    ShaderManager.LOGGER.error("Malformed GLSL import {}: {}", path, e.getMessage());
                    return "#error " + e.getMessage();
                }

                if (!this.importedLocations.add(locationx)) {
                    return null;
                }

                try (Reader importResource = files.get(locationx).openAsReader()) {
                    return IOUtils.toString(importResource);
                } catch (IOException e) {
                    ShaderManager.LOGGER.error("Could not open GLSL import {}: {}", locationx, e.getMessage());
                    return "#error " + e.getMessage();
                }
            }
        };
    }

    private static void loadPostChain(final Identifier location, final Resource resource, final Builder<Identifier, PostChainConfig> output) {
        Identifier id = POST_CHAIN_ID_CONVERTER.fileToId(location);

        try (Reader reader = resource.openAsReader()) {
            JsonElement json = StrictJsonParser.parse(reader);
            output.put(id, PostChainConfig.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(JsonSyntaxException::new));
        } catch (IOException | JsonParseException e) {
            LOGGER.error("Failed to parse post chain at {}", location, e);
        }
    }

    private static boolean isShader(final Identifier location) {
        return ShaderType.byLocation(location) != null || location.getPath().endsWith(".glsl");
    }

    protected void apply(final ShaderManager.Configs preparations, final ResourceManager manager, final ProfilerFiller profiler) {
        ShaderManager.CompilationCache newCompilationCache = new ShaderManager.CompilationCache(preparations);
        Set<RenderPipeline> pipelinesToPreload = new HashSet<>(RenderPipelines.getStaticPipelines());
        List<Identifier> failedLoads = new ArrayList<>();
        GpuDevice device = RenderSystem.getDevice();
        device.clearPipelineCache();

        for (RenderPipeline pipeline : pipelinesToPreload) {
            CompiledRenderPipeline compiled = device.precompilePipeline(pipeline, newCompilationCache::getShaderSource);
            if (!compiled.isValid()) {
                failedLoads.add(pipeline.getLocation());
            }
        }

        if (!failedLoads.isEmpty()) {
            device.clearPipelineCache();
            device.loadCriticalShaders();
            throw new RuntimeException(
                "Failed to load required shader programs:\n" + failedLoads.stream().map(entry -> " - " + entry).collect(Collectors.joining("\n"))
            );
        }

        this.compilationCache.close();
        this.compilationCache = newCompilationCache;
    }

    @Override
    public String getName() {
        return "Shader Loader";
    }

    private void tryTriggerRecovery(final Exception exception) {
        if (!this.compilationCache.triggeredRecovery) {
            this.recoveryHandler.accept(exception);
            this.compilationCache.triggeredRecovery = true;
        }
    }

    public @Nullable PostChain getPostChain(final Identifier id, final Set<Identifier> allowedTargets) {
        try {
            return this.compilationCache.getOrLoadPostChain(id, allowedTargets);
        } catch (ShaderManager.CompilationException e) {
            LOGGER.error("Failed to load post chain: {}", id, e);
            this.compilationCache.postChains.put(id, Optional.empty());
            this.tryTriggerRecovery(e);
            return null;
        }
    }

    @Override
    public void close() {
        this.compilationCache.close();
        this.postChainProjectionMatrixBuffer.close();
    }

    public @Nullable String getShader(final Identifier id, final ShaderType type) {
        return this.compilationCache.getShaderSource(id, type);
    }

    @OnlyIn(Dist.CLIENT)
    private class CompilationCache implements AutoCloseable {
        private final ShaderManager.Configs configs;
        private final Map<Identifier, Optional<PostChain>> postChains = new HashMap<>();
        private boolean triggeredRecovery;

        private CompilationCache(final ShaderManager.Configs configs) {
            this.configs = configs;
        }

        public @Nullable PostChain getOrLoadPostChain(final Identifier id, final Set<Identifier> allowedTargets) throws ShaderManager.CompilationException {
            Optional<PostChain> cached = this.postChains.get(id);
            if (cached != null) {
                return cached.orElse(null);
            }

            PostChain postChain = this.loadPostChain(id, allowedTargets);
            this.postChains.put(id, Optional.of(postChain));
            return postChain;
        }

        private PostChain loadPostChain(final Identifier id, final Set<Identifier> allowedTargets) throws ShaderManager.CompilationException {
            PostChainConfig config = this.configs.postChains.get(id);
            if (config == null) {
                throw new ShaderManager.CompilationException("Could not find post chain with id: " + id);
            } else {
                return PostChain.load(
                    config,
                    ShaderManager.this.textureManager,
                    allowedTargets,
                    id,
                    ShaderManager.this.postChainProjection,
                    ShaderManager.this.postChainProjectionMatrixBuffer
                );
            }
        }

        @Override
        public void close() {
            this.postChains.values().forEach(chain -> chain.ifPresent(PostChain::close));
            this.postChains.clear();
        }

        public @Nullable String getShaderSource(final Identifier id, final ShaderType type) {
            return this.configs.shaderSources.get(new ShaderManager.ShaderSourceKey(id, type));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class CompilationException extends Exception {
        public CompilationException(final String message) {
            super(message);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record Configs(Map<ShaderManager.ShaderSourceKey, String> shaderSources, Map<Identifier, PostChainConfig> postChains) {
        public static final ShaderManager.Configs EMPTY = new ShaderManager.Configs(Map.of(), Map.of());
    }

    @OnlyIn(Dist.CLIENT)
    private record ShaderSourceKey(Identifier id, ShaderType type) {
        @Override
        public String toString() {
            return this.id + " (" + this.type + ")";
        }
    }
}