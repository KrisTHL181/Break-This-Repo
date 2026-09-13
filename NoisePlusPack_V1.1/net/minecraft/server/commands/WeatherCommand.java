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

   public static void register(CommandDispatcher<CommandSourceStack> p_139167_) {
      p_139167_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("weather")
                     .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                  .then(
                     ((LiteralArgumentBuilder)Commands.literal("clear").executes(p_264806_ -> setClear((CommandSourceStack)p_264806_.getSource(), -1)))
                        .then(
                           Commands.argument("duration", TimeArgument.time(1))
                              .executes(p_264807_ -> setClear((CommandSourceStack)p_264807_.getSource(), IntegerArgumentType.getInteger(p_264807_, "duration")))
                        )
                  ))
               .then(
                  ((LiteralArgumentBuilder)Commands.literal("rain").executes(p_264805_ -> setRain((CommandSourceStack)p_264805_.getSource(), -1)))
                     .then(
                        Commands.argument("duration", TimeArgument.time(1))
                           .executes(p_264809_ -> setRain((CommandSourceStack)p_264809_.getSource(), IntegerArgumentType.getInteger(p_264809_, "duration")))
                     )
               ))
            .then(
               ((LiteralArgumentBuilder)Commands.literal("thunder").executes(p_264808_ -> setThunder((CommandSourceStack)p_264808_.getSource(), -1)))
                  .then(
                     Commands.argument("duration", TimeArgument.time(1))
                        .executes(p_264804_ -> setThunder((CommandSourceStack)p_264804_.getSource(), IntegerArgumentType.getInteger(p_264804_, "duration")))
                  )
            )
      );
   }

   private static int getDuration(CommandSourceStack p_265382_, int p_265171_, IntProvider p_265122_) {
      return p_265171_ == -1 ? p_265122_.sample(p_265382_.getServer().overworld().getRandom()) : p_265171_;
   }

   private static int setClear(CommandSourceStack p_139173_, int p_139174_) {
      p_139173_.getServer().overworld().setWeatherParameters(getDuration(p_139173_, p_139174_, ServerLevel.RAIN_DELAY), 0, false, false);
      p_139173_.sendSuccess(() -> Component.translatable("commands.weather.set.clear"), true);
      return p_139174_;
   }

   private static int setRain(CommandSourceStack p_139178_, int p_139179_) {
      p_139178_.getServer().overworld().setWeatherParameters(0, getDuration(p_139178_, p_139179_, ServerLevel.RAIN_DURATION), true, false);
      p_139178_.sendSuccess(() -> Component.translatable("commands.weather.set.rain"), true);
      return p_139179_;
   }

   private static int setThunder(CommandSourceStack p_139183_, int p_139184_) {
      p_139183_.getServer().overworld().setWeatherParameters(0, getDuration(p_139183_, p_139184_, ServerLevel.THUNDER_DURATION), true, true);
      p_139183_.sendSuccess(() -> Component.translatable("commands.weather.set.thunder"), true);
      return p_139184_;
   }
}
