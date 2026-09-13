package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.minecraft.world.level.gamerules.GameRules;

public class GameRuleCommand {
    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
        final LiteralArgumentBuilder<CommandSourceStack> base = Commands.literal("gamerule").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        new GameRules(context.enabledFeatures()).visitGameRuleTypes(new GameRuleTypeVisitor() {
            @Override
            public <T> void visit(final GameRule<T> gameRule) {
                LiteralArgumentBuilder<CommandSourceStack> unqualified = Commands.literal(gameRule.id());
                LiteralArgumentBuilder<CommandSourceStack> qualified = Commands.literal(gameRule.getIdentifier().toString());
                base.then(GameRuleCommand.buildRuleArguments(gameRule, unqualified)).then(GameRuleCommand.buildRuleArguments(gameRule, qualified));
            }
        });
        dispatcher.register(base);
    }

    private static <T> LiteralArgumentBuilder<CommandSourceStack> buildRuleArguments(
        final GameRule<T> gameRule, final LiteralArgumentBuilder<CommandSourceStack> ruleLiteral
    ) {
        return ruleLiteral.executes(c -> queryRule(c.getSource(), gameRule))
            .then(Commands.argument("value", gameRule.argument()).executes(c -> setRule(c, gameRule)));
    }

    private static <T> int setRule(final CommandContext<CommandSourceStack> context, final GameRule<T> gameRule) {
        CommandSourceStack source = context.getSource();
        T value = context.getArgument("value", gameRule.valueClass());
        source.getLevel().getGameRules().set(gameRule, value, context.getSource().getServer());
        source.sendSuccess(() -> Component.translatable("commands.gamerule.set", gameRule.id(), gameRule.serialize(value)), true);
        return gameRule.getCommandResult(value);
    }

    private static <T> int queryRule(final CommandSourceStack source, final GameRule<T> gameRule) {
        T value = source.getLevel().getGameRules().get(gameRule);
        source.sendSuccess(() -> Component.translatable("commands.gamerule.query", gameRule.id(), gameRule.serialize(value)), false);
        return gameRule.getCommandResult(value);
    }
}