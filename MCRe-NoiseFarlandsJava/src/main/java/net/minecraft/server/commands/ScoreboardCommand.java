package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
        dispatcher.register(
            Commands.literal("scoreboard")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(
                    Commands.literal("objectives")
                        .then(Commands.literal("list").executes(c -> listObjectives(c.getSource())))
                        .then(
                            Commands.literal("add")
                                .then(
                                    Commands.argument("objective", StringArgumentType.word())
                                        .then(
                                            Commands.argument("criteria", ObjectiveCriteriaArgument.criteria())
                                                .executes(
                                                    c -> addObjective(
                                                        c.getSource(),
                                                        StringArgumentType.getString(c, "objective"),
                                                        ObjectiveCriteriaArgument.getCriteria(c, "criteria"),
                                                        Component.literal(StringArgumentType.getString(c, "objective"))
                                                    )
                                                )
                                                .then(
                                                    Commands.argument("displayName", ComponentArgument.textComponent(context))
                                                        .executes(
                                                            c -> addObjective(
                                                                c.getSource(),
                                                                StringArgumentType.getString(c, "objective"),
                                                                ObjectiveCriteriaArgument.getCriteria(c, "criteria"),
                                                                ComponentArgument.getResolvedComponent(c, "displayName")
                                                            )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("modify")
                                .then(
                                    Commands.argument("objective", ObjectiveArgument.objective())
                                        .then(
                                            Commands.literal("displayname")
                                                .then(
                                                    Commands.argument("displayName", ComponentArgument.textComponent(context))
                                                        .executes(
                                                            c -> setDisplayName(
                                                                c.getSource(),
                                                                ObjectiveArgument.getObjective(c, "objective"),
                                                                ComponentArgument.getResolvedComponent(c, "displayName")
                                                            )
                                                        )
                                                )
                                        )
                                        .then(createRenderTypeModify())
                                        .then(
                                            Commands.literal("displayautoupdate")
                                                .then(
                                                    Commands.argument("value", BoolArgumentType.bool())
                                                        .executes(
                                                            c -> setDisplayAutoUpdate(
                                                                c.getSource(),
                                                                ObjectiveArgument.getObjective(c, "objective"),
                                                                BoolArgumentType.getBool(c, "value")
                                                            )
                                                        )
                                                )
                                        )
                                        .then(
                                            addNumberFormats(
                                                context,
                                                Commands.literal("numberformat"),
                                                (c, numberFormat) -> setObjectiveFormat(
                                                    c.getSource(), ObjectiveArgument.getObjective(c, "objective"), numberFormat
                                                )
                                            )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("remove")
                                .then(
                                    Commands.argument("objective", ObjectiveArgument.objective())
                                        .executes(c -> removeObjective(c.getSource(), ObjectiveArgument.getObjective(c, "objective")))
                                )
                        )
                        .then(
                            Commands.literal("setdisplay")
                                .then(
                                    Commands.argument("slot", ScoreboardSlotArgument.displaySlot())
                                        .executes(c -> clearDisplaySlot(c.getSource(), ScoreboardSlotArgument.getDisplaySlot(c, "slot")))
                                        .then(
                                            Commands.argument("objective", ObjectiveArgument.objective())
                                                .executes(
                                                    c -> setDisplaySlot(
                                                        c.getSource(),
                                                        ScoreboardSlotArgument.getDisplaySlot(c, "slot"),
                                                        ObjectiveArgument.getObjective(c, "objective")
                                                    )
                                                )
                                        )
                                )
                        )
                )
                .then(
                    Commands.literal("players")
                        .then(
                            Commands.literal("list")
                                .executes(c -> listTrackedPlayers(c.getSource()))
                                .then(
                                    Commands.argument("target", ScoreHolderArgument.scoreHolder())
                                        .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                        .executes(c -> listTrackedPlayerScores(c.getSource(), ScoreHolderArgument.getName(c, "target")))
                                )
                        )
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
                                                                c.getSource(),
                                                                ScoreHolderArgument.getNamesWithDefaultWildcard(c, "targets"),
                                                                ObjectiveArgument.getWritableObjective(c, "objective"),
                                                                IntegerArgumentType.getInteger(c, "score")
                                                            )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("get")
                                .then(
                                    Commands.argument("target", ScoreHolderArgument.scoreHolder())
                                        .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                        .then(
                                            Commands.argument("objective", ObjectiveArgument.objective())
                                                .executes(
                                                    c -> getScore(
                                                        c.getSource(), ScoreHolderArgument.getName(c, "target"), ObjectiveArgument.getObjective(c, "objective")
                                                    )
                                                )
                                        )
                                )
                        )
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
                                                                c.getSource(),
                                                                ScoreHolderArgument.getNamesWithDefaultWildcard(c, "targets"),
                                                                ObjectiveArgument.getWritableObjective(c, "objective"),
                                                                IntegerArgumentType.getInteger(c, "score")
                                                            )
                                                        )
                                                )
                                        )
                                )
                        )
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
                                                                c.getSource(),
                                                                ScoreHolderArgument.getNamesWithDefaultWildcard(c, "targets"),
                                                                ObjectiveArgument.getWritableObjective(c, "objective"),
                                                                IntegerArgumentType.getInteger(c, "score")
                                                            )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("reset")
                                .then(
                                    Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                                        .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                        .executes(c -> resetScores(c.getSource(), ScoreHolderArgument.getNamesWithDefaultWildcard(c, "targets")))
                                        .then(
                                            Commands.argument("objective", ObjectiveArgument.objective())
                                                .executes(
                                                    c -> resetScore(
                                                        c.getSource(),
                                                        ScoreHolderArgument.getNamesWithDefaultWildcard(c, "targets"),
                                                        ObjectiveArgument.getObjective(c, "objective")
                                                    )
                                                )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("enable")
                                .then(
                                    Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                                        .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                        .then(
                                            Commands.argument("objective", ObjectiveArgument.objective())
                                                .suggests(
                                                    (c, p) -> suggestTriggers(c.getSource(), ScoreHolderArgument.getNamesWithDefaultWildcard(c, "targets"), p)
                                                )
                                                .executes(
                                                    c -> enableTrigger(
                                                        c.getSource(),
                                                        ScoreHolderArgument.getNamesWithDefaultWildcard(c, "targets"),
                                                        ObjectiveArgument.getObjective(c, "objective")
                                                    )
                                                )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("display")
                                .then(
                                    Commands.literal("name")
                                        .then(
                                            Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                                                .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                .then(
                                                    Commands.argument("objective", ObjectiveArgument.objective())
                                                        .then(
                                                            Commands.argument("name", ComponentArgument.textComponent(context))
                                                                .executes(
                                                                    c -> setScoreDisplay(
                                                                        c.getSource(),
                                                                        ScoreHolderArgument.getNamesWithDefaultWildcard(c, "targets"),
                                                                        ObjectiveArgument.getObjective(c, "objective"),
                                                                        ComponentArgument.getResolvedComponent(c, "name")
                                                                    )
                                                                )
                                                        )
                                                        .executes(
                                                            c -> setScoreDisplay(
                                                                c.getSource(),
                                                                ScoreHolderArgument.getNamesWithDefaultWildcard(c, "targets"),
                                                                ObjectiveArgument.getObjective(c, "objective"),
                                                                null
                                                            )
                                                        )
                                                )
                                        )
                                )
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
                                                            c.getSource(),
                                                            ScoreHolderArgument.getNamesWithDefaultWildcard(c, "targets"),
                                                            ObjectiveArgument.getObjective(c, "objective"),
                                                            format
                                                        )
                                                    )
                                                )
                                        )
                                )
                        )
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
                                                                                c.getSource(),
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
                Commands.literal(renderType.getId()).executes(c -> setRenderType(c.getSource(), ObjectiveArgument.getObjective(c, "objective"), renderType))
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

    private static Component getFirstTargetName(final Collection<ScoreHolder> names) {
        return names.iterator().next().getFeedbackDisplayName();
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
        int result = 0;

        for (ScoreHolder target : targets) {
            ScoreAccess score = scoreboard.getOrCreatePlayerScore(target, targetObjective);

            for (ScoreHolder from : sources) {
                ScoreAccess sourceScore = scoreboard.getOrCreatePlayerScore(from, sourceObjective);
                operation.apply(score, sourceScore);
            }

            result += score.get();
        }

        if (targets.size() == 1) {
            int finalResult = result;
            source.sendSuccess(
                () -> Component.translatable(
                    "commands.scoreboard.players.operation.success.single", targetObjective.getFormattedDisplayName(), getFirstTargetName(targets), finalResult
                ),
                true
            );
        } else {
            source.sendSuccess(
                () -> Component.translatable(
                    "commands.scoreboard.players.operation.success.multiple", targetObjective.getFormattedDisplayName(), targets.size()
                ),
                true
            );
        }

        return result;
    }

    private static int enableTrigger(final CommandSourceStack source, final Collection<ScoreHolder> names, final Objective objective) throws CommandSyntaxException {
        if (objective.getCriteria() != ObjectiveCriteria.TRIGGER) {
            throw ERROR_NOT_TRIGGER.create();
        }

        Scoreboard scoreboard = source.getServer().getScoreboard();
        int count = 0;

        for (ScoreHolder name : names) {
            ScoreAccess score = scoreboard.getOrCreatePlayerScore(name, objective);
            if (score.locked()) {
                score.unlock();
                count++;
            }
        }

        if (count == 0) {
            throw ERROR_TRIGGER_ALREADY_ENABLED.create();
        }

        if (names.size() == 1) {
            source.sendSuccess(
                () -> Component.translatable(
                    "commands.scoreboard.players.enable.success.single", objective.getFormattedDisplayName(), getFirstTargetName(names)
                ),
                true
            );
        } else {
            source.sendSuccess(
                () -> Component.translatable("commands.scoreboard.players.enable.success.multiple", objective.getFormattedDisplayName(), names.size()), true
            );
        }

        return count;
    }

    private static int resetScores(final CommandSourceStack source, final Collection<ScoreHolder> names) {
        Scoreboard scoreboard = source.getServer().getScoreboard();

        for (ScoreHolder name : names) {
            scoreboard.resetAllPlayerScores(name);
        }

        if (names.size() == 1) {
            source.sendSuccess(() -> Component.translatable("commands.scoreboard.players.reset.all.single", getFirstTargetName(names)), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.scoreboard.players.reset.all.multiple", names.size()), true);
        }

        return names.size();
    }

    private static int resetScore(final CommandSourceStack source, final Collection<ScoreHolder> names, final Objective objective) {
        Scoreboard scoreboard = source.getServer().getScoreboard();

        for (ScoreHolder name : names) {
            scoreboard.resetSinglePlayerScore(name, objective);
        }

        if (names.size() == 1) {
            source.sendSuccess(
                () -> Component.translatable(
                    "commands.scoreboard.players.reset.specific.single", objective.getFormattedDisplayName(), getFirstTargetName(names)
                ),
                true
            );
        } else {
            source.sendSuccess(
                () -> Component.translatable("commands.scoreboard.players.reset.specific.multiple", objective.getFormattedDisplayName(), names.size()), true
            );
        }

        return names.size();
    }

    private static int setScore(final CommandSourceStack source, final Collection<ScoreHolder> names, final Objective objective, final int value) {
        Scoreboard scoreboard = source.getServer().getScoreboard();

        for (ScoreHolder name : names) {
            scoreboard.getOrCreatePlayerScore(name, objective).set(value);
        }

        if (names.size() == 1) {
            source.sendSuccess(
                () -> Component.translatable(
                    "commands.scoreboard.players.set.success.single", objective.getFormattedDisplayName(), getFirstTargetName(names), value
                ),
                true
            );
        } else {
            source.sendSuccess(
                () -> Component.translatable("commands.scoreboard.players.set.success.multiple", objective.getFormattedDisplayName(), names.size(), value),
                true
            );
        }

        return value * names.size();
    }

    private static int setScoreDisplay(
        final CommandSourceStack source, final Collection<ScoreHolder> names, final Objective objective, final @Nullable Component display
    ) {
        Scoreboard scoreboard = source.getServer().getScoreboard();

        for (ScoreHolder name : names) {
            scoreboard.getOrCreatePlayerScore(name, objective).display(display);
        }

        if (display == null) {
            if (names.size() == 1) {
                source.sendSuccess(
                    () -> Component.translatable(
                        "commands.scoreboard.players.display.name.clear.success.single", getFirstTargetName(names), objective.getFormattedDisplayName()
                    ),
                    true
                );
            } else {
                source.sendSuccess(
                    () -> Component.translatable(
                        "commands.scoreboard.players.display.name.clear.success.multiple", names.size(), objective.getFormattedDisplayName()
                    ),
                    true
                );
            }
        } else if (names.size() == 1) {
            source.sendSuccess(
                () -> Component.translatable(
                    "commands.scoreboard.players.display.name.set.success.single", display, getFirstTargetName(names), objective.getFormattedDisplayName()
                ),
                true
            );
        } else {
            source.sendSuccess(
                () -> Component.translatable(
                    "commands.scoreboard.players.display.name.set.success.multiple", display, names.size(), objective.getFormattedDisplayName()
                ),
                true
            );
        }

        return names.size();
    }

    private static int setScoreNumberFormat(
        final CommandSourceStack source, final Collection<ScoreHolder> names, final Objective objective, final @Nullable NumberFormat numberFormat
    ) {
        Scoreboard scoreboard = source.getServer().getScoreboard();

        for (ScoreHolder name : names) {
            scoreboard.getOrCreatePlayerScore(name, objective).numberFormatOverride(numberFormat);
        }

        if (numberFormat == null) {
            if (names.size() == 1) {
                source.sendSuccess(
                    () -> Component.translatable(
                        "commands.scoreboard.players.display.numberFormat.clear.success.single", getFirstTargetName(names), objective.getFormattedDisplayName()
                    ),
                    true
                );
            } else {
                source.sendSuccess(
                    () -> Component.translatable(
                        "commands.scoreboard.players.display.numberFormat.clear.success.multiple", names.size(), objective.getFormattedDisplayName()
                    ),
                    true
                );
            }
        } else if (names.size() == 1) {
            source.sendSuccess(
                () -> Component.translatable(
                    "commands.scoreboard.players.display.numberFormat.set.success.single", getFirstTargetName(names), objective.getFormattedDisplayName()
                ),
                true
            );
        } else {
            source.sendSuccess(
                () -> Component.translatable(
                    "commands.scoreboard.players.display.numberFormat.set.success.multiple", names.size(), objective.getFormattedDisplayName()
                ),
                true
            );
        }

        return names.size();
    }

    private static int addScore(final CommandSourceStack source, final Collection<ScoreHolder> names, final Objective objective, final int value) {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        int result = 0;

        for (ScoreHolder name : names) {
            ScoreAccess score = scoreboard.getOrCreatePlayerScore(name, objective);
            score.set(score.get() + value);
            result += score.get();
        }

        if (names.size() == 1) {
            int finalResult = result;
            source.sendSuccess(
                () -> Component.translatable(
                    "commands.scoreboard.players.add.success.single", value, objective.getFormattedDisplayName(), getFirstTargetName(names), finalResult
                ),
                true
            );
        } else {
            source.sendSuccess(
                () -> Component.translatable("commands.scoreboard.players.add.success.multiple", value, objective.getFormattedDisplayName(), names.size()),
                true
            );
        }

        return result;
    }

    private static int removeScore(final CommandSourceStack source, final Collection<ScoreHolder> names, final Objective objective, final int value) {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        int result = 0;

        for (ScoreHolder name : names) {
            ScoreAccess score = scoreboard.getOrCreatePlayerScore(name, objective);
            score.set(score.get() - value);
            result += score.get();
        }

        if (names.size() == 1) {
            int finalResult = result;
            source.sendSuccess(
                () -> Component.translatable(
                    "commands.scoreboard.players.remove.success.single", value, objective.getFormattedDisplayName(), getFirstTargetName(names), finalResult
                ),
                true
            );
        } else {
            source.sendSuccess(
                () -> Component.translatable("commands.scoreboard.players.remove.success.multiple", value, objective.getFormattedDisplayName(), names.size()),
                true
            );
        }

        return result;
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
        source.sendSuccess(
            () -> Component.translatable("commands.scoreboard.objectives.display.set", slot.getSerializedName(), objective.getDisplayName()), true
        );
        return 0;
    }

    private static int setDisplayName(final CommandSourceStack source, final Objective objective, final Component displayName) {
        if (!objective.getDisplayName().equals(displayName)) {
            objective.setDisplayName(displayName);
            source.sendSuccess(
                () -> Component.translatable("commands.scoreboard.objectives.modify.displayname", objective.getName(), objective.getFormattedDisplayName()),
                true
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