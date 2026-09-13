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
   static final Logger LOGGER = LogUtils.getLogger();
   public static final int MAX_LOG_LENGTH = 32768;
   public static final String SHADER_PATH = "shaders";
   private static final String SHADER_INCLUDE_PATH = "shaders/include/";
   private static final FileToIdConverter POST_CHAIN_ID_CONVERTER = FileToIdConverter.json("post_effect");
   final TextureManager textureManager;
   private final Consumer<Exception> recoveryHandler;
   private ShaderManager.CompilationCache compilationCache = new ShaderManager.CompilationCache(ShaderManager.Configs.EMPTY);
   final CachedOrthoProjectionMatrixBuffer postChainProjectionMatrixBuffer = new CachedOrthoProjectionMatrixBuffer("post", 0.1F, 1000.0F, false);

   public ShaderManager(TextureManager p_360733_, Consumer<Exception> p_367243_) {
      this.textureManager = p_360733_;
      this.recoveryHandler = p_367243_;
   }

   protected ShaderManager.Configs prepare(ResourceManager p_363890_, ProfilerFiller p_362646_) {
      Builder<ShaderManager.ShaderSourceKey, String> builder = ImmutableMap.builder();
      Map<Identifier, Resource> map = p_363890_.listResources("shaders", ShaderManager::isShader);

      for (Entry<Identifier, Resource> entry : map.entrySet()) {
         Identifier identifier = entry.getKey();
         ShaderType shadertype = ShaderType.byLocation(identifier);
         if (shadertype != null) {
            loadShader(identifier, entry.getValue(), shadertype, map, builder);
         }
      }

      Builder<Identifier, PostChainConfig> builder1 = ImmutableMap.builder();

      for (Entry<Identifier, Resource> entry1 : POST_CHAIN_ID_CONVERTER.listMatchingResources(p_363890_).entrySet()) {
         loadPostChain(entry1.getKey(), entry1.getValue(), builder1);
      }

      return new ShaderManager.Configs(builder.build(), builder1.build());
   }

   private static void loadShader(
      Identifier p_455103_,
      Resource p_361062_,
      ShaderType p_391859_,
      Map<Identifier, Resource> p_367069_,
      Builder<ShaderManager.ShaderSourceKey, String> p_365134_
   ) {
      Identifier identifier = p_391859_.idConverter().fileToId(p_455103_);
      GlslPreprocessor glslpreprocessor = createPreprocessor(p_367069_, p_455103_);

      try (Reader reader = p_361062_.openAsReader()) {
         String s = IOUtils.toString(reader);
         p_365134_.put(new ShaderManager.ShaderSourceKey(identifier, p_391859_), String.join("", glslpreprocessor.process(s)));
      } catch (IOException ioexception) {
         LOGGER.error("Failed to load shader source at {}", p_455103_, ioexception);
      }
   }

   private static GlslPreprocessor createPreprocessor(final Map<Identifier, Resource> p_367930_, Identifier p_450691_) {
      final Identifier identifier = p_450691_.withPath(FileUtil::getFullResourcePath);
      return new GlslPreprocessor() {
         private final Set<Identifier> importedLocations = new ObjectArraySet();

         @Override
         public @Nullable String applyImport(boolean p_365562_, String p_361440_) {
            Identifier identifier1;
            try {
               if (p_365562_) {
                  identifier1 = identifier.withPath(p_448191_ -> FileUtil.normalizeResourcePath(p_448191_ + p_361440_));
               } else {
                  identifier1 = Identifier.parse(p_361440_).withPrefix("shaders/include/");
               }
            } catch (IdentifierException identifierexception) {
               ShaderManager.LOGGER.error("Malformed GLSL import {}: {}", p_361440_, identifierexception.getMessage());
               return "#error " + identifierexception.getMessage();
            }

            if (!this.importedLocations.add(identifier1)) {
               return null;
            }

            try (Reader reader = p_367930_.get(identifier1).openAsReader()) {
               return IOUtils.toString(reader);
            } catch (IOException ioexception) {
               ShaderManager.LOGGER.error("Could not open GLSL import {}: {}", identifier1, ioexception.getMessage());
               return "#error " + ioexception.getMessage();
            }
         }
      };
   }

   private static void loadPostChain(Identifier p_460217_, Resource p_365135_, Builder<Identifier, PostChainConfig> p_362996_) {
      Identifier identifier = POST_CHAIN_ID_CONVERTER.fileToId(p_460217_);

      try (Reader reader = p_365135_.openAsReader()) {
         JsonElement jsonelement = StrictJsonParser.parse(reader);
         p_362996_.put(identifier, (PostChainConfig)PostChainConfig.CODEC.parse(JsonOps.INSTANCE, jsonelement).getOrThrow(JsonSyntaxException::new));
      } catch (JsonParseException | IOException ioexception) {
         LOGGER.error("Failed to parse post chain at {}", p_460217_, ioexception);
      }
   }

   private static boolean isShader(Identifier p_455622_) {
      return ShaderType.byLocation(p_455622_) != null || p_455622_.getPath().endsWith(".glsl");
   }

   protected void apply(ShaderManager.Configs p_360858_, ResourceManager p_369986_, ProfilerFiller p_364135_) {
      ShaderManager.CompilationCache shadermanager$compilationcache = new ShaderManager.CompilationCache(p_360858_);
      Set<RenderPipeline> set = new HashSet<>(RenderPipelines.getStaticPipelines());
      List<Identifier> list = new ArrayList<>();
      GpuDevice gpudevice = RenderSystem.getDevice();
      gpudevice.clearPipelineCache();

      for (RenderPipeline renderpipeline : set) {
         CompiledRenderPipeline compiledrenderpipeline = gpudevice.precompilePipeline(renderpipeline, shadermanager$compilationcache::getShaderSource);
         if (!compiledrenderpipeline.isValid()) {
            list.add(renderpipeline.getLocation());
         }
      }

      if (!list.isEmpty()) {
         gpudevice.clearPipelineCache();
         throw new RuntimeException(
            "Failed to load required shader programs:\n" + list.stream().map(p_455161_ -> " - " + p_455161_).collect(Collectors.joining("\n"))
         );
      }

      this.compilationCache.close();
      this.compilationCache = shadermanager$compilationcache;
   }

   @Override
   public String getName() {
      return "Shader Loader";
   }

   private void tryTriggerRecovery(Exception p_378248_) {
      if (!this.compilationCache.triggeredRecovery) {
         this.recoveryHandler.accept(p_378248_);
         this.compilationCache.triggeredRecovery = true;
      }
   }

   public @Nullable PostChain getPostChain(Identifier p_455256_, Set<Identifier> p_362698_) {
      try {
         return this.compilationCache.getOrLoadPostChain(p_455256_, p_362698_);
      } catch (ShaderManager.CompilationException shadermanager$compilationexception) {
         LOGGER.error("Failed to load post chain: {}", p_455256_, shadermanager$compilationexception);
         this.compilationCache.postChains.put(p_455256_, Optional.empty());
         this.tryTriggerRecovery(shadermanager$compilationexception);
         return null;
      }
   }

   @Override
   public void close() {
      this.compilationCache.close();
      this.postChainProjectionMatrixBuffer.close();
   }

   public @Nullable String getShader(Identifier p_460967_, ShaderType p_393108_) {
      return this.compilationCache.getShaderSource(p_460967_, p_393108_);
   }

   @OnlyIn(Dist.CLIENT)
   class CompilationCache implements AutoCloseable {
      private final ShaderManager.Configs configs;
      final Map<Identifier, Optional<PostChain>> postChains = new HashMap<>();
      boolean triggeredRecovery;

      CompilationCache(final ShaderManager.Configs p_369367_) {
         this.configs = p_369367_;
      }

      public @Nullable PostChain getOrLoadPostChain(Identifier p_456111_, Set<Identifier> p_368742_) throws ShaderManager.CompilationException {
         Optional<PostChain> optional = this.postChains.get(p_456111_);
         if (optional != null) {
            return optional.orElse(null);
         }

         PostChain postchain = this.loadPostChain(p_456111_, p_368742_);
         this.postChains.put(p_456111_, Optional.of(postchain));
         return postchain;
      }

      private PostChain loadPostChain(Identifier p_457690_, Set<Identifier> p_366419_) throws ShaderManager.CompilationException {
         PostChainConfig postchainconfig = this.configs.postChains.get(p_457690_);
         if (postchainconfig == null) {
            throw new ShaderManager.CompilationException("Could not find post chain with id: " + p_457690_);
         } else {
            return PostChain.load(postchainconfig, ShaderManager.this.textureManager, p_366419_, p_457690_, ShaderManager.this.postChainProjectionMatrixBuffer);
         }
      }

      @Override
      public void close() {
         this.postChains.values().forEach(p_407287_ -> p_407287_.ifPresent(PostChain::close));
         this.postChains.clear();
      }

      public @Nullable String getShaderSource(Identifier p_454193_, ShaderType p_392413_) {
         return this.configs.shaderSources.get(new ShaderManager.ShaderSourceKey(p_454193_, p_392413_));
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class CompilationException extends Exception {
      public CompilationException(String p_366142_) {
         super(p_366142_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record Configs(Map<ShaderManager.ShaderSourceKey, String> shaderSources, Map<Identifier, PostChainConfig> postChains) {
      public static final ShaderManager.Configs EMPTY = new ShaderManager.Configs(Map.of(), Map.of());
   }

   @OnlyIn(Dist.CLIENT)
   record ShaderSourceKey(Identifier id, ShaderType type) {
      @Override
      public String toString() {
         return this.id + " (" + this.type + ")";
      }
   }
}
