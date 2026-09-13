package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;

public class ServerPackCommand {
   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("serverpack")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .then(
                  Commands.literal("push")
                     .then(
                        ((RequiredArgumentBuilder)Commands.argument("url", StringArgumentType.string())
                              .then(
                                 ((RequiredArgumentBuilder)Commands.argument("uuid", UuidArgument.uuid())
                                       .then(
                                          Commands.argument("hash", StringArgumentType.word())
                                             .executes(
                                                c -> pushPack(
                                                   (CommandSourceStack)c.getSource(),
                                                   StringArgumentType.getString(c, "url"),
                                                   Optional.of(UuidArgument.getUuid(c, "uuid")),
                                                   Optional.of(StringArgumentType.getString(c, "hash"))
                                                )
                                             )
                                       ))
                                    .executes(
                                       c -> pushPack(
                                          (CommandSourceStack)c.getSource(),
                                          StringArgumentType.getString(c, "url"),
                                          Optional.of(UuidArgument.getUuid(c, "uuid")),
                                          Optional.empty()
                                       )
                                    )
                              ))
                           .executes(
                              c -> pushPack((CommandSourceStack)c.getSource(), StringArgumentType.getString(c, "url"), Optional.empty(), Optional.empty())
                           )
                     )
               ))
            .then(
               Commands.literal("pop")
                  .then(
                     Commands.argument("uuid", UuidArgument.uuid()).executes(c -> popPack((CommandSourceStack)c.getSource(), UuidArgument.getUuid(c, "uuid")))
                  )
            )
      );
   }

   private static void sendToAllConnections(final CommandSourceStack source, final Packet<?> packet) {
      source.getServer().getConnection().getConnections().forEach(connection -> connection.send(packet));
   }

   private static int pushPack(final CommandSourceStack source, final String url, final Optional<UUID> maybeId, final Optional<String> maybeHash) {
      UUID id = maybeId.orElseGet(() -> UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8)));
      String hash = maybeHash.orElse("");
      ClientboundResourcePackPushPacket packet = new ClientboundResourcePackPushPacket(id, url, hash, false, null);
      sendToAllConnections(source, packet);
      return 0;
   }

   private static int popPack(final CommandSourceStack source, final UUID uuid) {
      ClientboundResourcePackPopPacket packet = new ClientboundResourcePackPopPacket(Optional.of(uuid));
      sendToAllConnections(source, packet);
      return 0;
   }
}
