package net.minecraft.client.gui.screens.worldselection;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.DatapackLoadFailureScreen;
import net.minecraft.client.gui.screens.FileFixerAbortedScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.NoticeWithLinkScreen;
import net.minecraft.client.gui.screens.RecoverWorldDataScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.MemoryReserve;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.filefix.AbortedFileFixException;
import net.minecraft.util.filefix.CanceledFileFixException;
import net.minecraft.util.filefix.FailedCleanupFileFixException;
import net.minecraft.util.filefix.FileFixException;
import net.minecraft.util.filefix.virtualfilesystem.exception.CowFSSymlinkException;
import net.minecraft.util.worldupdate.UpgradeProgress;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.LevelDataAndDimensions;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.validation.ContentValidationException;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class WorldOpenFlows {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final UUID WORLD_PACK_ID = UUID.fromString("640a6a92-b6cb-48a0-b391-831586500359");
    private final Minecraft minecraft;
    private final LevelStorageSource levelSource;

    public WorldOpenFlows(final Minecraft minecraft, final LevelStorageSource levelSource) {
        this.minecraft = minecraft;
        this.levelSource = levelSource;
    }

    public void createFreshLevel(
        final String levelId,
        final LevelSettings levelSettings,
        final WorldOptions options,
        final Function<HolderLookup.Provider, WorldDimensions> dimensionsProvider,
        final Screen parentScreen
    ) {
        this.minecraft.setScreenAndShow(new GenericMessageScreen(Component.translatable("selectWorld.data_read")));
        LevelStorageSource.LevelStorageAccess levelSourceAccess = this.createWorldAccess(levelId);
        if (levelSourceAccess != null) {
            PackRepository packRepository = ServerPacksSource.createPackRepository(levelSourceAccess);
            WorldDataConfiguration dataConfiguration = levelSettings.dataConfiguration();

            try {
                WorldLoader.PackConfig packConfig = new WorldLoader.PackConfig(packRepository, dataConfiguration, false, false);
                WorldStem worldStem = this.loadWorldDataBlocking(
                    packConfig,
                    context -> {
                        WorldDimensions dimensions = dimensionsProvider.apply(context.datapackWorldgen());
                        WorldDimensions.Complete completeDimensions = dimensions.bake(context.datapackDimensions().lookupOrThrow(Registries.LEVEL_STEM));
                        return new WorldLoader.DataLoadOutput<>(
                            new LevelDataAndDimensions.WorldDataAndGenSettings(
                                new PrimaryLevelData(levelSettings, completeDimensions.specialWorldProperty(), completeDimensions.lifecycle()),
                                new WorldGenSettings(options, dimensions)
                            ),
                            completeDimensions.dimensionsRegistryAccess()
                        );
                    },
                    WorldStem::new
                );
                this.minecraft.doWorldLoad(levelSourceAccess, packRepository, worldStem, Optional.empty(), true);
            } catch (Exception e) {
                LOGGER.warn("Failed to load datapacks, can't proceed with server load", e);
                levelSourceAccess.safeClose();
                this.minecraft.gui.setScreen(parentScreen);
            }
        }
    }

    private LevelStorageSource.@Nullable LevelStorageAccess createWorldAccess(final String levelId) {
        try {
            return this.levelSource.validateAndCreateAccess(levelId);
        } catch (IOException e) {
            LOGGER.warn("Failed to read level {} data", levelId, e);
            SystemToast.onWorldAccessFailure(this.minecraft, levelId);
            this.minecraft.gui.setScreen(null);
            return null;
        } catch (ContentValidationException e) {
            LOGGER.warn("{}", e.getMessage());
            this.minecraft.gui.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> this.minecraft.gui.setScreen(null)));
            return null;
        }
    }

    public void createLevelFromExistingSettings(
        final LevelStorageSource.LevelStorageAccess levelSourceAccess,
        final ReloadableServerResources serverResources,
        final LayeredRegistryAccess<RegistryLayer> registryAccess,
        final LevelDataAndDimensions.WorldDataAndGenSettings worldDataAndGenSettings,
        final Optional<GameRules> gameRules
    ) {
        PackRepository packRepository = ServerPacksSource.createPackRepository(levelSourceAccess);
        CloseableResourceManager resourceManager = new WorldLoader.PackConfig(
                packRepository, worldDataAndGenSettings.data().getDataConfiguration(), false, false
            )
            .createResourceManager()
            .getSecond();
        this.minecraft
            .doWorldLoad(
                levelSourceAccess, packRepository, new WorldStem(resourceManager, serverResources, registryAccess, worldDataAndGenSettings), gameRules, true
            );
    }

    public WorldStem loadWorldStem(
        final LevelStorageSource.LevelStorageAccess worldAccess, final Dynamic<?> levelDataTag, final boolean safeMode, final PackRepository packRepository
    ) throws Exception {
        WorldLoader.PackConfig packConfig = LevelStorageSource.getPackConfig(levelDataTag, packRepository, safeMode);
        return this.loadWorldDataBlocking(
            packConfig,
            context -> {
                Registry<LevelStem> datapackDimensions = context.datapackDimensions().lookupOrThrow(Registries.LEVEL_STEM);
                LevelDataAndDimensions data = LevelStorageSource.getLevelDataAndDimensions(
                    worldAccess, levelDataTag, context.dataConfiguration(), datapackDimensions, context.datapackWorldgen()
                );
                return new WorldLoader.DataLoadOutput<>(data.worldDataAndGenSettings(), data.dimensions().dimensionsRegistryAccess());
            },
            WorldStem::new
        );
    }

    public Pair<LevelSettings, WorldCreationContext> recreateWorldData(final LevelStorageSource.LevelStorageAccess levelSourceAccess) throws Exception {
        PackRepository packRepository = ServerPacksSource.createPackRepository(levelSourceAccess);
        Dynamic<?> unfixedDataTag = levelSourceAccess.getUnfixedDataTag(false);
        int dataVersion = NbtUtils.getDataVersion(unfixedDataTag);
        if (DataFixers.getFileFixer().requiresFileFixing(dataVersion)) {
            throw new IllegalStateException("Can't recreate world before file fixing; shouldn't be able to get here");
        }

        Dynamic<?> levelDataTag = DataFixTypes.LEVEL.updateToCurrentVersion(DataFixers.getDataFixer(), unfixedDataTag, dataVersion);
        WorldLoader.PackConfig packConfig = LevelStorageSource.getPackConfig(levelDataTag, packRepository, false);

        @OnlyIn(Dist.CLIENT)
        record Data(LevelSettings levelSettings, WorldOptions options, Registry<LevelStem> existingDimensions) {
        }

        return this.loadWorldDataBlocking(
            packConfig,
            context -> {
                Registry<LevelStem> noDatapackDimensions = new MappedRegistry<>(Registries.LEVEL_STEM, Lifecycle.stable()).freeze();
                LevelDataAndDimensions existingData = LevelStorageSource.getLevelDataAndDimensions(
                    levelSourceAccess, levelDataTag, context.dataConfiguration(), noDatapackDimensions, context.datapackWorldgen()
                );
                return new WorldLoader.DataLoadOutput<>(
                    new Data(
                        existingData.worldDataAndGenSettings().data().getLevelSettings(),
                        existingData.worldDataAndGenSettings().genSettings().options(),
                        existingData.dimensions().dimensions()
                    ),
                    context.datapackDimensions()
                );
            },
            (resources, managers, registries, loadedData) -> {
                resources.close();
                DataResult<GameRuleMap> existingGameRules = LevelStorageSource.readExistingSavedData(
                    levelSourceAccess, registries.compositeAccess(), GameRuleMap.TYPE
                );
                existingGameRules.ifError(e -> LOGGER.error("Failed to parse existing game rules: {}", e.message()));
                InitialWorldCreationOptions initialWorldCreationOptions = new InitialWorldCreationOptions(
                    WorldCreationUiState.SelectedGameMode.SURVIVAL, existingGameRules.result().orElse(GameRuleMap.of()), null
                );
                return Pair.of(
                    loadedData.levelSettings,
                    new WorldCreationContext(
                        loadedData.options,
                        new WorldDimensions(loadedData.existingDimensions),
                        registries,
                        managers,
                        loadedData.levelSettings.dataConfiguration(),
                        initialWorldCreationOptions
                    )
                );
            }
        );
    }

    private <D, R> R loadWorldDataBlocking(
        final WorldLoader.PackConfig packConfig,
        final WorldLoader.WorldDataSupplier<D> worldDataGetter,
        final WorldLoader.ResultFactory<D, R> worldDataSupplier
    ) throws Exception {
        long start = Util.getMillis();
        WorldLoader.InitConfig config = new WorldLoader.InitConfig(packConfig, Commands.CommandSelection.INTEGRATED, LevelBasedPermissionSet.GAMEMASTER);
        CompletableFuture<R> resourceLoad = WorldLoader.load(config, worldDataGetter, worldDataSupplier, Util.backgroundExecutor(), this.minecraft);
        this.minecraft.managedBlock(resourceLoad::isDone);
        long end = Util.getMillis();
        LOGGER.debug("World resource load blocked for {} ms", end - start);
        return resourceLoad.get();
    }

    private void askForBackup(
        final LevelStorageSource.LevelStorageAccess levelAccess, final boolean oldCustomized, final Runnable proceedCallback, final Runnable cancelCallback
    ) {
        Component backupQuestion;
        Component backupWarning;
        if (oldCustomized) {
            backupQuestion = Component.translatable("selectWorld.backupQuestion.customized");
            backupWarning = Component.translatable("selectWorld.backupWarning.customized");
        } else {
            backupQuestion = Component.translatable("selectWorld.backupQuestion.experimental");
            backupWarning = Component.translatable("selectWorld.backupWarning.experimental");
        }

        this.minecraft
            .gui
            .setScreen(
                new BackupConfirmScreen(
                    cancelCallback,
                    (backup, eraseCache) -> EditWorldScreen.conditionallyMakeBackupAndShowToast(backup, levelAccess)
                        .thenAcceptAsync(var1x -> proceedCallback.run(), this.minecraft),
                    backupQuestion,
                    backupWarning,
                    false
                )
            );
    }

    public static void confirmWorldCreation(
        final Minecraft minecraft, final CreateWorldScreen parent, final Lifecycle lifecycle, final Runnable task, final boolean skipWarning
    ) {
        BooleanConsumer callback = confirmed -> {
            if (confirmed) {
                task.run();
            } else {
                minecraft.gui.setScreen(parent);
            }
        };
        if (skipWarning || lifecycle == Lifecycle.stable()) {
            task.run();
        } else if (lifecycle == Lifecycle.experimental()) {
            minecraft.gui
                .setScreen(
                    new ConfirmScreen(
                        callback,
                        Component.translatable("selectWorld.warning.experimental.title"),
                        Component.translatable("selectWorld.warning.experimental.question")
                    )
                );
        } else {
            minecraft.gui
                .setScreen(
                    new ConfirmScreen(
                        callback,
                        Component.translatable("selectWorld.warning.deprecated.title"),
                        Component.translatable("selectWorld.warning.deprecated.question")
                    )
                );
        }
    }

    public void openWorld(final String levelId, final Runnable onCancel) {
        this.minecraft.setScreenAndShow(new GenericMessageScreen(Component.translatable("selectWorld.data_read")));
        LevelStorageSource.LevelStorageAccess worldAccess = this.createWorldAccess(levelId);
        if (worldAccess != null) {
            this.openWorldLoadLevelData(worldAccess, onCancel);
        }
    }

    private void openWorldLoadLevelData(final LevelStorageSource.LevelStorageAccess worldAccess, final Runnable onCancel) {
        this.minecraft.setScreenAndShow(new GenericMessageScreen(Component.translatable("selectWorld.data_read")));

        Dynamic<?> levelDataTag;
        LevelSummary summary;
        try {
            levelDataTag = worldAccess.getUnfixedDataTag(false);
            summary = worldAccess.fixAndGetSummaryFromTag(levelDataTag);
        } catch (IOException | NbtException | ReportedNbtException e) {
            this.minecraft.gui.setScreen(new RecoverWorldDataScreen(this.minecraft, success -> {
                if (success) {
                    this.openWorldLoadLevelData(worldAccess, onCancel);
                } else {
                    worldAccess.safeClose();
                    onCancel.run();
                }
            }, worldAccess));
            return;
        } catch (OutOfMemoryError e) {
            MemoryReserve.release();
            String detailedMessage = "Ran out of memory trying to read level data of world folder \"" + worldAccess.getLevelId() + "\"";
            LOGGER.error(LogUtils.FATAL_MARKER, detailedMessage);
            OutOfMemoryError detailedException = new OutOfMemoryError("Ran out of memory reading level data");
            detailedException.initCause(e);
            CrashReport crashReport = CrashReport.forThrowable(detailedException, detailedMessage);
            CrashReportCategory worldDetails = crashReport.addCategory("World details");
            worldDetails.setDetail("World folder", worldAccess.getLevelId());
            throw new ReportedException(crashReport);
        }

        this.openWorldCheckVersionCompatibility(worldAccess, summary, levelDataTag, onCancel);
    }

    private void openWorldCheckVersionCompatibility(
        final LevelStorageSource.LevelStorageAccess worldAccess, final LevelSummary summary, final Dynamic<?> levelDataTag, final Runnable onCancel
    ) {
        if (!summary.isCompatible()) {
            worldAccess.safeClose();
            this.minecraft
                .gui
                .setScreen(
                    new AlertScreen(
                        onCancel,
                        Component.translatable("selectWorld.incompatible.title").withColor(-65536),
                        Component.translatable("selectWorld.incompatible.description", summary.getWorldVersionName())
                    )
                );
        } else {
            LevelSummary.BackupStatus backupStatus = summary.backupStatus();
            if (backupStatus.shouldBackup()) {
                String questionKey = "selectWorld.backupQuestion." + backupStatus.getTranslationKey();
                String warningKey = "selectWorld.backupWarning." + backupStatus.getTranslationKey();
                MutableComponent backupQuestion = Component.translatable(questionKey);
                if (backupStatus.isSevere()) {
                    backupQuestion.withColor(-2142128);
                }

                Component backupWarning = Component.translatable(warningKey, summary.getWorldVersionName(), SharedConstants.getCurrentVersion().name());
                this.minecraft.gui.setScreen(new BackupConfirmScreen(() -> {
                    worldAccess.safeClose();
                    onCancel.run();
                }, (backup, eraseCache) -> this.createBackupAndOpenWorld(worldAccess, levelDataTag, onCancel, backup), backupQuestion, backupWarning, false));
            } else {
                this.upgradeAndOpenWorld(worldAccess, levelDataTag, onCancel);
            }
        }
    }

    private void createBackupAndOpenWorld(
        final LevelStorageSource.LevelStorageAccess levelAccess, final Dynamic<?> levelDataTag, final Runnable onCancel, final boolean backup
    ) {
        EditWorldScreen.conditionallyMakeBackupAndShowToast(backup, levelAccess)
            .thenAcceptAsync(var4 -> this.upgradeAndOpenWorld(levelAccess, levelDataTag, onCancel), this.minecraft);
    }

    private void upgradeAndOpenWorld(final LevelStorageSource.LevelStorageAccess worldAccess, final Dynamic<?> levelDataTag, final Runnable onCancel) {
        Runnable cleanup = () -> {
            worldAccess.safeClose();
            onCancel.run();
        };
        int dataVersion = NbtUtils.getDataVersion(levelDataTag);
        boolean requiresFileFixing = DataFixers.getFileFixer().requiresFileFixing(dataVersion);
        UpgradeProgress upgradeProgress = new UpgradeProgress();
        if (requiresFileFixing) {
            FileFixerProgressScreen progressScreen = new FileFixerProgressScreen(upgradeProgress);
            this.minecraft.setScreenAndShow(progressScreen);
        }

        Util.backgroundExecutor().execute(() -> {
            Dynamic<?> levelDataTagFixed = this.tryFileFixAndReportErrors(worldAccess, levelDataTag, upgradeProgress, cleanup);
            if (levelDataTagFixed != null) {
                this.minecraft.execute(() -> {
                    if (requiresFileFixing) {
                        ConfirmScreen loadConfirmScreen = new ConfirmScreen(result -> {
                            if (result) {
                                this.openWorldLoadLevelStem(worldAccess, levelDataTagFixed, false, onCancel);
                            } else {
                                cleanup.run();
                            }
                        }, Component.translatable("upgradeWorld.done"), Component.translatable("upgradeWorld.joinNow"));
                        this.minecraft.setScreenAndShow(loadConfirmScreen);
                    } else {
                        this.openWorldLoadLevelStem(worldAccess, levelDataTagFixed, false, onCancel);
                    }
                });
            }
        });
    }

    private @Nullable Dynamic<?> tryFileFixAndReportErrors(
        final LevelStorageSource.LevelStorageAccess worldAccess, final Dynamic<?> levelDataTag, final UpgradeProgress upgradeProgress, final Runnable cleanup
    ) {
        try {
            return DataFixers.getFileFixer().fix(worldAccess, levelDataTag, upgradeProgress);
        } catch (CanceledFileFixException e) {
            this.minecraft
                .execute(
                    () -> this.minecraft
                        .setScreenAndShow(
                            new AlertScreen(
                                cleanup,
                                Component.translatable("upgradeWorld.canceled.title"),
                                Component.translatable("upgradeWorld.canceled.message"),
                                CommonComponents.GUI_OK,
                                true
                            )
                        )
                );
            return null;
        } catch (AbortedFileFixException e) {
            this.minecraft
                .execute(
                    () -> {
                        if (e.getCause() instanceof CowFSSymlinkException) {
                            this.minecraft
                                .setScreenAndShow(
                                    new AlertScreen(
                                        cleanup, Component.translatable("upgradeWorld.symlink.title"), Component.translatable("upgradeWorld.symlink.message")
                                    )
                                );
                        } else {
                            this.minecraft.setScreenAndShow(new FileFixerAbortedScreen(cleanup, Component.translatable("upgradeWorld.aborted.message")));
                        }
                    }
                );
            return null;
        } catch (FailedCleanupFileFixException e) {
            this.minecraft
                .execute(
                    () -> this.minecraft
                        .setScreenAndShow(
                            new AlertScreen(
                                cleanup,
                                Component.translatable("upgradeWorld.failed_cleanup.title"),
                                Component.translatable("upgradeWorld.failed_cleanup.message", Component.literal(e.newWorldFolderName()).withColor(-8355712))
                            )
                        )
                );
            return null;
        } catch (FileFixException e) {
            this.minecraft.delayCrash(e.makeReportedException().getReport());
            return null;
        } catch (Exception e) {
            LOGGER.error("Failed to upgrade the file structure of the world.", e);
            CrashReport report = CrashReport.forThrowable(e, "Failed to update file structure");
            this.minecraft.delayCrash(report);
            return null;
        }
    }

    private void openWorldLoadLevelStem(
        final LevelStorageSource.LevelStorageAccess worldAccess, final Dynamic<?> levelDataTag, final boolean safeMode, final Runnable onCancel
    ) {
        this.minecraft.setScreenAndShow(new GenericMessageScreen(Component.translatable("selectWorld.resource_load")));
        PackRepository packRepository = ServerPacksSource.createPackRepository(worldAccess);

        WorldStem worldStem;
        try {
            worldStem = this.loadWorldStem(worldAccess, levelDataTag, safeMode, packRepository);

            for (LevelStem levelStem : worldStem.registries().compositeAccess().lookupOrThrow(Registries.LEVEL_STEM)) {
                levelStem.generator().validate();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load level data or datapacks, can't proceed with server load", e);
            if (!safeMode) {
                this.minecraft.gui.setScreen(new DatapackLoadFailureScreen(() -> {
                    worldAccess.safeClose();
                    onCancel.run();
                }, () -> this.openWorldLoadLevelStem(worldAccess, levelDataTag, true, onCancel)));
            } else {
                worldAccess.safeClose();
                this.minecraft
                    .gui
                    .setScreen(
                        new AlertScreen(
                            onCancel,
                            Component.translatable("datapackFailure.safeMode.failed.title"),
                            Component.translatable("datapackFailure.safeMode.failed.description"),
                            CommonComponents.GUI_BACK,
                            true
                        )
                    );
            }

            return;
        }

        this.openWorldCheckWorldStemCompatibility(worldAccess, worldStem, packRepository, onCancel);
    }

    private void openWorldCheckWorldStemCompatibility(
        final LevelStorageSource.LevelStorageAccess worldAccess, final WorldStem worldStem, final PackRepository packRepository, final Runnable onCancel
    ) {
        LevelDataAndDimensions.WorldDataAndGenSettings worldDataAndGenSettings = worldStem.worldDataAndGenSettings();
        WorldData data = worldDataAndGenSettings.data();
        boolean oldCustomized = worldDataAndGenSettings.genSettings().options().isOldCustomizedWorld();
        boolean unstable = data.worldGenSettingsLifecycle() != Lifecycle.stable();
        if (!oldCustomized && !unstable) {
            this.openWorldLoadBundledResourcePack(worldAccess, worldStem, packRepository, onCancel);
        } else {
            this.askForBackup(
                worldAccess, oldCustomized, () -> this.openWorldLoadBundledResourcePack(worldAccess, worldStem, packRepository, onCancel), () -> {
                    worldStem.close();
                    worldAccess.safeClose();
                    onCancel.run();
                }
            );
        }
    }

    private void openWorldLoadBundledResourcePack(
        final LevelStorageSource.LevelStorageAccess worldAccess, final WorldStem worldStem, final PackRepository packRepository, final Runnable onCancel
    ) {
        DownloadedPackSource packSource = this.minecraft.getDownloadedPackSource();
        this.loadBundledResourcePack(packSource, worldAccess).thenApply(unused -> true).exceptionallyComposeAsync(t -> {
            LOGGER.warn("Failed to load pack: ", t);
            return this.promptBundledPackLoadFailure();
        }, this.minecraft).thenAcceptAsync(result -> {
            if (result) {
                this.openWorldCheckDiskSpace(worldAccess, worldStem, packSource, packRepository, onCancel);
            } else {
                packSource.popAll();
                worldStem.close();
                worldAccess.safeClose();
                onCancel.run();
            }
        }, this.minecraft).exceptionally(e -> {
            this.minecraft.delayCrash(CrashReport.forThrowable(e, "Load world"));
            return null;
        });
    }

    private void openWorldCheckDiskSpace(
        final LevelStorageSource.LevelStorageAccess worldAccess,
        final WorldStem worldStem,
        final DownloadedPackSource packSource,
        final PackRepository packRepository,
        final Runnable onCancel
    ) {
        if (worldAccess.checkForLowDiskSpace()) {
            Screen screen = new ConfirmScreen(
                skip -> {
                    if (skip) {
                        this.openWorldDoLoad(worldAccess, worldStem, packRepository);
                    } else {
                        packSource.popAll();
                        worldStem.close();
                        worldAccess.safeClose();
                        onCancel.run();
                    }
                },
                Component.translatable("selectWorld.warning.lowDiskSpace.title").withStyle(ChatFormatting.RED),
                Component.translatable("selectWorld.warning.lowDiskSpace.description"),
                CommonComponents.GUI_CONTINUE,
                CommonComponents.GUI_BACK
            );
            this.minecraft.gui.setScreen(screen);
        } else {
            this.openWorldDoLoad(worldAccess, worldStem, packRepository);
        }
    }

    private void openWorldDoLoad(final LevelStorageSource.LevelStorageAccess worldAccess, final WorldStem worldStem, final PackRepository packRepository) {
        this.minecraft.doWorldLoad(worldAccess, packRepository, worldStem, Optional.empty(), false);
    }

    private CompletableFuture<Void> loadBundledResourcePack(
        final DownloadedPackSource packSource, final LevelStorageSource.LevelStorageAccess levelSourceAccess
    ) {
        Path mapResourceFile = levelSourceAccess.getLevelPath(LevelResource.MAP_RESOURCE_FILE);
        if (Files.exists(mapResourceFile) && !Files.isDirectory(mapResourceFile)) {
            packSource.configureForLocalWorld();
            CompletableFuture<Void> result = packSource.waitForPackFeedback(WORLD_PACK_ID);
            packSource.pushLocalPack(WORLD_PACK_ID, mapResourceFile);
            return result;
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    private CompletableFuture<Boolean> promptBundledPackLoadFailure() {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        Screen screen = new ConfirmScreen(
            result::complete,
            Component.translatable("multiplayer.texturePrompt.failure.line1"),
            Component.translatable("multiplayer.texturePrompt.failure.line2"),
            CommonComponents.GUI_PROCEED,
            CommonComponents.GUI_CANCEL
        );
        this.minecraft.gui.setScreen(screen);
        return result;
    }
}