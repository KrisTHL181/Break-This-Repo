package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.commands.arguments.ObjectiveCriteriaArgument;
import net.minecraft.commands.arguments.OperationArgument;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import net.minecraft.commands.arguments.ScoreboardSlotArgument;
import net.minecraft.commands.arguments.StyleArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.chat.numbers.FixedFormat;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.jspecify.annotations.Nullable;

public class ScoreboardCommand {
   private static final SimpleCommandExceptionType ERROR_OBJECTIVE_ALREADY_EXISTS = new SimpleCommandExceptionType(
      Component.translatable("commands.scoreboard.objectives.add.duplicate")
   );
   private static final SimpleCommandExceptionType ERROR_DISPLAY_SLOT_ALREADY_EMPTY = new SimpleCommandExceptionType(
      Component.translatable("commands.scoreboard.objectives.display.alreadyEmpty")
   );
   private static final SimpleCommandExceptionType ERROR_DISPLAY_SLOT_ALREADY_SET = new SimpleCommandExceptionType(
      Component.translatable("commands.scoreboard.objectives.display.alreadySet")
   );
   private static final SimpleCommandExceptionType ERROR_TRIGGER_ALREADY_ENABLED = new SimpleCommandExceptionType(
      Component.translatable("commands.scoreboard.players.enable.failed")
   );
   private static final SimpleCommandExceptionType ERROR_NOT_TRIGGER = new SimpleCommandExceptionType(
      Component.translatable("commands.scoreboard.players.enable.invalid")
   );
   private static final Dynamic2CommandExceptionType ERROR_NO_VALUE = new Dynamic2CommandExceptionType(
      (objective, target) -> Component.translatableEscape("commands.scoreboard.players.get.null", objective, target)
   );
   private static final CommandResponseTracker.MessagesWithArg<ScoreHolder, Objective> RESPONSE_ENABLE = CommandResponseTracker.messages(
      ERROR_TRIGGER_ALREADY_ENABLED,
      (holder, var1, objective) -> Component.translatable(
         "commands.scoreboard.players.enable.success.single", objective.getFormattedDisplayName(), holder.getFeedbackDisplayName()
      ),
      (holderCount, var1, objective) -> Component.translatable(
         "commands.scoreboard.players.enable.success.multiple", objective.getFormattedDisplayName(), holderCount
      )
   );
   private static final CommandResponseTracker.Messages<ScoreHolder> RESPONSE_SCORE_RESET_ALL = CommandResponseTracker.messages(
      (holder, var1) -> Component.translatable("commands.scoreboard.players.reset.all.single", holder.getFeedbackDisplayName()),
      (holderCount, var1) -> Component.translatable("commands.scoreboard.players.reset.all.multiple", holderCount)
   );
   private static final CommandResponseTracker.MessagesWithArg<ScoreHolder, Objective> RESPONSE_SCORE_RESET = CommandResponseTracker.messages(
      (holder, var1, objective) -> Component.translatable(
         "commands.scoreboard.players.reset.specific.single", objective.getFormattedDisplayName(), holder.getFeedbackDisplayName()
      ),
      (holderCount, var1, objective) -> Component.translatable(
         "commands.scoreboard.players.reset.specific.multiple", objective.getFormattedDisplayName(), holderCount
      )
   );
   private static final CommandResponseTracker.MessagesWithArgs<ScoreHolder, Objective, Integer> RESPONSE_SCORE_SET = CommandResponseTracker.messages(
      (holder, var1, objective, value) -> Component.translatable(
         "commands.scoreboard.players.set.success.single", objective.getFormattedDisplayName(), holder.getFeedbackDisplayName(), value
      ),
      (holderCount, var1, objective, value) -> Component.translatable(
         "commands.scoreboard.players.set.success.multiple", objective.getFormattedDisplayName(), holderCount, value
      )
   );
   private static final CommandResponseTracker.MessagesWithArgs<ScoreHolder, Objective, Integer> RESPONSE_SCORE_ADD = CommandResponseTracker.messages(
      (holder, totalValue, objective, value) -> Component.translatable(
         "commands.scoreboard.players.add.success.single", value, objective.getFormattedDisplayName(), holder.getFeedbackDisplayName(), totalValue
      ),
      (holderCount, var1, objective, value) -> Component.translatable(
         "commands.scoreboard.players.add.success.multiple", value, objective.getFormattedDisplayName(), holderCount
      )
   );
   private static final CommandResponseTracker.MessagesWithArgs<ScoreHolder, Objective, Integer> RESPONSE_SCORE_REMOVE = CommandResponseTracker.messages(
      (holder, totalValue, objective, value) -> Component.translatable(
         "commands.scoreboard.players.remove.success.single", value, objective.getFormattedDisplayName(), holder.getFeedbackDisplayName(), totalValue
      ),
      (holderCount, var1, objective, value) -> Component.translatable(
         "commands.scoreboard.players.remove.success.multiple", value, objective.getFormattedDisplayName(), holderCount
      )
   );
   private static final CommandResponseTracker.MessagesWithArg<ScoreHolder, Objective> RESPONSE_SCORE_OPERATION = CommandResponseTracker.messages(
      (holder, totalValue, targetObjective) -> Component.translatable(
         "commands.scoreboard.players.operation.success.single", targetObjective.getFormattedDisplayName(), holder.getFeedbackDisplayName(), totalValue
      ),
      (holderCount, var1, targetObjective) -> Component.translatable(
         "commands.scoreboard.players.operation.success.multiple", targetObjective.getFormattedDisplayName(), holderCount
      )
   );
   private static final CommandResponseTracker.MessagesWithArg<ScoreHolder, Objective> RESPONSE_NUMBER_FORMAT_CLEAR = CommandResponseTracker.messages(
      (holder, var1, objective) -> Component.translatable(
         "commands.scoreboard.players.display.numberFormat.clear.success.single", holder.getFeedbackDisplayName(), objective.getFormattedDisplayName()
      ),
      (holderCount, var1, objective) -> Component.translatable(
         "commands.scoreboard.players.display.numberFormat.clear.success.multiple", holderCount, objective.getFormattedDisplayName()
      )
   );
   private static final CommandResponseTracker.MessagesWithArg<ScoreHolder, Objective> RESPONSE_NUMBER_FORMAT_SET = CommandResponseTracker.messages(
      (holder, var1, objective) -> Component.translatable(
         "commands.scoreboard.players.display.numberFormat.set.success.single", holder.getFeedbackDisplayName(), objective.getFormattedDisplayName()
      ),
      (holderCount, var1, objective) -> Component.translatable(
         "commands.scoreboard.players.display.numberFormat.set.success.multiple", holderCount, objective.getFormattedDisplayName()
      )
   );
   private static final CommandResponseTracker.MessagesWithArg<ScoreHolder, Objective> RESPONSE_DISPLAY_CLEAR = CommandResponseTracker.messages(
      (holder, var1, objective) -> Component.translatable(
         "commands.scoreboard.players.display.name.clear.success.single", holder.getFeedbackDisplayName(), objective.getFormattedDisplayName()
      ),
      (holderCount, var1, objective) -> Component.translatable(
         "commands.scoreboard.players.display.name.clear.success.multiple", holderCount, objective.getFormattedDisplayName()
      )
   );
   private static final CommandResponseTracker.MessagesWithArgs<ScoreHolder, Objective, Component> RESPONSE_DISPLAY_SET = CommandResponseTracker.messages(
      (holder, var1, objective, display) -> Component.translatable(
         "commands.scoreboard.players.display.name.set.success.single", display, holder.getFeedbackDisplayName(), objective.getFormattedDisplayName()
      ),
      (holderCount, var1, objective, display) -> Component.translatable(
         "commands.scoreboard.players.display.name.set.success.multiple", display, holderCount, objective.getFormattedDisplayName()
      )
   );

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("scoreboard")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .then(
                  ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("objectives")
                                 .then(Commands.literal("list").executes(c -> listObjectives((CommandSourceStack)c.getSource()))))
                              .then(
                                 Commands.literal("add")
                                    .then(
                                       Commands.argument("objective", StringArgumentType.word())
                                          .then(
                                             ((RequiredArgumentBuilder)Commands.argument("criteria", ObjectiveCriteriaArgument.criteria())
                                                   .executes(
                                                      c -> addObjective(
                                                         (CommandSourceStack)c.getSource(),
                                                         StringArgumentType.getString(c, "objective"),
                                                         ObjectiveCriteriaArgument.getCriteria(c, "criteria"),
                                                         Component.literal(StringArgumentType.getString(c, "objective"))
                                                      )
                                                   ))
                                                .then(
                                                   Commands.argument("displayName", ComponentArgument.textComponent(context))
                                                      .executes(
                                                         c -> addObjective(
                                                            (CommandSourceStack)c.getSource(),
                                                            StringArgumentType.getString(c, "objective"),
                                                            ObjectiveCriteriaArgument.getCriteria(c, "criteria"),
                                                            ComponentArgument.getResolvedComponent(c, "displayName")
                                                         )
                                                      )
                                                )
                                          )
                                    )
                              ))
                           .then(
                              Commands.literal("modify")
                                 .then(
                                    ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                                   "objective", ObjectiveArgument.objective()
                                                )
                                                .then(
                                                   Commands.literal("displayname")
                                                      .then(
                                                         Commands.argument("displayName", ComponentArgument.textComponent(context))
                                                            .executes(
                                                               c -> setDisplayName(
                                                                  (CommandSourceStack)c.getSource(),
                                                                  ObjectiveArgument.getObjective(c, "objective"),
                                                                  ComponentArgument.getResolvedComponent(c, "displayName")
                                                               )
                                                            )
                                                      )
                                                ))
                                             .then(createRenderTypeModify()))
                                          .then(
                                             Commands.literal("displayautoupdate")
                                                .then(
                                                   Commands.argument("value", BoolArgumentType.bool())
                                                      .executes(
                                                         c -> setDisplayAutoUpdate(
                                                            (CommandSourceStack)c.getSource(),
                                                            ObjectiveArgument.getObjective(c, "objective"),
                                                            BoolArgumentType.getBool(c, "value")
                                                         )
                                                      )
                                                )
                                          ))
                                       .then(
                                          addNumberFormats(
                                             context,
                                             Commands.literal("numberformat"),
                                             (c, numberFormat) -> setObjectiveFormat(
                                                (CommandSourceStack)c.getSource(), ObjectiveArgument.getObjective(c, "objective"), numberFormat
                                             )
                                          )
                                       )
                                 )
                           ))
                        .then(
                           Commands.literal("remove")
                              .then(
                                 Commands.argument("objective", ObjectiveArgument.objective())
                                    .executes(c -> removeObjective((CommandSourceStack)c.getSource(), ObjectiveArgument.getObjective(c, "objective")))
                              )
                        ))
                     .then(
                        Commands.literal("setdisplay")
                           .then(
                              ((RequiredArgumentBuilder)Commands.argument("slot", ScoreboardSlotArgument.displaySlot())
                                    .executes(c -> clearDisplaySlot((CommandSourceStack)c.getSource(), ScoreboardSlotArgument.getDisplaySlot(c, "slot"))))
                                 .then(
                                    Commands.argument("objective", ObjectiveArgument.objective())
                                       .executes(
                                          c -> setDisplaySlot(
                                             (CommandSourceStack)c.getSource(),
                                             ScoreboardSlotArgument.getDisplaySlot(c, "slot"),
                                             ObjectiveArgument.getObjective(c, "objective")
                                          )
                                       )
                                 )
                           )
                     )
               ))
            .then(
               ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                             "players"
                                          )
                                          .then(
                                             ((LiteralArgumentBuilder)Commands.literal("list")
                                                   .executes(c -> listTrackedPlayers((CommandSourceStack)c.getSource())))
                                                .then(
                                                   Commands.argument("target", ScoreHolderArgument.scoreHolder())
                                                      .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                      .executes(
                                                         c -> listTrackedPlayerScores(
                                                            (CommandSourceStack)c.getSource(), ScoreHolderArgument.getName(c, "target")
                                                         )
                                                      )
                                                )
                                          ))
                                       .then(
                                          Commands.literal("set")
                                             .then(
                                                Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                                                   .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                   .then(
                                                      Commands.argument("objective", ObjectiveArgument.objective())
                                                         .then(
                                                            Commands.argument("score", IntegerArgumentType.integer())
                                                               .executes(
                                                                  c -> setScore(
                                                                     (CommandSourceStack)c.getSource(),
                                                                     ScoreHolderArgument.getNamesWithDefaultWildcard(c, "targets"),
                                                                     ObjectiveArgument.getWritableObjective(c, "objective"),
                                                                     IntegerArgumentType.getInteger(c, "score")
                                                                  )
                                                               )
                                                         )
                                                   )
                                             )
                                       ))
                                    .then(
                                       Commands.literal("get")
                                          .then(
                                             Commands.argument("target", ScoreHolderArgument.scoreHolder())
                                                .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                .then(
                                                   Commands.argument("objective", ObjectiveArgument.objective())
                                                      .executes(
                                                         c -> getScore(
                                                            (CommandSourceStack)c.getSource(),
                                                            ScoreHolderArgument.getName(c, "target"),
                                                            ObjectiveArgument.getObjective(c, "objective")
                                                         )
                                                      )
                                                )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("add")
                                       .then(
                                          Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                                             .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                             .then(
                                                Commands.argument("objective", ObjectiveArgument.objective())
                                                   .then(
                                                      Commands.argument("score", IntegerArgumentType.integer(0))
                                                         .executes(
                                                            c -> addScore(
                                                               (CommandSourceStack)c.getSource(),
                                                               ScoreHolderArgument.getNamesWithDefaultWildcard(c, "targets"),
                                                               ObjectiveArgument.getWritableObjective(c, "objective"),
                                                               IntegerArgumentType.getInteger(c, "score")
                                                            )
                                                         )
                                                   )
                                             )
                                       )
                                 ))
                              .then(
                                 Commands.literal("remove")
                                    .then(
                                       Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                                          .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                          .then(
                                             Commands.argument("objective", ObjectiveArgument.objective())
                                                .then(
                                                   Commands.argument("score", IntegerArgumentType.integer(0))
                                                      .executes(
                                                         c -> removeScore(
                                                            (CommandSourceStack)c.getSource(),
                                                            ScoreHolderArgument.getNamesWithDefaultWildcard(c, "targets"),
                                                            ObjectiveArgument.getWritableObjective(c, "objective"),
                                                            IntegerArgumentType.getInteger(c, "score")
                                                         )
                                                      )
                                                )
                                          )
                                    )
                              ))
                           .then(
                              Commands.literal("reset")
                                 .then(
                                    ((RequiredArgumentBuilder)Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                                          .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                          .executes(
                                             c -> resetScores((CommandSourceStack)c.getSource(), ScoreHolderArgument.getNamesWithDefaultWildcard(c, "targets"))
                                          ))
                                       .then(
                                          Commands.argument("objective", ObjectiveArgument.objective())
                                             .executes(
                                                c -> resetScore(
                                                   (CommandSourceStack)c.getSource(),
                                                   ScoreHolderArgument.getNamesWithDefaultWildcard(c, "targets"),
                                                   ObjectiveArgument.getObjective(c, "objective")
                                                )
                                             )
                                       )
                                 )
                           ))
                        .then(
                           Commands.literal("enable")
                              .then(
                                 Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                                    .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                    .then(
                                       Commands.argument("objective", ObjectiveArgument.objective())
                                          .suggests(
                                             (c, p) -> suggestTriggers(
                                                (CommandSourceStack)c.getSource(), ScoreHolderArgument.getNamesWithDefaultWildcard(c, "targets"), p
                                             )
                                          )
                                          .executes(
                                             c -> enableTrigger(
                                                (CommandSourceStack)c.getSource(),
                                                ScoreHolderArgument.getNamesWithDefaultWildcard(c, "targets"),
                                                ObjectiveArgument.getObjective(c, "objective")
                                             )
                                          )
                                    )
                              )
                        ))
                     .then(
                        ((LiteralArgumentBuilder)Commands.literal("display")
                              .then(
                                 Commands.literal("name")
                                    .then(
                                       Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                                          .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                          .then(
                                             ((RequiredArgumentBuilder)Commands.argument("objective", ObjectiveArgument.objective())
                                                   .then(
                                                      Commands.argument("name", ComponentArgument.textComponent(context))
                                                         .executes(
                                                            c -> setScoreDisplay(
                                                               (CommandSourceStack)c.getSource(),
                                                               ScoreHolderArgument.getNamesWithDefaultWildcard(c, "targets"),
                                                               ObjectiveArgument.getObjective(c, "objective"),
                                                               ComponentArgument.getResolvedComponent(c, "name")
                                                            )
                                                         )
                                                   ))
                                                .executes(
                                                   c -> setScoreDisplay(
                                                      (CommandSourceStack)c.getSource(),
                                                      ScoreHolderArgument.getNamesWithDefaultWildcard(c, "targets"),
                                                      ObjectiveArgument.getObjective(c, "objective"),
                                                      null
                                                   )
                                                )
                                          )
                                    )
                              ))
                           .then(
                              Commands.literal("numberformat")
                                 .then(
                                    Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                                       .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                       .then(
                                          addNumberFormats(
                                             context,
                                             Commands.argument("objective", ObjectiveArgument.objective()),
                                             (c, format) -> setScoreNumberFormat(
                                                (CommandSourceStack)c.getSource(),
                                                ScoreHolderArgument.getNamesWithDefaultWildcard(c, "targets"),
                                                ObjectiveArgument.getObjective(c, "objective"),
                                                format
                                             )
                                          )
                                       )
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("operation")
                        .then(
                           Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                              .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                              .then(
                                 Commands.argument("targetObjective", ObjectiveArgument.objective())
                                    .then(
                                       Commands.argument("operation", OperationArgument.operation())
                                          .then(
                                             Commands.argument("source", ScoreHolderArgument.scoreHolders())
                                                .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                .then(
                                                   Commands.argument("sourceObjective", ObjectiveArgument.objective())
                                                      .executes(
                                                         c -> performOperation(
                                                            (CommandSourceStack)c.getSource(),
                                                            ScoreHolderArgument.getNamesWithDefaultWildcard(c, "targets"),
                                                            ObjectiveArgument.getWritableObjective(c, "targetObjective"),
                                                            OperationArgument.getOperation(c, "operation"),
                                                            ScoreHolderArgument.getNamesWithDefaultWildcard(c, "source"),
                                                            ObjectiveArgument.getObjective(c, "sourceObjective")
                                                         )
                                                      )
                                                )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static ArgumentBuilder<CommandSourceStack, ?> addNumberFormats(
      final CommandBuildContext context, final ArgumentBuilder<CommandSourceStack, ?> top, final ScoreboardCommand.NumberFormatCommandExecutor callback
   ) {
      return top.then(Commands.literal("blank").executes(c -> callback.run(c, BlankFormat.INSTANCE)))
         .then(Commands.literal("fixed").then(Commands.argument("contents", ComponentArgument.textComponent(context)).executes(c -> {
            Component contents = ComponentArgument.getResolvedComponent(c, "contents");
            return callback.run(c, new FixedFormat(contents));
         })))
         .then(Commands.literal("styled").then(Commands.argument("style", StyleArgument.style(context)).executes(c -> {
            Style style = StyleArgument.getStyle(c, "style");
            return callback.run(c, new StyledFormat(style));
         })))
         .executes(c -> callback.run(c, null));
   }

   private static LiteralArgumentBuilder<CommandSourceStack> createRenderTypeModify() {
      LiteralArgumentBuilder<CommandSourceStack> result = Commands.literal("rendertype");

      for (ObjectiveCriteria.RenderType renderType : ObjectiveCriteria.RenderType.values()) {
         result.then(
            Commands.literal(renderType.getId())
               .executes(c -> setRenderType((CommandSourceStack)c.getSource(), ObjectiveArgument.getObjective(c, "objective"), renderType))
         );
      }

      return result;
   }

   private static CompletableFuture<Suggestions> suggestTriggers(
      final CommandSourceStack source, final Collection<ScoreHolder> targets, final SuggestionsBuilder builder
   ) {
      List<String> result = Lists.newArrayList();
      Scoreboard scoreboard = source.getServer().getScoreboard();

      for (Objective objective : scoreboard.getObjectives()) {
         if (objective.getCriteria() == ObjectiveCriteria.TRIGGER) {
            boolean available = false;

            for (ScoreHolder name : targets) {
               ReadOnlyScoreInfo scoreInfo = scoreboard.getPlayerScoreInfo(name, objective);
               if (scoreInfo == null || scoreInfo.isLocked()) {
                  available = true;
                  break;
               }
            }

            if (available) {
               result.add(objective.getName());
            }
         }
      }

      return SharedSuggestionProvider.suggest(result, builder);
   }

   private static int getScore(final CommandSourceStack source, final ScoreHolder target, final Objective objective) throws CommandSyntaxException {
      Scoreboard scoreboard = source.getServer().getScoreboard();
      ReadOnlyScoreInfo score = scoreboard.getPlayerScoreInfo(target, objective);
      if (score == null) {
         throw ERROR_NO_VALUE.create(objective.getName(), target.getFeedbackDisplayName());
      }

      source.sendSuccess(
         () -> Component.translatable(
            "commands.scoreboard.players.get.success", target.getFeedbackDisplayName(), score.value(), objective.getFormattedDisplayName()
         ),
         false
      );
      return score.value();
   }

   private static int performOperation(
      final CommandSourceStack source,
      final Collection<ScoreHolder> targets,
      final Objective targetObjective,
      final OperationArgument.Operation operation,
      final Collection<ScoreHolder> sources,
      final Objective sourceObjective
   ) throws CommandSyntaxException {
      Scoreboard scoreboard = source.getServer().getScoreboard();
      CommandResponseTracker<ScoreHolder> tracker = CommandResponseTracker.create();

      for (ScoreHolder target : targets) {
         ScoreAccess score = scoreboard.getOrCreatePlayerScore(target, targetObjective);

         for (ScoreHolder from : sources) {
            ScoreAccess sourceScore = scoreboard.getOrCreatePlayerScore(from, sourceObjective);
            operation.apply(score, sourceScore);
         }

         tracker.track(target, score.get());
      }

      return tracker.sendFeedback(source, true, CommandResponseTracker.ElementType.ANY, RESPONSE_SCORE_OPERATION, targetObjective);
   }

   private static int enableTrigger(final CommandSourceStack source, final Collection<ScoreHolder> targets, final Objective objective) throws CommandSyntaxException {
      if (objective.getCriteria() != ObjectiveCriteria.TRIGGER) {
         throw ERROR_NOT_TRIGGER.create();
      }

      CommandResponseTracker<ScoreHolder> tracker = CommandResponseTracker.create();
      Scoreboard scoreboard = source.getServer().getScoreboard();

      for (ScoreHolder target : targets) {
         ScoreAccess score = scoreboard.getOrCreatePlayerScore(target, objective);
         if (score.locked()) {
            score.unlock();
            tracker.track(target);
         }
      }

      return tracker.sendFeedback(source, true, RESPONSE_ENABLE, objective);
   }

   private static int resetScores(final CommandSourceStack source, final Collection<ScoreHolder> targets) throws CommandSyntaxException {
      CommandResponseTracker<ScoreHolder> tracker = CommandResponseTracker.create();
      Scoreboard scoreboard = source.getServer().getScoreboard();

      for (ScoreHolder target : targets) {
         scoreboard.resetAllPlayerScores(target);
         tracker.track(target);
      }

      return tracker.sendFeedback(source, true, RESPONSE_SCORE_RESET_ALL);
   }

   private static int resetScore(final CommandSourceStack source, final Collection<ScoreHolder> targets, final Objective objective) throws CommandSyntaxException {
      CommandResponseTracker<ScoreHolder> tracker = CommandResponseTracker.create();
      Scoreboard scoreboard = source.getServer().getScoreboard();

      for (ScoreHolder target : targets) {
         scoreboard.resetSinglePlayerScore(target, objective);
         tracker.track(target);
      }

      return tracker.sendFeedback(source, true, RESPONSE_SCORE_RESET, objective);
   }

   private static int setScore(final CommandSourceStack source, final Collection<ScoreHolder> targets, final Objective objective, final int value) throws CommandSyntaxException {
      CommandResponseTracker<ScoreHolder> tracker = CommandResponseTracker.create();
      Scoreboard scoreboard = source.getServer().getScoreboard();

      for (ScoreHolder target : targets) {
         scoreboard.getOrCreatePlayerScore(target, objective).set(value);
         tracker.track(target, value);
      }

      return tracker.sendFeedback(source, true, RESPONSE_SCORE_SET, objective, value);
   }

   private static int setScoreDisplay(
      final CommandSourceStack source, final Collection<ScoreHolder> targets, final Objective objective, final @Nullable Component display
   ) throws CommandSyntaxException {
      CommandResponseTracker<ScoreHolder> tracker = CommandResponseTracker.create();
      Scoreboard scoreboard = source.getServer().getScoreboard();

      for (ScoreHolder target : targets) {
         scoreboard.getOrCreatePlayerScore(target, objective).display(display);
         tracker.track(target);
      }

      return display == null
         ? tracker.sendFeedback(source, true, RESPONSE_DISPLAY_CLEAR, objective)
         : tracker.sendFeedback(source, true, RESPONSE_DISPLAY_SET, objective, display);
   }

   private static int setScoreNumberFormat(
      final CommandSourceStack source, final Collection<ScoreHolder> targets, final Objective objective, final @Nullable NumberFormat numberFormat
   ) throws CommandSyntaxException {
      CommandResponseTracker<ScoreHolder> tracker = CommandResponseTracker.create();
      Scoreboard scoreboard = source.getServer().getScoreboard();

      for (ScoreHolder target : targets) {
         scoreboard.getOrCreatePlayerScore(target, objective).numberFormatOverride(numberFormat);
         tracker.track(target);
      }

      return numberFormat == null
         ? tracker.sendFeedback(source, true, RESPONSE_NUMBER_FORMAT_CLEAR, objective)
         : tracker.sendFeedback(source, true, RESPONSE_NUMBER_FORMAT_SET, objective);
   }

   private static int addScore(final CommandSourceStack source, final Collection<ScoreHolder> targets, final Objective objective, final int value) throws CommandSyntaxException {
      CommandResponseTracker<ScoreHolder> tracker = CommandResponseTracker.create();
      Scoreboard scoreboard = source.getServer().getScoreboard();

      for (ScoreHolder target : targets) {
         ScoreAccess score = scoreboard.getOrCreatePlayerScore(target, objective);
         score.set(score.get() + value);
         tracker.track(target, score.get());
      }

      return tracker.sendFeedback(source, true, CommandResponseTracker.ElementType.ANY, RESPONSE_SCORE_ADD, objective, value);
   }

   private static int removeScore(final CommandSourceStack source, final Collection<ScoreHolder> targets, final Objective objective, final int value) throws CommandSyntaxException {
      CommandResponseTracker<ScoreHolder> tracker = CommandResponseTracker.create();
      Scoreboard scoreboard = source.getServer().getScoreboard();

      for (ScoreHolder target : targets) {
         ScoreAccess score = scoreboard.getOrCreatePlayerScore(target, objective);
         score.set(score.get() - value);
         tracker.track(target, score.get());
      }

      return tracker.sendFeedback(source, true, CommandResponseTracker.ElementType.ANY, RESPONSE_SCORE_REMOVE, objective, value);
   }

   private static int listTrackedPlayers(final CommandSourceStack source) {
      Collection<ScoreHolder> entities = source.getServer().getScoreboard().getTrackedPlayers();
      if (entities.isEmpty()) {
         source.sendSuccess(() -> Component.translatable("commands.scoreboard.players.list.empty"), false);
      } else {
         source.sendSuccess(
            () -> Component.translatable(
               "commands.scoreboard.players.list.success", entities.size(), ComponentUtils.formatList(entities, ScoreHolder::getFeedbackDisplayName)
            ),
            false
         );
      }

      return entities.size();
   }

   private static int listTrackedPlayerScores(final CommandSourceStack source, final ScoreHolder entity) {
      Object2IntMap<Objective> scores = source.getServer().getScoreboard().listPlayerScores(entity);
      if (scores.isEmpty()) {
         source.sendSuccess(() -> Component.translatable("commands.scoreboard.players.list.entity.empty", entity.getFeedbackDisplayName()), false);
      } else {
         source.sendSuccess(
            () -> Component.translatable("commands.scoreboard.players.list.entity.success", entity.getFeedbackDisplayName(), scores.size()), false
         );
         Object2IntMaps.fastForEach(
            scores,
            entry -> source.sendSuccess(
               () -> Component.translatable(
                  "commands.scoreboard.players.list.entity.entry", ((Objective)entry.getKey()).getFormattedDisplayName(), entry.getIntValue()
               ),
               false
            )
         );
      }

      return scores.size();
   }

   private static int clearDisplaySlot(final CommandSourceStack source, final DisplaySlot slot) throws CommandSyntaxException {
      Scoreboard scoreboard = source.getServer().getScoreboard();
      if (scoreboard.getDisplayObjective(slot) == null) {
         throw ERROR_DISPLAY_SLOT_ALREADY_EMPTY.create();
      }

      scoreboard.setDisplayObjective(slot, null);
      source.sendSuccess(() -> Component.translatable("commands.scoreboard.objectives.display.cleared", slot.getSerializedName()), true);
      return 0;
   }

   private static int setDisplaySlot(final CommandSourceStack source, final DisplaySlot slot, final Objective objective) throws CommandSyntaxException {
      Scoreboard scoreboard = source.getServer().getScoreboard();
      if (scoreboard.getDisplayObjective(slot) == objective) {
         throw ERROR_DISPLAY_SLOT_ALREADY_SET.create();
      }

      scoreboard.setDisplayObjective(slot, objective);
      source.sendSuccess(() -> Component.translatable("commands.scoreboard.objectives.display.set", slot.getSerializedName(), objective.getDisplayName()), true);
      return 0;
   }

   private static int setDisplayName(final CommandSourceStack source, final Objective objective, final Component displayName) {
      if (!objective.getDisplayName().equals(displayName)) {
         objective.setDisplayName(displayName);
         source.sendSuccess(
            () -> Component.translatable("commands.scoreboard.objectives.modify.displayname", objective.getName(), objective.getFormattedDisplayName()), true
         );
      }

      return 0;
   }

   private static int setDisplayAutoUpdate(final CommandSourceStack source, final Objective objective, final boolean displayAutoUpdate) {
      if (objective.displayAutoUpdate() != displayAutoUpdate) {
         objective.setDisplayAutoUpdate(displayAutoUpdate);
         if (displayAutoUpdate) {
            source.sendSuccess(
               () -> Component.translatable(
                  "commands.scoreboard.objectives.modify.displayAutoUpdate.enable", objective.getName(), objective.getFormattedDisplayName()
               ),
               true
            );
         } else {
            source.sendSuccess(
               () -> Component.translatable(
                  "commands.scoreboard.objectives.modify.displayAutoUpdate.disable", objective.getName(), objective.getFormattedDisplayName()
               ),
               true
            );
         }
      }

      return 0;
   }

   private static int setObjectiveFormat(final CommandSourceStack source, final Objective objective, final @Nullable NumberFormat numberFormat) {
      objective.setNumberFormat(numberFormat);
      if (numberFormat != null) {
         source.sendSuccess(() -> Component.translatable("commands.scoreboard.objectives.modify.objectiveFormat.set", objective.getName()), true);
      } else {
         source.sendSuccess(() -> Component.translatable("commands.scoreboard.objectives.modify.objectiveFormat.clear", objective.getName()), true);
      }

      return 0;
   }

   private static int setRenderType(final CommandSourceStack source, final Objective objective, final ObjectiveCriteria.RenderType renderType) {
      if (objective.getRenderType() != renderType) {
         objective.setRenderType(renderType);
         source.sendSuccess(() -> Component.translatable("commands.scoreboard.objectives.modify.rendertype", objective.getFormattedDisplayName()), true);
      }

      return 0;
   }

   private static int removeObjective(final CommandSourceStack source, final Objective objective) {
      Scoreboard scoreboard = source.getServer().getScoreboard();
      scoreboard.removeObjective(objective);
      source.sendSuccess(() -> Component.translatable("commands.scoreboard.objectives.remove.success", objective.getFormattedDisplayName()), true);
      return scoreboard.getObjectives().size();
   }

   private static int addObjective(final CommandSourceStack source, final String name, final ObjectiveCriteria criteria, final Component displayName) throws CommandSyntaxException {
      Scoreboard scoreboard = source.getServer().getScoreboard();
      if (scoreboard.getObjective(name) != null) {
         throw ERROR_OBJECTIVE_ALREADY_EXISTS.create();
      }

      scoreboard.addObjective(name, criteria, displayName, criteria.getDefaultRenderType(), false, null);
      Objective objective = scoreboard.getObjective(name);
      source.sendSuccess(() -> Component.translatable("commands.scoreboard.objectives.add.success", objective.getFormattedDisplayName()), true);
      return scoreboard.getObjectives().size();
   }

   private static int listObjectives(final CommandSourceStack source) {
      Collection<Objective> objectives = source.getServer().getScoreboard().getObjectives();
      if (objectives.isEmpty()) {
         source.sendSuccess(() -> Component.translatable("commands.scoreboard.objectives.list.empty"), false);
      } else {
         source.sendSuccess(
            () -> Component.translatable(
               "commands.scoreboard.objectives.list.success", objectives.size(), ComponentUtils.formatList(objectives, Objective::getFormattedDisplayName)
            ),
            false
         );
      }

      return objectives.size();
   }

   @FunctionalInterface
   public interface NumberFormatCommandExecutor {
      int run(CommandContext<CommandSourceStack> context, @Nullable NumberFormat format) throws CommandSyntaxException;
   }
}
