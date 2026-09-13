package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.IntProvider;

public class WeatherCommand {
   private static final int DEFAULT_TIME = -1;

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("weather")
                     .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                  .then(
                     ((LiteralArgumentBuilder)Commands.literal("clear").executes(c -> setClear((CommandSourceStack)c.getSource(), -1)))
                        .then(
                           Commands.argument("duration", TimeArgument.time(1))
                              .executes(c -> setClear((CommandSourceStack)c.getSource(), IntegerArgumentType.getInteger(c, "duration")))
                        )
                  ))
               .then(
                  ((LiteralArgumentBuilder)Commands.literal("rain").executes(c -> setRain((CommandSourceStack)c.getSource(), -1)))
                     .then(
                        Commands.argument("duration", TimeArgument.time(1))
                           .executes(c -> setRain((CommandSourceStack)c.getSource(), IntegerArgumentType.getInteger(c, "duration")))
                     )
               ))
            .then(
               ((LiteralArgumentBuilder)Commands.literal("thunder").executes(c -> setThunder((CommandSourceStack)c.getSource(), -1)))
                  .then(
                     Commands.argument("duration", TimeArgument.time(1))
                        .executes(c -> setThunder((CommandSourceStack)c.getSource(), IntegerArgumentType.getInteger(c, "duration")))
                  )
            )
      );
   }

   private static int getDuration(final CommandSourceStack source, final int input, final IntProvider defaultDistribution) {
      return input == -1 ? defaultDistribution.sample(source.getLevel().getRandom()) : input;
   }

   private static int setClear(final CommandSourceStack source, final int duration) {
      source.getServer().setWeatherParameters(getDuration(source, duration, ServerLevel.RAIN_DELAY), 0, false, false);
      source.sendSuccess(() -> Component.translatable("commands.weather.set.clear"), true);
      return duration;
   }

   private static int setRain(final CommandSourceStack source, final int duration) {
      source.getServer().setWeatherParameters(0, getDuration(source, duration, ServerLevel.RAIN_DURATION), true, false);
      source.sendSuccess(() -> Component.translatable("commands.weather.set.rain"), true);
      return duration;
   }

   private static int setThunder(final CommandSourceStack source, final int duration) {
      source.getServer().setWeatherParameters(0, getDuration(source, duration, ServerLevel.THUNDER_DURATION), true, true);
      source.sendSuccess(() -> Component.translatable("commands.weather.set.thunder"), true);
      return duration;
   }
}
