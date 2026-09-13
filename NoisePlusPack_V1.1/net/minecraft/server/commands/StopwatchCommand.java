package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Stopwatch;
import net.minecraft.world.Stopwatches;

public class StopwatchCommand {
   private static final DynamicCommandExceptionType ERROR_ALREADY_EXISTS = new DynamicCommandExceptionType(
      p_456467_ -> Component.translatableEscape("commands.stopwatch.already_exists", p_456467_)
   );
   public static final DynamicCommandExceptionType ERROR_DOES_NOT_EXIST = new DynamicCommandExceptionType(
      p_455292_ -> Component.translatableEscape("commands.stopwatch.does_not_exist", p_455292_)
   );
   public static final SuggestionProvider<CommandSourceStack> SUGGEST_STOPWATCHES = (p_451924_, p_454103_) -> SharedSuggestionProvider.suggestResource(
      ((CommandSourceStack)p_451924_.getSource()).getServer().getStopwatches().ids(), p_454103_
   );

   public static void register(CommandDispatcher<CommandSourceStack> p_454389_) {
      p_454389_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                           "stopwatch"
                        )
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                     .then(
                        Commands.literal("create")
                           .then(
                              Commands.argument("id", IdentifierArgument.id())
                                 .executes(p_453744_ -> createStopwatch((CommandSourceStack)p_453744_.getSource(), IdentifierArgument.getId(p_453744_, "id")))
                           )
                     ))
                  .then(
                     Commands.literal("query")
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("id", IdentifierArgument.id())
                                 .suggests(SUGGEST_STOPWATCHES)
                                 .then(
                                    Commands.argument("scale", DoubleArgumentType.doubleArg())
                                       .executes(
                                          p_459180_ -> queryStopwatch(
                                             (CommandSourceStack)p_459180_.getSource(),
                                             IdentifierArgument.getId(p_459180_, "id"),
                                             DoubleArgumentType.getDouble(p_459180_, "scale")
                                          )
                                       )
                                 ))
                              .executes(p_453900_ -> queryStopwatch((CommandSourceStack)p_453900_.getSource(), IdentifierArgument.getId(p_453900_, "id"), 1.0))
                        )
                  ))
               .then(
                  Commands.literal("restart")
                     .then(
                        Commands.argument("id", IdentifierArgument.id())
                           .suggests(SUGGEST_STOPWATCHES)
                           .executes(p_454133_ -> restartStopwatch((CommandSourceStack)p_454133_.getSource(), IdentifierArgument.getId(p_454133_, "id")))
                     )
               ))
            .then(
               Commands.literal("remove")
                  .then(
                     Commands.argument("id", IdentifierArgument.id())
                        .suggests(SUGGEST_STOPWATCHES)
                        .executes(p_456712_ -> removeStopwatch((CommandSourceStack)p_456712_.getSource(), IdentifierArgument.getId(p_456712_, "id")))
                  )
            )
      );
   }

   private static int createStopwatch(CommandSourceStack p_460426_, Identifier p_460080_) throws CommandSyntaxException {
      MinecraftServer minecraftserver = p_460426_.getServer();
      Stopwatches stopwatches = minecraftserver.getStopwatches();
      Stopwatch stopwatch = new Stopwatch(Stopwatches.currentTime());
      if (!stopwatches.add(p_460080_, stopwatch)) {
         throw ERROR_ALREADY_EXISTS.create(p_460080_);
      }

      p_460426_.sendSuccess(() -> Component.translatable("commands.stopwatch.create.success", Component.translationArg(p_460080_)), true);
      return 1;
   }

   private static int queryStopwatch(CommandSourceStack p_453589_, Identifier p_452080_, double p_457469_) throws CommandSyntaxException {
      MinecraftServer minecraftserver = p_453589_.getServer();
      Stopwatches stopwatches = minecraftserver.getStopwatches();
      Stopwatch stopwatch = stopwatches.get(p_452080_);
      if (stopwatch == null) {
         throw ERROR_DOES_NOT_EXIST.create(p_452080_);
      }

      long i = Stopwatches.currentTime();
      double d0 = stopwatch.elapsedSeconds(i);
      p_453589_.sendSuccess(() -> Component.translatable("commands.stopwatch.query", Component.translationArg(p_452080_), d0), true);
      return (int)(d0 * p_457469_);
   }

   private static int restartStopwatch(CommandSourceStack p_451471_, Identifier p_456369_) throws CommandSyntaxException {
      MinecraftServer minecraftserver = p_451471_.getServer();
      Stopwatches stopwatches = minecraftserver.getStopwatches();
      if (!stopwatches.update(p_456369_, p_454753_ -> new Stopwatch(Stopwatches.currentTime()))) {
         throw ERROR_DOES_NOT_EXIST.create(p_456369_);
      }

      p_451471_.sendSuccess(() -> Component.translatable("commands.stopwatch.restart.success", Component.translationArg(p_456369_)), true);
      return 1;
   }

   private static int removeStopwatch(CommandSourceStack p_453681_, Identifier p_453157_) throws CommandSyntaxException {
      MinecraftServer minecraftserver = p_453681_.getServer();
      Stopwatches stopwatches = minecraftserver.getStopwatches();
      if (!stopwatches.remove(p_453157_)) {
         throw ERROR_DOES_NOT_EXIST.create(p_453157_);
      }

      p_453681_.sendSuccess(() -> Component.translatable("commands.stopwatch.remove.success", Component.translationArg(p_453157_)), true);
      return 1;
   }
}
