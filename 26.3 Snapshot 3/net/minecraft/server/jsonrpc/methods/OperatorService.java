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
   public static List<OperatorService.OperatorDto> get(final MinecraftApi minecraftApi) {
      return minecraftApi.operatorListService().getEntries().stream().filter(u -> u.getUser() != null).map(OperatorService.OperatorDto::from).toList();
   }

   public static List<OperatorService.OperatorDto> clear(final MinecraftApi minecraftApi, final ClientInfo clientInfo) {
      minecraftApi.operatorListService().clear(clientInfo);
      return get(minecraftApi);
   }

   public static List<OperatorService.OperatorDto> remove(final MinecraftApi minecraftApi, final List<PlayerDto> playerDtos, final ClientInfo clientInfo) {
      List<CompletableFuture<Optional<NameAndId>>> fetch = playerDtos.stream()
         .map(playerDto -> minecraftApi.playerListService().getUser(playerDto.id(), playerDto.name()))
         .toList();

      for (Optional<NameAndId> user : Util.sequence(fetch).join()) {
         user.ifPresent(nameAndId -> minecraftApi.operatorListService().deop(nameAndId, clientInfo));
      }

      return get(minecraftApi);
   }

   public static List<OperatorService.OperatorDto> add(
      final MinecraftApi minecraftApi, final List<OperatorService.OperatorDto> operators, final ClientInfo clientInfo
   ) {
      List<CompletableFuture<Optional<OperatorService.Op>>> fetch = operators.stream()
         .map(
            operator -> minecraftApi.playerListService()
               .getUser(operator.player().id(), operator.player().name())
               .thenApply(user -> user.map(nameAndId -> new OperatorService.Op(nameAndId, operator.permissionLevel(), operator.bypassesPlayerLimit())))
         )
         .toList();

      for (Optional<OperatorService.Op> op : Util.sequence(fetch).join()) {
         op.ifPresent(
            operator -> minecraftApi.operatorListService().op(operator.user(), operator.permissionLevel(), operator.bypassesPlayerLimit(), clientInfo)
         );
      }

      return get(minecraftApi);
   }

   public static List<OperatorService.OperatorDto> set(
      final MinecraftApi minecraftApi, final List<OperatorService.OperatorDto> operators, final ClientInfo clientInfo
   ) {
      List<CompletableFuture<Optional<OperatorService.Op>>> fetch = operators.stream()
         .map(
            operator -> minecraftApi.playerListService()
               .getUser(operator.player().id(), operator.player().name())
               .thenApply(user -> user.map(nameAndId -> new OperatorService.Op(nameAndId, operator.permissionLevel(), operator.bypassesPlayerLimit())))
         )
         .toList();
      Set<OperatorService.Op> finalOperators = Util.sequence(fetch).join().stream().flatMap(Optional::stream).collect(Collectors.toSet());
      Set<OperatorService.Op> currentOperators = minecraftApi.operatorListService()
         .getEntries()
         .stream()
         .filter(entry -> entry.getUser() != null)
         .map(entry -> new OperatorService.Op(entry.getUser(), Optional.of(entry.permissions().level()), Optional.of(entry.getBypassesPlayerLimit())))
         .collect(Collectors.toSet());
      currentOperators.stream()
         .filter(operator -> !finalOperators.contains(operator))
         .forEach(operator -> minecraftApi.operatorListService().deop(operator.user(), clientInfo));
      finalOperators.stream()
         .filter(operator -> !currentOperators.contains(operator))
         .forEach(operator -> minecraftApi.operatorListService().op(operator.user(), operator.permissionLevel(), operator.bypassesPlayerLimit(), clientInfo));
      return get(minecraftApi);
   }

   private record Op(NameAndId user, Optional<PermissionLevel> permissionLevel, Optional<Boolean> bypassesPlayerLimit) {
   }

   public record OperatorDto(PlayerDto player, Optional<PermissionLevel> permissionLevel, Optional<Boolean> bypassesPlayerLimit) {
      public static final MapCodec<OperatorService.OperatorDto> CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               PlayerDto.CODEC.codec().fieldOf("player").forGetter(OperatorService.OperatorDto::player),
               PermissionLevel.INT_CODEC.optionalFieldOf("permissionLevel").forGetter(OperatorService.OperatorDto::permissionLevel),
               Codec.BOOL.optionalFieldOf("bypassesPlayerLimit").forGetter(OperatorService.OperatorDto::bypassesPlayerLimit)
            )
            .apply(i, OperatorService.OperatorDto::new)
      );

      public static OperatorService.OperatorDto from(final ServerOpListEntry serverOpListEntry) {
         return new OperatorService.OperatorDto(
            PlayerDto.from(Objects.requireNonNull(serverOpListEntry.getUser())),
            Optional.of(serverOpListEntry.permissions().level()),
            Optional.of(serverOpListEntry.getBypassesPlayerLimit())
         );
      }
   }
}
