package com.mojang.jtracy;

public class ContinuousFrame {
    static final ContinuousFrame UNAVAILABLE = new ContinuousFrame(0);

    private final long id;

    ContinuousFrame(final long id) {
        this.id = id;
    }

    /**
     * Mark the boundary between two frames.
     */
    public void mark() {
        if (this != UNAVAILABLE) {
            TracyBindings.markFrame(id);
        }
    }
}
