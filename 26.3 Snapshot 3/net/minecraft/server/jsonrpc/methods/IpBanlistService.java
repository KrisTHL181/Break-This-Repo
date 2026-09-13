package net.minecraft.server.jsonrpc.methods;

import com.google.common.net.InetAddresses;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.network.chat.Component;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.util.ExtraCodecs;
import org.jspecify.annotations.Nullable;

public class IpBanlistService {
   private static final String BAN_SOURCE = "Management server";

   public static List<IpBanlistService.IpBanDto> get(final MinecraftApi minecraftApi) {
      return minecraftApi.banListService().getIpBanEntries().stream().map(IpBanlistService.IpBan::from).map(IpBanlistService.IpBanDto::from).toList();
   }

   public static List<IpBanlistService.IpBanDto> add(
      final MinecraftApi minecraftApi, final List<IpBanlistService.IncomingIpBanDto> bans, final ClientInfo clientInfo
   ) {
      bans.stream()
         .map(ban -> banIp(minecraftApi, ban, clientInfo))
         .flatMap(Collection::stream)
         .forEach(player -> player.connection.disconnect(Component.translatable("multiplayer.disconnect.ip_banned")));
      return get(minecraftApi);
   }

   private static List<ServerPlayer> banIp(final MinecraftApi minecraftApi, final IpBanlistService.IncomingIpBanDto ban, final ClientInfo clientInfo) {
      IpBanlistService.IpBan ipBan = ban.toIpBan();
      if (ipBan != null) {
         return banIp(minecraftApi, ipBan, clientInfo);
      }

      if (ban.player().isPresent()) {
         Optional<ServerPlayer> player = minecraftApi.playerListService().getPlayer(ban.player().get().id(), ban.player().get().name());
         if (player.isPresent()) {
            return banIp(minecraftApi, ban.toIpBan(player.get()), clientInfo);
         }
      }

      return List.of();
   }

   private static List<ServerPlayer> banIp(final MinecraftApi minecraftApi, final IpBanlistService.IpBan ban, final ClientInfo clientInfo) {
      minecraftApi.banListService().addIpBan(ban.toIpBanEntry(), clientInfo);
      return minecraftApi.playerListService().getPlayersWithAddress(ban.ip());
   }

   public static List<IpBanlistService.IpBanDto> clear(final MinecraftApi minecraftApi, final ClientInfo clientInfo) {
      minecraftApi.banListService().clearIpBans(clientInfo);
      return get(minecraftApi);
   }

   public static List<IpBanlistService.IpBanDto> remove(final MinecraftApi minecraftApi, final List<String> ban, final ClientInfo clientInfo) {
      ban.forEach(ip -> minecraftApi.banListService().removeIpBan(ip, clientInfo));
      return get(minecraftApi);
   }

   public static List<IpBanlistService.IpBanDto> set(final MinecraftApi minecraftApi, final List<IpBanlistService.IpBanDto> ips, final ClientInfo clientInfo) {
      Set<IpBanlistService.IpBan> finalBanlist = ips.stream()
         .filter(ban -> InetAddresses.isInetAddress(ban.ip()))
         .map(IpBanlistService.IpBanDto::toIpBan)
         .collect(Collectors.toSet());
      Set<IpBanlistService.IpBan> currentBans = minecraftApi.banListService()
         .getIpBanEntries()
         .stream()
         .map(IpBanlistService.IpBan::from)
         .collect(Collectors.toSet());
      currentBans.stream().filter(ban -> !finalBanlist.contains(ban)).forEach(ban -> minecraftApi.banListService().removeIpBan(ban.ip(), clientInfo));
      finalBanlist.stream().filter(ban -> !currentBans.contains(ban)).forEach(ban -> minecraftApi.banListService().addIpBan(ban.toIpBanEntry(), clientInfo));
      finalBanlist.stream()
         .filter(ban -> !currentBans.contains(ban))
         .flatMap(ban -> minecraftApi.playerListService().getPlayersWithAddress(ban.ip()).stream())
         .forEach(player -> player.connection.disconnect(Component.translatable("multiplayer.disconnect.ip_banned")));
      return get(minecraftApi);
   }

   public record IncomingIpBanDto(Optional<PlayerDto> player, Optional<String> ip, Optional<String> reason, Optional<String> source, Optional<Instant> expires) {
      public static final MapCodec<IpBanlistService.IncomingIpBanDto> CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               PlayerDto.CODEC.codec().optionalFieldOf("player").forGetter(IpBanlistService.IncomingIpBanDto::player),
               Codec.STRING.optionalFieldOf("ip").forGetter(IpBanlistService.IncomingIpBanDto::ip),
               Codec.STRING.optionalFieldOf("reason").forGetter(IpBanlistService.IncomingIpBanDto::reason),
               Codec.STRING.optionalFieldOf("source").forGetter(IpBanlistService.IncomingIpBanDto::source),
               ExtraCodecs.INSTANT_ISO8601.optionalFieldOf("expires").forGetter(IpBanlistService.IncomingIpBanDto::expires)
            )
            .apply(i, IpBanlistService.IncomingIpBanDto::new)
      );

      private IpBanlistService.IpBan toIpBan(final ServerPlayer player) {
         return new IpBanlistService.IpBan(player.getIpAddress(), this.reason().orElse(null), this.source().orElse("Management server"), this.expires());
      }

      private IpBanlistService.@Nullable IpBan toIpBan() {
         return !this.ip().isEmpty() && InetAddresses.isInetAddress(this.ip().get())
            ? new IpBanlistService.IpBan(this.ip().get(), this.reason().orElse(null), this.source().orElse("Management server"), this.expires())
            : null;
      }
   }

   private record IpBan(String ip, @Nullable String reason, String source, Optional<Instant> expires) {
      private static IpBanlistService.IpBan from(final IpBanListEntry entry) {
         return new IpBanlistService.IpBan(
            Objects.requireNonNull(entry.getUser()), entry.getReason(), entry.getSource(), Optional.ofNullable(entry.getExpires()).map(Date::toInstant)
         );
      }

      private IpBanListEntry toIpBanEntry() {
         return new IpBanListEntry(this.ip(), null, this.source(), this.expires().map(Date::from).orElse(null), this.reason());
      }
   }

   public record IpBanDto(String ip, Optional<String> reason, Optional<String> source, Optional<Instant> expires) {
      public static final MapCodec<IpBanlistService.IpBanDto> CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               Codec.STRING.fieldOf("ip").forGetter(IpBanlistService.IpBanDto::ip),
               Codec.STRING.optionalFieldOf("reason").forGetter(IpBanlistService.IpBanDto::reason),
               Codec.STRING.optionalFieldOf("source").forGetter(IpBanlistService.IpBanDto::source),
               ExtraCodecs.INSTANT_ISO8601.optionalFieldOf("expires").forGetter(IpBanlistService.IpBanDto::expires)
            )
            .apply(i, IpBanlistService.IpBanDto::new)
      );

      private static IpBanlistService.IpBanDto from(final IpBanlistService.IpBan ban) {
         return new IpBanlistService.IpBanDto(ban.ip(), Optional.ofNullable(ban.reason()), Optional.of(ban.source()), ban.expires());
      }

      public static IpBanlistService.IpBanDto from(final IpBanListEntry ban) {
         return from(IpBanlistService.IpBan.from(ban));
      }

      private IpBanlistService.IpBan toIpBan() {
         return new IpBanlistService.IpBan(this.ip(), this.reason().orElse(null), this.source().orElse("Management server"), this.expires());
      }
   }
}
