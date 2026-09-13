package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.util.profiling.jfr.Environment;
import net.minecraft.util.profiling.jfr.JvmProfiler;

public class JfrCommand {
   private static final SimpleCommandExceptionType START_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.jfr.start.failed"));
   private static final DynamicCommandExceptionType DUMP_FAILED = new DynamicCommandExceptionType(
      message -> Component.translatableEscape("commands.jfr.dump.failed", message)
   );

   private JfrCommand() {
   }

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("jfr")
                  .requires(Commands.hasPermission(Commands.LEVEL_OWNERS)))
               .then(Commands.literal("start").executes(c -> startJfr((CommandSourceStack)c.getSource()))))
            .then(Commands.literal("stop").executes(c -> stopJfr((CommandSourceStack)c.getSource())))
      );
   }

   private static int startJfr(final CommandSourceStack source) throws CommandSyntaxException {
      Environment env = Environment.from(source.getServer());
      if (!JvmProfiler.INSTANCE.start(env)) {
         throw START_FAILED.create();
      }

      source.sendSuccess(() -> Component.translatable("commands.jfr.started"), false);
      return 1;
   }

   private static int stopJfr(final CommandSourceStack source) throws CommandSyntaxException {
      try {
         Path savedRecording = Paths.get(".").relativize(JvmProfiler.INSTANCE.stop().normalize());
         Path clipboardPath = source.getServer().isPublished() && !SharedConstants.IS_RUNNING_IN_IDE ? savedRecording : savedRecording.toAbsolutePath();
         Component fileText = Component.literal(savedRecording.toString())
            .withStyle(ChatFormatting.UNDERLINE)
            .withStyle(
               style -> style.withClickEvent(new ClickEvent.CopyToClipboard(clipboardPath.toString()))
                  .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.copy.click")))
            );
         source.sendSuccess(() -> Component.translatable("commands.jfr.stopped", fileText), false);
         return 1;
      } catch (Throwable t) {
         throw DUMP_FAILED.create(t.getMessage());
      }
   }
}
