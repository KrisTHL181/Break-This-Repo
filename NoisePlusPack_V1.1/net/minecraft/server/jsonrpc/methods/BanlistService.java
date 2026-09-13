package net.minecraft.server.jsonrpc.methods;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.network.chat.Component;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class BanlistService {
   private static final String BAN_SOURCE = "Management server";

   public static List<BanlistService.UserBanDto> get(MinecraftApi p_424718_) {
      return p_424718_.banListService()
         .getUserBanEntries()
         .stream()
         .filter(p_428700_ -> p_428700_.getUser() != null)
         .map(BanlistService.UserBan::from)
         .map(BanlistService.UserBanDto::from)
         .toList();
   }

   public static List<BanlistService.UserBanDto> add(MinecraftApi p_426185_, List<BanlistService.UserBanDto> p_428516_, ClientInfo p_429256_) {
      List<CompletableFuture<Optional<BanlistService.UserBan>>> list = p_428516_.stream()
         .map(
            p_424633_ -> p_426185_.playerListService()
               .getUser(p_424633_.player().id(), p_424633_.player().name())
               .thenApply(p_426644_ -> p_426644_.map(p_424633_::toUserBan))
         )
         .toList();

      for (Optional<BanlistService.UserBan> optional : Util.sequence(list).join()) {
         if (!optional.isEmpty()) {
            BanlistService.UserBan banlistservice$userban = optional.get();
            p_426185_.banListService().addUserBan(banlistservice$userban.toBanEntry(), p_429256_);
            ServerPlayer serverplayer = p_426185_.playerListService().getPlayer(optional.get().player().id());
            if (serverplayer != null) {
               serverplayer.connection.disconnect(Component.translatable("multiplayer.disconnect.banned"));
            }
         }
      }

      return get(p_426185_);
   }

   public static List<BanlistService.UserBanDto> clear(MinecraftApi p_427947_, ClientInfo p_426070_) {
      p_427947_.banListService().clearUserBans(p_426070_);
      return get(p_427947_);
   }

   public static List<BanlistService.UserBanDto> remove(MinecraftApi p_428696_, List<PlayerDto> p_428704_, ClientInfo p_431322_) {
      List<CompletableFuture<Optional<NameAndId>>> list = p_428704_.stream()
         .map(p_427282_ -> p_428696_.playerListService().getUser(p_427282_.id(), p_427282_.name()))
         .toList();

      for (Optional<NameAndId> optional : Util.sequence(list).join()) {
         if (!optional.isEmpty()) {
            p_428696_.banListService().removeUserBan(optional.get(), p_431322_);
         }
      }

      return get(p_428696_);
   }

   public static List<BanlistService.UserBanDto> set(MinecraftApi p_422615_, List<BanlistService.UserBanDto> p_430618_, ClientInfo p_425550_) {
      List<CompletableFuture<Optional<BanlistService.UserBan>>> list = p_430618_.stream()
         .map(
            p_426835_ -> p_422615_.playerListService()
               .getUser(p_426835_.player().id(), p_426835_.player().name())
               .thenApply(p_430387_ -> p_430387_.map(p_426835_::toUserBan))
         )
         .toList();
      Set<BanlistService.UserBan> set = Util.sequence(list).join().stream().flatMap(Optional::stream).collect(Collectors.toSet());
      Set<BanlistService.UserBan> set1 = p_422615_.banListService()
         .getUserBanEntries()
         .stream()
         .filter(p_431506_ -> p_431506_.getUser() != null)
         .map(BanlistService.UserBan::from)
         .collect(Collectors.toSet());
      set1.stream().filter(p_424008_ -> !set.contains(p_424008_)).forEach(p_429677_ -> p_422615_.banListService().removeUserBan(p_429677_.player(), p_425550_));
      set.stream().filter(p_430371_ -> !set1.contains(p_430371_)).forEach(p_430786_ -> {
         p_422615_.banListService().addUserBan(p_430786_.toBanEntry(), p_425550_);
         ServerPlayer serverplayer = p_422615_.playerListService().getPlayer(p_430786_.player().id());
         if (serverplayer != null) {
            serverplayer.connection.disconnect(Component.translatable("multiplayer.disconnect.banned"));
         }
      });
      return get(p_422615_);
   }

   record UserBan(NameAndId player, @Nullable String reason, String source, Optional<Instant> expires) {
      static BanlistService.UserBan from(UserBanListEntry p_424950_) {
         return new BanlistService.UserBan(
            Objects.requireNonNull(p_424950_.getUser()),
            p_424950_.getReason(),
            p_424950_.getSource(),
            Optional.ofNullable(p_424950_.getExpires()).map(Date::toInstant)
         );
      }

      UserBanListEntry toBanEntry() {
         return new UserBanListEntry(
            new NameAndId(this.player().id(), this.player().name()), null, this.source(), this.expires().map(Date::from).orElse(null), this.reason()
         );
      }
   }

   public record UserBanDto(PlayerDto player, Optional<String> reason, Optional<String> source, Optional<Instant> expires) {
      public static final MapCodec<BanlistService.UserBanDto> CODEC = RecordCodecBuilder.mapCodec(
         p_422600_ -> p_422600_.group(
               PlayerDto.CODEC.codec().fieldOf("player").forGetter(BanlistService.UserBanDto::player),
               Codec.STRING.optionalFieldOf("reason").forGetter(BanlistService.UserBanDto::reason),
               Codec.STRING.optionalFieldOf("source").forGetter(BanlistService.UserBanDto::source),
               ExtraCodecs.INSTANT_ISO8601.optionalFieldOf("expires").forGetter(BanlistService.UserBanDto::expires)
            )
            .apply(p_422600_, BanlistService.UserBanDto::new)
      );

      private static BanlistService.UserBanDto from(BanlistService.UserBan p_423849_) {
         return new BanlistService.UserBanDto(
            PlayerDto.from(p_423849_.player()), Optional.ofNullable(p_423849_.reason()), Optional.of(p_423849_.source()), p_423849_.expires()
         );
      }

      public static BanlistService.UserBanDto from(UserBanListEntry p_428394_) {
         return from(BanlistService.UserBan.from(p_428394_));
      }

      private BanlistService.UserBan toUserBan(NameAndId p_422527_) {
         return new BanlistService.UserBan(p_422527_, this.reason().orElse(null), this.source().orElse("Management server"), this.expires());
      }
   }
}
