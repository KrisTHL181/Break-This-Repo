package net.minecraft.server.jsonrpc.methods;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

public class PlayerService {
   private static final Component DEFAULT_KICK_MESSAGE = Component.translatable("multiplayer.disconnect.kicked");

   public static List<PlayerDto> get(MinecraftApi p_431752_) {
      return p_431752_.playerListService().getPlayers().stream().map(PlayerDto::from).toList();
   }

   public static List<PlayerDto> kick(MinecraftApi p_422922_, List<PlayerService.KickDto> p_431870_, ClientInfo p_426519_) {
      List<PlayerDto> list = new ArrayList<>();

      for (PlayerService.KickDto playerservice$kickdto : p_431870_) {
         ServerPlayer serverplayer = getServerPlayer(p_422922_, playerservice$kickdto.player());
         if (serverplayer != null) {
            p_422922_.playerListService().remove(serverplayer, p_426519_);
            serverplayer.connection.disconnect(playerservice$kickdto.message.flatMap(Message::asComponent).orElse(DEFAULT_KICK_MESSAGE));
            list.add(playerservice$kickdto.player());
         }
      }

      return list;
   }

   private static @Nullable ServerPlayer getServerPlayer(MinecraftApi p_429984_, PlayerDto p_422500_) {
      if (p_422500_.id().isPresent()) {
         return p_429984_.playerListService().getPlayer(p_422500_.id().get());
      } else {
         return p_422500_.name().isPresent() ? p_429984_.playerListService().getPlayerByName(p_422500_.name().get()) : null;
      }
   }

   public record KickDto(PlayerDto player, Optional<Message> message) {
      public static final MapCodec<PlayerService.KickDto> CODEC = RecordCodecBuilder.mapCodec(
         p_431808_ -> p_431808_.group(
               PlayerDto.CODEC.codec().fieldOf("player").forGetter(PlayerService.KickDto::player),
               Message.CODEC.optionalFieldOf("message").forGetter(PlayerService.KickDto::message)
            )
            .apply(p_431808_, PlayerService.KickDto::new)
      );
   }
}
