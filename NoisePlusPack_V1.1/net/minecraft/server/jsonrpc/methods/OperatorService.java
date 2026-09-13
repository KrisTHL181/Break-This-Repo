package net.minecraft.server.jsonrpc.methods;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.util.Util;

public class OperatorService {
   public static List<OperatorService.OperatorDto> get(MinecraftApi p_424790_) {
      return p_424790_.operatorListService()
         .getEntries()
         .stream()
         .filter(p_427191_ -> p_427191_.getUser() != null)
         .map(OperatorService.OperatorDto::from)
         .toList();
   }

   public static List<OperatorService.OperatorDto> clear(MinecraftApi p_428961_, ClientInfo p_426334_) {
      p_428961_.operatorListService().clear(p_426334_);
      return get(p_428961_);
   }

   public static List<OperatorService.OperatorDto> remove(MinecraftApi p_430978_, List<PlayerDto> p_426858_, ClientInfo p_431498_) {
      List<CompletableFuture<Optional<NameAndId>>> list = p_426858_.stream()
         .map(p_426964_ -> p_430978_.playerListService().getUser(p_426964_.id(), p_426964_.name()))
         .toList();

      for (Optional<NameAndId> optional : Util.sequence(list).join()) {
         optional.ifPresent(p_422999_ -> p_430978_.operatorListService().deop(p_422999_, p_431498_));
      }

      return get(p_430978_);
   }

   public static List<OperatorService.OperatorDto> add(MinecraftApi p_423749_, List<OperatorService.OperatorDto> p_431209_, ClientInfo p_431107_) {
      List<CompletableFuture<Optional<OperatorService.Op>>> list = p_431209_.stream()
         .map(
            p_426063_ -> p_423749_.playerListService()
               .getUser(p_426063_.player().id(), p_426063_.player().name())
               .thenApply(
                  p_423563_ -> p_423563_.map(p_428805_ -> new OperatorService.Op(p_428805_, p_426063_.permissionLevel(), p_426063_.bypassesPlayerLimit()))
               )
         )
         .toList();

      for (Optional<OperatorService.Op> optional : Util.sequence(list).join()) {
         optional.ifPresent(
            p_422879_ -> p_423749_.operatorListService().op(p_422879_.user(), p_422879_.permissionLevel(), p_422879_.bypassesPlayerLimit(), p_431107_)
         );
      }

      return get(p_423749_);
   }

   public static List<OperatorService.OperatorDto> set(MinecraftApi p_422549_, List<OperatorService.OperatorDto> p_427943_, ClientInfo p_428674_) {
      List<CompletableFuture<Optional<OperatorService.Op>>> list = p_427943_.stream()
         .map(
            p_424463_ -> p_422549_.playerListService()
               .getUser(p_424463_.player().id(), p_424463_.player().name())
               .thenApply(
                  p_428723_ -> p_428723_.map(p_427312_ -> new OperatorService.Op(p_427312_, p_424463_.permissionLevel(), p_424463_.bypassesPlayerLimit()))
               )
         )
         .toList();
      Set<OperatorService.Op> set = Util.sequence(list).join().stream().flatMap(Optional::stream).collect(Collectors.toSet());
      Set<OperatorService.Op> set1 = p_422549_.operatorListService()
         .getEntries()
         .stream()
         .filter(p_427963_ -> p_427963_.getUser() != null)
         .map(
            p_449133_ -> new OperatorService.Op(
               p_449133_.getUser(), Optional.of(p_449133_.permissions().level()), Optional.of(p_449133_.getBypassesPlayerLimit())
            )
         )
         .collect(Collectors.toSet());
      set1.stream().filter(p_426958_ -> !set.contains(p_426958_)).forEach(p_424566_ -> p_422549_.operatorListService().deop(p_424566_.user(), p_428674_));
      set.stream()
         .filter(p_430704_ -> !set1.contains(p_430704_))
         .forEach(p_422555_ -> p_422549_.operatorListService().op(p_422555_.user(), p_422555_.permissionLevel(), p_422555_.bypassesPlayerLimit(), p_428674_));
      return get(p_422549_);
   }

   record Op(NameAndId user, Optional<PermissionLevel> permissionLevel, Optional<Boolean> bypassesPlayerLimit) {
   }

   public record OperatorDto(PlayerDto player, Optional<PermissionLevel> permissionLevel, Optional<Boolean> bypassesPlayerLimit) {
      public static final MapCodec<OperatorService.OperatorDto> CODEC = RecordCodecBuilder.mapCodec(
         p_449134_ -> p_449134_.group(
               PlayerDto.CODEC.codec().fieldOf("player").forGetter(OperatorService.OperatorDto::player),
               PermissionLevel.INT_CODEC.optionalFieldOf("permissionLevel").forGetter(OperatorService.OperatorDto::permissionLevel),
               Codec.BOOL.optionalFieldOf("bypassesPlayerLimit").forGetter(OperatorService.OperatorDto::bypassesPlayerLimit)
            )
            .apply(p_449134_, OperatorService.OperatorDto::new)
      );

      public static OperatorService.OperatorDto from(ServerOpListEntry p_427991_) {
         return new OperatorService.OperatorDto(
            PlayerDto.from(Objects.requireNonNull(p_427991_.getUser())),
            Optional.of(p_427991_.permissions().level()),
            Optional.of(p_427991_.getBypassesPlayerLimit())
         );
      }
   }
}
