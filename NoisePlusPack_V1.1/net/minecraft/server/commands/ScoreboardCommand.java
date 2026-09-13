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
      (p_308842_, p_308843_) -> Component.translatableEscape("commands.scoreboard.players.get.null", p_308842_, p_308843_)
   );

   public static void register(CommandDispatcher<CommandSourceStack> p_138469_, CommandBuildContext p_332947_) {
      p_138469_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("scoreboard")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .then(
                  ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("objectives")
                                 .then(Commands.literal("list").executes(p_138585_ -> listObjectives((CommandSourceStack)p_138585_.getSource()))))
                              .then(
                                 Commands.literal("add")
                                    .then(
                                       Commands.argument("objective", StringArgumentType.word())
                                          .then(
                                             ((RequiredArgumentBuilder)Commands.argument("criteria", ObjectiveCriteriaArgument.criteria())
                                                   .executes(
                                                      p_138583_ -> addObjective(
                                                         (CommandSourceStack)p_138583_.getSource(),
                                                         StringArgumentType.getString(p_138583_, "objective"),
                                                         ObjectiveCriteriaArgument.getCriteria(p_138583_, "criteria"),
                                                         Component.literal(StringArgumentType.getString(p_138583_, "objective"))
                                                      )
                                                   ))
                                                .then(
                                                   Commands.argument("displayName", ComponentArgument.textComponent(p_332947_))
                                                      .executes(
                                                         p_390089_ -> addObjective(
                                                            (CommandSourceStack)p_390089_.getSource(),
                                                            StringArgumentType.getString(p_390089_, "objective"),
                                                            ObjectiveCriteriaArgument.getCriteria(p_390089_, "criteria"),
                                                            ComponentArgument.getResolvedComponent(p_390089_, "displayName")
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
                                                         Commands.argument("displayName", ComponentArgument.textComponent(p_332947_))
                                                            .executes(
                                                               p_390086_ -> setDisplayName(
                                                                  (CommandSourceStack)p_390086_.getSource(),
                                                                  ObjectiveArgument.getObjective(p_390086_, "objective"),
                                                                  ComponentArgument.getResolvedComponent(p_390086_, "displayName")
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
                                                         p_308844_ -> setDisplayAutoUpdate(
                                                            (CommandSourceStack)p_308844_.getSource(),
                                                            ObjectiveArgument.getObjective(p_308844_, "objective"),
                                                            BoolArgumentType.getBool(p_308844_, "value")
                                                         )
                                                      )
                                                )
                                          ))
                                       .then(
                                          addNumberFormats(
                                             p_332947_,
                                             Commands.literal("numberformat"),
                                             (p_308837_, p_308838_) -> setObjectiveFormat(
                                                (CommandSourceStack)p_308837_.getSource(), ObjectiveArgument.getObjective(p_308837_, "objective"), p_308838_
                                             )
                                          )
                                       )
                                 )
                           ))
                        .then(
                           Commands.literal("remove")
                              .then(
                                 Commands.argument("objective", ObjectiveArgument.objective())
                                    .executes(
                                       p_138577_ -> removeObjective(
                                          (CommandSourceStack)p_138577_.getSource(), ObjectiveArgument.getObjective(p_138577_, "objective")
                                       )
                                    )
                              )
                        ))
                     .then(
                        Commands.literal("setdisplay")
                           .then(
                              ((RequiredArgumentBuilder)Commands.argument("slot", ScoreboardSlotArgument.displaySlot())
                                    .executes(
                                       p_296535_ -> clearDisplaySlot(
                                          (CommandSourceStack)p_296535_.getSource(), ScoreboardSlotArgument.getDisplaySlot(p_296535_, "slot")
                                       )
                                    ))
                                 .then(
                                    Commands.argument("objective", ObjectiveArgument.objective())
                                       .executes(
                                          p_296536_ -> setDisplaySlot(
                                             (CommandSourceStack)p_296536_.getSource(),
                                             ScoreboardSlotArgument.getDisplaySlot(p_296536_, "slot"),
                                             ObjectiveArgument.getObjective(p_296536_, "objective")
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
                                                   .executes(p_138571_ -> listTrackedPlayers((CommandSourceStack)p_138571_.getSource())))
                                                .then(
                                                   Commands.argument("target", ScoreHolderArgument.scoreHolder())
                                                      .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                      .executes(
                                                         p_308852_ -> listTrackedPlayerScores(
                                                            (CommandSourceStack)p_308852_.getSource(), ScoreHolderArgument.getName(p_308852_, "target")
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
                                                                  p_138567_ -> setScore(
                                                                     (CommandSourceStack)p_138567_.getSource(),
                                                                     ScoreHolderArgument.getNamesWithDefaultWildcard(p_138567_, "targets"),
                                                                     ObjectiveArgument.getWritableObjective(p_138567_, "objective"),
                                                                     IntegerArgumentType.getInteger(p_138567_, "score")
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
                                                         p_308815_ -> getScore(
                                                            (CommandSourceStack)p_308815_.getSource(),
                                                            ScoreHolderArgument.getName(p_308815_, "target"),
                                                            ObjectiveArgument.getObjective(p_308815_, "objective")
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
                                                            p_138563_ -> addScore(
                                                               (CommandSourceStack)p_138563_.getSource(),
                                                               ScoreHolderArgument.getNamesWithDefaultWildcard(p_138563_, "targets"),
                                                               ObjectiveArgument.getWritableObjective(p_138563_, "objective"),
                                                               IntegerArgumentType.getInteger(p_138563_, "score")
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
                                                         p_138561_ -> removeScore(
                                                            (CommandSourceStack)p_138561_.getSource(),
                                                            ScoreHolderArgument.getNamesWithDefaultWildcard(p_138561_, "targets"),
                                                            ObjectiveArgument.getWritableObjective(p_138561_, "objective"),
                                                            IntegerArgumentType.getInteger(p_138561_, "score")
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
                                             p_138559_ -> resetScores(
                                                (CommandSourceStack)p_138559_.getSource(),
                                                ScoreHolderArgument.getNamesWithDefaultWildcard(p_138559_, "targets")
                                             )
                                          ))
                                       .then(
                                          Commands.argument("objective", ObjectiveArgument.objective())
                                             .executes(
                                                p_138550_ -> resetScore(
                                                   (CommandSourceStack)p_138550_.getSource(),
                                                   ScoreHolderArgument.getNamesWithDefaultWildcard(p_138550_, "targets"),
                                                   ObjectiveArgument.getObjective(p_138550_, "objective")
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
                                             (p_138473_, p_138474_) -> suggestTriggers(
                                                (CommandSourceStack)p_138473_.getSource(),
                                                ScoreHolderArgument.getNamesWithDefaultWildcard(p_138473_, "targets"),
                                                p_138474_
                                             )
                                          )
                                          .executes(
                                             p_138537_ -> enableTrigger(
                                                (CommandSourceStack)p_138537_.getSource(),
                                                ScoreHolderArgument.getNamesWithDefaultWildcard(p_138537_, "targets"),
                                                ObjectiveArgument.getObjective(p_138537_, "objective")
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
                                                      Commands.argument("name", ComponentArgument.textComponent(p_332947_))
                                                         .executes(
                                                            p_390085_ -> setScoreDisplay(
                                                               (CommandSourceStack)p_390085_.getSource(),
                                                               ScoreHolderArgument.getNamesWithDefaultWildcard(p_390085_, "targets"),
                                                               ObjectiveArgument.getObjective(p_390085_, "objective"),
                                                               ComponentArgument.getResolvedComponent(p_390085_, "name")
                                                            )
                                                         )
                                                   ))
                                                .executes(
                                                   p_308862_ -> setScoreDisplay(
                                                      (CommandSourceStack)p_308862_.getSource(),
                                                      ScoreHolderArgument.getNamesWithDefaultWildcard(p_308862_, "targets"),
                                                      ObjectiveArgument.getObjective(p_308862_, "objective"),
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
                                             p_332947_,
                                             Commands.argument("objective", ObjectiveArgument.objective()),
                                             (p_308863_, p_308864_) -> setScoreNumberFormat(
                                                (CommandSourceStack)p_308863_.getSource(),
                                                ScoreHolderArgument.getNamesWithDefaultWildcard(p_308863_, "targets"),
                                                ObjectiveArgument.getObjective(p_308863_, "objective"),
                                                p_308864_
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
                                                         p_138471_ -> performOperation(
                                                            (CommandSourceStack)p_138471_.getSource(),
                                                            ScoreHolderArgument.getNamesWithDefaultWildcard(p_138471_, "targets"),
                                                            ObjectiveArgument.getWritableObjective(p_138471_, "targetObjective"),
                                                            OperationArgument.getOperation(p_138471_, "operation"),
                                                            ScoreHolderArgument.getNamesWithDefaultWildcard(p_138471_, "source"),
                                                            ObjectiveArgument.getObjective(p_138471_, "sourceObjective")
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
      CommandBuildContext p_330211_, ArgumentBuilder<CommandSourceStack, ?> p_312441_, ScoreboardCommand.NumberFormatCommandExecutor p_310857_
   ) {
      return p_312441_.then(Commands.literal("blank").executes(p_308836_ -> p_310857_.run(p_308836_, BlankFormat.INSTANCE)))
         .then(Commands.literal("fixed").then(Commands.argument("contents", ComponentArgument.textComponent(p_330211_)).executes(p_390088_ -> {
            Component component = ComponentArgument.getResolvedComponent(p_390088_, "contents");
            return p_310857_.run(p_390088_, new FixedFormat(component));
         })))
         .then(Commands.literal("styled").then(Commands.argument("style", StyleArgument.style(p_330211_)).executes(p_308869_ -> {
            Style style = StyleArgument.getStyle(p_308869_, "style");
            return p_310857_.run(p_308869_, new StyledFormat(style));
         })))
         .executes(p_308875_ -> p_310857_.run(p_308875_, null));
   }

   private static LiteralArgumentBuilder<CommandSourceStack> createRenderTypeModify() {
      LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder = Commands.literal("rendertype");

      for (ObjectiveCriteria.RenderType objectivecriteria$rendertype : ObjectiveCriteria.RenderType.values()) {
         literalargumentbuilder.then(
            Commands.literal(objectivecriteria$rendertype.getId())
               .executes(
                  p_138532_ -> setRenderType(
                     (CommandSourceStack)p_138532_.getSource(), ObjectiveArgument.getObjective(p_138532_, "objective"), objectivecriteria$rendertype
                  )
               )
         );
      }

      return literalargumentbuilder;
   }

   private static CompletableFuture<Suggestions> suggestTriggers(CommandSourceStack p_138511_, Collection<ScoreHolder> p_138512_, SuggestionsBuilder p_138513_) {
      List<String> list = Lists.newArrayList();
      Scoreboard scoreboard = p_138511_.getServer().getScoreboard();

      for (Objective objective : scoreboard.getObjectives()) {
         if (objective.getCriteria() == ObjectiveCriteria.TRIGGER) {
            boolean flag = false;

            for (ScoreHolder scoreholder : p_138512_) {
               ReadOnlyScoreInfo readonlyscoreinfo = scoreboard.getPlayerScoreInfo(scoreholder, objective);
               if (readonlyscoreinfo == null || readonlyscoreinfo.isLocked()) {
                  flag = true;
                  break;
               }
            }

            if (flag) {
               list.add(objective.getName());
            }
         }
      }

      return SharedSuggestionProvider.suggest(list, p_138513_);
   }

   private static int getScore(CommandSourceStack p_138499_, ScoreHolder p_311327_, Objective p_138501_) throws CommandSyntaxException {
      Scoreboard scoreboard = p_138499_.getServer().getScoreboard();
      ReadOnlyScoreInfo readonlyscoreinfo = scoreboard.getPlayerScoreInfo(p_311327_, p_138501_);
      if (readonlyscoreinfo == null) {
         throw ERROR_NO_VALUE.create(p_138501_.getName(), p_311327_.getFeedbackDisplayName());
      }

      p_138499_.sendSuccess(
         () -> Component.translatable(
            "commands.scoreboard.players.get.success", p_311327_.getFeedbackDisplayName(), readonlyscoreinfo.value(), p_138501_.getFormattedDisplayName()
         ),
         false
      );
      return readonlyscoreinfo.value();
   }

   private static Component getFirstTargetName(Collection<ScoreHolder> p_312538_) {
      return p_312538_.iterator().next().getFeedbackDisplayName();
   }

   private static int performOperation(
      CommandSourceStack p_138524_,
      Collection<ScoreHolder> p_138525_,
      Objective p_138526_,
      OperationArgument.Operation p_138527_,
      Collection<ScoreHolder> p_138528_,
      Objective p_138529_
   ) throws CommandSyntaxException {
      Scoreboard scoreboard = p_138524_.getServer().getScoreboard();
      int i = 0;

      for (ScoreHolder scoreholder : p_138525_) {
         ScoreAccess scoreaccess = scoreboard.getOrCreatePlayerScore(scoreholder, p_138526_);

         for (ScoreHolder scoreholder1 : p_138528_) {
            ScoreAccess scoreaccess1 = scoreboard.getOrCreatePlayerScore(scoreholder1, p_138529_);
            p_138527_.apply(scoreaccess, scoreaccess1);
         }

         i += scoreaccess.get();
      }

      if (p_138525_.size() == 1) {
         int j = i;
         p_138524_.sendSuccess(
            () -> Component.translatable(
               "commands.scoreboard.players.operation.success.single", p_138526_.getFormattedDisplayName(), getFirstTargetName(p_138525_), j
            ),
            true
         );
      } else {
         p_138524_.sendSuccess(
            () -> Component.translatable("commands.scoreboard.players.operation.success.multiple", p_138526_.getFormattedDisplayName(), p_138525_.size()), true
         );
      }

      return i;
   }

   private static int enableTrigger(CommandSourceStack p_138515_, Collection<ScoreHolder> p_138516_, Objective p_138517_) throws CommandSyntaxException {
      if (p_138517_.getCriteria() != ObjectiveCriteria.TRIGGER) {
         throw ERROR_NOT_TRIGGER.create();
      }

      Scoreboard scoreboard = p_138515_.getServer().getScoreboard();
      int i = 0;

      for (ScoreHolder scoreholder : p_138516_) {
         ScoreAccess scoreaccess = scoreboard.getOrCreatePlayerScore(scoreholder, p_138517_);
         if (scoreaccess.locked()) {
            scoreaccess.unlock();
            i++;
         }
      }

      if (i == 0) {
         throw ERROR_TRIGGER_ALREADY_ENABLED.create();
      }

      if (p_138516_.size() == 1) {
         p_138515_.sendSuccess(
            () -> Component.translatable(
               "commands.scoreboard.players.enable.success.single", p_138517_.getFormattedDisplayName(), getFirstTargetName(p_138516_)
            ),
            true
         );
      } else {
         p_138515_.sendSuccess(
            () -> Component.translatable("commands.scoreboard.players.enable.success.multiple", p_138517_.getFormattedDisplayName(), p_138516_.size()), true
         );
      }

      return i;
   }

   private static int resetScores(CommandSourceStack p_138508_, Collection<ScoreHolder> p_138509_) {
      Scoreboard scoreboard = p_138508_.getServer().getScoreboard();

      for (ScoreHolder scoreholder : p_138509_) {
         scoreboard.resetAllPlayerScores(scoreholder);
      }

      if (p_138509_.size() == 1) {
         p_138508_.sendSuccess(() -> Component.translatable("commands.scoreboard.players.reset.all.single", getFirstTargetName(p_138509_)), true);
      } else {
         p_138508_.sendSuccess(() -> Component.translatable("commands.scoreboard.players.reset.all.multiple", p_138509_.size()), true);
      }

      return p_138509_.size();
   }

   private static int resetScore(CommandSourceStack p_138541_, Collection<ScoreHolder> p_138542_, Objective p_138543_) {
      Scoreboard scoreboard = p_138541_.getServer().getScoreboard();

      for (ScoreHolder scoreholder : p_138542_) {
         scoreboard.resetSinglePlayerScore(scoreholder, p_138543_);
      }

      if (p_138542_.size() == 1) {
         p_138541_.sendSuccess(
            () -> Component.translatable(
               "commands.scoreboard.players.reset.specific.single", p_138543_.getFormattedDisplayName(), getFirstTargetName(p_138542_)
            ),
            true
         );
      } else {
         p_138541_.sendSuccess(
            () -> Component.translatable("commands.scoreboard.players.reset.specific.multiple", p_138543_.getFormattedDisplayName(), p_138542_.size()), true
         );
      }

      return p_138542_.size();
   }

   private static int setScore(CommandSourceStack p_138519_, Collection<ScoreHolder> p_138520_, Objective p_138521_, int p_138522_) {
      Scoreboard scoreboard = p_138519_.getServer().getScoreboard();

      for (ScoreHolder scoreholder : p_138520_) {
         scoreboard.getOrCreatePlayerScore(scoreholder, p_138521_).set(p_138522_);
      }

      if (p_138520_.size() == 1) {
         p_138519_.sendSuccess(
            () -> Component.translatable(
               "commands.scoreboard.players.set.success.single", p_138521_.getFormattedDisplayName(), getFirstTargetName(p_138520_), p_138522_
            ),
            true
         );
      } else {
         p_138519_.sendSuccess(
            () -> Component.translatable("commands.scoreboard.players.set.success.multiple", p_138521_.getFormattedDisplayName(), p_138520_.size(), p_138522_),
            true
         );
      }

      return p_138522_ * p_138520_.size();
   }

   private static int setScoreDisplay(CommandSourceStack p_311963_, Collection<ScoreHolder> p_313027_, Objective p_309793_, @Nullable Component p_313172_) {
      Scoreboard scoreboard = p_311963_.getServer().getScoreboard();

      for (ScoreHolder scoreholder : p_313027_) {
         scoreboard.getOrCreatePlayerScore(scoreholder, p_309793_).display(p_313172_);
      }

      if (p_313172_ == null) {
         if (p_313027_.size() == 1) {
            p_311963_.sendSuccess(
               () -> Component.translatable(
                  "commands.scoreboard.players.display.name.clear.success.single", getFirstTargetName(p_313027_), p_309793_.getFormattedDisplayName()
               ),
               true
            );
         } else {
            p_311963_.sendSuccess(
               () -> Component.translatable(
                  "commands.scoreboard.players.display.name.clear.success.multiple", p_313027_.size(), p_309793_.getFormattedDisplayName()
               ),
               true
            );
         }
      } else if (p_313027_.size() == 1) {
         p_311963_.sendSuccess(
            () -> Component.translatable(
               "commands.scoreboard.players.display.name.set.success.single", p_313172_, getFirstTargetName(p_313027_), p_309793_.getFormattedDisplayName()
            ),
            true
         );
      } else {
         p_311963_.sendSuccess(
            () -> Component.translatable(
               "commands.scoreboard.players.display.name.set.success.multiple", p_313172_, p_313027_.size(), p_309793_.getFormattedDisplayName()
            ),
            true
         );
      }

      return p_313027_.size();
   }

   private static int setScoreNumberFormat(
      CommandSourceStack p_310386_, Collection<ScoreHolder> p_310803_, Objective p_311141_, @Nullable NumberFormat p_311948_
   ) {
      Scoreboard scoreboard = p_310386_.getServer().getScoreboard();

      for (ScoreHolder scoreholder : p_310803_) {
         scoreboard.getOrCreatePlayerScore(scoreholder, p_311141_).numberFormatOverride(p_311948_);
      }

      if (p_311948_ == null) {
         if (p_310803_.size() == 1) {
            p_310386_.sendSuccess(
               () -> Component.translatable(
                  "commands.scoreboard.players.display.numberFormat.clear.success.single", getFirstTargetName(p_310803_), p_311141_.getFormattedDisplayName()
               ),
               true
            );
         } else {
            p_310386_.sendSuccess(
               () -> Component.translatable(
                  "commands.scoreboard.players.display.numberFormat.clear.success.multiple", p_310803_.size(), p_311141_.getFormattedDisplayName()
               ),
               true
            );
         }
      } else if (p_310803_.size() == 1) {
         p_310386_.sendSuccess(
            () -> Component.translatable(
               "commands.scoreboard.players.display.numberFormat.set.success.single", getFirstTargetName(p_310803_), p_311141_.getFormattedDisplayName()
            ),
            true
         );
      } else {
         p_310386_.sendSuccess(
            () -> Component.translatable(
               "commands.scoreboard.players.display.numberFormat.set.success.multiple", p_310803_.size(), p_311141_.getFormattedDisplayName()
            ),
            true
         );
      }

      return p_310803_.size();
   }

   private static int addScore(CommandSourceStack p_138545_, Collection<ScoreHolder> p_138546_, Objective p_138547_, int p_138548_) {
      Scoreboard scoreboard = p_138545_.getServer().getScoreboard();
      int i = 0;

      for (ScoreHolder scoreholder : p_138546_) {
         ScoreAccess scoreaccess = scoreboard.getOrCreatePlayerScore(scoreholder, p_138547_);
         scoreaccess.set(scoreaccess.get() + p_138548_);
         i += scoreaccess.get();
      }

      if (p_138546_.size() == 1) {
         int j = i;
         p_138545_.sendSuccess(
            () -> Component.translatable(
               "commands.scoreboard.players.add.success.single", p_138548_, p_138547_.getFormattedDisplayName(), getFirstTargetName(p_138546_), j
            ),
            true
         );
      } else {
         p_138545_.sendSuccess(
            () -> Component.translatable("commands.scoreboard.players.add.success.multiple", p_138548_, p_138547_.getFormattedDisplayName(), p_138546_.size()),
            true
         );
      }

      return i;
   }

   private static int removeScore(CommandSourceStack p_138554_, Collection<ScoreHolder> p_138555_, Objective p_138556_, int p_138557_) {
      Scoreboard scoreboard = p_138554_.getServer().getScoreboard();
      int i = 0;

      for (ScoreHolder scoreholder : p_138555_) {
         ScoreAccess scoreaccess = scoreboard.getOrCreatePlayerScore(scoreholder, p_138556_);
         scoreaccess.set(scoreaccess.get() - p_138557_);
         i += scoreaccess.get();
      }

      if (p_138555_.size() == 1) {
         int j = i;
         p_138554_.sendSuccess(
            () -> Component.translatable(
               "commands.scoreboard.players.remove.success.single", p_138557_, p_138556_.getFormattedDisplayName(), getFirstTargetName(p_138555_), j
            ),
            true
         );
      } else {
         p_138554_.sendSuccess(
            () -> Component.translatable(
               "commands.scoreboard.players.remove.success.multiple", p_138557_, p_138556_.getFormattedDisplayName(), p_138555_.size()
            ),
            true
         );
      }

      return i;
   }

   private static int listTrackedPlayers(CommandSourceStack p_138476_) {
      Collection<ScoreHolder> collection = p_138476_.getServer().getScoreboard().getTrackedPlayers();
      if (collection.isEmpty()) {
         p_138476_.sendSuccess(() -> Component.translatable("commands.scoreboard.players.list.empty"), false);
      } else {
         p_138476_.sendSuccess(
            () -> Component.translatable(
               "commands.scoreboard.players.list.success", collection.size(), ComponentUtils.formatList(collection, ScoreHolder::getFeedbackDisplayName)
            ),
            false
         );
      }

      return collection.size();
   }

   private static int listTrackedPlayerScores(CommandSourceStack p_138496_, ScoreHolder p_310716_) {
      Object2IntMap<Objective> object2intmap = p_138496_.getServer().getScoreboard().listPlayerScores(p_310716_);
      if (object2intmap.isEmpty()) {
         p_138496_.sendSuccess(() -> Component.translatable("commands.scoreboard.players.list.entity.empty", p_310716_.getFeedbackDisplayName()), false);
      } else {
         p_138496_.sendSuccess(
            () -> Component.translatable("commands.scoreboard.players.list.entity.success", p_310716_.getFeedbackDisplayName(), object2intmap.size()), false
         );
         Object2IntMaps.fastForEach(
            object2intmap,
            p_308821_ -> p_138496_.sendSuccess(
               () -> Component.translatable(
                  "commands.scoreboard.players.list.entity.entry", ((Objective)p_308821_.getKey()).getFormattedDisplayName(), p_308821_.getIntValue()
               ),
               false
            )
         );
      }

      return object2intmap.size();
   }

   private static int clearDisplaySlot(CommandSourceStack p_138478_, DisplaySlot p_301105_) throws CommandSyntaxException {
      Scoreboard scoreboard = p_138478_.getServer().getScoreboard();
      if (scoreboard.getDisplayObjective(p_301105_) == null) {
         throw ERROR_DISPLAY_SLOT_ALREADY_EMPTY.create();
      }

      scoreboard.setDisplayObjective(p_301105_, null);
      p_138478_.sendSuccess(() -> Component.translatable("commands.scoreboard.objectives.display.cleared", p_301105_.getSerializedName()), true);
      return 0;
   }

   private static int setDisplaySlot(CommandSourceStack p_138481_, DisplaySlot p_300906_, Objective p_138483_) throws CommandSyntaxException {
      Scoreboard scoreboard = p_138481_.getServer().getScoreboard();
      if (scoreboard.getDisplayObjective(p_300906_) == p_138483_) {
         throw ERROR_DISPLAY_SLOT_ALREADY_SET.create();
      }

      scoreboard.setDisplayObjective(p_300906_, p_138483_);
      p_138481_.sendSuccess(
         () -> Component.translatable("commands.scoreboard.objectives.display.set", p_300906_.getSerializedName(), p_138483_.getDisplayName()), true
      );
      return 0;
   }

   private static int setDisplayName(CommandSourceStack p_138492_, Objective p_138493_, Component p_138494_) {
      if (!p_138493_.getDisplayName().equals(p_138494_)) {
         p_138493_.setDisplayName(p_138494_);
         p_138492_.sendSuccess(
            () -> Component.translatable("commands.scoreboard.objectives.modify.displayname", p_138493_.getName(), p_138493_.getFormattedDisplayName()), true
         );
      }

      return 0;
   }

   private static int setDisplayAutoUpdate(CommandSourceStack p_311402_, Objective p_310615_, boolean p_309996_) {
      if (p_310615_.displayAutoUpdate() != p_309996_) {
         p_310615_.setDisplayAutoUpdate(p_309996_);
         if (p_309996_) {
            p_311402_.sendSuccess(
               () -> Component.translatable(
                  "commands.scoreboard.objectives.modify.displayAutoUpdate.enable", p_310615_.getName(), p_310615_.getFormattedDisplayName()
               ),
               true
            );
         } else {
            p_311402_.sendSuccess(
               () -> Component.translatable(
                  "commands.scoreboard.objectives.modify.displayAutoUpdate.disable", p_310615_.getName(), p_310615_.getFormattedDisplayName()
               ),
               true
            );
         }
      }

      return 0;
   }

   private static int setObjectiveFormat(CommandSourceStack p_312449_, Objective p_313010_, @Nullable NumberFormat p_310903_) {
      p_313010_.setNumberFormat(p_310903_);
      if (p_310903_ != null) {
         p_312449_.sendSuccess(() -> Component.translatable("commands.scoreboard.objectives.modify.objectiveFormat.set", p_313010_.getName()), true);
      } else {
         p_312449_.sendSuccess(() -> Component.translatable("commands.scoreboard.objectives.modify.objectiveFormat.clear", p_313010_.getName()), true);
      }

      return 0;
   }

   private static int setRenderType(CommandSourceStack p_138488_, Objective p_138489_, ObjectiveCriteria.RenderType p_138490_) {
      if (p_138489_.getRenderType() != p_138490_) {
         p_138489_.setRenderType(p_138490_);
         p_138488_.sendSuccess(() -> Component.translatable("commands.scoreboard.objectives.modify.rendertype", p_138489_.getFormattedDisplayName()), true);
      }

      return 0;
   }

   private static int removeObjective(CommandSourceStack p_138485_, Objective p_138486_) {
      Scoreboard scoreboard = p_138485_.getServer().getScoreboard();
      scoreboard.removeObjective(p_138486_);
      p_138485_.sendSuccess(() -> Component.translatable("commands.scoreboard.objectives.remove.success", p_138486_.getFormattedDisplayName()), true);
      return scoreboard.getObjectives().size();
   }

   private static int addObjective(CommandSourceStack p_138503_, String p_138504_, ObjectiveCriteria p_138505_, Component p_138506_) throws CommandSyntaxException {
      Scoreboard scoreboard = p_138503_.getServer().getScoreboard();
      if (scoreboard.getObjective(p_138504_) != null) {
         throw ERROR_OBJECTIVE_ALREADY_EXISTS.create();
      }

      scoreboard.addObjective(p_138504_, p_138505_, p_138506_, p_138505_.getDefaultRenderType(), false, null);
      Objective objective = scoreboard.getObjective(p_138504_);
      p_138503_.sendSuccess(() -> Component.translatable("commands.scoreboard.objectives.add.success", objective.getFormattedDisplayName()), true);
      return scoreboard.getObjectives().size();
   }

   private static int listObjectives(CommandSourceStack p_138539_) {
      Collection<Objective> collection = p_138539_.getServer().getScoreboard().getObjectives();
      if (collection.isEmpty()) {
         p_138539_.sendSuccess(() -> Component.translatable("commands.scoreboard.objectives.list.empty"), false);
      } else {
         p_138539_.sendSuccess(
            () -> Component.translatable(
               "commands.scoreboard.objectives.list.success", collection.size(), ComponentUtils.formatList(collection, Objective::getFormattedDisplayName)
            ),
            false
         );
      }

      return collection.size();
   }

   @FunctionalInterface
   public interface NumberFormatCommandExecutor {
      int run(CommandContext<CommandSourceStack> var1, @Nullable NumberFormat var2) throws CommandSyntaxException;
   }
}
