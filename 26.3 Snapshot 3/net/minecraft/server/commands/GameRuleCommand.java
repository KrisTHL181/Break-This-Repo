package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.minecraft.world.level.gamerules.GameRules;

public class GameRuleCommand {
   private static final Dynamic2CommandExceptionType ERROR_GAME_RULE_NOT_SET = new Dynamic2CommandExceptionType(
      (gameRule, value) -> Component.translatableEscape("commands.gamerule.not_set", gameRule, value)
   );

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
      final LiteralArgumentBuilder<CommandSourceStack> base = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("gamerule")
         .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
      new GameRules(context.enabledFeatures())
         .visitGameRuleTypes(
            new GameRuleTypeVisitor() {
               @Override
               public <T> void visit(final GameRule<T> gameRule) {
                  LiteralArgumentBuilder<CommandSourceStack> unqualified = Commands.literal(gameRule.id());
                  LiteralArgumentBuilder<CommandSourceStack> qualified = Commands.literal(gameRule.getIdentifier().toString());
                  ((LiteralArgumentBuilder)base.then(GameRuleCommand.buildRuleArguments(gameRule, unqualified)))
                     .then(GameRuleCommand.buildRuleArguments(gameRule, qualified));
               }
            }
         );
      dispatcher.register(base);
   }

   private static <T> LiteralArgumentBuilder<CommandSourceStack> buildRuleArguments(
      final GameRule<T> gameRule, final LiteralArgumentBuilder<CommandSourceStack> ruleLiteral
   ) {
      return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)ruleLiteral.executes(
            c -> queryRule((CommandSourceStack)c.getSource(), gameRule)
         ))
         .then(Commands.argument("value", gameRule.argument()).executes(c -> setRule(c, gameRule)));
   }

   private static <T> int setRule(final CommandContext<CommandSourceStack> context, final GameRule<T> gameRule) throws CommandSyntaxException {
      CommandSourceStack source = (CommandSourceStack)context.getSource();
      T value = (T)context.getArgument("value", gameRule.valueClass());
      GameRules gameRules = source.getLevel().getGameRules();
      String seralizedValue = gameRule.serialize(value);
      if (gameRules.get(gameRule).equals(value)) {
         throw ERROR_GAME_RULE_NOT_SET.create(gameRule, seralizedValue);
      }

      gameRules.set(gameRule, value, ((CommandSourceStack)context.getSource()).getServer());
      source.sendSuccess(() -> Component.translatable("commands.gamerule.set", gameRule.id(), seralizedValue), true);
      return gameRule.getCommandResult(value);
   }

   private static <T> int queryRule(final CommandSourceStack source, final GameRule<T> gameRule) {
      T value = source.getLevel().getGameRules().get(gameRule);
      source.sendSuccess(() -> Component.translatable("commands.gamerule.query", gameRule.id(), gameRule.serialize(value)), false);
      return gameRule.getCommandResult(value);
   }
}
