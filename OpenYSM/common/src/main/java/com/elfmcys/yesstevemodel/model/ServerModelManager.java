package com.elfmcys.yesstevemodel.model;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.AuthModelsCapability;
import com.elfmcys.yesstevemodel.capability.ModelInfoCapability;
import com.elfmcys.yesstevemodel.client.ExportResult;
import com.elfmcys.yesstevemodel.config.ServerConfig;
import com.elfmcys.yesstevemodel.mixin.ConnectionAccessor;
import com.elfmcys.yesstevemodel.mixin.ServerCommonPacketListenerImplAccessor;
import com.elfmcys.yesstevemodel.model.format.ServerAnimationInfo;
import com.elfmcys.yesstevemodel.model.format.ServerModelData;
import com.elfmcys.yesstevemodel.model.format.ServerModelInfo;
import com.elfmcys.yesstevemodel.model.format.UUIDComponentData;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.S2CModelSyncPayload;
import com.elfmcys.yesstevemodel.network.message.S2CSyncAuthModelsPacket;
import com.elfmcys.yesstevemodel.resource.YSMBinaryDeserializer;
import com.elfmcys.yesstevemodel.resource.YSMBinarySerializer;
import com.elfmcys.yesstevemodel.resource.YSMClientMapper;
import com.elfmcys.yesstevemodel.resource.YSMFolderDeserializer;
import com.elfmcys.yesstevemodel.resource.models.MainModelInfo;
import com.elfmcys.yesstevemodel.resource.pojo.RawYsmModel;
import com.elfmcys.yesstevemodel.util.YSMNativeHelper;
import com.elfmcys.yesstevemodel.util.YSMThreadPool;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.architectury.platform.Platform;
import dev.architectury.utils.GameInstance;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.floats.FloatReferencePair;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rip.ysm.api.PlatformAPI;
import rip.ysm.legacy.YesModelUtils;
import rip.ysm.security.YSMByteBuf;
import rip.ysm.security.YsmCrypt;
import rip.ysm.security.YsmCrypt.CachePayload;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

public final class ServerModelManager {
    /**
     * 配置相关文件夹
     */
    public static final Path FOLDER = Paths.get("config", YesSteveModel.MOD_ID);

    /**
     * 自定义模型所放置的文件夹
     */
    public static final Path BUILT = FOLDER.resolve("built");
    public static final Path CUSTOM = FOLDER.resolve("custom");
    public static final Path AUTH = FOLDER.resolve("auth");
    public static final Path EXPORT = FOLDER.resolve("export");

    /**
     * 生成缓存文件的文件夹
     */
    public static final Path CACHE = FOLDER.resolve("cache");
    public static final Path CACHE_SERVER_INDEX_FILE = CACHE.resolve("server_index");
    public static final Path CACHE_SERVER_CATALOG_FILE = CACHE.resolve("server_catalog.json");
    public static final Path CACHE_SERVER = CACHE.resolve("server");
    public static final Path CACHE_CLIENT = CACHE.resolve("client");

    /**
     * 模型名称 -> 模型额外信息缓存
     * 可以方便的通过此缓存，来判断客户端发来的 MD5 在不在服务端
     * 从而将服务器文件发送给玩家
     * 还可以获取其他服务端模型信息
     */
    private static Map<String, ServerModelData> CACHE_NAME_INFO = Maps.newHashMap();

    private static IntOpenHashSet modelHashSet = new IntOpenHashSet();

    /**
     * 放置授权模型名称
     */
    private static Set<String> AUTH_MODELS = Sets.newHashSet();

    private static final Map<UUID, PlayerSyncState> syncStates = new ConcurrentHashMap<>();
    private static final Map<String, ServerPackData> packs = new ConcurrentHashMap<>();
    private static final SecureRandom theRandom = new SecureRandom();
    public static byte[] serverKey;
    private static volatile boolean initialized = false;
    private static volatile String loadedSourceState;
    private static final Gson GSON = new Gson();
    private static final int CATALOG_VERSION = 2;

    private static RateLimiter bandwidthLimiter = null;
    private static Semaphore threadLimiter = null;
    private static boolean limitsInitialized = false;

    private static void initRateLimit() {
        if (!limitsInitialized) {
            try {
                int mbps = ServerConfig.BANDWIDTH_LIMIT.get();
                double bytesPerSec = Math.max(1.0, mbps * 131072.0);
                bandwidthLimiter = RateLimiter.create(bytesPerSec);

                int threads = ServerConfig.THREAD_COUNT.get();
                if (threads <= 0) {
                    threads = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
                }
                threadLimiter = new Semaphore(threads);

                limitsInitialized = true;
            } catch (Exception e) {
                YesSteveModel.LOGGER.error("[YSM] Failed to initialize limits from config", e);
                bandwidthLimiter = RateLimiter.create(5 * 131072.0);
                threadLimiter = new Semaphore(Math.max(2, Runtime.getRuntime().availableProcessors() - 1));
                limitsInitialized = true;
            }
        }
    }

    public static class ServerPackData {
        public String folderPath;
        public byte[] iconData;
        public int iconWidth, iconHeight, iconFormat;
        public String name;
        public String description;
        public Map<String, Map<String, String>> lang;
    }

    private static class CatalogFile {
        int version = CATALOG_VERSION;
        Map<String, CachedModelMetadata> models = new LinkedHashMap<>();
    }

    private static class CachedModelMetadata {
        SourceSnapshot source;
        boolean auth;
        boolean customSkinModel;
        RawYsmModel.RawMetadata metadata;
        RawYsmModel.RawProperties properties;
        RawYsmModel.RawFooter footer;
        int bones;
        int cubes;
        int faces;
        Map<String, String[]> animations;
        Map<String, Map<String, String>> translations;
        String[] textures;
        String[][] projectiles;
        String[][] vehicles;
        long cacheSize;
        long cacheModified;
    }

    private static class SourceSnapshot {
        String fingerprint;
        Map<String, SourceFileState> files = new LinkedHashMap<>();
    }

    private static class SourceFileState {
        long size;
        long modified;
        String sha256;
    }

    public static void reloadPacks() throws IOException {
        initialized = false;
        loadedSourceState = null;
        CACHE_NAME_INFO.clear();
        AUTH_MODELS.clear();

        createFolder(FOLDER);
        createFolder(BUILT);
        createFolder(CUSTOM);
        createFolder(AUTH);
        createFolder(EXPORT);

        createFolder(CACHE);
        createFolder(CACHE_SERVER);
        createFolder(CACHE_CLIENT);

        extractBuiltinModels();

        Files.writeString(BUILT.resolve("notice.txt"),
                "This directory is cleared every time the game starts!\n" +
                        "该目录会在每次游戏启动时清空！",
                StandardCharsets.UTF_8);

        Path blacklistFile = FOLDER.resolve("blacklist.txt");
        if (!Files.exists(blacklistFile)) {
            String content =
                    "# Yes Steve Model 模组 - 内置模型黑名单配置文件\n" +
                            "# Yes Steve Model Mod - Built-in Model Blacklist Configuration File\n" +
                            "\n" +
                            "# 功能说明：\n" +
                            "# 随着内置模型数量的增加，为了满足个性化定制需求，本模组提供了黑名单功能\n" +
                            "# 允许用户选择性地禁用不需要的内置模型，以节省存储空间和加载时间\n" +
                            "#\n" +
                            "# Feature Description:\n" +
                            "# As the number of built-in models increases, this mod provides blacklist functionality\n" +
                            "# to meet customization needs, allowing users to selectively disable unwanted built-in\n" +
                            "# models to save storage space and loading time.\n" +
                            "\n" +
                            "# 使用方法：\n" +
                            "# 1. 在游戏启动前编辑此文件\n" +
                            "# 2. 清空 <游戏目录>/config/yes_steve_model/builtin 文件夹中的已解压模型文件\n" +
                            "# 3. 重新启动游戏，模组将根据黑名单规则跳过指定模型的解压\n" +
                            "#\n" +
                            "# Usage Instructions:\n" +
                            "# 1. Edit this file before starting the game\n" +
                            "# 2. Clear extracted model files in <game_directory>/config/yes_steve_model/builtin folder\n" +
                            "# 3. Restart the game, the mod will skip extracting specified models based on blacklist rules\n" +
                            "\n" +
                            "# 注意事项：\n" +
                            "# - default 模型采用特殊加载机制，无法通过黑名单禁用\n" +
                            "# - 配置文件位置：<游戏目录>/config/yes_steve_model/blacklist.txt\n" +
                            "# - 以 # 开头的行被视为注释，不会被处理\n" +
                            "# - 每行一个规则，使用正则表达式匹配模型的完整解压路径\n" +
                            "#\n" +
                            "# Important Notes:\n" +
                            "# - The default model uses special loading mechanism and cannot be disabled via blacklist\n" +
                            "# - Config file location: <game_directory>/config/yes_steve_model/blacklist.txt\n" +
                            "# - Lines starting with # are comments and will not be processed\n" +
                            "# - One rule per line, using regular expressions to match the complete extraction path of models\n" +
                            "\n" +
                            "# 路径匹配规则：\n" +
                            "# 模组解压时会使用以下格式的路径进行正则表达式匹配：\n" +
                            "#\n" +
                            "# Path Matching Rules:\n" +
                            "# The mod will use the following path formats for regular expression matching during extraction:\n" +
                            "#\n" +
                            "# assets/yes_steve_model/builtin/wine_fox/01_taisho_maid/animations/arrow.animation.json\n" +
                            "# assets/yes_steve_model/builtin/wine_fox/01_taisho_maid/avatar/nico.png\n" +
                            "# assets/yes_steve_model/builtin/misc/2_steve/ysm.json\n" +
                            "\n" +
                            "# 配置示例：\n" +
                            "# 重要提示：下面的示例都以 # 开头，这表示它们目前是注释状态，不会生效\n" +
                            "# 如果你想要启用某个规则，请删除该行开头的 # 号和空格\n" +
                            "#\n" +
                            "# Configuration Examples:\n" +
                            "# Important Notice: All examples below start with #, meaning they are currently commented out and inactive\n" +
                            "# To enable a rule, delete the # symbol and space at the beginning of that line\n" +
                            "\n" +
                            "# 示例1：禁用所有酒狐系列模型 | Example 1: Disable all Wine Fox series models\n" +
                            "# assets/yes_steve_model/builtin/wine_fox/.*\n" +
                            "\n" +
                            "# 示例2：禁用杂项模型文件夹下的所有模型 | Example 2: Disable all models in misc folder\n" +
                            "# assets/yes_steve_model/builtin/misc/.*\n" +
                            "\n" +
                            "# 示例3：禁用特定的大正女仆酒狐模型 | Example 3: Disable specific Taisho Maid Wine Fox model\n" +
                            "# assets/yes_steve_model/builtin/wine_fox/01_taisho_maid/.*\n" +
                            "\n" +
                            "# 示例4：禁用所有内置模型 | Example 4: Disable all built-in models\n" +
                            "# .*";
            Files.writeString(blacklistFile, content, StandardCharsets.UTF_8);
        }
        processBlacklist(blacklistFile);

        Path serverIndex = CACHE_SERVER_INDEX_FILE;
        byte[] serverKeyBytes;

        if (Files.exists(serverIndex)) {
            try {
                String jsonStr = Files.readString(serverIndex, StandardCharsets.UTF_8);
                JsonObject jsonElement = JsonParser.parseString(jsonStr).getAsJsonObject();

                if (jsonElement.get("server_key") != null && jsonElement.get("server_key").getAsJsonPrimitive().isString()) {
                    serverKeyBytes = Base64.getDecoder().decode(jsonElement.get("server_key").getAsString());
                    if (serverKeyBytes.length != 56) {
                        throw new IllegalStateException("ServerKey length must be 56 bytes, but got " + serverKeyBytes.length);
                    }
                } else {
                    serverKeyBytes = new byte[56];
                    new SecureRandom().nextBytes(serverKeyBytes);
                    jsonElement.addProperty("server_key", Base64.getEncoder().encodeToString(serverKeyBytes));
                    Files.writeString(serverIndex, jsonElement.toString(), StandardCharsets.UTF_8);
                }
            } catch (Exception e) {
                serverKeyBytes = new byte[56];
                new SecureRandom().nextBytes(serverKeyBytes);
                JsonObject jsonElement = new JsonObject();
                jsonElement.addProperty("server_key", Base64.getEncoder().encodeToString(serverKeyBytes));
                Files.writeString(serverIndex, jsonElement.toString(), StandardCharsets.UTF_8);
            }
        } else {
            serverKeyBytes = new byte[56];
            new SecureRandom().nextBytes(serverKeyBytes);
            JsonObject jsonElement = new JsonObject();
            jsonElement.addProperty("server_key", Base64.getEncoder().encodeToString(serverKeyBytes));
            Files.writeString(serverIndex, jsonElement.toString(), StandardCharsets.UTF_8);
        }

        serverKey = serverKeyBytes;
        nativeLoadModels(null);
    }

    private static void extractBuiltinModels() {
        try {
            Path assetsBuiltin = Platform.getMod(YesSteveModel.MOD_ID).findResource("assets", YesSteveModel.MOD_ID, "builtin").orElse(null);

            if (assetsBuiltin == null || !Files.isDirectory(assetsBuiltin)) return;

            Set<String> sourcePaths = new HashSet<>();
            try (Stream<Path> walker = Files.walk(assetsBuiltin)) {
                walker.forEach(src -> {
                    try {
                        Path relative = assetsBuiltin.relativize(src);
                        sourcePaths.add(relative.toString().replace('\\', '/'));
                        Path dest = ServerModelManager.BUILT.resolve(relative.toString());
                        if (Files.isDirectory(src)) {
                            Files.createDirectories(dest);
                        } else {
                            Files.createDirectories(dest.getParent());
                            boolean unchanged = Files.isRegularFile(dest)
                                    && Files.size(src) == Files.size(dest)
                                    && Files.getLastModifiedTime(src).toMillis() == Files.getLastModifiedTime(dest).toMillis();
                            if (!unchanged) {
                                var modified = Files.getLastModifiedTime(src);
                                Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                                try {
                                    Files.setLastModifiedTime(dest, modified);
                                } catch (IOException ignored) {
                                }
                            }
                        }
                    } catch (IOException e) {
                        YesSteveModel.LOGGER.warn("Failed to extract builtin: " + src.getFileName(), e);
                    }
                });
            }
            try (Stream<Path> walker = Files.walk(BUILT)) {
                walker.sorted(Comparator.reverseOrder()).forEach(dest -> {
                    if (dest.equals(BUILT) || dest.equals(BUILT.resolve("notice.txt"))) return;
                    String relative = BUILT.relativize(dest).toString().replace('\\', '/');
                    if (!sourcePaths.contains(relative)) {
                        try {
                            Files.deleteIfExists(dest);
                        } catch (IOException e) {
                            YesSteveModel.LOGGER.warn("Failed to remove stale builtin: " + dest.getFileName(), e);
                        }
                    }
                });
            }
        } catch (Exception e) {
            YesSteveModel.LOGGER.error("Failed to extract builtin models", e);
        }
    }

    private static void processBlacklist(Path blacklistFile) {
        List<Pattern> rules = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(blacklistFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                try {
                    rules.add(Pattern.compile(line));
                } catch (PatternSyntaxException ignored) {
                }
            }
        } catch (IOException e) {
            return;
        }

        if (rules.isEmpty() || !Files.isDirectory(BUILT)) return;

        try (DirectoryStream<Path> groups = Files.newDirectoryStream(BUILT)) {
            for (Path group : groups) {
                if (!Files.isDirectory(group)) continue;
                boolean hasRemainingModels = false;
                try (DirectoryStream<Path> models = Files.newDirectoryStream(group)) {
                    for (Path model : models) {
                        if (!Files.isDirectory(model)) continue;

                        String matchPath = "assets/yes_steve_model/builtin/" + group.getFileName() + "/" + model.getFileName() + "/";
                        boolean deleted = false;
                        for (Pattern rule : rules) {
                            if (rule.matcher(matchPath).find()) {
                                deleteRecursively(model);
                                deleted = true;
                                break;
                            }
                        }

                        if (!deleted) {
                            hasRemainingModels = true;
                        }
                    }
                }
                if (!hasRemainingModels) {
                    deleteRecursively(group);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            Files.deleteIfExists(dir);
            return;
        }
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(dir)) {
            for (Path entry : entries) {
                deleteRecursively(entry);
            }
        }
        Files.deleteIfExists(dir);
    }

    private static void createFolder(Path path) {
        File folder = path.toFile();
        if (!folder.isDirectory()) {
            try {
                Files.createDirectories(folder.toPath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class PlayerSyncState {
        byte[] clientKey = new byte[56];
        byte[] key1;
        byte[] clientNextKey;
        int step = 0;
        List<ServerModelData> allowedModels = new ArrayList<>();

        // TODO: 未来可基于UUID持久化，这里目前每次加入生成固定clientKey
        PlayerSyncState() {new Random(114514).nextBytes(clientKey);}
    }

    public static void nativeSendModelData(UUID uuid, @Nullable ByteBuffer data) {
        if (data != null && !data.hasRemaining() && data.position() > 0) {
            data.flip();
        }

        if (data == null || data.remaining() == 0) {
            syncStates.remove(uuid);
            return;
        }

        PlayerSyncState state = syncStates.get(uuid);
        if (state == null) return;

        try {
            byte[] packetBytes = new byte[data.remaining()];
            data.get(packetBytes);
            System.out.println("Server Handle packet, step=" + state.step + ", length=" + packetBytes.length);

            if (state.step == 1) {
                // 等待Pong
                byte[] decrypted = YsmCrypt.decrypt(packetBytes, state.key1);
                if (decrypted == null || decrypted.length < 56) return;

                // 客戶端生成的密鑰
                state.clientNextKey = Arrays.copyOfRange(decrypted, decrypted.length - 56, decrypted.length);
                byte[] payload = Arrays.copyOfRange(decrypted, 0, decrypted.length - 56);

                try (YSMByteBuf buf = new YSMByteBuf(Unpooled.wrappedBuffer(payload))) {
                    buf.skipGarbageHeader();
                    if (buf.getRawBuf().readByte() != 0x02) return;
                }

                // 發送可用模型
                state.step = 2;
                sendPacket03(uuid, state);
            } else if (state.step == 2) {
                byte[] decrypted = YsmCrypt.decrypt(packetBytes, state.key1);
                if (decrypted == null) return;

                try (YSMByteBuf buf = new YSMByteBuf(Unpooled.wrappedBuffer(decrypted))) {
                    buf.skipGarbageHeader();
                    if (buf.getRawBuf().readByte() != 0x04) return;

                    int numRequests = buf.readVarInt();
                    List<long[]> requestedHashes = new ArrayList<>();
                    for (int i = 0; i < numRequests; i++) {
                        requestedHashes.add(new long[]{buf.readVarLong(), buf.readVarLong()});
                    }
                    state.step = 3;
                    sendPacket05(uuid, state, requestedHashes);
                }
            }
        } catch (Exception e) {
            YesSteveModel.LOGGER.error("[YSM] Server sync error for " + uuid, e);
        }
    }

    public static boolean nativeLoadModels(Object callback) {
        long begin = System.currentTimeMillis();
        ForkJoinPool pool = createLoadPool();
        try {
            Map<String, ServerModelData> loadedModels = new ConcurrentHashMap<>();
            Set<String> authIds = ConcurrentHashMap.newKeySet();
            Set<String> validCacheFiles = ConcurrentHashMap.newKeySet();
            CatalogFile previousCatalog = readCatalog();
            CatalogFile nextCatalog = new CatalogFile();
            Map<String, CachedModelMetadata> nextCatalogModels = new ConcurrentHashMap<>();
            ConcurrentMap<String, SourceSnapshot> snapshots = new ConcurrentHashMap<>();

            packs.clear();

            pool.submit(() -> {
                scanSource(BUILT, CACHE_SERVER, loadedModels, authIds, validCacheFiles, false, previousCatalog, nextCatalogModels, snapshots);
                scanSource(CUSTOM, CACHE_SERVER, loadedModels, authIds, validCacheFiles, false, previousCatalog, nextCatalogModels, snapshots);
                scanSource(AUTH, CACHE_SERVER, loadedModels, authIds, validCacheFiles, true, previousCatalog, nextCatalogModels, snapshots);
            }).get();

            nextCatalog.models.putAll(nextCatalogModels);
            loadedSourceState = computeModelSourceState();

            Map<String, ServerModelData> orderedModels = new LinkedHashMap<>();
            loadedModels.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> orderedModels.put(entry.getKey(), entry.getValue()));

            try {
                writeCatalog(nextCatalog);
            } catch (IOException e) {
                YesSteveModel.LOGGER.warn("Failed to write server model catalog", e);
            }

            try (Stream<Path> stream = Files.list(CACHE_SERVER)) {
                stream.forEach(file -> {
                    if (!validCacheFiles.contains(file.getFileName().toString())) {
                        try { Files.deleteIfExists(file); } catch (Exception ignored) {}
                    }
                });
            } catch (Exception ignored) {}

            YesSteveModel.LOGGER.info("[YSM] Loaded {} models in {}ms", orderedModels.size(), System.currentTimeMillis() - begin);
            ModelLoadResult result = new ModelLoadResult(true, null, orderedModels, authIds.toArray(new String[0]));
            AUTH_MODELS = new HashSet<>(authIds);

            onModelLoadComplete(result, callback);
            return true;
        } catch (Exception e) {
            YesSteveModel.LOGGER.error("[YSM] Model loading failed", e);
            return false;
        } finally {
            pool.shutdown();
        }
    }

    private static ForkJoinPool createLoadPool() {
        int cpuLimit = Runtime.getRuntime().availableProcessors();
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long availableMemory = runtime.maxMemory() - usedMemory;
        long perThreadBudget = 1024L * 1024 * 1024;
        int memoryLimit = Math.max(1, (int) Math.min(cpuLimit, availableMemory / perThreadBudget));
        int parallelism = Math.max(1, Math.min(cpuLimit, memoryLimit));
        YesSteveModel.LOGGER.info("Begin build cache with {} worker(s). (cpu={}, mem={}MB free, budget={}MB/thread)", parallelism, cpuLimit, availableMemory / (1024 * 1024), perThreadBudget / (1024 * 1024));
        AtomicInteger index = new AtomicInteger();
        return new ForkJoinPool(parallelism, p -> {
            ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(p);
            thread.setName("ModelWorker#" + index.getAndIncrement());
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        }, null, true);
    }

    private static void scanSource(Path baseDir, Path cacheDir, Map<String, ServerModelData> loaded, Set<String> authIds, Set<String> validCaches, boolean isAuth, CatalogFile previousCatalog, Map<String, CachedModelMetadata> nextCatalog, ConcurrentMap<String, SourceSnapshot> snapshots) {
        if (baseDir == null || !Files.isDirectory(baseDir)) return;

        scanDirectoryPacks(baseDir, "");
        String sourceGroup = baseDir.getFileName().toString();
        scanDirectoryModels(baseDir, "", cacheDir, loaded, authIds, validCaches, isAuth, previousCatalog, nextCatalog, snapshots, sourceGroup, null);

        List<Path> zipFiles = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(baseDir, FileVisitOption.FOLLOW_LINKS)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip"))
                    .forEach(zipFiles::add);
        } catch (FileSystemLoopException e) {
            YesSteveModel.LOGGER.warn("Symlink loop while scanning zips in: " + baseDir, e);
        } catch (IOException e) {
            YesSteveModel.LOGGER.error("Failed to collect zip files from: " + baseDir, e);
            return;
        }

        zipFiles.parallelStream().forEach(zipFile -> processZip(zipFile, baseDir, cacheDir, loaded, authIds, validCaches, isAuth, previousCatalog, nextCatalog, snapshots, sourceGroup));
    }

    private static void processZip(Path zipFile, Path baseDir, Path cacheDir, Map<String, ServerModelData> loaded, Set<String> authIds, Set<String> validCaches, boolean isAuth, CatalogFile previousCatalog, Map<String, CachedModelMetadata> nextCatalog, ConcurrentMap<String, SourceSnapshot> snapshots, String sourceGroup) {
        String zipName = zipFile.getFileName().toString();
        String zipBaseName = zipName.substring(0, zipName.length() - 4);

        Path parent = zipFile.getParent();
        String prefix = parent != null ? baseDir.relativize(parent).toString().replace('\\', '/') : "";
        if (!prefix.isEmpty() && !prefix.endsWith("/")) prefix += "/";

        try {
            URI uri = URI.create("jar:" + zipFile.toUri());
            try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                Path root = fs.getPath("/");
                if (YSMFolderDeserializer.isModelFolder(root)) {
                    String modelId = prefix + zipBaseName;
                    String catalogKey = catalogKey(sourceGroup, modelId);
                    CachedModelMetadata previous = previousCatalog.models.get(catalogKey);
                    SourceSnapshot snapshot = snapshot(zipFile, previous != null ? previous.source : null, snapshots);
                    ServerModelData cached = restoreCachedModel(modelId, previous, snapshot, cacheDir, isAuth, validCaches);
                    if (cached != null) {
                        registerModel(catalogKey, modelId, cached, previous, snapshot, loaded, authIds, nextCatalog, isAuth);
                    } else {
                        try (YSMFolderDeserializer deserializer = new YSMFolderDeserializer(root)) {
                            RawYsmModel rawModel = deserializer.deserialize();
                            registerParsedModel(catalogKey, modelId, rawModel, snapshot, cacheDir, loaded, authIds, validCaches, nextCatalog, isAuth);
                        } catch (Exception e) {
                            YesSteveModel.LOGGER.error("Failed to load model in zip: " + zipFile, e);
                        }
                    }
                } else {
                    scanDirectoryPacks(root, prefix);
                    scanDirectoryModels(root, prefix, cacheDir, loaded, authIds, validCaches, isAuth, previousCatalog, nextCatalog, snapshots, sourceGroup, zipFile);
                }
            }
        } catch (Exception e) {
            YesSteveModel.LOGGER.error("Failed to scan zip model source: " + zipFile, e);
        }
    }

    private static void scanDirectoryModels(Path searchRoot, String prefix, Path cacheDir, Map<String, ServerModelData> loaded, Set<String> authIds, Set<String> validCaches, boolean isAuth, CatalogFile previousCatalog, Map<String, CachedModelMetadata> nextCatalog, ConcurrentMap<String, SourceSnapshot> snapshots, String sourceGroup, @Nullable Path sourceOverride) {
        if (searchRoot == null || !Files.isDirectory(searchRoot)) return;

        List<Path> modelFolders = new ArrayList<>();
        List<Path> ysmFiles = new ArrayList<>();

        try {
            Files.walkFileTree(searchRoot, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                @Override
                public @NotNull FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) {
                    if (dir.equals(searchRoot)) return FileVisitResult.CONTINUE;
                    try {
                        if (YSMFolderDeserializer.isModelFolder(dir)) {
                            modelFolders.add(dir);
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                    } catch (Exception e) {
                        YesSteveModel.LOGGER.error("Error checking directory: " + dir, e);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
                    if (file.getFileName().toString().endsWith(".ysm")) ysmFiles.add(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NotNull FileVisitResult visitFileFailed(@NotNull Path file, @NotNull IOException exc) {
                    if (exc instanceof FileSystemLoopException) {
                        YesSteveModel.LOGGER.warn("Symlink loop skipped at: " + file);
                        return FileVisitResult.CONTINUE;
                    }
                    YesSteveModel.LOGGER.warn("Failed to visit: " + file, exc);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            YesSteveModel.LOGGER.error("Failed to walk directory tree: " + searchRoot, e);
            return;
        }

        modelFolders.parallelStream().forEach(dir -> processModelFolder(dir, searchRoot, prefix, cacheDir, loaded, authIds, validCaches, isAuth, previousCatalog, nextCatalog, snapshots, sourceGroup, sourceOverride));
        ysmFiles.parallelStream().forEach(file -> processYsmFile(file, searchRoot, prefix, cacheDir, loaded, authIds, validCaches, isAuth, previousCatalog, nextCatalog, snapshots, sourceGroup, sourceOverride));
    }

    private static void processModelFolder(Path dir, Path searchRoot, String prefix, Path cacheDir, Map<String, ServerModelData> loaded, Set<String> authIds, Set<String> validCaches, boolean isAuth, CatalogFile previousCatalog, Map<String, CachedModelMetadata> nextCatalog, ConcurrentMap<String, SourceSnapshot> snapshots, String sourceGroup, @Nullable Path sourceOverride) {
        try {
            String modelId = prefix + searchRoot.relativize(dir).toString().replace('\\', '/');
            String key = catalogKey(sourceGroup, modelId);
            CachedModelMetadata previous = previousCatalog.models.get(key);
            Path source = sourceOverride != null ? sourceOverride : dir;
            SourceSnapshot current = snapshot(source, previous != null ? previous.source : null, snapshots);
            ServerModelData cached = restoreCachedModel(modelId, previous, current, cacheDir, isAuth, validCaches);
            if (cached != null) {
                registerModel(key, modelId, cached, previous, current, loaded, authIds, nextCatalog, isAuth);
                return;
            }

            RawYsmModel rawModel = null;
            try (YSMFolderDeserializer deserializer = new YSMFolderDeserializer(dir)) {
                rawModel = deserializer.deserialize();
            } catch (Exception e) {
                YesSteveModel.LOGGER.error("Failed to load model at: " + dir, e);
            }

            if (rawModel != null) {
                try {
                    registerParsedModel(key, modelId, rawModel, current, cacheDir, loaded, authIds, validCaches, nextCatalog, isAuth);
                } catch (Exception e) {
                    YesSteveModel.LOGGER.error("Failed to process model at: " + dir, e);
                }
            }
        } catch (Exception e) {
            YesSteveModel.LOGGER.error("Error processing model folder: " + dir, e);
        }
    }

    private static void processYsmFile(Path file, Path searchRoot, String prefix, Path cacheDir, Map<String, ServerModelData> loaded, Set<String> authIds, Set<String> validCaches, boolean isAuth, CatalogFile previousCatalog, Map<String, CachedModelMetadata> nextCatalog, ConcurrentMap<String, SourceSnapshot> snapshots, String sourceGroup, @Nullable Path sourceOverride) {
        try {
            String modelId = prefix + searchRoot.relativize(file).toString().replace('\\', '/');
            String key = catalogKey(sourceGroup, modelId);
            CachedModelMetadata previous = previousCatalog.models.get(key);
            Path source = sourceOverride != null ? sourceOverride : file;
            SourceSnapshot current = snapshot(source, previous != null ? previous.source : null, snapshots);
            ServerModelData cached = restoreCachedModel(modelId, previous, current, cacheDir, isAuth, validCaches);
            if (cached != null) {
                registerModel(key, modelId, cached, previous, current, loaded, authIds, nextCatalog, isAuth);
                return;
            }
            byte[] raw = Files.readAllBytes(file);
            int ysmCryptoVersion = YesModelUtils.getYsmCryptoVersion(raw);
            if (ysmCryptoVersion == -1) throw new IllegalStateException("Unknown YSM crypto version for file: " + file);

            RawYsmModel rawModel;
            if (ysmCryptoVersion == 1 || ysmCryptoVersion == 2) {
                Map<String, byte[]> input = YesModelUtils.input(raw);
                try (YSMFolderDeserializer deserializer = new YSMFolderDeserializer(input)) {
                    rawModel = deserializer.deserialize();
                }
            } else {
                byte[] decrypted = YsmCrypt.decryptYsmFile(raw);
                try (YSMBinaryDeserializer deserializer = new YSMBinaryDeserializer(decrypted)) {
                    rawModel = deserializer.deserializeKeepOpen();
                    deserializer.parseYSMFooter(rawModel);
                }
            }

            registerParsedModel(key, modelId, rawModel, current, cacheDir, loaded, authIds, validCaches, nextCatalog, isAuth);
        } catch (Exception e) {
            YesSteveModel.LOGGER.error("Failed to load binary model at: " + file, e);
        }
    }

    private static CatalogFile readCatalog() {
        if (!Files.isRegularFile(CACHE_SERVER_CATALOG_FILE)) return new CatalogFile();
        try (var reader = Files.newBufferedReader(CACHE_SERVER_CATALOG_FILE, StandardCharsets.UTF_8)) {
            CatalogFile catalog = GSON.fromJson(reader, CatalogFile.class);
            if (catalog == null || catalog.version != CATALOG_VERSION || catalog.models == null) return new CatalogFile();
            return catalog;
        } catch (Exception e) {
            YesSteveModel.LOGGER.warn("Failed to read server model catalog", e);
            return new CatalogFile();
        }
    }

    private static void writeCatalog(CatalogFile catalog) throws IOException {
        Files.createDirectories(CACHE);
        Path temporary = CACHE_SERVER_CATALOG_FILE.resolveSibling(CACHE_SERVER_CATALOG_FILE.getFileName() + ".tmp");
        try (var writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            GSON.toJson(catalog, writer);
        }
        try {
            Files.move(temporary, CACHE_SERVER_CATALOG_FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, CACHE_SERVER_CATALOG_FILE, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String catalogKey(String sourceGroup, String modelId) {
        return sourceGroup + '|' + modelId;
    }

    private static SourceSnapshot snapshot(Path source, @Nullable SourceSnapshot previous, ConcurrentMap<String, SourceSnapshot> snapshots) throws IOException {
        String cacheKey = source.toAbsolutePath().normalize().toString();
        SourceSnapshot cached = snapshots.get(cacheKey);
        if (cached != null) return cached;

        SourceSnapshot current = new SourceSnapshot();
        MessageDigest aggregate = newSha256();
        if (Files.isRegularFile(source)) {
            addSourceFile(source, "$", previous, current, aggregate);
        } else {
            List<Path> files;
            try (Stream<Path> stream = Files.walk(source, FileVisitOption.FOLLOW_LINKS)) {
                files = stream.filter(Files::isRegularFile).sorted(Comparator.comparing(path -> source.relativize(path).toString())).toList();
            } catch (FileSystemLoopException e) {
                YesSteveModel.LOGGER.warn("Symlink loop while snapshotting: " + source, e);
                files = List.of();
            }
            Map<Path, SourceFileState> parallel;
            try {
                parallel = files.parallelStream().collect(java.util.stream.Collectors.toConcurrentMap(file -> file, file -> {
                    try {
                        String relative = source.relativize(file).toString().replace('\\', '/');
                        SourceFileState old = previous != null && previous.files != null ? previous.files.get(relative) : null;
                        SourceFileState state = new SourceFileState();
                        state.size = Files.size(file);
                        state.modified = Files.getLastModifiedTime(file).toMillis();
                        if (old != null && old.size == state.size && old.modified == state.modified && old.sha256 != null) {
                            state.sha256 = old.sha256;
                        } else {
                            state.sha256 = hashFile(file);
                        }
                        return state;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }));
            } catch (RuntimeException e) {
                if (e.getCause() instanceof IOException io) throw io;
                throw e;
            }
            for (Path file : files) {
                String relative = source.relativize(file).toString().replace('\\', '/');
                SourceFileState state = parallel.get(file);
                current.files.put(relative, state);
                aggregate.update(relative.getBytes(StandardCharsets.UTF_8));
                aggregate.update((byte) 0);
                aggregate.update(state.sha256.getBytes(StandardCharsets.US_ASCII));
                aggregate.update((byte) 0);
            }
        }
        current.fingerprint = HexFormat.of().formatHex(aggregate.digest());
        SourceSnapshot existing = snapshots.putIfAbsent(cacheKey, current);
        return existing != null ? existing : current;
    }

    private static void addSourceFile(Path file, String relative, @Nullable SourceSnapshot previous, SourceSnapshot current, MessageDigest aggregate) throws IOException {
        SourceFileState old = previous != null && previous.files != null ? previous.files.get(relative) : null;
        SourceFileState state = new SourceFileState();
        state.size = Files.size(file);
        state.modified = Files.getLastModifiedTime(file).toMillis();
        if (old != null && old.size == state.size && old.modified == state.modified && old.sha256 != null) {
            state.sha256 = old.sha256;
        } else {
            state.sha256 = hashFile(file);
        }
        current.files.put(relative, state);
        aggregate.update(relative.getBytes(StandardCharsets.UTF_8));
        aggregate.update((byte) 0);
        aggregate.update(state.sha256.getBytes(StandardCharsets.US_ASCII));
        aggregate.update((byte) 0);
    }

    private static String hashFile(Path file) throws IOException {
        MessageDigest digest = newSha256();
        byte[] buffer = new byte[65536];
        try (InputStream input = Files.newInputStream(file)) {
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Nullable
    private static ServerModelData restoreCachedModel(String modelId, @Nullable CachedModelMetadata cached, SourceSnapshot current, Path cacheDir, boolean isAuth, Set<String> validCaches) {
        if (cached == null || cached.source == null || cached.source.fingerprint == null || !cached.source.fingerprint.equals(current.fingerprint) || cached.auth != isAuth || cached.properties == null || cached.properties.sha256 == null || cached.properties.sha256.isEmpty()) return null;
        try {
            long[] hashes = YsmCrypt.calculateModelHashes(cached.properties.sha256, serverKey);
            String cacheFileName = String.format("%016x%016x", hashes[0], hashes[1]);
            Path cacheFile = cacheDir.resolve(cacheFileName);
            if (!Files.isRegularFile(cacheFile) || Files.size(cacheFile) != cached.cacheSize || Files.getLastModifiedTime(cacheFile).toMillis() != cached.cacheModified) return null;

            RawYsmModel raw = new RawYsmModel();
            raw.metadata = cached.metadata != null ? cached.metadata : new RawYsmModel.RawMetadata();
            raw.properties = cached.properties;
            raw.footer = cached.footer != null ? cached.footer : new RawYsmModel.RawFooter();
            if (cached.translations != null) {
                cached.translations.forEach((locale, values) -> raw.languageFiles.put(locale, new RawYsmModel.RawLanguageFile("", values)));
            }
            ServerModelInfo info = YSMClientMapper.buildModelInfo(raw, new MainModelInfo(cached.bones, cached.cubes, cached.faces));
            ServerAnimationInfo animations = new ServerAnimationInfo(cached.animations != null ? cached.animations : Collections.emptyMap(), cached.textures != null ? cached.textures : new String[0]);
            Object[] projectiles = cached.projectiles != null ? cached.projectiles : new String[0][];
            Object[] vehicles = cached.vehicles != null ? cached.vehicles : new String[0][];
            validCaches.add(cacheFileName);
            return new ServerModelData(modelId, animations, projectiles, vehicles, info, cached.customSkinModel, isAuth);
        } catch (Exception e) {
            YesSteveModel.LOGGER.warn("Failed to restore cached model metadata: " + modelId, e);
            return null;
        }
    }

    private static void registerModel(String key, String modelId, ServerModelData data, CachedModelMetadata metadata, SourceSnapshot current, Map<String, ServerModelData> loaded, Set<String> authIds, Map<String, CachedModelMetadata> nextCatalog, boolean isAuth) {
        metadata.source = current;
        loaded.put(modelId, data);
        nextCatalog.put(key, metadata);
        if (isAuth) authIds.add(modelId);
    }

    private static void registerParsedModel(String key, String modelId, @Nullable RawYsmModel raw, SourceSnapshot source, Path cacheDir, Map<String, ServerModelData> loaded, Set<String> authIds, Set<String> validCaches, Map<String, CachedModelMetadata> nextCatalog, boolean isAuth) {
        if (raw == null) return;
        ServerModelData data = processAndCacheModel(modelId, raw, cacheDir, isAuth, validCaches);
        if (data == null) return;
        loaded.put(modelId, data);
        if (isAuth) authIds.add(modelId);
        try {
            CachedModelMetadata metadata = createCachedMetadata(modelId, raw, source, cacheDir, isAuth);
            nextCatalog.put(key, metadata);
        } catch (IOException e) {
            YesSteveModel.LOGGER.warn("Failed to cache model metadata: " + modelId, e);
        }
    }

    private static CachedModelMetadata createCachedMetadata(String modelId, RawYsmModel raw, SourceSnapshot source, Path cacheDir, boolean isAuth) throws IOException {
        CachedModelMetadata cached = new CachedModelMetadata();
        cached.source = source;
        cached.auth = isAuth;
        cached.customSkinModel = "misc/2_steve".equals(modelId) || "misc/1_alex".equals(modelId);
        cached.metadata = copyMetadata(raw.metadata);
        cached.properties = copyProperties(raw.properties);
        cached.footer = copyFooter(raw.footer);

        if (raw.mainEntity.mainModel != null) {
            cached.bones = raw.mainEntity.mainModel.bones.size();
            for (RawYsmModel.RawBone bone : raw.mainEntity.mainModel.bones) {
                cached.cubes += bone.cubes.size();
                for (RawYsmModel.RawCube cube : bone.cubes) cached.faces += cube.faces.size();
            }
        }
        cached.animations = new LinkedHashMap<>();
        for (Map.Entry<String, RawYsmModel.RawAnimationFile> entry : raw.mainEntity.animationFiles.entrySet()) {
            cached.animations.put(entry.getKey(), entry.getValue().animations.keySet().toArray(new String[0]));
        }
        cached.translations = new LinkedHashMap<>();
        for (Map.Entry<String, RawYsmModel.RawLanguageFile> entry : raw.languageFiles.entrySet()) {
            cached.translations.put(entry.getKey(), new LinkedHashMap<>(entry.getValue().data));
        }
        cached.textures = raw.mainEntity.textures.keySet().toArray(new String[0]);
        cached.projectiles = raw.projectiles.stream().map(value -> value.matchIds != null ? value.matchIds : new String[0]).toArray(String[][]::new);
        cached.vehicles = raw.vehicles.stream().map(value -> value.matchIds != null ? value.matchIds : new String[0]).toArray(String[][]::new);

        long[] hashes = YsmCrypt.calculateModelHashes(raw.properties.sha256, serverKey);
        Path cacheFile = cacheDir.resolve(String.format("%016x%016x", hashes[0], hashes[1]));
        cached.cacheSize = Files.size(cacheFile);
        cached.cacheModified = Files.getLastModifiedTime(cacheFile).toMillis();
        return cached;
    }

    private static RawYsmModel.RawMetadata copyMetadata(RawYsmModel.RawMetadata source) {
        RawYsmModel.RawMetadata copy = new RawYsmModel.RawMetadata();
        copy.name = source.name;
        copy.tips = source.tips;
        copy.licenseType = source.licenseType;
        copy.licenseDescription = source.licenseDescription;
        copy.links = new LinkedHashMap<>(source.links);
        for (RawYsmModel.RawMetadata.Author author : source.authors) {
            RawYsmModel.RawMetadata.Author item = new RawYsmModel.RawMetadata.Author();
            item.name = author.name;
            item.role = author.role;
            item.comment = author.comment;
            item.contacts = new LinkedHashMap<>(author.contacts);
            item.avatar = author.avatar;
            copy.authors.add(item);
        }
        return copy;
    }

    private static RawYsmModel.RawProperties copyProperties(RawYsmModel.RawProperties source) {
        RawYsmModel.RawProperties copy = new RawYsmModel.RawProperties();
        copy.sha256 = source.sha256;
        copy.widthScale = source.widthScale;
        copy.heightScale = source.heightScale;
        copy.defaultTexture = source.defaultTexture;
        copy.previewAnimation = source.previewAnimation;
        copy.isFree = source.isFree;
        copy.renderLayersFirst = source.renderLayersFirst;
        copy.allCutout = source.allCutout;
        copy.disablePreviewRotation = source.disablePreviewRotation;
        copy.guiNoLighting = source.guiNoLighting;
        copy.mergeMultilineExpr = source.mergeMultilineExpr;
        copy.guiForeground = source.guiForeground;
        copy.guiBackground = source.guiBackground;
        copy.extraAnimations = new LinkedHashMap<>(source.extraAnimations);
        copy.extraAnimationClassifies = new ArrayList<>(source.extraAnimationClassifies);
        copy.extraAnimationButtons = new ArrayList<>(source.extraAnimationButtons);
        return copy;
    }

    private static RawYsmModel.RawFooter copyFooter(RawYsmModel.RawFooter source) {
        RawYsmModel.RawFooter copy = new RawYsmModel.RawFooter();
        copy.version = source.version;
        copy.unkInt1 = source.unkInt1;
        copy.rand = source.rand;
        copy.time = source.time;
        copy.extra = source.extra;
        copy.unkInt2 = source.unkInt2;
        return copy;
    }

    private static void scanDirectoryPacks(Path searchRoot, String prefix) {
        if (searchRoot == null || !Files.isDirectory(searchRoot)) return;
        try (var stream = Files.walk(searchRoot, 1, FileVisitOption.FOLLOW_LINKS)) {
            stream.filter(Files::isDirectory).forEach(path -> {
                if (path.equals(searchRoot)) return;
                Path packJson = path.resolve("ysm-pack.json");
                if (Files.exists(packJson)) {
                    try {
                        ServerPackData packData = new ServerPackData();
                        String rel = searchRoot.relativize(path).toString().replace('\\', '/');
                        packData.folderPath = prefix + rel + (rel.endsWith("/") ? "" : "/");

                        String jsonStr = Files.readString(packJson, StandardCharsets.UTF_8);
                        JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();
                        if (json.has("name")) packData.name = json.get("name").getAsString();
                        if (json.has("description")) packData.description = json.get("description").getAsString();

                        if (json.has("lang") && json.get("lang").isJsonObject()) {
                            packData.lang = new HashMap<>();
                            JsonObject langObj = json.getAsJsonObject("lang");
                            for (Map.Entry<String, JsonElement> entry : langObj.entrySet()) {
                                if (entry.getValue().isJsonObject()) {
                                    Map<String, String> translations = new HashMap<>();
                                    for (Map.Entry<String, JsonElement> transEntry : entry.getValue().getAsJsonObject().entrySet()) {
                                        translations.put(transEntry.getKey(), transEntry.getValue().getAsString());
                                    }
                                    packData.lang.put(entry.getKey(), translations);
                                }
                            }
                        }

                        Path packPng = path.resolve("ysm-pack.png");
                        if (Files.exists(packPng)) {
                            byte[] data = Files.readAllBytes(packPng);
                            int[] dims = getPngDimensions(data);
                            packData.iconData = data;
                            packData.iconWidth = dims[0];
                            packData.iconHeight = dims[1];
                            packData.iconFormat = 2; // 2=PNG
                        }
                        packs.put(packData.folderPath, packData);
                    } catch (Exception e) {
                        YesSteveModel.LOGGER.error("Failed to load pack metadata: " + packJson, e);
                    }
                }
            });
        } catch (Exception e) {
            YesSteveModel.LOGGER.error("Failed to walk directory for packs: " + searchRoot, e);
        }
    }

    private static int[] getPngDimensions(byte[] data) {
        if (data == null || data.length < 24) return new int[]{0, 0};
        if ((data[0] & 0xFF) != 0x89 || data[1] != 0x50 || data[2] != 0x4E || data[3] != 0x47) return new int[]{0, 0};
        int width = ((data[16] & 0xFF) << 24) | ((data[17] & 0xFF) << 16) | ((data[18] & 0xFF) << 8) | (data[19] & 0xFF);
        int height = ((data[20] & 0xFF) << 24) | ((data[21] & 0xFF) << 16) | ((data[22] & 0xFF) << 8) | (data[23] & 0xFF);
        return new int[]{width, height};
    }

    private static ServerModelData processAndCacheModel(String modelId, RawYsmModel model, Path serverCacheDir, boolean isAuth, Set<String> validCacheFiles) {
        String sha256 = model.properties.sha256;
        if (sha256 == null || sha256.isEmpty()) return null;

        try {
            long[] hashes = YsmCrypt.calculateModelHashes(sha256, serverKey);
            String cacheFileName = String.format("%016x%016x", hashes[0], hashes[1]);
            Path cacheFile = serverCacheDir.resolve(cacheFileName);
            if (!serverCacheDir.toFile().isDirectory()) {
                Files.createDirectories(serverCacheDir);
            }
            boolean needsUpdate = true;
            if (Files.exists(cacheFile)) {
                byte[] existingData = Files.readAllBytes(cacheFile);
                if (YsmCrypt.verifyServerCache(existingData, hashes[0], hashes[1])) {
                    needsUpdate = false;
                }
            }
            if (needsUpdate) {
                byte[] encryptedCache;
                try (YSMByteBuf serialized = YSMBinarySerializer.serialize(model, 32, true)) {
                    io.netty.buffer.ByteBuf raw = serialized.getRawBuf();
                    if (raw.hasArray()) {
                        int off = raw.arrayOffset() + raw.readerIndex();
                        int len = raw.readableBytes();
                        encryptedCache = YsmCrypt.encryptServerCache(raw.array(), off, len, serverKey, hashes[0], hashes[1]);
                    } else {
                        encryptedCache = YsmCrypt.encryptServerCache(serialized.toArray(), serverKey, hashes[0], hashes[1]);
                    }
                }
                Files.write(cacheFile, encryptedCache);
            }
            validCacheFiles.add(cacheFileName);

            boolean isCustomSkinModel = "misc/2_steve".equals(modelId) || "misc/1_alex".equals(modelId); // 对没错就是写死的

            return mapToDataClass(modelId, model, isAuth, isCustomSkinModel);
        } catch (Exception e) {
            YesSteveModel.LOGGER.error("Failed to process and cache model: " + modelId, e);
            return null;
        }
    }


    private static ServerModelData mapToDataClass(String modelId, RawYsmModel raw, boolean isAuth, boolean isCustomSkinModel) {
        ServerModelInfo serverModelInfo = YSMClientMapper.buildModelInfo(raw);
        // Animations
        Map<String, String[]> animMap = new HashMap<>();
        for (Map.Entry<String, RawYsmModel.RawAnimationFile> e : raw.mainEntity.animationFiles.entrySet()) {
            animMap.put(e.getKey(), e.getValue().animations.keySet().toArray(new String[0]));
        }
        String[] texArr = raw.mainEntity.textures.keySet().toArray(new String[0]);
        ServerAnimationInfo animInfo = new ServerAnimationInfo(animMap, texArr);

        // Sub Entities
        Object[] projectiles = raw.projectiles.stream().map(v -> v.matchIds != null ? v.matchIds : new String[0]).toArray();
        Object[] vehicles = raw.vehicles.stream().map(v -> v.matchIds != null ? v.matchIds : new String[0]).toArray();
        return new ServerModelData(modelId, animInfo, projectiles, vehicles, serverModelInfo, isCustomSkinModel, isAuth);
    }

    public static void nativeSyncModels(UUID[] uuids, String[] playerNames, String[] modelIds, Object callback) {
        initRateLimit();
        YSMThreadPool.submitSync(() -> {
            try {
                MinecraftServer currentServer = GameInstance.getServer();
                if (currentServer == null) return;

                for (UUID uuid : uuids) {
                    PlayerSyncState state = new PlayerSyncState();
                    syncStates.put(uuid, state);
                    state.allowedModels.clear();
                    if (!shouldHideModelsFrom(uuid)) {
                        state.allowedModels.addAll(CACHE_NAME_INFO.values());
                    }
                    state.step = 1;

                    // HandshakePing
//                    byte[] garbage = new byte[16 + SECURE_RANDOM_S.nextInt(48)];
//                    SECURE_RANDOM_S.nextBytes(garbage);
//                    byte[] payload = new byte[2 + garbage.length + 1];
//                    payload[0] = (byte)(garbage.length & 0xFF);
//                    payload[1] = (byte)((garbage.length >> 8) & 0xFF);
//                    System.arraycopy(garbage, 0, payload, 2, garbage.length);
//                    payload[2 + garbage.length] = 0x01;
//
//                    var result = YsmCrypt.encrypt(payload, K0_SERVER, true);
//                    state.key1 = result.nextKey();
//
//                    sendModelData(uuid, ByteBuffer.wrap(result.data()), new PendingTransfer());
                    int garbageLen = 16 + theRandom.nextInt(48);
                    byte[] garbage = new byte[garbageLen];
                    theRandom.nextBytes(garbage);

                    try (YSMByteBuf outBuf = new YSMByteBuf(Unpooled.buffer())) {
                        outBuf.writeGarbageHeader(garbageLen, garbage);
                        outBuf.writeByte((byte) 0x01);
                        YsmCrypt.EncryptedPacket result = YsmCrypt.encrypt(outBuf.toArray(), YsmCrypt.publicKey, true);
                        state.key1 = result.nextKey();

                        sendModelData(uuid, ByteBuffer.wrap(result.data()), new PendingTransfer());
                    }
                }
//                if (callback != null) onAuthDataReceived(null, callback);
            } catch (Exception e) {
                YesSteveModel.LOGGER.error("[YSM] Sync initiation failed", e);
            }
        });
    }

    public static boolean isClientOnlyHost() {
        if (PlatformAPI.isServer()) {
            return false;
        }
        return ClientOnlyHostBridge.isActive();
    }

    private static boolean shouldHideModelsFrom(UUID uuid) {
        return isClientOnlyHost() && !ClientOnlyHostBridge.isLocalHost(uuid);
    }

    private static void sendPacket03(UUID uuid, PlayerSyncState state) {
        int garbageLen = 16 + theRandom.nextInt(48);
        byte[] garbage = new byte[garbageLen];
        theRandom.nextBytes(garbage);

        try (YSMByteBuf outBuf = new YSMByteBuf(Unpooled.buffer())) {
            outBuf.writeGarbageHeader(garbageLen, garbage);

            outBuf.writeVarInt(3); // Type
            outBuf.writeVarLong(0L); // 這個決定了cache資料夾的名稱

            outBuf.getRawBuf().writeBytes(serverKey);
            outBuf.getRawBuf().writeBytes(state.clientKey);

            outBuf.writeVarInt(state.allowedModels.size());
            for (ServerModelData model : state.allowedModels) {
                String sha256 = model.getLoadedModelData().getModelHash();
                long[] hashes = YsmCrypt.calculateModelHashes(sha256, serverKey);
                outBuf.writeVarLong(hashes[0]);
                outBuf.writeVarLong(hashes[1]);
                outBuf.writeString(model.getModelId());
                outBuf.writeVarInt(model.isAuth() ? 1 : 0);
                outBuf.writeVarInt(model.isCustomSkinModel() ? 1 : 0);
                outBuf.writeVarInt(32); // format
            }

            Collection<ServerPackData> visiblePacks = shouldHideModelsFrom(uuid) ? List.of() : packs.values();
            outBuf.writeVarInt(visiblePacks.size());
            for (ServerPackData pack : visiblePacks) {
                outBuf.writeString(pack.folderPath);

                // 寫入圖標資訊
                if (pack.iconData != null) {
                    outBuf.writeVarInt(1);
                    outBuf.writeByteArray(pack.iconData);
                    outBuf.writeVarInt(pack.iconWidth);
                    outBuf.writeVarInt(pack.iconHeight);
                    outBuf.writeVarInt(pack.iconFormat);
                    outBuf.writeVarInt(1); // unkImageData
                } else {
                    outBuf.writeVarInt(0);
                }

                // 寫入基礎資訊
                if (pack.name != null || pack.description != null) {
                    outBuf.writeVarInt(1);
                    outBuf.writeString(pack.name != null ? pack.name : "");
                    outBuf.writeString(pack.description != null ? pack.description : "");
                } else {
                    outBuf.writeVarInt(0);
                }

                // 寫入語言本地化
                if (pack.lang != null && !pack.lang.isEmpty()) {
                    outBuf.writeVarInt(pack.lang.size());
                    for (Map.Entry<String, Map<String, String>> langEntry : pack.lang.entrySet()) {
                        outBuf.writeString(langEntry.getKey());
                        outBuf.writeVarInt(langEntry.getValue().size());
                        for (Map.Entry<String, String> kv : langEntry.getValue().entrySet()) {
                            outBuf.writeString(kv.getKey());
                            outBuf.writeString(kv.getValue());
                        }
                    }
                } else {
                    outBuf.writeVarInt(0);
                }
            }

            outBuf.writeVarInt(0);  // \0

            YsmCrypt.EncryptedPacket result = YsmCrypt.encrypt(outBuf.toArray(), state.clientNextKey, false);
            sendModelData(uuid, ByteBuffer.wrap(result.data()), new PendingTransfer());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void sendPacket05(UUID uuid, PlayerSyncState state, List<long[]> requestedHashes) {
        if (shouldHideModelsFrom(uuid)) return;
        YSMThreadPool.submitSync(() -> {
            boolean permitAcquired = false;
            try {
                threadLimiter.acquire();
                permitAcquired = true;

                PendingTransfer transfer = new PendingTransfer();

                for (long[] hashes : requestedHashes) {
                    long hash1 = hashes[0];
                    long hash2 = hashes[1];
                    String fileName = String.format("%016x%016x", hash1, hash2);
                    Path file = ServerModelManager.CACHE_SERVER.resolve(fileName);

                    if (!Files.exists(file)) continue;

                    byte[] fileData = Files.readAllBytes(file);
                    int totalSize = fileData.length;
                    int maxChunkSize = 30720;
                    int chunkCount = (totalSize + maxChunkSize - 1) / maxChunkSize;
                    int chunkSize = (totalSize + chunkCount - 1) / chunkCount;

                    int offset = 0;

                    while (offset < totalSize) {
                        if (syncStates.get(uuid) != state) {
                            return;
                        }
                        int length = Math.min(chunkSize, totalSize - offset);

                        int garbageLen = 16 + theRandom.nextInt(48);
                        byte[] garbage = new byte[garbageLen];
                        theRandom.nextBytes(garbage);

                        try (YSMByteBuf outBuf = new YSMByteBuf(Unpooled.buffer())) {
                            outBuf.writeGarbageHeader(garbageLen, garbage);
                            outBuf.writeVarInt(5); // Type
                            outBuf.writeVarLong(hash1);
                            outBuf.writeVarLong(hash2);
                            outBuf.writeVarInt(totalSize);
                            outBuf.writeVarInt(offset);
                            outBuf.writeVarInt(length);
                            outBuf.getRawBuf().writeBytes(fileData, offset, length);
                            YsmCrypt.EncryptedPacket result = YsmCrypt.encrypt(outBuf.toArray(), state.key1, false);

//                            bandwidthLimiter.acquire(result.data().length); //TODO


                            // Stream chunks
                            boolean success = sendModelData(uuid, ByteBuffer.wrap(result.data()), transfer);
                            if (success) {
                                offset += length;
                            } else {
                                return;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                YesSteveModel.LOGGER.error("Failed to send model chunks to " + uuid, e);
            } finally {
                if (permitAcquired) {
                    threadLimiter.release();
                }
            }
        });
    }

    public static void nativeExportModel(String modelID, @Nullable String extra, @Nullable Consumer<ExportResult> callback) {
        YSMThreadPool.submit(() -> {
            try {
                ServerModelData modelData = CACHE_NAME_INFO.get(modelID);
                if (modelData == null) {
                    if (callback != null) {
                        callback.accept(new ExportResult(false, (Component) YSMNativeHelper.createTranslatableComponent("commands.yes_steve_model.export.failure",new Object[]{": " + modelID + "\n Model not found"}), "", "", 0));
                    }
                    return;
                }

                String sha256 = modelData.getLoadedModelData().getModelHash();
                long[] hashes = YsmCrypt.calculateModelHashes(sha256, serverKey);
                String cacheFileName = String.format("%016x%016x", hashes[0], hashes[1]);
                Path cacheFile = CACHE_SERVER.resolve(cacheFileName);

                if (!Files.exists(cacheFile)) {
                    if (callback != null) {
                        callback.accept(new ExportResult(false, Component.literal("Cache file missing for: " + modelID), "", "", 0));
                    }
                    return;
                }

                byte[] cacheData = Files.readAllBytes(cacheFile);
                CachePayload cachePayload = YsmCrypt.readInPlace(cacheData, serverKey);

                int coreDataLength;
                try (YSMBinaryDeserializer deserializer = new YSMBinaryDeserializer(cachePayload.data(), cachePayload.formatVersion())) {
                    deserializer.deserializeKeepOpen();
                    coreDataLength = deserializer.getReader().getOffset();
                }

                try (YSMByteBuf outBuf = new YSMByteBuf(Unpooled.buffer())) {
                    outBuf.writeDword(cachePayload.formatVersion());
                    outBuf.getRawBuf().writeBytes(cachePayload.data(), 0, coreDataLength);
                    outBuf.writeVarInt(cachePayload.formatVersion()); // version
                    outBuf.writeVarInt(1);
                    byte[] randBytes = new byte[8];
                    theRandom.nextBytes(randBytes);
                    StringBuilder sb = new StringBuilder(16);
                    for (byte b : randBytes) {
                        sb.append(String.format("%02x", b));
                    }
                    outBuf.writeString(sb.toString());
                    outBuf.writeVarLong(java.time.Instant.now().getEpochSecond());
                    outBuf.writeString(extra != null ? extra : "");
                    outBuf.writeVarInt(0);
                    byte[] rawBytes = new byte[outBuf.getRawBuf().readableBytes()];
                    outBuf.getRawBuf().readBytes(rawBytes);
                    byte[] finalEncrypted = YsmCrypt.encryptYsmFile(rawBytes);
                    Path exportPath = EXPORT.resolve(modelID + ".ysm");
                    Files.createDirectories(exportPath.getParent());
                    Files.write(exportPath, finalEncrypted);
                    if (callback != null) {
                        String displayPath = Paths.get("export", modelID + ".ysm").toString();
                        callback.accept(new ExportResult(true, null, displayPath, "", 0));
                    }
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.accept(new ExportResult(false, Component.literal("Export failed: " + e.getMessage()), "", "", 0));
                }
            }
        });
    }

    public static Optional<ServerModelData> getModelDefinition(String str) {
        return Optional.ofNullable(CACHE_NAME_INFO.get(str));
    }

    public static Map<String, ServerModelData> getServerModelInfo() {
        return CACHE_NAME_INFO;
    }

    public static Set<String> getAuthModels() {
        return AUTH_MODELS;
    }

    public static boolean canReuseLoadedModels() {
        if (!initialized || loadedSourceState == null) return false;
        try {
            return loadedSourceState.equals(computeModelSourceState());
        } catch (IOException e) {
            return false;
        }
    }

    private static String computeModelSourceState() throws IOException {
        MessageDigest digest = newSha256();
        addSourceTreeState(BUILT, "built", digest);
        addSourceTreeState(CUSTOM, "custom", digest);
        addSourceTreeState(AUTH, "auth", digest);
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void addSourceTreeState(Path root, String group, MessageDigest digest) throws IOException {
        if (!Files.isDirectory(root)) return;
        try (Stream<Path> stream = Files.walk(root, FileVisitOption.FOLLOW_LINKS)) {
            for (Path file : stream.filter(Files::isRegularFile).sorted(Comparator.comparing(path -> root.relativize(path).toString())).toList()) {
                String relative = group + '/' + root.relativize(file).toString().replace('\\', '/');
                digest.update(relative.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(Long.toString(Files.size(file)).getBytes(StandardCharsets.US_ASCII));
                digest.update((byte) 0);
                digest.update(Long.toString(Files.getLastModifiedTime(file).toMillis()).getBytes(StandardCharsets.US_ASCII));
                digest.update((byte) 0);
            }
        }
    }

    public static void requestPlayerAuth(ServerPlayer serverPlayer, @Nullable Consumer<UUIDComponentData> consumer) {
        MinecraftServer currentServer = GameInstance.getServer();
        currentServer.execute(() -> {
            List<ServerPlayer> players = currentServer.getPlayerList().getPlayers();
            ArrayList<FloatReferencePair<ServerPlayer>> arrayList = new ArrayList<>();
            for (ServerPlayer serverPlayer2 : players) {
                if (serverPlayer2.level().dimensionType() == serverPlayer.level().dimensionType()) {
                    arrayList.add(FloatReferencePair.of(serverPlayer2.distanceTo(serverPlayer), serverPlayer2));
                }
            }
            arrayList.sort((a, b) -> Float.compare(a.firstFloat(), b.firstFloat()));
            nativeSyncModels(new UUID[]{serverPlayer.getUUID()}, new String[]{serverPlayer.getGameProfile().getName()}, collectPlayerModelIds(arrayList.stream().map(it.unimi.dsi.fastutil.Pair::second).toList()), consumer);
        });
    }

    public static boolean loadModels(@Nullable Consumer<ModelLoadResult> consumer, @Nullable Consumer<UUIDComponentData> consumer2) {
        Consumer<ModelLoadResult> action = modelLoadResult -> {
            if (consumer != null) {
                consumer.accept(modelLoadResult);
            }
            MinecraftServer currentServer = GameInstance.getServer();
            if (currentServer == null) {
                return;
            }
            currentServer.execute(() -> {
                List<ServerPlayer> players = currentServer.getPlayerList().getPlayers();
                for (ServerPlayer value : players) {
                    validatePlayerModel(value);
                }
                nativeSyncModels(players.stream().filter(NetworkHandler::isPlayerConnected).map((player) -> player.getUUID()).toArray(i -> new UUID[i]), players.stream().filter(NetworkHandler::isPlayerConnected).map(serverPlayer -> serverPlayer.getGameProfile().getName()).toArray(i2 -> new String[i2]), collectPlayerModelIds(players), consumer2);
            });
        };
        return nativeLoadModels(action);
    }

    private static String[] collectPlayerModelIds(Collection<ServerPlayer> collection) {
        return collection.stream().filter(NetworkHandler::isPlayerConnected).map(serverPlayer -> ModelInfoCapability.get(serverPlayer).map(ModelInfoCapability::getModelId)).filter(Optional::isPresent).map(Optional::get).distinct().toArray(String[]::new);
    }

    private static void onModelLoadComplete(ModelLoadResult modelLoadResult, @Nullable Object obj) {
        Consumer<ModelLoadResult> consumer = (Consumer<ModelLoadResult>) obj;
        MinecraftServer currentServer = GameInstance.getServer();
        if (modelLoadResult.isSuccess()) {
            IntOpenHashSet hashes = new IntOpenHashSet(modelLoadResult.getModelDefinitions().size());
            for (ServerModelData data : modelLoadResult.getModelDefinitions().values()) {
                hashes.add(data.getLoadedModelData().getHashId());
            }
            modelHashSet = hashes;
        }
        if (currentServer != null) {
            currentServer.execute(() -> {
                if (modelLoadResult.isSuccess()) {
                    CACHE_NAME_INFO = modelLoadResult.getModelDefinitions();
                    AUTH_MODELS = modelLoadResult.getAuthModelIds();
                }
                initialized = modelLoadResult.isSuccess();
                if (consumer != null) {
                    YSMThreadPool.submit(() -> consumer.accept(modelLoadResult));
                }
            });
            return;
        }
        if (modelLoadResult.isSuccess()) {
            CACHE_NAME_INFO = modelLoadResult.getModelDefinitions();
            AUTH_MODELS = modelLoadResult.getAuthModelIds();
        }
        initialized = modelLoadResult.isSuccess();
        if (consumer != null) {
            consumer.accept(modelLoadResult);
        }
    }

    public static void syncModelToPlayer(UUID uuid) {
        nativeSendModelData(uuid, null);
    }

    private static Connection getPlayerConnection(UUID uuid) {
        ServerPlayer player;
        MinecraftServer currentServer = GameInstance.getServer();
        if (currentServer == null || (player = currentServer.getPlayerList().getPlayer(uuid)) == null) {
            return null;
        }
        ServerGamePacketListenerImpl serverGamePacketListenerImpl = player.connection;
        if (!serverGamePacketListenerImpl.isAcceptingMessages() || !serverGamePacketListenerImpl.getClass().equals(ServerGamePacketListenerImpl.class)) {
            return null;
        }
        return ((ServerCommonPacketListenerImplAccessor) serverGamePacketListenerImpl).ysm$getConnection();
    }

    private static boolean sendModelData(UUID uuid, ByteBuffer byteBuffer, PendingTransfer pendingTransfer) {
        Connection connection = getPlayerConnection(uuid);
        if (connection != null) {
            for (Packet<?> packet : NetworkHandler.toClientboundPackets(new S2CModelSyncPayload(byteBuffer), uuid)) {
                if (!sendPacketReliably(connection, packet, pendingTransfer)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static Object createModelPacket(ByteBuffer byteBuffer) {
        return NetworkHandler.toClientboundPacket(new S2CModelSyncPayload(byteBuffer));
    }

    private static boolean sendPacketToPlayer(UUID uuid, Object obj, PendingTransfer pendingTransfer) {
        Connection connection = getPlayerConnection(uuid);
        if (connection != null) {
            return sendPacketReliably(connection, obj, pendingTransfer);
        }
        return false;
    }

    private static boolean sendPacketReliably(Connection connection, Object obj, PendingTransfer pendingTransfer) {
        io.netty.channel.Channel channel = ((ConnectionAccessor) connection).ysm$getChannel();
        if (channel.eventLoop().inEventLoop()) {
            YesSteveModel.LOGGER.error("[YSM] Refusing to wait for a packet from its Netty event loop");
            return false;
        }

        if (!pendingTransfer.hasStarted) {
            pendingTransfer.hasStarted = true;
            pendingTransfer.pendingBytes = channel.unsafe().outboundBuffer().totalPendingWriteBytes() + 65536;
        }

        final AtomicInteger atomicInteger = new AtomicInteger(0);
        while (connection.isConnected() && channel.isActive()) {
            if (channel.unsafe().outboundBuffer().totalPendingWriteBytes() > pendingTransfer.pendingBytes) {
                if (!YSMThreadPool.awaitTermination(10)) {
                    return false;
                }
            } else {
                try {
                    connection.send((Packet<?>) obj, new PacketSendListener() {
                        public void onSuccess() {
                            atomicInteger.set(1);
                            PacketSendListener.super.onSuccess();
                        }

                        @Nullable
                        public Packet<?> onFailure() {
                            atomicInteger.set(-1);
                            return null;
                        }
                    });
                    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
                    while (atomicInteger.get() == 0
                            && connection.isConnected()
                            && channel.isActive()
                            && System.nanoTime() < deadline) {
                        if (!YSMThreadPool.awaitTermination(5)) {
                            return false;
                        }
                    }
                    if (atomicInteger.get() == 1) {
                        return true;
                    }
                    if (atomicInteger.get() == 0 && connection.isConnected() && channel.isActive()) {
                        YesSteveModel.LOGGER.warn("[YSM] Timed out waiting for a model packet to be sent");
                    }
                    return false;
                } catch (Throwable th) {
                    th.printStackTrace();
                    return false;
                }
            }
        }
        return false;
    }

    public static Pair<String, String> getDefaultModelConfig() {
        String defaultModelId = ServerConfig.DEFAULT_MODEL_ID.get();
        String defaultTexture = ServerConfig.DEFAULT_MODEL_TEXTURE.get();
        if (defaultTexture.toLowerCase().endsWith(".png") && defaultTexture.length() > 4) {
            defaultTexture = defaultTexture.substring(0, defaultTexture.length() - 4);
        }
        if (!initialized) {
            return Pair.of(defaultModelId, defaultTexture);
        }
        ServerModelData modelData = CACHE_NAME_INFO.get(defaultModelId);
        if (modelData == null) {
            return Pair.of("default", "default");
        }
        if (!modelData.getModelInfo().getTextures().contains(defaultTexture)) {
            if (modelData.getModelInfo().getTextures().contains(modelData.getLoadedModelData().getModelProperties().getDefaultTexture())) {
                defaultTexture = modelData.getLoadedModelData().getModelProperties().getDefaultTexture();
            } else {
                defaultTexture = modelData.getModelInfo().getTextures().get(0);
            }
        }
        return Pair.of(defaultModelId, defaultTexture);
    }

    private static void onAuthDataReceived(UUIDComponentData uuidComponentData, @Nullable Object obj) {
        Consumer consumer = (Consumer) obj;
        if (consumer != null) {
            consumer.accept(uuidComponentData);
        }
    }

    public static void validatePlayerModel(ServerPlayer serverPlayer) {
        if (!CACHE_NAME_INFO.isEmpty()) {
            ModelInfoCapability.get(serverPlayer).ifPresent(modelInfoCap -> {
                AuthModelsCapability.get(serverPlayer).ifPresent(authModelsCap -> {
                    if (authModelsCap.getAuthModels().removeIf(str -> !CACHE_NAME_INFO.containsKey(str))) {
                        NetworkHandler.sendToClientPlayer(new S2CSyncAuthModelsPacket(authModelsCap.getAuthModels()), serverPlayer);
                    }
                    String modelId = modelInfoCap.getModelId();
                    if (!getServerModelInfo().containsKey(modelId) || ((AUTH_MODELS.contains(modelId) && !authModelsCap.containsModel(modelInfoCap.getModelId())) || !CACHE_NAME_INFO.get(modelId).getModelInfo().getTextures().contains(modelInfoCap.getSelectTexture()))) {
                        modelInfoCap.resetToDefault();
                    }
                    modelInfoCap.retainAnimationKeys(modelHashSet);
                });
            });
        }
    }

    public static Map<String, ServerPackData> getPacks() {
        return packs;
    }

    private static class PendingTransfer {
        public long pendingBytes;

        public boolean hasStarted = false;

        private PendingTransfer() {
        }
    }
}
