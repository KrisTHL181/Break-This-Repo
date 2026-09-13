package net.minecraft.world.entity.player;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.Nullable;

public enum PlayerModelType implements StringRepresentable {
   SLIM("slim", "slim"),
   WIDE("wide", "default");

   public static final Codec<PlayerModelType> CODEC = StringRepresentable.fromEnum(PlayerModelType::values);
   private static final Function<String, PlayerModelType> NAME_LOOKUP = StringRepresentable.createNameLookup(values(), p_423456_ -> p_423456_.legacyServicesId);
   public static final StreamCodec<ByteBuf, PlayerModelType> STREAM_CODEC = ByteBufCodecs.BOOL
      .map(p_430703_ -> p_430703_ ? SLIM : WIDE, p_425170_ -> p_425170_ == SLIM);
   private final String id;
   private final String legacyServicesId;

   PlayerModelType(final String p_425525_, final String p_428224_) {
      this.id = p_425525_;
      this.legacyServicesId = p_428224_;
   }

   public static PlayerModelType byLegacyServicesName(@Nullable String p_427693_) {
      return Objects.requireNonNullElse(NAME_LOOKUP.apply(p_427693_), WIDE);
   }

   @Override
   public String getSerializedName() {
      return this.id;
   }
}
