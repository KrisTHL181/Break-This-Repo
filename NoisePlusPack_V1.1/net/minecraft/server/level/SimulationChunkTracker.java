package net.minecraft.server.level;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;

public class SimulationChunkTracker extends ChunkTracker {
   public static final int MAX_LEVEL = 33;
   protected final Long2ByteMap chunks = new Long2ByteOpenHashMap();
   private final TicketStorage ticketStorage;

   public SimulationChunkTracker(TicketStorage p_395447_) {
      super(34, 16, 256);
      this.ticketStorage = p_395447_;
      p_395447_.setSimulationChunkUpdatedListener(this::update);
      this.chunks.defaultReturnValue((byte)33);
   }

   @Override
   protected int getLevelFromSource(long p_395160_) {
      return this.ticketStorage.getTicketLevelAt(p_395160_, true);
   }

   public int getLevel(ChunkPos p_396184_) {
      return this.getLevel(p_396184_.toLong());
   }

   @Override
   protected int getLevel(long p_397279_) {
      return this.chunks.get(p_397279_);
   }

   @Override
   protected void setLevel(long p_393143_, int p_394676_) {
      if (p_394676_ >= 33) {
         this.chunks.remove(p_393143_);
      } else {
         this.chunks.put(p_393143_, (byte)p_394676_);
      }
   }

   public void runAllUpdates() {
      this.runUpdates(Integer.MAX_VALUE);
   }
}
