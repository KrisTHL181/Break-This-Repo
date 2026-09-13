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

   public static void register(CommandDispatcher<CommandSourceStack> p_180438_) {
      p_180438_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("perf")
                  .requires(Commands.hasPermission(Commands.LEVEL_OWNERS)))
               .then(Commands.literal("start").executes(p_180455_ -> startProfilingDedicatedServer((CommandSourceStack)p_180455_.getSource()))))
            .then(Commands.literal("stop").executes(p_180440_ -> stopProfilingDedicatedServer((CommandSourceStack)p_180440_.getSource())))
      );
   }

   private static int startProfilingDedicatedServer(CommandSourceStack p_180442_) throws CommandSyntaxException {
      MinecraftServer minecraftserver = p_180442_.getServer();
      if (minecraftserver.isRecordingMetrics()) {
         throw ERROR_ALREADY_RUNNING.create();
      }

      Consumer<ProfileResults> consumer = p_180460_ -> whenStopped(p_180442_, p_180460_);
      Consumer<Path> consumer1 = p_180453_ -> saveResults(p_180442_, p_180453_, minecraftserver);
      minecraftserver.startRecordingMetrics(consumer, consumer1);
      p_180442_.sendSuccess(() -> Component.translatable("commands.perf.started"), false);
      return 0;
   }

   private static int stopProfilingDedicatedServer(CommandSourceStack p_180457_) throws CommandSyntaxException {
      MinecraftServer minecraftserver = p_180457_.getServer();
      if (!minecraftserver.isRecordingMetrics()) {
         throw ERROR_NOT_RUNNING.create();
      }

      minecraftserver.finishRecordingMetrics();
      return 0;
   }

   private static void saveResults(CommandSourceStack p_180447_, Path p_180448_, MinecraftServer p_180449_) {
      String s = String.format(
         Locale.ROOT, "%s-%s-%s", Util.getFilenameFormattedDateTime(), p_180449_.getWorldData().getLevelName(), SharedConstants.getCurrentVersion().id()
      );

      String s1;
      try {
         s1 = FileUtil.findAvailableName(MetricsPersister.PROFILING_RESULTS_DIR, s, ".zip");
      } catch (IOException ioexception1) {
         p_180447_.sendFailure(Component.translatable("commands.perf.reportFailed"));
         LOGGER.error("Failed to create report name", ioexception1);
         return;
      }

      try (FileZipper filezipper = new FileZipper(MetricsPersister.PROFILING_RESULTS_DIR.resolve(s1))) {
         filezipper.add(Paths.get("system.txt"), p_180449_.fillSystemReport(new SystemReport()).toLineSeparatedString());
         filezipper.add(p_180448_);
      }

      try {
         FileUtils.forceDelete(p_180448_.toFile());
      } catch (IOException ioexception) {
         LOGGER.warn("Failed to delete temporary profiling file {}", p_180448_, ioexception);
      }

      p_180447_.sendSuccess(() -> Component.translatable("commands.perf.reportSaved", s1), false);
   }

   private static void whenStopped(CommandSourceStack p_180444_, ProfileResults p_180445_) {
      if (p_180445_ != EmptyProfileResults.EMPTY) {
         int i = p_180445_.getTickDuration();
         double d0 = (double)p_180445_.getNanoDuration() / TimeUtil.NANOSECONDS_PER_SECOND;
         p_180444_.sendSuccess(
            () -> Component.translatable("commands.perf.stopped", String.format(Locale.ROOT, "%.2f", d0), i, String.format(Locale.ROOT, "%.2f", i / d0)), false
         );
      }
   }
}
