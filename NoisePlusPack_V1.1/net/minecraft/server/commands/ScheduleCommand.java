package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.MacroFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.timers.FunctionCallback;
import net.minecraft.world.level.timers.FunctionTagCallback;
import net.minecraft.world.level.timers.TimerQueue;

public class ScheduleCommand {
   private static final SimpleCommandExceptionType ERROR_SAME_TICK = new SimpleCommandExceptionType(Component.translatable("commands.schedule.same_tick"));
   private static final DynamicCommandExceptionType ERROR_CANT_REMOVE = new DynamicCommandExceptionType(
      p_308808_ -> Component.translatableEscape("commands.schedule.cleared.failure", p_308808_)
   );
   private static final SimpleCommandExceptionType ERROR_MACRO = new SimpleCommandExceptionType(Component.translatableEscape("commands.schedule.macro"));
   private static final SuggestionProvider<CommandSourceStack> SUGGEST_SCHEDULE = (p_138424_, p_138425_) -> SharedSuggestionProvider.suggest(
      ((CommandSourceStack)p_138424_.getSource()).getServer().getWorldData().overworldData().getScheduledEvents().getEventsIds(), p_138425_
   );

   public static void register(CommandDispatcher<CommandSourceStack> p_138420_) {
      p_138420_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("schedule")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .then(
                  Commands.literal("function")
                     .then(
                        Commands.argument("function", FunctionArgument.functions())
                           .suggests(FunctionCommand.SUGGEST_FUNCTION)
                           .then(
                              ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("time", TimeArgument.time())
                                       .executes(
                                          p_138459_ -> schedule(
                                             (CommandSourceStack)p_138459_.getSource(),
                                             FunctionArgument.getFunctionOrTag(p_138459_, "function"),
                                             IntegerArgumentType.getInteger(p_138459_, "time"),
                                             true
                                          )
                                       ))
                                    .then(
                                       Commands.literal("append")
                                          .executes(
                                             p_138457_ -> schedule(
                                                (CommandSourceStack)p_138457_.getSource(),
                                                FunctionArgument.getFunctionOrTag(p_138457_, "function"),
                                                IntegerArgumentType.getInteger(p_138457_, "time"),
                                                false
                                             )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("replace")
                                       .executes(
                                          p_138455_ -> schedule(
                                             (CommandSourceStack)p_138455_.getSource(),
                                             FunctionArgument.getFunctionOrTag(p_138455_, "function"),
                                             IntegerArgumentType.getInteger(p_138455_, "time"),
                                             true
                                          )
                                       )
                                 )
                           )
                     )
               ))
            .then(
               Commands.literal("clear")
                  .then(
                     Commands.argument("function", StringArgumentType.greedyString())
                        .suggests(SUGGEST_SCHEDULE)
                        .executes(p_138422_ -> remove((CommandSourceStack)p_138422_.getSource(), StringArgumentType.getString(p_138422_, "function")))
                  )
            )
      );
   }

   private static int schedule(
      CommandSourceStack p_138429_,
      Pair<Identifier, Either<CommandFunction<CommandSourceStack>, Collection<CommandFunction<CommandSourceStack>>>> p_138430_,
      int p_138431_,
      boolean p_138432_
   ) throws CommandSyntaxException {
      if (p_138431_ == 0) {
         throw ERROR_SAME_TICK.create();
      }

      long i = p_138429_.getLevel().getGameTime() + p_138431_;
      Identifier identifier = (Identifier)p_138430_.getFirst();
      TimerQueue<MinecraftServer> timerqueue = p_138429_.getServer().getWorldData().overworldData().getScheduledEvents();
      Optional<CommandFunction<CommandSourceStack>> optional = ((Either)p_138430_.getSecond()).left();
      if (optional.isPresent()) {
         if (optional.get() instanceof MacroFunction) {
            throw ERROR_MACRO.create();
         }

         String s = identifier.toString();
         if (p_138432_) {
            timerqueue.remove(s);
         }

         timerqueue.schedule(s, i, new FunctionCallback(identifier));
         p_138429_.sendSuccess(() -> Component.translatable("commands.schedule.created.function", Component.translationArg(identifier), p_138431_, i), true);
      } else {
         String s1 = "#" + identifier;
         if (p_138432_) {
            timerqueue.remove(s1);
         }

         timerqueue.schedule(s1, i, new FunctionTagCallback(identifier));
         p_138429_.sendSuccess(() -> Component.translatable("commands.schedule.created.tag", Component.translationArg(identifier), p_138431_, i), true);
      }

      return Math.floorMod(i, Integer.MAX_VALUE);
   }

   private static int remove(CommandSourceStack p_138434_, String p_138435_) throws CommandSyntaxException {
      int i = p_138434_.getServer().getWorldData().overworldData().getScheduledEvents().remove(p_138435_);
      if (i == 0) {
         throw ERROR_CANT_REMOVE.create(p_138435_);
      }

      p_138434_.sendSuccess(() -> Component.translatable("commands.schedule.cleared.success", i, p_138435_), true);
      return i;
   }
}
