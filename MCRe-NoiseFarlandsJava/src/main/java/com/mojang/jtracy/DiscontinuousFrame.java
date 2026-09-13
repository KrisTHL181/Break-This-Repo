package com.mojang.jtracy;

public class DiscontinuousFrame {
    static final DiscontinuousFrame UNAVAILABLE = new DiscontinuousFrame(0);

    private final long id;

    DiscontinuousFrame(final long id) {
        this.id = id;
    }

    /**
     * Begin this frame.
     * This method must be called before the next call to end().
     */
    public void start() {
        if (this != UNAVAILABLE) {
            TracyBindings.markFrameStart(id);
        }
    }

    /**
     * End this frame.
     * This method must be called before the next call to start().
     */
    public void end() {
        if (this != UNAVAILABLE) {
            TracyBindings.markFrameEnd(id);
        }
    }
}
