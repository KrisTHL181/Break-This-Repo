package net.minecraft.client.renderer;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.MessageBox;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.jtracy.TracyClient;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.Screenshot;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.GuiBannerResultRenderer;
import net.minecraft.client.gui.render.pip.GuiBookModelRenderer;
import net.minecraft.client.gui.render.pip.GuiEntityRenderer;
import net.minecraft.client.gui.render.pip.GuiProfilerChartRenderer;
import net.minecraft.client.gui.render.pip.GuiSkinRenderer;
import net.minecraft.client.main.SilentInitException;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.OptionsRenderState;
import net.minecraft.client.renderer.state.WindowRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.CommonLinks;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.io.IOUtils;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class GameRenderer implements AutoCloseable, TrackedWaypoint.Projector {
    private static final Identifier BLUR_POST_CHAIN_ID = Identifier.withDefaultNamespace("blur");
    public static final int MAX_BLUR_RADIUS = 10;
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final float PROJECTION_3D_HUD_Z_FAR = 100.0F;
    private static final float PORTAL_SPINNING_SPEED = 20.0F;
    private static final float NAUSEA_SPINNING_SPEED = 7.0F;
    private final Minecraft minecraft;
    private final GameRenderState gameRenderState = new GameRenderState();
    private final RandomSource random = RandomSource.create();
    public final ItemInHandRenderer itemInHandRenderer;
    private final ScreenEffectRenderer screenEffectRenderer;
    private final DebugCrosshairRenderer debugCrosshairRenderer;
    private final RenderBuffers renderBuffers;
    private final RenderTarget mainRenderTarget;
    private float spinningEffectTime;
    private float spinningEffectSpeed;
    private float bossOverlayWorldDarkening;
    private float bossOverlayWorldDarkeningO;
    private boolean renderBlockOutline = true;
    private long lastScreenshotAttempt;
    private boolean hasWorldScreenshot;
    private final Lightmap lightmap = new Lightmap();
    private final LightmapRenderStateExtractor lightmapRenderStateExtractor;
    private final UiLightmap uiLightmap = new UiLightmap();
    private boolean useUiLightmap;
    private final OverlayTexture overlayTexture = new OverlayTexture();
    protected final Panorama panorama = new Panorama();
    private final CrossFrameResourcePool resourcePool = new CrossFrameResourcePool(3);
    private final FogRenderer fogRenderer = new FogRenderer();
    private final GuiRenderer guiRenderer;
    private final FeatureRenderDispatcher featureRenderDispatcher;
    private final SubmitNodeStorage handAndScreenSubmitNodeStorage = new SubmitNodeStorage();
    private @Nullable Identifier postEffectId;
    private boolean effectActive;
    private final Camera mainCamera = new Camera();
    private final Projection hudProjection = new Projection();
    private final Lighting lighting = new Lighting();
    private final GlobalSettingsUniform globalSettingsUniform = new GlobalSettingsUniform();
    private final ProjectionMatrixBuffer levelProjectionMatrixBuffer = new ProjectionMatrixBuffer("level");
    private final ProjectionMatrixBuffer hud3dProjectionMatrixBuffer = new ProjectionMatrixBuffer("3d hud");

    public GameRenderer(final Minecraft minecraft, final ItemInHandRenderer itemInHandRenderer, final ModelManager modelManager) {
        this.minecraft = minecraft;
        this.itemInHandRenderer = itemInHandRenderer;
        this.lightmapRenderStateExtractor = new LightmapRenderStateExtractor(this, minecraft);

        try {
            int maxSectionBuilders = Runtime.getRuntime().availableProcessors();
            this.renderBuffers = new RenderBuffers(maxSectionBuilders);
        } catch (OutOfMemoryError e) {
            MessageBox.error(
                "Oh no! The game was unable to allocate memory off-heap while trying to start. You may try to free some memory by closing other applications on your computer, check that your system meets the minimum requirements, and try again. If the problem persists, please visit: "
                    + CommonLinks.GENERAL_HELP
            );
            throw new SilentInitException("Unable to allocate render buffers", e);
        }

        AtlasManager atlasManager = minecraft.getAtlasManager();
        this.featureRenderDispatcher = new FeatureRenderDispatcher(this.renderBuffers, modelManager, atlasManager, minecraft.font, this.gameRenderState);
        this.guiRenderer = new GuiRenderer(
            this.gameRenderState.guiRenderState,
            this.featureRenderDispatcher,
            List.of(
                new GuiEntityRenderer(minecraft.getEntityRenderDispatcher()),
                new GuiSkinRenderer(),
                new GuiBookModelRenderer(),
                new GuiBannerResultRenderer(atlasManager),
                new GuiProfilerChartRenderer()
            )
        );
        this.screenEffectRenderer = new ScreenEffectRenderer(minecraft, atlasManager);
        this.debugCrosshairRenderer = new DebugCrosshairRenderer();
        this.mainRenderTarget = new MainTarget(minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
    }

    @Override
    public void close() {
        this.debugCrosshairRenderer.close();
        this.globalSettingsUniform.close();
        this.lightmap.close();
        this.overlayTexture.close();
        this.uiLightmap.close();
        this.resourcePool.close();
        this.guiRenderer.close();
        this.levelProjectionMatrixBuffer.close();
        this.hud3dProjectionMatrixBuffer.close();
        this.lighting.close();
        this.fogRenderer.close();
        this.featureRenderDispatcher.close();
        this.mainRenderTarget.destroyBuffers();
        this.renderBuffers.close();
    }

    public RenderBuffers renderBuffers() {
        return this.renderBuffers;
    }

    public FeatureRenderDispatcher featureRenderDispatcher() {
        return this.featureRenderDispatcher;
    }

    public GameRenderState gameRenderState() {
        return this.gameRenderState;
    }

    public void setRenderBlockOutline(final boolean renderBlockOutline) {
        this.renderBlockOutline = renderBlockOutline;
    }

    public void clearPostEffect() {
        this.postEffectId = null;
        this.effectActive = false;
    }

    public void togglePostEffect() {
        this.effectActive = !this.effectActive;
    }

    public void checkEntityPostEffect(final @Nullable Entity cameraEntity) {
        switch (cameraEntity) {
            case Creeper ignored:
                this.setPostEffect(Identifier.withDefaultNamespace("creeper"));
                break;
            case Spider ignored:
                this.setPostEffect(Identifier.withDefaultNamespace("spider"));
                break;
            case EnderMan ignored:
                this.setPostEffect(Identifier.withDefaultNamespace("invert"));
                break;
            case null:
            default:
                this.clearPostEffect();
        }
    }

    private void setPostEffect(final Identifier id) {
        this.postEffectId = id;
        this.effectActive = true;
    }

    public void processBlurEffect() {
        PostChain postChain = this.minecraft.getShaderManager().getPostChain(BLUR_POST_CHAIN_ID, LevelTargetBundle.MAIN_TARGETS);
        if (postChain != null) {
            postChain.process(this.mainRenderTarget, this.resourcePool);
        }
    }

    public void preloadUiShader(final ResourceProvider resourceProvider) {
        GpuDevice device = RenderSystem.getDevice();
        ShaderSource shaderSource = (id, type) -> {
            Identifier location = type.idConverter().idToFile(id);

            try (Reader reader = resourceProvider.getResourceOrThrow(location).openAsReader()) {
                return IOUtils.toString(reader);
            } catch (IOException exception) {
                LOGGER.error("Coudln't preload {} shader {}: {}", type, id, exception);
                return null;
            }
        };
        device.precompilePipeline(RenderPipelines.GUI, shaderSource);
        device.precompilePipeline(RenderPipelines.GUI_TEXTURED, shaderSource);
        if (TracyClient.isAvailable()) {
            device.precompilePipeline(RenderPipelines.TRACY_BLIT, shaderSource);
        }
    }

    public void tick() {
        this.lightmapRenderStateExtractor.tick();
        LocalPlayer player = this.minecraft.player;
        if (this.mainCamera.entity() == null) {
            this.mainCamera.setEntity(player);
        }

        this.mainCamera.tick();
        this.itemInHandRenderer.tick();
        float portalIntensity = player.portalEffectIntensity;
        float nauseaIntensity = player.getEffectBlendFactor(MobEffects.NAUSEA, 1.0F);
        if (!(portalIntensity > 0.0F) && !(nauseaIntensity > 0.0F)) {
            this.spinningEffectSpeed = 0.0F;
        } else {
            this.spinningEffectSpeed = (portalIntensity * 20.0F + nauseaIntensity * 7.0F) / (portalIntensity + nauseaIntensity);
            this.spinningEffectTime = this.spinningEffectTime + this.spinningEffectSpeed;
        }

        if (this.minecraft.level.tickRateManager().runsNormally()) {
            this.bossOverlayWorldDarkeningO = this.bossOverlayWorldDarkening;
            if (this.minecraft.gui.hud.getBossOverlay().shouldDarkenScreen()) {
                this.bossOverlayWorldDarkening += 0.05F;
                if (this.bossOverlayWorldDarkening > 1.0F) {
                    this.bossOverlayWorldDarkening = 1.0F;
                }
            } else if (this.bossOverlayWorldDarkening > 0.0F) {
                this.bossOverlayWorldDarkening -= 0.0125F;
            }

            this.screenEffectRenderer.tick();
        }
    }

    public @Nullable Identifier currentPostEffect() {
        return this.postEffectId;
    }

    public void resize(final int width, final int height) {
        this.resourcePool.clear();
        this.mainRenderTarget.resize(width, height);
        this.minecraft.levelRenderer.resize(width, height);
    }

    private void bobHurt(final CameraRenderState cameraState, final PoseStack poseStack) {
        if (cameraState.entityRenderState.isLiving) {
            float hurt = cameraState.entityRenderState.hurtTime;
            if (cameraState.entityRenderState.isDeadOrDying) {
                float duration = Math.min(cameraState.entityRenderState.deathTime, 20.0F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(40.0F - 8000.0F / (duration + 200.0F)));
            }

            if (hurt < 0.0F) {
                return;
            }

            hurt /= cameraState.entityRenderState.hurtDuration;
            hurt = Mth.sin(hurt * hurt * hurt * hurt * (float) Math.PI);
            float rr = cameraState.entityRenderState.hurtDir;
            poseStack.mulPose(Axis.YP.rotationDegrees(-rr));
            float tiltAmount = (float)(-hurt * 14.0 * this.gameRenderState.optionsRenderState.damageTiltStrength);
            poseStack.mulPose(Axis.ZP.rotationDegrees(tiltAmount));
            poseStack.mulPose(Axis.YP.rotationDegrees(rr));
        }
    }

    private void bobView(final CameraRenderState cameraState, final PoseStack poseStack) {
        if (cameraState.entityRenderState.isPlayer) {
            float backwardsInterpolatedWalkDistance = cameraState.entityRenderState.backwardsInterpolatedWalkDistance;
            float bob = cameraState.entityRenderState.bob;
            poseStack.translate(
                Mth.sin(backwardsInterpolatedWalkDistance * (float) Math.PI) * bob * 0.5F,
                -Math.abs(Mth.cos(backwardsInterpolatedWalkDistance * (float) Math.PI) * bob),
                0.0F
            );
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(backwardsInterpolatedWalkDistance * (float) Math.PI) * bob * 3.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(Math.abs(Mth.cos(backwardsInterpolatedWalkDistance * (float) Math.PI - 0.2F) * bob) * 5.0F));
        }
    }

    private void renderItemInHand(final CameraRenderState cameraState, final float deltaPartialTick, final Matrix4fc modelViewMatrix) {
        if (!cameraState.isPanoramicMode) {
            if (this.gameRenderState.optionsRenderState.cameraType.isFirstPerson()
                && !cameraState.entityRenderState.isSleeping
                && !this.gameRenderState.guiRenderState.isHudHidden
                && this.minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR) {
                PoseStack poseStack = new PoseStack();
                poseStack.pushPose();
                poseStack.mulPose(modelViewMatrix.invert(new Matrix4f()));
                Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
                modelViewStack.pushMatrix().mul(modelViewMatrix);
                this.bobHurt(cameraState, poseStack);
                if (this.gameRenderState.optionsRenderState.bobView) {
                    this.bobView(cameraState, poseStack);
                }

                this.itemInHandRenderer
                    .submitHandsWithItems(
                        deltaPartialTick,
                        poseStack,
                        this.handAndScreenSubmitNodeStorage,
                        this.minecraft.player,
                        this.minecraft.getEntityRenderDispatcher().getPackedLightCoords(this.minecraft.player, deltaPartialTick)
                    );
                this.featureRenderDispatcher.renderAllFeatures(this.handAndScreenSubmitNodeStorage);
                modelViewStack.popMatrix();
                poseStack.popPose();
            }
        }
    }

    public static float nightVisionScale(final LivingEntity camera, final float a) {
        MobEffectInstance nightVision = camera.getEffect(MobEffects.NIGHT_VISION);
        return !nightVision.endsWithin(200) ? 1.0F : 0.7F + Mth.sin((nightVision.getDuration() - a) * (float) Math.PI * 0.2F) * 0.3F;
    }

    public void update(final DeltaTracker deltaTracker) {
        ProfilerFiller profiler = Profiler.get();
        profiler.push("camera");
        this.mainCamera.update(deltaTracker);
        profiler.pop();
    }

    public void extract(final DeltaTracker deltaTracker, final boolean advanceGameTime) {
        boolean resourcesLoaded = this.minecraft.isGameLoadFinished();
        boolean readyForLevelRendering = resourcesLoaded && advanceGameTime && this.minecraft.level != null;
        float worldPartialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);
        this.extractWindow();
        this.extractOptions();
        if (readyForLevelRendering) {
            this.lightmapRenderStateExtractor.extract(this.gameRenderState.lightmapRenderState, 1.0F);
            float cameraEntityPartialTicks = this.mainCamera.getCameraEntityPartialTicks(deltaTracker);
            this.extractCamera(deltaTracker, worldPartialTicks, cameraEntityPartialTicks);
            this.minecraft.levelExtractor.extract(deltaTracker, this.mainCamera, worldPartialTicks);
        }

        this.minecraft.gui.extractRenderState(deltaTracker, readyForLevelRendering, resourcesLoaded);
        this.minecraft.getMetricsRecorder().sampleDuringExtract();
    }

    public void render(final DeltaTracker deltaTracker, final boolean advanceGameTime) {
        ProfilerFiller profiler = Profiler.get();
        profiler.push("render");
        WindowRenderState windowRenderState = this.gameRenderState.windowRenderState;
        if (windowRenderState.width != this.mainRenderTarget.width || windowRenderState.height != this.mainRenderTarget.height) {
            this.resize(windowRenderState.width, windowRenderState.height);
        }

        RenderSystem.getDevice()
            .createCommandEncoder()
            .clearColorAndDepthTextures(
                this.mainRenderTarget.getColorTexture(), this.gameRenderState.guiRenderState.clearColorOverride, this.mainRenderTarget.getDepthTexture(), 0.0
            );
        boolean resourcesLoaded = this.minecraft.isGameLoadFinished();
        boolean shouldRenderLevel = resourcesLoaded && advanceGameTime && this.minecraft.level != null;
        this.globalSettingsUniform
            .update(
                windowRenderState.width,
                windowRenderState.height,
                this.gameRenderState.optionsRenderState.glintStrength,
                this.minecraft.level == null ? 0L : this.minecraft.level.getGameTime(),
                deltaTracker,
                this.gameRenderState.optionsRenderState.menuBackgroundBlurriness,
                this.gameRenderState.levelRenderState.cameraRenderState.pos,
                this.gameRenderState.optionsRenderState.textureFiltering == TextureFilteringMethod.RGSS
            );
        if (shouldRenderLevel) {
            this.lightmap.render(this.gameRenderState.lightmapRenderState);
            profiler.push("world");
            this.renderLevel(deltaTracker);
            this.tryTakeScreenshotIfNeeded();
            this.minecraft.levelRenderer.doEntityOutline();
            if (this.postEffectId != null && this.effectActive) {
                PostChain postChain = this.minecraft.getShaderManager().getPostChain(this.postEffectId, LevelTargetBundle.MAIN_TARGETS);
                if (postChain != null) {
                    postChain.process(this.mainRenderTarget, this.resourcePool);
                }
            }

            profiler.pop();
        }

        this.fogRenderer.endFrame();
        RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(this.mainRenderTarget.getDepthTexture(), 0.0);
        this.lighting().setupFor(Lighting.Entry.ITEMS_3D);
        this.useUiLightmap = true;
        profiler.push("gui");
        this.guiRenderer.render();
        this.guiRenderer.endFrame();
        profiler.pop();
        this.useUiLightmap = false;
        this.renderBuffers.endFrame();
        this.resourcePool.endFrame();
        profiler.pop();
    }

    private void tryTakeScreenshotIfNeeded() {
        if (!this.hasWorldScreenshot && this.minecraft.isLocalServer()) {
            long time = Util.getMillis();
            if (time - this.lastScreenshotAttempt >= 1000L) {
                this.lastScreenshotAttempt = time;
                IntegratedServer server = this.minecraft.getSingleplayerServer();
                if (server != null && !server.isStopped()) {
                    server.getWorldScreenshotFile().ifPresent(path -> {
                        if (Files.isRegularFile(path)) {
                            this.hasWorldScreenshot = true;
                        } else {
                            this.takeAutoScreenshot(path);
                        }
                    });
                }
            }
        }
    }

    private void takeAutoScreenshot(final Path screenshotFile) {
        if (this.minecraft.levelExtractor.countRenderedSections() > 10 && this.minecraft.levelRenderer.hasRenderedAllSections()) {
            Screenshot.takeScreenshot(this.mainRenderTarget, screenshot -> Util.ioPool().execute(() -> {
                int width = screenshot.getWidth();
                int height = screenshot.getHeight();
                int x = 0;
                int y = 0;
                if (width > height) {
                    x = (width - height) / 2;
                    width = height;
                } else {
                    y = (height - width) / 2;
                    height = width;
                }

                try (NativeImage scaled = new NativeImage(64, 64, false)) {
                    screenshot.resizeSubRectTo(x, y, width, height, scaled);
                    scaled.writeToFile(screenshotFile);
                } catch (IOException e) {
                    LOGGER.warn("Couldn't save auto screenshot", e);
                } finally {
                    screenshot.close();
                }
            }));
        }
    }

    private boolean shouldRenderBlockOutline() {
        if (!this.renderBlockOutline) {
            return false;
        }

        Entity cameraEntity = this.minecraft.getCameraEntity();
        boolean renderOutline = cameraEntity instanceof Player && !this.minecraft.gui.hud.isHidden();
        if (renderOutline && !((Player)cameraEntity).getAbilities().mayBuild) {
            ItemStack itemStack = ((LivingEntity)cameraEntity).getMainHandItem();
            HitResult hitResult = this.minecraft.hitResult;
            if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = ((BlockHitResult)hitResult).getBlockPos();
                BlockState blockState = this.minecraft.level.getBlockState(pos);
                if (this.minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR) {
                    renderOutline = blockState.getMenuProvider(this.minecraft.level, pos) != null;
                } else {
                    BlockInWorld blockInWorld = new BlockInWorld(this.minecraft.level, pos, false);
                    Registry<Block> blockRegistry = this.minecraft.level.registryAccess().lookupOrThrow(Registries.BLOCK);
                    renderOutline = !itemStack.isEmpty()
                        && (itemStack.canBreakBlockInAdventureMode(blockInWorld) || itemStack.canPlaceOnBlockInAdventureMode(blockInWorld));
                }
            }
        }

        return renderOutline;
    }

    public void renderLevel(final DeltaTracker deltaTracker) {
        float worldPartialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);
        float cameraEntityPartialTicks = this.mainCamera.getCameraEntityPartialTicks(deltaTracker);
        LocalPlayer player = this.minecraft.player;
        ProfilerFiller profiler = Profiler.get();
        boolean renderOutline = this.shouldRenderBlockOutline();
        OptionsRenderState optionsState = this.gameRenderState.optionsRenderState;
        CameraRenderState cameraState = this.gameRenderState.levelRenderState.cameraRenderState;
        Matrix4fc modelViewMatrix = cameraState.viewRotationMatrix;
        profiler.push("matrices");
        Matrix4f projectionMatrix = new Matrix4f(cameraState.projectionMatrix);
        PoseStack bobStack = new PoseStack();
        this.bobHurt(cameraState, bobStack);
        if (optionsState.bobView) {
            this.bobView(cameraState, bobStack);
        }

        projectionMatrix.mul(bobStack.last().pose());
        float screenEffectScale = optionsState.screenEffectScale;
        float portalIntensity = Mth.lerp(worldPartialTicks, player.oPortalEffectIntensity, player.portalEffectIntensity);
        float nauseaIntensity = player.getEffectBlendFactor(MobEffects.NAUSEA, worldPartialTicks);
        float spinningEffectIntensity = Math.max(portalIntensity, nauseaIntensity) * (screenEffectScale * screenEffectScale);
        if (spinningEffectIntensity > 0.0F) {
            float skew = 5.0F / (spinningEffectIntensity * spinningEffectIntensity + 5.0F) - spinningEffectIntensity * 0.04F;
            skew *= skew;
            Vector3f axis = new Vector3f(0.0F, Mth.SQRT_OF_TWO / 2.0F, Mth.SQRT_OF_TWO / 2.0F);
            float angle = (this.spinningEffectTime + worldPartialTicks * this.spinningEffectSpeed) * (float) (Math.PI / 180.0);
            projectionMatrix.rotate(angle, axis);
            projectionMatrix.scale(1.0F / skew, 1.0F, 1.0F);
            projectionMatrix.rotate(-angle, axis);
        }

        RenderSystem.setProjectionMatrix(this.levelProjectionMatrixBuffer.getBuffer(projectionMatrix), ProjectionType.PERSPECTIVE);
        profiler.popPush("fog");
        this.fogRenderer.updateBuffer(cameraState.fogData);
        GpuBufferSlice terrainFog = this.fogRenderer.getBuffer(FogRenderer.FogMode.WORLD);
        profiler.popPush("level");
        boolean shouldCreateBossFog = this.minecraft.gui.hud.getBossOverlay().shouldCreateWorldFog();
        this.minecraft
            .levelRenderer
            .render(this.resourcePool, deltaTracker, renderOutline, cameraState, modelViewMatrix, terrainFog, cameraState.fogData.color, !shouldCreateBossFog);
        profiler.popPush("hand");
        boolean isSleeping = cameraState.entityRenderState.isSleeping;
        this.hudProjection
            .setupPerspective(0.05F, 100.0F, cameraState.hudFov, this.gameRenderState.windowRenderState.width, this.gameRenderState.windowRenderState.height);
        RenderSystem.setProjectionMatrix(this.hud3dProjectionMatrixBuffer.getBuffer(this.hudProjection), ProjectionType.PERSPECTIVE);
        RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(this.mainRenderTarget.getDepthTexture(), 0.0);
        this.renderItemInHand(cameraState, cameraEntityPartialTicks, modelViewMatrix);
        profiler.popPush("screenEffects");
        this.screenEffectRenderer
            .submit(
                optionsState.cameraType.isFirstPerson(),
                isSleeping,
                worldPartialTicks,
                this.handAndScreenSubmitNodeStorage,
                this.gameRenderState.guiRenderState.isHudHidden
            );
        this.featureRenderDispatcher.renderAllFeatures(this.handAndScreenSubmitNodeStorage);
        profiler.pop();
        RenderSystem.setShaderFog(this.fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
        if (this.gameRenderState.levelRenderState.render3dCrosshair
            && optionsState.cameraType.isFirstPerson()
            && !this.gameRenderState.guiRenderState.isHudHidden) {
            this.debugCrosshairRenderer.render(cameraState, this.gameRenderState.windowRenderState.guiScale);
        }
    }

    private void extractWindow() {
        WindowRenderState windowState = this.gameRenderState.windowRenderState;
        Window window = this.minecraft.getWindow();
        windowState.width = window.getWidth();
        windowState.height = window.getHeight();
        windowState.guiScale = window.getGuiScale();
        windowState.appropriateLineWidth = window.getAppropriateLineWidth();
        windowState.isMinimized = window.isMinimized();
    }

    private void extractOptions() {
        OptionsRenderState optionsState = this.gameRenderState.optionsRenderState;
        Options options = this.minecraft.options;
        optionsState.cloudRange = options.cloudRange().get();
        optionsState.cutoutLeaves = options.cutoutLeaves().get();
        optionsState.improvedTransparency = options.improvedTransparency().get();
        optionsState.ambientOcclusion = options.ambientOcclusion().get();
        optionsState.menuBackgroundBlurriness = options.getMenuBackgroundBlurriness();
        optionsState.panoramaSpeed = options.panoramaSpeed().get();
        optionsState.maxAnisotropyValue = options.maxAnisotropyValue();
        optionsState.textureFiltering = options.textureFiltering().get();
        optionsState.bobView = options.bobView().get();
        optionsState.screenEffectScale = options.screenEffectScale().get().floatValue();
        optionsState.glintSpeed = options.glintSpeed().get();
        optionsState.glintStrength = options.glintStrength().get();
        optionsState.damageTiltStrength = options.damageTiltStrength().get();
        optionsState.backgroundForChatOnly = options.backgroundForChatOnly().get();
        optionsState.textBackgroundOpacity = options.textBackgroundOpacity().get().floatValue();
        optionsState.cloudStatus = options.getCloudStatus();
        optionsState.cameraType = options.getCameraType();
        optionsState.renderDistance = options.getEffectiveRenderDistance();
        optionsState.chunkSectionFadeInTime = options.chunkSectionFadeInTime().get();
        optionsState.prioritizeChunkUpdates = options.prioritizeChunkUpdates().get();
        optionsState.fov = options.fov().get();
    }

    private void extractCamera(final DeltaTracker deltaTracker, final float worldPartialTicks, final float cameraEntityPartialTicks) {
        CameraRenderState cameraState = this.gameRenderState.levelRenderState.cameraRenderState;
        this.mainCamera.extractRenderState(cameraState, cameraEntityPartialTicks);
        cameraState.fogType = this.mainCamera.getFluidInCamera();
        cameraState.fogData = this.fogRenderer
            .setupFog(
                this.mainCamera,
                this.minecraft.options.getEffectiveRenderDistance(),
                deltaTracker,
                this.bossOverlayWorldDarkening(worldPartialTicks),
                this.minecraft.level
            );
    }

    public void resetData() {
        this.screenEffectRenderer.resetItemActivation();
        this.minecraft.getMapTextureManager().resetData();
        this.mainCamera.reset();
        this.hasWorldScreenshot = false;
    }

    public void displayItemActivation(final ItemStack itemStack) {
        this.screenEffectRenderer.displayItemActivation(itemStack, this.random);
    }

    public float bossOverlayWorldDarkening(final float a) {
        return Mth.lerp(a, this.bossOverlayWorldDarkeningO, this.bossOverlayWorldDarkening);
    }

    public Camera mainCamera() {
        return this.mainCamera;
    }

    public GpuTextureView lightmap() {
        return this.useUiLightmap ? this.uiLightmap.getTextureView() : this.lightmap.getTextureView();
    }

    public GpuTextureView levelLightmap() {
        return this.lightmap.getTextureView();
    }

    public OverlayTexture overlayTexture() {
        return this.overlayTexture;
    }

    public RenderTarget mainRenderTarget() {
        return this.mainRenderTarget;
    }

    @Override
    public Vec3 projectPointToScreen(final Vec3 point) {
        Matrix4f mvp = this.mainCamera.getViewRotationProjectionMatrix(new Matrix4f());
        Vec3 camPos = this.mainCamera.position();
        Vec3 offset = point.subtract(camPos);
        Vector3f vector3f = mvp.transformProject(offset.toVector3f());
        return new Vec3(vector3f);
    }

    @Override
    public double projectHorizonToScreen() {
        float xRot = this.mainCamera.xRot();
        if (xRot <= -90.0F) {
            return Double.NEGATIVE_INFINITY;
        }

        if (xRot >= 90.0F) {
            return Double.POSITIVE_INFINITY;
        }

        float fov = this.mainCamera.getFov();
        return Math.tan(xRot * (float) (Math.PI / 180.0)) / Math.tan(fov / 2.0F * (float) (Math.PI / 180.0));
    }

    public Lighting lighting() {
        return this.lighting;
    }

    public void setLevel(final @Nullable ClientLevel level) {
        if (level != null) {
            this.lighting.updateLevel(level.dimensionType().cardinalLightType());
        }

        this.mainCamera.setLevel(level);
    }

    public Panorama panorama() {
        return this.panorama;
    }

    public void registerPanoramaTextures(final TextureManager textureManager) {
        this.guiRenderer.registerPanoramaTextures(textureManager);
    }
}