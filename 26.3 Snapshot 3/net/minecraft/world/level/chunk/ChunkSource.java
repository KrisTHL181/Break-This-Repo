package net.minecraft.world.level.chunk;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.io.IOException;
import java.util.function.BooleanSupplier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.jspecify.annotations.Nullable;

public abstract class ChunkSource implements AutoCloseable, LightChunkGetter {
   public @Nullable LevelChunk getChunk(final int x, final int z, final boolean loadOrGenerate) {
      return (LevelChunk)this.getChunk(x, z, ChunkStatus.FULL, loadOrGenerate);
   }

   public @Nullable LevelChunk getChunkNow(final int x, final int z) {
      return this.getChunk(x, z, false);
   }

   @Override
   public @Nullable LightChunk getChunkForLighting(final int x, final int z) {
      return this.getChunk(x, z, ChunkStatus.EMPTY, false);
   }

   public boolean hasChunk(final int x, final int z) {
      return this.getChunk(x, z, ChunkStatus.FULL, false) != null;
   }

   public abstract @Nullable ChunkAccess getChunk(int x, int z, ChunkStatus targetStatus, boolean loadOrGenerate);

   public abstract void tick(BooleanSupplier haveTime, final boolean tickChunks);

   public void onSectionEmptinessChanged(final int sectionX, final int sectionY, final int sectionZ, final boolean empty) {
   }

   public abstract String gatherStats();

   public abstract int getLoadedChunksCount();

   @Override
   public void close() throws IOException {
   }

   public abstract LevelLightEngine getLightEngine();

   public void setSpawnSettings(final boolean spawnEnemies) {
   }

   public boolean updateChunkForced(final ChunkPos pos, final boolean forced) {
      return false;
   }

   public LongSet getForceLoadedChunks() {
      return LongSet.of();
   }
}
