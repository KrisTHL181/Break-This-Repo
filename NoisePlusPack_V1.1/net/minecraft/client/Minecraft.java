package net.minecraft.client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.BanDetails;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.UserApiService.UserFlag;
import com.mojang.authlib.minecraft.UserApiService.UserProperties;
import com.mojang.authlib.yggdrasil.ProfileActionType;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.blaze3d.TracyFrameCapture;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.ClientShutdownWatchdog;
import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.FramerateLimitTracker;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.IconSet;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.TimerQuery;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.datafixers.DataFixer;
import com.mojang.jtracy.DiscontinuousFrame;
import com.mojang.jtracy.TracyClient;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import dev.miru.main.ModMain;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.net.Proxy;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.Optionull;
import net.minecraft.ReportType;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.SystemReport;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import net.minecraft.client.gui.components.debugchart.ProfilerPieChart;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.BanNoticeScreens;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.OutOfMemoryScreen;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.main.SilentInitException;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleResources;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.LocalPlayerResolver;
import net.minecraft.client.profiling.ClientMetricsSamplersProvider;
import net.minecraft.client.quickplay.QuickPlay;
import net.minecraft.client.quickplay.QuickPlayLog;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.GpuWarnlistManager;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.PanoramicScreenshotParameters;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.client.renderer.VirtualScreen;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.ClientPackSource;
import net.minecraft.client.resources.DryFoliageColorReloadListener;
import net.minecraft.client.resources.FoliageColorReloadListener;
import net.minecraft.client.resources.GrassColorReloadListener;
import net.minecraft.client.resources.MapTextureManager;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.client.resources.WaypointStyleManager;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.telemetry.ClientTelemetryManager;
import net.minecraft.client.telemetry.TelemetryProperty;
import net.minecraft.client.telemetry.events.GameLoadTimesEvent;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.SimpleGizmoCollector;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketProcessor;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.KeybindResolver;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.Dialogs;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.server.level.progress.LoggingLevelLoadListener;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DialogTags;
import net.minecraft.util.CommonLinks;
import net.minecraft.util.FileUtil;
import net.minecraft.util.FileZipper;
import net.minecraft.util.MemoryReserve;
import net.minecraft.util.ModCheck;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.profiling.ContinuousProfiler;
import net.minecraft.util.profiling.EmptyProfileResults;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.SingleTickProfiler;
import net.minecraft.util.profiling.Zone;
import net.minecraft.util.profiling.metrics.profiling.ActiveMetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.InactiveMetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.MetricsRecorder;
import net.minecraft.util.profiling.metrics.storage.MetricsPersister;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.io.FileUtils;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class Minecraft extends ReentrantBlockableEventLoop<Runnable> implements WindowEventHandler {
   static Minecraft instance;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_TICKS_PER_UPDATE = 10;
   public static final Identifier DEFAULT_FONT = Identifier.withDefaultNamespace("default");
   public static final Identifier UNIFORM_FONT = Identifier.withDefaultNamespace("uniform");
   public static final Identifier ALT_FONT = Identifier.withDefaultNamespace("alt");
   private static final Identifier REGIONAL_COMPLIANCIES = Identifier.withDefaultNamespace("regional_compliancies.json");
   private static final CompletableFuture<Unit> RESOURCE_RELOAD_INITIAL_TASK = CompletableFuture.completedFuture(Unit.INSTANCE);
   private static final Component SOCIAL_INTERACTIONS_NOT_AVAILABLE = Component.translatable("multiplayer.socialInteractions.not_available");
   private static final Component SAVING_LEVEL = Component.translatable("menu.savingLevel");
   public static final String UPDATE_DRIVERS_ADVICE = "Please make sure you have up-to-date drivers (see aka.ms/mcdriver for instructions).";
   private final long canary = Double.doubleToLongBits(Math.PI);
   private final Path resourcePackDirectory;
   private final CompletableFuture<@Nullable ProfileResult> profileFuture;
   private final TextureManager textureManager;
   private final ShaderManager shaderManager;
   private final DataFixer fixerUpper;
   private final VirtualScreen virtualScreen;
   private final Window window;
   private final DeltaTracker.Timer deltaTracker = new DeltaTracker.Timer(20.0F, 0L, this::getTickTargetMillis);
   private final RenderBuffers renderBuffers;
   public final LevelRenderer levelRenderer;
   private final EntityRenderDispatcher entityRenderDispatcher;
   private final ItemModelResolver itemModelResolver;
   private final ItemRenderer itemRenderer;
   private final MapRenderer mapRenderer;
   public final ParticleEngine particleEngine;
   private final ParticleResources particleResources;
   private final User user;
   public final Font font;
   public final Font fontFilterFishy;
   public final GameRenderer gameRenderer;
   public final Gui gui;
   public final Options options;
   public final DebugScreenEntryList debugEntries;
   private final HotbarManager hotbarManager;
   public final MouseHandler mouseHandler;
   public final KeyboardHandler keyboardHandler;
   private InputType lastInputType = InputType.NONE;
   public final File gameDirectory;
   private final String launchedVersion;
   private final String versionType;
   private final Proxy proxy;
   private final boolean offlineDeveloperMode;
   private final LevelStorageSource levelSource;
   private final boolean demo;
   private final boolean allowsMultiplayer;
   private final boolean allowsChat;
   private final ReloadableResourceManager resourceManager;
   private final VanillaPackResources vanillaPackResources;
   private final DownloadedPackSource downloadedPackSource;
   private final PackRepository resourcePackRepository;
   private final LanguageManager languageManager;
   private final BlockColors blockColors;
   private final RenderTarget mainRenderTarget;
   private final @Nullable TracyFrameCapture tracyFrameCapture;
   private final SoundManager soundManager;
   private final MusicManager musicManager;
   private final FontManager fontManager;
   private final SplashManager splashManager;
   private final GpuWarnlistManager gpuWarnlistManager;
   private final PeriodicNotificationManager regionalCompliancies = new PeriodicNotificationManager(REGIONAL_COMPLIANCIES, Minecraft::countryEqualsISO3);
   private final UserApiService userApiService;
   private final CompletableFuture<UserProperties> userPropertiesFuture;
   private final SkinManager skinManager;
   private final AtlasManager atlasManager;
   private final ModelManager modelManager;
   private final BlockRenderDispatcher blockRenderer;
   private final MapTextureManager mapTextureManager;
   private final WaypointStyleManager waypointStyles;
   private final ToastManager toastManager;
   private final Tutorial tutorial;
   private final PlayerSocialManager playerSocialManager;
   private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
   private final ClientTelemetryManager telemetryManager;
   private final ProfileKeyPairManager profileKeyPairManager;
   private final RealmsDataFetcher realmsDataFetcher;
   private final QuickPlayLog quickPlayLog;
   private final Services services;
   private final PlayerSkinRenderCache playerSkinRenderCache;
   public @Nullable MultiPlayerGameMode gameMode;
   public @Nullable ClientLevel level;
   public @Nullable LocalPlayer player;
   private @Nullable IntegratedServer singleplayerServer;
   private @Nullable Connection pendingConnection;
   private boolean isLocalServer;
   private @Nullable Entity cameraEntity;
   public @Nullable Entity crosshairPickEntity;
   public @Nullable HitResult hitResult;
   private int rightClickDelay;
   protected int missTime;
   private volatile boolean pause;
   private long lastNanoTime = Util.getNanos();
   private long lastTime;
   private int frames;
   public boolean noRender;
   public @Nullable Screen screen;
   private @Nullable Overlay overlay;
   private boolean clientLevelTeardownInProgress;
   Thread gameThread;
   private volatile boolean running;
   private @Nullable Supplier<CrashReport> delayedCrash;
   private static int fps;
   private long frameTimeNs;
   private final FramerateLimitTracker framerateLimitTracker;
   public boolean wireframe;
   public boolean smartCull = true;
   private boolean windowActive;
   private @Nullable CompletableFuture<Void> pendingReload;
   private @Nullable TutorialToast socialInteractionsToast;
   private int fpsPieRenderTicks;
   private final ContinuousProfiler fpsPieProfiler;
   private MetricsRecorder metricsRecorder = InactiveMetricsRecorder.INSTANCE;
   private final ResourceLoadStateTracker reloadStateTracker = new ResourceLoadStateTracker();
   private long savedCpuDuration;
   private double gpuUtilization;
   private TimerQuery.@Nullable FrameProfile currentFrameProfile;
   private final GameNarrator narrator;
   private final ChatListener chatListener;
   private ReportingContext reportingContext;
   private final CommandHistory commandHistory;
   private final DirectoryValidator directoryValidator;
   private boolean gameLoadFinished;
   private final long clientStartTimeMs;
   private long clientTickCount;
   private final PacketProcessor packetProcessor;
   private final SimpleGizmoCollector perTickGizmos = new SimpleGizmoCollector();
   private List<SimpleGizmoCollector.GizmoInstance> drainedLatestTickGizmos = new ArrayList<>();

   public Minecraft(final GameConfig p_91084_) {
      super("Client");
      instance = this;
      this.clientStartTimeMs = System.currentTimeMillis();
      this.gameDirectory = p_91084_.location.gameDirectory;
      File file1 = p_91084_.location.assetDirectory;
      this.resourcePackDirectory = p_91084_.location.resourcePackDirectory.toPath();
      this.launchedVersion = p_91084_.game.launchVersion;
      this.versionType = p_91084_.game.versionType;
      Path path = this.gameDirectory.toPath();
      this.directoryValidator = LevelStorageSource.parseValidator(path.resolve("allowed_symlinks.txt"));
      ClientPackSource clientpacksource = new ClientPackSource(p_91084_.location.getExternalAssetSource(), this.directoryValidator);
      this.downloadedPackSource = new DownloadedPackSource(this, path.resolve("downloads"), p_91084_.user);
      RepositorySource repositorysource = new FolderRepositorySource(
         this.resourcePackDirectory, PackType.CLIENT_RESOURCES, PackSource.DEFAULT, this.directoryValidator
      );
      this.resourcePackRepository = new PackRepository(clientpacksource, this.downloadedPackSource.createRepositorySource(), repositorysource);
      this.vanillaPackResources = clientpacksource.getVanillaPack();
      this.proxy = p_91084_.user.proxy;
      this.offlineDeveloperMode = true;
      YggdrasilAuthenticationService yggdrasilauthenticationservice = this.offlineDeveloperMode
         ? YggdrasilAuthenticationService.createOffline(this.proxy)
         : new YggdrasilAuthenticationService(this.proxy);
      this.services = Services.create(yggdrasilauthenticationservice, this.gameDirectory);
      this.user = p_91084_.user.user;
      this.profileFuture = this.offlineDeveloperMode
         ? CompletableFuture.completedFuture(null)
         : CompletableFuture.supplyAsync(() -> this.services.sessionService().fetchProfile(this.user.getProfileId(), true), Util.nonCriticalIoPool());
      this.userApiService = this.createUserApiService(yggdrasilauthenticationservice, p_91084_);
      this.userPropertiesFuture = CompletableFuture.supplyAsync(() -> {
         try {
            return this.userApiService.fetchProperties();
         } catch (AuthenticationException authenticationexception) {
            LOGGER.error("Failed to fetch user properties", authenticationexception);
            return UserApiService.OFFLINE_PROPERTIES;
         }
      }, Util.nonCriticalIoPool());
      LOGGER.info("Setting user: {}", this.user.getName());
      LOGGER.debug("(Session ID is {})", this.user.getSessionId());
      this.demo = p_91084_.game.demo;
      this.allowsMultiplayer = !p_91084_.game.disableMultiplayer;
      this.allowsChat = !p_91084_.game.disableChat;
      this.singleplayerServer = null;
      KeybindResolver.setKeyResolver(KeyMapping::createNameSupplier);
      this.fixerUpper = DataFixers.getDataFixer();
      this.gameThread = Thread.currentThread();
      this.options = new Options(this, this.gameDirectory);
      this.debugEntries = new DebugScreenEntryList(this.gameDirectory);
      this.toastManager = new ToastManager(this, this.options);
      boolean flag = this.options.startedCleanly;
      this.options.startedCleanly = false;
      this.options.save();
      this.running = true;
      this.tutorial = new Tutorial(this, this.options);
      this.hotbarManager = new HotbarManager(path, this.fixerUpper);
      LOGGER.info("Backend library: {}", RenderSystem.getBackendDescription());
      DisplayData displaydata = p_91084_.display;
      if (this.options.overrideHeight > 0 && this.options.overrideWidth > 0) {
         displaydata = p_91084_.display.withSize(this.options.overrideWidth, this.options.overrideHeight);
      }

      if (!flag) {
         displaydata = displaydata.withFullscreen(false);
         this.options.fullscreenVideoModeString = null;
         LOGGER.warn("Detected unexpected shutdown during last game startup: resetting fullscreen mode");
      }

      Util.timeSource = RenderSystem.initBackendSystem();
      this.virtualScreen = new VirtualScreen(this);
      this.window = this.virtualScreen.newWindow(displaydata, this.options.fullscreenVideoModeString, this.createTitle());
      this.setWindowActive(true);
      this.window.setWindowCloseCallback(new Runnable() {
         private boolean threadStarted;

         @Override
         public void run() {
            if (!this.threadStarted) {
               this.threadStarted = true;
               ClientShutdownWatchdog.startShutdownWatchdog(p_91084_.location.gameDirectory, Minecraft.this.gameThread.threadId());
            }
         }
      });
      GameLoadTimesEvent.INSTANCE.endStep(TelemetryProperty.LOAD_TIME_PRE_WINDOW_MS);

      try {
         this.window.setIcon(this.vanillaPackResources, SharedConstants.getCurrentVersion().stable() ? IconSet.RELEASE : IconSet.SNAPSHOT);
      } catch (IOException ioexception) {
         LOGGER.error("Couldn't set icon", ioexception);
      }

      this.mouseHandler = new MouseHandler(this);
      this.mouseHandler.setup(this.window);
      this.keyboardHandler = new KeyboardHandler(this);
      this.keyboardHandler.setup(this.window);
      RenderSystem.initRenderer(
         this.window.handle(),
         this.options.glDebugVerbosity,
         SharedConstants.DEBUG_SYNCHRONOUS_GL_LOGS,
         (p_447787_, p_447788_) -> this.getShaderManager().getShader(p_447787_, p_447788_),
         p_91084_.game.renderDebugLabels
      );
      this.options.applyGraphicsPreset(this.options.graphicsPreset().get());
      LOGGER.info("Using optional rendering extensions: {}", String.join(", ", RenderSystem.getDevice().getEnabledExtensions()));
      this.mainRenderTarget = new MainTarget(this.window.getWidth(), this.window.getHeight());
      this.resourceManager = new ReloadableResourceManager(PackType.CLIENT_RESOURCES);
      this.resourcePackRepository.reload();
      this.options.loadSelectedResourcePacks(this.resourcePackRepository);
      this.languageManager = new LanguageManager(this.options.languageCode, p_340763_ -> {
         if (this.player != null) {
            this.player.connection.updateSearchTrees();
         }
      });
      this.resourceManager.registerReloadListener(this.languageManager);
      this.textureManager = new TextureManager(this.resourceManager);
      this.resourceManager.registerReloadListener(this.textureManager);
      this.shaderManager = new ShaderManager(this.textureManager, this::triggerResourcePackRecovery);
      this.resourceManager.registerReloadListener(this.shaderManager);
      SkinTextureDownloader skintexturedownloader = new SkinTextureDownloader(this.proxy, this.textureManager, this);
      this.skinManager = new SkinManager(file1.toPath().resolve("skins"), this.services, skintexturedownloader, this);
      this.levelSource = new LevelStorageSource(path.resolve("saves"), path.resolve("backups"), this.directoryValidator, this.fixerUpper);
      this.commandHistory = new CommandHistory(path);
      this.musicManager = new MusicManager(this);
      this.soundManager = new SoundManager(this.options);
      this.resourceManager.registerReloadListener(this.soundManager);
      this.splashManager = new SplashManager(this.user);
      this.resourceManager.registerReloadListener(this.splashManager);
      this.atlasManager = new AtlasManager(this.textureManager, this.options.mipmapLevels().get());
      this.resourceManager.registerReloadListener(this.atlasManager);
      ProfileResolver profileresolver = new LocalPlayerResolver(this, this.services.profileResolver());
      this.playerSkinRenderCache = new PlayerSkinRenderCache(this.textureManager, this.skinManager, profileresolver);
      ClientMannequin.registerOverrides(this.playerSkinRenderCache);
      this.fontManager = new FontManager(this.textureManager, this.atlasManager, this.playerSkinRenderCache);
      this.font = this.fontManager.createFont();
      this.fontFilterFishy = this.fontManager.createFontFilterFishy();
      this.resourceManager.registerReloadListener(this.fontManager);
      this.updateFontOptions();
      this.resourceManager.registerReloadListener(new GrassColorReloadListener());
      this.resourceManager.registerReloadListener(new FoliageColorReloadListener());
      this.resourceManager.registerReloadListener(new DryFoliageColorReloadListener());
      this.window.setErrorSection("Startup");
      RenderSystem.setupDefaultState();
      this.window.setErrorSection("Post startup");
      this.blockColors = BlockColors.createDefault();
      this.modelManager = new ModelManager(this.blockColors, this.atlasManager, this.playerSkinRenderCache);
      this.resourceManager.registerReloadListener(this.modelManager);
      EquipmentAssetManager equipmentassetmanager = new EquipmentAssetManager();
      this.resourceManager.registerReloadListener(equipmentassetmanager);
      this.itemModelResolver = new ItemModelResolver(this.modelManager);
      this.itemRenderer = new ItemRenderer();
      this.mapTextureManager = new MapTextureManager(this.textureManager);
      this.mapRenderer = new MapRenderer(this.atlasManager, this.mapTextureManager);

      try {
         int i = Runtime.getRuntime().availableProcessors();
         Tesselator.init();
         this.renderBuffers = new RenderBuffers(i);
      } catch (OutOfMemoryError outofmemoryerror) {
         TinyFileDialogs.tinyfd_messageBox(
            "Minecraft",
            "Oh no! The game was unable to allocate memory off-heap while trying to start. You may try to free some memory by closing other applications on your computer, check that your system meets the minimum requirements, and try again. If the problem persists, please visit: "
               + CommonLinks.GENERAL_HELP,
            "ok",
            "error",
            true
         );
         throw new SilentInitException("Unable to allocate render buffers", outofmemoryerror);
      }

      this.playerSocialManager = new PlayerSocialManager(this, this.userApiService);
      this.blockRenderer = new BlockRenderDispatcher(this.modelManager.getBlockModelShaper(), this.atlasManager, this.blockColors);
      this.resourceManager.registerReloadListener(this.blockRenderer);
      this.entityRenderDispatcher = new EntityRenderDispatcher(
         this,
         this.textureManager,
         this.itemModelResolver,
         this.mapRenderer,
         this.blockRenderer,
         this.atlasManager,
         this.font,
         this.options,
         this.modelManager.entityModels(),
         equipmentassetmanager,
         this.playerSkinRenderCache
      );
      this.resourceManager.registerReloadListener(this.entityRenderDispatcher);
      this.blockEntityRenderDispatcher = new BlockEntityRenderDispatcher(
         this.font,
         this.modelManager.entityModels(),
         this.blockRenderer,
         this.itemModelResolver,
         this.itemRenderer,
         this.entityRenderDispatcher,
         this.atlasManager,
         this.playerSkinRenderCache
      );
      this.resourceManager.registerReloadListener(this.blockEntityRenderDispatcher);
      this.particleResources = new ParticleResources();
      this.resourceManager.registerReloadListener(this.particleResources);
      this.particleEngine = new ParticleEngine(this.level, this.particleResources);
      this.particleResources.onReload(this.particleEngine::clearParticles);
      this.waypointStyles = new WaypointStyleManager();
      this.resourceManager.registerReloadListener(this.waypointStyles);
      this.gameRenderer = new GameRenderer(this, this.entityRenderDispatcher.getItemInHandRenderer(), this.renderBuffers, this.blockRenderer);
      this.levelRenderer = new LevelRenderer(
         this,
         this.entityRenderDispatcher,
         this.blockEntityRenderDispatcher,
         this.renderBuffers,
         this.gameRenderer.getLevelRenderState(),
         this.gameRenderer.getFeatureRenderDispatcher()
      );
      this.resourceManager.registerReloadListener(this.levelRenderer);
      this.resourceManager.registerReloadListener(this.levelRenderer.getCloudRenderer());
      this.gpuWarnlistManager = new GpuWarnlistManager();
      this.resourceManager.registerReloadListener(this.gpuWarnlistManager);
      this.resourceManager.registerReloadListener(this.regionalCompliancies);
      this.gui = new Gui(this);
      RealmsClient realmsclient = RealmsClient.getOrCreate(this);
      this.realmsDataFetcher = new RealmsDataFetcher(realmsclient);
      RenderSystem.setErrorCallback(this::onFullscreenError);
      if (this.mainRenderTarget.width != this.window.getWidth() || this.mainRenderTarget.height != this.window.getHeight()) {
         StringBuilder stringbuilder = new StringBuilder(
            "Recovering from unsupported resolution ("
               + this.window.getWidth()
               + "x"
               + this.window.getHeight()
               + ").\nPlease make sure you have up-to-date drivers (see aka.ms/mcdriver for instructions)."
         );

         try {
            GpuDevice gpudevice = RenderSystem.getDevice();
            List<String> list = gpudevice.getLastDebugMessages();
            if (!list.isEmpty()) {
               stringbuilder.append("\n\nReported GL debug messages:\n").append(String.join("\n", list));
            }
         } catch (Throwable var16) {
         }

         this.window.setWindowed(this.mainRenderTarget.width, this.mainRenderTarget.height);
         TinyFileDialogs.tinyfd_messageBox("Minecraft", stringbuilder.toString(), "ok", "error", false);
      } else if (this.options.fullscreen().get() && !this.window.isFullscreen()) {
         if (flag) {
            this.window.toggleFullScreen();
            this.options.fullscreen().set(this.window.isFullscreen());
         } else {
            this.options.fullscreen().set(false);
         }
      }

      this.window.updateVsync(this.options.enableVsync().get());
      this.window.updateRawMouseInput(this.options.rawMouseInput().get());
      this.window.setAllowCursorChanges(this.options.allowCursorChanges().get());
      this.window.setDefaultErrorCallback();
      this.resizeDisplay();
      this.gameRenderer.preloadUiShader(this.vanillaPackResources.asProvider());
      this.telemetryManager = new ClientTelemetryManager(this, this.userApiService, this.user);
      this.profileKeyPairManager = this.offlineDeveloperMode
         ? ProfileKeyPairManager.EMPTY_KEY_MANAGER
         : ProfileKeyPairManager.create(this.userApiService, this.user, path);
      this.narrator = new GameNarrator(this);
      this.narrator.checkStatus(this.options.narrator().get() != NarratorStatus.OFF);
      this.chatListener = new ChatListener(this);
      this.chatListener.setMessageDelay(this.options.chatDelay().get());
      this.reportingContext = ReportingContext.create(ReportEnvironment.local(), this.userApiService);
      TitleScreen.registerTextures(this.textureManager);
      LoadingOverlay.registerTextures(this.textureManager);
      this.gameRenderer.getPanorama().registerTextures(this.textureManager);
      this.setScreen(new GenericMessageScreen(Component.translatable("gui.loadingMinecraft")));
      List<PackResources> list1 = this.resourcePackRepository.openAllSelected();
      this.reloadStateTracker.startReload(ResourceLoadStateTracker.ReloadReason.INITIAL, list1);
      ReloadInstance reloadinstance = this.resourceManager
         .createReload(Util.backgroundExecutor().forName("resourceLoad"), this, RESOURCE_RELOAD_INITIAL_TASK, list1);
      GameLoadTimesEvent.INSTANCE.beginStep(TelemetryProperty.LOAD_TIME_LOADING_OVERLAY_MS);
      Minecraft.GameLoadCookie minecraft$gameloadcookie = new Minecraft.GameLoadCookie(realmsclient, p_91084_.quickPlay);
      this.setOverlay(
         new LoadingOverlay(
            this, reloadinstance, p_447796_ -> Util.ifElse(p_447796_, p_296162_ -> this.rollbackResourcePacks(p_296162_, minecraft$gameloadcookie), () -> {
               if (SharedConstants.IS_RUNNING_IN_IDE) {
                  this.selfTest();
               }

               this.reloadStateTracker.finishReload();
               this.onResourceLoadFinished(minecraft$gameloadcookie);
            }), false
         )
      );
      this.quickPlayLog = QuickPlayLog.of(p_91084_.quickPlay.logPath());
      this.framerateLimitTracker = new FramerateLimitTracker(this.options, this);
      this.fpsPieProfiler = new ContinuousProfiler(Util.timeSource, () -> this.fpsPieRenderTicks, this.framerateLimitTracker::isHeavilyThrottled);
      if (TracyClient.isAvailable() && p_91084_.game.captureTracyImages) {
         this.tracyFrameCapture = new TracyFrameCapture();
      } else {
         this.tracyFrameCapture = null;
      }

      this.packetProcessor = new PacketProcessor(this.gameThread);
   }

   public boolean hasShiftDown() {
      Window window = this.getWindow();
      return InputConstants.isKeyDown(window, 340) || InputConstants.isKeyDown(window, 344);
   }

   public boolean hasControlDown() {
      Window window = this.getWindow();
      return InputConstants.isKeyDown(window, 341) || InputConstants.isKeyDown(window, 345);
   }

   public boolean hasAltDown() {
      Window window = this.getWindow();
      return InputConstants.isKeyDown(window, 342) || InputConstants.isKeyDown(window, 346);
   }

   private void onResourceLoadFinished(Minecraft.@Nullable GameLoadCookie p_299693_) {
      if (!this.gameLoadFinished) {
         this.gameLoadFinished = true;
         this.onGameLoadFinished(p_299693_);
      }
   }

   private void onGameLoadFinished(Minecraft.@Nullable GameLoadCookie p_300808_) {
      Runnable runnable = this.buildInitialScreens(p_300808_);
      GameLoadTimesEvent.INSTANCE.endStep(TelemetryProperty.LOAD_TIME_LOADING_OVERLAY_MS);
      GameLoadTimesEvent.INSTANCE.endStep(TelemetryProperty.LOAD_TIME_TOTAL_TIME_MS);
      GameLoadTimesEvent.INSTANCE.send(this.telemetryManager.getOutsideSessionSender());
      runnable.run();
      this.options.startedCleanly = true;
      this.options.save();
   }

   public boolean isGameLoadFinished() {
      return this.gameLoadFinished;
   }

   private Runnable buildInitialScreens(Minecraft.@Nullable GameLoadCookie p_299870_) {
      List<Function<Runnable, Screen>> list = new ArrayList<>();
      boolean flag = this.addInitialScreens(list);
      Runnable runnable = () -> {
         if (p_299870_ != null && p_299870_.quickPlayData.isEnabled()) {
            QuickPlay.connect(this, p_299870_.quickPlayData.variant(), p_299870_.realmsClient());
         } else {
            this.setScreen(new TitleScreen(true, new LogoRenderer(flag)));
         }
      };

      for (Function<Runnable, Screen> function : Lists.reverse(list)) {
         Screen screen = function.apply(runnable);
         runnable = () -> this.setScreen(screen);
      }

      return runnable;
   }

   private boolean addInitialScreens(List<Function<Runnable, Screen>> p_297818_) {
      boolean flag = false;
      if (this.options.onboardAccessibility || SharedConstants.DEBUG_FORCE_ONBOARDING_SCREEN) {
         p_297818_.add(p_296165_ -> new AccessibilityOnboardingScreen(this.options, p_296165_));
         flag = true;
      }

      BanDetails bandetails = this.multiplayerBan();
      if (bandetails != null) {
         p_297818_.add(p_296160_ -> BanNoticeScreens.create(p_447794_ -> {
            if (p_447794_) {
               Util.getPlatform().openUri(CommonLinks.SUSPENSION_HELP);
            }

            p_296160_.run();
         }, bandetails));
      }

      ProfileResult profileresult = this.profileFuture.join();
      if (profileresult != null) {
         GameProfile gameprofile = profileresult.profile();
         Set<ProfileActionType> set = profileresult.actions();
         if (set.contains(ProfileActionType.FORCED_NAME_CHANGE)) {
            p_297818_.add(p_420639_ -> BanNoticeScreens.createNameBan(gameprofile.name(), p_420639_));
         }

         if (set.contains(ProfileActionType.USING_BANNED_SKIN)) {
            p_297818_.add(BanNoticeScreens::createSkinBan);
         }
      }

      return flag;
   }

   private static boolean countryEqualsISO3(Object p_210783_) {
      try {
         return Locale.getDefault().getISO3Country().equals(p_210783_);
      } catch (MissingResourceException missingresourceexception) {
         return false;
      }
   }

   public void updateTitle() {
      this.window.setTitle(this.createTitle());
   }

   private String createTitle() {
      StringBuilder stringbuilder = new StringBuilder(
         "Minecraft Terrain Plus Pack [%s/%s] "
            .formatted(this.screen != null ? this.screen.getClass().getName() : "", this.screen == null ? "" : this.screen.getTitle().getString())
      );
      stringbuilder.append(SharedConstants.getCurrentVersion().name());
      ClientPacketListener clientpacketlistener = this.getConnection();
      if (clientpacketlistener != null && clientpacketlistener.getConnection().isConnected()) {
         stringbuilder.append(" / ");
         ServerData serverdata = this.getCurrentServer();
         if (this.singleplayerServer != null && !this.singleplayerServer.isPublished()) {
            stringbuilder.append(I18n.get("title.singleplayer"));
         } else if (serverdata != null && serverdata.isRealm()) {
            stringbuilder.append(I18n.get("title.multiplayer.realms"));
         } else if (this.singleplayerServer != null || serverdata != null && serverdata.isLan()) {
            stringbuilder.append(I18n.get("title.multiplayer.lan"));
         } else {
            stringbuilder.append(I18n.get("title.multiplayer.other"));
         }
      }

      return stringbuilder.toString();
   }

   private UserApiService createUserApiService(YggdrasilAuthenticationService p_193586_, GameConfig p_193587_) {
      return p_193587_.game.offlineDeveloperMode ? UserApiService.OFFLINE : p_193586_.createUserApiService(p_193587_.user.user.getAccessToken());
   }

   public boolean isOfflineDeveloperMode() {
      return true;
   }

   public static ModCheck checkModStatus() {
      return ModCheck.identify("vanilla", () -> "Vanilla", "Client", Minecraft.class);
   }

   private void rollbackResourcePacks(Throwable p_91240_, Minecraft.@Nullable GameLoadCookie p_299515_) {
      if (this.resourcePackRepository.getSelectedIds().size() > 1) {
         this.clearResourcePacksOnError(p_91240_, null, p_299515_);
      } else {
         Util.throwAsRuntime(p_91240_);
      }
   }

   public void clearResourcePacksOnError(Throwable p_91242_, @Nullable Component p_91243_, Minecraft.@Nullable GameLoadCookie p_299857_) {
      LOGGER.info("Caught error loading resourcepacks, removing all selected resourcepacks", p_91242_);
      this.reloadStateTracker.startRecovery(p_91242_);
      this.downloadedPackSource.onRecovery();
      this.resourcePackRepository.setSelected(Collections.emptyList());
      this.options.resourcePacks.clear();
      this.options.incompatibleResourcePacks.clear();
      this.options.save();
      this.reloadResourcePacks(true, p_299857_).thenRunAsync(() -> this.addResourcePackLoadFailToast(p_91243_), this);
   }

   private void abortResourcePackRecovery() {
      this.setOverlay(null);
      if (this.level != null) {
         this.level.disconnect(ClientLevel.DEFAULT_QUIT_MESSAGE);
         this.disconnectWithProgressScreen();
      }

      this.setScreen(new TitleScreen());
      this.addResourcePackLoadFailToast(null);
   }

   private void addResourcePackLoadFailToast(@Nullable Component p_273566_) {
      ToastManager toastmanager = this.getToastManager();
      SystemToast.addOrUpdate(toastmanager, SystemToast.SystemToastId.PACK_LOAD_FAILURE, Component.translatable("resourcePack.load_fail"), p_273566_);
   }

   public void triggerResourcePackRecovery(Exception p_362406_) {
      if (!this.resourcePackRepository.isAbleToClearAnyPack()) {
         if (this.resourcePackRepository.getSelectedIds().size() <= 1) {
            LOGGER.error(LogUtils.FATAL_MARKER, p_362406_.getMessage(), p_362406_);
            this.emergencySaveAndCrash(new CrashReport(p_362406_.getMessage(), p_362406_));
         } else {
            this.schedule(this::abortResourcePackRecovery);
         }
      } else {
         this.clearResourcePacksOnError(p_362406_, Component.translatable("resourcePack.runtime_failure"), null);
      }
   }

   public void run() {
      this.gameThread = Thread.currentThread();
      if (Runtime.getRuntime().availableProcessors() > 4) {
         this.gameThread.setPriority(10);
      }

      DiscontinuousFrame discontinuousframe = TracyClient.createDiscontinuousFrame("Client Tick");

      try {
         boolean flag = false;

         while (this.running) {
            this.handleDelayedCrash();

            try {
               SingleTickProfiler singletickprofiler = SingleTickProfiler.createTickProfiler("Renderer");
               boolean flag1 = this.getDebugOverlay().showProfilerChart();

               try (Profiler.Scope profiler$scope = Profiler.use(this.constructProfiler(flag1, singletickprofiler))) {
                  this.metricsRecorder.startTick();
                  discontinuousframe.start();
                  this.runTick(!flag);
                  discontinuousframe.end();
                  this.metricsRecorder.endTick();
               }

               this.finishProfilers(flag1, singletickprofiler);
            } catch (OutOfMemoryError outofmemoryerror) {
               if (flag) {
                  throw outofmemoryerror;
               }

               this.emergencySave();
               this.setScreen(new OutOfMemoryScreen());
               System.gc();
               LOGGER.error(LogUtils.FATAL_MARKER, "Out of memory", outofmemoryerror);
               flag = true;
            }
         }
      } catch (ReportedException reportedexception) {
         LOGGER.error(LogUtils.FATAL_MARKER, "Reported exception thrown!", reportedexception);
         this.emergencySaveAndCrash(reportedexception.getReport());
      } catch (Throwable throwable1) {
         LOGGER.error(LogUtils.FATAL_MARKER, "Unreported exception thrown!", throwable1);
         this.emergencySaveAndCrash(new CrashReport("Unexpected error", throwable1));
      }
   }

   void updateFontOptions() {
      this.fontManager.updateOptions(this.options);
   }

   private void onFullscreenError(int p_91114_, long p_91115_) {
      this.options.enableVsync().set(false);
      this.options.save();
   }

   public RenderTarget getMainRenderTarget() {
      return this.mainRenderTarget;
   }

   public String getLaunchedVersion() {
      return this.launchedVersion;
   }

   public String getVersionType() {
      return this.versionType;
   }

   public void delayCrash(CrashReport p_231413_) {
      this.delayedCrash = () -> this.fillReport(p_231413_);
   }

   public void delayCrashRaw(CrashReport p_231440_) {
      this.delayedCrash = () -> p_231440_;
   }

   private void handleDelayedCrash() {
      if (this.delayedCrash != null) {
         crash(this, this.gameDirectory, this.delayedCrash.get());
      }
   }

   public void emergencySaveAndCrash(CrashReport p_313046_) {
      MemoryReserve.release();
      CrashReport crashreport = this.fillReport(p_313046_);
      this.emergencySave();
      crash(this, this.gameDirectory, crashreport);
   }

   public static int saveReport(File p_362993_, CrashReport p_366163_) {
      Path path = p_362993_.toPath().resolve("crash-reports");
      Path path1 = path.resolve("crash-" + Util.getFilenameFormattedDateTime() + "-client.txt");
      Bootstrap.realStdoutPrintln(p_366163_.getFriendlyReport(ReportType.CRASH));
      if (p_366163_.getSaveFile() != null) {
         Bootstrap.realStdoutPrintln("#@!@# Game crashed! Crash report saved to: #@!@# " + p_366163_.getSaveFile().toAbsolutePath());
         return -1;
      } else if (p_366163_.saveToFile(path1, ReportType.CRASH)) {
         Bootstrap.realStdoutPrintln("#@!@# Game crashed! Crash report saved to: #@!@# " + path1.toAbsolutePath());
         return -1;
      } else {
         Bootstrap.realStdoutPrintln("#@?@# Game crashed! Crash report could not be saved. #@?@#");
         return -2;
      }
   }

   public static void crash(@Nullable Minecraft p_311916_, File p_309666_, CrashReport p_91333_) {
      int i = saveReport(p_309666_, p_91333_);
      if (p_311916_ != null) {
         p_311916_.soundManager.emergencyShutdown();
      }

      System.exit(i);
   }

   public boolean isEnforceUnicode() {
      return this.options.forceUnicodeFont().get();
   }

   public CompletableFuture<Void> reloadResourcePacks() {
      return this.reloadResourcePacks(false, null);
   }

   private CompletableFuture<Void> reloadResourcePacks(boolean p_168020_, Minecraft.@Nullable GameLoadCookie p_300647_) {
      if (this.pendingReload != null) {
         return this.pendingReload;
      }

      CompletableFuture<Void> completablefuture = new CompletableFuture<>();
      if (!p_168020_ && this.overlay instanceof LoadingOverlay) {
         this.pendingReload = completablefuture;
         return completablefuture;
      }

      this.resourcePackRepository.reload();
      List<PackResources> list = this.resourcePackRepository.openAllSelected();
      if (!p_168020_) {
         this.reloadStateTracker.startReload(ResourceLoadStateTracker.ReloadReason.MANUAL, list);
      }

      this.setOverlay(
         new LoadingOverlay(
            this,
            this.resourceManager.createReload(Util.backgroundExecutor().forName("resourceLoad"), this, RESOURCE_RELOAD_INITIAL_TASK, list),
            p_447792_ -> Util.ifElse(p_447792_, p_308166_ -> {
               if (p_168020_) {
                  this.downloadedPackSource.onRecoveryFailure();
                  this.abortResourcePackRecovery();
               } else {
                  this.rollbackResourcePacks(p_308166_, p_300647_);
               }
            }, () -> {
               this.levelRenderer.allChanged();
               this.reloadStateTracker.finishReload();
               this.downloadedPackSource.onReloadSuccess();
               completablefuture.complete(null);
               this.onResourceLoadFinished(p_300647_);
            }),
            !p_168020_
         )
      );
      return completablefuture;
   }

   private void selfTest() {
      boolean flag = false;
      BlockModelShaper blockmodelshaper = this.getBlockRenderer().getBlockModelShaper();
      BlockStateModel blockstatemodel = blockmodelshaper.getModelManager().getMissingBlockStateModel();

      for (Block block : BuiltInRegistries.BLOCK) {
         UnmodifiableIterator block1 = block.getStateDefinition().getPossibleStates().iterator();

         while (block1.hasNext()) {
            BlockState blockstate = (BlockState)block1.next();
            if (blockstate.getRenderShape() == RenderShape.MODEL) {
               BlockStateModel blockstatemodel1 = blockmodelshaper.getBlockModel(blockstate);
               if (blockstatemodel1 == blockstatemodel) {
                  LOGGER.debug("Missing model for: {}", blockstate);
                  flag = true;
               }
            }
         }
      }

      TextureAtlasSprite textureatlassprite1 = blockstatemodel.particleIcon();

      for (Block block1 : BuiltInRegistries.BLOCK) {
         UnmodifiableIterator var15 = block1.getStateDefinition().getPossibleStates().iterator();

         while (var15.hasNext()) {
            BlockState blockstate1 = (BlockState)var15.next();
            TextureAtlasSprite textureatlassprite = blockmodelshaper.getParticleIcon(blockstate1);
            if (!blockstate1.isAir() && textureatlassprite == textureatlassprite1) {
               LOGGER.debug("Missing particle icon for: {}", blockstate1);
            }
         }
      }

      BuiltInRegistries.ITEM.listElements().forEach(p_357641_ -> {
         Item item = p_357641_.value();
         String s = item.getDescriptionId();
         String s1 = Component.translatable(s).getString();
         if (s1.toLowerCase(Locale.ROOT).equals(item.getDescriptionId())) {
            LOGGER.debug("Missing translation for: {} {} {}", new Object[]{p_357641_.key().identifier(), s, item});
         }
      });
      flag |= MenuScreens.selfTest();
      flag |= EntityRenderers.validateRegistrations();
      if (flag) {
         throw new IllegalStateException("Your game data is foobar, fix the errors above!");
      }
   }

   public LevelStorageSource getLevelSource() {
      return this.levelSource;
   }

   public void openChatScreen(ChatComponent.ChatMethod p_424408_) {
      Minecraft.ChatStatus minecraft$chatstatus = this.getChatStatus();
      if (!minecraft$chatstatus.isChatAllowed(this.isLocalServer())) {
         if (this.gui.isShowingChatDisabledByPlayer()) {
            this.gui.setChatDisabledByPlayerShown(false);
            this.setScreen(new ConfirmLinkScreen(p_447786_ -> {
               if (p_447786_) {
                  Util.getPlatform().openUri(CommonLinks.ACCOUNT_SETTINGS);
               }

               this.setScreen(null);
            }, Minecraft.ChatStatus.INFO_DISABLED_BY_PROFILE, CommonLinks.ACCOUNT_SETTINGS, true));
         } else {
            Component component = minecraft$chatstatus.getMessage();
            this.gui.setOverlayMessage(component, false);
            this.narrator.saySystemNow(component);
            this.gui.setChatDisabledByPlayerShown(minecraft$chatstatus == Minecraft.ChatStatus.DISABLED_BY_PROFILE);
         }
      } else {
         this.gui.getChat().openScreen(p_424408_, ChatScreen::new);
      }
   }

   public void setScreen(@Nullable Screen p_91153_) {
      if (!ModMain.getOptions().getExpd_noRealmsErrorScreenValue() || !(p_91153_ instanceof RealmsGenericErrorScreen)) {
         if (SharedConstants.IS_RUNNING_IN_IDE && Thread.currentThread() != this.gameThread) {
            LOGGER.error("setScreen called from non-game thread");
         }

         if (this.screen != null) {
            this.screen.removed();
         } else {
            this.setLastInputType(InputType.NONE);
         }

         if (p_91153_ == null) {
            if (this.clientLevelTeardownInProgress) {
               throw new IllegalStateException("Trying to return to in-game GUI during disconnection");
            }

            if (this.level == null) {
               p_91153_ = new TitleScreen();
            } else if (this.player.isDeadOrDying()) {
               if (this.player.shouldShowDeathScreen()) {
                  p_91153_ = new DeathScreen(null, this.level.getLevelData().isHardcore(), this.player);
               } else {
                  this.player.respawn();
               }
            } else {
               p_91153_ = this.gui.getChat().restoreChatScreen();
            }
         }

         this.screen = p_91153_;
         if (this.screen != null) {
            this.screen.added();
         }

         if (p_91153_ != null) {
            this.mouseHandler.releaseMouse();
            KeyMapping.releaseAll();
            p_91153_.init(this.window.getGuiScaledWidth(), this.window.getGuiScaledHeight());
            this.noRender = false;
         } else {
            if (this.level != null) {
               KeyMapping.restoreToggleStatesOnScreenClosed();
            }

            this.soundManager.resume();
            this.mouseHandler.grabMouse();
         }

         this.updateTitle();
      }
   }

   public void setOverlay(@Nullable Overlay p_91151_) {
      this.overlay = p_91151_;
   }

   public void destroy() {
      try {
         LOGGER.info("Stopping!");

         try {
            this.narrator.destroy();
         } catch (Throwable var7) {
         }

         try {
            if (this.level != null) {
               this.level.disconnect(ClientLevel.DEFAULT_QUIT_MESSAGE);
            }

            this.disconnectWithProgressScreen();
         } catch (Throwable var6) {
         }

         if (this.screen != null) {
            this.screen.removed();
         }

         this.close();
      } finally {
         Util.timeSource = System::nanoTime;
         if (this.delayedCrash == null) {
            System.exit(0);
         }
      }
   }

   @Override
   public void close() {
      if (this.currentFrameProfile != null) {
         this.currentFrameProfile.cancel();
      }

      try {
         this.telemetryManager.close();
         this.regionalCompliancies.close();
         this.atlasManager.close();
         this.fontManager.close();
         this.gameRenderer.close();
         this.shaderManager.close();
         this.levelRenderer.close();
         this.soundManager.destroy();
         this.mapTextureManager.close();
         this.textureManager.close();
         this.resourceManager.close();
         if (this.tracyFrameCapture != null) {
            this.tracyFrameCapture.close();
         }

         FreeTypeUtil.destroy();
         Util.shutdownExecutors();
         RenderSystem.getSamplerCache().close();
         RenderSystem.getDevice().close();
      } catch (Throwable throwable) {
         LOGGER.error("Shutdown failure!", throwable);
         throw throwable;
      } finally {
         this.virtualScreen.close();
         this.window.close();
      }
   }

   private void runTick(boolean p_91384_) {
      this.window.setErrorSection("Pre render");
      if (this.window.shouldClose()) {
         this.stop();
      }

      if (this.pendingReload != null && !(this.overlay instanceof LoadingOverlay)) {
         CompletableFuture<Void> completablefuture = this.pendingReload;
         this.pendingReload = null;
         this.reloadResourcePacks().thenRun(() -> completablefuture.complete(null));
      }

      int k = this.deltaTracker.advanceTime(Util.getMillis(), p_91384_);
      ProfilerFiller profilerfiller = Profiler.get();
      if (p_91384_) {
         try (Gizmos.TemporaryCollection gizmos$temporarycollection = this.collectPerTickGizmos()) {
            profilerfiller.push("scheduledPacketProcessing");
            this.packetProcessor.processQueuedPackets();
            profilerfiller.popPush("scheduledExecutables");
            this.runAllTasks();
            profilerfiller.pop();
         }

         profilerfiller.push("tick");
         if (k > 0 && this.isLevelRunningNormally()) {
            profilerfiller.push("textures");
            this.textureManager.tick();
            profilerfiller.pop();
         }

         for (int l = 0; l < Math.min(10, k); l++) {
            profilerfiller.incrementCounter("clientTick");

            try (Gizmos.TemporaryCollection gizmos$temporarycollection1 = this.collectPerTickGizmos()) {
               this.tick();
            }
         }

         if (k > 0 && (this.level == null || this.level.tickRateManager().runsNormally())) {
            this.drainedLatestTickGizmos = this.perTickGizmos.drainGizmos();
         }

         profilerfiller.pop();
      }

      this.window.setErrorSection("Render");

      boolean flag;
      try (Gizmos.TemporaryCollection gizmos$temporarycollection2 = this.levelRenderer.collectPerFrameGizmos()) {
         profilerfiller.push("gpuAsync");
         RenderSystem.executePendingTasks();
         profilerfiller.popPush("sound");
         this.soundManager.updateSource(this.gameRenderer.getMainCamera());
         profilerfiller.popPush("toasts");
         this.toastManager.update();
         profilerfiller.popPush("mouse");
         this.mouseHandler.handleAccumulatedMovement();
         profilerfiller.popPush("render");
         long i = Util.getNanos();
         if (!this.debugEntries.isCurrentlyEnabled(DebugScreenEntries.GPU_UTILIZATION) && !this.metricsRecorder.isRecording()) {
            flag = false;
            this.gpuUtilization = 0.0;
         } else {
            flag = (this.currentFrameProfile == null || this.currentFrameProfile.isDone()) && !TimerQuery.getInstance().isRecording();
            if (flag) {
               TimerQuery.getInstance().beginProfile();
            }
         }

         RenderTarget rendertarget = this.getMainRenderTarget();
         RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(rendertarget.getColorTexture(), 0, rendertarget.getDepthTexture(), 1.0);
         profilerfiller.push("gameRenderer");
         if (!this.noRender) {
            this.gameRenderer.render(this.deltaTracker, p_91384_);
         }

         profilerfiller.popPush("blit");
         if (!this.window.isMinimized()) {
            rendertarget.blitToScreen();
         }

         this.frameTimeNs = Util.getNanos() - i;
         if (flag) {
            this.currentFrameProfile = TimerQuery.getInstance().endProfile();
         }

         profilerfiller.popPush("updateDisplay");
         if (this.tracyFrameCapture != null) {
            this.tracyFrameCapture.upload();
            this.tracyFrameCapture.capture(rendertarget);
         }

         this.window.updateDisplay(this.tracyFrameCapture);
         int j = this.framerateLimitTracker.getFramerateLimit();
         if (j < 260) {
            RenderSystem.limitDisplayFPS(j);
         }

         profilerfiller.pop();
         profilerfiller.popPush("yield");
         Thread.yield();
         profilerfiller.pop();
      }

      this.window.setErrorSection("Post render");
      this.frames++;
      boolean flag1 = this.pause;
      this.pause = this.hasSingleplayerServer()
         && (this.screen != null && this.screen.isPauseScreen() || this.overlay != null && this.overlay.isPauseScreen())
         && !this.singleplayerServer.isPublished();
      if (!flag1 && this.pause) {
         this.soundManager.pauseAllExcept(SoundSource.MUSIC, SoundSource.UI);
      }

      this.deltaTracker.updatePauseState(this.pause);
      this.deltaTracker.updateFrozenState(!this.isLevelRunningNormally());
      long i1 = Util.getNanos();
      long j1 = i1 - this.lastNanoTime;
      if (flag) {
         this.savedCpuDuration = j1;
      }

      this.getDebugOverlay().logFrameDuration(j1);
      this.lastNanoTime = i1;
      profilerfiller.push("fpsUpdate");
      if (this.currentFrameProfile != null && this.currentFrameProfile.isDone()) {
         this.gpuUtilization = this.currentFrameProfile.get() * 100.0 / this.savedCpuDuration;
      }

      while (Util.getMillis() >= this.lastTime + 1000L) {
         fps = this.frames;
         this.lastTime += 1000L;
         this.frames = 0;
      }

      profilerfiller.pop();
   }

   private ProfilerFiller constructProfiler(boolean p_167971_, @Nullable SingleTickProfiler p_167972_) {
      if (!p_167971_) {
         this.fpsPieProfiler.disable();
         if (!this.metricsRecorder.isRecording() && p_167972_ == null) {
            return InactiveProfiler.INSTANCE;
         }
      }

      ProfilerFiller profilerfiller;
      if (p_167971_) {
         if (!this.fpsPieProfiler.isEnabled()) {
            this.fpsPieRenderTicks = 0;
            this.fpsPieProfiler.enable();
         }

         this.fpsPieRenderTicks++;
         profilerfiller = this.fpsPieProfiler.getFiller();
      } else {
         profilerfiller = InactiveProfiler.INSTANCE;
      }

      if (this.metricsRecorder.isRecording()) {
         profilerfiller = ProfilerFiller.combine(profilerfiller, this.metricsRecorder.getProfiler());
      }

      return SingleTickProfiler.decorateFiller(profilerfiller, p_167972_);
   }

   private void finishProfilers(boolean p_91339_, @Nullable SingleTickProfiler p_91340_) {
      if (p_91340_ != null) {
         p_91340_.endTick();
      }

      ProfilerPieChart profilerpiechart = this.getDebugOverlay().getProfilerPieChart();
      if (p_91339_) {
         profilerpiechart.setPieChartResults(this.fpsPieProfiler.getResults());
      } else {
         profilerpiechart.setPieChartResults(null);
      }
   }

   @Override
   public void resizeDisplay() {
      int i = this.window.calculateScale(this.options.guiScale().get(), this.isEnforceUnicode());
      this.window.setGuiScale(i);
      if (this.screen != null) {
         this.screen.resize(this.window.getGuiScaledWidth(), this.window.getGuiScaledHeight());
      }

      RenderTarget rendertarget = this.getMainRenderTarget();
      rendertarget.resize(this.window.getWidth(), this.window.getHeight());
      this.gameRenderer.resize(this.window.getWidth(), this.window.getHeight());
      this.mouseHandler.setIgnoreFirstMove();
   }

   @Override
   public void cursorEntered() {
      this.mouseHandler.cursorEntered();
   }

   public int getFps() {
      return fps;
   }

   public long getFrameTimeNs() {
      return this.frameTimeNs;
   }

   private void emergencySave() {
      MemoryReserve.release();

      try {
         if (this.isLocalServer && this.singleplayerServer != null) {
            this.singleplayerServer.halt(true);
         }

         this.disconnectWithSavingScreen();
      } catch (Throwable var2) {
      }

      System.gc();
   }

   public boolean debugClientMetricsStart(Consumer<Component> p_167947_) {
      if (this.metricsRecorder.isRecording()) {
         this.debugClientMetricsStop();
         return false;
      }

      Consumer<ProfileResults> consumer = p_231435_ -> {
         if (p_231435_ != EmptyProfileResults.EMPTY) {
            int i = p_231435_.getTickDuration();
            double d0 = (double)p_231435_.getNanoDuration() / TimeUtil.NANOSECONDS_PER_SECOND;
            this.execute(
               () -> p_167947_.accept(
                  Component.translatable("commands.debug.stopped", String.format(Locale.ROOT, "%.2f", d0), i, String.format(Locale.ROOT, "%.2f", i / d0))
               )
            );
         }
      };
      Consumer<Path> consumer1 = p_231438_ -> {
         Component component = Component.literal(p_231438_.toString())
            .withStyle(ChatFormatting.UNDERLINE)
            .withStyle(p_389132_ -> p_389132_.withClickEvent(new ClickEvent.OpenFile(p_231438_.getParent())));
         this.execute(() -> p_167947_.accept(Component.translatable("debug.profiling.stop", component)));
      };
      SystemReport systemreport = fillSystemReport(new SystemReport(), this, this.languageManager, this.launchedVersion, this.options);
      Consumer<List<Path>> consumer2 = p_231349_ -> {
         Path path = this.archiveProfilingReport(systemreport, p_231349_);
         consumer1.accept(path);
      };
      Consumer<Path> consumer3;
      if (this.singleplayerServer == null) {
         consumer3 = p_231404_ -> consumer2.accept(ImmutableList.of(p_231404_));
      } else {
         this.singleplayerServer.fillSystemReport(systemreport);
         CompletableFuture<Path> completablefuture = new CompletableFuture<>();
         CompletableFuture<Path> completablefuture1 = new CompletableFuture<>();
         CompletableFuture.allOf(completablefuture, completablefuture1)
            .thenRunAsync(() -> consumer2.accept(ImmutableList.of(completablefuture.join(), completablefuture1.join())), Util.ioPool());
         this.singleplayerServer.startRecordingMetrics(p_231351_ -> {}, completablefuture1::complete);
         consumer3 = completablefuture::complete;
      }

      this.metricsRecorder = ActiveMetricsRecorder.createStarted(
         new ClientMetricsSamplersProvider(Util.timeSource, this.levelRenderer), Util.timeSource, Util.ioPool(), new MetricsPersister("client"), p_231401_ -> {
            this.metricsRecorder = InactiveMetricsRecorder.INSTANCE;
            consumer.accept(p_231401_);
         }, consumer3
      );
      return true;
   }

   private void debugClientMetricsStop() {
      this.metricsRecorder.end();
      if (this.singleplayerServer != null) {
         this.singleplayerServer.finishRecordingMetrics();
      }
   }

   private void debugClientMetricsCancel() {
      this.metricsRecorder.cancel();
      if (this.singleplayerServer != null) {
         this.singleplayerServer.cancelRecordingMetrics();
      }
   }

   private Path archiveProfilingReport(SystemReport p_167857_, List<Path> p_167858_) {
      String s;
      if (this.isLocalServer()) {
         s = this.getSingleplayerServer().getWorldData().getLevelName();
      } else {
         ServerData serverdata = this.getCurrentServer();
         s = serverdata != null ? serverdata.name : "unknown";
      }

      Path path;
      try {
         String s2 = String.format(Locale.ROOT, "%s-%s-%s", Util.getFilenameFormattedDateTime(), s, SharedConstants.getCurrentVersion().id());
         String s1 = FileUtil.findAvailableName(MetricsPersister.PROFILING_RESULTS_DIR, s2, ".zip");
         path = MetricsPersister.PROFILING_RESULTS_DIR.resolve(s1);
      } catch (IOException ioexception1) {
         throw new UncheckedIOException(ioexception1);
      }

      try (FileZipper filezipper = new FileZipper(path)) {
         filezipper.add(Paths.get("system.txt"), p_167857_.toLineSeparatedString());
         filezipper.add(Paths.get("client").resolve(this.options.getFile().getName()), this.options.dumpOptionsForReport());
         p_167858_.forEach(filezipper::add);
      } finally {
         for (Path path1 : p_167858_) {
            try {
               FileUtils.forceDelete(path1.toFile());
            } catch (IOException ioexception) {
               LOGGER.warn("Failed to delete temporary profiling result {}", path1, ioexception);
            }
         }
      }

      return path;
   }

   public void stop() {
      this.running = false;
   }

   public boolean isRunning() {
      return this.running;
   }

   public void pauseGame(boolean p_91359_) {
      if (this.screen == null) {
         boolean flag = this.hasSingleplayerServer() && !this.singleplayerServer.isPublished();
         if (flag) {
            this.setScreen(new PauseScreen(!p_91359_));
         } else {
            this.setScreen(new PauseScreen(true));
         }
      }
   }

   private void continueAttack(boolean p_91387_) {
      if (!p_91387_) {
         this.missTime = 0;
      }

      if (this.missTime <= 0 && !this.player.isUsingItem()) {
         ItemStack itemstack = this.player.getItemInHand(InteractionHand.MAIN_HAND);
         if (!itemstack.has(DataComponents.PIERCING_WEAPON)) {
            if (p_91387_ && this.hitResult != null && this.hitResult.getType() == HitResult.Type.BLOCK) {
               BlockHitResult blockhitresult = (BlockHitResult)this.hitResult;
               BlockPos blockpos = blockhitresult.getBlockPos();
               if (!this.level.getBlockState(blockpos).isAir()) {
                  Direction direction = blockhitresult.getDirection();
                  if (this.gameMode.continueDestroyBlock(blockpos, direction)) {
                     this.level.addBreakingBlockEffect(blockpos, direction);
                     this.player.swing(InteractionHand.MAIN_HAND);
                  }
               }
            } else {
               this.gameMode.stopDestroyBlock();
            }
         }
      }
   }

   private boolean startAttack() {
      if (this.missTime > 0) {
         return false;
      }

      if (this.hitResult == null) {
         LOGGER.error("Null returned as 'hitResult', this shouldn't happen!");
         if (this.gameMode.hasMissTime()) {
            this.missTime = 10;
         }

         return false;
      } else {
         if (this.player.isHandsBusy()) {
            return false;
         }

         ItemStack itemstack = this.player.getItemInHand(InteractionHand.MAIN_HAND);
         if (!itemstack.isItemEnabled(this.level.enabledFeatures())) {
            return false;
         }

         if (this.player.cannotAttackWithItem(itemstack, 0)) {
            return false;
         }

         boolean flag = false;
         PiercingWeapon piercingweapon = itemstack.get(DataComponents.PIERCING_WEAPON);
         if (piercingweapon != null && !this.gameMode.isSpectator()) {
            this.gameMode.piercingAttack(piercingweapon);
            this.player.swing(InteractionHand.MAIN_HAND);
            return true;
         }

         switch (this.hitResult.getType()) {
            case ENTITY:
               AttackRange attackrange = itemstack.get(DataComponents.ATTACK_RANGE);
               if (attackrange == null || attackrange.isInRange(this.player, this.hitResult.getLocation())) {
                  this.gameMode.attack(this.player, ((EntityHitResult)this.hitResult).getEntity());
               }
               break;
            case BLOCK:
               BlockHitResult blockhitresult = (BlockHitResult)this.hitResult;
               BlockPos blockpos = blockhitresult.getBlockPos();
               if (!this.level.getBlockState(blockpos).isAir()) {
                  this.gameMode.startDestroyBlock(blockpos, blockhitresult.getDirection());
                  if (this.level.getBlockState(blockpos).isAir()) {
                     flag = true;
                  }
                  break;
               }
            case MISS:
               if (this.gameMode.hasMissTime()) {
                  this.missTime = 10;
               }

               this.player.resetAttackStrengthTicker();
         }

         if (!this.player.isSpectator()) {
            this.player.swing(InteractionHand.MAIN_HAND);
         }

         return flag;
      }
   }

   private void startUseItem() {
      if (!this.gameMode.isDestroying()) {
         this.rightClickDelay = 4;
         if (!this.player.isHandsBusy()) {
            if (this.hitResult == null) {
               LOGGER.warn("Null returned as 'hitResult', this shouldn't happen!");
            }

            for (InteractionHand interactionhand : InteractionHand.values()) {
               ItemStack itemstack = this.player.getItemInHand(interactionhand);
               if (!itemstack.isItemEnabled(this.level.enabledFeatures())) {
                  return;
               }

               if (this.hitResult != null) {
                  switch (this.hitResult.getType()) {
                     case ENTITY:
                        EntityHitResult entityhitresult = (EntityHitResult)this.hitResult;
                        Entity entity = entityhitresult.getEntity();
                        if (!this.level.getWorldBorder().isWithinBounds(entity.blockPosition())) {
                           return;
                        }

                        if (this.player.isWithinEntityInteractionRange(entity, 0.0)) {
                           InteractionResult interactionresult = this.gameMode.interactAt(this.player, entity, entityhitresult, interactionhand);
                           if (!interactionresult.consumesAction()) {
                              interactionresult = this.gameMode.interact(this.player, entity, interactionhand);
                           }

                           if (interactionresult instanceof InteractionResult.Success interactionresult$success2) {
                              if (interactionresult$success2.swingSource() == InteractionResult.SwingSource.CLIENT) {
                                 this.player.swing(interactionhand);
                              }

                              return;
                           }
                        }
                        break;
                     case BLOCK:
                        BlockHitResult blockhitresult = (BlockHitResult)this.hitResult;
                        int i = itemstack.getCount();
                        InteractionResult interactionresult1 = this.gameMode.useItemOn(this.player, interactionhand, blockhitresult);
                        if (interactionresult1 instanceof InteractionResult.Success interactionresult$success) {
                           if (interactionresult$success.swingSource() == InteractionResult.SwingSource.CLIENT) {
                              this.player.swing(interactionhand);
                              if (!itemstack.isEmpty() && (itemstack.getCount() != i || this.player.hasInfiniteMaterials())) {
                                 this.gameRenderer.itemInHandRenderer.itemUsed(interactionhand);
                              }
                           }

                           return;
                        }

                        if (interactionresult1 instanceof InteractionResult.Fail) {
                           return;
                        }
                  }
               }

               if (!itemstack.isEmpty() && this.gameMode.useItem(this.player, interactionhand) instanceof InteractionResult.Success interactionresult$success1) {
                  if (interactionresult$success1.swingSource() == InteractionResult.SwingSource.CLIENT) {
                     this.player.swing(interactionhand);
                  }

                  this.gameRenderer.itemInHandRenderer.itemUsed(interactionhand);
                  return;
               }
            }
         }
      }
   }

   public MusicManager getMusicManager() {
      return this.musicManager;
   }

   public void tick() {
      this.clientTickCount++;
      if (this.level != null && !this.pause) {
         this.level.tickRateManager().tick();
      }

      if (this.rightClickDelay > 0) {
         this.rightClickDelay--;
      }

      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("gui");
      this.chatListener.tick();
      this.gui.tick(this.pause);
      profilerfiller.pop();
      this.gameRenderer.pick(1.0F);
      this.tutorial.onLookAt(this.level, this.hitResult);
      profilerfiller.push("gameMode");
      if (!this.pause && this.level != null) {
         this.gameMode.tick();
      }

      profilerfiller.popPush("screen");
      if (this.screen != null || this.player == null) {
         if (this.screen instanceof InBedChatScreen inbedchatscreen && !this.player.isSleeping()) {
            inbedchatscreen.onPlayerWokeUp();
         }
      } else if (this.player.isDeadOrDying() && !(this.screen instanceof DeathScreen)) {
         this.setScreen(null);
      } else if (this.player.isSleeping() && this.level != null) {
         this.gui.getChat().openScreen(ChatComponent.ChatMethod.MESSAGE, InBedChatScreen::new);
      }

      if (this.screen != null) {
         this.missTime = 10000;
      }

      if (this.screen != null) {
         try {
            this.screen.tick();
         } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Ticking screen");
            this.screen.fillCrashDetails(crashreport);
            throw new ReportedException(crashreport);
         }
      }

      if (this.overlay != null) {
         this.overlay.tick();
      }

      if (!this.getDebugOverlay().showDebugScreen()) {
         this.gui.clearCache();
      }

      if (this.overlay == null && this.screen == null) {
         profilerfiller.popPush("Keybindings");
         this.handleKeybinds();
         if (this.missTime > 0) {
            this.missTime--;
         }
      }

      if (this.level != null) {
         if (!this.pause) {
            profilerfiller.popPush("gameRenderer");
            this.gameRenderer.tick();
            profilerfiller.popPush("entities");
            this.level.tickEntities();
            profilerfiller.popPush("blockEntities");
            this.level.tickBlockEntities();
         }
      } else if (this.gameRenderer.currentPostEffect() != null) {
         this.gameRenderer.clearPostEffect();
      }

      this.musicManager.tick();
      this.soundManager.tick(this.pause);
      if (this.level != null) {
         if (!this.pause) {
            profilerfiller.popPush("level");
            if (!this.options.joinedFirstServer && this.isMultiplayerServer()) {
               Component component = Component.translatable("tutorial.socialInteractions.title");
               Component component1 = Component.translatable("tutorial.socialInteractions.description", Tutorial.key("socialInteractions"));
               this.socialInteractionsToast = new TutorialToast(this.font, TutorialToast.Icons.SOCIAL_INTERACTIONS, component, component1, true, 8000);
               this.toastManager.addToast(this.socialInteractionsToast);
               this.options.joinedFirstServer = true;
               this.options.save();
            }

            this.tutorial.tick();

            try {
               this.level.tick(() -> true);
            } catch (Throwable throwable1) {
               CrashReport crashreport1 = CrashReport.forThrowable(throwable1, "Exception in world tick");
               if (this.level == null) {
                  CrashReportCategory crashreportcategory = crashreport1.addCategory("Affected level");
                  crashreportcategory.setDetail("Problem", "Level is null!");
               } else {
                  this.level.fillReportDetails(crashreport1);
               }

               throw new ReportedException(crashreport1);
            }
         }

         profilerfiller.popPush("animateTick");
         if (!this.pause && this.isLevelRunningNormally()) {
            this.level.animateTick(this.player.getBlockX(), this.player.getBlockY(), this.player.getBlockZ());
         }

         profilerfiller.popPush("particles");
         if (!this.pause && this.isLevelRunningNormally()) {
            this.particleEngine.tick();
         }

         ClientPacketListener clientpacketlistener = this.getConnection();
         if (clientpacketlistener != null && !this.pause) {
            clientpacketlistener.send(ServerboundClientTickEndPacket.INSTANCE);
         }
      } else if (this.pendingConnection != null) {
         profilerfiller.popPush("pendingConnection");
         this.pendingConnection.tick();
      }

      profilerfiller.popPush("keyboard");
      this.keyboardHandler.tick();
      profilerfiller.pop();
   }

   private boolean isLevelRunningNormally() {
      return this.level == null || this.level.tickRateManager().runsNormally();
   }

   private boolean isMultiplayerServer() {
      return !this.isLocalServer || this.singleplayerServer != null && this.singleplayerServer.isPublished();
   }

   private void handleKeybinds() {
      while (this.options.keyTogglePerspective.consumeClick()) {
         CameraType cameratype = this.options.getCameraType();
         this.options.setCameraType(this.options.getCameraType().cycle());
         if (cameratype.isFirstPerson() != this.options.getCameraType().isFirstPerson()) {
            this.gameRenderer.checkEntityPostEffect(this.options.getCameraType().isFirstPerson() ? this.getCameraEntity() : null);
         }

         this.levelRenderer.needsUpdate();
      }

      while (this.options.keySmoothCamera.consumeClick()) {
         this.options.smoothCamera = !this.options.smoothCamera;
      }

      for (int i = 0; i < 9; i++) {
         boolean flag = this.options.keySaveHotbarActivator.isDown();
         boolean flag1 = this.options.keyLoadHotbarActivator.isDown();
         if (this.options.keyHotbarSlots[i].consumeClick()) {
            if (this.player.isSpectator()) {
               this.gui.getSpectatorGui().onHotbarSelected(i);
            } else if (this.player.hasInfiniteMaterials() && this.screen == null && (flag1 || flag)) {
               CreativeModeInventoryScreen.handleHotbarLoadOrSave(this, i, flag1, flag);
            } else {
               this.player.getInventory().setSelectedSlot(i);
            }
         }
      }

      while (this.options.keySocialInteractions.consumeClick()) {
         if (!this.isMultiplayerServer() && !SharedConstants.DEBUG_SOCIAL_INTERACTIONS) {
            this.player.displayClientMessage(SOCIAL_INTERACTIONS_NOT_AVAILABLE, true);
            this.narrator.saySystemNow(SOCIAL_INTERACTIONS_NOT_AVAILABLE);
         } else {
            if (this.socialInteractionsToast != null) {
               this.socialInteractionsToast.hide();
               this.socialInteractionsToast = null;
            }

            this.setScreen(new SocialInteractionsScreen());
         }
      }

      while (this.options.keyInventory.consumeClick()) {
         if (this.gameMode.isServerControlledInventory()) {
            this.player.sendOpenInventory();
         } else {
            this.tutorial.onOpenInventory();
            this.setScreen(new InventoryScreen(this.player));
         }
      }

      while (this.options.keyAdvancements.consumeClick()) {
         this.setScreen(new AdvancementsScreen(this.player.connection.getAdvancements()));
      }

      while (this.options.keyQuickActions.consumeClick()) {
         this.getQuickActionsDialog().ifPresent(p_404787_ -> this.player.connection.showDialog((Holder<Dialog>)p_404787_, this.screen));
      }

      while (this.options.keySwapOffhand.consumeClick()) {
         if (!this.player.isSpectator()) {
            this.getConnection()
               .send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
         }
      }

      while (this.options.keyDrop.consumeClick()) {
         if (!this.player.isSpectator() && this.player.drop(this.hasControlDown())) {
            this.player.swing(InteractionHand.MAIN_HAND);
         }
      }

      while (this.options.keyChat.consumeClick()) {
         this.openChatScreen(ChatComponent.ChatMethod.MESSAGE);
      }

      if (this.screen == null && this.overlay == null && this.options.keyCommand.consumeClick()) {
         this.openChatScreen(ChatComponent.ChatMethod.COMMAND);
      }

      boolean flag2 = false;
      if (this.player.isUsingItem()) {
         if (!this.options.keyUse.isDown()) {
            this.gameMode.releaseUsingItem(this.player);
         }

         while (this.options.keyAttack.consumeClick()) {
         }

         while (this.options.keyUse.consumeClick()) {
         }

         while (this.options.keyPickItem.consumeClick()) {
         }
      } else {
         while (this.options.keyAttack.consumeClick()) {
            flag2 |= this.startAttack();
         }

         while (this.options.keyUse.consumeClick()) {
            this.startUseItem();
         }

         while (this.options.keyPickItem.consumeClick()) {
            this.pickBlock();
         }

         if (this.player.isSpectator()) {
            while (this.options.keySpectatorHotbar.consumeClick()) {
               this.gui.getSpectatorGui().onHotbarActionKeyPressed();
            }
         }
      }

      if (this.options.keyUse.isDown() && this.rightClickDelay == 0 && !this.player.isUsingItem()) {
         this.startUseItem();
      }

      this.continueAttack(this.screen == null && !flag2 && this.options.keyAttack.isDown() && this.mouseHandler.isMouseGrabbed());
   }

   private Optional<Holder<Dialog>> getQuickActionsDialog() {
      Registry<Dialog> registry = this.player.connection.registryAccess().lookupOrThrow(Registries.DIALOG);
      return registry.get(DialogTags.QUICK_ACTIONS).flatMap(p_404789_ -> {
         if (p_404789_.size() == 0) {
            return Optional.empty();
         } else {
            return p_404789_.size() == 1 ? Optional.of(p_404789_.get(0)) : registry.get(Dialogs.QUICK_ACTIONS);
         }
      });
   }

   public ClientTelemetryManager getTelemetryManager() {
      return this.telemetryManager;
   }

   public double getGpuUtilization() {
      return this.gpuUtilization;
   }

   public ProfileKeyPairManager getProfileKeyPairManager() {
      return this.profileKeyPairManager;
   }

   public WorldOpenFlows createWorldOpenFlows() {
      return new WorldOpenFlows(this, this.levelSource);
   }

   public void doWorldLoad(LevelStorageSource.LevelStorageAccess p_261564_, PackRepository p_261826_, WorldStem p_261470_, boolean p_261465_) {
      this.disconnectWithProgressScreen();
      Instant instant = Instant.now();
      LevelLoadTracker levelloadtracker = new LevelLoadTracker(p_261465_ ? 500L : 0L);
      LevelLoadingScreen levelloadingscreen = new LevelLoadingScreen(levelloadtracker, LevelLoadingScreen.Reason.OTHER);
      this.setScreen(levelloadingscreen);
      int i = Math.max(5, 3) + ChunkLevel.RADIUS_AROUND_FULL_CHUNK + 1;

      try {
         p_261564_.saveDataTag(p_261470_.registries().compositeAccess(), p_261470_.worldData());
         LevelLoadListener levelloadlistener = LevelLoadListener.compose(levelloadtracker, LoggingLevelLoadListener.forSingleplayer());
         this.singleplayerServer = MinecraftServer.spin(
            p_420644_ -> new IntegratedServer(p_420644_, this, p_261564_, p_261826_, p_261470_, this.services, levelloadlistener)
         );
         levelloadtracker.setServerChunkStatusView(this.singleplayerServer.createChunkLoadStatusView(i));
         this.isLocalServer = true;
         this.updateReportEnvironment(ReportEnvironment.local());
         this.quickPlayLog.setWorldData(QuickPlayLog.Type.SINGLEPLAYER, p_261564_.getLevelId(), p_261470_.worldData().getLevelName());
      } catch (Throwable throwable) {
         CrashReport crashreport = CrashReport.forThrowable(throwable, "Starting integrated server");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Starting integrated server");
         crashreportcategory.setDetail("Level ID", p_261564_.getLevelId());
         crashreportcategory.setDetail("Level Name", () -> p_261470_.worldData().getLevelName());
         throw new ReportedException(crashreport);
      }

      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("waitForServer");
      long k = TimeUnit.SECONDS.toNanos(1L) / 60L;

      while (!this.singleplayerServer.isReady() || this.overlay != null) {
         long j = Util.getNanos() + k;
         levelloadingscreen.tick();
         if (this.overlay != null) {
            this.overlay.tick();
         }

         this.runTick(false);
         this.runAllTasks();
         this.managedBlock(() -> Util.getNanos() > j);
         this.handleDelayedCrash();
      }

      profilerfiller.pop();
      Duration duration = Duration.between(instant, Instant.now());
      SocketAddress socketaddress = this.singleplayerServer.getConnection().startMemoryChannel();
      Connection connection = Connection.connectToLocalServer(socketaddress);
      connection.initiateServerboundPlayConnection(
         socketaddress.toString(),
         0,
         new ClientHandshakePacketListenerImpl(connection, this, null, null, p_261465_, duration, p_231442_ -> {}, levelloadtracker, null)
      );
      connection.send(new ServerboundHelloPacket(this.getUser().getName(), this.getUser().getProfileId()));
      this.pendingConnection = connection;
   }

   public void setLevel(ClientLevel p_91157_) {
      this.level = p_91157_;
      this.updateLevelInEngines(p_91157_);
   }

   public void disconnectFromWorld(Component p_426769_) {
      boolean flag = this.isLocalServer();
      ServerData serverdata = this.getCurrentServer();
      if (this.level != null) {
         this.level.disconnect(p_426769_);
      }

      if (flag) {
         this.disconnectWithSavingScreen();
      } else {
         this.disconnectWithProgressScreen();
      }

      TitleScreen titlescreen = new TitleScreen();
      if (flag) {
         this.setScreen(titlescreen);
      } else if (serverdata != null && serverdata.isRealm()) {
         this.setScreen(new RealmsMainScreen(titlescreen));
      } else {
         this.setScreen(new JoinMultiplayerScreen(titlescreen));
      }
   }

   public void disconnectWithSavingScreen() {
      this.disconnect(new GenericMessageScreen(SAVING_LEVEL), false);
   }

   public void disconnectWithProgressScreen() {
      this.disconnectWithProgressScreen(true);
   }

   public void disconnectWithProgressScreen(boolean p_458973_) {
      this.disconnect(new ProgressScreen(true), false, p_458973_);
   }

   public void disconnect(Screen p_335030_, boolean p_330226_) {
      this.disconnect(p_335030_, p_330226_, true);
   }

   public void disconnect(Screen p_457642_, boolean p_451618_, boolean p_453114_) {
      ClientPacketListener clientpacketlistener = this.getConnection();
      if (clientpacketlistener != null) {
         this.dropAllTasks();
         clientpacketlistener.close();
         if (!p_451618_) {
            this.clearDownloadedResourcePacks();
         }
      }

      this.playerSocialManager.stopOnlineMode();
      if (this.metricsRecorder.isRecording()) {
         this.debugClientMetricsCancel();
      }

      IntegratedServer integratedserver = this.singleplayerServer;
      this.singleplayerServer = null;
      this.gameRenderer.resetData();
      this.gameMode = null;
      this.narrator.clear();
      this.clientLevelTeardownInProgress = true;

      try {
         if (this.level != null) {
            this.gui.onDisconnected();
         }

         if (integratedserver != null) {
            this.setScreen(new GenericMessageScreen(SAVING_LEVEL));
            ProfilerFiller profilerfiller = Profiler.get();
            profilerfiller.push("waitForServer");

            while (!integratedserver.isShutdown()) {
               this.runTick(false);
            }

            profilerfiller.pop();
         }

         this.setScreenAndShow(p_457642_);
         this.isLocalServer = false;
         this.level = null;
         this.updateLevelInEngines(null, p_453114_);
         this.player = null;
      } finally {
         this.clientLevelTeardownInProgress = false;
      }
   }

   public void clearDownloadedResourcePacks() {
      this.downloadedPackSource.cleanupAfterDisconnect();
      this.runAllTasks();
   }

   public void clearClientLevel(Screen p_297406_) {
      ClientPacketListener clientpacketlistener = this.getConnection();
      if (clientpacketlistener != null) {
         clientpacketlistener.clearLevel();
      }

      if (this.metricsRecorder.isRecording()) {
         this.debugClientMetricsCancel();
      }

      this.gameRenderer.resetData();
      this.gameMode = null;
      this.narrator.clear();
      this.clientLevelTeardownInProgress = true;

      try {
         this.setScreenAndShow(p_297406_);
         this.gui.onDisconnected();
         this.level = null;
         this.updateLevelInEngines(null);
         this.player = null;
      } finally {
         this.clientLevelTeardownInProgress = false;
      }
   }

   public void setScreenAndShow(Screen p_91347_) {
      try (Zone zone = Profiler.get().zone("forcedTick")) {
         this.setScreen(p_91347_);
         this.runTick(false);
      }
   }

   private void updateLevelInEngines(@Nullable ClientLevel p_91325_) {
      this.updateLevelInEngines(p_91325_, true);
   }

   private void updateLevelInEngines(@Nullable ClientLevel p_455017_, boolean p_459891_) {
      if (p_459891_) {
         this.soundManager.stop();
      }

      this.setCameraEntity(null);
      this.pendingConnection = null;
      this.levelRenderer.setLevel(p_455017_);
      this.particleEngine.setLevel(p_455017_);
      this.gameRenderer.setLevel(p_455017_);
      this.updateTitle();
   }

   private UserProperties userProperties() {
      return this.userPropertiesFuture.join();
   }

   public boolean telemetryOptInExtra() {
      return this.extraTelemetryAvailable() && this.options.telemetryOptInExtra().get();
   }

   public boolean extraTelemetryAvailable() {
      return this.allowsTelemetry() && this.userProperties().flag(UserFlag.OPTIONAL_TELEMETRY_AVAILABLE);
   }

   public boolean allowsTelemetry() {
      return SharedConstants.IS_RUNNING_IN_IDE && !SharedConstants.DEBUG_FORCE_TELEMETRY ? false : this.userProperties().flag(UserFlag.TELEMETRY_ENABLED);
   }

   public boolean allowsMultiplayer() {
      return this.allowsMultiplayer && this.userProperties().flag(UserFlag.SERVERS_ALLOWED) && this.multiplayerBan() == null && !this.isNameBanned();
   }

   public boolean allowsRealms() {
      return this.userProperties().flag(UserFlag.REALMS_ALLOWED) && this.multiplayerBan() == null;
   }

   public @Nullable BanDetails multiplayerBan() {
      return (BanDetails)this.userProperties().bannedScopes().get("MULTIPLAYER");
   }

   public boolean isNameBanned() {
      ProfileResult profileresult = this.profileFuture.getNow(null);
      return profileresult != null && profileresult.actions().contains(ProfileActionType.FORCED_NAME_CHANGE);
   }

   public boolean isBlocked(UUID p_91247_) {
      return this.getChatStatus().isChatAllowed(false)
         ? this.playerSocialManager.shouldHideMessageFrom(p_91247_)
         : (this.player == null || !p_91247_.equals(this.player.getUUID())) && !p_91247_.equals(Util.NIL_UUID);
   }

   public Minecraft.ChatStatus getChatStatus() {
      if (this.options.chatVisibility().get() == ChatVisiblity.HIDDEN) {
         return Minecraft.ChatStatus.DISABLED_BY_OPTIONS;
      } else if (!this.allowsChat) {
         return Minecraft.ChatStatus.DISABLED_BY_LAUNCHER;
      } else {
         return !this.userProperties().flag(UserFlag.CHAT_ALLOWED) ? Minecraft.ChatStatus.DISABLED_BY_PROFILE : Minecraft.ChatStatus.ENABLED;
      }
   }

   public final boolean isDemo() {
      return ModMain.getOptions().getExpd_demoModeValue();
   }

   public final boolean canSwitchGameMode() {
      return this.player != null && this.gameMode != null;
   }

   public @Nullable ClientPacketListener getConnection() {
      return this.player == null ? null : this.player.connection;
   }

   public static boolean renderNames() {
      return !instance.options.hideGui;
   }

   public static boolean useShaderTransparency() {
      return !instance.gameRenderer.isPanoramicMode() && instance.options.improvedTransparency().get();
   }

   public static boolean useAmbientOcclusion() {
      return instance.options.ambientOcclusion().get();
   }

   private void pickBlock() {
      if (this.hitResult != null && this.hitResult.getType() != HitResult.Type.MISS) {
         boolean flag = this.hasControlDown();
         switch (this.hitResult) {
            case BlockHitResult blockhitresult:
               this.gameMode.handlePickItemFromBlock(blockhitresult.getBlockPos(), flag);
               break;
            case EntityHitResult entityhitresult:
               this.gameMode.handlePickItemFromEntity(entityhitresult.getEntity(), flag);
               break;
            default:
         }
      }
   }

   public CrashReport fillReport(CrashReport p_91355_) {
      SystemReport systemreport = p_91355_.getSystemReport();

      try {
         fillSystemReport(systemreport, this, this.languageManager, this.launchedVersion, this.options);
         this.fillUptime(p_91355_.addCategory("Uptime"));
         if (this.level != null) {
            this.level.fillReportDetails(p_91355_);
         }

         if (this.singleplayerServer != null) {
            this.singleplayerServer.fillSystemReport(systemreport);
         }

         this.reloadStateTracker.fillCrashReport(p_91355_);
      } catch (Throwable throwable) {
         LOGGER.error("Failed to collect details", throwable);
      }

      return p_91355_;
   }

   public static void fillReport(
      @Nullable Minecraft p_167873_, @Nullable LanguageManager p_167874_, String p_167875_, @Nullable Options p_167876_, CrashReport p_167877_
   ) {
      SystemReport systemreport = p_167877_.getSystemReport();
      fillSystemReport(systemreport, p_167873_, p_167874_, p_167875_, p_167876_);
   }

   private static String formatSeconds(double p_311783_) {
      return String.format(Locale.ROOT, "%.3fs", p_311783_);
   }

   private void fillUptime(CrashReportCategory p_309523_) {
      p_309523_.setDetail("JVM uptime", () -> formatSeconds(ManagementFactory.getRuntimeMXBean().getUptime() / 1000.0));
      p_309523_.setDetail("Wall uptime", () -> formatSeconds((System.currentTimeMillis() - this.clientStartTimeMs) / 1000.0));
      p_309523_.setDetail("High-res time", () -> formatSeconds(Util.getMillis() / 1000.0));
      p_309523_.setDetail("Client ticks", () -> String.format(Locale.ROOT, "%d ticks / %.3fs", this.clientTickCount, this.clientTickCount / 20.0));
   }

   private static SystemReport fillSystemReport(
      SystemReport p_167851_, @Nullable Minecraft p_167852_, @Nullable LanguageManager p_167853_, String p_167854_, @Nullable Options p_167855_
   ) {
      p_167851_.setDetail("Launched Version", () -> p_167854_);
      String s = getLauncherBrand();
      if (s != null) {
         p_167851_.setDetail("Launcher name", s);
      }

      p_167851_.setDetail("Backend library", RenderSystem::getBackendDescription);
      p_167851_.setDetail("Backend API", RenderSystem::getApiDescription);
      p_167851_.setDetail("Window size", () -> p_167852_ != null ? p_167852_.window.getWidth() + "x" + p_167852_.window.getHeight() : "<not initialized>");
      p_167851_.setDetail("GFLW Platform", Window::getPlatform);
      p_167851_.setDetail("Render Extensions", () -> String.join(", ", RenderSystem.getDevice().getEnabledExtensions()));
      p_167851_.setDetail("GL debug messages", () -> {
         GpuDevice gpudevice = RenderSystem.tryGetDevice();
         if (gpudevice == null) {
            return "<no renderer available>";
         } else {
            return gpudevice.isDebuggingEnabled() ? String.join("\n", gpudevice.getLastDebugMessages()) : "<debugging unavailable>";
         }
      });
      p_167851_.setDetail("Is Modded", () -> checkModStatus().fullDescription());
      p_167851_.setDetail("Universe", () -> p_167852_ != null ? Long.toHexString(p_167852_.canary) : "404");
      p_167851_.setDetail("Type", "Client (map_client.txt)");
      if (p_167855_ != null) {
         if (p_167852_ != null) {
            String s1 = p_167852_.getGpuWarnlistManager().getAllWarnings();
            if (s1 != null) {
               p_167851_.setDetail("GPU Warnings", s1);
            }
         }

         p_167851_.setDetail("Transparency", p_167855_.improvedTransparency().get() ? "shader" : "regular");
         p_167851_.setDetail("Render Distance", p_167855_.getEffectiveRenderDistance() + "/" + p_167855_.renderDistance().get() + " chunks");
      }

      if (p_167852_ != null) {
         p_167851_.setDetail("Resource Packs", () -> PackRepository.displayPackList(p_167852_.getResourcePackRepository().getSelectedPacks()));
      }

      if (p_167853_ != null) {
         p_167851_.setDetail("Current Language", () -> p_167853_.getSelected());
      }

      p_167851_.setDetail("Locale", String.valueOf(Locale.getDefault()));
      p_167851_.setDetail("System encoding", () -> System.getProperty("sun.jnu.encoding", "<not set>"));
      p_167851_.setDetail("File encoding", () -> System.getProperty("file.encoding", "<not set>"));
      p_167851_.setDetail("CPU", GLX::_getCpuInfo);
      return p_167851_;
   }

   public static Minecraft getInstance() {
      return instance;
   }

   public CompletableFuture<Void> delayTextureReload() {
      return this.<CompletableFuture<Void>>submit(this::reloadResourcePacks).thenCompose(p_231391_ -> (CompletionStage<Void>)p_231391_);
   }

   public void updateReportEnvironment(ReportEnvironment p_239477_) {
      if (!this.reportingContext.matches(p_239477_)) {
         this.reportingContext = ReportingContext.create(p_239477_, this.userApiService);
      }
   }

   public @Nullable ServerData getCurrentServer() {
      return Optionull.map(this.getConnection(), ClientPacketListener::getServerData);
   }

   public boolean isLocalServer() {
      return this.isLocalServer;
   }

   public boolean hasSingleplayerServer() {
      return this.isLocalServer && this.singleplayerServer != null;
   }

   public @Nullable IntegratedServer getSingleplayerServer() {
      return this.singleplayerServer;
   }

   public boolean isSingleplayer() {
      IntegratedServer integratedserver = this.getSingleplayerServer();
      return integratedserver != null && !integratedserver.isPublished();
   }

   public boolean isLocalPlayer(UUID p_298914_) {
      return p_298914_.equals(this.getUser().getProfileId());
   }

   public User getUser() {
      return this.user;
   }

   public GameProfile getGameProfile() {
      ProfileResult profileresult = this.profileFuture.join();
      return profileresult != null ? profileresult.profile() : new GameProfile(this.user.getProfileId(), this.user.getName());
   }

   public Proxy getProxy() {
      return this.proxy;
   }

   public TextureManager getTextureManager() {
      return this.textureManager;
   }

   public ShaderManager getShaderManager() {
      return this.shaderManager;
   }

   public ResourceManager getResourceManager() {
      return this.resourceManager;
   }

   public PackRepository getResourcePackRepository() {
      return this.resourcePackRepository;
   }

   public VanillaPackResources getVanillaPackResources() {
      return this.vanillaPackResources;
   }

   public DownloadedPackSource getDownloadedPackSource() {
      return this.downloadedPackSource;
   }

   public Path getResourcePackDirectory() {
      return this.resourcePackDirectory;
   }

   public LanguageManager getLanguageManager() {
      return this.languageManager;
   }

   public boolean isPaused() {
      return this.pause;
   }

   public GpuWarnlistManager getGpuWarnlistManager() {
      return this.gpuWarnlistManager;
   }

   public SoundManager getSoundManager() {
      return this.soundManager;
   }

   public @Nullable Music getSituationalMusic() {
      Music music = Optionull.map(this.screen, Screen::getBackgroundMusic);
      if (music != null) {
         return music;
      }

      Camera camera = this.gameRenderer.getMainCamera();
      if (this.player != null && camera != null) {
         Level level = this.player.level();
         if (level.dimension() == Level.END && this.gui.getBossOverlay().shouldPlayMusic()) {
            return Musics.END_BOSS;
         }

         BackgroundMusic backgroundmusic = camera.attributeProbe().getValue(EnvironmentAttributes.BACKGROUND_MUSIC, 1.0F);
         boolean flag = this.player.getAbilities().instabuild && this.player.getAbilities().mayfly;
         boolean flag1 = this.player.isUnderWater();
         return backgroundmusic.select(flag, flag1).orElse(null);
      } else {
         return Musics.MENU;
      }
   }

   public float getMusicVolume() {
      if (this.screen != null && this.screen.getBackgroundMusic() != null) {
         return 1.0F;
      }

      Camera camera = this.gameRenderer.getMainCamera();
      return camera != null ? camera.attributeProbe().getValue(EnvironmentAttributes.MUSIC_VOLUME, 1.0F) : 1.0F;
   }

   public Services services() {
      return this.services;
   }

   public SkinManager getSkinManager() {
      return this.skinManager;
   }

   public @Nullable Entity getCameraEntity() {
      return this.cameraEntity;
   }

   public void setCameraEntity(@Nullable Entity p_91119_) {
      this.cameraEntity = p_91119_;
      this.gameRenderer.checkEntityPostEffect(p_91119_);
   }

   public boolean shouldEntityAppearGlowing(Entity p_91315_) {
      return p_91315_.isCurrentlyGlowing()
         || this.player != null && this.player.isSpectator() && this.options.keySpectatorOutlines.isDown() && p_91315_.getType() == EntityType.PLAYER;
   }

   @Override
   protected Thread getRunningThread() {
      return this.gameThread;
   }

   @Override
   public Runnable wrapRunnable(Runnable p_91376_) {
      return p_91376_;
   }

   @Override
   protected boolean shouldRun(Runnable p_91365_) {
      return true;
   }

   public BlockRenderDispatcher getBlockRenderer() {
      return this.blockRenderer;
   }

   public EntityRenderDispatcher getEntityRenderDispatcher() {
      return this.entityRenderDispatcher;
   }

   public BlockEntityRenderDispatcher getBlockEntityRenderDispatcher() {
      return this.blockEntityRenderDispatcher;
   }

   public ItemRenderer getItemRenderer() {
      return this.itemRenderer;
   }

   public MapRenderer getMapRenderer() {
      return this.mapRenderer;
   }

   public DataFixer getFixerUpper() {
      return this.fixerUpper;
   }

   public DeltaTracker getDeltaTracker() {
      return this.deltaTracker;
   }

   public BlockColors getBlockColors() {
      return this.blockColors;
   }

   public boolean showOnlyReducedInfo() {
      return this.player != null && this.player.isReducedDebugInfo() || this.options.reducedDebugInfo().get();
   }

   public ToastManager getToastManager() {
      return this.toastManager;
   }

   public Tutorial getTutorial() {
      return this.tutorial;
   }

   public boolean isWindowActive() {
      return this.windowActive;
   }

   public HotbarManager getHotbarManager() {
      return this.hotbarManager;
   }

   public ModelManager getModelManager() {
      return this.modelManager;
   }

   public AtlasManager getAtlasManager() {
      return this.atlasManager;
   }

   public MapTextureManager getMapTextureManager() {
      return this.mapTextureManager;
   }

   public WaypointStyleManager getWaypointStyles() {
      return this.waypointStyles;
   }

   @Override
   public void setWindowActive(boolean p_91261_) {
      this.windowActive = p_91261_;
   }

   public Component grabPanoramixScreenshot(File p_167900_) {
      int i = 4;
      int j = 4096;
      int k = 4096;
      int l = this.window.getWidth();
      int i1 = this.window.getHeight();
      RenderTarget rendertarget = this.getMainRenderTarget();
      float f = this.player.getXRot();
      float f1 = this.player.getYRot();
      float f2 = this.player.xRotO;
      float f3 = this.player.yRotO;
      this.gameRenderer.setRenderBlockOutline(false);

      MutableComponent mutablecomponent;
      try {
         this.gameRenderer.setPanoramicScreenshotParameters(new PanoramicScreenshotParameters(new Vector3f(this.gameRenderer.getMainCamera().forwardVector())));
         this.window.setWidth(4096);
         this.window.setHeight(4096);
         rendertarget.resize(4096, 4096);

         for (int j1 = 0; j1 < 6; j1++) {
            switch (j1) {
               case 0:
                  this.player.setYRot(f1);
                  this.player.setXRot(0.0F);
                  break;
               case 1:
                  this.player.setYRot((f1 + 90.0F) % 360.0F);
                  this.player.setXRot(0.0F);
                  break;
               case 2:
                  this.player.setYRot((f1 + 180.0F) % 360.0F);
                  this.player.setXRot(0.0F);
                  break;
               case 3:
                  this.player.setYRot((f1 - 90.0F) % 360.0F);
                  this.player.setXRot(0.0F);
                  break;
               case 4:
                  this.player.setYRot(f1);
                  this.player.setXRot(-90.0F);
                  break;
               case 5:
               default:
                  this.player.setYRot(f1);
                  this.player.setXRot(90.0F);
            }

            this.player.yRotO = this.player.getYRot();
            this.player.xRotO = this.player.getXRot();
            this.gameRenderer.updateCamera(DeltaTracker.ONE);
            this.gameRenderer.renderLevel(DeltaTracker.ONE);

            try {
               Thread.sleep(10L);
            } catch (InterruptedException var19) {
            }

            Screenshot.grab(p_167900_, "panorama_" + j1 + ".png", rendertarget, 4, p_231415_ -> {});
         }

         Component component = Component.literal(p_167900_.getName())
            .withStyle(ChatFormatting.UNDERLINE)
            .withStyle(p_389130_ -> p_389130_.withClickEvent(new ClickEvent.OpenFile(p_167900_.getAbsoluteFile())));
         return Component.translatable("screenshot.success", component);
      } catch (Exception exception) {
         LOGGER.error("Couldn't save image", exception);
         mutablecomponent = Component.translatable("screenshot.failure", exception.getMessage());
      } finally {
         this.player.setXRot(f);
         this.player.setYRot(f1);
         this.player.xRotO = f2;
         this.player.yRotO = f3;
         this.gameRenderer.setRenderBlockOutline(true);
         this.window.setWidth(l);
         this.window.setHeight(i1);
         rendertarget.resize(l, i1);
         this.gameRenderer.setPanoramicScreenshotParameters(null);
      }

      return mutablecomponent;
   }

   public SplashManager getSplashManager() {
      return this.splashManager;
   }

   public @Nullable Overlay getOverlay() {
      return this.overlay;
   }

   public PlayerSocialManager getPlayerSocialManager() {
      return this.playerSocialManager;
   }

   public Window getWindow() {
      return this.window;
   }

   public FramerateLimitTracker getFramerateLimitTracker() {
      return this.framerateLimitTracker;
   }

   public DebugScreenOverlay getDebugOverlay() {
      return this.gui.getDebugOverlay();
   }

   public RenderBuffers renderBuffers() {
      return this.renderBuffers;
   }

   public void updateMaxMipLevel(int p_91313_) {
      this.atlasManager.updateMaxMipLevel(p_91313_);
   }

   public EntityModelSet getEntityModels() {
      return this.modelManager.entityModels().get();
   }

   public boolean isTextFilteringEnabled() {
      return this.userProperties().flag(UserFlag.PROFANITY_FILTER_ENABLED);
   }

   public void prepareForMultiplayer() {
      this.playerSocialManager.startOnlineMode();
      this.getProfileKeyPairManager().prepareKeyPair();
   }

   public InputType getLastInputType() {
      return this.lastInputType;
   }

   public void setLastInputType(InputType p_265509_) {
      this.lastInputType = p_265509_;
   }

   public GameNarrator getNarrator() {
      return this.narrator;
   }

   public ChatListener getChatListener() {
      return this.chatListener;
   }

   public ReportingContext getReportingContext() {
      return this.reportingContext;
   }

   public RealmsDataFetcher realmsDataFetcher() {
      return this.realmsDataFetcher;
   }

   public QuickPlayLog quickPlayLog() {
      return this.quickPlayLog;
   }

   public CommandHistory commandHistory() {
      return this.commandHistory;
   }

   public DirectoryValidator directoryValidator() {
      return this.directoryValidator;
   }

   public PlayerSkinRenderCache playerSkinRenderCache() {
      return this.playerSkinRenderCache;
   }

   private float getTickTargetMillis(float p_311597_) {
      if (this.level != null) {
         TickRateManager tickratemanager = this.level.tickRateManager();
         if (tickratemanager.runsNormally()) {
            return Math.max(p_311597_, tickratemanager.millisecondsPerTick());
         }
      }

      return p_311597_;
   }

   public ItemModelResolver getItemModelResolver() {
      return this.itemModelResolver;
   }

   public boolean canInterruptScreen() {
      return (this.screen == null || this.screen.canInterruptWithAnotherScreen()) && !this.clientLevelTeardownInProgress;
   }

   public static @Nullable String getLauncherBrand() {
      return System.getProperty("minecraft.launcher.brand");
   }

   public PacketProcessor packetProcessor() {
      return this.packetProcessor;
   }

   public Gizmos.TemporaryCollection collectPerTickGizmos() {
      return Gizmos.withCollector(this.perTickGizmos);
   }

   public Collection<SimpleGizmoCollector.GizmoInstance> getPerTickGizmos() {
      return this.drainedLatestTickGizmos;
   }

   @OnlyIn(Dist.CLIENT)
   public enum ChatStatus {
      ENABLED(CommonComponents.EMPTY) {
         @Override
         public boolean isChatAllowed(boolean p_168045_) {
            return true;
         }
      },
      DISABLED_BY_OPTIONS(Component.translatable("chat.disabled.options").withStyle(ChatFormatting.RED)) {
         @Override
         public boolean isChatAllowed(boolean p_168051_) {
            return false;
         }
      },
      DISABLED_BY_LAUNCHER(Component.translatable("chat.disabled.launcher").withStyle(ChatFormatting.RED)) {
         @Override
         public boolean isChatAllowed(boolean p_168057_) {
            return p_168057_;
         }
      },
      DISABLED_BY_PROFILE(
         Component.translatable("chat.disabled.profile", Component.keybind(Minecraft.instance.options.keyChat.getName())).withStyle(ChatFormatting.RED)
      ) {
         @Override
         public boolean isChatAllowed(boolean p_168063_) {
            return p_168063_;
         }
      };

      static final Component INFO_DISABLED_BY_PROFILE = Component.translatable("chat.disabled.profile.moreInfo");
      private final Component message;

      ChatStatus(final Component p_168033_) {
         this.message = p_168033_;
      }

      public Component getMessage() {
         return this.message;
      }

      public abstract boolean isChatAllowed(boolean var1);
   }

   @OnlyIn(Dist.CLIENT)
   record GameLoadCookie(RealmsClient realmsClient, GameConfig.QuickPlayData quickPlayData) {
   }
}
