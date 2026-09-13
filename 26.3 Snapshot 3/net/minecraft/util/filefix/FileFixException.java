package net.minecraft.util.filefix;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import org.jspecify.annotations.Nullable;

public class FileFixException extends RuntimeException {
   protected final @Nullable FileSystemCapabilities fileSystemCapabilities;

   public FileFixException(final @Nullable Exception cause, final @Nullable FileSystemCapabilities fileSystemCapabilities) {
      super(cause);
      this.fileSystemCapabilities = fileSystemCapabilities;
   }

   protected CrashReport createCrashReport() {
      CrashReport crashReport = CrashReport.forThrowable(this, "Upgrading world failed with errors");
      CrashReportCategory fsCapabilities = crashReport.addCategory("File system capabilities");
      fsCapabilities.setDetail("Hard Links", this.fileSystemCapabilities == null ? "null" : this.fileSystemCapabilities.hardLinks());
      fsCapabilities.setDetail("Atomic Move", this.fileSystemCapabilities == null ? "null" : this.fileSystemCapabilities.atomicMove());
      return crashReport;
   }

   public ReportedException makeReportedException() {
      return new ReportedException(this.createCrashReport());
   }
}
