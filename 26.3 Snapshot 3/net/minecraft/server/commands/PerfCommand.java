package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.SystemReport;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.FileUtil;
import net.minecraft.util.FileZipper;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.EmptyProfileResults;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.metrics.storage.MetricsPersister;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

public class PerfCommand {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final SimpleCommandExceptionType ERROR_NOT_RUNNING = new SimpleCommandExceptionType(Component.translatable("commands.perf.notRunning"));
   private static final SimpleCommandExceptionType ERROR_ALREADY_RUNNING = new SimpleCommandExceptionType(
      Component.translatable("commands.perf.alreadyRunning")
   );

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("perf")
                  .requires(Commands.hasPermission(Commands.LEVEL_OWNERS)))
               .then(Commands.literal("start").executes(c -> startProfilingDedicatedServer((CommandSourceStack)c.getSource()))))
            .then(Commands.literal("stop").executes(c -> stopProfilingDedicatedServer((CommandSourceStack)c.getSource())))
      );
   }

   private static int startProfilingDedicatedServer(final CommandSourceStack source) throws CommandSyntaxException {
      MinecraftServer server = source.getServer();
      if (server.isRecordingMetrics()) {
         throw ERROR_ALREADY_RUNNING.create();
      }

      Consumer<ProfileResults> onStopped = results -> whenStopped(source, results);
      Consumer<Path> onReportFinished = profilingLogs -> saveResults(source, profilingLogs, server);
      server.startRecordingMetrics(onStopped, onReportFinished);
      source.sendSuccess(() -> Component.translatable("commands.perf.started"), false);
      return 0;
   }

   private static int stopProfilingDedicatedServer(final CommandSourceStack source) throws CommandSyntaxException {
      MinecraftServer server = source.getServer();
      if (!server.isRecordingMetrics()) {
         throw ERROR_NOT_RUNNING.create();
      }

      server.finishRecordingMetrics();
      return 0;
   }

   private static void saveResults(final CommandSourceStack source, final Path report, final MinecraftServer server) {
      String profilingName = String.format(
         Locale.ROOT, "%s-%s-%s", Util.getFilenameFormattedDateTime(), server.getWorldData().getLevelName(), SharedConstants.getCurrentVersion().id()
      );

      String zipFile;
      try {
         zipFile = FileUtil.findAvailableName(MetricsPersister.PROFILING_RESULTS_DIR, profilingName, ".zip");
      } catch (IOException e) {
         source.sendFailure(Component.translatable("commands.perf.reportFailed"));
         LOGGER.error("Failed to create report name", e);
         return;
      }

      try (FileZipper fileZipper = new FileZipper(MetricsPersister.PROFILING_RESULTS_DIR.resolve(zipFile))) {
         fileZipper.add(Paths.get("system.txt"), server.fillSystemReport(new SystemReport()).toLineSeparatedString());
         fileZipper.add(report);
      }

      try {
         FileUtils.forceDelete(report.toFile());
      } catch (IOException e) {
         LOGGER.warn("Failed to delete temporary profiling file {}", report, e);
      }

      source.sendSuccess(() -> Component.translatable("commands.perf.reportSaved", zipFile), false);
   }

   private static void whenStopped(final CommandSourceStack source, final ProfileResults results) {
      if (results != EmptyProfileResults.EMPTY) {
         int ticks = results.getTickDuration();
         double durationInSeconds = (double)results.getNanoDuration() / TimeUtil.NANOSECONDS_PER_SECOND;
         source.sendSuccess(
            () -> Component.translatable(
               "commands.perf.stopped",
               String.format(Locale.ROOT, "%.2f", durationInSeconds),
               ticks,
               String.format(Locale.ROOT, "%.2f", ticks / durationInSeconds)
            ),
            false
         );
      }
   }
}
