package net.minecraft.world.level.levelgen;

import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;

public class WorldGenerationContext {
    private final int minY;
    private final int height;

    public WorldGenerationContext(final ChunkGenerator generator, final LevelHeightAccessor heightAccessor) {
        this.minY = Math.max(heightAccessor.getMinY(), generator.getMinY());
        this.height = Math.min(heightAccessor.getHeight(), generator.getGenDepth());
    }

    public int getMinGenY() {
        return this.minY;
    }

    public int getGenDepth() {
        return this.height;
    }
}