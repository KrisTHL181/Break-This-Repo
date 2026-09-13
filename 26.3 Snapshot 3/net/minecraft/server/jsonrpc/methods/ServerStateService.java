package net.minecraft.server.jsonrpc.methods;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.level.ServerPlayer;

public class ServerStateService {
   public static ServerStateService.ServerState status(final MinecraftApi minecraftApi) {
      return !minecraftApi.serverStateService().isReady()
         ? ServerStateService.ServerState.NOT_STARTED
         : new ServerStateService.ServerState(true, PlayerService.get(minecraftApi), ServerStatus.Version.current());
   }

   public static boolean save(final MinecraftApi minecraftApi, final boolean flush, final ClientInfo clientInfo) {
      return minecraftApi.serverStateService().saveEverything(true, flush, true, clientInfo);
   }

   public static boolean stop(final MinecraftApi minecraftApi, final ClientInfo clientInfo) {
      minecraftApi.submit(() -> minecraftApi.serverStateService().halt(false, clientInfo));
      return true;
   }

   public static boolean systemMessage(final MinecraftApi minecraftApi, final ServerStateService.SystemMessage systemMessage, final ClientInfo clientInfo) {
      Component component = systemMessage.message().asComponent().orElse(null);
      if (component == null) {
         return false;
      }

      if (systemMessage.receivingPlayers().isPresent()) {
         if (systemMessage.receivingPlayers().get().isEmpty()) {
            return false;
         }

         for (PlayerDto playerDto : systemMessage.receivingPlayers().get()) {
            ServerPlayer player;
            if (playerDto.id().isPresent()) {
               player = minecraftApi.playerListService().getPlayer(playerDto.id().get());
            } else {
               if (!playerDto.name().isPresent()) {
                  continue;
               }

               player = minecraftApi.playerListService().getPlayerByName(playerDto.name().get());
            }

            if (player != null) {
               player.sendSystemMessage(component, systemMessage.overlay());
            }
         }
      } else {
         minecraftApi.serverStateService().broadcastSystemMessage(component, systemMessage.overlay(), clientInfo);
      }

      return true;
   }

   public record ServerState(boolean started, List<PlayerDto> players, ServerStatus.Version version) {
      public static final Codec<ServerStateService.ServerState> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               Codec.BOOL.fieldOf("started").forGetter(ServerStateService.ServerState::started),
               PlayerDto.CODEC.codec().listOf().lenientOptionalFieldOf("players", List.of()).forGetter(ServerStateService.ServerState::players),
               ServerStatus.Version.CODEC.fieldOf("version").forGetter(ServerStateService.ServerState::version)
            )
            .apply(i, ServerStateService.ServerState::new)
      );
      public static final ServerStateService.ServerState NOT_STARTED = new ServerStateService.ServerState(false, List.of(), ServerStatus.Version.current());
   }

   public record SystemMessage(Message message, boolean overlay, Optional<List<PlayerDto>> receivingPlayers) {
      public static final Codec<ServerStateService.SystemMessage> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               Message.CODEC.fieldOf("message").forGetter(ServerStateService.SystemMessage::message),
               Codec.BOOL.fieldOf("overlay").forGetter(ServerStateService.SystemMessage::overlay),
               PlayerDto.CODEC.codec().listOf().lenientOptionalFieldOf("receivingPlayers").forGetter(ServerStateService.SystemMessage::receivingPlayers)
            )
            .apply(i, ServerStateService.SystemMessage::new)
      );
   }
}
