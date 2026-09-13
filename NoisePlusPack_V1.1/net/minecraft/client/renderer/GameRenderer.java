package net.minecraft.client.renderer;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.jtracy.TracyClient;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.Screenshot;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.font.ActiveArea;
import net.minecraft.client.gui.font.EmptyArea;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.pip.GuiBannerResultRenderer;
import net.minecraft.client.gui.render.pip.GuiBookModelRenderer;
import net.minecraft.client.gui.render.pip.GuiEntityRenderer;
import net.minecraft.client.gui.render.pip.GuiProfilerChartRenderer;
import net.minecraft.client.gui.render.pip.GuiSignRenderer;
import net.minecraft.client.gui.render.pip.GuiSkinRenderer;
import net.minecraft.client.gui.render.state.ColoredRectangleRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.screens.debug.DebugOptionsScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.Zone;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.io.IOUtils;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class GameRenderer implements TrackedWaypoint.Projector, AutoCloseable {
   private static final Identifier BLUR_POST_CHAIN_ID = Identifier.withDefaultNamespace("blur");
   public static final int MAX_BLUR_RADIUS = 10;
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final float PROJECTION_Z_NEAR = 0.05F;
   public static final float PROJECTION_3D_HUD_Z_FAR = 100.0F;
   private static final float PORTAL_SPINNING_SPEED = 20.0F;
   private static final float NAUSEA_SPINNING_SPEED = 7.0F;
   private final Minecraft minecraft;
   private final RandomSource random = RandomSource.create();
   private float renderDistance;
   public final ItemInHandRenderer itemInHandRenderer;
   private final ScreenEffectRenderer screenEffectRenderer;
   private final RenderBuffers renderBuffers;
   private float spinningEffectTime;
   private float spinningEffectSpeed;
   private float fovModifier;
   private float oldFovModifier;
   private float darkenWorldAmount;
   private float darkenWorldAmountO;
   private boolean renderBlockOutline = true;
   private long lastScreenshotAttempt;
   private boolean hasWorldScreenshot;
   private long lastActiveTime = Util.getMillis();
   private final LightTexture lightTexture;
   private final OverlayTexture overlayTexture = new OverlayTexture();
   private @Nullable PanoramicScreenshotParameters panoramicScreenshotParameters;
   protected final CubeMap cubeMap = new CubeMap(Identifier.withDefaultNamespace("textures/gui/title/background/panorama"));
   protected final PanoramaRenderer panorama = new PanoramaRenderer(this.cubeMap);
   private final CrossFrameResourcePool resourcePool = new CrossFrameResourcePool(3);
   private final FogRenderer fogRenderer = new FogRenderer();
   private final GuiRenderer guiRenderer;
   final GuiRenderState guiRenderState;
   private final LevelRenderState levelRenderState = new LevelRenderState();
   private final SubmitNodeStorage submitNodeStorage;
   private final FeatureRenderDispatcher featureRenderDispatcher;
   private @Nullable Identifier postEffectId;
   private boolean effectActive;
   private final Camera mainCamera = new Camera();
   private final Lighting lighting = new Lighting();
   private final GlobalSettingsUniform globalSettingsUniform = new GlobalSettingsUniform();
   private final PerspectiveProjectionMatrixBuffer levelProjectionMatrixBuffer = new PerspectiveProjectionMatrixBuffer("level");
   private final CachedPerspectiveProjectionMatrixBuffer hud3dProjectionMatrixBuffer = new CachedPerspectiveProjectionMatrixBuffer("3d hud", 0.05F, 100.0F);

   public GameRenderer(Minecraft p_234219_, ItemInHandRenderer p_234220_, RenderBuffers p_234222_, BlockRenderDispatcher p_426284_) {
      this.minecraft = p_234219_;
      this.itemInHandRenderer = p_234220_;
      this.lightTexture = new LightTexture(this, p_234219_);
      this.renderBuffers = p_234222_;
      this.guiRenderState = new GuiRenderState();
      MultiBufferSource.BufferSource multibuffersource$buffersource = p_234222_.bufferSource();
      AtlasManager atlasmanager = p_234219_.getAtlasManager();
      this.submitNodeStorage = new SubmitNodeStorage();
      this.featureRenderDispatcher = new FeatureRenderDispatcher(
         this.submitNodeStorage,
         p_426284_,
         multibuffersource$buffersource,
         atlasmanager,
         p_234222_.outlineBufferSource(),
         p_234222_.crumblingBufferSource(),
         p_234219_.font
      );
      this.guiRenderer = new GuiRenderer(
         this.guiRenderState,
         multibuffersource$buffersource,
         this.submitNodeStorage,
         this.featureRenderDispatcher,
         List.of(
            new GuiEntityRenderer(multibuffersource$buffersource, p_234219_.getEntityRenderDispatcher()),
            new GuiSkinRenderer(multibuffersource$buffersource),
            new GuiBookModelRenderer(multibuffersource$buffersource),
            new GuiBannerResultRenderer(multibuffersource$buffersource, atlasmanager),
            new GuiSignRenderer(multibuffersource$buffersource, atlasmanager),
            new GuiProfilerChartRenderer(multibuffersource$buffersource)
         )
      );
      this.screenEffectRenderer = new ScreenEffectRenderer(p_234219_, atlasmanager, multibuffersource$buffersource);
   }

   @Override
   public void close() {
      this.globalSettingsUniform.close();
      this.lightTexture.close();
      this.overlayTexture.close();
      this.resourcePool.close();
      this.guiRenderer.close();
      this.levelProjectionMatrixBuffer.close();
      this.hud3dProjectionMatrixBuffer.close();
      this.lighting.close();
      this.cubeMap.close();
      this.fogRenderer.close();
      this.featureRenderDispatcher.close();
   }

   public SubmitNodeStorage getSubmitNodeStorage() {
      return this.submitNodeStorage;
   }

   public FeatureRenderDispatcher getFeatureRenderDispatcher() {
      return this.featureRenderDispatcher;
   }

   public LevelRenderState getLevelRenderState() {
      return this.levelRenderState;
   }

   public void setRenderBlockOutline(boolean p_172776_) {
      this.renderBlockOutline = p_172776_;
   }

   public void setPanoramicScreenshotParameters(@Nullable PanoramicScreenshotParameters p_452754_) {
      this.panoramicScreenshotParameters = p_452754_;
   }

   public @Nullable PanoramicScreenshotParameters getPanoramicScreenshotParameters() {
      return this.panoramicScreenshotParameters;
   }

   public boolean isPanoramicMode() {
      return this.panoramicScreenshotParameters != null;
   }

   public void clearPostEffect() {
      this.postEffectId = null;
      this.effectActive = false;
   }

   public void togglePostEffect() {
      this.effectActive = !this.effectActive;
   }

   public void checkEntityPostEffect(@Nullable Entity p_109107_) {
      switch (p_109107_) {
         case Creeper creeper:
            this.setPostEffect(Identifier.withDefaultNamespace("creeper"));
            break;
         case Spider spider:
            this.setPostEffect(Identifier.withDefaultNamespace("spider"));
            break;
         case EnderMan enderman:
            this.setPostEffect(Identifier.withDefaultNamespace("invert"));
            break;
         case null:
         default:
            this.clearPostEffect();
      }
   }

   private void setPostEffect(Identifier p_452796_) {
      this.postEffectId = p_452796_;
      this.effectActive = true;
   }

   public void processBlurEffect() {
      PostChain postchain = this.minecraft.getShaderManager().getPostChain(BLUR_POST_CHAIN_ID, LevelTargetBundle.MAIN_TARGETS);
      if (postchain != null) {
         postchain.process(this.minecraft.getMainRenderTarget(), this.resourcePool);
      }
   }

   public void preloadUiShader(ResourceProvider p_172723_) {
      GpuDevice gpudevice = RenderSystem.getDevice();
      ShaderSource shadersource = (p_456060_, p_389361_) -> {
         Identifier identifier = p_389361_.idConverter().idToFile(p_456060_);

         try (Reader reader = p_172723_.getResourceOrThrow(identifier).openAsReader()) {
            return IOUtils.toString(reader);
         } catch (IOException ioexception) {
            LOGGER.error("Coudln't preload {} shader {}: {}", new Object[]{p_389361_, p_456060_, ioexception});
            return null;
         }
      };
      gpudevice.precompilePipeline(RenderPipelines.GUI, shadersource);
      gpudevice.precompilePipeline(RenderPipelines.GUI_TEXTURED, shadersource);
      if (TracyClient.isAvailable()) {
         gpudevice.precompilePipeline(RenderPipelines.TRACY_BLIT, shadersource);
      }
   }

   public void tick() {
      this.tickFov();
      this.lightTexture.tick();
      LocalPlayer localplayer = this.minecraft.player;
      if (this.minecraft.getCameraEntity() == null) {
         this.minecraft.setCameraEntity(localplayer);
      }

      this.mainCamera.tick();
      this.itemInHandRenderer.tick();
      float f = localplayer.portalEffectIntensity;
      float f1 = localplayer.getEffectBlendFactor(MobEffects.NAUSEA, 1.0F);
      if (!(f > 0.0F) && !(f1 > 0.0F)) {
         this.spinningEffectSpeed = 0.0F;
      } else {
         this.spinningEffectSpeed = (f * 20.0F + f1 * 7.0F) / (f + f1);
         this.spinningEffectTime = this.spinningEffectTime + this.spinningEffectSpeed;
      }

      if (this.minecraft.level.tickRateManager().runsNormally()) {
         this.darkenWorldAmountO = this.darkenWorldAmount;
         if (this.minecraft.gui.getBossOverlay().shouldDarkenScreen()) {
            this.darkenWorldAmount += 0.05F;
            if (this.darkenWorldAmount > 1.0F) {
               this.darkenWorldAmount = 1.0F;
            }
         } else if (this.darkenWorldAmount > 0.0F) {
            this.darkenWorldAmount -= 0.0125F;
         }

         this.screenEffectRenderer.tick();
         ProfilerFiller profilerfiller = Profiler.get();
         profilerfiller.push("levelRenderer");
         this.minecraft.levelRenderer.tick(this.mainCamera);
         profilerfiller.pop();
      }
   }

   public @Nullable Identifier currentPostEffect() {
      return this.postEffectId;
   }

   public void resize(int p_109098_, int p_109099_) {
      this.resourcePool.clear();
      this.minecraft.levelRenderer.resize(p_109098_, p_109099_);
   }

   public void pick(float p_109088_) {
      Entity entity = this.minecraft.getCameraEntity();
      if (entity != null && this.minecraft.level != null && this.minecraft.player != null) {
         Profiler.get().push("pick");
         this.minecraft.hitResult = this.minecraft.player.raycastHitResult(p_109088_, entity);
         this.minecraft.crosshairPickEntity = this.minecraft.hitResult instanceof EntityHitResult entityhitresult ? entityhitresult.getEntity() : null;
         Profiler.get().pop();
      }
   }

   private void tickFov() {
      float f;
      if (this.minecraft.getCameraEntity() instanceof AbstractClientPlayer abstractclientplayer) {
         Options options = this.minecraft.options;
         boolean flag = options.getCameraType().isFirstPerson();
         float f1 = options.fovEffectScale().get().floatValue();
         f = abstractclientplayer.getFieldOfViewModifier(flag, f1);
      } else {
         f = 1.0F;
      }

      this.oldFovModifier = this.fovModifier;
      this.fovModifier = this.fovModifier + (f - this.fovModifier) * 0.5F;
      this.fovModifier = Mth.clamp(this.fovModifier, 0.1F, 1.5F);
   }

   private float getFov(Camera p_109142_, float p_109143_, boolean p_109144_) {
      if (this.isPanoramicMode()) {
         return 90.0F;
      }

      float f = 70.0F;
      if (p_109144_) {
         f = this.minecraft.options.fov().get().intValue();
         f *= Mth.lerp(p_109143_, this.oldFovModifier, this.fovModifier);
      }

      if (p_109142_.entity() instanceof LivingEntity livingentity && livingentity.isDeadOrDying()) {
         float f1 = Math.min(livingentity.deathTime + p_109143_, 20.0F);
         f /= (1.0F - 500.0F / (f1 + 500.0F)) * 2.0F + 1.0F;
      }

      FogType fogtype = p_109142_.getFluidInCamera();
      if (fogtype == FogType.LAVA || fogtype == FogType.WATER) {
         float f2 = this.minecraft.options.fovEffectScale().get().floatValue();
         f *= Mth.lerp(f2, 1.0F, 0.85714287F);
      }

      return f;
   }

   private void bobHurt(PoseStack p_109118_, float p_109119_) {
      if (this.minecraft.getCameraEntity() instanceof LivingEntity livingentity) {
         float f2 = livingentity.hurtTime - p_109119_;
         if (livingentity.isDeadOrDying()) {
            float f = Math.min(livingentity.deathTime + p_109119_, 20.0F);
            p_109118_.mulPose(Axis.ZP.rotationDegrees(40.0F - 8000.0F / (f + 200.0F)));
         }

         if (f2 < 0.0F) {
            return;
         }

         f2 /= livingentity.hurtDuration;
         f2 = Mth.sin(f2 * f2 * f2 * f2 * (float) Math.PI);
         float f3 = livingentity.getHurtDir();
         p_109118_.mulPose(Axis.YP.rotationDegrees(-f3));
         float f1 = (float)(-f2 * 14.0 * this.minecraft.options.damageTiltStrength().get());
         p_109118_.mulPose(Axis.ZP.rotationDegrees(f1));
         p_109118_.mulPose(Axis.YP.rotationDegrees(f3));
      }
   }

   private void bobView(PoseStack p_109139_, float p_109140_) {
      if (this.minecraft.getCameraEntity() instanceof AbstractClientPlayer abstractclientplayer) {
         ClientAvatarState clientavatarstate = abstractclientplayer.avatarState();
         float $$5 = clientavatarstate.getBackwardsInterpolatedWalkDistance(p_109140_);
         float $$6 = clientavatarstate.getInterpolatedBob(p_109140_);
         p_109139_.translate(Mth.sin($$5 * (float) Math.PI) * $$6 * 0.5F, -Math.abs(Mth.cos($$5 * (float) Math.PI) * $$6), 0.0F);
         p_109139_.mulPose(Axis.ZP.rotationDegrees(Mth.sin($$5 * (float) Math.PI) * $$6 * 3.0F));
         p_109139_.mulPose(Axis.XP.rotationDegrees(Math.abs(Mth.cos($$5 * (float) Math.PI - 0.2F) * $$6) * 5.0F));
      }
   }

   private void renderItemInHand(float p_109123_, boolean p_408020_, Matrix4f p_331664_) {
      if (!this.isPanoramicMode()) {
         this.featureRenderDispatcher.renderAllFeatures();
         this.renderBuffers.bufferSource().endBatch();
         PoseStack posestack = new PoseStack();
         posestack.pushPose();
         posestack.mulPose(p_331664_.invert(new Matrix4f()));
         Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
         matrix4fstack.pushMatrix().mul(p_331664_);
         this.bobHurt(posestack, p_109123_);
         if (this.minecraft.options.bobView().get()) {
            this.bobView(posestack, p_109123_);
         }

         if (this.minecraft.options.getCameraType().isFirstPerson()
            && !p_408020_
            && !this.minecraft.options.hideGui
            && this.minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR) {
            this.itemInHandRenderer
               .renderHandsWithItems(
                  p_109123_,
                  posestack,
                  this.minecraft.gameRenderer.getSubmitNodeStorage(),
                  this.minecraft.player,
                  this.minecraft.getEntityRenderDispatcher().getPackedLightCoords(this.minecraft.player, p_109123_)
               );
         }

         matrix4fstack.popMatrix();
         posestack.popPose();
      }
   }

   public Matrix4f getProjectionMatrix(float p_364788_) {
      Matrix4f matrix4f = new Matrix4f();
      return matrix4f.perspective(
         p_364788_ * (float) (Math.PI / 180.0),
         (float)this.minecraft.getWindow().getWidth() / this.minecraft.getWindow().getHeight(),
         0.05F,
         this.getDepthFar()
      );
   }

   public float getDepthFar() {
      return Math.max(this.renderDistance * 4.0F, this.minecraft.options.cloudRange().get() * 16);
   }

   public static float getNightVisionScale(LivingEntity p_109109_, float p_109110_) {
      MobEffectInstance mobeffectinstance = p_109109_.getEffect(MobEffects.NIGHT_VISION);
      return !mobeffectinstance.endsWithin(200) ? 1.0F : 0.7F + Mth.sin((mobeffectinstance.getDuration() - p_109110_) * (float) Math.PI * 0.2F) * 0.3F;
   }

   public void render(DeltaTracker p_343467_, boolean p_109096_) {
      if (!this.minecraft.isWindowActive()
         && this.minecraft.options.pauseOnLostFocus
         && (!this.minecraft.options.touchscreen().get() || !this.minecraft.mouseHandler.isRightPressed())) {
         if (Util.getMillis() - this.lastActiveTime > 500L) {
            this.minecraft.pauseGame(false);
         }
      } else {
         this.lastActiveTime = Util.getMillis();
      }

      if (!this.minecraft.noRender) {
         ProfilerFiller profilerfiller = Profiler.get();
         profilerfiller.push("camera");
         this.updateCamera(p_343467_);
         profilerfiller.pop();
         this.globalSettingsUniform
            .update(
               this.minecraft.getWindow().getWidth(),
               this.minecraft.getWindow().getHeight(),
               this.minecraft.options.glintStrength().get(),
               this.minecraft.level == null ? 0L : this.minecraft.level.getGameTime(),
               p_343467_,
               this.minecraft.options.getMenuBackgroundBlurriness(),
               this.mainCamera,
               this.minecraft.options.textureFiltering().get() == TextureFilteringMethod.RGSS
            );
         boolean flag = this.minecraft.isGameLoadFinished();
         int i = (int)this.minecraft.mouseHandler.getScaledXPos(this.minecraft.getWindow());
         int j = (int)this.minecraft.mouseHandler.getScaledYPos(this.minecraft.getWindow());
         if (flag && p_109096_ && this.minecraft.level != null) {
            profilerfiller.push("world");
            this.renderLevel(p_343467_);
            this.tryTakeScreenshotIfNeeded();
            this.minecraft.levelRenderer.doEntityOutline();
            if (this.postEffectId != null && this.effectActive) {
               PostChain postchain = this.minecraft.getShaderManager().getPostChain(this.postEffectId, LevelTargetBundle.MAIN_TARGETS);
               if (postchain != null) {
                  postchain.process(this.minecraft.getMainRenderTarget(), this.resourcePool);
               }
            }

            profilerfiller.pop();
         }

         this.fogRenderer.endFrame();
         RenderTarget rendertarget = this.minecraft.getMainRenderTarget();
         RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(rendertarget.getDepthTexture(), 1.0);
         this.minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
         this.guiRenderState.reset();
         profilerfiller.push("guiExtraction");
         GuiGraphics guigraphics = new GuiGraphics(this.minecraft, this.guiRenderState, i, j);
         if (flag && p_109096_ && this.minecraft.level != null) {
            this.minecraft.gui.render(guigraphics, p_343467_);
         }

         if (this.minecraft.getOverlay() != null) {
            try {
               this.minecraft.getOverlay().render(guigraphics, i, j, p_343467_.getGameTimeDeltaTicks());
            } catch (Throwable throwable2) {
               CrashReport crashreport = CrashReport.forThrowable(throwable2, "Rendering overlay");
               CrashReportCategory crashreportcategory = crashreport.addCategory("Overlay render details");
               crashreportcategory.setDetail("Overlay name", () -> this.minecraft.getOverlay().getClass().getCanonicalName());
               throw new ReportedException(crashreport);
            }
         } else if (flag && this.minecraft.screen != null) {
            try {
               this.minecraft.screen.renderWithTooltipAndSubtitles(guigraphics, i, j, p_343467_.getGameTimeDeltaTicks());
            } catch (Throwable throwable1) {
               CrashReport crashreport1 = CrashReport.forThrowable(throwable1, "Rendering screen");
               CrashReportCategory crashreportcategory1 = crashreport1.addCategory("Screen render details");
               crashreportcategory1.setDetail("Screen name", () -> this.minecraft.screen.getClass().getCanonicalName());
               this.minecraft.mouseHandler.fillMousePositionDetails(crashreportcategory1, this.minecraft.getWindow());
               throw new ReportedException(crashreport1);
            }

            if (SharedConstants.DEBUG_CURSOR_POS) {
               this.minecraft.mouseHandler.drawDebugMouseInfo(this.minecraft.font, guigraphics);
            }

            try {
               if (this.minecraft.screen != null) {
                  this.minecraft.screen.handleDelayedNarration();
               }
            } catch (Throwable throwable) {
               CrashReport crashreport2 = CrashReport.forThrowable(throwable, "Narrating screen");
               CrashReportCategory crashreportcategory2 = crashreport2.addCategory("Screen details");
               crashreportcategory2.setDetail("Screen name", () -> this.minecraft.screen.getClass().getCanonicalName());
               throw new ReportedException(crashreport2);
            }
         }

         if (flag && p_109096_ && this.minecraft.level != null) {
            this.minecraft.gui.renderSavingIndicator(guigraphics, p_343467_);
         }

         if (flag) {
            try (Zone zone = profilerfiller.zone("toasts")) {
               this.minecraft.getToastManager().render(guigraphics);
            }
         }

         if (!(this.minecraft.screen instanceof DebugOptionsScreen)) {
            this.minecraft.gui.renderDebugOverlay(guigraphics);
         }

         this.minecraft.gui.renderDeferredSubtitles();
         if (SharedConstants.DEBUG_ACTIVE_TEXT_AREAS) {
            this.renderActiveTextDebug();
         }

         profilerfiller.popPush("guiRendering");
         this.guiRenderer.render(this.fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
         this.guiRenderer.incrementFrameNumber();
         profilerfiller.pop();
         guigraphics.applyCursor(this.minecraft.getWindow());
         this.submitNodeStorage.endFrame();
         this.featureRenderDispatcher.endFrame();
         this.resourcePool.endFrame();
      }
   }

   private void renderActiveTextDebug() {
      this.guiRenderState.nextStratum();
      this.guiRenderState
         .forEachText(
            p_448149_ -> p_448149_.ensurePrepared()
               .visit(
                  new Font.GlyphVisitor() {
                     private int index;

                     @Override
                     public void acceptGlyph(TextRenderable.Styled p_457427_) {
                        this.renderDebugMarkers(p_457427_, false);
                     }

                     @Override
                     public void acceptEmptyArea(EmptyArea p_456531_) {
                        this.renderDebugMarkers(p_456531_, true);
                     }

                     private void renderDebugMarkers(ActiveArea p_459717_, boolean p_457171_) {
                        int i = (p_457171_ ? 128 : 255) - (this.index++ & 1) * 64;
                        Style style = p_459717_.style();
                        int j = style.getClickEvent() != null ? i : 0;
                        int k = style.getHoverEvent() != null ? i : 0;
                        int l = j != 0 && k != 0 ? 0 : i;
                        int i1 = ARGB.color(128, j, k, l);
                        GameRenderer.this.guiRenderState
                           .submitGuiElement(
                              new ColoredRectangleRenderState(
                                 RenderPipelines.GUI,
                                 TextureSetup.noTexture(),
                                 p_448149_.pose,
                                 (int)p_459717_.activeLeft(),
                                 (int)p_459717_.activeTop(),
                                 (int)p_459717_.activeRight(),
                                 (int)p_459717_.activeBottom(),
                                 i1,
                                 i1,
                                 p_448149_.scissor
                              )
                           );
                     }
                  }
               )
         );
   }

   private void tryTakeScreenshotIfNeeded() {
      if (!this.hasWorldScreenshot && this.minecraft.isLocalServer()) {
         long i = Util.getMillis();
         if (i - this.lastScreenshotAttempt >= 1000L) {
            this.lastScreenshotAttempt = i;
            IntegratedServer integratedserver = this.minecraft.getSingleplayerServer();
            if (integratedserver != null && !integratedserver.isStopped()) {
               integratedserver.getWorldScreenshotFile().ifPresent(p_234239_ -> {
                  if (Files.isRegularFile(p_234239_)) {
                     this.hasWorldScreenshot = true;
                  } else {
                     this.takeAutoScreenshot(p_234239_);
                  }
               });
            }
         }
      }
   }

   private void takeAutoScreenshot(Path p_182643_) {
      if (this.minecraft.levelRenderer.countRenderedSections() > 10 && this.minecraft.levelRenderer.hasRenderedAllSections()) {
         Screenshot.takeScreenshot(this.minecraft.getMainRenderTarget(), p_448151_ -> Util.ioPool().execute(() -> {
            int i = p_448151_.getWidth();
            int j = p_448151_.getHeight();
            int k = 0;
            int l = 0;
            if (i > j) {
               k = (i - j) / 2;
               i = j;
            } else {
               l = (j - i) / 2;
               j = i;
            }

            try (NativeImage nativeimage = new NativeImage(64, 64, false)) {
               p_448151_.resizeSubRectTo(k, l, i, j, nativeimage);
               nativeimage.writeToFile(p_182643_);
            } catch (IOException ioexception) {
               LOGGER.warn("Couldn't save auto screenshot", ioexception);
            } finally {
               p_448151_.close();
            }
         }));
      }
   }

   private boolean shouldRenderBlockOutline() {
      if (!this.renderBlockOutline) {
         return false;
      }

      Entity entity = this.minecraft.getCameraEntity();
      boolean flag = entity instanceof Player && !this.minecraft.options.hideGui;
      if (flag && !((Player)entity).getAbilities().mayBuild) {
         ItemStack itemstack = ((LivingEntity)entity).getMainHandItem();
         HitResult hitresult = this.minecraft.hitResult;
         if (hitresult != null && hitresult.getType() == HitResult.Type.BLOCK) {
            BlockPos blockpos = ((BlockHitResult)hitresult).getBlockPos();
            BlockState blockstate = this.minecraft.level.getBlockState(blockpos);
            if (this.minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR) {
               flag = blockstate.getMenuProvider(this.minecraft.level, blockpos) != null;
            } else {
               BlockInWorld blockinworld = new BlockInWorld(this.minecraft.level, blockpos, false);
               Registry<Block> registry = this.minecraft.level.registryAccess().lookupOrThrow(Registries.BLOCK);
               flag = !itemstack.isEmpty() && (itemstack.canBreakBlockInAdventureMode(blockinworld) || itemstack.canPlaceOnBlockInAdventureMode(blockinworld));
            }
         }
      }

      return flag;
   }

   public void updateCamera(DeltaTracker p_450216_) {
      float f = p_450216_.getGameTimeDeltaPartialTick(true);
      LocalPlayer localplayer = this.minecraft.player;
      if (localplayer != null && this.minecraft.level != null) {
         if (this.minecraft.getCameraEntity() == null) {
            this.minecraft.setCameraEntity(localplayer);
         }

         Entity entity = this.minecraft.getCameraEntity() == null ? localplayer : this.minecraft.getCameraEntity();
         float f1 = this.minecraft.level.tickRateManager().isEntityFrozen(entity) ? 1.0F : f;
         this.mainCamera
            .setup(
               this.minecraft.level, entity, !this.minecraft.options.getCameraType().isFirstPerson(), this.minecraft.options.getCameraType().isMirrored(), f1
            );
      }
   }

   public void renderLevel(DeltaTracker p_342230_) {
      float f = p_342230_.getGameTimeDeltaPartialTick(true);
      LocalPlayer localplayer = this.minecraft.player;
      this.lightTexture.updateLightTexture(1.0F);
      this.pick(f);
      ProfilerFiller profilerfiller = Profiler.get();
      boolean flag = this.shouldRenderBlockOutline();
      this.extractCamera(f);
      this.renderDistance = this.minecraft.options.getEffectiveRenderDistance() * 16;
      profilerfiller.push("matrices");
      float f1 = this.getFov(this.mainCamera, f, true);
      Matrix4f matrix4f = this.getProjectionMatrix(f1);
      PoseStack posestack = new PoseStack();
      this.bobHurt(posestack, this.mainCamera.getPartialTickTime());
      if (this.minecraft.options.bobView().get()) {
         this.bobView(posestack, this.mainCamera.getPartialTickTime());
      }

      matrix4f.mul(posestack.last().pose());
      float f2 = this.minecraft.options.screenEffectScale().get().floatValue();
      float f3 = Mth.lerp(f, localplayer.oPortalEffectIntensity, localplayer.portalEffectIntensity);
      float f4 = localplayer.getEffectBlendFactor(MobEffects.NAUSEA, f);
      float f5 = Math.max(f3, f4) * (f2 * f2);
      if (f5 > 0.0F) {
         float f6 = 5.0F / (f5 * f5 + 5.0F) - f5 * 0.04F;
         f6 *= f6;
         Vector3f vector3f = new Vector3f(0.0F, Mth.SQRT_OF_TWO / 2.0F, Mth.SQRT_OF_TWO / 2.0F);
         float f7 = (this.spinningEffectTime + f * this.spinningEffectSpeed) * (float) (Math.PI / 180.0);
         matrix4f.rotate(f7, vector3f);
         matrix4f.scale(1.0F / f6, 1.0F, 1.0F);
         matrix4f.rotate(-f7, vector3f);
      }

      RenderSystem.setProjectionMatrix(this.levelProjectionMatrixBuffer.getBuffer(matrix4f), ProjectionType.PERSPECTIVE);
      Quaternionf quaternionf = this.mainCamera.rotation().conjugate(new Quaternionf());
      Matrix4f matrix4f1 = new Matrix4f().rotation(quaternionf);
      profilerfiller.popPush("fog");
      Vector4f vector4f = this.fogRenderer
         .setupFog(this.mainCamera, this.minecraft.options.getEffectiveRenderDistance(), p_342230_, this.getDarkenWorldAmount(f), this.minecraft.level);
      GpuBufferSlice gpubufferslice = this.fogRenderer.getBuffer(FogRenderer.FogMode.WORLD);
      profilerfiller.popPush("level");
      boolean flag1 = this.minecraft.gui.getBossOverlay().shouldCreateWorldFog();
      this.minecraft
         .levelRenderer
         .renderLevel(
            this.resourcePool, p_342230_, flag, this.mainCamera, matrix4f1, matrix4f, this.getProjectionMatrixForCulling(f1), gpubufferslice, vector4f, !flag1
         );
      profilerfiller.popPush("hand");
      boolean flag2 = this.minecraft.getCameraEntity() instanceof LivingEntity && ((LivingEntity)this.minecraft.getCameraEntity()).isSleeping();
      RenderSystem.setProjectionMatrix(
         this.hud3dProjectionMatrixBuffer
            .getBuffer(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight(), this.getFov(this.mainCamera, f, false)),
         ProjectionType.PERSPECTIVE
      );
      RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(this.minecraft.getMainRenderTarget().getDepthTexture(), 1.0);
      this.renderItemInHand(f, flag2, matrix4f1);
      profilerfiller.popPush("screenEffects");
      MultiBufferSource.BufferSource multibuffersource$buffersource = this.renderBuffers.bufferSource();
      this.screenEffectRenderer.renderScreenEffect(flag2, f, this.submitNodeStorage);
      this.featureRenderDispatcher.renderAllFeatures();
      multibuffersource$buffersource.endBatch();
      profilerfiller.pop();
      RenderSystem.setShaderFog(this.fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
      if (this.minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.THREE_DIMENSIONAL_CROSSHAIR)
         && this.minecraft.options.getCameraType().isFirstPerson()
         && !this.minecraft.options.hideGui) {
         this.minecraft.getDebugOverlay().render3dCrosshair(this.mainCamera);
      }
   }

   private void extractCamera(float p_427495_) {
      CameraRenderState camerarenderstate = this.levelRenderState.cameraRenderState;
      camerarenderstate.initialized = this.mainCamera.isInitialized();
      camerarenderstate.pos = this.mainCamera.position();
      camerarenderstate.blockPos = this.mainCamera.blockPosition();
      camerarenderstate.entityPos = this.mainCamera.entity().getPosition(p_427495_);
      camerarenderstate.orientation = new Quaternionf(this.mainCamera.rotation());
   }

   private Matrix4f getProjectionMatrixForCulling(float p_429486_) {
      float f = Math.max(p_429486_, this.minecraft.options.fov().get().intValue());
      return this.getProjectionMatrix(f);
   }

   public void resetData() {
      this.screenEffectRenderer.resetItemActivation();
      this.minecraft.getMapTextureManager().resetData();
      this.mainCamera.reset();
      this.hasWorldScreenshot = false;
   }

   public void displayItemActivation(ItemStack p_109114_) {
      this.screenEffectRenderer.displayItemActivation(p_109114_, this.random);
   }

   public Minecraft getMinecraft() {
      return this.minecraft;
   }

   public float getDarkenWorldAmount(float p_109132_) {
      return Mth.lerp(p_109132_, this.darkenWorldAmountO, this.darkenWorldAmount);
   }

   public float getRenderDistance() {
      return this.renderDistance;
   }

   public Camera getMainCamera() {
      return this.mainCamera;
   }

   public LightTexture lightTexture() {
      return this.lightTexture;
   }

   public OverlayTexture overlayTexture() {
      return this.overlayTexture;
   }

   @Override
   public Vec3 projectPointToScreen(Vec3 p_407933_) {
      Matrix4f matrix4f = this.getProjectionMatrix(this.getFov(this.mainCamera, 0.0F, true));
      Quaternionf quaternionf = this.mainCamera.rotation().conjugate(new Quaternionf());
      Matrix4f matrix4f1 = new Matrix4f().rotation(quaternionf);
      Matrix4f matrix4f2 = matrix4f.mul(matrix4f1);
      Vec3 vec3 = this.mainCamera.position();
      Vec3 vec31 = p_407933_.subtract(vec3);
      Vector3f vector3f = matrix4f2.transformProject(vec31.toVector3f());
      return new Vec3(vector3f);
   }

   @Override
   public double projectHorizonToScreen() {
      float f = this.mainCamera.xRot();
      if (f <= -90.0F) {
         return Double.NEGATIVE_INFINITY;
      }

      if (f >= 90.0F) {
         return Double.POSITIVE_INFINITY;
      }

      float f1 = this.getFov(this.mainCamera, 0.0F, true);
      return Math.tan(f * (float) (Math.PI / 180.0)) / Math.tan(f1 / 2.0F * (float) (Math.PI / 180.0));
   }

   public GlobalSettingsUniform getGlobalSettingsUniform() {
      return this.globalSettingsUniform;
   }

   public Lighting getLighting() {
      return this.lighting;
   }

   public void setLevel(@Nullable ClientLevel p_405879_) {
      if (p_405879_ != null) {
         this.lighting.updateLevel(p_405879_.dimensionType().cardinalLightType());
      }
   }

   public PanoramaRenderer getPanorama() {
      return this.panorama;
   }
}
