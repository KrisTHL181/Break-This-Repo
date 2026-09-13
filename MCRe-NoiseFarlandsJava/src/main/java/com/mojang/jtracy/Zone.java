package com.mojang.jtracy;

public class Zone implements AutoCloseable {
    static final Zone UNAVAILABLE = new Zone(0);

    private final int id;

    Zone(final int id) {
        this.id = id;
    }

    /**
     * Associate some text with this zone.
     * Multiple texts can be added to the same zone.
     *
     * @param text Text to associate
     * @return This zone, for builder-style creation
     */
    public Zone addText(final String text) {
        if (this != UNAVAILABLE) {
            TracyBindings.addZoneText(id, text);
        }
        return this;
    }

    /**
     * Associate a specific color with this zone.
     * <p>
     * Color should be in 0xRRGGBB style.
     * A value of 0 means "no color", rather than black. Use (e.g.) 0x000001 for black.
     * Only one color may be attached to the zone.
     *
     * @param color Color to associate
     * @return This zone, for builder-style creation
     */
    public Zone setColor(final int color) {
        if (this != UNAVAILABLE) {
            TracyBindings.setZoneColor(id, color);
        }
        return this;
    }

    /**
     * Adds a value to this zone.
     * Multiple values may be added to a zone.
     * <p>
     * This is similar to adding a text with the specific value, but is optimised for numbers.
     *
     * @param value Value to associate
     * @return This zone, for builder-style creation
     */
    public Zone addValue(final long value) {
        if (this != UNAVAILABLE) {
            TracyBindings.addZoneValue(id, value);
        }
        return this;
    }

    @Override
    public void close() {
        if (this != UNAVAILABLE) {
            TracyBindings.endZone(id);
        }
    }
}
