package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Locale;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CustomCommandExecutor;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.Frame;
import net.minecraft.commands.execution.TraceCallbacks;
import net.minecraft.commands.execution.tasks.CallFunction;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.InstantiatedFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.ProfileResults;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class DebugCommand {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final SimpleCommandExceptionType ERROR_NOT_RUNNING = new SimpleCommandExceptionType(Component.translatable("commands.debug.notRunning"));
   private static final SimpleCommandExceptionType ERROR_ALREADY_RUNNING = new SimpleCommandExceptionType(
      Component.translatable("commands.debug.alreadyRunning")
   );
   static final SimpleCommandExceptionType NO_RECURSIVE_TRACES = new SimpleCommandExceptionType(Component.translatable("commands.debug.function.noRecursion"));
   static final SimpleCommandExceptionType NO_RETURN_RUN = new SimpleCommandExceptionType(Component.translatable("commands.debug.function.noReturnRun"));

   public static void register(CommandDispatcher<CommandSourceStack> p_136906_) {
      p_136906_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("debug")
                     .requires(Commands.hasPermission(Commands.LEVEL_ADMINS)))
                  .then(Commands.literal("start").executes(p_180069_ -> start((CommandSourceStack)p_180069_.getSource()))))
               .then(Commands.literal("stop").executes(p_136918_ -> stop((CommandSourceStack)p_136918_.getSource()))))
            .then(
               ((LiteralArgumentBuilder)Commands.literal("function").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)))
                  .then(
                     Commands.argument("name", FunctionArgument.functions())
                        .suggests(FunctionCommand.SUGGEST_FUNCTION)
                        .executes(new DebugCommand.TraceCustomExecutor())
                  )
            )
      );
   }

   private static int start(CommandSourceStack p_136910_) throws CommandSyntaxException {
      MinecraftServer minecraftserver = p_136910_.getServer();
      if (minecraftserver.isTimeProfilerRunning()) {
         throw ERROR_ALREADY_RUNNING.create();
      }

      minecraftserver.startTimeProfiler();
      p_136910_.sendSuccess(() -> Component.translatable("commands.debug.started"), true);
      return 0;
   }

   private static int stop(CommandSourceStack p_136916_) throws CommandSyntaxException {
      MinecraftServer minecraftserver = p_136916_.getServer();
      if (!minecraftserver.isTimeProfilerRunning()) {
         throw ERROR_NOT_RUNNING.create();
      }

      ProfileResults profileresults = minecraftserver.stopTimeProfiler();
      double d0 = (double)profileresults.getNanoDuration() / TimeUtil.NANOSECONDS_PER_SECOND;
      double d1 = profileresults.getTickDuration() / d0;
      p_136916_.sendSuccess(
         () -> Component.translatable(
            "commands.debug.stopped", String.format(Locale.ROOT, "%.2f", d0), profileresults.getTickDuration(), String.format(Locale.ROOT, "%.2f", d1)
         ),
         true
      );
      return (int)d1;
   }

   static class TraceCustomExecutor
      extends CustomCommandExecutor.WithErrorHandling<CommandSourceStack>
      implements CustomCommandExecutor.CommandAdapter<CommandSourceStack> {
      public void runGuarded(
         CommandSourceStack p_309819_, ContextChain<CommandSourceStack> p_311173_, ChainModifiers p_312111_, ExecutionControl<CommandSourceStack> p_311988_
      ) throws CommandSyntaxException {
         if (p_312111_.isReturn()) {
            throw DebugCommand.NO_RETURN_RUN.create();
         }

         if (p_311988_.tracer() != null) {
            throw DebugCommand.NO_RECURSIVE_TRACES.create();
         }

         CommandContext<CommandSourceStack> commandcontext = p_311173_.getTopContext();
         Collection<CommandFunction<CommandSourceStack>> collection = FunctionArgument.getFunctions(commandcontext, "name");
         MinecraftServer minecraftserver = p_309819_.getServer();
         String s = "debug-trace-" + Util.getFilenameFormattedDateTime() + ".txt";
         CommandDispatcher<CommandSourceStack> commanddispatcher = p_309819_.getServer().getFunctions().getDispatcher();
         int i = 0;

         try {
            Path path = minecraftserver.getFile("debug");
            Files.createDirectories(path);
            final PrintWriter printwriter = new PrintWriter(Files.newBufferedWriter(path.resolve(s), StandardCharsets.UTF_8));
            DebugCommand.Tracer debugcommand$tracer = new DebugCommand.Tracer(printwriter);
            p_311988_.tracer(debugcommand$tracer);

            for (final CommandFunction<CommandSourceStack> commandfunction : collection) {
               try {
                  CommandSourceStack commandsourcestack = p_309819_.withSource(debugcommand$tracer).withMaximumPermission(LevelBasedPermissionSet.GAMEMASTER);
                  InstantiatedFunction<CommandSourceStack> instantiatedfunction = commandfunction.instantiate(null, commanddispatcher);
                  p_311988_.queueNext((new CallFunction<CommandSourceStack>(instantiatedfunction, CommandResultCallback.EMPTY, false) {
                     public void execute(CommandSourceStack p_310186_, ExecutionContext<CommandSourceStack> p_311250_, Frame p_311262_) {
                        printwriter.println(commandfunction.id());
                        super.execute(p_310186_, p_311250_, p_311262_);
                     }
                  }).bind(commandsourcestack));
                  i += instantiatedfunction.entries().size();
               } catch (FunctionInstantiationException functioninstantiationexception) {
                  p_309819_.sendFailure(functioninstantiationexception.messageComponent());
               }
            }
         } catch (IOException | UncheckedIOException uncheckedioexception) {
            DebugCommand.LOGGER.warn("Tracing failed", uncheckedioexception);
            p_309819_.sendFailure(Component.translatable("commands.debug.function.traceFailed"));
         }

         int j = i;
         p_311988_.queueNext(
            (p_311688_, p_310332_) -> {
               if (collection.size() == 1) {
                  p_309819_.sendSuccess(
                     () -> Component.translatable("commands.debug.function.success.single", j, Component.translationArg(collection.iterator().next().id()), s),
                     true
                  );
               } else {
                  p_309819_.sendSuccess(() -> Component.translatable("commands.debug.function.success.multiple", j, collection.size(), s), true);
               }
            }
         );
      }
   }

   static class Tracer implements CommandSource, TraceCallbacks {
      public static final int INDENT_OFFSET = 1;
      private final PrintWriter output;
      private int lastIndent;
      private boolean waitingForResult;

      Tracer(PrintWriter p_180079_) {
         this.output = p_180079_;
      }

      private void indentAndSave(int p_180082_) {
         this.printIndent(p_180082_);
         this.lastIndent = p_180082_;
      }

      private void printIndent(int p_180098_) {
         for (int i = 0; i < p_180098_ + 1; i++) {
            this.output.write("    ");
         }
      }

      private void newLine() {
         if (this.waitingForResult) {
            this.output.println();
            this.waitingForResult = false;
         }
      }

      @Override
      public void onCommand(int p_180084_, String p_180085_) {
         this.newLine();
         this.indentAndSave(p_180084_);
         this.output.print("[C] ");
         this.output.print(p_180085_);
         this.waitingForResult = true;
      }

      @Override
      public void onReturn(int p_180087_, String p_180088_, int p_180089_) {
         if (this.waitingForResult) {
            this.output.print(" -> ");
            this.output.println(p_180089_);
            this.waitingForResult = false;
         } else {
            this.indentAndSave(p_180087_);
            this.output.print("[R = ");
            this.output.print(p_180089_);
            this.output.print("] ");
            this.output.println(p_180088_);
         }
      }

      @Override
      public void onCall(int p_180091_, Identifier p_451023_, int p_180093_) {
         this.newLine();
         this.indentAndSave(p_180091_);
         this.output.print("[F] ");
         this.output.print(p_451023_);
         this.output.print(" size=");
         this.output.println(p_180093_);
      }

      @Override
      public void onError(String p_180101_) {
         this.newLine();
         this.indentAndSave(this.lastIndent + 1);
         this.output.print("[E] ");
         this.output.print(p_180101_);
      }

      @Override
      public void sendSystemMessage(Component p_214427_) {
         this.newLine();
         this.printIndent(this.lastIndent + 1);
         this.output.print("[M] ");
         this.output.println(p_214427_.getString());
      }

      @Override
      public boolean acceptsSuccess() {
         return true;
      }

      @Override
      public boolean acceptsFailure() {
         return true;
      }

      @Override
      public boolean shouldInformAdmins() {
         return false;
      }

      @Override
      public boolean alwaysAccepts() {
         return true;
      }

      @Override
      public void close() {
         IOUtils.closeQuietly(this.output);
      }
   }
}
