package net.minecraft.sounds;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryFileCodec;

public record SoundEvent(Identifier location, Optional<Float> fixedRange) {
   public static final Codec<SoundEvent> DIRECT_CODEC = RecordCodecBuilder.create(
      p_449207_ -> p_449207_.group(
            Identifier.CODEC.fieldOf("sound_id").forGetter(SoundEvent::location), Codec.FLOAT.lenientOptionalFieldOf("range").forGetter(SoundEvent::fixedRange)
         )
         .apply(p_449207_, SoundEvent::create)
   );
   public static final Codec<Holder<SoundEvent>> CODEC = RegistryFileCodec.create(Registries.SOUND_EVENT, DIRECT_CODEC);
   public static final StreamCodec<ByteBuf, SoundEvent> DIRECT_STREAM_CODEC = StreamCodec.composite(
      Identifier.STREAM_CODEC, SoundEvent::location, ByteBufCodecs.FLOAT.apply(ByteBufCodecs::optional), SoundEvent::fixedRange, SoundEvent::create
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, Holder<SoundEvent>> STREAM_CODEC = ByteBufCodecs.holder(Registries.SOUND_EVENT, DIRECT_STREAM_CODEC);

   private static SoundEvent create(Identifier p_453447_, Optional<Float> p_263346_) {
      return p_263346_.<SoundEvent>map(p_449205_ -> createFixedRangeEvent(p_453447_, p_449205_)).orElseGet(() -> createVariableRangeEvent(p_453447_));
   }

   public static SoundEvent createVariableRangeEvent(Identifier p_459796_) {
      return new SoundEvent(p_459796_, Optional.empty());
   }

   public static SoundEvent createFixedRangeEvent(Identifier p_460128_, float p_263029_) {
      return new SoundEvent(p_460128_, Optional.of(p_263029_));
   }

   public float getRange(float p_215669_) {
      return this.fixedRange.orElse(p_215669_ > 1.0F ? 16.0F * p_215669_ : 16.0F);
   }
}
