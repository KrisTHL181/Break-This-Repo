package net.minecraft.nbt;

import net.minecraft.CrashReport;
import net.minecraft.ReportedException;

public class ReportedNbtException extends ReportedException {
   public ReportedNbtException(final CrashReport report) {
      super(report);
   }
}
