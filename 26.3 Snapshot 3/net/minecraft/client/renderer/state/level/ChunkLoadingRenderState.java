package net.minecraft.client.renderer.state.level;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public class ChunkLoadingRenderState {
   public LongOpenHashSet addedEmptySections = new LongOpenHashSet();
   public LongOpenHashSet removedEmptySections = new LongOpenHashSet();
   public LongOpenHashSet addedLoadedChunks = new LongOpenHashSet();
   public LongOpenHashSet removedLoadedChunks = new LongOpenHashSet();
   public LongOpenHashSet loadedExpectedChunks = new LongOpenHashSet();

   public void reset() {
      this.loadedExpectedChunks.clear();
   }
}
