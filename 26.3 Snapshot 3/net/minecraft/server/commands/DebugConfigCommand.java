package net.minecraft.server.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceOrIdArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.Holder;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.jspecify.annotations.Nullable;

public class DebugConfigCommand {
   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("debugconfig")
                     .requires(Commands.hasPermission(Commands.LEVEL_ADMINS)))
                  .then(
                     Commands.literal("config")
                        .then(
                           Commands.argument("target", EntityArgument.player())
                              .executes(c -> config((CommandSourceStack)c.getSource(), EntityArgument.getPlayer(c, "target")))
                        )
                  ))
               .then(
                  Commands.literal("unconfig")
                     .then(
                        Commands.argument("target", UuidArgument.uuid())
                           .suggests((c, p) -> SharedSuggestionProvider.suggest(getUuidsInConfig(((CommandSourceStack)c.getSource()).getServer()), p))
                           .executes(c -> unconfig((CommandSourceStack)c.getSource(), UuidArgument.getUuid(c, "target")))
                     )
               ))
            .then(
               Commands.literal("dialog")
                  .then(
                     Commands.argument("target", UuidArgument.uuid())
                        .suggests((c, p) -> SharedSuggestionProvider.suggest(getUuidsInConfig(((CommandSourceStack)c.getSource()).getServer()), p))
                        .then(
                           Commands.argument("dialog", ResourceOrIdArgument.dialog(context))
                              .executes(
                                 c -> showDialog(
                                    (CommandSourceStack)c.getSource(), UuidArgument.getUuid(c, "target"), ResourceOrIdArgument.getDialog(c, "dialog")
                                 )
                              )
                        )
                  )
            )
      );
   }

   private static Iterable<String> getUuidsInConfig(final MinecraftServer server) {
      Set<String> result = new HashSet<>();

      for (Connection connection : server.getConnection().getConnections()) {
         if (connection.getPacketListener() instanceof ServerConfigurationPacketListenerImpl configListener) {
            result.add(configListener.getOwner().id().toString());
         }
      }

      return result;
   }

   private static int config(final CommandSourceStack source, final ServerPlayer target) {
      GameProfile gameProfile = target.getGameProfile();
      target.connection.switchToConfig();
      source.sendSuccess(() -> Component.literal("Switched player " + gameProfile.name() + "(" + gameProfile.id() + ") to config mode"), false);
      return 1;
   }

   private static @Nullable ServerConfigurationPacketListenerImpl findConfigPlayer(final MinecraftServer server, final UUID target) {
      for (Connection connection : server.getConnection().getConnections()) {
         if (connection.getPacketListener() instanceof ServerConfigurationPacketListenerImpl configListener && configListener.getOwner().id().equals(target)) {
            return configListener;
         }
      }

      return null;
   }

   private static int unconfig(final CommandSourceStack source, final UUID target) {
      ServerConfigurationPacketListenerImpl listener = findConfigPlayer(source.getServer(), target);
      if (listener != null) {
         listener.returnToWorld();
         return 1;
      } else {
         source.sendFailure(Component.literal("Can't find player to unconfig"));
         return 0;
      }
   }

   private static int showDialog(final CommandSourceStack source, final UUID target, final Holder<Dialog> dialog) {
      ServerConfigurationPacketListenerImpl listener = findConfigPlayer(source.getServer(), target);
      if (listener != null) {
         listener.send(new ClientboundShowDialogPacket(dialog));
         return 1;
      } else {
         source.sendFailure(Component.literal("Can't find player to talk to"));
         return 0;
      }
   }
}
