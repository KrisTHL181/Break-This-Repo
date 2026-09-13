package net.minecraft.util.profiling.jfr;

import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Supplier;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.profiling.jfr.parse.JfrStatsParser;
import net.minecraft.util.profiling.jfr.parse.JfrStatsResult;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class SummaryReporter {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Runnable onDeregistration;

   protected SummaryReporter(final Runnable onDeregistration) {
      this.onDeregistration = onDeregistration;
   }

   public void recordingStopped(final @Nullable Path result) {
      if (result != null) {
         this.onDeregistration.run();
         infoWithFallback(() -> "Dumped flight recorder profiling to " + result);

         JfrStatsResult statsResult;
         try {
            statsResult = JfrStatsParser.parse(result);
         } catch (Throwable t) {
            warnWithFallback(() -> "Failed to parse JFR recording", t);
            return;
         }

         try {
            infoWithFallback(statsResult::asJson);
            Path jsonReport = result.resolveSibling("jfr-report-" + StringUtils.substringBefore(result.getFileName().toString(), ".jfr") + ".json");
            Files.writeString(jsonReport, statsResult.asJson(), StandardOpenOption.CREATE);
            infoWithFallback(() -> "Dumped recording summary to " + jsonReport);
         } catch (Throwable t) {
            warnWithFallback(() -> "Failed to output JFR report", t);
         }
      }
   }

   private static void infoWithFallback(final Supplier<String> message) {
      if (LogUtils.isLoggerActive()) {
         LOGGER.info("{}", message.get());
      } else {
         Bootstrap.realStdoutPrintln(message.get());
      }
   }

   private static void warnWithFallback(final Supplier<String> message, final Throwable t) {
      if (LogUtils.isLoggerActive()) {
         LOGGER.warn("{}", message.get(), t);
      } else {
         Bootstrap.realStdoutPrintln(message.get());
         t.printStackTrace(Bootstrap.STDOUT);
      }
   }
}
