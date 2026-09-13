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
   public static void register(CommandDispatcher<CommandSourceStack> p_299014_, CommandBuildContext p_408756_) {
      p_299014_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("debugconfig")
                     .requires(Commands.hasPermission(Commands.LEVEL_ADMINS)))
                  .then(
                     Commands.literal("config")
                        .then(
                           Commands.argument("target", EntityArgument.player())
                              .executes(p_300433_ -> config((CommandSourceStack)p_300433_.getSource(), EntityArgument.getPlayer(p_300433_, "target")))
                        )
                  ))
               .then(
                  Commands.literal("unconfig")
                     .then(
                        Commands.argument("target", UuidArgument.uuid())
                           .suggests(
                              (p_297904_, p_297883_) -> SharedSuggestionProvider.suggest(
                                 getUuidsInConfig(((CommandSourceStack)p_297904_.getSource()).getServer()), p_297883_
                              )
                           )
                           .executes(p_301004_ -> unconfig((CommandSourceStack)p_301004_.getSource(), UuidArgument.getUuid(p_301004_, "target")))
                     )
               ))
            .then(
               Commands.literal("dialog")
                  .then(
                     Commands.argument("target", UuidArgument.uuid())
                        .suggests(
                           (p_405154_, p_405155_) -> SharedSuggestionProvider.suggest(
                              getUuidsInConfig(((CommandSourceStack)p_405154_.getSource()).getServer()), p_405155_
                           )
                        )
                        .then(
                           Commands.argument("dialog", ResourceOrIdArgument.dialog(p_408756_))
                              .executes(
                                 p_405153_ -> showDialog(
                                    (CommandSourceStack)p_405153_.getSource(),
                                    UuidArgument.getUuid(p_405153_, "target"),
                                    ResourceOrIdArgument.getDialog(p_405153_, "dialog")
                                 )
                              )
                        )
                  )
            )
      );
   }

   private static Iterable<String> getUuidsInConfig(MinecraftServer p_299245_) {
      Set<String> set = new HashSet<>();

      for (Connection connection : p_299245_.getConnection().getConnections()) {
         if (connection.getPacketListener() instanceof ServerConfigurationPacketListenerImpl serverconfigurationpacketlistenerimpl) {
            set.add(serverconfigurationpacketlistenerimpl.getOwner().id().toString());
         }
      }

      return set;
   }

   private static int config(CommandSourceStack p_297745_, ServerPlayer p_300074_) {
      GameProfile gameprofile = p_300074_.getGameProfile();
      p_300074_.connection.switchToConfig();
      p_297745_.sendSuccess(() -> Component.literal("Switched player " + gameprofile.name() + "(" + gameprofile.id() + ") to config mode"), false);
      return 1;
   }

   private static @Nullable ServerConfigurationPacketListenerImpl findConfigPlayer(MinecraftServer p_409452_, UUID p_407916_) {
      for (Connection connection : p_409452_.getConnection().getConnections()) {
         if (connection.getPacketListener() instanceof ServerConfigurationPacketListenerImpl serverconfigurationpacketlistenerimpl
            && serverconfigurationpacketlistenerimpl.getOwner().id().equals(p_407916_)) {
            return serverconfigurationpacketlistenerimpl;
         }
      }

      return null;
   }

   private static int unconfig(CommandSourceStack p_300627_, UUID p_299392_) {
      ServerConfigurationPacketListenerImpl serverconfigurationpacketlistenerimpl = findConfigPlayer(p_300627_.getServer(), p_299392_);
      if (serverconfigurationpacketlistenerimpl != null) {
         serverconfigurationpacketlistenerimpl.returnToWorld();
         return 1;
      } else {
         p_300627_.sendFailure(Component.literal("Can't find player to unconfig"));
         return 0;
      }
   }

   private static int showDialog(CommandSourceStack p_406029_, UUID p_406101_, Holder<Dialog> p_409673_) {
      ServerConfigurationPacketListenerImpl serverconfigurationpacketlistenerimpl = findConfigPlayer(p_406029_.getServer(), p_406101_);
      if (serverconfigurationpacketlistenerimpl != null) {
         serverconfigurationpacketlistenerimpl.send(new ClientboundShowDialogPacket(p_409673_));
         return 1;
      } else {
         p_406029_.sendFailure(Component.literal("Can't find player to talk to"));
         return 0;
      }
   }
}
