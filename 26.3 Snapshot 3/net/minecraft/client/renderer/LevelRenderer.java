package net.minecraft.client.renderer;

import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Options;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.chunk.TranslucencyPointOfView;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.oit.OitRenderPassProvider;
import net.minecraft.client.renderer.oit.OitStage;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.OptionsRenderState;
import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SectionUpdateRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.SimpleGizmoCollector;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;

public class LevelRenderer implements AutoCloseable {
   public static final int OIT_WAVELET_RANK = 2;
   public static final int OIT_COEFFICIENT_COUNT = Math.powExact(2, 3);
   public static final int OIT_TRANSMITTANCE_TARGET_COUNT = OIT_COEFFICIENT_COUNT / 4;
   private static final Identifier ENTITY_OUTLINE_POST_CHAIN_ID = Identifier.withDefaultNamespace("entity_outline");
   private static final int MINIMUM_TRANSPARENT_SORT_COUNT = 15;
   private static final float CHUNK_VISIBILITY_THRESHOLD = 0.3F;
   private static final Vector4fc DEPTH_BOUNDS_CLEAR_COLOR = new Vector4f(-Float.MAX_VALUE, 0.0F, 0.0F, 0.0F);
   private static final Vector4fc ZERO_CLEAR_COLOR = new Vector4f(0.0F);
   private final GameRenderer gameRenderer;
   private final EntityRenderDispatcher entityRenderDispatcher;
   private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
   private final RenderBuffers renderBuffers;
   private final FeatureRenderDispatcher featureRenderDispatcher;
   private final SubmitNodeStorage submitNodeStorage = new SubmitNodeStorage();
   private final ModelManager modelManager;
   private final TextureManager textureManager;
   private final AtlasManager atlasManager;
   private final ShaderManager shaderManager;
   private final LevelRenderState levelRenderState;
   private final OptionsRenderState optionsRenderState;
   private @Nullable SkyRenderer skyRenderer;
   private final CloudRenderer cloudRenderer = new CloudRenderer();
   private final WorldBorderRenderer worldBorderRenderer = new WorldBorderRenderer();
   private final WeatherEffectRenderer weatherEffectRenderer = new WeatherEffectRenderer();
   private final SectionOcclusionGraph sectionOcclusionGraph = new SectionOcclusionGraph();
   private final ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections = new ObjectArrayList(10000);
   private final ObjectArrayList<SectionRenderDispatcher.RenderSection> nearbyVisibleSections = new ObjectArrayList(50);
   private @Nullable ViewArea viewArea;
   private final RenderTarget entityOutlineTarget;
   private final LevelTargetBundle targets = new LevelTargetBundle();
   private @Nullable SectionRenderDispatcher sectionRenderDispatcher;
   private @Nullable BlockPos lastTranslucentSortBlockPos;
   private int translucencyResortIterationIndex;
   private @Nullable GpuSampler chunkLayerSampler;
   private boolean currentFrameRendersEntityOutline;
   private final SimpleGizmoCollector renderThreadGizmos = new SimpleGizmoCollector();
   private LevelRenderer.FinalizedGizmos finalizedGizmos = new LevelRenderer.FinalizedGizmos(new DrawableGizmoPrimitives(), new DrawableGizmoPrimitives());

   public LevelRenderer(
      final EntityRenderDispatcher entityRenderDispatcher,
      final BlockEntityRenderDispatcher blockEntityRenderDispatcher,
      final ModelManager modelManager,
      final TextureManager textureManager,
      final AtlasManager atlasManager,
      final ShaderManager shaderManager,
      final GameRenderer gameRenderer,
      final int width,
      final int height
   ) {
      this.gameRenderer = gameRenderer;
      this.entityRenderDispatcher = entityRenderDispatcher;
      this.blockEntityRenderDispatcher = blockEntityRenderDispatcher;
      this.renderBuffers = gameRenderer.renderBuffers();
      this.featureRenderDispatcher = gameRenderer.featureRenderDispatcher();
      this.modelManager = modelManager;
      this.textureManager = textureManager;
      this.atlasManager = atlasManager;
      this.shaderManager = shaderManager;
      this.levelRenderState = gameRenderer.gameRenderState().levelRenderState;
      this.optionsRenderState = gameRenderer.gameRenderState().optionsRenderState;
      this.entityOutlineTarget = new TextureTarget("Entity Outline", width, height, GpuFormat.RGBA8_UNORM, null);
   }

   public void render(
      final GraphicsResourceAllocator resourceAllocator,
      final DeltaTracker deltaTracker,
      final boolean renderOutline,
      final CameraRenderState cameraState,
      final Matrix4fc modelViewMatrix,
      final GpuBufferSlice terrainFog,
      final Vector4f fogColor,
      final boolean shouldRenderSky
   ) {
      RenderSystem.isRenderingLevel = true;
      float deltaPartialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
      final ProfilerFiller profiler = Profiler.get();
      this.submitNodeStorage.setUseImprovedTransparency(this.gameRenderer.useImprovedTransparency());
      profiler.push("repositionCamera");
      this.repositionCamera(cameraState);
      Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
      modelViewStack.pushMatrix();
      modelViewStack.mul(modelViewMatrix);
      profiler.popPush("submitFeatures");
      this.submitFeatures(this.levelRenderState, this.submitNodeStorage, renderOutline);
      profiler.popPush("prepareFeatures");
      FeatureRenderDispatcher.PreparedFrame featureFrame = this.featureRenderDispatcher.prepareFrame(this.submitNodeStorage);
      this.currentFrameRendersEntityOutline = featureFrame.hasAnyOutline() && this.levelRenderState.shouldShowEntityOutlines;
      profiler.popPush("setupFrameGraph");
      FrameGraphBuilder frame = new FrameGraphBuilder();
      this.targets.main = frame.importExternal("main", this.gameRenderer.mainRenderTarget());
      int screenWidth = this.gameRenderer.mainRenderTarget().width;
      int screenHeight = this.gameRenderer.mainRenderTarget().height;
      if (this.gameRenderer.useImprovedTransparency()) {
         RenderTargetDescriptor depthBoundsTargetDescriptor = new RenderTargetDescriptor(
            screenWidth, screenHeight, new RenderTargetDescriptor.TextureProperties(DEPTH_BOUNDS_CLEAR_COLOR, GpuFormat.RGBA32_FLOAT), null
         );
         this.targets.depthBounds = frame.createInternal("depth_bounds", depthBoundsTargetDescriptor);
         RenderTargetDescriptor transmittanceTargetDescriptor = new RenderTargetDescriptor(
            screenWidth, screenHeight, new RenderTargetDescriptor.TextureProperties(ZERO_CLEAR_COLOR, GpuFormat.RGBA16_FLOAT), null
         );

         for (int i = 0; i < OIT_TRANSMITTANCE_TARGET_COUNT; i++) {
            this.targets.transmittance.set(i, frame.createInternal("transmittance", transmittanceTargetDescriptor));
         }

         RenderTargetDescriptor accumulateTargetDescriptor = new RenderTargetDescriptor(
            screenWidth, screenHeight, new RenderTargetDescriptor.TextureProperties(ZERO_CLEAR_COLOR, GpuFormat.RGBA16_FLOAT), null
         );
         this.targets.accumulate = frame.createInternal("accumulate", accumulateTargetDescriptor);
         RenderTargetDescriptor extraDepthTargetDescriptor = new RenderTargetDescriptor(
            screenWidth, screenHeight, null, RenderTargetDescriptor.TextureProperties.DEFAULT_DEPTH
         );
         this.targets.oitCloudDepth = frame.createInternal("cloud_depth", extraDepthTargetDescriptor);
         this.targets.oitTerrainWithWaterPatchDepth = frame.createInternal("terrain_depth", extraDepthTargetDescriptor);
      }

      this.targets.entityOutline = frame.importExternal("entity_outline", this.entityOutlineTarget);
      FramePass clearPass = frame.addPass("clear");
      this.targets.main = clearPass.readsAndWrites(this.targets.main);
      clearPass.executes(
         () -> {
            RenderTarget mainRenderTarget = this.gameRenderer.mainRenderTarget();
            RenderSystem.getDevice()
               .createCommandEncoder()
               .clearColorAndDepthTextures(
                  mainRenderTarget.getColorTexture(), new Vector4f(fogColor.x, fogColor.y, fogColor.z, 0.0F), mainRenderTarget.getDepthTexture(), 0.0
               );
         }
      );
      if (shouldRenderSky) {
         this.addSkyPass(frame, cameraState, terrainFog);
      }

      ChunkSectionsToRender chunkSectionsToRender = this.prepareChunkRenders(this.levelRenderState.cameraRenderState.viewRotationMatrix);
      this.addMainPass(frame, featureFrame, terrainFog, chunkSectionsToRender, deltaPartialTick);
      PostChain entityOutlineChain = this.shaderManager.getPostChain(ENTITY_OUTLINE_POST_CHAIN_ID, LevelTargetBundle.OUTLINE_TARGETS);
      if (this.currentFrameRendersEntityOutline && entityOutlineChain != null) {
         entityOutlineChain.addToFrame(frame, screenWidth, screenHeight, this.targets);
      }

      profiler.popPush("executeFrameGraph");
      frame.execute(resourceAllocator, new FrameGraphBuilder.Inspector() {
         @Override
         public void beforeExecutePass(final String name) {
            profiler.push(name);
         }

         @Override
         public void afterExecutePass(final String name) {
            profiler.pop();
         }
      });
      profiler.pop();
      this.targets.clear();
      modelViewStack.popMatrix();
      featureFrame.close();
      profiler.push("compileSections");
      this.compileSections(cameraState);
      profiler.pop();
      if (this.sectionRenderDispatcher != null) {
         this.sectionRenderDispatcher.lock();
         profiler.push("uploadTerrainBuffers");

         try {
            this.sectionRenderDispatcher.uploadTerrainBuffersToGpu();
         } finally {
            this.sectionRenderDispatcher.unlock();
         }

         profiler.pop();
      }

      profiler.push("updateSectionOcclusion");
      this.sectionOcclusionGraph.update(cameraState, this.optionsRenderState.fov, this.levelRenderState.chunkLoadingRenderState);
      profiler.pop();
      Runnable playerCompiledSectionCallback = this.levelRenderState.playerCompiledSectionCallback;
      if (playerCompiledSectionCallback != null && this.isSectionCompiledAndVisible(this.levelRenderState.cameraRenderState.blockPos)) {
         playerCompiledSectionCallback.run();
      }

      RenderSystem.isRenderingLevel = false;
   }

   private void submitFeatures(final LevelRenderState levelRenderState, final SubmitNodeCollector submitNodeCollector, final boolean renderOutline) {
      PoseStack poseStack = new PoseStack();
      this.submitEntities(poseStack, levelRenderState, submitNodeCollector);
      levelRenderState.entityRenderStates.clear();
      this.submitBlockEntities(poseStack, levelRenderState, submitNodeCollector);
      levelRenderState.blockEntityRenderStates.clear();
      this.submitBlockDestroyAnimation(poseStack, submitNodeCollector, levelRenderState);
      levelRenderState.blockBreakingRenderStates.clear();
      levelRenderState.particlesRenderState.submit(submitNodeCollector, levelRenderState.cameraRenderState);
      if (renderOutline) {
         this.submitBlockOutline(poseStack, this.submitNodeStorage, levelRenderState);
      }

      this.finalizeGizmoCollection();
      this.finalizedGizmos.standardPrimitives().submit(submitNodeCollector, levelRenderState.cameraRenderState, false);
      this.finalizedGizmos.alwaysOnTopPrimitives().submit(submitNodeCollector, levelRenderState.cameraRenderState, true);
      if (!levelRenderState.shouldShowEntityOutlines) {
         ObjectIterator var5 = this.submitNodeStorage.getSubmitsPerOrder().values().iterator();

         while (var5.hasNext()) {
            SubmitNodeCollection collection = (SubmitNodeCollection)var5.next();
            collection.outline.clear();
         }
      }

      this.checkPoseStack(poseStack);
   }

   private void repositionCamera(final CameraRenderState camera) {
      Vec3 cameraPos = camera.pos;
      SectionPos cameraSectionPos = SectionPos.of(cameraPos);
      if (this.viewArea.repositionCamera(cameraSectionPos)) {
         this.worldBorderRenderer.invalidate();
      }

      this.sectionRenderDispatcher.setCameraPosition(cameraPos);
   }

   private void addSkyPass(final FrameGraphBuilder frame, final CameraRenderState cameraState, final GpuBufferSlice skyFog) {
      FogType fogType = cameraState.fogType;
      if (fogType != FogType.POWDER_SNOW && fogType != FogType.LAVA && !cameraState.entityRenderState.doesMobEffectBlockSky) {
         if (this.levelRenderState.shouldResetSkyRenderer || this.skyRenderer == null) {
            if (this.skyRenderer != null) {
               this.skyRenderer.close();
            }

            this.skyRenderer = new SkyRenderer(this.textureManager, this.atlasManager, this.gameRenderer.mainRenderTarget());
         }

         SkyRenderState state = this.levelRenderState.skyRenderState;
         if (state.skybox != DimensionType.Skybox.NONE) {
            FramePass pass = frame.addPass("sky");
            this.targets.main = pass.readsAndWrites(this.targets.main);
            pass.executes(
               () -> {
                  RenderSystem.setShaderFog(skyFog);
                  if (state.skybox == DimensionType.Skybox.END) {
                     this.skyRenderer.renderEndSky();
                     if (state.endFlashIntensity > 1.0E-5F) {
                        PoseStack poseStack = new PoseStack();
                        this.skyRenderer.renderEndFlash(poseStack, state.endFlashIntensity, state.endFlashXAngle, state.endFlashYAngle);
                     }
                  } else {
                     PoseStack poseStack = new PoseStack();
                     this.skyRenderer.renderSkyDisc(state.skyColor);
                     this.skyRenderer.renderSunriseAndSunset(poseStack, state.sunAngle, state.sunriseAndSunsetColor);
                     this.skyRenderer
                        .renderSunMoonAndStars(
                           poseStack, state.sunAngle, state.moonAngle, state.starAngle, state.moonPhase, state.rainBrightness, state.starBrightness
                        );
                     if (state.shouldRenderDarkDisc) {
                        this.skyRenderer.renderDarkDisc();
                     }
                  }
               }
            );
         }
      }
   }

   private void addMainPass(
      final FrameGraphBuilder frame,
      final FeatureRenderDispatcher.PreparedFrame featureFrame,
      final GpuBufferSlice terrainFog,
      final ChunkSectionsToRender chunkSectionsToRender,
      final float deltaPartialTick
   ) {
      FramePass pass = frame.addPass("main");
      this.targets.main = pass.readsAndWrites(this.targets.main);
      boolean useImprovedTransparency = this.gameRenderer.useImprovedTransparency();
      if (useImprovedTransparency) {
         this.targets.depthBounds = pass.readsAndWrites(this.targets.depthBounds);

         for (int i = 0; i < OIT_TRANSMITTANCE_TARGET_COUNT; i++) {
            this.targets.transmittance.set(i, pass.readsAndWrites(this.targets.transmittance.get(i)));
         }

         this.targets.accumulate = pass.readsAndWrites(this.targets.accumulate);
         if (this.optionsRenderState.cloudStatus != CloudStatus.OFF && ARGB.alpha(this.levelRenderState.cloudColor) > 0) {
            this.targets.oitCloudDepth = pass.readsAndWrites(this.targets.oitCloudDepth);
         }

         if (featureFrame.hasAnyWaterMask()) {
            this.targets.oitTerrainWithWaterPatchDepth = pass.readsAndWrites(this.targets.oitTerrainWithWaterPatchDepth);
         }
      }

      if (this.currentFrameRendersEntityOutline && this.targets.entityOutline != null) {
         this.targets.entityOutline = pass.readsAndWrites(this.targets.entityOutline);
      }

      pass.executes(
         () -> {
            RenderSystem.setShaderFog(terrainFog);
            if (this.levelRenderState.shouldResetChunkLayerSampler || this.chunkLayerSampler == null) {
               if (this.chunkLayerSampler != null) {
                  this.chunkLayerSampler.close();
               }

               int maxAnisotropy = this.optionsRenderState.textureFiltering == TextureFilteringMethod.ANISOTROPIC
                  ? this.optionsRenderState.maxAnisotropyValue
                  : 1;
               this.chunkLayerSampler = RenderSystem.getDevice()
                  .createSampler(
                     AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, maxAnisotropy, OptionalDouble.empty()
                  );
            }

            this.prepareTranslucents(deltaPartialTick);
            this.gameRenderer.lighting().setupFor(Lighting.Entry.LEVEL);
            RenderTarget mainTarget = this.targets.main.get();

            try (RenderPass renderPass = RenderSystem.getDevice()
                  .createCommandEncoder()
                  .createRenderPass(
                     () -> useImprovedTransparency ? "Solid" : "Main",
                     mainTarget.getColorTextureView(),
                     Optional.empty(),
                     mainTarget.getDepthTextureView(),
                     OptionalDouble.empty()
                  )) {
               RenderSystem.bindDefaultUniforms(renderPass);
               this.executeSolid(chunkSectionsToRender, featureFrame, renderPass);
               if (!useImprovedTransparency) {
                  this.executeClassicTransparency(chunkSectionsToRender, featureFrame, renderPass);
               }
            }

            if (useImprovedTransparency) {
               this.executeOit(chunkSectionsToRender, featureFrame);
            }

            this.executeOutline(featureFrame);
            if (featureFrame.hasAnyAlwaysOnTop()) {
               PoseStack poseStack = new PoseStack();
               if (!this.finalizedGizmos.alwaysOnTopPrimitives().isEmpty()) {
                  try (RenderPass renderPass = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(
                           () -> "Always on top features",
                           mainTarget.getColorTextureView(),
                           Optional.empty(),
                           mainTarget.getDepthTextureView(),
                           OptionalDouble.of(0.0)
                        )) {
                     RenderSystem.bindDefaultUniforms(renderPass);
                     featureFrame.executeAlwaysOnTop(renderPass);
                  }
               }

               this.checkPoseStack(poseStack);
            }
         }
      );
   }

   private void executeSolid(
      final ChunkSectionsToRender chunkSectionsToRender, final FeatureRenderDispatcher.PreparedFrame featureFrame, final RenderPass renderPass
   ) {
      ProfilerFiller profiler = Profiler.get();
      profiler.push("solidTerrain");
      chunkSectionsToRender.renderGroup(ChunkSectionLayerGroup.OPAQUE, renderPass, this.chunkLayerSampler, this.levelRenderState.renderWireframeTerrain);
      profiler.popPush("renderSolidFeatures");
      featureFrame.executeSolid(renderPass);
      profiler.pop();
   }

   private void prepareTranslucents(final float deltaPartialTick) {
      CloudStatus cloudStatus = this.optionsRenderState.cloudStatus;
      boolean shouldRenderClouds = cloudStatus != CloudStatus.OFF && ARGB.alpha(this.levelRenderState.cloudColor) > 0;
      if (shouldRenderClouds) {
         this.cloudRenderer
            .prepare(
               this.levelRenderState.cloudColor,
               cloudStatus,
               this.levelRenderState.cloudHeight,
               this.optionsRenderState.cloudRange,
               this.levelRenderState.cameraRenderState.pos,
               this.levelRenderState.gameTime,
               deltaPartialTick
            );
      }

      int renderDistance = this.optionsRenderState.renderDistance * 16;
      CameraRenderState cameraState = this.levelRenderState.cameraRenderState;
      this.worldBorderRenderer
         .prepare(this.levelRenderState.worldBorderRenderState, cameraState.pos, renderDistance, this.levelRenderState.cameraRenderState.depthFar);
      this.weatherEffectRenderer.prepare(cameraState.pos, this.levelRenderState.weatherRenderState);
      RenderSystem.resizeAllAutoStorageIndexBuffers();
   }

   private void executeOit(final ChunkSectionsToRender chunkSectionsToRender, final FeatureRenderDispatcher.PreparedFrame featureFrame) {
      boolean frameHasWaterMask = featureFrame.hasAnyWaterMask();
      CloudStatus cloudStatus = this.optionsRenderState.cloudStatus;
      boolean shouldRenderClouds = cloudStatus != CloudStatus.OFF && ARGB.alpha(this.levelRenderState.cloudColor) > 0;
      int renderDistance = this.optionsRenderState.renderDistance * 16;
      CameraRenderState cameraState = this.levelRenderState.cameraRenderState;
      RenderTarget depthBoundsTarget = OutputTarget.DEPTH_BOUNDS_TARGET.getRenderTarget();
      RenderTarget accumulateTarget = OutputTarget.ACCUMULATE_TARGET.getRenderTarget();
      GpuTextureView depthTextureView = this.gameRenderer.mainRenderTarget().getDepthTextureView();
      RenderTarget mainTarget = this.targets.main.get();
      if (frameHasWaterMask) {
         this.executeOitWaterMask(featureFrame, mainTarget);
      }

      if (shouldRenderClouds) {
         this.targets.oitCloudDepth.get().copyDepthFrom(mainTarget);
      }

      GpuTextureView depthBoundsTargetView = depthBoundsTarget.getColorTextureView();
      GpuTextureView accumulateTargetView = accumulateTarget.getColorTextureView();
      OitRenderPassProvider.Parameters params = new OitRenderPassProvider.Parameters(depthBoundsTargetView, accumulateTargetView, depthTextureView);
      OitRenderPassProvider.Parameters cloudParams;
      if (shouldRenderClouds) {
         GpuTextureView cloudDepthTextureView = this.targets.oitCloudDepth.get().getDepthTextureView();
         cloudParams = new OitRenderPassProvider.Parameters(depthBoundsTargetView, accumulateTargetView, cloudDepthTextureView);
      } else {
         cloudParams = null;
      }

      OitRenderPassProvider.Parameters terrainParams;
      if (frameHasWaterMask) {
         GpuTextureView terrainDepthTextureView = this.targets.oitTerrainWithWaterPatchDepth.get().getDepthTextureView();
         terrainParams = new OitRenderPassProvider.Parameters(depthBoundsTargetView, accumulateTargetView, terrainDepthTextureView);
      } else {
         terrainParams = params;
      }

      for (OitStage stage : OitStage.values()) {
         chunkSectionsToRender.renderOit(this.chunkLayerSampler, stage, terrainParams, this.gameRenderer.lightmap());
         if (shouldRenderClouds) {
            this.cloudRenderer.renderOit(cloudStatus, stage, cloudParams);
         }

         try (RenderPass renderPass = OitRenderPassProvider.createRenderPass(stage, () -> "Features, World Border, Weather", params)) {
            featureFrame.executeOit(stage, renderPass);
            this.worldBorderRenderer.renderOit(this.levelRenderState.worldBorderRenderState, cameraState.pos, renderDistance, stage, renderPass);
            this.weatherEffectRenderer.renderOit(stage, this.levelRenderState.weatherRenderState, renderPass);
         }
      }

      GpuSampler nearestSampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
      RenderTarget renderTarget = this.gameRenderer.mainRenderTarget();

      try (RenderPass renderPass = RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(
               () -> "OIT Composite", renderTarget.getColorTextureView(), Optional.empty(), renderTarget.getDepthTextureView(), OptionalDouble.empty()
            )) {
         RenderSystem.bindDefaultUniforms(renderPass);
         renderPass.bindTexture("Sampler0", accumulateTargetView, nearestSampler);

         for (int i = 0; i < OIT_TRANSMITTANCE_TARGET_COUNT; i++) {
            renderPass.bindTexture("Coeff" + i, OutputTarget.TRANSMITTANCE_TARGETS[i].getRenderTarget().getColorTextureView(), nearestSampler);
         }

         renderPass.bindTexture("DepthBoundsSampler", depthBoundsTargetView, nearestSampler);
         renderPass.setPipeline(RenderSystem.getCompiledPipeline(RenderPipelines.OIT_COMPOSITE));
         renderPass.draw(3, 1, 0, 0);
      }
   }

   private void executeOitWaterMask(final FeatureRenderDispatcher.PreparedFrame featureFrame, final RenderTarget mainTarget) {
      RenderTarget terrainTarget = this.targets.oitTerrainWithWaterPatchDepth.get();
      terrainTarget.copyDepthFrom(mainTarget);
      RenderPassDescriptor descriptor = RenderPassDescriptor.builder(() -> "Water mask").withDepthAttachment(terrainTarget.getDepthTextureView()).build();

      try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(descriptor)) {
         RenderSystem.bindDefaultUniforms(renderPass);
         featureFrame.executeWaterMask(renderPass);
      }
   }

   private void executeClassicTransparency(
      final ChunkSectionsToRender chunkSectionsToRender, final FeatureRenderDispatcher.PreparedFrame featureFrame, final RenderPass renderPass
   ) {
      ProfilerFiller profiler = Profiler.get();
      CloudStatus cloudStatus = this.optionsRenderState.cloudStatus;
      boolean shouldRenderClouds = cloudStatus != CloudStatus.OFF && ARGB.alpha(this.levelRenderState.cloudColor) > 0;
      int renderDistance = this.optionsRenderState.renderDistance * 16;
      profiler.push("renderTranslucentFeatures");
      featureFrame.executeTranslucent(renderPass);
      profiler.pop();
      profiler.push("translucentTerrain");
      chunkSectionsToRender.renderGroup(ChunkSectionLayerGroup.TRANSLUCENT, renderPass, this.chunkLayerSampler, this.levelRenderState.renderWireframeTerrain);
      profiler.pop();
      featureFrame.executeTranslucentAfterTerrain(renderPass);
      if (shouldRenderClouds) {
         this.cloudRenderer.render(cloudStatus, renderPass);
      }

      Vec3 cameraPos = this.levelRenderState.cameraRenderState.pos;
      this.weatherEffectRenderer.render(this.levelRenderState.weatherRenderState, renderPass);
      this.worldBorderRenderer.render(this.levelRenderState.worldBorderRenderState, renderPass, cameraPos, renderDistance);
   }

   private void executeOutline(final FeatureRenderDispatcher.PreparedFrame featureFrame) {
      if (this.currentFrameRendersEntityOutline) {
         try (RenderPass renderPass = RenderSystem.getDevice()
               .createCommandEncoder()
               .createRenderPass(() -> "Outline", this.entityOutlineTarget.getColorTextureView(), Optional.of(ZERO_CLEAR_COLOR), null, OptionalDouble.empty())) {
            RenderSystem.bindDefaultUniforms(renderPass);
            featureFrame.executeOutline(renderPass);
         }
      }
   }

   public ChunkSectionsToRender prepareChunkRenders(final Matrix4fc modelViewMatrix) {
      ObjectListIterator<SectionRenderDispatcher.RenderSection> iterator = this.visibleSections.listIterator(0);
      EnumMap<ChunkSectionLayer, Int2ObjectOpenHashMap<List<RenderPass.Draw<GpuBufferSlice[]>>>> drawGroups = new EnumMap<>(ChunkSectionLayer.class);
      int largestIndexCount = 0;

      for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
         drawGroups.put(layer, new Int2ObjectOpenHashMap());
      }

      List<DynamicUniforms.ChunkSectionInfo> sectionInfos = new ArrayList<>();
      GpuTextureView blockAtlas = this.textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
      int textureAtlasWidth = blockAtlas.getWidth(0);
      int textureAtlasHeight = blockAtlas.getHeight(0);
      if (this.sectionRenderDispatcher != null) {
         this.sectionRenderDispatcher.lock();

         try {
            while (iterator.hasNext()) {
               SectionRenderDispatcher.RenderSection section = (SectionRenderDispatcher.RenderSection)iterator.next();
               SectionMesh sectionMesh = section.getSectionMesh();
               BlockPos renderOffset = section.getRenderOrigin();
               long now = Util.getMillis();
               int uboIndex = -1;

               for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                  SectionMesh.SectionDraw draw = sectionMesh.getSectionDraw(layer);
                  SectionRenderDispatcher.RenderSectionBufferSlice slice = this.sectionRenderDispatcher.getRenderSectionSlice(sectionMesh, layer);
                  if (slice != null && draw != null && (!draw.hasCustomIndexBuffer() || slice.indexBuffer() != null)) {
                     if (uboIndex == -1) {
                        uboIndex = sectionInfos.size();
                        sectionInfos.add(
                           new DynamicUniforms.ChunkSectionInfo(
                              new Matrix4f(modelViewMatrix),
                              renderOffset.getX(),
                              renderOffset.getY(),
                              renderOffset.getZ(),
                              section.getVisibility(now),
                              textureAtlasWidth,
                              textureAtlasHeight
                           )
                        );
                     }

                     int combinedHash = 173;
                     VertexFormat vertexFormat = layer.pipeline().getVertexFormatBinding(0);
                     GpuBuffer vertexBuffer = slice.vertexBuffer();
                     if (layer != ChunkSectionLayer.TRANSLUCENT) {
                        combinedHash = 31 * combinedHash + vertexBuffer.hashCode();
                     }

                     int firstIndex = 0;
                     GpuBuffer indexBuffer;
                     IndexType indexType;
                     if (!draw.hasCustomIndexBuffer()) {
                        if (draw.indexCount() > largestIndexCount) {
                           largestIndexCount = draw.indexCount();
                        }

                        indexBuffer = null;
                        indexType = null;
                     } else {
                        indexBuffer = slice.indexBuffer();
                        indexType = draw.indexType();
                        if (layer != ChunkSectionLayer.TRANSLUCENT) {
                           combinedHash = 31 * combinedHash + indexBuffer.hashCode();
                           combinedHash = 31 * combinedHash + indexType.hashCode();
                        }

                        firstIndex = (int)(slice.indexBufferOffset() / indexType.bytes);
                     }

                     int finalUboIndex = uboIndex;
                     int baseVertex = (int)(slice.vertexBufferOffset() / vertexFormat.getVertexSize());
                     List<RenderPass.Draw<GpuBufferSlice[]>> draws = (List<RenderPass.Draw<GpuBufferSlice[]>>)drawGroups.get(layer)
                        .computeIfAbsent(combinedHash, var0 -> new ArrayList());
                     draws.add(
                        new RenderPass.Draw<>(
                           0,
                           vertexBuffer,
                           indexBuffer,
                           indexType,
                           firstIndex,
                           draw.indexCount(),
                           baseVertex,
                           (sectionUbos, uploader) -> uploader.upload("ChunkSection", sectionUbos[finalUboIndex])
                        )
                     );
                  }
               }
            }
         } finally {
            this.sectionRenderDispatcher.unlock();
         }
      }

      GpuBufferSlice[] chunkSectionInfos = RenderSystem.getDynamicUniforms().writeChunkSections(sectionInfos.toArray(new DynamicUniforms.ChunkSectionInfo[0]));
      RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
      if (largestIndexCount != 0) {
         autoIndices.requestIndexCount(largestIndexCount);
      }

      return new ChunkSectionsToRender(blockAtlas, drawGroups, largestIndexCount, chunkSectionInfos);
   }

   private void compileSections(final CameraRenderState camera) {
      ProfilerFiller profiler = Profiler.get();
      profiler.push("populateSectionsToCompile");
      BlockPos cameraPosition = camera.blockPos;
      long fadeDuration = Mth.floor(this.optionsRenderState.chunkSectionFadeInTime * 1000.0);

      for (SectionUpdateRenderState state : this.levelRenderState.sectionUpdateRenderStates) {
         BlockPos center = SectionPos.of(state.sectionNode()).center();
         double distSqr = center.distSqr(cameraPosition);
         boolean isNearby = distSqr < 768.0;
         boolean rebuildSync = false;
         if (this.optionsRenderState.prioritizeChunkUpdates == PrioritizeChunkUpdates.NEARBY) {
            rebuildSync = isNearby || state.playerChanged();
         } else if (this.optionsRenderState.prioritizeChunkUpdates == PrioritizeChunkUpdates.PLAYER_AFFECTED) {
            rebuildSync = state.playerChanged();
         }

         SectionRenderDispatcher.RenderSection section = this.viewArea.getRenderSection(state.sectionNode());
         if (!isNearby && !section.wasPreviouslyEmpty()) {
            section.setFadeDuration(fadeDuration);
         } else {
            section.setFadeDuration(0L);
         }

         section.setWasPreviouslyEmpty(false);
         if (rebuildSync) {
            profiler.push("compileSectionSynchronously");
            section.compileSync(state.region());
            profiler.pop();
         } else {
            section.compileAsync(state.region());
         }
      }

      profiler.popPush("scheduleTranslucentResort");
      this.scheduleTranslucentSectionResort(camera.pos);
      profiler.pop();
   }

   private void checkPoseStack(final PoseStack poseStack) {
      if (!poseStack.isEmpty()) {
         throw new IllegalStateException("Pose stack not empty");
      }
   }

   private void submitEntities(final PoseStack poseStack, final LevelRenderState levelRenderState, final SubmitNodeCollector output) {
      Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
      double camX = cameraPos.x();
      double camY = cameraPos.y();
      double camZ = cameraPos.z();

      for (EntityRenderState state : levelRenderState.entityRenderStates) {
         this.entityRenderDispatcher.submit(state, levelRenderState.cameraRenderState, state.x - camX, state.y - camY, state.z - camZ, poseStack, output);
      }
   }

   private void submitBlockEntities(final PoseStack poseStack, final LevelRenderState levelRenderState, final SubmitNodeCollector submitNodeCollector) {
      Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
      double camX = cameraPos.x();
      double camY = cameraPos.y();
      double camZ = cameraPos.z();

      for (BlockEntityRenderState renderState : levelRenderState.blockEntityRenderStates) {
         BlockPos blockPos = renderState.blockPos;
         poseStack.pushPose();
         poseStack.translate(blockPos.getX() - camX, blockPos.getY() - camY, blockPos.getZ() - camZ);
         this.blockEntityRenderDispatcher.submit(renderState, poseStack, submitNodeCollector, levelRenderState.cameraRenderState);
         poseStack.popPose();
      }
   }

   private void submitBlockDestroyAnimation(final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final LevelRenderState levelRenderState) {
      if (!levelRenderState.blockBreakingRenderStates.isEmpty()) {
         Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
         double camX = cameraPos.x();
         double camY = cameraPos.y();
         double camZ = cameraPos.z();
         List<BlockStateModelPart> parts = new ArrayList<>();
         RandomSource random = RandomSource.createThreadLocalInstance();

         for (BlockBreakingRenderState state : levelRenderState.blockBreakingRenderStates) {
            if (state.blockState().getRenderShape() == RenderShape.MODEL) {
               BlockPos pos = state.blockPos();
               poseStack.pushPose();
               poseStack.translate(pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ);
               poseStack.translate(state.blockState().getOffset(pos));
               BlockStateModel model = this.modelManager.getBlockStateModelSet().get(state.blockState());
               random.setSeed(state.blockState().getSeed(pos));
               model.collectParts(random, parts);
               submitNodeCollector.submitBreakingBlockModel(poseStack, List.copyOf(parts), state.progress(), model.hasMaterialFlag(1));
               parts.clear();
               poseStack.popPose();
            }
         }
      }
   }

   private void submitBlockOutline(final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final LevelRenderState levelRenderState) {
      BlockOutlineRenderState state = levelRenderState.blockOutlineRenderState;
      if (state != null) {
         Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
         BlockPos pos = state.pos();
         poseStack.pushPose();
         poseStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
         if (state.highContrast()) {
            this.submitHitOutline(poseStack, submitNodeCollector, RenderTypes.secondaryBlockOutline(), state, -16777216, 7.0F, state.isTranslucent());
         }

         int outlineColor = state.highContrast() ? -11010079 : ARGB.black(102);
         this.submitHitOutline(
            poseStack,
            submitNodeCollector,
            state.highContrast() ? RenderTypes.linesDepthBias() : RenderTypes.linesTranslucent(),
            state,
            outlineColor,
            this.gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth,
            state.isTranslucent()
         );
         poseStack.popPose();
      }
   }

   private void submitHitOutline(
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final RenderType renderType,
      final BlockOutlineRenderState state,
      final int color,
      final float width,
      final boolean afterTerrain
   ) {
      if (SharedConstants.DEBUG_SHAPES) {
         submitNodeCollector.submitShapeOutline(poseStack, state.shape(), renderType, -1, width, afterTerrain);
         if (state.collisionShape() != null) {
            submitNodeCollector.submitShapeOutline(
               poseStack, state.collisionShape(), renderType, ARGB.colorFromFloat(0.4F, 0.0F, 0.0F, 0.0F), width, afterTerrain
            );
         }

         if (state.occlusionShape() != null) {
            submitNodeCollector.submitShapeOutline(
               poseStack, state.occlusionShape(), renderType, ARGB.colorFromFloat(0.4F, 0.0F, 1.0F, 0.0F), width, afterTerrain
            );
         }

         if (state.interactionShape() != null) {
            submitNodeCollector.submitShapeOutline(
               poseStack, state.interactionShape(), renderType, ARGB.colorFromFloat(0.4F, 0.0F, 0.0F, 1.0F), width, afterTerrain
            );
         }
      } else {
         submitNodeCollector.submitShapeOutline(poseStack, state.shape(), renderType, color, width, afterTerrain);
      }
   }

   public void resize(final int width, final int height) {
      this.sectionOcclusionGraph.invalidate();
      this.entityOutlineTarget.resize(width, height);
   }

   public void endFrame() {
      this.cloudRenderer.endFrame();
   }

   @Override
   public void close() {
      this.resetLevelRenderData();
      this.entityOutlineTarget.destroyBuffers();
      if (this.skyRenderer != null) {
         this.skyRenderer.close();
      }

      if (this.chunkLayerSampler != null) {
         this.chunkLayerSampler.close();
      }

      this.worldBorderRenderer.close();
      this.cloudRenderer.close();
      this.weatherEffectRenderer.close();
   }

   public void blitEntityOutline() {
      if (this.currentFrameRendersEntityOutline) {
         this.entityOutlineTarget
            .blitAndBlendToTexture(this.gameRenderer.mainRenderTarget().getColorTextureView(), this.gameRenderer.mainRenderTarget().getDepthTextureView());
      }
   }

   public void invalidateCompiledGeometry(final ClientLevel level, final Options options, final Camera camera, final BlockColors blockColors) {
      SectionCompiler sectionCompiler = new SectionCompiler(
         options.ambientOcclusion().get(),
         options.cutoutLeaves().get(),
         this.modelManager.getBlockStateModelSet(),
         this.modelManager.getFluidStateModelSet(),
         blockColors
      );
      if (this.sectionRenderDispatcher == null) {
         this.sectionRenderDispatcher = new SectionRenderDispatcher(
            Util.backgroundExecutor(), this.renderBuffers, sectionCompiler, this.sectionOcclusionGraph::schedulePropagationFrom
         );
      } else {
         this.sectionRenderDispatcher.setCompiler(sectionCompiler);
      }

      this.cloudRenderer().markForRebuild();
      LeavesBlock.setCutoutLeaves(options.cutoutLeaves().get());
      if (this.viewArea != null) {
         this.viewArea.releaseAllBuffers();
      }

      this.sectionRenderDispatcher.clearCompileQueue();
      this.viewArea = new ViewArea(
         this.sectionRenderDispatcher,
         level.getMinY(),
         level.getMaxY(),
         level.getMinSectionY(),
         level.getMaxSectionY(),
         options.getEffectiveRenderDistance(),
         this.sectionOcclusionGraph
      );
      this.sectionOcclusionGraph().waitAndReset(this.viewArea);
      this.clearVisibleSections();
      SectionPos cameraSectionPos = SectionPos.of(camera.position());
      this.viewArea.repositionCamera(cameraSectionPos);
   }

   private void scheduleTranslucentSectionResort(final Vec3 cameraPos) {
      if (!this.visibleSections.isEmpty()) {
         BlockPos cameraBlockPos = BlockPos.containing(cameraPos);
         boolean blockPosChanged = !cameraBlockPos.equals(this.lastTranslucentSortBlockPos);
         TranslucencyPointOfView pointOfView = new TranslucencyPointOfView();
         ObjectListIterator resortsLeft = this.nearbyVisibleSections.iterator();

         while (resortsLeft.hasNext()) {
            SectionRenderDispatcher.RenderSection section = (SectionRenderDispatcher.RenderSection)resortsLeft.next();
            this.scheduleResort(section, pointOfView, cameraPos, blockPosChanged, true);
         }

         this.translucencyResortIterationIndex = this.translucencyResortIterationIndex % this.visibleSections.size();
         int resortsLeftx = Math.max(this.visibleSections.size() / 8, 15);

         while (resortsLeftx-- > 0) {
            int index = this.translucencyResortIterationIndex++ % this.visibleSections.size();
            this.scheduleResort((SectionRenderDispatcher.RenderSection)this.visibleSections.get(index), pointOfView, cameraPos, blockPosChanged, false);
         }

         this.lastTranslucentSortBlockPos = cameraBlockPos;
      }
   }

   private void scheduleResort(
      final SectionRenderDispatcher.RenderSection section,
      final TranslucencyPointOfView pointOfView,
      final Vec3 cameraPos,
      final boolean blockPosChanged,
      final boolean isNearby
   ) {
      pointOfView.set(cameraPos, section.getSectionNode());
      boolean pointOfViewChanged = section.getSectionMesh().isDifferentPointOfView(pointOfView);
      boolean resortBecauseBlockPosChanged = blockPosChanged && (pointOfView.isAxisAligned() || isNearby);
      if ((resortBecauseBlockPosChanged || pointOfViewChanged)
         && !section.transparencyResortingScheduled()
         && section.hasTranslucentGeometry()
         && !this.gameRenderer.useImprovedTransparency()) {
         section.resortTransparency();
      }
   }

   public void clearVisibleSections() {
      this.visibleSections.clear();
      this.nearbyVisibleSections.clear();
   }

   public void resetLevelRenderData() {
      if (this.viewArea != null) {
         this.viewArea.releaseAllBuffers();
         this.viewArea = null;
      }

      if (this.sectionRenderDispatcher != null) {
         this.sectionRenderDispatcher.dispose();
      }

      this.sectionRenderDispatcher = null;
      this.sectionOcclusionGraph.waitAndReset(null);
      this.clearVisibleSections();
   }

   public boolean hasRenderedAllSections() {
      return this.sectionRenderDispatcher == null || this.sectionRenderDispatcher.isQueueEmpty();
   }

   public boolean isSectionCompiledAndVisible(final BlockPos blockPos) {
      if (this.viewArea == null) {
         return false;
      }

      SectionRenderDispatcher.RenderSection renderSection = this.viewArea.getRenderSectionAt(blockPos);
      return renderSection != null && renderSection.sectionMesh.get() != CompiledSectionMesh.UNCOMPILED
         ? renderSection.getVisibility(Util.getMillis()) >= 0.3F
         : false;
   }

   public @Nullable SectionRenderDispatcher sectionRenderDispatcher() {
      return this.sectionRenderDispatcher;
   }

   public EntityRenderDispatcher entityRenderDispatcher() {
      return this.entityRenderDispatcher;
   }

   public BlockEntityRenderDispatcher blockEntityRenderDispatcher() {
      return this.blockEntityRenderDispatcher;
   }

   public @Nullable RenderTarget entityOutlineTarget() {
      return this.targets.entityOutline != null ? this.targets.entityOutline.get() : null;
   }

   public @Nullable RenderTarget terrainDepthTarget() {
      return this.targets.oitTerrainWithWaterPatchDepth != null ? this.targets.oitTerrainWithWaterPatchDepth.get() : null;
   }

   public @Nullable RenderTarget depthBoundsTarget() {
      return this.targets.depthBounds != null ? this.targets.depthBounds.get() : null;
   }

   public @Nullable RenderTarget transmittanceTarget(final int index) {
      return this.targets.transmittance.get(index).get() != null ? this.targets.transmittance.get(index).get() : null;
   }

   public @Nullable RenderTarget accumulateTarget() {
      return this.targets.accumulate != null ? this.targets.accumulate.get() : null;
   }

   public CloudRenderer cloudRenderer() {
      return this.cloudRenderer;
   }

   public @Nullable SkyRenderer skyRenderer() {
      return this.skyRenderer;
   }

   public WeatherEffectRenderer weatherEffectRenderer() {
      return this.weatherEffectRenderer;
   }

   public WorldBorderRenderer worldBorderRenderer() {
      return this.worldBorderRenderer;
   }

   public @Nullable ViewArea viewArea() {
      return this.viewArea;
   }

   public ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections() {
      return this.visibleSections;
   }

   public ObjectArrayList<SectionRenderDispatcher.RenderSection> nearbyVisibleSections() {
      return this.nearbyVisibleSections;
   }

   public LongCollection expectedChunks() {
      return this.sectionOcclusionGraph.expectedChunks();
   }

   public SectionOcclusionGraph sectionOcclusionGraph() {
      return this.sectionOcclusionGraph;
   }

   public Gizmos.TemporaryCollection collectPerFrameRenderThreadGizmos() {
      return Gizmos.withCollector(this.renderThreadGizmos);
   }

   private void finalizeGizmoCollection() {
      DrawableGizmoPrimitives standardPrimitives = new DrawableGizmoPrimitives();
      DrawableGizmoPrimitives alwaysOnTopPrimitives = new DrawableGizmoPrimitives();
      long currentMillis = Util.getMillis();

      for (SimpleGizmoCollector.GizmoInstance instance : this.renderThreadGizmos.drainGizmos()) {
         instance.gizmo().emit(instance.isAlwaysOnTop() ? alwaysOnTopPrimitives : standardPrimitives, instance.getAlphaMultiplier(currentMillis));
      }

      this.finalizedGizmos = new LevelRenderer.FinalizedGizmos(standardPrimitives, alwaysOnTopPrimitives);
   }

   public void addMainThreadGizmos(final List<SimpleGizmoCollector.GizmoInstance> mainThreadGizmos) {
      this.renderThreadGizmos.addTemporaryGizmos(mainThreadGizmos);
   }

   private record FinalizedGizmos(DrawableGizmoPrimitives standardPrimitives, DrawableGizmoPrimitives alwaysOnTopPrimitives) {
   }
}
