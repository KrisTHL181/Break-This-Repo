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

   public static List<IpBanlistService.IpBanDto> get(MinecraftApi p_425297_) {
      return p_425297_.banListService().getIpBanEntries().stream().map(IpBanlistService.IpBan::from).map(IpBanlistService.IpBanDto::from).toList();
   }

   public static List<IpBanlistService.IpBanDto> add(MinecraftApi p_424721_, List<IpBanlistService.IncomingIpBanDto> p_423129_, ClientInfo p_425885_) {
      p_423129_.stream()
         .map(p_425915_ -> banIp(p_424721_, p_425915_, p_425885_))
         .flatMap(Collection::stream)
         .forEach(p_429944_ -> p_429944_.connection.disconnect(Component.translatable("multiplayer.disconnect.ip_banned")));
      return get(p_424721_);
   }

   private static List<ServerPlayer> banIp(MinecraftApi p_426929_, IpBanlistService.IncomingIpBanDto p_424770_, ClientInfo p_428837_) {
      IpBanlistService.IpBan ipbanlistservice$ipban = p_424770_.toIpBan();
      if (ipbanlistservice$ipban != null) {
         return banIp(p_426929_, ipbanlistservice$ipban, p_428837_);
      }

      if (p_424770_.player().isPresent()) {
         Optional<ServerPlayer> optional = p_426929_.playerListService().getPlayer(p_424770_.player().get().id(), p_424770_.player().get().name());
         if (optional.isPresent()) {
            return banIp(p_426929_, p_424770_.toIpBan(optional.get()), p_428837_);
         }
      }

      return List.of();
   }

   private static List<ServerPlayer> banIp(MinecraftApi p_424247_, IpBanlistService.IpBan p_429030_, ClientInfo p_427912_) {
      p_424247_.banListService().addIpBan(p_429030_.toIpBanEntry(), p_427912_);
      return p_424247_.playerListService().getPlayersWithAddress(p_429030_.ip());
   }

   public static List<IpBanlistService.IpBanDto> clear(MinecraftApi p_427207_, ClientInfo p_427875_) {
      p_427207_.banListService().clearIpBans(p_427875_);
      return get(p_427207_);
   }

   public static List<IpBanlistService.IpBanDto> remove(MinecraftApi p_425865_, List<String> p_426022_, ClientInfo p_427811_) {
      p_426022_.forEach(p_430043_ -> p_425865_.banListService().removeIpBan(p_430043_, p_427811_));
      return get(p_425865_);
   }

   public static List<IpBanlistService.IpBanDto> set(MinecraftApi p_426333_, List<IpBanlistService.IpBanDto> p_423908_, ClientInfo p_430642_) {
      Set<IpBanlistService.IpBan> set = p_423908_.stream()
         .filter(p_428390_ -> InetAddresses.isInetAddress(p_428390_.ip()))
         .map(IpBanlistService.IpBanDto::toIpBan)
         .collect(Collectors.toSet());
      Set<IpBanlistService.IpBan> set1 = p_426333_.banListService().getIpBanEntries().stream().map(IpBanlistService.IpBan::from).collect(Collectors.toSet());
      set1.stream().filter(p_428566_ -> !set.contains(p_428566_)).forEach(p_422461_ -> p_426333_.banListService().removeIpBan(p_422461_.ip(), p_430642_));
      set.stream()
         .filter(p_424282_ -> !set1.contains(p_424282_))
         .forEach(p_428414_ -> p_426333_.banListService().addIpBan(p_428414_.toIpBanEntry(), p_430642_));
      set.stream()
         .filter(p_426115_ -> !set1.contains(p_426115_))
         .flatMap(p_423967_ -> p_426333_.playerListService().getPlayersWithAddress(p_423967_.ip()).stream())
         .forEach(p_430766_ -> p_430766_.connection.disconnect(Component.translatable("multiplayer.disconnect.ip_banned")));
      return get(p_426333_);
   }

   public record IncomingIpBanDto(Optional<PlayerDto> player, Optional<String> ip, Optional<String> reason, Optional<String> source, Optional<Instant> expires) {
      public static final MapCodec<IpBanlistService.IncomingIpBanDto> CODEC = RecordCodecBuilder.mapCodec(
         p_422618_ -> p_422618_.group(
               PlayerDto.CODEC.codec().optionalFieldOf("player").forGetter(IpBanlistService.IncomingIpBanDto::player),
               Codec.STRING.optionalFieldOf("ip").forGetter(IpBanlistService.IncomingIpBanDto::ip),
               Codec.STRING.optionalFieldOf("reason").forGetter(IpBanlistService.IncomingIpBanDto::reason),
               Codec.STRING.optionalFieldOf("source").forGetter(IpBanlistService.IncomingIpBanDto::source),
               ExtraCodecs.INSTANT_ISO8601.optionalFieldOf("expires").forGetter(IpBanlistService.IncomingIpBanDto::expires)
            )
            .apply(p_422618_, IpBanlistService.IncomingIpBanDto::new)
      );

      IpBanlistService.IpBan toIpBan(ServerPlayer p_424961_) {
         return new IpBanlistService.IpBan(p_424961_.getIpAddress(), this.reason().orElse(null), this.source().orElse("Management server"), this.expires());
      }

      IpBanlistService.@Nullable IpBan toIpBan() {
         return !this.ip().isEmpty() && InetAddresses.isInetAddress(this.ip().get())
            ? new IpBanlistService.IpBan(this.ip().get(), this.reason().orElse(null), this.source().orElse("Management server"), this.expires())
            : null;
      }
   }

   record IpBan(String ip, @Nullable String reason, String source, Optional<Instant> expires) {
      static IpBanlistService.IpBan from(IpBanListEntry p_426553_) {
         return new IpBanlistService.IpBan(
            Objects.requireNonNull(p_426553_.getUser()),
            p_426553_.getReason(),
            p_426553_.getSource(),
            Optional.ofNullable(p_426553_.getExpires()).map(Date::toInstant)
         );
      }

      IpBanListEntry toIpBanEntry() {
         return new IpBanListEntry(this.ip(), null, this.source(), this.expires().map(Date::from).orElse(null), this.reason());
      }
   }

   public record IpBanDto(String ip, Optional<String> reason, Optional<String> source, Optional<Instant> expires) {
      public static final MapCodec<IpBanlistService.IpBanDto> CODEC = RecordCodecBuilder.mapCodec(
         p_429548_ -> p_429548_.group(
               Codec.STRING.fieldOf("ip").forGetter(IpBanlistService.IpBanDto::ip),
               Codec.STRING.optionalFieldOf("reason").forGetter(IpBanlistService.IpBanDto::reason),
               Codec.STRING.optionalFieldOf("source").forGetter(IpBanlistService.IpBanDto::source),
               ExtraCodecs.INSTANT_ISO8601.optionalFieldOf("expires").forGetter(IpBanlistService.IpBanDto::expires)
            )
            .apply(p_429548_, IpBanlistService.IpBanDto::new)
      );

      private static IpBanlistService.IpBanDto from(IpBanlistService.IpBan p_431578_) {
         return new IpBanlistService.IpBanDto(p_431578_.ip(), Optional.ofNullable(p_431578_.reason()), Optional.of(p_431578_.source()), p_431578_.expires());
      }

      public static IpBanlistService.IpBanDto from(IpBanListEntry p_423216_) {
         return from(IpBanlistService.IpBan.from(p_423216_));
      }

      private IpBanlistService.IpBan toIpBan() {
         return new IpBanlistService.IpBan(this.ip(), this.reason().orElse(null), this.source().orElse("Management server"), this.expires());
      }
   }
}
