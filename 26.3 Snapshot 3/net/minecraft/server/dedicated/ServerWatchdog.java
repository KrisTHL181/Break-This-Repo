package net.minecraft.server.dedicated;

import com.google.common.collect.Streams;
import com.mojang.logging.LogUtils;
import java.lang.management.ThreadInfo;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportType;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.Util;
import net.minecraft.world.level.gamerules.GameRules;
import org.slf4j.Logger;

public class ServerWatchdog implements Runnable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final long MAX_SHUTDOWN_TIME = 10000L;
   private static final int SHUTDOWN_STATUS = 1;
   private static final Comparator<ThreadInfo> THREAD_INFO_COMPARATOR = Comparator.comparing(ThreadInfo::isDaemon)
      .thenComparing(ThreadInfo::getThreadState)
      .thenComparing(ThreadInfo::getThreadName);
   private final DedicatedServer server;
   private final long maxTickTimeNanos;

   public ServerWatchdog(final DedicatedServer server) {
      this.server = server;
      this.maxTickTimeNanos = server.getMaxTickLength() * TimeUtil.NANOSECONDS_PER_MILLISECOND;
   }

   @Override
   public void run() {
      while (this.server.isRunning()) {
         long nextTickTimeNanos = this.server.getNextTickTime();
         long currentTimeNanos = Util.getNanos();
         long deltaNanos = currentTimeNanos - nextTickTimeNanos;
         if (deltaNanos > this.maxTickTimeNanos) {
            LOGGER.error(
               LogUtils.FATAL_MARKER,
               "A single server tick took {} seconds (should be max {})",
               String.format(Locale.ROOT, "%.2f", (float)deltaNanos / (float)TimeUtil.NANOSECONDS_PER_SECOND),
               String.format(Locale.ROOT, "%.2f", this.server.tickRateManager().millisecondsPerTick() / (float)TimeUtil.MILLISECONDS_PER_SECOND)
            );
            LOGGER.error(LogUtils.FATAL_MARKER, "Considering it to be crashed, server will forcibly shutdown.");
            CrashReport report = createWatchdogCrashReport("Watching Server", this.server.getRunningThread().threadId());
            this.server.fillSystemReport(report.getSystemReport());
            CrashReportCategory serverStats = report.addCategory("Performance stats");
            serverStats.setDetail("Random tick rate", () -> this.server.getGameRules().getAsString(GameRules.RANDOM_TICK_SPEED));
            serverStats.setDetail(
               "Level stats",
               () -> Streams.stream(this.server.getAllLevels())
                  .map(level -> level.dimension().identifier() + ": " + level.getWatchdogStats())
                  .collect(Collectors.joining(",\n"))
            );
            Bootstrap.realStdoutPrintln("Crash report:\n" + report.getFriendlyReport(ReportType.CRASH));
            Path file = this.server.getServerDirectory().resolve("crash-reports").resolve("crash-" + Util.getFilenameFormattedDateTime() + "-server.txt");
            if (report.saveToFile(file, ReportType.CRASH)) {
               LOGGER.error("This crash report has been saved to: {}", file.toAbsolutePath());
            } else {
               LOGGER.error("We were unable to save this crash report to disk.");
            }

            this.exit();
         }

         try {
            Thread.sleep((nextTickTimeNanos + this.maxTickTimeNanos - currentTimeNanos) / TimeUtil.NANOSECONDS_PER_MILLISECOND);
         } catch (InterruptedException var10) {
         }
      }
   }

   public static CrashReport createWatchdogCrashReport(final String message, final long mainThreadId) {
      ThreadInfo[] threadInfos = Util.dumpThreadInfo();
      Arrays.sort(threadInfos, THREAD_INFO_COMPARATOR);
      StringBuilder builder = new StringBuilder();
      Error exception = new Error("Watchdog (" + message + ")");

      for (ThreadInfo threadInfo : threadInfos) {
         if (threadInfo.getThreadId() == mainThreadId) {
            exception.setStackTrace(threadInfo.getStackTrace());
         }

         builder.append("\n");
         builder.append(threadInfo);
      }

      CrashReport report = new CrashReport(message, exception);
      CrashReportCategory threadDump = report.addCategory("Thread Dump");
      threadDump.setDetail("Threads", builder);
      return report;
   }

   private void exit() {
      try {
         Timer timer = new Timer();
         timer.schedule(new TimerTask() {
            @Override
            public void run() {
               Runtime.getRuntime().halt(1);
            }
         }, 10000L);
         System.exit(1);
      } catch (Throwable ignored) {
         Runtime.getRuntime().halt(1);
      }
   }
}
