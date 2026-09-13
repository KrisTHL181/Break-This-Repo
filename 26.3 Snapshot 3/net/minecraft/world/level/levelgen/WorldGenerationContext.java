package net.minecraft.world.level.levelgen;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;

public class WorldGenerationContext {
   private final int minY;
   private final int height;
   private final int seaLevel;

   public static WorldGenerationContext of(final LevelAccessor level) {
      return level instanceof WorldGenLevel worldGenLevel
         ? new WorldGenerationContext(worldGenLevel.getLevel().getChunkSource().getGenerator(), level)
         : new WorldGenerationContext(level);
   }

   public WorldGenerationContext(final ChunkGenerator generator, final LevelHeightAccessor heightAccessor) {
      this.minY = Math.max(heightAccessor.getMinY(), generator.getMinY());
      this.height = Math.min(heightAccessor.getHeight(), generator.getGenDepth());
      this.seaLevel = generator.getSeaLevel();
   }

   private WorldGenerationContext(final LevelAccessor level) {
      this.minY = level.getMinY();
      this.height = level.getHeight();
      this.seaLevel = level.getSeaLevel();
   }

   public int getMinGenY() {
      return this.minY;
   }

   public int getGenDepth() {
      return this.height;
   }

   public int seaLevel() {
      return this.seaLevel;
   }
}
