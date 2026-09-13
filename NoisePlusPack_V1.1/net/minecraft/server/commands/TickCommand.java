package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.util.TimeUtil;

public class TickCommand {
   private static final float MAX_TICKRATE = 10000.0F;
   private static final String DEFAULT_TICKRATE = String.valueOf(20);

   public static void register(CommandDispatcher<CommandSourceStack> p_309698_) {
      p_309698_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                 "tick"
                              )
                              .requires(Commands.hasPermission(Commands.LEVEL_ADMINS)))
                           .then(Commands.literal("query").executes(p_310022_ -> tickQuery((CommandSourceStack)p_310022_.getSource()))))
                        .then(
                           Commands.literal("rate")
                              .then(
                                 Commands.argument("rate", FloatArgumentType.floatArg(1.0F, 10000.0F))
                                    .suggests((p_311314_, p_311289_) -> SharedSuggestionProvider.suggest(new String[]{DEFAULT_TICKRATE}, p_311289_))
                                    .executes(
                                       p_312329_ -> setTickingRate((CommandSourceStack)p_312329_.getSource(), FloatArgumentType.getFloat(p_312329_, "rate"))
                                    )
                              )
                        ))
                     .then(
                        ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("step")
                                 .executes(p_311036_ -> step((CommandSourceStack)p_311036_.getSource(), 1)))
                              .then(Commands.literal("stop").executes(p_309944_ -> stopStepping((CommandSourceStack)p_309944_.getSource()))))
                           .then(
                              Commands.argument("time", TimeArgument.time(1))
                                 .suggests((p_313203_, p_312907_) -> SharedSuggestionProvider.suggest(new String[]{"1t", "1s"}, p_312907_))
                                 .executes(p_312113_ -> step((CommandSourceStack)p_312113_.getSource(), IntegerArgumentType.getInteger(p_312113_, "time")))
                           )
                     ))
                  .then(
                     ((LiteralArgumentBuilder)Commands.literal("sprint")
                           .then(Commands.literal("stop").executes(p_311524_ -> stopSprinting((CommandSourceStack)p_311524_.getSource()))))
                        .then(
                           Commands.argument("time", TimeArgument.time(1))
                              .suggests((p_311140_, p_312761_) -> SharedSuggestionProvider.suggest(new String[]{"60s", "1d", "3d"}, p_312761_))
                              .executes(p_311082_ -> sprint((CommandSourceStack)p_311082_.getSource(), IntegerArgumentType.getInteger(p_311082_, "time")))
                        )
                  ))
               .then(Commands.literal("unfreeze").executes(p_309501_ -> setFreeze((CommandSourceStack)p_309501_.getSource(), false))))
            .then(Commands.literal("freeze").executes(p_312020_ -> setFreeze((CommandSourceStack)p_312020_.getSource(), true)))
      );
   }

   private static String nanosToMilisString(long p_312994_) {
      return String.format(Locale.ROOT, "%.1f", (float)p_312994_ / (float)TimeUtil.NANOSECONDS_PER_MILLISECOND);
   }

   private static int setTickingRate(CommandSourceStack p_311838_, float p_312705_) {
      ServerTickRateManager servertickratemanager = p_311838_.getServer().tickRateManager();
      servertickratemanager.setTickRate(p_312705_);
      String s = String.format(Locale.ROOT, "%.1f", p_312705_);
      p_311838_.sendSuccess(() -> Component.translatable("commands.tick.rate.success", s), true);
      return (int)p_312705_;
   }

   private static int tickQuery(CommandSourceStack p_310546_) {
      ServerTickRateManager servertickratemanager = p_310546_.getServer().tickRateManager();
      String s = nanosToMilisString(p_310546_.getServer().getAverageTickTimeNanos());
      float f = servertickratemanager.tickrate();
      String s1 = String.format(Locale.ROOT, "%.1f", f);
      if (servertickratemanager.isSprinting()) {
         p_310546_.sendSuccess(() -> Component.translatable("commands.tick.status.sprinting"), false);
         p_310546_.sendSuccess(() -> Component.translatable("commands.tick.query.rate.sprinting", s1, s), false);
      } else {
         if (servertickratemanager.isFrozen()) {
            p_310546_.sendSuccess(() -> Component.translatable("commands.tick.status.frozen"), false);
         } else if (servertickratemanager.nanosecondsPerTick() < p_310546_.getServer().getAverageTickTimeNanos()) {
            p_310546_.sendSuccess(() -> Component.translatable("commands.tick.status.lagging"), false);
         } else {
            p_310546_.sendSuccess(() -> Component.translatable("commands.tick.status.running"), false);
         }

         String s2 = nanosToMilisString(servertickratemanager.nanosecondsPerTick());
         p_310546_.sendSuccess(() -> Component.translatable("commands.tick.query.rate.running", s1, s, s2), false);
      }

      long[] along = Arrays.copyOf(p_310546_.getServer().getTickTimesNanos(), p_310546_.getServer().getTickTimesNanos().length);
      Arrays.sort(along);
      String s3 = nanosToMilisString(along[along.length / 2]);
      String s4 = nanosToMilisString(along[(int)(along.length * 0.95)]);
      String s5 = nanosToMilisString(along[(int)(along.length * 0.99)]);
      p_310546_.sendSuccess(() -> Component.translatable("commands.tick.query.percentiles", s3, s4, s5, along.length), false);
      return (int)f;
   }

   private static int sprint(CommandSourceStack p_311527_, int p_312312_) {
      boolean flag = p_311527_.getServer().tickRateManager().requestGameToSprint(p_312312_);
      if (flag) {
         p_311527_.sendSuccess(() -> Component.translatable("commands.tick.sprint.stop.success"), true);
      }

      p_311527_.sendSuccess(() -> Component.translatable("commands.tick.status.sprinting"), true);
      return 1;
   }

   private static int setFreeze(CommandSourceStack p_309500_, boolean p_312715_) {
      ServerTickRateManager servertickratemanager = p_309500_.getServer().tickRateManager();
      if (p_312715_) {
         if (servertickratemanager.isSprinting()) {
            servertickratemanager.stopSprinting();
         }

         if (servertickratemanager.isSteppingForward()) {
            servertickratemanager.stopStepping();
         }
      }

      servertickratemanager.setFrozen(p_312715_);
      if (p_312715_) {
         p_309500_.sendSuccess(() -> Component.translatable("commands.tick.status.frozen"), true);
      } else {
         p_309500_.sendSuccess(() -> Component.translatable("commands.tick.status.running"), true);
      }

      return p_312715_ ? 1 : 0;
   }

   private static int step(CommandSourceStack p_312155_, int p_311495_) {
      ServerTickRateManager servertickratemanager = p_312155_.getServer().tickRateManager();
      boolean flag = servertickratemanager.stepGameIfPaused(p_311495_);
      if (flag) {
         p_312155_.sendSuccess(() -> Component.translatable("commands.tick.step.success", p_311495_), true);
      } else {
         p_312155_.sendFailure(Component.translatable("commands.tick.step.fail"));
      }

      return 1;
   }

   private static int stopStepping(CommandSourceStack p_310383_) {
      ServerTickRateManager servertickratemanager = p_310383_.getServer().tickRateManager();
      boolean flag = servertickratemanager.stopStepping();
      if (flag) {
         p_310383_.sendSuccess(() -> Component.translatable("commands.tick.step.stop.success"), true);
         return 1;
      } else {
         p_310383_.sendFailure(Component.translatable("commands.tick.step.stop.fail"));
         return 0;
      }
   }

   private static int stopSprinting(CommandSourceStack p_312590_) {
      ServerTickRateManager servertickratemanager = p_312590_.getServer().tickRateManager();
      boolean flag = servertickratemanager.stopSprinting();
      if (flag) {
         p_312590_.sendSuccess(() -> Component.translatable("commands.tick.sprint.stop.success"), true);
         return 1;
      } else {
         p_312590_.sendFailure(Component.translatable("commands.tick.sprint.stop.fail"));
         return 0;
      }
   }
}
