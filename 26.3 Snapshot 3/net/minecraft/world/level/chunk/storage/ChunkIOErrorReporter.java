package net.minecraft.world.level.chunk.storage;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.world.level.ChunkPos;

public interface ChunkIOErrorReporter {
   void reportChunkLoadFailure(Throwable throwable, RegionStorageInfo storageInfo, ChunkPos pos);

   void reportChunkSaveFailure(Throwable throwable, RegionStorageInfo storageInfo, ChunkPos pos);

   static ReportedException createMisplacedChunkReport(final ChunkPos storedPos, final ChunkPos requestedPos) {
      CrashReport report = CrashReport.forThrowable(
         new IllegalStateException("Retrieved chunk position " + storedPos + " does not match requested " + requestedPos), "Chunk found in invalid location"
      );
      CrashReportCategory category = report.addCategory("Misplaced Chunk");
      category.setDetail("Stored Position", storedPos::toString);
      return new ReportedException(report);
   }

   default void reportMisplacedChunk(final ChunkPos storedPos, final ChunkPos requestedPos, final RegionStorageInfo storageInfo) {
      this.reportChunkLoadFailure(createMisplacedChunkReport(storedPos, requestedPos), storageInfo, requestedPos);
   }
}
