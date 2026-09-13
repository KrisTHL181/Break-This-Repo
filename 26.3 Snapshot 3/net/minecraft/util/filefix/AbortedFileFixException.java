package net.minecraft.util.filefix;

import java.util.List;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.util.filefix.virtualfilesystem.FileMove;
import org.jspecify.annotations.Nullable;

public final class AbortedFileFixException extends FileFixException {
   private final List<FileMove> notRevertedMoves;

   public AbortedFileFixException(final Exception cause, final List<FileMove> notRevertedMoves, final @Nullable FileSystemCapabilities fileSystemCapabilities) {
      super(cause, fileSystemCapabilities);
      this.notRevertedMoves = notRevertedMoves;
   }

   public AbortedFileFixException(final Exception cause) {
      this(cause, List.of(), null);
   }

   public List<FileMove> notRevertedMoves() {
      return this.notRevertedMoves;
   }

   @Override
   protected CrashReport createCrashReport() {
      CrashReport crashReport = super.createCrashReport();
      CrashReportCategory failedReverts = crashReport.addCategory("Moves that failed to revert");

      for (int i = 0; i < this.notRevertedMoves.size(); i++) {
         FileMove notRevertedMove = this.notRevertedMoves.get(i);
         failedReverts.setDetail(String.valueOf(i), notRevertedMove.from() + " -> " + notRevertedMove.to());
      }

      return crashReport;
   }
}
