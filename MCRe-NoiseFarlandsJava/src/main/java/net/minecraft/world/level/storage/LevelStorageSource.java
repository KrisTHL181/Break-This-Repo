package net.minecraft.world.level.storage;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.NbtFormatException;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.nbt.visitors.SkipFields;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.util.DirectoryLock;
import net.minecraft.util.FileUtil;
import net.minecraft.util.MemoryReserve;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.validation.ContentValidationException;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import net.minecraft.world.level.validation.PathAllowList;
import org.apache.commons.io.function.IORunnable;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class LevelStorageSource {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String TAG_DATA = "Data";
    private static final PathMatcher NO_SYMLINKS_ALLOWED = path -> false;
    public static final String ALLOWED_SYMLINKS_CONFIG_NAME = "allowed_symlinks.txt";
    private static final int DISK_SPACE_WARNING_THRESHOLD = 67108864;
    private static final Component LOAD_FOLDER_ACCESS_MESSAGE = Component.translatable("selectWorld.load_folder_access");
    private final Path baseDir;
    private final Path backupDir;
    private final DataFixer fixerUpper;
    private final DirectoryValidator worldDirValidator;

    public LevelStorageSource(final Path baseDir, final Path backupDir, final DirectoryValidator worldDirValidator, final DataFixer fixerUpper) {
        this.fixerUpper = fixerUpper;

        try {
            FileUtil.createDirectoriesSafe(baseDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        this.baseDir = baseDir;
        this.backupDir = backupDir;
        this.worldDirValidator = worldDirValidator;
    }

    public static DirectoryValidator parseValidator(final Path configPath) {
        if (Files.exists(configPath)) {
            try (BufferedReader reader = Files.newBufferedReader(configPath)) {
                return new DirectoryValidator(PathAllowList.readPlain(reader));
            } catch (Exception e) {
                LOGGER.error("Failed to parse {}, disallowing all symbolic links", "allowed_symlinks.txt", e);
            }
        }

        return new DirectoryValidator(NO_SYMLINKS_ALLOWED);
    }

    public static LevelStorageSource createDefault(final Path path) {
        DirectoryValidator validator = parseValidator(path.resolve("allowed_symlinks.txt"));
        return new LevelStorageSource(path, path.resolve("../backups"), validator, DataFixers.getDataFixer());
    }

    public static WorldDataConfiguration readDataConfig(final Dynamic<?> levelData) {
        return WorldDataConfiguration.CODEC.parse(levelData).resultOrPartial(LOGGER::error).orElse(WorldDataConfiguration.DEFAULT);
    }

    public static WorldLoader.PackConfig getPackConfig(final Dynamic<?> levelDataTag, final PackRepository packRepository, final boolean safeMode) {
        return new WorldLoader.PackConfig(packRepository, readDataConfig(levelDataTag), safeMode, false);
    }

    public static LevelDataAndDimensions getLevelDataAndDimensions(
        final LevelStorageSource.LevelStorageAccess worldAccess,
        final Dynamic<?> levelDataTag,
        final WorldDataConfiguration dataConfiguration,
        final Registry<LevelStem> datapackDimensions,
        final HolderLookup.Provider registryAccess
    ) {
        if (DataFixers.getFileFixer().requiresFileFixing(NbtUtils.getDataVersion(levelDataTag))) {
            throw new IllegalStateException("Cannot get level data without file fixing first");
        }

        Dynamic<?> dataTag = RegistryOps.injectRegistryContext(levelDataTag, registryAccess);
        WorldGenSettings worldGenSettings = readExistingSavedData(worldAccess, registryAccess, WorldGenSettings.TYPE)
            .mapOrElse(
                Function.identity(),
                error -> {
                    LOGGER.error(
                        "Unable to read or access the world gen settings file! Falling back to the default settings with a random world seed. {}",
                        error.message()
                    );
                    return new WorldGenSettings(WorldOptions.defaultWithRandomSeed(), new WorldDimensions(datapackDimensions));
                }
            );
        LevelSettings settings = LevelSettings.parse(dataTag, dataConfiguration);
        WorldDimensions.Complete dimensions = worldGenSettings.dimensions().bake(datapackDimensions);
        Lifecycle lifecycle = dimensions.lifecycle().add(registryAccess.allRegistriesLifecycle());
        PrimaryLevelData worldData = PrimaryLevelData.parse(dataTag, settings, dimensions.specialWorldProperty(), lifecycle);
        return LevelDataAndDimensions.create(worldData, worldGenSettings, dimensions);
    }

    public static <T extends SavedData> DataResult<T> readExistingSavedData(
        final LevelStorageSource.LevelStorageAccess access, final HolderLookup.Provider registryAccess, final SavedDataType<T> savedDataType
    ) {
        Path dataLocation = savedDataType.id().withSuffix(".dat").resolveAgainst(access.getLevelPath(LevelResource.DATA));

        CompoundTag fileContents;
        try {
            fileContents = NbtIo.readCompressed(dataLocation, NbtAccounter.unlimitedHeap());
        } catch (IOException e) {
            return DataResult.error(e::getMessage);
        }

        return savedDataType.codec().parse(RegistryOps.create(NbtOps.INSTANCE, registryAccess), fileContents.getCompoundOrEmpty("data"));
    }

    public static void writeGameRules(final WorldData worldData, final Path worldFolder, final GameRules gameRules) throws IOException {
        Codec<GameRules> codec = GameRules.codec(worldData.enabledFeatures());
        writeSavedData(worldFolder, NbtOps.INSTANCE, GameRuleMap.TYPE, codec, gameRules);
    }

    public static void writeWorldGenSettings(final RegistryAccess registryAccess, final Path worldFolder, final WorldGenSettings worldGenSettings) throws IOException {
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registryAccess);
        writeSavedData(worldFolder, ops, WorldGenSettings.TYPE, WorldGenSettings.CODEC, worldGenSettings);
    }

    private static <T> void writeSavedData(final Path worldFolder, final DynamicOps<Tag> ops, final SavedDataType<?> type, final Codec<T> codec, final T data) throws IOException {
        Tag encoded = codec.encodeStart(ops, data).getOrThrow();
        CompoundTag fullTag = new CompoundTag();
        fullTag.put("data", encoded);
        NbtUtils.addCurrentDataVersion(fullTag);
        Path path = type.id().withSuffix(".dat").resolveAgainst(worldFolder.resolve("data"));
        FileUtil.createDirectoriesSafe(path.getParent());
        NbtIo.writeCompressed(fullTag, path);
    }

    public String getName() {
        return "Anvil";
    }

    public LevelStorageSource.LevelCandidates findLevelCandidates() throws LevelStorageException {
        if (!Files.exists(this.baseDir)) {
            try {
                Files.createDirectory(this.baseDir);
            } catch (IOException e) {
                throw new LevelStorageException(LOAD_FOLDER_ACCESS_MESSAGE, e);
            }
        } else if (!Files.isDirectory(this.baseDir)) {
            throw new LevelStorageException(LOAD_FOLDER_ACCESS_MESSAGE);
        }

        try (Stream<Path> paths = Files.list(this.baseDir)) {
            List<LevelStorageSource.LevelDirectory> candidates = paths.filter(x$0 -> Files.isDirectory(x$0))
                .map(LevelStorageSource.LevelDirectory::new)
                .filter(directory -> Files.isRegularFile(directory.dataFile()) || Files.isRegularFile(directory.oldDataFile()))
                .toList();
            return new LevelStorageSource.LevelCandidates(candidates);
        } catch (IOException e) {
            throw new LevelStorageException(LOAD_FOLDER_ACCESS_MESSAGE);
        }
    }

    public CompletableFuture<List<LevelSummary>> loadLevelSummaries(final LevelStorageSource.LevelCandidates candidates) {
        List<CompletableFuture<LevelSummary>> futures = new ArrayList<>(candidates.levels.size());

        for (LevelStorageSource.LevelDirectory level : candidates.levels) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                boolean locked;
                try {
                    locked = DirectoryLock.isLocked(level.path());
                } catch (Exception e) {
                    LOGGER.warn("Failed to read {} lock", level.path(), e);
                    return null;
                }

                try {
                    return this.readLevelSummary(level, locked);
                } catch (OutOfMemoryError e) {
                    MemoryReserve.release();
                    String detailedMessage = "Ran out of memory trying to read summary of world folder \"" + level.directoryName() + "\"";
                    LOGGER.error(LogUtils.FATAL_MARKER, detailedMessage);
                    OutOfMemoryError detailedException = new OutOfMemoryError("Ran out of memory reading level data");
                    detailedException.initCause(e);
                    CrashReport crashReport = CrashReport.forThrowable(detailedException, detailedMessage);
                    CrashReportCategory worldDetails = crashReport.addCategory("World details");
                    worldDetails.setDetail("Folder Name", level.directoryName());

                    try {
                        long size = Files.size(level.dataFile());
                        worldDetails.setDetail("level.dat size", size);
                    } catch (IOException ex) {
                        worldDetails.setDetailError("level.dat size", ex);
                    }

                    throw new ReportedException(crashReport);
                }
            }, Util.backgroundExecutor().forName("loadLevelSummaries")));
        }

        return Util.sequenceFailFastAndCancel(futures).thenApply(levels -> levels.stream().filter(Objects::nonNull).sorted().toList());
    }

    private int getStorageVersion() {
        return 19133;
    }

    private static CompoundTag readLevelDataTagRaw(final Path dataFile) throws IOException {
        return NbtIo.readCompressed(dataFile, NbtAccounter.uncompressedQuota());
    }

    private LevelSummary readLevelSummary(final LevelStorageSource.LevelDirectory level, final boolean locked) {
        Path dataFile = level.dataFile();
        if (Files.exists(dataFile)) {
            try {
                if (Files.isSymbolicLink(dataFile)) {
                    List<ForbiddenSymlinkInfo> issues = this.worldDirValidator.validateSymlink(dataFile);
                    if (!issues.isEmpty()) {
                        LOGGER.warn("{}", ContentValidationException.getMessage(dataFile, issues));
                        return new LevelSummary.SymlinkLevelSummary(level.directoryName(), level.iconFile());
                    }
                }

                if (readLightweightData(dataFile) instanceof CompoundTag root) {
                    CompoundTag tag = root.getCompoundOrEmpty("Data");
                    int dataVersion = NbtUtils.getDataVersion(tag);
                    Dynamic<?> updated = DataFixTypes.LEVEL_SUMMARY.updateToCurrentVersion(this.fixerUpper, new Dynamic<>(NbtOps.INSTANCE, tag), dataVersion);
                    return this.makeLevelSummary(updated, level, locked, dataVersion);
                }

                LOGGER.warn("Invalid root tag in {}", dataFile);
            } catch (Exception e) {
                LOGGER.error("Exception reading {}", dataFile, e);
            }
        }

        return new LevelSummary.CorruptedLevelSummary(level.directoryName(), level.iconFile(), getFileModificationTime(level));
    }

    private static long getFileModificationTime(final LevelStorageSource.LevelDirectory level) {
        Instant timeStamp = getFileModificationTime(level.dataFile());
        if (timeStamp == null) {
            timeStamp = getFileModificationTime(level.oldDataFile());
        }

        return timeStamp == null ? -1L : timeStamp.toEpochMilli();
    }

    private static @Nullable Instant getFileModificationTime(final Path path) {
        try {
            return Files.getLastModifiedTime(path).toInstant();
        } catch (IOException var2) {
            return null;
        }
    }

    private LevelSummary makeLevelSummary(
        final Dynamic<?> dataTag, final LevelStorageSource.LevelDirectory levelDirectory, final boolean locked, final int dataVersion
    ) {
        LevelVersion levelVersion = LevelVersion.parse(dataTag);
        int levelDataVersion = levelVersion.levelDataVersion();
        if (levelDataVersion != 19132 && levelDataVersion != 19133) {
            throw new NbtFormatException("Unknown data version: " + Integer.toHexString(levelDataVersion));
        }

        boolean requiresManualConversion = levelDataVersion != this.getStorageVersion();
        boolean requiresFileFixing = DataFixers.getFileFixer().requiresFileFixing(dataVersion);
        Path icon = levelDirectory.iconFile();
        WorldDataConfiguration dataConfiguration = readDataConfig(dataTag);
        LevelSettings settings = LevelSettings.parse(dataTag, dataConfiguration);
        FeatureFlagSet enabledFeatureFlags = parseFeatureFlagsFromSummary(dataTag);
        boolean experimental = FeatureFlags.isExperimental(enabledFeatureFlags);
        return new LevelSummary(
            settings, levelVersion, levelDirectory.directoryName(), requiresManualConversion, requiresFileFixing, locked, experimental, icon
        );
    }

    private static FeatureFlagSet parseFeatureFlagsFromSummary(final Dynamic<?> tag) {
        Set<Identifier> enabledFlags = tag.get("enabled_features")
            .asStream()
            .flatMap(entry -> entry.asString().result().map(Identifier::tryParse).stream())
            .collect(Collectors.toSet());
        return FeatureFlags.REGISTRY.fromNames(enabledFlags, unknownId -> {});
    }

    private static @Nullable Tag readLightweightData(final Path dataFile) throws IOException {
        SkipFields parser = new SkipFields(
            new FieldSelector("Data", CompoundTag.TYPE, "Player"), new FieldSelector("Data", CompoundTag.TYPE, "WorldGenSettings")
        );
        NbtIo.parseCompressed(dataFile, parser, NbtAccounter.uncompressedQuota());
        return parser.getResult();
    }

    public boolean isNewLevelIdAcceptable(final String levelId) {
        try {
            Path fullPath = this.getLevelPath(levelId);
            Files.createDirectory(fullPath);
            Files.deleteIfExists(fullPath);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean levelExists(final String levelId) {
        try {
            return Files.isDirectory(this.getLevelPath(levelId));
        } catch (InvalidPathException e) {
            return false;
        }
    }

    public Path getLevelPath(final String levelId) {
        return this.baseDir.resolve(levelId);
    }

    public Path getBaseDir() {
        return this.baseDir;
    }

    public Path getBackupPath() {
        return this.backupDir;
    }

    public LevelStorageSource.LevelStorageAccess validateAndCreateAccess(final String levelId) throws IOException, ContentValidationException {
        Path levelPath = this.getLevelPath(levelId);
        List<ForbiddenSymlinkInfo> validationResults = this.worldDirValidator.validateDirectory(levelPath, true);
        if (!validationResults.isEmpty()) {
            throw new ContentValidationException(levelPath, validationResults);
        } else {
            return new LevelStorageSource.LevelStorageAccess(levelId, levelPath);
        }
    }

    public LevelStorageSource.LevelStorageAccess createAccess(final String levelId) throws IOException {
        Path levelPath = this.getLevelPath(levelId);
        return new LevelStorageSource.LevelStorageAccess(levelId, levelPath);
    }

    public DirectoryValidator getWorldDirValidator() {
        return this.worldDirValidator;
    }

    public record LevelCandidates(List<LevelStorageSource.LevelDirectory> levels) implements Iterable<LevelStorageSource.LevelDirectory> {
        public boolean isEmpty() {
            return this.levels.isEmpty();
        }

        @Override
        public Iterator<LevelStorageSource.LevelDirectory> iterator() {
            return this.levels.iterator();
        }
    }

    public record LevelDirectory(Path path) {
        public String directoryName() {
            return this.path.getFileName().toString();
        }

        public Path dataFile() {
            return this.resourcePath(LevelResource.LEVEL_DATA_FILE);
        }

        public Path oldDataFile() {
            return this.resourcePath(LevelResource.OLD_LEVEL_DATA_FILE);
        }

        public Path corruptedDataFile(final ZonedDateTime time) {
            return this.path.resolve(LevelResource.LEVEL_DATA_FILE.id() + "_corrupted_" + time.format(FileNameDateFormatter.FORMATTER));
        }

        public Path rawDataFile(final ZonedDateTime time) {
            return this.path.resolve(LevelResource.LEVEL_DATA_FILE.id() + "_raw_" + time.format(FileNameDateFormatter.FORMATTER));
        }

        public Path iconFile() {
            return this.resourcePath(LevelResource.ICON_FILE);
        }

        public Path lockFile() {
            return this.resourcePath(LevelResource.LOCK_FILE);
        }

        public Path resourcePath(final LevelResource resource) {
            return this.path.resolve(resource.id());
        }
    }

    public class LevelStorageAccess implements AutoCloseable {
        private DirectoryLock lock;
        private final LevelStorageSource.LevelDirectory levelDirectory;
        private final String levelId;
        private final Map<LevelResource, Path> resources = Maps.newHashMap();

        private LevelStorageAccess(final String levelId, final Path path) throws IOException {
            this.levelId = levelId;
            this.levelDirectory = new LevelStorageSource.LevelDirectory(path);
            this.createLock();
        }

        private void createLock() throws IOException {
            this.lock = DirectoryLock.create(this.levelDirectory.path);
        }

        public void releaseTemporarilyAndRun(final IORunnable runnable) throws IOException {
            this.close();

            try {
                runnable.run();
            } finally {
                this.createLock();
            }
        }

        public long estimateDiskSpace() {
            try {
                return Files.getFileStore(this.levelDirectory.path).getUsableSpace();
            } catch (Exception ignored) {
                return Long.MAX_VALUE;
            }
        }

        public boolean checkForLowDiskSpace() {
            return this.estimateDiskSpace() < 67108864L;
        }

        public void safeClose() {
            try {
                this.close();
            } catch (IOException e) {
                LevelStorageSource.LOGGER.warn("Failed to unlock access to level {}", this.getLevelId(), e);
            }
        }

        public LevelStorageSource parent() {
            return LevelStorageSource.this;
        }

        public LevelStorageSource.LevelDirectory getLevelDirectory() {
            return this.levelDirectory;
        }

        public String getLevelId() {
            return this.levelId;
        }

        public Path getLevelPath(final LevelResource resource) {
            return this.resources.computeIfAbsent(resource, this.levelDirectory::resourcePath);
        }

        public Path getDimensionPath(final ResourceKey<Level> name) {
            return DimensionType.getStorageFolder(name, this.levelDirectory.path());
        }

        private void checkLock() {
            if (!this.lock.isValid()) {
                throw new IllegalStateException("Lock is no longer valid");
            }
        }

        public PlayerDataStorage createPlayerStorage() {
            this.checkLock();
            return new PlayerDataStorage(this, LevelStorageSource.this.fixerUpper);
        }

        public void collectIssues(final boolean useFallback) throws IOException {
            this.checkLock();
            Dynamic<?> unfixedDataTag = this.getUnfixedDataTag(useFallback);
            int dataVersion = NbtUtils.getDataVersion(unfixedDataTag);
            Dynamic<?> fixedDataTag = DataFixTypes.LEVEL.updateToCurrentVersion(LevelStorageSource.this.fixerUpper, unfixedDataTag, dataVersion);
            LevelStorageSource.this.makeLevelSummary(fixedDataTag, this.levelDirectory, false, dataVersion);
        }

        public LevelSummary fixAndGetSummary() throws IOException {
            this.checkLock();
            return this.fixAndGetSummaryFromTag(this.getUnfixedDataTag(false));
        }

        public LevelSummary fixAndGetSummaryFromTag(final Dynamic<?> dataTag) {
            this.checkLock();
            int dataVersion = NbtUtils.getDataVersion(dataTag);
            Dynamic<?> dataTagFixed = DataFixTypes.LEVEL_SUMMARY.updateToCurrentVersion(LevelStorageSource.this.fixerUpper, dataTag, dataVersion);
            return LevelStorageSource.this.makeLevelSummary(dataTagFixed, this.levelDirectory, false, dataVersion);
        }

        public Dynamic<?> getUnfixedDataTagWithFallback() throws IOException {
            Dynamic<?> unfixedDataTag;
            try {
                unfixedDataTag = this.getUnfixedDataTag(false);
            } catch (IOException | NbtException | ReportedNbtException e) {
                LevelStorageSource.LOGGER.warn("Failed to load world data from {}", this.levelDirectory.dataFile(), e);
                LevelStorageSource.LOGGER.info("Attempting to use fallback {}", this.levelDirectory.oldDataFile());
                unfixedDataTag = this.getUnfixedDataTag(true);
                this.restoreLevelDataFromOld();
            }

            return unfixedDataTag;
        }

        public Dynamic<?> getUnfixedDataTag(final boolean useFallback) throws IOException {
            this.checkLock();
            Path dataFile = this.getDataFile(useFallback);
            CompoundTag root = LevelStorageSource.readLevelDataTagRaw(dataFile);
            return new Dynamic<>(NbtOps.INSTANCE, root.getCompoundOrEmpty("Data"));
        }

        private Path getDataFile(final boolean useFallback) {
            return useFallback ? this.levelDirectory.oldDataFile() : this.levelDirectory.dataFile();
        }

        public void saveDataTag(final WorldData levelData) {
            this.saveDataTag(levelData, null);
        }

        public void saveDataTag(final WorldData levelData, final @Nullable UUID singleplayerUUID) {
            CompoundTag dataTag = levelData.createTag(singleplayerUUID);
            CompoundTag root = new CompoundTag();
            root.put("Data", dataTag);
            this.saveLevelData(root);
        }

        public void saveLevelData(final Dynamic<?> tag) {
            Tag genericTag = tag.convert(NbtOps.INSTANCE).getValue();
            CompoundTag root = new CompoundTag();
            root.put("Data", genericTag);
            this.saveLevelData(root);
        }

        private void saveLevelData(final CompoundTag root) {
            Path worldDir = this.levelDirectory.path();

            try {
                Path dataFile = Files.createTempFile(worldDir, "level", ".dat");
                NbtIo.writeCompressed(root, dataFile);
                Path oldDataFile = this.levelDirectory.oldDataFile();
                Path currentFile = this.levelDirectory.dataFile();
                Util.safeReplaceFile(currentFile, dataFile, oldDataFile);
            } catch (Exception e) {
                LevelStorageSource.LOGGER.error("Failed to save level {}", worldDir, e);
            }
        }

        public Optional<Path> getIconFile() {
            return !this.lock.isValid() ? Optional.empty() : Optional.of(this.levelDirectory.iconFile());
        }

        public void deleteLevel() throws IOException {
            this.checkLock();
            final Path lockPath = this.levelDirectory.lockFile();
            LevelStorageSource.LOGGER.info("Deleting level {}", this.levelId);

            for (int attempt = 1; attempt <= 5; attempt++) {
                LevelStorageSource.LOGGER.info("Attempt {}...", attempt);

                try {
                    Files.walkFileTree(this.levelDirectory.path(), new SimpleFileVisitor<Path>() {
                        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                            if (!file.equals(lockPath)) {
                                LevelStorageSource.LOGGER.debug("Deleting {}", file);
                                Files.deleteIfExists(file);
                            }

                            return FileVisitResult.CONTINUE;
                        }

                        public FileVisitResult postVisitDirectory(final Path dir, final @Nullable IOException exc) throws IOException {
                            if (exc != null) {
                                throw exc;
                            }

                            if (dir.equals(LevelStorageAccess.this.levelDirectory.path())) {
                                LevelStorageAccess.this.lock.close();
                                Files.deleteIfExists(lockPath);
                            }

                            Files.deleteIfExists(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                    break;
                } catch (IOException e) {
                    if (attempt >= 5) {
                        throw e;
                    }

                    LevelStorageSource.LOGGER.warn("Failed to delete {}", this.levelDirectory.path(), e);

                    try {
                        Thread.sleep(500L);
                    } catch (InterruptedException var5) {
                    }
                }
            }
        }

        public void renameLevel(final String newName) throws IOException {
            this.modifyLevelDataWithoutDatafix(tag -> tag.putString("LevelName", newName.trim()));
        }

        public void renameAndDropPlayer(final String newName) throws IOException {
            this.modifyLevelDataWithoutDatafix(tag -> {
                tag.putString("LevelName", newName.trim());
                tag.remove("singleplayer_uuid");
            });
        }

        private void modifyLevelDataWithoutDatafix(final Consumer<CompoundTag> updater) throws IOException {
            this.checkLock();
            CompoundTag root = LevelStorageSource.readLevelDataTagRaw(this.levelDirectory.dataFile());
            updater.accept(root.getCompoundOrEmpty("Data"));
            this.saveLevelData(root);
        }

        public long makeWorldBackup() throws IOException {
            this.checkLock();
            String zipFilePrefix = FileNameDateFormatter.FORMATTER.format(ZonedDateTime.now()) + "_" + this.levelId;
            Path root = LevelStorageSource.this.getBackupPath();

            try {
                FileUtil.createDirectoriesSafe(root);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Path zipFilePath = root.resolve(FileUtil.findAvailableName(root, zipFilePrefix, ".zip"));

            try (final ZipOutputStream stream = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(zipFilePath)))) {
                final Path rootPath = Paths.get(this.levelId);
                Files.walkFileTree(this.levelDirectory.path(), new SimpleFileVisitor<Path>() {
                    public FileVisitResult visitFile(final Path path, final BasicFileAttributes attrs) throws IOException {
                        if (path.endsWith("session.lock")) {
                            return FileVisitResult.CONTINUE;
                        }

                        String entryPath = rootPath.resolve(LevelStorageAccess.this.levelDirectory.path().relativize(path)).toString().replace('\\', '/');
                        ZipEntry entry = new ZipEntry(entryPath);
                        stream.putNextEntry(entry);
                        com.google.common.io.Files.asByteSource(path.toFile()).copyTo(stream);
                        stream.closeEntry();
                        return FileVisitResult.CONTINUE;
                    }
                });
            }

            return Files.size(zipFilePath);
        }

        public boolean hasWorldData() {
            return Files.exists(this.levelDirectory.dataFile()) || Files.exists(this.levelDirectory.oldDataFile());
        }

        @Override
        public void close() throws IOException {
            this.lock.close();
        }

        public boolean restoreLevelDataFromOld() {
            return Util.safeReplaceOrMoveFile(
                this.levelDirectory.dataFile(), this.levelDirectory.oldDataFile(), this.levelDirectory.corruptedDataFile(ZonedDateTime.now()), true
            );
        }

        public @Nullable Instant getFileModificationTime(final boolean fallback) {
            return LevelStorageSource.getFileModificationTime(this.getDataFile(fallback));
        }
    }
}