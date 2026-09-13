package net.minecraft.util.filefix;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;

public final class FailedCleanupFileFixException extends FileFixException {
   private final String newWorldFolderName;

   public FailedCleanupFileFixException(final Exception cause, final String newWorldFolderName, final FileSystemCapabilities fileSystemCapabilities) {
      super(cause, fileSystemCapabilities);
      this.newWorldFolderName = newWorldFolderName;
   }

   @Override
   protected CrashReport createCrashReport() {
      CrashReport crashReport = super.createCrashReport();
      CrashReportCategory worldUpgrade = crashReport.addCategory("World upgrade");
      worldUpgrade.setDetail("New Name", this.newWorldFolderName);
      return crashReport;
   }

   public String newWorldFolderName() {
      return this.newWorldFolderName;
   }
}
