package net.minecraft.server.level;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class ChunkLoadCounter {
   private final List<ChunkHolder> pendingChunks = new ArrayList<>();
   private int totalChunks;

   public void track(ServerLevel p_422667_, Runnable p_426227_) {
      ServerChunkCache serverchunkcache = p_422667_.getChunkSource();
      LongSet longset = new LongOpenHashSet();
      serverchunkcache.runDistanceManagerUpdates();
      serverchunkcache.chunkMap.allChunksWithAtLeastStatus(ChunkStatus.FULL).forEach(p_425959_ -> longset.add(p_425959_.getPos().toLong()));
      p_426227_.run();
      serverchunkcache.runDistanceManagerUpdates();
      serverchunkcache.chunkMap.allChunksWithAtLeastStatus(ChunkStatus.FULL).forEach(p_425935_ -> {
         if (!longset.contains(p_425935_.getPos().toLong())) {
            this.pendingChunks.add(p_425935_);
            this.totalChunks++;
         }
      });
   }

   public int readyChunks() {
      return this.totalChunks - this.pendingChunks();
   }

   public int pendingChunks() {
      this.pendingChunks.removeIf(p_427077_ -> p_427077_.getLatestStatus() == ChunkStatus.FULL);
      return this.pendingChunks.size();
   }

   public int totalChunks() {
      return this.totalChunks;
   }
}
