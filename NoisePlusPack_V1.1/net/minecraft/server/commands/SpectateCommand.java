package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public class SpectateCommand {
   private static final SimpleCommandExceptionType ERROR_SELF = new SimpleCommandExceptionType(Component.translatable("commands.spectate.self"));
   private static final DynamicCommandExceptionType ERROR_NOT_SPECTATOR = new DynamicCommandExceptionType(
      p_308882_ -> Component.translatableEscape("commands.spectate.not_spectator", p_308882_)
   );
   private static final DynamicCommandExceptionType ERROR_CANNOT_SPECTATE = new DynamicCommandExceptionType(
      p_421369_ -> Component.translatableEscape("commands.spectate.cannot_spectate", p_421369_)
   );

   public static void register(CommandDispatcher<CommandSourceStack> p_138678_) {
      p_138678_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("spectate")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .executes(
                  p_138692_ -> spectate((CommandSourceStack)p_138692_.getSource(), null, ((CommandSourceStack)p_138692_.getSource()).getPlayerOrException())
               ))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("target", EntityArgument.entity())
                     .executes(
                        p_138690_ -> spectate(
                           (CommandSourceStack)p_138690_.getSource(),
                           EntityArgument.getEntity(p_138690_, "target"),
                           ((CommandSourceStack)p_138690_.getSource()).getPlayerOrException()
                        )
                     ))
                  .then(
                     Commands.argument("player", EntityArgument.player())
                        .executes(
                           p_138680_ -> spectate(
                              (CommandSourceStack)p_138680_.getSource(),
                              EntityArgument.getEntity(p_138680_, "target"),
                              EntityArgument.getPlayer(p_138680_, "player")
                           )
                        )
                  )
            )
      );
   }

   private static int spectate(CommandSourceStack p_138684_, @Nullable Entity p_138685_, ServerPlayer p_138686_) throws CommandSyntaxException {
      if (p_138686_ == p_138685_) {
         throw ERROR_SELF.create();
      }

      if (!p_138686_.isSpectator()) {
         throw ERROR_NOT_SPECTATOR.create(p_138686_.getDisplayName());
      }

      if (p_138685_ != null && p_138685_.getType().clientTrackingRange() == 0) {
         throw ERROR_CANNOT_SPECTATE.create(p_138685_.getDisplayName());
      }

      p_138686_.setCamera(p_138685_);
      if (p_138685_ != null) {
         p_138684_.sendSuccess(() -> Component.translatable("commands.spectate.success.started", p_138685_.getDisplayName()), false);
      } else {
         p_138684_.sendSuccess(() -> Component.translatable("commands.spectate.success.stopped"), false);
      }

      return 1;
   }
}
