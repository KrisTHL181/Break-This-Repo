package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.function.Function;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.ResolutionContext;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

public class TitleCommand {
   private static final CommandResponseTracker.Messages<ServerPlayer> RESPONSE_CLEAR_TITLE = CommandResponseTracker.messages(
      (player, var1) -> Component.translatable("commands.title.cleared.single", player.getDisplayName()),
      (playerCount, var1) -> Component.translatable("commands.title.cleared.multiple", playerCount)
   );
   private static final CommandResponseTracker.Messages<ServerPlayer> RESPONSE_RESET_TITLE = CommandResponseTracker.messages(
      (player, var1) -> Component.translatable("commands.title.reset.single", player.getDisplayName()),
      (playerCount, var1) -> Component.translatable("commands.title.reset.multiple", playerCount)
   );
   private static final CommandResponseTracker.Messages<ServerPlayer> RESPONSE_SHOW_TITLE = CommandResponseTracker.messages(
      (player, var1) -> Component.translatable("commands.title.show.title.single", player.getDisplayName()),
      (playerCount, var1) -> Component.translatable("commands.title.show.title.multiple", playerCount)
   );
   private static final CommandResponseTracker.Messages<ServerPlayer> RESPONSE_SHOW_ACTIONBAR = CommandResponseTracker.messages(
      (player, var1) -> Component.translatable("commands.title.show.actionbar.single", player.getDisplayName()),
      (playerCount, var1) -> Component.translatable("commands.title.show.actionbar.multiple", playerCount)
   );
   private static final CommandResponseTracker.Messages<ServerPlayer> RESPONSE_SHOW_SUBTITLE = CommandResponseTracker.messages(
      (player, var1) -> Component.translatable("commands.title.show.subtitle.single", player.getDisplayName()),
      (playerCount, var1) -> Component.translatable("commands.title.show.subtitle.multiple", playerCount)
   );
   private static final CommandResponseTracker.Messages<ServerPlayer> RESPONSE_SET_TIMES = CommandResponseTracker.messages(
      (player, var1) -> Component.translatable("commands.title.times.single", player.getDisplayName()),
      (playerCount, var1) -> Component.translatable("commands.title.times.multiple", playerCount)
   );

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("title").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(
               ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                    "targets", EntityArgument.players()
                                 )
                                 .then(
                                    Commands.literal("clear")
                                       .executes(c -> clearTitle((CommandSourceStack)c.getSource(), EntityArgument.getPlayers(c, "targets")))
                                 ))
                              .then(
                                 Commands.literal("reset")
                                    .executes(c -> resetTitle((CommandSourceStack)c.getSource(), EntityArgument.getPlayers(c, "targets")))
                              ))
                           .then(
                              Commands.literal("title")
                                 .then(
                                    Commands.argument("title", ComponentArgument.textComponent(context))
                                       .executes(
                                          c -> showTitle(
                                             (CommandSourceStack)c.getSource(),
                                             EntityArgument.getPlayers(c, "targets"),
                                             ComponentArgument.getRawComponent(c, "title"),
                                             RESPONSE_SHOW_TITLE,
                                             ClientboundSetTitleTextPacket::new
                                          )
                                       )
                                 )
                           ))
                        .then(
                           Commands.literal("subtitle")
                              .then(
                                 Commands.argument("title", ComponentArgument.textComponent(context))
                                    .executes(
                                       c -> showTitle(
                                          (CommandSourceStack)c.getSource(),
                                          EntityArgument.getPlayers(c, "targets"),
                                          ComponentArgument.getRawComponent(c, "title"),
                                          RESPONSE_SHOW_SUBTITLE,
                                          ClientboundSetSubtitleTextPacket::new
                                       )
                                    )
                              )
                        ))
                     .then(
                        Commands.literal("actionbar")
                           .then(
                              Commands.argument("title", ComponentArgument.textComponent(context))
                                 .executes(
                                    c -> showTitle(
                                       (CommandSourceStack)c.getSource(),
                                       EntityArgument.getPlayers(c, "targets"),
                                       ComponentArgument.getRawComponent(c, "title"),
                                       RESPONSE_SHOW_ACTIONBAR,
                                       ClientboundSetActionBarTextPacket::new
                                    )
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("times")
                        .then(
                           Commands.argument("fadeIn", TimeArgument.time())
                              .then(
                                 Commands.argument("stay", TimeArgument.time())
                                    .then(
                                       Commands.argument("fadeOut", TimeArgument.time())
                                          .executes(
                                             c -> setTimes(
                                                (CommandSourceStack)c.getSource(),
                                                EntityArgument.getPlayers(c, "targets"),
                                                IntegerArgumentType.getInteger(c, "fadeIn"),
                                                IntegerArgumentType.getInteger(c, "stay"),
                                                IntegerArgumentType.getInteger(c, "fadeOut")
                                             )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static int sendPacketToPlayers(
      final CommandSourceStack source,
      final Collection<ServerPlayer> targets,
      final Packet<?> packet,
      final CommandResponseTracker.Messages<ServerPlayer> messages
   ) throws CommandSyntaxException {
      CommandResponseTracker<ServerPlayer> tracker = CommandResponseTracker.create();

      for (ServerPlayer player : targets) {
         player.connection.send(packet);
         tracker.track(player);
      }

      return tracker.sendFeedback(source, true, messages);
   }

   private static int clearTitle(final CommandSourceStack source, final Collection<ServerPlayer> targets) throws CommandSyntaxException {
      return sendPacketToPlayers(source, targets, new ClientboundClearTitlesPacket(false), RESPONSE_CLEAR_TITLE);
   }

   private static int resetTitle(final CommandSourceStack source, final Collection<ServerPlayer> targets) throws CommandSyntaxException {
      return sendPacketToPlayers(source, targets, new ClientboundClearTitlesPacket(true), RESPONSE_RESET_TITLE);
   }

   private static int showTitle(
      final CommandSourceStack source,
      final Collection<ServerPlayer> targets,
      final Component title,
      final CommandResponseTracker.Messages<ServerPlayer> messages,
      final Function<Component, Packet<?>> factory
   ) throws CommandSyntaxException {
      CommandResponseTracker<ServerPlayer> tracker = CommandResponseTracker.create();

      for (ServerPlayer player : targets) {
         player.connection
            .send(factory.apply(ComponentUtils.resolve(ResolutionContext.builder().withSource(source).withEntityOverride(player).build(), title)));
         tracker.track(player);
      }

      return tracker.sendFeedback(source, true, messages);
   }

   private static int setTimes(final CommandSourceStack source, final Collection<ServerPlayer> targets, final int fadeIn, final int stay, final int fadeOut) throws CommandSyntaxException {
      return sendPacketToPlayers(source, targets, new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut), RESPONSE_SET_TIMES);
   }
}
