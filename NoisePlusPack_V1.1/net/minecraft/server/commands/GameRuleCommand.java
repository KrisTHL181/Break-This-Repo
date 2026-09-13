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
   public static void register(CommandDispatcher<CommandSourceStack> p_137745_, CommandBuildContext p_368575_) {
      final LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("gamerule")
         .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
      new GameRules(p_368575_.enabledFeatures())
         .visitGameRuleTypes(
            new GameRuleTypeVisitor() {
               @Override
               public <T> void visit(GameRule<T> p_457702_) {
                  LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder1 = Commands.literal(p_457702_.id());
                  LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder2 = Commands.literal(p_457702_.getIdentifier().toString());
                  ((LiteralArgumentBuilder)literalargumentbuilder.then(GameRuleCommand.buildRuleArguments(p_457702_, literalargumentbuilder1)))
                     .then(GameRuleCommand.buildRuleArguments(p_457702_, literalargumentbuilder2));
               }
            }
         );
      p_137745_.register(literalargumentbuilder);
   }

   static <T> LiteralArgumentBuilder<CommandSourceStack> buildRuleArguments(GameRule<T> p_457531_, LiteralArgumentBuilder<CommandSourceStack> p_458298_) {
      return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)p_458298_.executes(
            p_448952_ -> queryRule((CommandSourceStack)p_448952_.getSource(), p_457531_)
         ))
         .then(Commands.argument("value", p_457531_.argument()).executes(p_448948_ -> setRule(p_448948_, p_457531_)));
   }

   private static <T> int setRule(CommandContext<CommandSourceStack> p_137755_, GameRule<T> p_458900_) {
      CommandSourceStack commandsourcestack = (CommandSourceStack)p_137755_.getSource();
      T t = (T)p_137755_.getArgument("value", p_458900_.valueClass());
      commandsourcestack.getLevel().getGameRules().set(p_458900_, t, ((CommandSourceStack)p_137755_.getSource()).getServer());
      commandsourcestack.sendSuccess(() -> Component.translatable("commands.gamerule.set", p_458900_.id(), p_458900_.serialize(t)), true);
      return p_458900_.getCommandResult(t);
   }

   private static <T> int queryRule(CommandSourceStack p_137758_, GameRule<T> p_455211_) {
      T t = p_137758_.getLevel().getGameRules().get(p_455211_);
      p_137758_.sendSuccess(() -> Component.translatable("commands.gamerule.query", p_455211_.id(), p_455211_.serialize(t)), false);
      return p_455211_.getCommandResult(t);
   }
}
