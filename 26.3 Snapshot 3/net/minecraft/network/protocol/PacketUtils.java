package net.minecraft.network.protocol;

import com.mojang.logging.LogUtils;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketProcessor;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class PacketUtils {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static <T extends PacketListener> void ensureRunningOnSameThread(final Packet<T> packet, final T listener, final ServerLevel level) throws RunningOnDifferentThreadException {
      ensureRunningOnSameThread(packet, listener, level.getServer().packetProcessor());
   }

   public static <T extends PacketListener> void ensureRunningOnSameThread(final Packet<T> packet, final T listener, final PacketProcessor packetProcessor) throws RunningOnDifferentThreadException {
      if (!packetProcessor.isSameThread()) {
         packetProcessor.scheduleIfPossible(listener, packet);
         throw RunningOnDifferentThreadException.RUNNING_ON_DIFFERENT_THREAD;
      }
   }

   public static <T extends PacketListener> ReportedException makeReportedException(final Exception cause, final Packet<T> packet, final T listener) {
      if (cause instanceof ReportedException re) {
         fillCrashReport(re.getReport(), listener, packet);
         return re;
      } else {
         CrashReport report = CrashReport.forThrowable(cause, "Main thread packet handler");
         fillCrashReport(report, listener, packet);
         return new ReportedException(report);
      }
   }

   public static <T extends PacketListener> void fillCrashReport(final CrashReport report, final T listener, final @Nullable Packet<T> packet) {
      if (packet != null) {
         CrashReportCategory details = report.addCategory("Incoming Packet");
         details.setDetail("Type", () -> packet.type().toString());
         details.setDetail("Is Terminal", () -> Boolean.toString(packet.isTerminal()));
         details.setDetail("Is Skippable", () -> Boolean.toString(packet.isSkippable()));
      }

      listener.fillCrashReport(report);
   }
}
