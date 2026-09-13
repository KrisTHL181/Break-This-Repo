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
   public static ServerStateService.ServerState status(MinecraftApi p_422764_) {
      return !p_422764_.serverStateService().isReady()
         ? ServerStateService.ServerState.NOT_STARTED
         : new ServerStateService.ServerState(true, PlayerService.get(p_422764_), ServerStatus.Version.current());
   }

   public static boolean save(MinecraftApi p_429268_, boolean p_426388_, ClientInfo p_424983_) {
      return p_429268_.serverStateService().saveEverything(true, p_426388_, true, p_424983_);
   }

   public static boolean stop(MinecraftApi p_423117_, ClientInfo p_425676_) {
      p_423117_.submit(() -> p_423117_.serverStateService().halt(false, p_425676_));
      return true;
   }

   public static boolean systemMessage(MinecraftApi p_429772_, ServerStateService.SystemMessage p_423393_, ClientInfo p_425875_) {
      Component component = p_423393_.message().asComponent().orElse(null);
      if (component == null) {
         return false;
      }

      if (p_423393_.receivingPlayers().isPresent()) {
         if (p_423393_.receivingPlayers().get().isEmpty()) {
            return false;
         }

         for (PlayerDto playerdto : p_423393_.receivingPlayers().get()) {
            ServerPlayer serverplayer;
            if (playerdto.id().isPresent()) {
               serverplayer = p_429772_.playerListService().getPlayer(playerdto.id().get());
            } else {
               if (!playerdto.name().isPresent()) {
                  continue;
               }

               serverplayer = p_429772_.playerListService().getPlayerByName(playerdto.name().get());
            }

            if (serverplayer != null) {
               serverplayer.sendSystemMessage(component, p_423393_.overlay());
            }
         }
      } else {
         p_429772_.serverStateService().broadcastSystemMessage(component, p_423393_.overlay(), p_425875_);
      }

      return true;
   }

   public record ServerState(boolean started, List<PlayerDto> players, ServerStatus.Version version) {
      public static final Codec<ServerStateService.ServerState> CODEC = RecordCodecBuilder.create(
         p_429583_ -> p_429583_.group(
               Codec.BOOL.fieldOf("started").forGetter(ServerStateService.ServerState::started),
               PlayerDto.CODEC.codec().listOf().lenientOptionalFieldOf("players", List.of()).forGetter(ServerStateService.ServerState::players),
               ServerStatus.Version.CODEC.fieldOf("version").forGetter(ServerStateService.ServerState::version)
            )
            .apply(p_429583_, ServerStateService.ServerState::new)
      );
      public static final ServerStateService.ServerState NOT_STARTED = new ServerStateService.ServerState(false, List.of(), ServerStatus.Version.current());
   }

   public record SystemMessage(Message message, boolean overlay, Optional<List<PlayerDto>> receivingPlayers) {
      public static final Codec<ServerStateService.SystemMessage> CODEC = RecordCodecBuilder.create(
         p_424091_ -> p_424091_.group(
               Message.CODEC.fieldOf("message").forGetter(ServerStateService.SystemMessage::message),
               Codec.BOOL.fieldOf("overlay").forGetter(ServerStateService.SystemMessage::overlay),
               PlayerDto.CODEC.codec().listOf().lenientOptionalFieldOf("receivingPlayers").forGetter(ServerStateService.SystemMessage::receivingPlayers)
            )
            .apply(p_424091_, ServerStateService.SystemMessage::new)
      );
   }
}
