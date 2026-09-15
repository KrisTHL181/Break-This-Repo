package com.mojang.jtracy;

public class Plot {
    static final Plot UNAVAILABLE = new Plot(0);

    private final long handle;

    Plot(final long handle) {
        this.handle = handle;
    }

    public void setValue(final double value) {
        if (this != UNAVAILABLE) {
            TracyBindings.plotValue(handle, value);
        }
    }
}
