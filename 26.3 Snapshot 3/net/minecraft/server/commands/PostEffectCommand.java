package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public class PostEffectCommand {
   private static final SimpleCommandExceptionType ERROR_ADD_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.posteffect.add.failed"));
   private static final SimpleCommandExceptionType ERROR_CLEAR_FAILED = new SimpleCommandExceptionType(
      Component.translatable("commands.posteffect.clear.failed")
   );
   private static final SimpleCommandExceptionType ERROR_REMOVE_FAILED = new SimpleCommandExceptionType(
      Component.translatable("commands.posteffect.remove.failed")
   );
   private static final CommandResponseTracker.MessagesWithArg<ServerPlayer, Identifier> RESPONSE_ADD = CommandResponseTracker.messages(
      ERROR_ADD_FAILED,
      (entity, var1, posteffect) -> Component.translatable(
         "commands.posteffect.add.success.single", Component.translationArg(posteffect), entity.getDisplayName()
      ),
      (entityCount, var1, posteffect) -> Component.translatable("commands.posteffect.add.success.multiple", Component.translationArg(posteffect), entityCount)
   );
   private static final CommandResponseTracker.Messages<ServerPlayer> RESPONSE_CLEAR = CommandResponseTracker.messages(
      ERROR_CLEAR_FAILED,
      (entity, var1) -> Component.translatable("commands.posteffect.clear.success.single", entity.getDisplayName()),
      (entityCount, var1) -> Component.translatable("commands.posteffect.clear.success.multiple", entityCount)
   );
   private static final CommandResponseTracker.MessagesWithArg<ServerPlayer, Identifier> RESPONSE_REMOVE = CommandResponseTracker.messages(
      ERROR_REMOVE_FAILED,
      (entity, var1, posteffect) -> Component.translatable(
         "commands.posteffect.remove.success.single", Component.translationArg(posteffect), entity.getDisplayName()
      ),
      (entityCount, var1, posteffect) -> Component.translatable(
         "commands.posteffect.remove.success.multiple", Component.translationArg(posteffect), entityCount
      )
   );

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                           "posteffect"
                        )
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                     .then(
                        Commands.literal("add")
                           .then(
                              Commands.argument("targets", EntityArgument.players())
                                 .then(
                                    Commands.argument("posteffect", IdentifierArgument.id())
                                       .suggests(SuggestionProviders.cast(SuggestionProviders.POST_EFFECTS))
                                       .executes(
                                          c -> addPostEffect(
                                             (CommandSourceStack)c.getSource(),
                                             EntityArgument.getPlayers(c, "targets"),
                                             IdentifierArgument.getId(c, "posteffect")
                                          )
                                       )
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("clear")
                        .then(
                           Commands.argument("targets", EntityArgument.players())
                              .executes(c -> clearPostEffect((CommandSourceStack)c.getSource(), EntityArgument.getPlayers(c, "targets")))
                        )
                  ))
               .then(
                  Commands.literal("list")
                     .then(
                        Commands.argument("target", EntityArgument.player())
                           .executes(c -> listPostEffects((CommandSourceStack)c.getSource(), EntityArgument.getPlayer(c, "target")))
                     )
               ))
            .then(
               Commands.literal("remove")
                  .then(
                     Commands.argument("targets", EntityArgument.players())
                        .then(
                           Commands.argument("posteffect", IdentifierArgument.id())
                              .suggests(SuggestionProviders.cast(SuggestionProviders.POST_EFFECTS))
                              .executes(
                                 c -> removePostEffect(
                                    (CommandSourceStack)c.getSource(), EntityArgument.getPlayers(c, "targets"), IdentifierArgument.getId(c, "posteffect")
                                 )
                              )
                        )
                  )
            )
      );
   }

   private static int addPostEffect(final CommandSourceStack source, final Collection<ServerPlayer> players, final Identifier posteffect) throws CommandSyntaxException {
      CommandResponseTracker<ServerPlayer> tracker = CommandResponseTracker.create();

      for (ServerPlayer player : players) {
         tracker.track(player, player.addPostEffect(posteffect));
      }

      return tracker.sendFeedback(source, true, RESPONSE_ADD, posteffect);
   }

   private static int clearPostEffect(final CommandSourceStack source, final Collection<ServerPlayer> players) throws CommandSyntaxException {
      CommandResponseTracker<ServerPlayer> tracker = CommandResponseTracker.create();

      for (ServerPlayer player : players) {
         tracker.track(player, player.clearPostEffects());
      }

      return tracker.sendFeedback(source, true, RESPONSE_CLEAR);
   }

   private static int listPostEffects(final CommandSourceStack source, final ServerPlayer player) {
      List<Identifier> postEffects = player.getPostEffects();
      if (postEffects.isEmpty()) {
         source.sendSuccess(() -> Component.translatable("commands.posteffect.list.empty", player.getDisplayName()), false);
      } else {
         String names = postEffects.stream().map(Identifier::toString).collect(Collectors.joining(", "));
         source.sendSuccess(() -> Component.translatable("commands.posteffect.list.success", player.getDisplayName(), postEffects.size(), names), false);
      }

      return postEffects.size();
   }

   private static int removePostEffect(final CommandSourceStack source, final Collection<ServerPlayer> players, final Identifier posteffect) throws CommandSyntaxException {
      CommandResponseTracker<ServerPlayer> tracker = CommandResponseTracker.create();

      for (ServerPlayer player : players) {
         tracker.track(player, player.removePostEffect(posteffect));
      }

      return tracker.sendFeedback(source, true, RESPONSE_REMOVE, posteffect);
   }
}
