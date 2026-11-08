package com.elfmcys.yesstevemodel.client.upload;

import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.C2SModelUploadChunkPacket;
import com.elfmcys.yesstevemodel.network.message.C2SModelUploadFinishPacket;
import com.elfmcys.yesstevemodel.network.message.C2SModelUploadStartPacket;
import com.elfmcys.yesstevemodel.util.DigestUtil;

import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ModelUploadSession {
    private static final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    private static volatile ModelUploadSession instance;
    private static volatile boolean serverLimitsKnown = false;
    private static volatile int lastMaxTotalBytes = 16777216; // 16MB
    private static volatile int lastChunksPerTick = 4;
    private final String modelId;
    private final byte[] data;
    private final String sha256;
    private volatile State state = State.STARTING;
    private volatile long uploadId = 0L;
    private volatile int chunkSize = 32000;
    private volatile int chunksPerTick = 4;
    private volatile int nextOffset = 0;
    private volatile String message = "";

    private ModelUploadSession(String modelId, byte[] data) {
        this.modelId = modelId;
        this.data = data;
        this.sha256 = DigestUtil.sha256Hex(data);
    }

    public static ModelUploadSession getInstance() {
        return instance;
    }

    public static synchronized String start(String modelId, byte[] data) {
        if (instance != null && !instance.isTerminal()) {
            return "Upload already in progress";
        }
        if (data.length == 0) {
            return "Empty file";
        }
        if (serverLimitsKnown && data.length > lastMaxTotalBytes) {
            return "File exceeds server limit (" + formatBytes(lastMaxTotalBytes) + ")";
        }
        if (!isYsmFile(data)) {
            return "Invalid file type!";
        }
        ModelUploadSession session = new ModelUploadSession(modelId, data);
        instance = session;
        notifyListeners();
        NetworkHandler.sendToServer(new C2SModelUploadStartPacket(modelId, data.length, session.sha256));
        return null;
    }

    public static boolean hasServerLimits() {
        return serverLimitsKnown;
    }

    public static int getLastMaxTotalBytes() {
        return lastMaxTotalBytes;
    }

    public static int getLastChunksPerTick() {
        return lastChunksPerTick;
    }

    public static String formatBytes(int bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

    public static synchronized void clearIfTerminal() {
        if (instance != null && instance.isTerminal()) {
            instance = null;
            notifyListeners();
        }
    }

    public static void addListener(Listener l) {
        listeners.add(l);
    }

    public static void removeListener(Listener l) {
        listeners.remove(l);
    }

    public static synchronized void onStartAck(long uploadId, byte status, int chunkSize, int maxTotalBytes, int chunksPerTick, String message) {
        if (maxTotalBytes > 0) {
            lastMaxTotalBytes = maxTotalBytes;
        }
        if (chunksPerTick > 0) {
            lastChunksPerTick = chunksPerTick;
        }
        serverLimitsKnown = true;
        ModelUploadSession s = instance;
        if (s == null || s.state != State.STARTING) {
            return;
        }
        if (status != 0) {
            s.fail(getRequestErrorText(status) + (message.isEmpty() ? "" : ": " + message));
            return;
        }
        s.uploadId = uploadId;
        s.chunkSize = Math.max(1, chunkSize);
        s.chunksPerTick = Math.max(1, chunksPerTick);
        s.state = State.UPLOADING;
        s.message = "Uploading…";
        notifyListeners();
    }

    public static synchronized void onResult(long uploadId, byte status, String modelId, long h1, long h2, String message) {
        ModelUploadSession s = instance;
        if (s == null || s.uploadId != uploadId) {
            return;
        }
        if (status == 0) {
            s.state = State.COMPLETED;
            s.message = "Uploaded as " + modelId;
        } else {
            s.fail(getResponseErrorText(status) + (message.isEmpty() ? "" : ": " + message));
        }
        notifyListeners();
    }

    public static void tickCurrent() {
        ModelUploadSession s = instance;
        if (s == null) {
            return;
        }
        s.tick();
    }

    private static void notifyListeners() {
        ModelUploadSession s = instance;
        for (Listener l : listeners) {
            l.onSessionUpdate(s);
        }
    }

    private static boolean isYsmFile(byte[] data) {
        byte[] ysmHeader = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 0x59, 0x53, 0x47, 0x50};
        if (data.length < ysmHeader.length) {
            return false;
        }
        for (int i = 0; i < ysmHeader.length; i++) {
            if (data[i] != ysmHeader[i]) {
                return false;
            }
        }
        return true;
    }

    private static String getRequestErrorText(byte status) {
        return switch (status) {
            case 1 -> "Model ID already exists";
            case 2 -> "File exceeds server limit";
            case 3 -> "No upload permission";
            case 4 -> "Server busy, try again later";
            case 5 -> "Invalid model ID or hash";
            case 6 -> "Uploads disabled on server";
            default -> "error: " + status;
        };
    }

    private static String getResponseErrorText(byte status) {
        return switch (status) {
            case 1 -> "Hash mismatch";
            case 2 -> "Server failed to parse model";
            case 3 -> "Server storage error";
            case 4 -> "Session expired";
            case 5 -> "Incomplete upload";
            case 6 -> "Server rejected write";
            default -> "error: " + status;
        };
    }

    private synchronized void tick() {
        if (state != State.UPLOADING) {
            return;
        }
        int budget = Math.max(1, chunksPerTick);
        for (int i = 0; i < budget && nextOffset < data.length; i++) {
            int end = Math.min(nextOffset + chunkSize, data.length);
            byte[] slice = Arrays.copyOfRange(data, nextOffset, end);
            NetworkHandler.sendToServer(new C2SModelUploadChunkPacket(uploadId, nextOffset, slice));
            nextOffset = end;
        }
        if (nextOffset >= data.length) {
            state = State.FINISHING;
            message = "Verifying…";
            NetworkHandler.sendToServer(new C2SModelUploadFinishPacket(uploadId));
        }
        notifyListeners();
    }

    private void fail(String reason) {
        state = State.FAILED;
        message = reason;
    }

    public boolean isTerminal() {
        return state == State.COMPLETED || state == State.FAILED;
    }

    public State getState() {
        return state;
    }

    public String getModelId() {
        return modelId;
    }

    public int getTotalBytes() {
        return data.length;
    }

    public int getSentBytes() {
        return Math.min(nextOffset, data.length);
    }

    public String getMessage() {
        return message;
    }

    public float getProgress() {
        if (data.length == 0) {
            return 1f;
        }
        if (state == State.COMPLETED) {
            return 1f;
        }
        return (float) getSentBytes() / data.length;
    }

    public enum State {STARTING, UPLOADING, FINISHING, COMPLETED, FAILED}

    public interface Listener {
        void onSessionUpdate(ModelUploadSession session);
    }
}
