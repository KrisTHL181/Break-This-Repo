package com.mojang.jtracy;

import java.nio.ByteBuffer;

class TracyBindings {
    private TracyBindings() {
    }

    static native void startup();

    static native void shutdown();

    static native void markFrame(long handle);

    static native void markFrameStart(long handle);

    static native void markFrameEnd(long handle);

    static native int beginZone(String name, String function, String file, int line);

    static native int frameImage(ByteBuffer image, int width, int height, int offset, boolean flip);

    static native void endZone(int id);

    static native void addZoneText(int id, String text);

    static native void setZoneColor(int id, int color);

    static native void addZoneValue(int id, long value);

    static native long mallocNamed(long pool, long pointer, int size);

    static native long freeNamed(long pool, long pointer);

    static native void setThreadName(String name, int group);

    static native void plotValue(long handle, double value);

    static native long leakName(String name);

    static native void appInfo(String text);

    static native void message(String text);

    static native void messageColored(String text, int color);

    static native void newGpuContext(int contextId, long gpuTime, float period, int flags, int type);

    static native void setGpuContextName(int contextId, String name);

    static native int beginGpuZone(int contextId, int queryId, String name, String function, String file, int line);

    static native int endGpuZone(int contextId, int queryId);

    static native int submitQueryTimestamp(int contextId, int queryId, long timestamp);
}
