package net.minecraft.server.jsonrpc.api;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

public record PlayerDto(Optional<UUID> id, Optional<String> name) {
   public static final MapCodec<PlayerDto> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(UUIDUtil.STRING_CODEC.optionalFieldOf("id").forGetter(PlayerDto::id), Codec.STRING.optionalFieldOf("name").forGetter(PlayerDto::name))
         .apply(i, PlayerDto::new)
   );

   public static PlayerDto from(final GameProfile gameProfile) {
      return new PlayerDto(Optional.of(gameProfile.id()), Optional.of(gameProfile.name()));
   }

   public static PlayerDto from(final NameAndId nameAndId) {
      return new PlayerDto(Optional.of(nameAndId.id()), Optional.of(nameAndId.name()));
   }

   public static PlayerDto from(final ServerPlayer player) {
      GameProfile gameProfile = player.getGameProfile();
      return from(gameProfile);
   }
}
