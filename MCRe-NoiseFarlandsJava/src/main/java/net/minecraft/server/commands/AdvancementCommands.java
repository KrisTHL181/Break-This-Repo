package net.minecraft.server.commands;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class AdvancementCommands {
    private static final DynamicCommandExceptionType ERROR_NO_ACTION_PERFORMED = new DynamicCommandExceptionType(msg -> (Component)msg);
    private static final Dynamic2CommandExceptionType ERROR_CRITERION_NOT_FOUND = new Dynamic2CommandExceptionType(
        (name, criterion) -> Component.translatableEscape("commands.advancement.criterionNotFound", name, criterion)
    );

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("advancement")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(
                    Commands.literal("grant")
                        .then(
                            Commands.argument("targets", EntityArgument.players())
                                .then(
                                    Commands.literal("only")
                                        .then(
                                            Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                                .executes(
                                                    c -> perform(
                                                        c.getSource(),
                                                        EntityArgument.getPlayers(c, "targets"),
                                                        AdvancementCommands.Action.GRANT,
                                                        getAdvancements(c, ResourceKeyArgument.getAdvancement(c, "advancement"), AdvancementCommands.Mode.ONLY)
                                                    )
                                                )
                                                .then(
                                                    Commands.argument("criterion", StringArgumentType.greedyString())
                                                        .suggests(
                                                            (c, p) -> SharedSuggestionProvider.suggest(
                                                                ResourceKeyArgument.getAdvancement(c, "advancement").value().criteria().keySet(), p
                                                            )
                                                        )
                                                        .executes(
                                                            c -> performCriterion(
                                                                c.getSource(),
                                                                EntityArgument.getPlayers(c, "targets"),
                                                                AdvancementCommands.Action.GRANT,
                                                                ResourceKeyArgument.getAdvancement(c, "advancement"),
                                                                StringArgumentType.getString(c, "criterion")
                                                            )
                                                        )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("from")
                                        .then(
                                            Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                                .executes(
                                                    c -> perform(
                                                        c.getSource(),
                                                        EntityArgument.getPlayers(c, "targets"),
                                                        AdvancementCommands.Action.GRANT,
                                                        getAdvancements(c, ResourceKeyArgument.getAdvancement(c, "advancement"), AdvancementCommands.Mode.FROM)
                                                    )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("until")
                                        .then(
                                            Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                                .executes(
                                                    c -> perform(
                                                        c.getSource(),
                                                        EntityArgument.getPlayers(c, "targets"),
                                                        AdvancementCommands.Action.GRANT,
                                                        getAdvancements(c, ResourceKeyArgument.getAdvancement(c, "advancement"), AdvancementCommands.Mode.UNTIL)
                                                    )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("through")
                                        .then(
                                            Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                                .executes(
                                                    c -> perform(
                                                        c.getSource(),
                                                        EntityArgument.getPlayers(c, "targets"),
                                                        AdvancementCommands.Action.GRANT,
                                                        getAdvancements(
                                                            c, ResourceKeyArgument.getAdvancement(c, "advancement"), AdvancementCommands.Mode.THROUGH
                                                        )
                                                    )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("everything")
                                        .executes(
                                            c -> perform(
                                                c.getSource(),
                                                EntityArgument.getPlayers(c, "targets"),
                                                AdvancementCommands.Action.GRANT,
                                                c.getSource().getServer().getAdvancements().getAllAdvancements(),
                                                false
                                            )
                                        )
                                )
                        )
                )
                .then(
                    Commands.literal("revoke")
                        .then(
                            Commands.argument("targets", EntityArgument.players())
                                .then(
                                    Commands.literal("only")
                                        .then(
                                            Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                                .executes(
                                                    c -> perform(
                                                        c.getSource(),
                                                        EntityArgument.getPlayers(c, "targets"),
                                                        AdvancementCommands.Action.REVOKE,
                                                        getAdvancements(c, ResourceKeyArgument.getAdvancement(c, "advancement"), AdvancementCommands.Mode.ONLY)
                                                    )
                                                )
                                                .then(
                                                    Commands.argument("criterion", StringArgumentType.greedyString())
                                                        .suggests(
                                                            (c, p) -> SharedSuggestionProvider.suggest(
                                                                ResourceKeyArgument.getAdvancement(c, "advancement").value().criteria().keySet(), p
                                                            )
                                                        )
                                                        .executes(
                                                            c -> performCriterion(
                                                                c.getSource(),
                                                                EntityArgument.getPlayers(c, "targets"),
                                                                AdvancementCommands.Action.REVOKE,
                                                                ResourceKeyArgument.getAdvancement(c, "advancement"),
                                                                StringArgumentType.getString(c, "criterion")
                                                            )
                                                        )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("from")
                                        .then(
                                            Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                                .executes(
                                                    c -> perform(
                                                        c.getSource(),
                                                        EntityArgument.getPlayers(c, "targets"),
                                                        AdvancementCommands.Action.REVOKE,
                                                        getAdvancements(c, ResourceKeyArgument.getAdvancement(c, "advancement"), AdvancementCommands.Mode.FROM)
                                                    )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("until")
                                        .then(
                                            Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                                .executes(
                                                    c -> perform(
                                                        c.getSource(),
                                                        EntityArgument.getPlayers(c, "targets"),
                                                        AdvancementCommands.Action.REVOKE,
                                                        getAdvancements(c, ResourceKeyArgument.getAdvancement(c, "advancement"), AdvancementCommands.Mode.UNTIL)
                                                    )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("through")
                                        .then(
                                            Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                                .executes(
                                                    c -> perform(
                                                        c.getSource(),
                                                        EntityArgument.getPlayers(c, "targets"),
                                                        AdvancementCommands.Action.REVOKE,
                                                        getAdvancements(
                                                            c, ResourceKeyArgument.getAdvancement(c, "advancement"), AdvancementCommands.Mode.THROUGH
                                                        )
                                                    )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("everything")
                                        .executes(
                                            c -> perform(
                                                c.getSource(),
                                                EntityArgument.getPlayers(c, "targets"),
                                                AdvancementCommands.Action.REVOKE,
                                                c.getSource().getServer().getAdvancements().getAllAdvancements()
                                            )
                                        )
                                )
                        )
                )
        );
    }

    private static int perform(
        final CommandSourceStack source,
        final Collection<ServerPlayer> players,
        final AdvancementCommands.Action action,
        final Collection<AdvancementHolder> advancements
    ) throws CommandSyntaxException {
        return perform(source, players, action, advancements, true);
    }

    private static int perform(
        final CommandSourceStack source,
        final Collection<ServerPlayer> players,
        final AdvancementCommands.Action action,
        final Collection<AdvancementHolder> advancements,
        final boolean showAdvancements
    ) throws CommandSyntaxException {
        int advancementCount = 0;
        int playerCount = 0;

        for (ServerPlayer player : players) {
            int changedAdvancements = action.perform(player, advancements, showAdvancements);
            if (changedAdvancements > 0) {
                playerCount++;
            }

            advancementCount += changedAdvancements;
        }

        if (advancementCount == 0) {
            if (advancements.size() == 1) {
                Component advancementName = Advancement.name(Iterables.getOnlyElement(advancements));
                if (players.size() == 1) {
                    throw ERROR_NO_ACTION_PERFORMED.create(
                        Component.translatable(action.getKey() + ".one.to.one.failure", advancementName, Iterables.getOnlyElement(players).getDisplayName())
                    );
                } else {
                    throw ERROR_NO_ACTION_PERFORMED.create(Component.translatable(action.getKey() + ".one.to.many.failure", advancementName, players.size()));
                }
            } else if (players.size() == 1) {
                throw ERROR_NO_ACTION_PERFORMED.create(
                    Component.translatable(action.getKey() + ".many.to.one.failure", advancements.size(), Iterables.getOnlyElement(players).getDisplayName())
                );
            } else {
                throw ERROR_NO_ACTION_PERFORMED.create(Component.translatable(action.getKey() + ".many.to.many.failure", advancements.size(), players.size()));
            }
        } else {
            if (advancements.size() == 1) {
                Component advancementName = Advancement.name(Iterables.getOnlyElement(advancements));
                if (players.size() == 1) {
                    source.sendSuccess(
                        () -> Component.translatable(
                            action.getKey() + ".one.to.one.success", advancementName, Iterables.getOnlyElement(players).getDisplayName()
                        ),
                        true
                    );
                } else {
                    int finalPlayerCount = playerCount;
                    source.sendSuccess(() -> Component.translatable(action.getKey() + ".one.to.many.success", advancementName, finalPlayerCount), true);
                }
            } else {
                int finalAdvancementCount = advancementCount;
                if (players.size() == 1) {
                    source.sendSuccess(
                        () -> Component.translatable(
                            action.getKey() + ".many.to.one.success", finalAdvancementCount, Iterables.getOnlyElement(players).getDisplayName()
                        ),
                        true
                    );
                } else {
                    int finalPlayerCount = playerCount;
                    source.sendSuccess(() -> Component.translatable(action.getKey() + ".many.to.many.success", finalAdvancementCount, finalPlayerCount), true);
                }
            }

            return advancementCount;
        }
    }

    private static int performCriterion(
        final CommandSourceStack source,
        final Collection<ServerPlayer> players,
        final AdvancementCommands.Action action,
        final AdvancementHolder holder,
        final String criterion
    ) throws CommandSyntaxException {
        int playerCount = 0;
        Advancement advancement = holder.value();
        if (!advancement.criteria().containsKey(criterion)) {
            throw ERROR_CRITERION_NOT_FOUND.create(Advancement.name(holder), criterion);
        }

        for (ServerPlayer player : players) {
            if (action.performCriterion(player, holder, criterion)) {
                playerCount++;
            }
        }

        if (playerCount == 0) {
            if (players.size() == 1) {
                throw ERROR_NO_ACTION_PERFORMED.create(
                    Component.translatable(
                        action.getKey() + ".criterion.to.one.failure", criterion, Advancement.name(holder), Iterables.getOnlyElement(players).getDisplayName()
                    )
                );
            } else {
                throw ERROR_NO_ACTION_PERFORMED.create(
                    Component.translatable(action.getKey() + ".criterion.to.many.failure", criterion, Advancement.name(holder), players.size())
                );
            }
        } else {
            if (players.size() == 1) {
                source.sendSuccess(
                    () -> Component.translatable(
                        action.getKey() + ".criterion.to.one.success", criterion, Advancement.name(holder), Iterables.getOnlyElement(players).getDisplayName()
                    ),
                    true
                );
            } else {
                int finalPlayerCount = playerCount;
                source.sendSuccess(
                    () -> Component.translatable(action.getKey() + ".criterion.to.many.success", criterion, Advancement.name(holder), finalPlayerCount), true
                );
            }

            return playerCount;
        }
    }

    private static List<AdvancementHolder> getAdvancements(
        final CommandContext<CommandSourceStack> context, final AdvancementHolder target, final AdvancementCommands.Mode mode
    ) {
        AdvancementTree advancementTree = context.getSource().getServer().getAdvancements().tree();
        AdvancementNode targetNode = advancementTree.get(target);
        if (targetNode == null) {
            return List.of(target);
        }

        List<AdvancementHolder> advancements = new ArrayList<>();
        if (mode.parents) {
            for (AdvancementNode parent = targetNode.parent(); parent != null; parent = parent.parent()) {
                advancements.add(parent.holder());
            }
        }

        advancements.add(target);
        if (mode.children) {
            addChildren(targetNode, advancements);
        }

        return advancements;
    }

    private static void addChildren(final AdvancementNode parent, final List<AdvancementHolder> output) {
        for (AdvancementNode child : parent.children()) {
            output.add(child.holder());
            addChildren(child, output);
        }
    }

    private enum Action {
        GRANT("grant") {
            @Override
            protected boolean perform(final ServerPlayer player, final AdvancementHolder advancement) {
                AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
                if (progress.isDone()) {
                    return false;
                }

                for (String criterion : progress.getRemainingCriteria()) {
                    player.getAdvancements().award(advancement, criterion);
                }

                return true;
            }

            @Override
            protected boolean performCriterion(final ServerPlayer player, final AdvancementHolder advancement, final String criterion) {
                return player.getAdvancements().award(advancement, criterion);
            }
        },
        REVOKE("revoke") {
            @Override
            protected boolean perform(final ServerPlayer player, final AdvancementHolder advancement) {
                AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
                if (!progress.hasProgress()) {
                    return false;
                }

                for (String criterion : progress.getCompletedCriteria()) {
                    player.getAdvancements().revoke(advancement, criterion);
                }

                return true;
            }

            @Override
            protected boolean performCriterion(final ServerPlayer player, final AdvancementHolder advancement, final String criterion) {
                return player.getAdvancements().revoke(advancement, criterion);
            }
        };

        private final String key;

        Action(final String key) {
            this.key = "commands.advancement." + key;
        }

        public int perform(final ServerPlayer player, final Iterable<AdvancementHolder> advancements, final boolean showAdvancements) {
            int count = 0;
            if (!showAdvancements) {
                player.getAdvancements().flushDirty(player, true);
            }

            for (AdvancementHolder advancement : advancements) {
                if (this.perform(player, advancement)) {
                    count++;
                }
            }

            if (!showAdvancements) {
                player.getAdvancements().flushDirty(player, false);
            }

            return count;
        }

        protected abstract boolean perform(ServerPlayer player, AdvancementHolder advancement);

        protected abstract boolean performCriterion(ServerPlayer player, AdvancementHolder advancement, String criterion);

        protected String getKey() {
            return this.key;
        }
    }

    private enum Mode {
        ONLY(false, false),
        THROUGH(true, true),
        FROM(false, true),
        UNTIL(true, false),
        EVERYTHING(true, true);

        private final boolean parents;
        private final boolean children;

        Mode(final boolean parents, final boolean children) {
            this.parents = parents;
            this.children = children;
        }
    }
}