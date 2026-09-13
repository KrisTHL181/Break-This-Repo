package net.minecraft.client.gui.screens.worldselection;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.commands.Commands;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.FileUtil;
import net.minecraft.util.Util;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelDataAndDimensions;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.validation.DirectoryValidator;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class CreateWorldScreen extends Screen {
   private static final int GROUP_BOTTOM = 1;
   private static final int TAB_COLUMN_WIDTH = 210;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String TEMP_WORLD_PREFIX = "mcworld-";
   private static final Component GAME_MODEL_LABEL = Component.translatable("selectWorld.gameMode");
   private static final Component NAME_LABEL = Component.translatable("selectWorld.enterName");
   private static final Component EXPERIMENTS_LABEL = Component.translatable("selectWorld.experiments");
   private static final Component ALLOW_COMMANDS_INFO = Component.translatable("selectWorld.allowCommands.info");
   private static final Component PREPARING_WORLD_DATA = Component.translatable("createWorld.preparing");
   private static final int HORIZONTAL_BUTTON_SPACING = 10;
   private static final int VERTICAL_BUTTON_SPACING = 8;
   public static final Identifier TAB_HEADER_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/tab_header_background.png");
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private final WorldCreationUiState uiState;
   private final TabManager tabManager = new TabManager(x$0 -> this.addRenderableWidget(x$0), x$0 -> this.removeWidget(x$0));
   private boolean recreated;
   private final DirectoryValidator packValidator;
   private final CreateWorldCallback createWorldCallback;
   private final Runnable onClose;
   private @Nullable Path tempDataPackDir;
   private @Nullable PackRepository tempDataPackRepository;
   private @Nullable MenuTabBar tabNavigationBar;

   public static void openFresh(final Minecraft minecraft, final Runnable onClose) {
      openFresh(
         minecraft,
         onClose,
         (createWorldScreen, finalLayers, worldDataAndGenSettings, gameRules, tempDataPackDir) -> createWorldScreen.createNewWorld(
            finalLayers, worldDataAndGenSettings, gameRules
         )
      );
   }

   public static void openFresh(final Minecraft minecraft, final Runnable onClose, final CreateWorldCallback createWorld) {
      WorldCreationContextMapper worldCreationContext = (managers, registries, cookie) -> new WorldCreationContext(
         cookie.worldGenSettings(), registries, managers, cookie.dataConfiguration()
      );
      Function<WorldLoader.DataLoadContext, WorldGenSettings> worldGenSettings = context -> new WorldGenSettings(
         WorldOptions.defaultWithRandomSeed(), WorldPresets.createNormalWorldDimensions(context.datapackWorldRegistries())
      );
      openCreateWorldScreen(minecraft, onClose, worldGenSettings, worldCreationContext, WorldPresets.NORMAL, createWorld);
   }

   public static void testWorld(final Minecraft minecraft, final Runnable onClose) {
      WorldCreationContextMapper worldCreationContext = (managers, registries, cookie) -> new WorldCreationContext(
         cookie.worldGenSettings().options(),
         cookie.worldGenSettings().dimensions(),
         registries,
         managers,
         cookie.dataConfiguration(),
         new InitialWorldCreationOptions(
            WorldCreationUiState.SelectedGameMode.CREATIVE,
            new GameRuleMap.Builder().set(GameRules.ADVANCE_TIME, false).set(GameRules.ADVANCE_WEATHER, false).set(GameRules.SPAWN_MOBS, false).build(),
            null
         )
      );
      Function<WorldLoader.DataLoadContext, WorldGenSettings> worldGenSettings = context -> new WorldGenSettings(
         WorldOptions.testWorldWithRandomSeed(), WorldPresets.createTestWorldDimensions(context.datapackWorldRegistries())
      );
      openCreateWorldScreen(
         minecraft,
         onClose,
         worldGenSettings,
         worldCreationContext,
         WorldPresets.FLAT_ALL_DIMENSIONS,
         (createWorldScreen, finalLayers, worldDataAndGenSettings, gameRules, tempDataPackDir) -> createWorldScreen.createNewWorld(
            finalLayers, worldDataAndGenSettings, gameRules
         )
      );
   }

   private static void openCreateWorldScreen(
      final Minecraft minecraft,
      final Runnable onClose,
      final Function<WorldLoader.DataLoadContext, WorldGenSettings> worldGenSettings,
      final WorldCreationContextMapper worldCreationContext,
      final ResourceKey<WorldPreset> worldPreset,
      final CreateWorldCallback createWorld
   ) {
      queueLoadScreen(minecraft, PREPARING_WORLD_DATA);
      long start = Util.getMillis();
      PackRepository vanillaOnlyPackRepository = new PackRepository(new ServerPacksSource(minecraft.directoryValidator()));
      WorldDataConfiguration dataConfig = SharedConstants.IS_RUNNING_IN_IDE
         ? new WorldDataConfiguration(new DataPackConfig(List.of("vanilla", "tests"), List.of()), FeatureFlags.DEFAULT_FLAGS)
         : WorldDataConfiguration.DEFAULT;
      WorldLoader.InitConfig loadConfig = createDefaultLoadConfig(vanillaOnlyPackRepository, dataConfig);
      CompletableFuture<WorldCreationContext> loadResult = WorldLoader.load(
         loadConfig,
         context -> new WorldLoader.DataLoadOutput<>(
            new DataPackReloadCookie(worldGenSettings.apply(context), context.dataConfiguration()), context.datapackDimensions()
         ),
         (resources, managers, registries, cookie) -> {
            resources.close();
            return worldCreationContext.apply(managers, registries, cookie);
         },
         Util.backgroundExecutor(),
         minecraft
      );
      minecraft.managedBlock(loadResult::isDone);
      long end = Util.getMillis();
      LOGGER.debug("Resource load for world creation blocked for {} ms", end - start);
      minecraft.gui.setScreen(new CreateWorldScreen(minecraft, onClose, loadResult.join(), Optional.of(worldPreset), OptionalLong.empty(), createWorld));
   }

   public static CreateWorldScreen createFromExisting(
      final Minecraft minecraft,
      final Runnable onClose,
      final LevelSettings levelSettings,
      final WorldCreationContext worldCreationContext,
      final @Nullable Path newDataPackDir
   ) {
      CreateWorldScreen result = new CreateWorldScreen(
         minecraft,
         onClose,
         worldCreationContext,
         WorldPresets.fromSettings(worldCreationContext.selectedDimensions()),
         OptionalLong.of(worldCreationContext.options().seed()),
         (createWorldScreen, finalLayers, worldDataAndGenSettings, gameRules, tempDataPackDir) -> createWorldScreen.createNewWorld(
            finalLayers, worldDataAndGenSettings, gameRules
         )
      );
      result.recreated = true;
      result.uiState.setName(levelSettings.levelName());
      result.uiState.setAllowCommands(levelSettings.allowCommands());
      result.uiState.setDifficulty(levelSettings.difficultySettings().difficulty());
      result.uiState.getGameRules().setAll(worldCreationContext.initialWorldCreationOptions().gameRuleOverwrites(), null);
      if (levelSettings.difficultySettings().hardcore()) {
         result.uiState.setGameMode(WorldCreationUiState.SelectedGameMode.HARDCORE);
      } else if (levelSettings.gameType().isSurvival()) {
         result.uiState.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
      } else if (levelSettings.gameType().isCreative()) {
         result.uiState.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
      }

      result.tempDataPackDir = newDataPackDir;
      return result;
   }

   private CreateWorldScreen(
      final Minecraft minecraft,
      final Runnable onClose,
      final WorldCreationContext settings,
      final Optional<ResourceKey<WorldPreset>> preset,
      final OptionalLong seed,
      final CreateWorldCallback createWorldCallback
   ) {
      super(Component.translatable("selectWorld.create"));
      this.onClose = onClose;
      this.packValidator = minecraft.directoryValidator();
      this.createWorldCallback = createWorldCallback;
      this.uiState = new WorldCreationUiState(minecraft.getLevelSource().getBaseDir(), settings, preset, seed);
   }

   public WorldCreationUiState getUiState() {
      return this.uiState;
   }

   @Override
   protected void init() {
      this.tabNavigationBar = MenuTabBar.builder(this.tabManager, this.width)
         .addTabs(new CreateWorldScreen.GameTab(), new CreateWorldScreen.WorldTab(), new CreateWorldScreen.MoreTab())
         .build();
      this.addRenderableWidget(this.tabNavigationBar);
      LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      footer.addChild(Button.builder(Component.translatable("selectWorld.create"), button -> this.onCreate()).build());
      footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.popScreen()).build());
      this.layout.visitWidgets(button -> {
         button.setTabOrderGroup(1);
         this.addRenderableWidget(button);
      });
      this.tabNavigationBar.selectTab(0, false);
      this.uiState.onChanged();
      this.repositionElements();
   }

   @Override
   protected void setInitialFocus() {
   }

   @Override
   public void repositionElements() {
      if (this.tabNavigationBar != null) {
         this.tabNavigationBar.arrangeElements(this.width);
         int tabAreaTop = this.tabNavigationBar.getRectangle().bottom();
         ScreenRectangle tabArea = new ScreenRectangle(0, tabAreaTop, this.width, this.height - this.layout.getFooterHeight() - tabAreaTop);
         this.tabManager.setTabArea(tabArea);
         this.layout.setHeaderHeight(tabAreaTop);
         this.layout.arrangeElements();
      }
   }

   private static void queueLoadScreen(final Minecraft minecraft, final Component message) {
      minecraft.setScreenAndShow(new GenericMessageScreen(message));
   }

   private void onCreate() {
      WorldCreationContext context = this.uiState.getSettings();
      WorldDimensions worldDimensions = context.selectedDimensions();
      WorldDimensions.Complete finalDimensions = worldDimensions.bake(context.datapackDimensions());
      LayeredRegistryAccess<RegistryLayer> finalLayers = context.worldgenRegistries()
         .replaceFrom(RegistryLayer.DIMENSIONS, finalDimensions.dimensionsRegistryAccess());
      FeatureFlagSet enabledFeatures = context.dataConfiguration().enabledFeatures();
      Lifecycle lifecycleFromFeatures = FeatureFlags.isExperimental(enabledFeatures) ? Lifecycle.experimental() : Lifecycle.stable();
      Lifecycle lifecycleFromRegistries = finalLayers.compositeAccess().allRegistriesLifecycle();
      Lifecycle lifecycle = lifecycleFromRegistries.add(lifecycleFromFeatures);
      boolean skipWarning = !this.recreated && lifecycleFromRegistries == Lifecycle.stable();
      boolean isDebug = finalDimensions.specialWorldProperty() == PrimaryLevelData.SpecialWorldProperty.DEBUG;
      LevelSettings levelSettings = this.createLevelSettings(isDebug);
      GameRules gameRules;
      if (isDebug) {
         gameRules = MinecraftServer.DEFAULT_GAME_RULES.get();
         gameRules.set(GameRules.ADVANCE_TIME, false, null);
      } else {
         gameRules = this.uiState.getGameRules().copy(enabledFeatures);
      }

      PrimaryLevelData worldData = new PrimaryLevelData(levelSettings, finalDimensions.specialWorldProperty(), lifecycle);
      WorldOptions options = this.uiState.getSettings().options();
      WorldGenSettings worldGenSettings = new WorldGenSettings(options, worldDimensions);
      LevelDataAndDimensions.WorldDataAndGenSettings worldDataAndGenSettings = new LevelDataAndDimensions.WorldDataAndGenSettings(worldData, worldGenSettings);
      WorldOpenFlows.confirmWorldCreation(
         this.minecraft, this, lifecycle, () -> this.createWorldAndCleanup(finalLayers, worldDataAndGenSettings, Optional.of(gameRules)), skipWarning
      );
   }

   private void createWorldAndCleanup(
      final LayeredRegistryAccess<RegistryLayer> finalLayers,
      final LevelDataAndDimensions.WorldDataAndGenSettings worldDataAndGenSettings,
      final Optional<GameRules> gameRules
   ) {
      boolean worldCreationSuccessful = this.createWorldCallback.create(this, finalLayers, worldDataAndGenSettings, gameRules, this.tempDataPackDir);
      this.removeTempDataPackDir();
      if (!worldCreationSuccessful) {
         this.popScreen();
      }
   }

   private boolean createNewWorld(
      final LayeredRegistryAccess<RegistryLayer> finalLayers,
      final LevelDataAndDimensions.WorldDataAndGenSettings worldDataAndGenSettings,
      final Optional<GameRules> gameRules
   ) {
      String worldFolder = this.uiState.getTargetFolder();
      WorldCreationContext context = this.uiState.getSettings();
      queueLoadScreen(this.minecraft, PREPARING_WORLD_DATA);
      Optional<LevelStorageSource.LevelStorageAccess> newWorldAccess = createNewWorldDirectory(this.minecraft, worldFolder, this.tempDataPackDir);
      if (newWorldAccess.isEmpty()) {
         SystemToast.onPackCopyFailure(this.minecraft, worldFolder);
         return false;
      } else {
         this.minecraft
            .createWorldOpenFlows()
            .createLevelFromExistingSettings(newWorldAccess.get(), context.dataPackResources(), finalLayers, worldDataAndGenSettings, gameRules);
         return true;
      }
   }

   private LevelSettings createLevelSettings(final boolean isDebug) {
      String name = this.uiState.getName().trim();
      return isDebug
         ? new LevelSettings(
            name, GameType.SPECTATOR, new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT
         )
         : new LevelSettings(
            name,
            this.uiState.getGameMode().gameType,
            new LevelSettings.DifficultySettings(this.uiState.getDifficulty(), this.uiState.isHardcore(), false),
            this.uiState.isAllowCommands(),
            this.uiState.getSettings().dataConfiguration()
         );
   }

   @Override
   public boolean keyPressed(final KeyEvent event) {
      if (this.tabNavigationBar.keyPressed(event)) {
         return true;
      } else if (super.keyPressed(event)) {
         return true;
      } else if (event.isConfirmation()) {
         this.onCreate();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void onClose() {
      this.popScreen();
   }

   public void popScreen() {
      this.onClose.run();
      this.removeTempDataPackDir();
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractRenderState(graphics, mouseX, mouseY, a);
      graphics.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0, this.height - this.layout.getFooterHeight() - 2, 0.0F, 0.0F, this.width, 2, 32, 2);
   }

   @Override
   protected void extractMenuBackground(final GuiGraphicsExtractor graphics) {
      graphics.blit(RenderPipelines.GUI_TEXTURED, TAB_HEADER_BACKGROUND, 0, 0, 0.0F, 0.0F, this.width, this.layout.getHeaderHeight(), 16, 16);
      this.extractMenuBackground(graphics, 0, this.layout.getHeaderHeight(), this.width, this.height);
   }

   private @Nullable Path getOrCreateTempDataPackDir() {
      if (this.tempDataPackDir == null) {
         try {
            this.tempDataPackDir = Files.createTempDirectory("mcworld-");
         } catch (IOException e) {
            LOGGER.warn("Failed to create temporary dir", e);
            SystemToast.onPackCopyFailure(this.minecraft, this.uiState.getTargetFolder());
            this.popScreen();
         }
      }

      return this.tempDataPackDir;
   }

   private void openExperimentsScreen(final WorldDataConfiguration dataConfiguration) {
      Pair<Path, PackRepository> settings = this.getDataPackSelectionSettings(dataConfiguration);
      if (settings != null) {
         this.minecraft
            .gui
            .setScreen(
               new ExperimentsScreen(
                  this, (PackRepository)settings.getSecond(), packRepository -> this.tryApplyNewDataPacks(packRepository, false, this::openExperimentsScreen)
               )
            );
      }
   }

   private void openDataPackSelectionScreen(final WorldDataConfiguration dataConfiguration) {
      Pair<Path, PackRepository> settings = this.getDataPackSelectionSettings(dataConfiguration);
      if (settings != null) {
         this.minecraft
            .gui
            .setScreen(
               new PackSelectionScreen(
                  (PackRepository)settings.getSecond(),
                  packRepository -> this.tryApplyNewDataPacks(packRepository, true, this::openDataPackSelectionScreen),
                  (Path)settings.getFirst(),
                  Component.translatable("dataPack.title")
               )
            );
      }
   }

   private void tryApplyNewDataPacks(final PackRepository packRepository, final boolean isDataPackScreen, final Consumer<WorldDataConfiguration> onAbort) {
      List<String> newEnabled = ImmutableList.copyOf(packRepository.getSelectedIds());
      List<String> newDisabled = packRepository.getAvailableIds().stream().filter(id -> !newEnabled.contains(id)).collect(ImmutableList.toImmutableList());
      WorldDataConfiguration newConfig = new WorldDataConfiguration(
         new DataPackConfig(newEnabled, newDisabled), this.uiState.getSettings().dataConfiguration().enabledFeatures()
      );
      if (this.uiState.tryUpdateDataConfiguration(newConfig)) {
         this.minecraft.gui.setScreen(this);
      } else {
         FeatureFlagSet requestedFeatureFlags = packRepository.getRequestedFeatureFlags();
         if (FeatureFlags.isExperimental(requestedFeatureFlags) && isDataPackScreen) {
            this.minecraft.gui.setScreen(new ConfirmExperimentalFeaturesScreen(packRepository.getSelectedPacks(), accepted -> {
               if (accepted) {
                  this.applyNewPackConfig(packRepository, newConfig, onAbort);
               } else {
                  onAbort.accept(this.uiState.getSettings().dataConfiguration());
               }
            }));
         } else {
            this.applyNewPackConfig(packRepository, newConfig, onAbort);
         }
      }
   }

   private void applyNewPackConfig(final PackRepository packRepository, final WorldDataConfiguration newConfig, final Consumer<WorldDataConfiguration> onAbort) {
      this.minecraft.setScreenAndShow(new GenericMessageScreen(Component.translatable("dataPack.validation.working")));
      WorldLoader.InitConfig config = createDefaultLoadConfig(packRepository, newConfig);
      WorldLoader.<DataPackReloadCookie, WorldCreationContext>load(
            config,
            context -> {
               if (context.datapackWorldRegistries().lookupOrThrow(Registries.WORLD_PRESET).listElements().findAny().isEmpty()) {
                  throw new IllegalStateException("Needs at least one world preset to continue");
               }

               if (context.datapackWorldRegistries().lookupOrThrow(Registries.BIOME).listElements().findAny().isEmpty()) {
                  throw new IllegalStateException("Needs at least one biome continue");
               }

               WorldCreationContext existingContext = this.uiState.getSettings();
               DynamicOps<JsonElement> writeOps = existingContext.worldgenLoadContext().createSerializationContext(JsonOps.INSTANCE);
               DataResult<JsonElement> encoded = WorldGenSettings.CODEC
                  .encodeStart(writeOps, new WorldGenSettings(existingContext.options(), existingContext.selectedDimensions()))
                  .setLifecycle(Lifecycle.stable());
               DynamicOps<JsonElement> readOps = context.datapackWorldRegistries().createSerializationContext(JsonOps.INSTANCE);
               WorldGenSettings settings = (WorldGenSettings)encoded.flatMap(r -> WorldGenSettings.CODEC.parse(readOps, r))
                  .getOrThrow(error -> new IllegalStateException("Error parsing worldgen settings after loading data packs: " + error));
               return new WorldLoader.DataLoadOutput<>(new DataPackReloadCookie(settings, context.dataConfiguration()), context.datapackDimensions());
            },
            (resources, managers, registries, cookie) -> {
               resources.close();
               return new WorldCreationContext(cookie.worldGenSettings(), registries, managers, cookie.dataConfiguration());
            },
            Util.backgroundExecutor(),
            this.minecraft
         )
         .thenApply(settings -> {
            settings.validate();
            return (WorldCreationContext)settings;
         })
         .thenAcceptAsync(this.uiState::setSettings, this.minecraft)
         .handleAsync(
            (nothing, throwable) -> {
               if (throwable != null) {
                  LOGGER.warn("Failed to validate datapack", throwable);
                  this.minecraft
                     .gui
                     .setScreen(
                        new ConfirmScreen(
                           retry -> {
                              if (retry) {
                                 onAbort.accept(this.uiState.getSettings().dataConfiguration());
                              } else {
                                 onAbort.accept(WorldDataConfiguration.DEFAULT);
                              }
                           },
                           Component.translatable("dataPack.validation.failed"),
                           CommonComponents.EMPTY,
                           Component.translatable("dataPack.validation.back"),
                           Component.translatable("dataPack.validation.reset")
                        )
                     );
               } else {
                  this.minecraft.gui.setScreen(this);
               }

               return null;
            },
            this.minecraft
         );
   }

   private static WorldLoader.InitConfig createDefaultLoadConfig(final PackRepository packRepository, final WorldDataConfiguration config) {
      WorldLoader.PackConfig packConfig = new WorldLoader.PackConfig(packRepository, config, false, true);
      return new WorldLoader.InitConfig(packConfig, Commands.CommandSelection.INTEGRATED, LevelBasedPermissionSet.GAMEMASTER);
   }

   private void removeTempDataPackDir() {
      if (this.tempDataPackDir != null && Files.exists(this.tempDataPackDir)) {
         try (Stream<Path> files = Files.walk(this.tempDataPackDir)) {
            files.sorted(Comparator.reverseOrder()).forEach(path -> {
               try {
                  Files.delete(path);
               } catch (IOException e) {
                  LOGGER.warn("Failed to remove temporary file {}", path, ex);
               }
            });
         } catch (IOException e) {
            LOGGER.warn("Failed to list temporary dir {}", this.tempDataPackDir);
         }
      }

      this.tempDataPackDir = null;
   }

   private static void copyBetweenDirs(final Path sourceDir, final Path targetDir, final Path sourcePath) {
      try {
         Util.copyBetweenDirs(sourceDir, targetDir, sourcePath);
      } catch (IOException e) {
         LOGGER.warn("Failed to copy datapack file from {} to {}", sourcePath, targetDir);
         throw new UncheckedIOException(e);
      }
   }

   private static Optional<LevelStorageSource.LevelStorageAccess> createNewWorldDirectory(
      final Minecraft minecraft, final String worldFolder, final @Nullable Path tempDataPackDir
   ) {
      try {
         LevelStorageSource.LevelStorageAccess access = minecraft.getLevelSource().createAccess(worldFolder);
         if (tempDataPackDir == null) {
            return Optional.of(access);
         }

         try (Stream<Path> files = Files.walk(tempDataPackDir)) {
            Path targetDir = access.getLevelPath(LevelResource.DATAPACK_DIR);
            FileUtil.createDirectoriesSafe(targetDir);
            files.filter(f -> !f.equals(tempDataPackDir)).forEach(source -> copyBetweenDirs(tempDataPackDir, targetDir, source));
            return Optional.of(access);
         } catch (IOException | UncheckedIOException e) {
            LOGGER.warn("Failed to copy datapacks to world {}", worldFolder, e);
            access.close();
         }
      } catch (IOException | UncheckedIOException e) {
         LOGGER.warn("Failed to create access for {}", worldFolder, e);
      }

      return Optional.empty();
   }

   public static Path createTempDataPackDirFromExistingWorld(final Path sourcePackDir, final Minecraft minecraft) {
      MutableObject<Path> tempDataPackDir = new MutableObject();

      try (Stream<Path> dataPackContents = Files.walk(sourcePackDir)) {
         dataPackContents.filter(p -> !p.equals(sourcePackDir)).forEach(source -> {
            Path targetDir = (Path)tempDataPackDir.get();
            if (targetDir == null) {
               try {
                  targetDir = Files.createTempDirectory("mcworld-");
               } catch (IOException e) {
                  LOGGER.warn("Failed to create temporary dir");
                  throw new UncheckedIOException(ex);
               }

               tempDataPackDir.setValue(targetDir);
            }

            copyBetweenDirs(sourcePackDir, targetDir, source);
         });
      } catch (IOException | UncheckedIOException e) {
         LOGGER.warn("Failed to copy datapacks from world {}", sourcePackDir, e);
         SystemToast.onPackCopyFailure(minecraft, sourcePackDir.toString());
         return null;
      }

      return (Path)tempDataPackDir.get();
   }

   private @Nullable Pair<Path, PackRepository> getDataPackSelectionSettings(final WorldDataConfiguration dataConfiguration) {
      Path dataPackDir = this.getOrCreateTempDataPackDir();
      if (dataPackDir != null) {
         if (this.tempDataPackRepository == null) {
            this.tempDataPackRepository = ServerPacksSource.createPackRepository(dataPackDir, this.packValidator);
            this.tempDataPackRepository.reload();
         }

         this.tempDataPackRepository.setSelected(dataConfiguration.dataPacks().getEnabled());
         return Pair.of(dataPackDir, this.tempDataPackRepository);
      } else {
         return null;
      }
   }

   private class GameTab extends GridLayoutTab {
      private static final Component TITLE = Component.translatable("createWorld.tab.game.title");
      private static final Component ALLOW_COMMANDS = Component.translatable("selectWorld.allowCommands");
      private final EditBox nameEdit;

      private GameTab() {
         super(TITLE);
         GridLayout.RowHelper helper = this.layout.rowSpacing(8).createRowHelper(1);
         LayoutSettings buttonLayoutSettings = helper.newCellSettings();
         this.nameEdit = new EditBox(CreateWorldScreen.this.font, 208, 20, Component.translatable("selectWorld.enterName"));
         this.nameEdit.setValue(CreateWorldScreen.this.uiState.getName());
         this.nameEdit.setResponder(CreateWorldScreen.this.uiState::setName);
         CreateWorldScreen.this.uiState
            .addListener(
               uiState -> this.nameEdit
                  .setTooltip(
                     Tooltip.create(
                        Component.translatable("selectWorld.targetFolder", Component.literal(uiState.getTargetFolder()).withStyle(ChatFormatting.ITALIC))
                     )
                  )
            );
         CreateWorldScreen.this.setInitialFocus(this.nameEdit);
         helper.addChild(
            CommonLayouts.labeledElement(CreateWorldScreen.this.font, this.nameEdit, CreateWorldScreen.NAME_LABEL),
            helper.newCellSettings().alignHorizontallyCenter()
         );
         CycleButton<WorldCreationUiState.SelectedGameMode> gameModeButton = helper.addChild(
            CycleButton.<WorldCreationUiState.SelectedGameMode>builder(
                  selectedGameMode -> selectedGameMode.displayName, CreateWorldScreen.this.uiState.getGameMode()
               )
               .withValues(
                  WorldCreationUiState.SelectedGameMode.SURVIVAL,
                  WorldCreationUiState.SelectedGameMode.HARDCORE,
                  WorldCreationUiState.SelectedGameMode.CREATIVE
               )
               .create(0, 0, 210, 20, CreateWorldScreen.GAME_MODEL_LABEL, (button, gameMode) -> CreateWorldScreen.this.uiState.setGameMode(gameMode)),
            buttonLayoutSettings
         );
         CreateWorldScreen.this.uiState.addListener(data -> {
            gameModeButton.setValue(data.getGameMode());
            gameModeButton.active = !data.isDebug();
            gameModeButton.setTooltip(Tooltip.create(data.getGameMode().getInfo()));
         });
         CycleButton<Difficulty> difficultyButton = helper.addChild(
            CycleButton.builder(Difficulty::getDisplayName, CreateWorldScreen.this.uiState.getDifficulty())
               .withValues(Difficulty.values())
               .create(0, 0, 210, 20, Component.translatable("options.difficulty"), (button, value) -> CreateWorldScreen.this.uiState.setDifficulty(value)),
            buttonLayoutSettings
         );
         CreateWorldScreen.this.uiState.addListener(d -> {
            difficultyButton.setValue(CreateWorldScreen.this.uiState.getDifficulty());
            difficultyButton.active = !CreateWorldScreen.this.uiState.isHardcore();
            difficultyButton.setTooltip(Tooltip.create(CreateWorldScreen.this.uiState.getDifficulty().getInfo()));
         });
         CycleButton<Boolean> allowCommandsButton = helper.addChild(
            CycleButton.onOffBuilder(CreateWorldScreen.this.uiState.isAllowCommands())
               .withTooltip(state -> Tooltip.create(CreateWorldScreen.ALLOW_COMMANDS_INFO))
               .create(0, 0, 210, 20, ALLOW_COMMANDS, (b, state) -> CreateWorldScreen.this.uiState.setAllowCommands(state))
         );
         CreateWorldScreen.this.uiState.addListener(d -> {
            allowCommandsButton.setValue(CreateWorldScreen.this.uiState.isAllowCommands());
            allowCommandsButton.active = !CreateWorldScreen.this.uiState.isDebug() && !CreateWorldScreen.this.uiState.isHardcore();
         });
         if (!SharedConstants.getCurrentVersion().stable()) {
            helper.addChild(
               Button.builder(
                     CreateWorldScreen.EXPERIMENTS_LABEL,
                     button -> CreateWorldScreen.this.openExperimentsScreen(CreateWorldScreen.this.uiState.getSettings().dataConfiguration())
                  )
                  .width(210)
                  .build()
            );
         }
      }
   }

   private class MoreTab extends GridLayoutTab {
      private static final Component TITLE = Component.translatable("createWorld.tab.more.title");
      private static final Component GAME_RULES_LABEL = Component.translatable("selectWorld.gameRules");
      private static final Component DATA_PACKS_LABEL = Component.translatable("selectWorld.dataPacks");

      private MoreTab() {
         super(TITLE);
         GridLayout.RowHelper helper = this.layout.rowSpacing(8).createRowHelper(1);
         helper.addChild(Button.builder(GAME_RULES_LABEL, b -> this.openGameRulesScreen()).width(210).build());
         helper.addChild(
            Button.builder(
                  CreateWorldScreen.EXPERIMENTS_LABEL,
                  b -> CreateWorldScreen.this.openExperimentsScreen(CreateWorldScreen.this.uiState.getSettings().dataConfiguration())
               )
               .width(210)
               .build()
         );
         helper.addChild(
            Button.builder(
                  DATA_PACKS_LABEL, b -> CreateWorldScreen.this.openDataPackSelectionScreen(CreateWorldScreen.this.uiState.getSettings().dataConfiguration())
               )
               .width(210)
               .build()
         );
      }

      private void openGameRulesScreen() {
         CreateWorldScreen.this.minecraft
            .gui
            .setScreen(
               new WorldCreationGameRulesScreen(
                  CreateWorldScreen.this.uiState.getGameRules().copy(CreateWorldScreen.this.uiState.getSettings().dataConfiguration().enabledFeatures()),
                  gameRules -> {
                     CreateWorldScreen.this.minecraft.gui.setScreen(CreateWorldScreen.this);
                     gameRules.ifPresent(CreateWorldScreen.this.uiState::setGameRules);
                  }
               )
            );
      }
   }

   private class WorldTab extends GridLayoutTab {
      private static final Component TITLE = Component.translatable("createWorld.tab.world.title");
      private static final Component AMPLIFIED_HELP_TEXT = Component.translatable("generator.minecraft.amplified.info");
      private static final Component GENERATE_STRUCTURES = Component.translatable("selectWorld.mapFeatures");
      private static final Component GENERATE_STRUCTURES_INFO = Component.translatable("selectWorld.mapFeatures.info");
      private static final Component BONUS_CHEST = Component.translatable("selectWorld.bonusItems");
      private static final Component SEED_LABEL = Component.translatable("selectWorld.enterSeed");
      private static final Component SEED_EMPTY_HINT = Component.translatable("selectWorld.seedInfo");
      private static final int WORLD_TAB_WIDTH = 310;
      private final EditBox seedEdit;
      private final Button customizeTypeButton;

      private WorldTab() {
         super(TITLE);
         GridLayout.RowHelper helper = this.layout.columnSpacing(10).rowSpacing(8).createRowHelper(2);
         CycleButton<WorldCreationUiState.WorldTypeEntry> typeButton = helper.addChild(
            CycleButton.builder(WorldCreationUiState.WorldTypeEntry::describePreset, CreateWorldScreen.this.uiState.getWorldType())
               .withValues(this.createWorldTypeValueSupplier())
               .create(
                  0, 0, 150, 20, Component.translatable("selectWorld.mapType"), (button, newPreset) -> CreateWorldScreen.this.uiState.setWorldType(newPreset)
               )
         );
         typeButton.setValue(CreateWorldScreen.this.uiState.getWorldType());
         CreateWorldScreen.this.uiState.addListener(data -> {
            WorldCreationUiState.WorldTypeEntry worldType = data.getWorldType();
            typeButton.setValue(worldType);
            if (worldType.isAmplified()) {
               typeButton.setTooltip(Tooltip.create(AMPLIFIED_HELP_TEXT));
            } else {
               typeButton.setTooltip(null);
            }

            typeButton.active = CreateWorldScreen.this.uiState.getWorldType().preset() != null;
         });
         this.customizeTypeButton = helper.addChild(Button.builder(Component.translatable("selectWorld.customizeType"), b -> this.openPresetEditor()).build());
         CreateWorldScreen.this.uiState.addListener(data -> this.customizeTypeButton.active = !data.isDebug() && data.getPresetEditor() != null);
         this.seedEdit = new EditBox(CreateWorldScreen.this.font, 308, 20, Component.translatable("selectWorld.enterSeed")) {
            @Override
            protected MutableComponent createNarrationMessage() {
               return super.createNarrationMessage().append(CommonComponents.NARRATION_SEPARATOR).append(CreateWorldScreen.WorldTab.SEED_EMPTY_HINT);
            }
         };
         this.seedEdit.setHint(SEED_EMPTY_HINT);
         this.seedEdit.setValue(CreateWorldScreen.this.uiState.getSeed());
         this.seedEdit.setResponder(value -> CreateWorldScreen.this.uiState.setSeed(this.seedEdit.getValue()));
         helper.addChild(CommonLayouts.labeledElement(CreateWorldScreen.this.font, this.seedEdit, SEED_LABEL), 2);
         SwitchGrid.Builder switchGridBuilder = SwitchGrid.builder(310);
         switchGridBuilder.addSwitch(
               GENERATE_STRUCTURES, CreateWorldScreen.this.uiState::isGenerateStructures, CreateWorldScreen.this.uiState::setGenerateStructures
            )
            .withIsActiveCondition(() -> !CreateWorldScreen.this.uiState.isDebug())
            .withInfo(GENERATE_STRUCTURES_INFO);
         switchGridBuilder.addSwitch(BONUS_CHEST, CreateWorldScreen.this.uiState::isBonusChest, CreateWorldScreen.this.uiState::setBonusChest)
            .withIsActiveCondition(() -> !CreateWorldScreen.this.uiState.isHardcore() && !CreateWorldScreen.this.uiState.isDebug());
         SwitchGrid switchGrid = switchGridBuilder.build();
         helper.addChild(switchGrid.layout(), 2);
         CreateWorldScreen.this.uiState.addListener(d -> switchGrid.refreshStates());
      }

      private void openPresetEditor() {
         PresetEditor editor = CreateWorldScreen.this.uiState.getPresetEditor();
         if (editor != null) {
            CreateWorldScreen.this.minecraft.gui.setScreen(editor.createEditScreen(CreateWorldScreen.this, CreateWorldScreen.this.uiState.getSettings()));
         }
      }

      private CycleButton.ValueListSupplier<WorldCreationUiState.WorldTypeEntry> createWorldTypeValueSupplier() {
         return new CycleButton.ValueListSupplier<WorldCreationUiState.WorldTypeEntry>() {
            @Override
            public List<WorldCreationUiState.WorldTypeEntry> getSelectedList() {
               return CycleButton.DEFAULT_ALT_LIST_SELECTOR.getAsBoolean()
                  ? CreateWorldScreen.this.uiState.getAltPresetList()
                  : CreateWorldScreen.this.uiState.getNormalPresetList();
            }

            @Override
            public List<WorldCreationUiState.WorldTypeEntry> getDefaultList() {
               return CreateWorldScreen.this.uiState.getNormalPresetList();
            }
         };
      }
   }
}
