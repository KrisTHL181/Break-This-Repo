package com.mojang.jtracy;

public class MemoryPool {
    static final MemoryPool UNAVAILABLE = new MemoryPool(0);

    private final long id;

    MemoryPool(final long id) {
        this.id = id;
    }

    /**
     * Inform Tracy of a malloc in this memory pool.
     */
    public void malloc(final long pointer, final int size) {
        if (this != UNAVAILABLE) {
            TracyBindings.mallocNamed(id, pointer, size);
        }
    }

    /**
     * Inform Tracy of a free in this memory pool.
     */
    public void free(final long pointer) {
        if (this != UNAVAILABLE) {
            TracyBindings.freeNamed(id, pointer);
        }
    }
}
