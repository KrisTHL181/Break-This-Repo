package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class TriggerCommand {
   private static final SimpleCommandExceptionType ERROR_NOT_PRIMED = new SimpleCommandExceptionType(Component.translatable("commands.trigger.failed.unprimed"));
   private static final SimpleCommandExceptionType ERROR_INVALID_OBJECTIVE = new SimpleCommandExceptionType(
      Component.translatable("commands.trigger.failed.invalid")
   );

   public static void register(CommandDispatcher<CommandSourceStack> p_139142_) {
      p_139142_.register(
         (LiteralArgumentBuilder)Commands.literal("trigger")
            .then(
               ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("objective", ObjectiveArgument.objective())
                        .suggests((p_139146_, p_139147_) -> suggestObjectives((CommandSourceStack)p_139146_.getSource(), p_139147_))
                        .executes(
                           p_308912_ -> simpleTrigger(
                              (CommandSourceStack)p_308912_.getSource(),
                              ((CommandSourceStack)p_308912_.getSource()).getPlayerOrException(),
                              ObjectiveArgument.getObjective(p_308912_, "objective")
                           )
                        ))
                     .then(
                        Commands.literal("add")
                           .then(
                              Commands.argument("value", IntegerArgumentType.integer())
                                 .executes(
                                    p_308911_ -> addValue(
                                       (CommandSourceStack)p_308911_.getSource(),
                                       ((CommandSourceStack)p_308911_.getSource()).getPlayerOrException(),
                                       ObjectiveArgument.getObjective(p_308911_, "objective"),
                                       IntegerArgumentType.getInteger(p_308911_, "value")
                                    )
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("set")
                        .then(
                           Commands.argument("value", IntegerArgumentType.integer())
                              .executes(
                                 p_308913_ -> setValue(
                                    (CommandSourceStack)p_308913_.getSource(),
                                    ((CommandSourceStack)p_308913_.getSource()).getPlayerOrException(),
                                    ObjectiveArgument.getObjective(p_308913_, "objective"),
                                    IntegerArgumentType.getInteger(p_308913_, "value")
                                 )
                              )
                        )
                  )
            )
      );
   }

   public static CompletableFuture<Suggestions> suggestObjectives(CommandSourceStack p_139149_, SuggestionsBuilder p_139150_) {
      ScoreHolder scoreholder = p_139149_.getEntity();
      List<String> list = Lists.newArrayList();
      if (scoreholder != null) {
         Scoreboard scoreboard = p_139149_.getServer().getScoreboard();

         for (Objective objective : scoreboard.getObjectives()) {
            if (objective.getCriteria() == ObjectiveCriteria.TRIGGER) {
               ReadOnlyScoreInfo readonlyscoreinfo = scoreboard.getPlayerScoreInfo(scoreholder, objective);
               if (readonlyscoreinfo != null && !readonlyscoreinfo.isLocked()) {
                  list.add(objective.getName());
               }
            }
         }
      }

      return SharedSuggestionProvider.suggest(list, p_139150_);
   }

   private static int addValue(CommandSourceStack p_139155_, ServerPlayer p_310899_, Objective p_310001_, int p_139157_) throws CommandSyntaxException {
      ScoreAccess scoreaccess = getScore(p_139155_.getServer().getScoreboard(), p_310899_, p_310001_);
      int i = scoreaccess.add(p_139157_);
      p_139155_.sendSuccess(() -> Component.translatable("commands.trigger.add.success", p_310001_.getFormattedDisplayName(), p_139157_), true);
      return i;
   }

   private static int setValue(CommandSourceStack p_139161_, ServerPlayer p_312734_, Objective p_309575_, int p_139163_) throws CommandSyntaxException {
      ScoreAccess scoreaccess = getScore(p_139161_.getServer().getScoreboard(), p_312734_, p_309575_);
      scoreaccess.set(p_139163_);
      p_139161_.sendSuccess(() -> Component.translatable("commands.trigger.set.success", p_309575_.getFormattedDisplayName(), p_139163_), true);
      return p_139163_;
   }

   private static int simpleTrigger(CommandSourceStack p_139152_, ServerPlayer p_310805_, Objective p_313189_) throws CommandSyntaxException {
      ScoreAccess scoreaccess = getScore(p_139152_.getServer().getScoreboard(), p_310805_, p_313189_);
      int i = scoreaccess.add(1);
      p_139152_.sendSuccess(() -> Component.translatable("commands.trigger.simple.success", p_313189_.getFormattedDisplayName()), true);
      return i;
   }

   private static ScoreAccess getScore(Scoreboard p_309433_, ScoreHolder p_310288_, Objective p_139140_) throws CommandSyntaxException {
      if (p_139140_.getCriteria() != ObjectiveCriteria.TRIGGER) {
         throw ERROR_INVALID_OBJECTIVE.create();
      } else {
         ReadOnlyScoreInfo readonlyscoreinfo = p_309433_.getPlayerScoreInfo(p_310288_, p_139140_);
         if (readonlyscoreinfo != null && !readonlyscoreinfo.isLocked()) {
            ScoreAccess scoreaccess = p_309433_.getOrCreatePlayerScore(p_310288_, p_139140_);
            scoreaccess.lock();
            return scoreaccess;
         } else {
            throw ERROR_NOT_PRIMED.create();
         }
      }
   }
}
