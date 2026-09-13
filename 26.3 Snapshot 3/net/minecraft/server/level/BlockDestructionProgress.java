package net.minecraft.server.level;

import net.minecraft.core.BlockPos;

public class BlockDestructionProgress implements Comparable<BlockDestructionProgress> {
   private final int id;
   private final BlockPos pos;
   private int progress;
   private long updatedRenderTick;

   public BlockDestructionProgress(final int id, final BlockPos pos) {
      this.id = id;
      this.pos = pos;
   }

   public int getId() {
      return this.id;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public void setProgress(int progress) {
      if (progress > 10) {
         progress = 10;
      }

      this.progress = progress;
   }

   public int getProgress() {
      return this.progress;
   }

   public void updateTick(final long tick) {
      this.updatedRenderTick = tick;
   }

   public long getUpdatedRenderTick() {
      return this.updatedRenderTick;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         BlockDestructionProgress that = (BlockDestructionProgress)o;
         return this.id == that.id;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Integer.hashCode(this.id);
   }

   public int compareTo(final BlockDestructionProgress o) {
      return this.progress != o.progress ? Integer.compare(this.progress, o.progress) : Integer.compare(this.id, o.id);
   }
}
