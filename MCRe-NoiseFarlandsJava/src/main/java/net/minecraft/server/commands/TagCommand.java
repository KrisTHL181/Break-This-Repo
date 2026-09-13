package net.minecraft.server.commands;

import com.google.common.collect.Sets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.world.entity.Entity;

public class TagCommand {
    private static final SimpleCommandExceptionType ERROR_ADD_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.tag.add.failed"));
    private static final SimpleCommandExceptionType ERROR_REMOVE_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.tag.remove.failed"));

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("tag")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(
                    Commands.argument("targets", EntityArgument.entities())
                        .then(
                            Commands.literal("add")
                                .then(
                                    Commands.argument("name", StringArgumentType.word())
                                        .executes(c -> addTag(c.getSource(), EntityArgument.getEntities(c, "targets"), StringArgumentType.getString(c, "name")))
                                )
                        )
                        .then(
                            Commands.literal("remove")
                                .then(
                                    Commands.argument("name", StringArgumentType.word())
                                        .suggests((c, p) -> SharedSuggestionProvider.suggest(getTags(EntityArgument.getEntities(c, "targets")), p))
                                        .executes(
                                            c -> removeTag(c.getSource(), EntityArgument.getEntities(c, "targets"), StringArgumentType.getString(c, "name"))
                                        )
                                )
                        )
                        .then(Commands.literal("list").executes(c -> listTags(c.getSource(), EntityArgument.getEntities(c, "targets"))))
                )
        );
    }

    private static Collection<String> getTags(final Collection<? extends Entity> entities) {
        Set<String> result = Sets.newHashSet();

        for (Entity entity : entities) {
            result.addAll(entity.entityTags());
        }

        return result;
    }

    private static int addTag(final CommandSourceStack source, final Collection<? extends Entity> targets, final String name) throws CommandSyntaxException {
        int count = 0;

        for (Entity entity : targets) {
            if (entity.addTag(name)) {
                count++;
            }
        }

        if (count == 0) {
            throw ERROR_ADD_FAILED.create();
        }

        if (targets.size() == 1) {
            source.sendSuccess(() -> Component.translatable("commands.tag.add.success.single", name, targets.iterator().next().getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.tag.add.success.multiple", name, targets.size()), true);
        }

        return count;
    }

    private static int removeTag(final CommandSourceStack source, final Collection<? extends Entity> targets, final String name) throws CommandSyntaxException {
        int count = 0;

        for (Entity entity : targets) {
            if (entity.removeTag(name)) {
                count++;
            }
        }

        if (count == 0) {
            throw ERROR_REMOVE_FAILED.create();
        }

        if (targets.size() == 1) {
            source.sendSuccess(() -> Component.translatable("commands.tag.remove.success.single", name, targets.iterator().next().getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.tag.remove.success.multiple", name, targets.size()), true);
        }

        return count;
    }

    private static int listTags(final CommandSourceStack source, final Collection<? extends Entity> targets) {
        Set<String> tags = Sets.newHashSet();

        for (Entity entity : targets) {
            tags.addAll(entity.entityTags());
        }

        if (targets.size() == 1) {
            Entity entity = targets.iterator().next();
            if (tags.isEmpty()) {
                source.sendSuccess(() -> Component.translatable("commands.tag.list.single.empty", entity.getDisplayName()), false);
            } else {
                source.sendSuccess(
                    () -> Component.translatable("commands.tag.list.single.success", entity.getDisplayName(), tags.size(), ComponentUtils.formatList(tags)),
                    false
                );
            }
        } else if (tags.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.tag.list.multiple.empty", targets.size()), false);
        } else {
            source.sendSuccess(
                () -> Component.translatable("commands.tag.list.multiple.success", targets.size(), tags.size(), ComponentUtils.formatList(tags)), false
            );
        }

        return tags.size();
    }
}