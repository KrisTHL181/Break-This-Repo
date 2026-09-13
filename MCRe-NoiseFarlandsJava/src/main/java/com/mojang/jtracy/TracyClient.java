package com.mojang.jtracy;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class TracyClient {
    private static boolean loaded = false;
    private static AtomicInteger lastGpuContextId = new AtomicInteger(0);

    /**
     * Returns {@code true} if the Tracy client is available.
     * <p>
     * This will only be the case if the natives have been loaded successfully.
     * This will not be the case until the first call to {@link TracyClient#load()},
     * and only if that call was successful.
     *
     * @return {@code true} if Tracy is available
     */
    public static boolean isAvailable() {
        return loaded;
    }

    /**
     * Attempt to load the Tracy client.
     * <p>
     * This will only function if the client is so far unavailable. Repeated calls after it's loaded will do nothing.
     * If the library fails to load, it will throw an exception.
     *
     * @throws UnsatisfiedLinkError Any appropriate error that caused the library to not load.
     */
    public static synchronized void load() throws UnsatisfiedLinkError {
        if (!loaded) {
            new Loader().load();
            loaded = true;
        }
    }

    /***
     * Mark the boundary between two render frames.
     * This ideally should be after the swap buffers command, when all rendering is submitted to the gpu.
     */
    public static void markFrame() {
        if (loaded) {
            TracyBindings.markFrame(0);
        }
    }

    /***
     * Upload an image representing a captured frame.
     * <p>
     * It is strongly recommended to use a small an image as possible; ideally 320x180.
     * A large image (256 KB after compression) may not be accepted.
     *
     * @param image The image to upload. It <b>must</b> be a direct buffer.
     * @param width Width of the image, in pixels. It <b>must</b> be divisible by 4.
     * @param height Height of the image, in pixels. It <b>must</b> be divisible by 4.
     * @param offset The offset of this image from the <b>current</b> frame. 0 means this frame, 1 is last frame, etc.
     * @param flip If this image is upside-down (e.g. OpenGL)
     */
    public static void frameImage(final ByteBuffer image, final int width, final int height, final int offset, final boolean flip) {
        if (loaded) {
            TracyBindings.frameImage(image, width, height, offset, flip);
        }
    }

    /**
     * Begin a zone with the given name.
     * You must end the zone after it is finished, in the correct (reverse) order they were created.
     *
     * @param name          Name of the zone to display
     * @param captureSource Whether to capture the callers source location. This adds more overhead.
     * @return A handle to a zone that you can end
     */
    public static Zone beginZone(final String name, final boolean captureSource) {
        if (loaded) {
            String function = "";
            String file = "";
            int line = 0;
            if (captureSource) {
                final StackWalker walker = StackWalker.getInstance(Set.of(StackWalker.Option.RETAIN_CLASS_REFERENCE), 2);
                Optional<StackWalker.StackFrame> result = walker.walk(s -> s.filter(frame -> frame.getDeclaringClass() != TracyClient.class).findFirst());
                if (result.isPresent()) {
                    StackWalker.StackFrame frame = result.get();
                    function = frame.getMethodName();
                    file = frame.getFileName();
                    line = frame.getLineNumber();
                }
            }
            return new Zone(TracyBindings.beginZone(name, function, file, line));
        } else {
            return Zone.UNAVAILABLE;
        }
    }

    /**
     * Begin a zone with the given name and source location.
     *
     * @param name     Name of the zone to display
     * @param function Name of the function that this zone belongs to
     * @param file     Name of the file that this zone belongs to
     * @param line     Line number of the file that this zone belongs to
     * @return A handle to a zone that you can end
     */
    public static Zone beginZone(final String name, final String function, final String file, final int line) {
        if (loaded) {
            return new Zone(TracyBindings.beginZone(name, function, file, line));
        } else {
            return Zone.UNAVAILABLE;
        }
    }

    /**
     * Set the name of the current thread in Tracy.
     *
     * @param name  Name of the thread to display
     * @param group An arbitrary group id; 0 means no group!
     */
    public static void setThreadName(final String name, final int group) {
        if (loaded) {
            TracyBindings.setThreadName(name, group);
        }
    }

    /**
     * Creates a {@link Plot plot} with the given name.
     * Names <em>should</em> be unique, you should reuse the object returned instead of creating multiple {@link Plot plots} for the same name.
     *
     * @param name Name of the plot
     * @return A plot that you can interact with
     * @see Plot#setValue(double)
     */
    public static Plot createPlot(final String name) {
        if (loaded) {
            return new Plot(TracyBindings.leakName(name));
        } else {
            return Plot.UNAVAILABLE;
        }
    }

    /**
     * Creates a secondary {@link DiscontinuousFrame discontinuous frame} type with the given name.
     * Names <em>should</em> be unique, you should reuse the object returned instead of creating multiple {@link DiscontinuousFrame discontinuous frames} for the same name.
     * <p>
     * Unlike {@link ContinuousFrame continuous frames}, {@link DiscontinuousFrame discontinuous frames} must have their
     * beginning and end explicitly marked, and there may be gaps.
     *
     * @param name Name of the frame
     * @return A frame that you can interact with
     * @see DiscontinuousFrame#start()
     * @see DiscontinuousFrame#end()
     */
    public static DiscontinuousFrame createDiscontinuousFrame(final String name) {
        if (loaded) {
            return new DiscontinuousFrame(TracyBindings.leakName(name));
        } else {
            return DiscontinuousFrame.UNAVAILABLE;
        }
    }

    /**
     * Creates a secondary {@link ContinuousFrame continuous frame} type with the given name.
     * Names <em>should</em> be unique, you should reuse the object returned instead of creating multiple {@link ContinuousFrame continuous frames} for the same name.
     * <p>
     * Unlike {@link DiscontinuousFrame discontinuous frames}, {@link ContinuousFrame continuous frames} have no gaps,
     * and marking the end of one frame is immediately followed by the beginning of the next.
     *
     * @param name Name of the frame
     * @return A frame that you can interact with
     * @see ContinuousFrame#mark()
     */
    public static ContinuousFrame createContinuousFrame(final String name) {
        if (loaded) {
            return new ContinuousFrame(TracyBindings.leakName(name));
        } else {
            return ContinuousFrame.UNAVAILABLE;
        }
    }

    /**
     * Creates a pool of memory with the given name.
     * Names <em>should</em> be unique, you should reuse the object returned instead of creating multiple {@link MemoryPool MemoryPools} for the same name.
     * <p>
     * This pool will not allocate, but should be used to inform Tracy of real allocations made by the application.
     *
     * @param name Name of the pool of memory
     * @return A pool that you can interact with
     */
    public static MemoryPool createMemoryPool(final String name) {
        if (loaded) {
            return new MemoryPool(TracyBindings.leakName(name));
        } else {
            return MemoryPool.UNAVAILABLE;
        }
    }

    /**
     * Records any information about this application into the profiled data.
     * <p>
     * This can be useful for recording the version, whether debugging is enabled, etc.
     *
     * @param text Any information you wish to record
     */
    public static void reportAppInfo(final String text) {
        if (loaded) {
            TracyBindings.appInfo(text);
        }
    }

    /**
     * Records a message in Tracy's timeline.
     * This might be used, for example, to capture the debug output of your application.
     *
     * @param text Message text to log
     * @see TracyClient#message(String, int)
     */
    public static void message(final String text) {
        if (loaded) {
            TracyBindings.message(text);
        }
    }

    /**
     * Records a colored message in Tracy's timeline.
     * This might be used, for example, to capture the debug output of your application.
     * <p>
     * Color should be in 0xRRGGBB style.
     *
     * @param text  Message text to log
     * @param color Color for this message
     * @see TracyClient#message(String)
     */
    public static void message(final String text, final int color) {
        if (loaded) {
            TracyBindings.messageColored(text, color);
        }
    }

    /**
     * Records a message in Tracy's timeline.
     * This might be used, for example, to capture the debug output of your application.
     *
     * @param text {@link Supplier} to provide the message text to log
     * @see TracyClient#message(Supplier, int)
     */
    public static void message(final Supplier<String> text) {
        if (loaded) {
            TracyBindings.message(text.get());
        }
    }

    /**
     * Records a colored message in Tracy's timeline.
     * This might be used, for example, to capture the debug output of your application.
     * <p>
     * Color should be in 0xRRGGBB style.
     *
     * @param text  {@link Supplier} to provide the message text to log
     * @param color Color for this message
     * @see TracyClient#message(Supplier)
     */
    public static void message(final Supplier<String> text, final int color) {
        if (loaded) {
            TracyBindings.messageColored(text.get(), color);
        }
    }

    /**
     * Creates a new GPU context that you can use to record GPU zones on.
     * <p>
     * Reuse the GpuContext returned by this object, as each context is unique.
     * Multiple GPU contexts can live at the same time, but no more than 255.
     *
     * @see TracyClient#message(Supplier)
     */
    public static GpuContext createGpuContext(final GpuApi api, final long gpuTimestamp, final float gpuPeriod) {
        if (loaded) {
            final int id = lastGpuContextId.incrementAndGet();
            if (id == 255) {
                throw new UnsupportedOperationException("Too many GPU contexts were created");
            }
            TracyBindings.newGpuContext(id, gpuTimestamp, gpuPeriod, 0, api.getId());
            return new GpuContext(id);
        } else {
            return GpuContext.UNAVAILABLE;
        }
    }
}