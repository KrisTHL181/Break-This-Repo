package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import org.jspecify.annotations.Nullable;

public class StopSoundCommand {
   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      RequiredArgumentBuilder<CommandSourceStack, EntitySelector> target = (RequiredArgumentBuilder<CommandSourceStack, EntitySelector>)((RequiredArgumentBuilder)Commands.argument(
               "targets", EntityArgument.players()
            )
            .executes(c -> stopSound((CommandSourceStack)c.getSource(), EntityArgument.getPlayers(c, "targets"), null, null)))
         .then(
            Commands.literal("*")
               .then(
                  Commands.argument("sound", IdentifierArgument.id())
                     .suggests(SuggestionProviders.cast(SuggestionProviders.AVAILABLE_SOUNDS))
                     .executes(
                        c -> stopSound((CommandSourceStack)c.getSource(), EntityArgument.getPlayers(c, "targets"), null, IdentifierArgument.getId(c, "sound"))
                     )
               )
         );

      for (SoundSource source : SoundSource.values()) {
         target.then(
            ((LiteralArgumentBuilder)Commands.literal(source.getName())
                  .executes(c -> stopSound((CommandSourceStack)c.getSource(), EntityArgument.getPlayers(c, "targets"), source, null)))
               .then(
                  Commands.argument("sound", IdentifierArgument.id())
                     .suggests(SuggestionProviders.cast(SuggestionProviders.AVAILABLE_SOUNDS))
                     .executes(
                        c -> stopSound((CommandSourceStack)c.getSource(), EntityArgument.getPlayers(c, "targets"), source, IdentifierArgument.getId(c, "sound"))
                     )
               )
         );
      }

      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("stopsound").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(target)
      );
   }

   private static int stopSound(
      final CommandSourceStack source, final Collection<ServerPlayer> targets, final @Nullable SoundSource soundSource, final @Nullable Identifier sound
   ) {
      ClientboundStopSoundPacket packet = new ClientboundStopSoundPacket(sound, soundSource);

      for (ServerPlayer player : targets) {
         player.connection.send(packet);
      }

      if (soundSource != null) {
         if (sound != null) {
            source.sendSuccess(
               () -> Component.translatable("commands.stopsound.success.source.sound", Component.translationArg(sound), soundSource.getName()), true
            );
         } else {
            source.sendSuccess(() -> Component.translatable("commands.stopsound.success.source.any", soundSource.getName()), true);
         }
      } else if (sound != null) {
         source.sendSuccess(() -> Component.translatable("commands.stopsound.success.sourceless.sound", Component.translationArg(sound)), true);
      } else {
         source.sendSuccess(() -> Component.translatable("commands.stopsound.success.sourceless.any"), true);
      }

      return targets.size();
   }
}
