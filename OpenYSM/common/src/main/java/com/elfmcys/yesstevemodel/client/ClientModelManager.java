package com.elfmcys.yesstevemodel.client;

import com.elfmcys.yesstevemodel.NativeLibLoader;
import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.client.gui.IGuiWidget;
import com.elfmcys.yesstevemodel.client.gui.metadata.ModelDisplayAssets;
import com.elfmcys.yesstevemodel.client.model.*;
import com.elfmcys.yesstevemodel.client.texture.OuterFileTexture;
import com.elfmcys.yesstevemodel.client.upload.IResourceLocatable;
import com.elfmcys.yesstevemodel.client.upload.UploadManager;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.model.format.ServerModelData;
import com.elfmcys.yesstevemodel.model.format.ServerModelInfo;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.C2SModelSyncPayload;
import com.elfmcys.yesstevemodel.resource.YSMBinaryDeserializer;
import com.elfmcys.yesstevemodel.resource.YSMClientMapper;
import com.elfmcys.yesstevemodel.resource.YSMFolderDeserializer;
import com.elfmcys.yesstevemodel.resource.models.ModelPackData;
import com.elfmcys.yesstevemodel.resource.pojo.RawYsmModel;
import com.elfmcys.yesstevemodel.util.FileTypeUtil;
import com.elfmcys.yesstevemodel.util.YSMThreadPool;
import com.elfmcys.yesstevemodel.util.data.OrderedStringMap;
import com.mojang.blaze3d.systems.RenderSystem;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.message.StringFormattedMessage;
import org.jetbrains.annotations.Nullable;
import rip.ysm.security.YSMByteBuf;
import rip.ysm.security.YSMClientCache;
import rip.ysm.security.YsmCrypt;
import rip.ysm.security.YsmCrypt.CachePayload;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class ClientModelManager {
    private static int syncStep = 1;
    private static byte[] key1;
    private static byte[] lastKey;
    private static byte[] serverKey;
    private static byte[] clientKey;
    private static String currentCacheFolderName;
    private static final AtomicInteger pendingModelsCount = new AtomicInteger(0);

    private static final ThreadPoolExecutor modelPhraseExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
            r -> {
                Thread t = new Thread(r, "YSM-Model-Parse-Thread");
                t.setDaemon(true);
                return t;
            }
    );
    private static final ThreadPoolExecutor modelPrepareExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
            r -> {
                Thread t = new Thread(r, "YSM-Model-Prepare-Thread");
                t.setDaemon(true);
                return t;
            }
    );
    private static final Semaphore preparedModelSlots = new Semaphore(2);
    private static final AtomicInteger modelLoadGeneration = new AtomicInteger();

    private static final Map<UUID, ServerModelContext> serverModels = new ConcurrentHashMap<>();

    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();
    private static volatile ModelAssembly localModelContext;
    private static volatile Runnable pendingModelCallback;
    private static IResourceLocatable defaultTexture;
    private static volatile Connection serverConnection;

    private static volatile Map<String, ModelAssembly> modelAssemblyMap = Object2ReferenceMaps.emptyMap();
    private static volatile Map<String, ModelPackData> modelPackMap = new Object2ReferenceOpenHashMap<>();
    private static final int MAX_LOADED_LOCAL_MODELS = 64;
    private static final Map<String, LazyModelSource> lazyModelSources = new ConcurrentHashMap<>();
    private static final Set<String> localOnlyModelIds = ConcurrentHashMap.newKeySet();
    private static final Set<String> loadingLazyModels = ConcurrentHashMap.newKeySet();
    private static final LinkedHashMap<String, Boolean> loadedLocalModelAccess = new LinkedHashMap<>(64, 0.75f, true);
    private static final Object loadedLocalModelAccessLock = new Object();
    private static Boolean lastLazyModelLoading;

    private static final ConcurrentLinkedQueue<Pair<ModelAssembly, String>> pendingModelQueue = new ConcurrentLinkedQueue<>();
    private static final WeakHashMap<IGuiWidget, Object> guiWidgets = new WeakHashMap<>();
    private static final SyncStatus syncState = new SyncStatus();
    private static boolean isOysmServer = false;
    private static boolean allowUpload = false;

    public enum SyncState {
        WAITING, LOADING, IDLE, PREPARING, SYNCING
    }

    public static class ServerModelContext {
        public final UUID uuid;
        public final long hash1;
        public final long hash2;
        public final String modelId;
        public final boolean isAuth;
        public final int isCustomSkinModel;
        public final int version;

        public Path downloadFile;
        public int totalSize;
        public int bytesReceived;
        public boolean downloadComplete;
        public final int generation;

        public ServerModelContext(long hash1, long hash2, String modelId, boolean isAuth, int isCustomSkinModel, int version, int generation) {
            this.uuid = new UUID(hash1, hash2);
            this.hash1 = hash1;
            this.hash2 = hash2;
            this.modelId = modelId;
            this.isAuth = isAuth;
            this.isCustomSkinModel = isCustomSkinModel;
            this.version = version;
            this.generation = generation;
        }
    }

    public static void loadDefaultModel() {
        YesSteveModel.LOGGER.info("[YSM] Loading builtin default model...");

        GeoModel.initSIMD();

        try {
            String resourcePath = "/assets/yes_steve_model/builtin/default";
            URL resourceUrl = YesSteveModel.class.getResource(resourcePath);
            if (resourceUrl == null) {
                YesSteveModel.LOGGER.error("[YSM] Builtin default model not found in classpath: " + resourcePath);
                return;
            }
            URI uri = resourceUrl.toURI();
            Path defaultPath;
            FileSystem jarFs = null;
            if ("jar".equals(uri.getScheme())) {
                try {
                    jarFs = FileSystems.getFileSystem(uri);
                } catch (FileSystemNotFoundException e) {
                    jarFs = FileSystems.newFileSystem(uri, Collections.emptyMap());
                }
                defaultPath = jarFs.getPath(resourcePath);
            } else {
                defaultPath = Paths.get(uri);
            }

            try (YSMFolderDeserializer deserializer = new YSMFolderDeserializer(defaultPath)) {
                RawYsmModel rawModel = deserializer.deserialize();

                ClientModelInfo parsedBundle = YSMClientMapper.buildParsedBundle(rawModel, "default");


                onModelDataReceived(parsedBundle, "default", true, false);
                YesSteveModel.LOGGER.info("[YSM] Successfully pushed Default Model to render queue.");
            } catch (Exception e) {
                YesSteveModel.LOGGER.error("[YSM] Failed to dispatch Default Model", e);
            }

        } catch (Exception e) {
            YesSteveModel.LOGGER.error("[YSM] Failed to load builtin default model", e);
        }
    }

    private static void registerLocalModelCatalog() {
        if (!isLazyModelLoadingEnabled() && !ClientOnlyMode.isActive()) return;
        YesSteveModel.LOGGER.info("[YSM] Registering local model catalog for lazy loading...");
        Map<String, ServerModelData> serverModelInfo = ServerModelManager.getServerModelInfo();
        if (serverModelInfo != null && !serverModelInfo.isEmpty()) {
            runPendingModelCallback();
            for (Map.Entry<String, ServerModelData> entry : serverModelInfo.entrySet()) {
                String modelId = entry.getKey();
                if ("default".equals(modelId)) continue;

                ServerModelData modelData = entry.getValue();
                boolean isAuth = modelData.isAuth();

                try {
                    String sha256 = modelData.getLoadedModelData().getModelHash();
                    long[] hashes = YsmCrypt.calculateModelHashes(sha256, ServerModelManager.serverKey);
                    String cacheFileName = String.format("%016x%016x", hashes[0], hashes[1]);
                    Path cacheFile = ServerModelManager.CACHE_SERVER.resolve(cacheFileName);

                    if (Files.exists(cacheFile)) {
                        LazyModelSource source = lazyModelSources.computeIfAbsent(modelId, ignored -> new LazyModelSource(cacheFile, ServerModelManager.serverKey, modelData.getLoadedModelData(), isAuth, false));
                        localOnlyModelIds.add(modelId);
                        if (!modelAssemblyMap.containsKey(modelId)) {
                            pendingModelQueue.add(Pair.of(new LazyModelAssembly(modelId, source), modelId));
                        }
                    }
                } catch (Exception e) {
                    YesSteveModel.LOGGER.error("[YSM] Failed to register local model: " + modelId, e);
                }
            }
            flushPendingModels();
            Minecraft.getInstance().execute(() -> {
                if (syncState.currentState == SyncState.SYNCING) {
                    syncState.syncedModels = syncState.totalModels;
                    int total = syncState.totalModels;
                    forEachGuiWidget(guiWidget -> guiWidget.onSyncProgress(total, total));
                }
            });
        }
    }

    public static void enterClientOnlyMode() {
        if (!ClientOnlyMode.markCatalogLoaded()) return;
        Minecraft.getInstance().execute(() -> syncState.setState(SyncState.LOADING));
        YSMThreadPool.submit(() -> {
            try {
                if (ServerModelManager.canReuseLoadedModels()) {
                    registerClientOnlyCatalog();
                    return;
                }
                ServerModelManager.loadModels(result -> {
                    if (result.isSuccess()) {
                        registerClientOnlyCatalog();
                    } else {
                        YesSteveModel.LOGGER.error("[YSM] Client-only model loading failed: " + result.getErrorMessage().getString(256));
                        Minecraft.getInstance().execute(() -> syncState.setState(SyncState.IDLE));
                    }
                }, null);
            } catch (Exception e) {
                YesSteveModel.LOGGER.error("[YSM] Failed to enter client-only mode", e);
                Minecraft.getInstance().execute(() -> syncState.setState(SyncState.IDLE));
            }
        });
    }

    private static void registerClientOnlyCatalog() {
        Map<String, ServerModelData> serverModelInfo = ServerModelManager.getServerModelInfo();
        int total = serverModelInfo == null ? 0 : serverModelInfo.size();
        Minecraft.getInstance().execute(() -> {
            if (total > 0) {
                syncState.startSyncing(total);
            }
            forEachGuiWidget(guiWidget -> guiWidget.onSyncProgress(total, 0));
        });
        registerLocalModelPacks();
        Minecraft.getInstance().execute(() -> {
            registerLocalModelCatalog();
            if (!isLazyModelLoadingEnabled()) {
                requestAllLazyModels();
            }
            if (!NetworkHandler.isClientConnected()) {
                syncState.setState(SyncState.IDLE);
            }
            Map<String, ModelAssembly> models = modelAssemblyMap;
            forEachGuiWidget(guiWidget -> {
                guiWidget.onModelsUpdated(models);
                guiWidget.onSyncComplete();
            });
            applyClientOnlySelection();
            YesSteveModel.LOGGER.info("[YSM] Client-only catalog registered, " + models.size() + " model(s) available.");
        });
    }

    public static void applyClientOnlySelection() {
        if (!ClientOnlyMode.isActive() || !ClientOnlySelection.hasSelection()) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        String modelId = ClientOnlySelection.getModelId();
        if (!modelAssemblyMap.containsKey(modelId)) return;
        PlayerCapability.get(player).ifPresent(cap -> {
            if (!modelId.equals(cap.getModelId()) || !ClientOnlySelection.getTextureId().equals(cap.getCurrentTextureName())) {
                cap.initModelWithTexture(modelId, ClientOnlySelection.getTextureId());
            }
        });
    }

    private static void processServerData(ByteBuffer data) {
        if (data == null) {
            resetClientState();
            return;
        }
        try {
            if (!data.hasRemaining() && data.position() > 0) {
                data.flip();
            }
            if (!data.hasRemaining()) return;

            byte[] packetBytes = new byte[data.remaining()];
            data.get(packetBytes);

            byte[] decrypted;
            if (syncStep == 1) {
                decrypted = YsmCrypt.decrypt(packetBytes, YsmCrypt.publicKey);
                System.out.println(Arrays.toString(decrypted));
                if (decrypted != null) handlePacket01(decrypted);
            } else if (syncStep == 2) {
                decrypted = YsmCrypt.decrypt(packetBytes, lastKey);
                if (decrypted != null) {
                    try (YSMByteBuf buf = new YSMByteBuf(Unpooled.wrappedBuffer(decrypted))) {
                        handlePacket03(buf);
                    }
                }
            } else if (syncStep == 3) {
                decrypted = YsmCrypt.decrypt(packetBytes, key1);
                if (decrypted != null) {
                    try (YSMByteBuf buf = new YSMByteBuf(Unpooled.wrappedBuffer(decrypted))) {
                        handlePacket05(buf);
                    }
                }
            }
        } catch (Exception e) {
            YesSteveModel.LOGGER.error("[YSM] Sync Error at step " + syncStep, e);
        }
    }

    private static void handlePacket01(byte[] decryptedBuffer) throws Exception {
        key1 = new byte[56];
        System.arraycopy(decryptedBuffer, decryptedBuffer.length - 56, key1, 0, 56);
        syncStep = 2;

        YesSteveModel.LOGGER.info("[YSM] Exchanged Key1. Preparing to send Packet 02.");
        onSyncProgress(-1); // Preparing GUI stage

        int garbageLen = 16 + SECURE_RANDOM.nextInt(48);
        byte[] garbage = new byte[garbageLen];
        SECURE_RANDOM.nextBytes(garbage);

        try (YSMByteBuf outBuf = new YSMByteBuf(Unpooled.buffer())) {
            outBuf.writeGarbageHeader(garbageLen, garbage);
            outBuf.getRawBuf().writeByte(0x02);
            outBuf.getRawBuf().writeByte(0x00);

            YsmCrypt.EncryptedPacket result = YsmCrypt.encrypt(outBuf.toArray(), key1, true);
            lastKey = result.nextKey();

            sendModelFile(ByteBuffer.wrap(result.data()));
        }
    }

    private record ModelHash(long hash1, long hash2) {
    }

    private static final List<ModelHash> cachedModelHashes = new ArrayList<>();

    private static void handlePacket03(YSMByteBuf buf) throws Exception {
        int generation = modelLoadGeneration.get();
        buf.skipGarbageHeader();
        int type = buf.readVarInt(); // expect 3
        long folderHash = buf.readVarLong();
        currentCacheFolderName = Long.toHexString(folderHash);

        serverKey = new byte[56];
        buf.getRawBuf().readBytes(serverKey);

        clientKey = new byte[56];
        buf.getRawBuf().readBytes(clientKey);

        File cacheDir = ServerModelManager.CACHE_CLIENT.resolve(currentCacheFolderName).toFile();
        if (!cacheDir.exists()) cacheDir.mkdirs();

        boolean useLocalModelCatalog = Minecraft.getInstance().isLocalServer() && isLazyModelLoadingEnabled();
        Map<UUID, File> localCacheMap = useLocalModelCatalog ? Map.of() : YSMClientCache.buildCacheIndex(cacheDir, clientKey);
        List<ModelHash> modelsToRequest = new ArrayList<>();

        int unkSize = buf.readVarInt();
        onSyncProgress(unkSize);

        if (useLocalModelCatalog || ClientOnlyMode.isForced()) {
            registerLocalModelCatalog();
        }

        Set<String> validServerModelIds = new HashSet<>();
        List<String> previousModelIds = new ArrayList<>();
        List<String> updatedModelIds = new ArrayList<>();
        List<Boolean> isAuthList = new ArrayList<>();

        for (int i = 0; i < unkSize; i++) {
            long hash1 = buf.readVarLong();
            long hash2 = buf.readVarLong();
            ModelHash mHash = new ModelHash(hash1, hash2);
            cachedModelHashes.add(mHash);

            String modelId = buf.readString();
            boolean isAuth = buf.readVarInt() == 1;// isAuth
            int isCustomSkinModel = buf.readVarInt();// is misc/2_steve misc/1_alex
//            System.out.println("Received model hash: " + mHash + ", id: " + modelId + ", unk1: " + isAuth + ", unk2: " + isCustomSkinModel);
            int version = buf.readVarInt(); // 对于文件夹未加密的模型，为65535

            ServerModelContext ctx = new ServerModelContext(hash1, hash2, modelId, isAuth, isCustomSkinModel, version, generation);
            serverModels.put(ctx.uuid, ctx);
            validServerModelIds.add(modelId);

            if (ClientOnlyMode.isForced() && localOnlyModelIds.contains(modelId)) {
                YesSteveModel.LOGGER.info("[YSM] Model id '{}' exists locally, keeping the local copy and ignoring the server one.", modelId);
                continue;
            }

            boolean alreadyInMemory = modelAssemblyMap != null && modelAssemblyMap.containsKey(modelId);

            if (alreadyInMemory) {
                previousModelIds.add(modelId);
                updatedModelIds.add(modelId);
                isAuthList.add(isAuth);
                continue;
            }

            File cachedFile = localCacheMap.get(ctx.uuid);
            boolean isFileValid = YSMClientCache.verifyFileContent(cachedFile, hash1, hash2);

            if (isFileValid) {
                YesSteveModel.LOGGER.info("[YSM] Cache HIT & Validated: " + ctx.uuid);
                    byte[] cacheKey = clientKey.clone();
                    modelPhraseExecutor.execute(() -> {
                        if (generation != modelLoadGeneration.get()) return;
                        try {
                            byte[] fileBytes = Files.readAllBytes(cachedFile.toPath());
                            if (generation != modelLoadGeneration.get()) return;
                            CachePayload cachePayload = YsmCrypt.readInPlace(fileBytes, cacheKey);
                            registerRemoteLazyModel(cachePayload, cachedFile.toPath(), cacheKey, ctx);
                        } catch (Exception e) {
                            YesSteveModel.LOGGER.error("[YSM] Failed to prepare cached model: " + modelId, e);
                        }
                    });
            } else {
                YesSteveModel.LOGGER.info("[YSM] Cache MISS or Invalid: " + ctx.uuid + " -> Requesting...");
                modelsToRequest.add(mHash);
            }
        }

        int unkSize2 = buf.readVarInt();
        List<ModelPackData> parsedPacks = new ArrayList<>();

        for (int i = 0; i < unkSize2; i++) {
            String folderPath = buf.readString();

            OuterFileTexture iconTexture = null;
            if (buf.readVarInt() != 0) {
                byte[] textureData = buf.readByteArray();
                int textureWidth = buf.readVarInt();
                int textureHeight = buf.readVarInt();
                int imageFormat = buf.readVarInt();
                int unkImageData = buf.readVarInt();

                iconTexture = YSMClientMapper.toTexture(textureData, imageFormat, textureWidth, textureHeight);
            }

            String folderName = "";
            String folderDesc = "";
            int hasYSMPackInfo = buf.readVarInt();
            if (hasYSMPackInfo != 0) {
                folderName = buf.readString();
                folderDesc = buf.readString();
            }

            Map<String, Map<String, String>> languageData = new HashMap<>();
            int languageSize = buf.readVarInt();
            for (int j = 0; j < languageSize; j++) {
                String languageType = buf.readString();
                int translateKeySize = buf.readVarInt();
                Map<String, String> translationMap = new HashMap<>();
                for (int k = 0; k < translateKeySize; k++) {
                    translationMap.put(buf.readString(), buf.readString());
                }
                languageData.put(languageType, translationMap);
            }
            parsedPacks.add(new ModelPackData(folderPath, folderName, folderDesc, iconTexture, languageData));
        }

        if (!parsedPacks.isEmpty()) {
            onModelPacksReceived(parsedPacks.toArray(new ModelPackData[0]));
        }

        List<String> modelsToRemove = new ArrayList<>();
        if (modelAssemblyMap != null) {
            for (String loadedId : modelAssemblyMap.keySet()) {
                if ("default".equals(loadedId)) continue;
                if (ClientOnlyMode.isForced() && localOnlyModelIds.contains(loadedId)) continue;

                if (!validServerModelIds.contains(loadedId)) {
                    modelsToRemove.add(loadedId);
                } else if (modelsToRequest.stream().anyMatch(h -> serverModels.containsKey(new UUID(h.hash1, h.hash2)) && serverModels.get(new UUID(h.hash1, h.hash2)).modelId.equals(loadedId))) {
                    modelsToRemove.add(loadedId);
                }
            }
        }

        if (!modelsToRemove.isEmpty() || !previousModelIds.isEmpty()) {
            boolean[] authArr = new boolean[isAuthList.size()];
            for (int j = 0; j < isAuthList.size(); j++) {
                authArr[j] = isAuthList.get(j);
            }

            onModelContextsUpdated(
                    modelsToRemove.isEmpty() ? null : modelsToRemove.toArray(new String[0]),
                    previousModelIds.isEmpty() ? null : previousModelIds.toArray(new String[0]),
                    updatedModelIds.isEmpty() ? null : updatedModelIds.toArray(new String[0]),
                    authArr
            );
            YesSteveModel.LOGGER.info("[YSM] Cleaned up {} outdated models and updated {} existing models during sync.", modelsToRemove.size(), previousModelIds.size());
        }

        syncStep = 3;
        pendingModelsCount.set(modelsToRequest.size());

        int garbageLen = 16 + SECURE_RANDOM.nextInt(48);
        byte[] garbage = new byte[garbageLen];
        SECURE_RANDOM.nextBytes(garbage);

        try (YSMByteBuf outBuf = new YSMByteBuf(Unpooled.buffer())) {
            outBuf.writeGarbageHeader(garbageLen, garbage);
            outBuf.getRawBuf().writeByte(0x04);

            outBuf.writeVarInt(modelsToRequest.size());
            for (ModelHash h : modelsToRequest) {
                outBuf.writeVarLong(h.hash1);
                outBuf.writeVarLong(h.hash2);
            }

            YsmCrypt.EncryptedPacket result = YsmCrypt.encrypt(outBuf.toArray(), key1, false);
            sendModelFile(ByteBuffer.wrap(result.data()));
        }

        if (pendingModelsCount.get() == 0) {
            modelPhraseExecutor.submit(() -> {
                YesSteveModel.LOGGER.info("[YSM] All models loaded from local cache. Handshake complete!");
                onSyncComplete();
            });
        }
    }

    private static void handlePacket05(YSMByteBuf buf) throws Exception {
        buf.skipGarbageHeader();
        int type = buf.readVarInt();
        if (type != 5) return;

        long hash1 = buf.readVarLong();
        long hash2 = buf.readVarLong();
        UUID uuid = new UUID(hash1, hash2);

        ServerModelContext ctx = serverModels.get(uuid);
        if (ctx == null || ctx.generation != modelLoadGeneration.get()) {
            YesSteveModel.LOGGER.warn("[YSM] Received unexpected file chunk for model: " + uuid);
            return;
        }

        int totalSize = buf.readVarInt();
        int chunkOffset = buf.readVarInt();
        int chunkLength = buf.readVarInt();

        if (totalSize <= 0 || chunkOffset < 0 || chunkLength < 0 || chunkOffset > totalSize - chunkLength || chunkLength > buf.getRawBuf().readableBytes()) {
            throw new IllegalArgumentException("Invalid model chunk bounds for " + uuid);
        }

        Path completedFile;
        synchronized (ctx) {
            if (ctx.downloadComplete) {
                buf.getRawBuf().skipBytes(chunkLength);
                return;
            }

            if (ctx.downloadFile == null) {
                String folder = currentCacheFolderName != null ? currentCacheFolderName : "default_cache";
                Path cacheDir = ServerModelManager.CACHE_CLIENT.resolve(folder);
                Files.createDirectories(cacheDir);
                ctx.downloadFile = Files.createTempFile(cacheDir, ".ysm-download-", ".part");
                ctx.totalSize = totalSize;
                ctx.bytesReceived = 0;
            } else if (ctx.totalSize != totalSize) {
                throw new IllegalArgumentException("Model size changed during transfer for " + uuid);
            }

            byte[] chunk = new byte[chunkLength];
            buf.getRawBuf().readBytes(chunk);
            try (SeekableByteChannel channel = Files.newByteChannel(ctx.downloadFile, StandardOpenOption.WRITE)) {
                channel.position(chunkOffset);
                ByteBuffer chunkBuffer = ByteBuffer.wrap(chunk);
                while (chunkBuffer.hasRemaining()) {
                    channel.write(chunkBuffer);
                }
            }
            ctx.bytesReceived += chunkLength;

            if (ctx.bytesReceived < totalSize) return;

            completedFile = ctx.downloadFile;
            ctx.downloadFile = null;
            ctx.totalSize = 0;
            ctx.bytesReceived = 0;
            ctx.downloadComplete = true;
        }

        byte[] serverKeySnapshot = serverKey.clone();
        byte[] clientKeySnapshot = clientKey.clone();
        String cacheFolderSnapshot = currentCacheFolderName != null ? currentCacheFolderName : "default_cache";
        modelPhraseExecutor.execute(new RemoteModelTask(completedFile, ctx, serverKeySnapshot, clientKeySnapshot, cacheFolderSnapshot));
    }


    private static void parseAndLoadModel(CachePayload cachePayload, String modelId, boolean isAuth, int generation) {
        try {
            if (generation != modelLoadGeneration.get()) return;
//            if (true) return;
            // IR

            try (YSMBinaryDeserializer deserializer = new YSMBinaryDeserializer(cachePayload.data(), cachePayload.formatVersion())) {
                RawYsmModel rawModel = deserializer.deserializeKeepOpen();
                YSMByteBuf reader = deserializer.getReader();

                // 读取版本号
                rawModel.footer.version = reader.readVarInt();// 65535 或 32

                rawModel.footer.unkInt1 = reader.readVarInt(); // 待分析
                if (rawModel.footer.unkInt1 != 0) {
                    rawModel.footer.rand = reader.readString();
                }

                rawModel.footer.time = reader.readVarLong();

                if (rawModel.footer.unkInt1 != 0) {
                    rawModel.footer.extra = reader.readString();
                    rawModel.footer.unkInt2 = reader.readVarInt();
                }

            // 组装到客户端模型
            ClientModelInfo parsedBundle = YSMClientMapper.buildParsedBundle(rawModel, modelId);
            if (generation == modelLoadGeneration.get()) {
                onModelDataReceived(parsedBundle, modelId, false, isAuth);
            }
            }
        } catch (Exception e) {
            YesSteveModel.LOGGER.error("[YSM] Failed to parse and load model: " + modelId, e);
        }
    }

    private static void registerRemoteLazyModel(CachePayload cachePayload, Path cacheFile, byte[] cacheKey, ServerModelContext context) {
        if (context.generation != modelLoadGeneration.get()) return;
        try (YSMBinaryDeserializer deserializer = new YSMBinaryDeserializer(cachePayload.data(), cachePayload.formatVersion())) {
            RawYsmModel rawModel = deserializer.deserializeKeepOpen();
            ServerModelInfo modelInfo = YSMClientMapper.buildModelInfo(rawModel);
            if (context.generation != modelLoadGeneration.get()) return;

            LazyModelSource source = new LazyModelSource(cacheFile, cacheKey, modelInfo, context.isAuth, true);
            lazyModelSources.put(context.modelId, source);
            pendingModelQueue.add(Pair.of(new LazyModelAssembly(context.modelId, source), context.modelId));
            processModelData(null, context.modelId, false, context.isAuth);
        } catch (Exception e) {
            YesSteveModel.LOGGER.error("[YSM] Failed to prepare lazy model: " + context.modelId, e);
        }
    }

    private static OrderedStringMap<String, OuterFileTexture> toOrderedTextureMap(Map<String, OuterFileTexture> textures) {
        if (textures == null || textures.isEmpty()) {
            return new OrderedStringMap<>(new String[0], new OuterFileTexture[0]);
        }
        return new OrderedStringMap<>(
                textures.keySet().toArray(new String[0]),
                textures.values().toArray(new OuterFileTexture[0])
        );
    }

    private static void resetClientState() {
        modelLoadGeneration.incrementAndGet();
        syncStep = 1;
        key1 = null;
        lastKey = null;
        serverKey = null;
        clientKey = null;

        ArrayList<Runnable> discardedModelTasks = new ArrayList<>();
        modelPhraseExecutor.getQueue().drainTo(discardedModelTasks);
        for (Runnable task : discardedModelTasks) {
            if (task instanceof CancellableModelTask cancellableTask) cancellableTask.cancel();
        }
        modelPrepareExecutor.getQueue().clear();

        currentCacheFolderName = null;
        pendingModelsCount.set(0);
        cachedModelHashes.clear();
        removeLazyLocalModels();
        lazyModelSources.clear();
        localOnlyModelIds.clear();
        loadingLazyModels.clear();
        synchronized (loadedLocalModelAccessLock) {
            loadedLocalModelAccess.clear();
        }

        for (ServerModelContext ctx : serverModels.values()) {
            deleteQuietly(ctx.downloadFile);
        }
        serverModels.clear();

        Map<String, ModelAssembly> oldModels = modelAssemblyMap;
        Object2ReferenceOpenHashMap<String, ModelAssembly> retainedModels = new Object2ReferenceOpenHashMap<>();
        ModelAssembly defaultModel = oldModels.get("default");
        if (defaultModel != null) retainedModels.put("default", defaultModel);
        modelAssemblyMap = retainedModels;
        ArrayList<ModelAssembly> discardedModels = new ArrayList<>();
        for (Map.Entry<String, ModelAssembly> entry : oldModels.entrySet()) {
            if (!"default".equals(entry.getKey())) discardedModels.add(entry.getValue());
        }
        if (!discardedModels.isEmpty()) {
            Minecraft.getInstance().execute(() -> discardedModels.forEach(ClientModelManager::releaseModelAssembly));
        }

        Map<String, ModelPackData> oldPreviews = modelPackMap;
        if (oldPreviews != null && !oldPreviews.isEmpty()) {
            for (ModelPackData preview : oldPreviews.values()) {
                if (preview.getTexture() != null) {
                    ResourceLocation loc = FileTypeUtil.getPackIconLocation(preview.getPath());
                    Minecraft.getInstance().execute(() -> {
                        Minecraft.getInstance().getTextureManager().release(loc);
                    });
                }
            }
        }

        modelPackMap = new Object2ReferenceOpenHashMap<>();
        pendingModelCallback = null;
        pendingModelQueue.clear();

        forEachGuiWidget(l -> {
            try {
                l.onSyncBegin();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
    }

    public static SyncStatus getSyncStatus() {
        RenderSystem.assertOnGameThread();
        return syncState;
    }

    public static Map<String, ModelAssembly> getModelAssemblyMap() {
        return modelAssemblyMap;
    }

    public static Map<String, ModelPackData> getModelPackMap() {
        return modelPackMap;
    }

    public static Optional<ModelAssembly> getModelContext(String str) {
        ModelAssembly assembly = modelAssemblyMap.get(str);
        if (assembly instanceof LazyModelAssembly) {
            requestLazyModel(str);
            return Optional.empty();
        }
        if (assembly != null && lazyModelSources.containsKey(str)) {
            touchLoadedLocalModel(str);
        }
        return Optional.ofNullable(assembly);
    }

    public static ModelAssembly getLocalModelContext() {
        runPendingModelCallback();
        flushPendingModels();

        ModelAssembly model = localModelContext;
        if (model != null) return model;

        // 触发预加载
        loadDefaultModel();
        model = localModelContext;
        if (model != null) return model;

        Map<String, ModelAssembly> reg = modelAssemblyMap;
        if (reg != null && !reg.isEmpty()) {
            model = reg.get("default");
            if (model == null) {
                for (ModelAssembly v : reg.values()) {
                    if (v != null && !(v instanceof LazyModelAssembly)) {
                        model = v;
                        break;
                    }
                }
            }
            if (model != null) {
                localModelContext = model;
                return model;
            }
        }
        return null;
    }

    public static ResourceLocation getDefaultTexture() {
        return defaultTexture.getResourceLocation().get();
    }

    public static <T extends IGuiWidget> T registerGuiWidget(T t) {
        guiWidgets.put(t, null);
        return t;
    }

    public static void unregisterGuiWidget(IGuiWidget guiWidget) {
        guiWidgets.remove(guiWidget, null);
    }

    private static void forEachGuiWidget(Consumer<IGuiWidget> consumer) {
        Iterator<IGuiWidget> it = guiWidgets.keySet().iterator();
        while (it.hasNext()) {
            try {
                consumer.accept(it.next());
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
    }

    public static void resetSync() {
        isOysmServer = false;
        allowUpload = false;
        ClientOnlyMode.reset();
        processServerData(null);
        NetworkHandler.resetClientHandshake();
        Minecraft.getInstance().execute(() -> {
            syncState.setState(SyncState.WAITING);
        });
    }

    public static boolean isAllowUpload() {
        return allowUpload;
    }

    public static boolean isOysmServer() {
        return isOysmServer;
    }

    private static void sendModelFile(ByteBuffer byteBuffer) {
        if (Minecraft.getInstance().player != null) {
            try {
                NetworkHandler.sendToServer(new C2SModelSyncPayload(byteBuffer));
                return;
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
        Connection connection = serverConnection;
        if (!connection.isConnected()) {
            return;
        }
        try {
            connection.send(NetworkHandler.toServerboundPacket(new C2SModelSyncPayload(byteBuffer)));
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    public static void startSync(Connection connection, ByteBuffer byteBuffer) {
        serverConnection = connection;
        processServerData(byteBuffer);
    }

    public static void onSyncConnected() {
        if (Minecraft.getInstance().isLocalServer()) {
            syncState.setState(SyncState.LOADING);
        } else {
            syncState.setState(SyncState.IDLE);
        }
        forEachGuiWidget(IGuiWidget::onSyncBegin);
    }

    private static void onSyncProgress(int totalModels) {
        if (totalModels == -1) {
            Minecraft.getInstance().execute(() -> {
                syncState.setState(SyncState.PREPARING);
                forEachGuiWidget(IGuiWidget::onSyncError);
            });
        } else {
            Minecraft.getInstance().execute(() -> {
                if (totalModels > 0) {
                    syncState.startSyncing(totalModels);
                } else {
                    syncState.setState(SyncState.IDLE);
                }
                forEachGuiWidget(guiWidget -> guiWidget.onSyncProgress(totalModels, 0));
            });
        }
    }

    private static void registerLocalModelPacks() {
        Map<String, ServerModelManager.ServerPackData> packs = ServerModelManager.getPacks();
        if (packs == null || packs.isEmpty()) return;
        List<ModelPackData> parsedPacks = new ArrayList<>();
        for (ServerModelManager.ServerPackData pack : packs.values()) {
            OuterFileTexture iconTexture = null;
            if (pack.iconData != null) {
                try {
                    iconTexture = YSMClientMapper.toTexture(pack.iconData, pack.iconFormat, pack.iconWidth, pack.iconHeight);
                } catch (Exception e) {
                    YesSteveModel.LOGGER.error("[YSM] Failed to decode pack icon: " + pack.folderPath, e);
                }
            }
            parsedPacks.add(new ModelPackData(pack.folderPath, pack.name != null ? pack.name : "", pack.description != null ? pack.description : "", iconTexture, pack.lang != null ? pack.lang : new HashMap<>()));
        }
        if (!parsedPacks.isEmpty()) {
            onModelPacksReceived(parsedPacks.toArray(new ModelPackData[0]));
        }
    }

    private static void onModelPacksReceived(ModelPackData[] packDataArr) {
        Object2ReferenceOpenHashMap<String, ModelPackData> newPackMap = new Object2ReferenceOpenHashMap<>();

        for (ModelPackData packData : packDataArr) {
            if (StringUtils.isBlank(packData.getName())) {
                packData = new ModelPackData(packData.getPath(), FileTypeUtil.getFinalPathSegment(packData.getPath()), packData.getDescription(), packData.getTexture(), packData.getTranslations());
            }
            newPackMap.put(packData.getPath(), packData);
            OuterFileTexture iconTexture = packData.getTexture();
            if (iconTexture != null) {
                ResourceLocation location2 = FileTypeUtil.getPackIconLocation(packData.getPath());
                Minecraft.getInstance().submit(() -> {
                    Minecraft.getInstance().getTextureManager().register(location2, iconTexture);
                });
            }
        }

        for (ModelPackData packData : modelPackMap.values()) {
            if (!newPackMap.containsKey(packData.getPath()) && packData.getTexture() != null) {
                ResourceLocation location = FileTypeUtil.getPackIconLocation(packData.getPath());
                Minecraft.getInstance().submit(() -> Minecraft.getInstance().getTextureManager().release(location));
            }
        }
        modelPackMap = newPackMap;
    }

    private static void onModelContextsUpdated(String[] removedModelIds, String[] previousModelIds, String[] updatedModelIds, boolean[] isAuthArr) {
        Minecraft.getInstance().execute(() -> {
            Object2ReferenceOpenHashMap<String, ModelAssembly> map = new Object2ReferenceOpenHashMap<>(modelAssemblyMap);
            if (removedModelIds != null) {
                ArrayList<ModelAssembly> removed = new ArrayList<>(removedModelIds.length);
                for (String str : removedModelIds) {
                    ModelAssembly assembly = map.remove(str);
                    lazyModelSources.remove(str);
                    localOnlyModelIds.remove(str);
                    loadingLazyModels.remove(str);
                    synchronized (loadedLocalModelAccessLock) {
                        loadedLocalModelAccess.remove(str);
                    }
                    if (assembly != null) {
                        removed.add(assembly);
                    }
                }
                Minecraft.getInstance().execute(() -> {
                    for (ModelAssembly assembly : removed) {
                        releaseModelAssembly(assembly);
                    }
                });
            }
            if (previousModelIds != null) {
                ModelAssembly[] modelAssemblies = new ModelAssembly[previousModelIds.length];
                for (int i = 0; i < previousModelIds.length; i++) {
                    modelAssemblies[i] = map.remove(previousModelIds[i]);
                }
                for (int i = 0; i < modelAssemblies.length; i++) {
                    ModelAssembly modelAssembly = modelAssemblies[i];
                    if (modelAssembly != null) {
                        LazyModelSource source = lazyModelSources.remove(previousModelIds[i]);
                        if (source != null) {
                            lazyModelSources.put(updatedModelIds[i], source);
                            synchronized (loadedLocalModelAccessLock) {
                                loadedLocalModelAccess.remove(previousModelIds[i]);
                                if (!(modelAssembly instanceof LazyModelAssembly)) {
                                    loadedLocalModelAccess.put(updatedModelIds[i], Boolean.TRUE);
                                }
                            }
                            if (modelAssembly instanceof LazyModelAssembly) {
                                modelAssembly = new LazyModelAssembly(updatedModelIds[i], source);
                            }
                        }
                        modelAssembly.getTextureRegistry().setAuthModel(isAuthArr[i]);
                        map.put(updatedModelIds[i], modelAssembly);
                    }
                }
            }
            modelAssemblyMap = map;
            if ((removedModelIds != null && removedModelIds.length > 0) || (previousModelIds != null && previousModelIds.length > 0)) {
                forEachGuiWidget(guiWidget -> {
                    guiWidget.onModelsLoaded(map);
                });
            }
        });
    }

    private static void onModelDataReceived(@Nullable ClientModelInfo parsedBundle, String modelId, boolean isPrimary, boolean isAuth) throws Exception {
        if (isPrimary) {
            pendingModelCallback = () -> {
                processModelData(parsedBundle, modelId, true, false);
            };
        } else {
            runPendingModelCallback();
            processModelData(parsedBundle, modelId, false, isAuth);
        }
    }

    public static void runPendingModelCallback() {
        Runnable runnable = pendingModelCallback;
        if (runnable != null) {
            synchronized (runnable) {
                Runnable runnable2 = pendingModelCallback;
                if (runnable2 != null) {
                    runnable2.run();
                    pendingModelCallback = null;
                }
            }
        }
    }

    public static void processModelData(@Nullable ClientModelInfo parsedBundle, String modelId, boolean isPrimary, boolean isAuth) {
        if (parsedBundle != null) {
            try {
                ModelAssembly runtimeModel = ModelAssemblyFactory.buildAssembly(parsedBundle, isPrimary, isAuth);
                pendingModelQueue.add(Pair.of(runtimeModel, modelId));
                if (isPrimary) {
                    localModelContext = runtimeModel;

                    Minecraft.getInstance().execute(() -> {
                        defaultTexture = UploadManager.getOrCreateLocatable(runtimeModel.getAnimationBundle().getTextures().getValueAt(0), true);
                    });
                    return;
                }
            } catch (Exception e) {
                if (isPrimary) throw e;
                YesSteveModel.LOGGER.error(
                        new StringFormattedMessage("Failed to process {}", modelId), e);
                return;
            }
        }
        int generation = modelLoadGeneration.get();
        Minecraft.getInstance().execute(() -> {
            if (generation == modelLoadGeneration.get() && syncState.currentState == SyncState.SYNCING) {
                syncState.syncedModels = Math.min(syncState.syncedModels + 1, syncState.totalModels);
                int loaded = syncState.syncedModels;
                if (loaded >= syncState.totalModels) {
                    syncState.setState(SyncState.IDLE);
                }
                forEachGuiWidget(guiWidget -> {
                    guiWidget.onSyncProgress(syncState.getTotalModels(), loaded);
                });
            }
        });
    }

    private static void onSyncComplete() {
        syncStep = 1;
        serverModels.clear();
        cachedModelHashes.clear();

        Minecraft.getInstance().execute(() -> {
            syncState.setState(SyncState.IDLE);
            forEachGuiWidget(IGuiWidget::onSyncComplete);
        });
    }

    public static void setAllowUpload(boolean allowUpload) {
        ClientModelManager.allowUpload = allowUpload;
    }

    public static void setOysmServer(boolean isOysmServer) {
        ClientModelManager.isOysmServer = isOysmServer;
    }

    private static void onSyncError(@Nullable Object obj) {
        Minecraft.getInstance().execute(() -> {
            syncState.setState(SyncState.IDLE);
            forEachGuiWidget(guiWidget -> {
                guiWidget.onSyncMessage(obj == null ? null : (Component) obj);
            });
            if (obj instanceof Component component) {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.sendSystemMessage(component);
                }
                YesSteveModel.LOGGER.error(component.getString(256));
            }
        });
    }

    public static void flushPendingModels() {
        if (pendingModelQueue.isEmpty())
            return;

        Object2ReferenceOpenHashMap<String, ModelAssembly> object2ReferenceOpenHashMap = new Object2ReferenceOpenHashMap<>(modelAssemblyMap);
        while (true) {
            Pair<ModelAssembly, String> pairPoll = pendingModelQueue.poll();
            if (pairPoll != null) {
                object2ReferenceOpenHashMap.put(pairPoll.getRight(), pairPoll.getLeft());
                if (!(pairPoll.getLeft() instanceof LazyModelAssembly) && lazyModelSources.containsKey(pairPoll.getRight())) {
                    touchLoadedLocalModel(pairPoll.getRight());
                }
            } else {
                trimLoadedLocalModels(object2ReferenceOpenHashMap);
                modelAssemblyMap = object2ReferenceOpenHashMap;
                forEachGuiWidget(guiWidget -> guiWidget.onModelsUpdated(object2ReferenceOpenHashMap));
                return;
            }
        }
    }

    private static void requestLazyModel(String modelId) {
        requestLazyModel(modelId, false);
    }

    private static void requestLazyModel(String modelId, boolean fullPreload) {
        LazyModelSource source = lazyModelSources.get(modelId);
        if (source == null) return;

        ModelAssembly current = modelAssemblyMap.get(modelId);
        if (current != null && !(current instanceof LazyModelAssembly)) {
            touchLoadedLocalModel(modelId);
            return;
        }
        if (!loadingLazyModels.add(modelId)) return;
        int generation = modelLoadGeneration.get();

        modelPrepareExecutor.execute(() -> {
            boolean slotAcquired = false;
            boolean handedOff = false;
            try {
                if (generation != modelLoadGeneration.get()) return;
                preparedModelSlots.acquire();
                slotAcquired = true;
                if (generation != modelLoadGeneration.get()) return;
                byte[] fileData = Files.readAllBytes(source.cacheFile());
                if (generation != modelLoadGeneration.get() || lazyModelSources.get(modelId) != source) return;
                CachePayload cachePayload = YsmCrypt.readInPlace(fileData, source.key());
                if (generation != modelLoadGeneration.get() || lazyModelSources.get(modelId) != source) return;
                modelPhraseExecutor.execute(new PreparedModelTask(cachePayload, modelId, source, fullPreload, generation));
                handedOff = true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                YesSteveModel.LOGGER.error("[YSM] Failed to load local cache for model: " + modelId, e);
            } finally {
                if (!handedOff) {
                    if (slotAcquired) preparedModelSlots.release();
                    loadingLazyModels.remove(modelId);
                }
            }
        });
    }

    private static void requestAllLazyModels() {
        for (Map.Entry<String, LazyModelSource> entry : new ArrayList<>(lazyModelSources.entrySet())) {
            if (!entry.getValue().alwaysLazy()) {
                requestLazyModel(entry.getKey(), true);
            }
        }
    }

    private static boolean isLazyModelLoadingEnabled() {
        return GeneralConfig.LAZY_MODEL_LOADING == null || GeneralConfig.LAZY_MODEL_LOADING.get();
    }

    public static void updateModelLoadingMode() {
        boolean enabled = isLazyModelLoadingEnabled();
        boolean firstCall = lastLazyModelLoading == null;
        if (!firstCall && lastLazyModelLoading == enabled) return;
        lastLazyModelLoading = enabled;
        if (!firstCall) {
            modelLoadGeneration.incrementAndGet();
            loadingLazyModels.clear();
        }
        if (!enabled) {
            requestAllLazyModels();
            return;
        }
        Object2ReferenceOpenHashMap<String, ModelAssembly> map = new Object2ReferenceOpenHashMap<>(modelAssemblyMap);
        trimLoadedLocalModels(map);
        modelAssemblyMap = map;
        forEachGuiWidget(guiWidget -> guiWidget.onModelsUpdated(map));
    }

    private static void removeLazyLocalModels() {
        if (lazyModelSources.isEmpty()) return;
        Object2ReferenceOpenHashMap<String, ModelAssembly> map = new Object2ReferenceOpenHashMap<>(modelAssemblyMap);
        ArrayList<ModelAssembly> removed = new ArrayList<>();
        for (String modelId : lazyModelSources.keySet()) {
            ModelAssembly assembly = map.remove(modelId);
            if (assembly != null && !(assembly instanceof LazyModelAssembly)) {
                removed.add(assembly);
            }
        }
        modelAssemblyMap = map;
        if (!removed.isEmpty()) {
            Minecraft.getInstance().execute(() -> removed.forEach(ClientModelManager::releaseModelAssembly));
        }
    }

    private static void touchLoadedLocalModel(String modelId) {
        synchronized (loadedLocalModelAccessLock) {
            loadedLocalModelAccess.put(modelId, Boolean.TRUE);
        }
    }

    private static void trimLoadedLocalModels(Object2ReferenceOpenHashMap<String, ModelAssembly> map) {
        boolean localLazyLoading = isLazyModelLoadingEnabled();
        ArrayList<ModelAssembly> removed = new ArrayList<>();
        synchronized (loadedLocalModelAccessLock) {
            Iterator<String> iterator = loadedLocalModelAccess.keySet().iterator();
            while (loadedLocalModelAccess.size() > MAX_LOADED_LOCAL_MODELS && iterator.hasNext()) {
                String modelId = iterator.next();
                ModelAssembly assembly = map.get(modelId);
                LazyModelSource source = lazyModelSources.get(modelId);
                if (source == null || assembly == null || assembly instanceof LazyModelAssembly || loadingLazyModels.contains(modelId)) {
                    iterator.remove();
                    continue;
                }
                if (!source.alwaysLazy() && !localLazyLoading) continue;
                map.put(modelId, new LazyModelAssembly(modelId, source));
                iterator.remove();
                removed.add(assembly);
            }
        }
        for (ModelAssembly assembly : removed) {
            releaseModelAssembly(assembly);
        }
    }

    private static void releaseModelAssembly(ModelAssembly assembly) {
        if (assembly instanceof LazyModelAssembly) return;
        for (AbstractTexture texture : assembly.getTextures()) {
            UploadManager.removeTexture(texture);
        }
        if (NativeLibLoader.isLoaded()) {
            for (ProjectileModelBundle bundle : assembly.getProjectileModels().values()) {
                bundle.getModel().freeNativeCache();
            }
            for (VehicleModelBundle bundle : assembly.getVehicleModels().values()) {
                bundle.getModel().freeNativeCache();
            }
            assembly.getAnimationBundle().getMainModel().freeNativeCache();
            assembly.getAnimationBundle().getArmModel().freeNativeCache();
        }
    }

    private record LazyModelSource(Path cacheFile, byte[] key, ServerModelInfo modelInfo, boolean isAuth, boolean alwaysLazy) {
        private LazyModelSource {
            key = key.clone();
        }
    }

    private interface CancellableModelTask extends Runnable {
        void cancel();
    }

    private static void deleteQuietly(@Nullable Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (Exception e) {
            YesSteveModel.LOGGER.warn("[YSM] Failed to remove temporary model download: " + path, e);
        }
    }

    private static final class RemoteModelTask implements CancellableModelTask {
        private final Path downloadFile;
        private final ServerModelContext context;
        private final byte[] serverKey;
        private final byte[] clientKey;
        private final String cacheFolder;
        private final AtomicBoolean claimed = new AtomicBoolean();

        private RemoteModelTask(Path downloadFile, ServerModelContext context, byte[] serverKey, byte[] clientKey, String cacheFolder) {
            this.downloadFile = downloadFile;
            this.context = context;
            this.serverKey = serverKey;
            this.clientKey = clientKey;
            this.cacheFolder = cacheFolder;
        }

        @Override
        public void run() {
            if (!claimed.compareAndSet(false, true)) return;
            try {
                if (context.generation != modelLoadGeneration.get()) return;
                byte[] fileBuffer = Files.readAllBytes(downloadFile);
                if (context.generation != modelLoadGeneration.get()) return;
                byte[] cachedFileData = YsmCrypt.transcodeServerDataToClientCache(fileBuffer, serverKey, clientKey, context.hash1, context.hash2);
                if (context.generation != modelLoadGeneration.get()) return;

                Path cacheDir = ServerModelManager.CACHE_CLIENT.resolve(cacheFolder);
                Files.createDirectories(cacheDir);
                String cacheName = YSMClientCache.generateCacheFileName(context.hash1, context.hash2, clientKey);
                Path cacheFile = cacheDir.resolve(cacheName);
                Files.write(cacheFile, cachedFileData);

                YesSteveModel.LOGGER.info("[YSM] Downloaded & Cached: " + cacheFile.toAbsolutePath());
                CachePayload cachePayload = YsmCrypt.readInPlace(cachedFileData, clientKey);
                registerRemoteLazyModel(cachePayload, cacheFile, clientKey, context);
            } catch (Exception e) {
                YesSteveModel.LOGGER.error("[YSM] Failed to save/parse downloaded model: " + context.modelId, e);
            } finally {
                deleteQuietly(downloadFile);
                if (context.generation == modelLoadGeneration.get() && pendingModelsCount.decrementAndGet() <= 0) {
                    YesSteveModel.LOGGER.info("[YSM] All missing models downloaded and loaded successfully!");
                    onSyncComplete();
                }
            }
        }

        @Override
        public void cancel() {
            if (claimed.compareAndSet(false, true)) deleteQuietly(downloadFile);
        }
    }

    private static final class PreparedModelTask implements CancellableModelTask {
        private final CachePayload cachePayload;
        private final String modelId;
        private final LazyModelSource source;
        private final boolean fullPreload;
        private final int generation;
        private final AtomicBoolean claimed = new AtomicBoolean();

        private PreparedModelTask(CachePayload cachePayload, String modelId, LazyModelSource source, boolean fullPreload, int generation) {
            this.cachePayload = cachePayload;
            this.modelId = modelId;
            this.source = source;
            this.fullPreload = fullPreload;
            this.generation = generation;
        }

        @Override
        public void run() {
            if (!claimed.compareAndSet(false, true)) return;
            try {
                if (generation == modelLoadGeneration.get() && lazyModelSources.get(modelId) == source) {
                    parseAndLoadModel(cachePayload, modelId, source.isAuth(), generation);
                }
            } finally {
                release();
            }
        }

        @Override
        public void cancel() {
            if (claimed.compareAndSet(false, true)) release();
        }

        private void release() {
            preparedModelSlots.release();
            loadingLazyModels.remove(modelId);
        }
    }

    private static final class LazyModelAssembly extends ModelAssembly {
        private final String modelId;
        private final ServerModelInfo modelInfo;
        private final ModelDisplayAssets displayAssets;
        private final ModelResourceBundle metadataResources;

        private LazyModelAssembly(String modelId, LazyModelSource source) {
            super(null, Map.of(), Map.of(), createLazyResourceBundle(source.modelInfo()), source.modelInfo(), new ModelDisplayAssets(source.modelInfo().getModelProperties().getDefaultTexture(), source.isAuth(), Map.of(), Map.of()), List.of());
            this.modelId = modelId;
            this.modelInfo = source.modelInfo();
            this.displayAssets = super.getTextureRegistry();
            this.metadataResources = super.getExpressionCache();
        }

        private static ModelResourceBundle createLazyResourceBundle(ServerModelInfo modelInfo) {
            return new ModelResourceBundle(Map.of(), new Object2ReferenceOpenHashMap<>(), new Object2ReferenceOpenHashMap<>(), modelInfo.getTranslations());
        }

        private ModelAssembly loadedAssembly() {
            ModelAssembly current = modelAssemblyMap.get(modelId);
            return current != null && current != this && !(current instanceof LazyModelAssembly) ? current : null;
        }

        private ModelAssembly requestAndGetFallback() {
            requestLazyModel(modelId);
            ModelAssembly loaded = loadedAssembly();
            if (loaded != null) return loaded;
            return localModelContext;
        }

        @Override
        public PlayerModelBundle getAnimationBundle() {
            ModelAssembly assembly = requestAndGetFallback();
            return assembly == null ? null : assembly.getAnimationBundle();
        }

        @Override
        public ModelResourceBundle getExpressionCache() {
            ModelAssembly assembly = loadedAssembly();
            return assembly == null ? metadataResources : assembly.getExpressionCache();
        }

        @Override
        public Map<ResourceLocation, ProjectileModelBundle> getProjectileModels() {
            requestLazyModel(modelId);
            ModelAssembly assembly = loadedAssembly();
            return assembly == null ? Map.of() : assembly.getProjectileModels();
        }

        @Override
        public Map<ResourceLocation, VehicleModelBundle> getVehicleModels() {
            requestLazyModel(modelId);
            ModelAssembly assembly = loadedAssembly();
            return assembly == null ? Map.of() : assembly.getVehicleModels();
        }

        @Override
        public ServerModelInfo getModelData() {
            ModelAssembly assembly = loadedAssembly();
            return assembly == null ? modelInfo : assembly.getModelData();
        }

        @Override
        public ModelDisplayAssets getTextureRegistry() {
            ModelAssembly assembly = loadedAssembly();
            return assembly == null ? displayAssets : assembly.getTextureRegistry();
        }

        @Override
        public List<AbstractTexture> getTextures() {
            ModelAssembly assembly = loadedAssembly();
            return assembly == null ? List.of() : assembly.getTextures();
        }
    }

    public static int getPendingModelCount() {
        return pendingModelQueue.size();
    }

    public static boolean isModelPending(String modelId) {
        if (!(modelAssemblyMap.get(modelId) instanceof LazyModelAssembly)) {
            return false;
        }
        requestLazyModel(modelId);
        return true;
    }

    public static class SyncStatus {
        private SyncState currentState = SyncState.WAITING;

        private int totalModels = -1;

        private int syncedModels = -1;

        public SyncState getCurrentState() {
            return this.currentState;
        }

        public int getSyncedModels() {
            return this.syncedModels;
        }

        public int getTotalModels() {
            return this.totalModels;
        }

        public void setState(SyncState syncState) {
            System.out.println("Sync state: " + syncState);
            this.currentState = syncState;
            this.totalModels = -1;
            this.syncedModels = -1;
        }

        public void startSyncing(int totalModels) {
            this.currentState = SyncState.SYNCING;
            this.totalModels = totalModels;
            this.syncedModels = 0;
        }
    }

    public static void exportAllCachedModels(@Nullable String extra, @Nullable Consumer<ExportResult> callback) {
        YSMThreadPool.submit(() -> {
            try {
                if (clientKey == null) {
                    if (callback != null) {
                        callback.accept(new ExportResult(false, Component.literal("未连接到服务器或尚未完成握手同步，无法获取客户端解密密钥。"), "", "", 0));
                    }
                    return;
                }

                String folder = currentCacheFolderName != null ? currentCacheFolderName : "default_cache";
                File cacheDir = ServerModelManager.CACHE_CLIENT.resolve(folder).toFile();

                if (!cacheDir.exists() || !cacheDir.isDirectory()) {
                    if (callback != null) {
                        callback.accept(new ExportResult(false, Component.literal("尚未生成任何缓存或缓存文件夹不存在: " + folder), "", "", 0));
                    }
                    return;
                }

                File[] files = cacheDir.listFiles();
                if (files == null || files.length == 0) {
                    if (callback != null) {
                        callback.accept(new ExportResult(false, Component.literal("缓存文件夹中没有任何模型可供导出。"), "", "", 0));
                    }
                    return;
                }

                int successCount = 0;
                for (File file : files) {
                    if (!file.isFile()) continue;

                    try {
                        byte[] fileBytes = Files.readAllBytes(file.toPath());
                        CachePayload cachePayload = YsmCrypt.readInPlace(fileBytes, clientKey);

                        int coreDataLength;
                        String exportName = file.getName(); // Fallback name

                        try (YSMBinaryDeserializer deserializer = new YSMBinaryDeserializer(cachePayload.data(), cachePayload.formatVersion())) {
                            RawYsmModel rawModel = deserializer.deserializeKeepOpen();
                            coreDataLength = deserializer.getReader().getRawBuf().readerIndex();

                            if (rawModel.metadata != null && rawModel.metadata.name != null && !rawModel.metadata.name.trim().isEmpty()) {
                                exportName = rawModel.metadata.name.trim();
                            } else if (rawModel.properties != null && rawModel.properties.sha256 != null && !rawModel.properties.sha256.isEmpty()) {
                                exportName = rawModel.properties.sha256;
                            }
                        }

                        exportName = exportName.replaceAll("[\\\\/:*?\"<>|]", "_");

                        try (YSMByteBuf outBuf = new YSMByteBuf(Unpooled.buffer())) {
                            outBuf.writeDword(cachePayload.formatVersion());

                            outBuf.getRawBuf().writeBytes(cachePayload.data(), 0, coreDataLength);

                            outBuf.writeVarInt(32); // Version
                            outBuf.writeVarInt(1);

                            byte[] randBytes = new byte[8];
                            SECURE_RANDOM.nextBytes(randBytes);
                            StringBuilder sb = new StringBuilder(16);
                            for (byte b : randBytes) {
                                sb.append(String.format("%02x", b));
                            }
                            outBuf.writeString(sb.toString()); // rand hash

                            outBuf.writeVarLong(java.time.Instant.now().getEpochSecond()); // time
                            outBuf.writeString(extra != null ? extra : ""); // extra info
                            outBuf.writeVarInt(0); // padding

                            byte[] rawBytes = new byte[outBuf.getRawBuf().readableBytes()];
                            outBuf.getRawBuf().readBytes(rawBytes);

                            byte[] finalEncrypted = YsmCrypt.encryptYsmFile(rawBytes);

                            Path exportPath = ServerModelManager.EXPORT.resolve(exportName + ".ysm");
                            Files.createDirectories(exportPath.getParent());
                            Files.write(exportPath, finalEncrypted);

                            successCount++;
                            YesSteveModel.LOGGER.info("[YSM] Successfully exported cached model to: " + exportPath);
                        }
                    } catch (Exception e) {
                        YesSteveModel.LOGGER.error("[YSM] Failed to export cached model: " + file.getName(), e);
                    }
                }

                if (callback != null) {
                    String displayPath = Paths.get("export").toString();
                    if (successCount > 0) {
                        callback.accept(new ExportResult(true, null, displayPath, "", 0));
                    } else {
                        callback.accept(new ExportResult(false, Component.literal("导出完成，但没有成功导出任何模型。可能是缓存已损坏。"), "", "", 0));
                    }
                }
            } catch (Exception e) {
                YesSteveModel.LOGGER.error("[YSM] Error during batch export", e);
                if (callback != null) {
                    callback.accept(new ExportResult(false, Component.literal("批量导出过程发生严重错误: " + e.getMessage()), "", "", 0));
                }
            }
        });
    }
}
