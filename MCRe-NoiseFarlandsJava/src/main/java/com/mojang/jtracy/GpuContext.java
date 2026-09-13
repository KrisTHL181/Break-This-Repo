package com.mojang.jtracy;

public class GpuContext {
    static final GpuContext UNAVAILABLE = new GpuContext(0);

    private final int id;

    GpuContext(final int id) {
        this.id = id;
    }

    /**
     * Sets a name of this context.
     *
     * @param name Name of this context
     * @return This context, for builder-style creation
     */
    public GpuContext setName(final String name) {
        if (this != UNAVAILABLE) {
            TracyBindings.setGpuContextName(id, name);
        }
        return this;
    }

    /**
     * Marks the beginning of a GPU zone.
     * <p>
     * Query is an arbitrary value that must be unique per frame. You are not permitted to reuse a query ID until it is later freed with {@link #submitQueryTimestamp(int, long)}.
     * <p>
     * You are expected to record a GPU timestamp as close as possible to this function, and then call {@link #submitQueryTimestamp(int, long)}.
     * <p>
     * Unlike with CPU Zones, GPU zones are started and ended as a stack. You do not explicitly mark _which_ zone you're ending,
     * so calling this and {@link #endZone(int)} correctly is vitally important and errors will not be caught for you.
     *
     * @param query    A unique ID to associate the start of this zone with a gpu timestamp later
     * @param name     Name of the zone to display
     * @param function Name of the function that this zone belongs to
     * @param file     Name of the file that this zone belongs to
     * @param line     Line number of the file that this zone belongs to
     */
    public void beginZone(final int query, final String name, final String function, final String file, final int line) {
        if (this != UNAVAILABLE) {
            TracyBindings.beginGpuZone(id, query, name, function, file, line);
        }
    }

    /**
     * Marks the end of a GPU zone.
     * <p>
     * Query is an arbitrary value that must be unique per frame. You are not permitted to reuse a query ID until it is later freed with {@link #submitQueryTimestamp(int, long)}.
     * <p>
     * You are expected to record a GPU timestamp as close as possible to this function, and then call {@link #submitQueryTimestamp(int, long)}.
     * <p>
     * Unlike with CPU Zones, GPU zones are started and ended as a stack. You do not explicitly mark _which_ zone you're ending,
     * so calling this and {@link #beginZone(int, String, String, String, int)} correctly is vitally important and errors will not be caught for you.
     *
     * @param query A unique ID to associate the end of a zone with a gpu timestamp later
     */
    public void endZone(final int query) {
        if (this != UNAVAILABLE) {
            TracyBindings.endGpuZone(id, query);
        }
    }

    /**
     * Submits a timestamp for a query, freeing the ID for reuse.
     *
     * @param query     Query ID previously generated for a zone's start or end.
     * @param timestamp GPU timestamp corresponding to that query ID.
     */
    public void submitQueryTimestamp(final int query, final long timestamp) {
        if (this != UNAVAILABLE) {
            TracyBindings.submitQueryTimestamp(id, query, timestamp);
        }
    }
}
