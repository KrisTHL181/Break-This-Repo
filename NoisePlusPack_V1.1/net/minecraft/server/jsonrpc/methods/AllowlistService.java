package net.minecraft.server.jsonrpc.methods;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.StoredUserEntry;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.util.Util;

public class AllowlistService {
   public static List<PlayerDto> get(MinecraftApi p_425475_) {
      return p_425475_.allowListService()
         .getEntries()
         .stream()
         .filter(p_428452_ -> p_428452_.getUser() != null)
         .map(p_426478_ -> PlayerDto.from(p_426478_.getUser()))
         .toList();
   }

   public static List<PlayerDto> add(MinecraftApi p_428263_, List<PlayerDto> p_424131_, ClientInfo p_425388_) {
      List<CompletableFuture<Optional<NameAndId>>> list = p_424131_.stream()
         .map(p_427140_ -> p_428263_.playerListService().getUser(p_427140_.id(), p_427140_.name()))
         .toList();

      for (Optional<NameAndId> optional : Util.sequence(list).join()) {
         optional.ifPresent(p_429557_ -> p_428263_.allowListService().add(new UserWhiteListEntry(p_429557_), p_425388_));
      }

      return get(p_428263_);
   }

   public static List<PlayerDto> clear(MinecraftApi p_426057_, ClientInfo p_424778_) {
      p_426057_.allowListService().clear(p_424778_);
      return get(p_426057_);
   }

   public static List<PlayerDto> remove(MinecraftApi p_427599_, List<PlayerDto> p_424061_, ClientInfo p_422763_) {
      List<CompletableFuture<Optional<NameAndId>>> list = p_424061_.stream()
         .map(p_424930_ -> p_427599_.playerListService().getUser(p_424930_.id(), p_424930_.name()))
         .toList();

      for (Optional<NameAndId> optional : Util.sequence(list).join()) {
         optional.ifPresent(p_427892_ -> p_427599_.allowListService().remove(p_427892_, p_422763_));
      }

      p_427599_.allowListService().kickUnlistedPlayers(p_422763_);
      return get(p_427599_);
   }

   public static List<PlayerDto> set(MinecraftApi p_427686_, List<PlayerDto> p_428218_, ClientInfo p_430443_) {
      List<CompletableFuture<Optional<NameAndId>>> list = p_428218_.stream()
         .map(p_428589_ -> p_427686_.playerListService().getUser(p_428589_.id(), p_428589_.name()))
         .toList();
      Set<NameAndId> set = Util.sequence(list).join().stream().flatMap(Optional::stream).collect(Collectors.toSet());
      Set<NameAndId> set1 = p_427686_.allowListService().getEntries().stream().map(StoredUserEntry::getUser).collect(Collectors.toSet());
      set1.stream().filter(p_424178_ -> !set.contains(p_424178_)).forEach(p_427298_ -> p_427686_.allowListService().remove(p_427298_, p_430443_));
      set.stream()
         .filter(p_429556_ -> !set1.contains(p_429556_))
         .forEach(p_422687_ -> p_427686_.allowListService().add(new UserWhiteListEntry(p_422687_), p_430443_));
      p_427686_.allowListService().kickUnlistedPlayers(p_430443_);
      return get(p_427686_);
   }
}
