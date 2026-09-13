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
      p_428446_ -> p_428446_.group(
            UUIDUtil.STRING_CODEC.optionalFieldOf("id").forGetter(PlayerDto::id), Codec.STRING.optionalFieldOf("name").forGetter(PlayerDto::name)
         )
         .apply(p_428446_, PlayerDto::new)
   );

   public static PlayerDto from(GameProfile p_422601_) {
      return new PlayerDto(Optional.of(p_422601_.id()), Optional.of(p_422601_.name()));
   }

   public static PlayerDto from(NameAndId p_429617_) {
      return new PlayerDto(Optional.of(p_429617_.id()), Optional.of(p_429617_.name()));
   }

   public static PlayerDto from(ServerPlayer p_428320_) {
      GameProfile gameprofile = p_428320_.getGameProfile();
      return from(gameprofile);
   }
}
