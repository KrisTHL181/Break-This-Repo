package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.SwingAnimationType;

public record SwingAnimation(SwingAnimationType type, int duration) {
   public static final SwingAnimation DEFAULT = new SwingAnimation(SwingAnimationType.WHACK, 6);
   public static final Codec<SwingAnimation> CODEC = RecordCodecBuilder.create(
      p_459175_ -> p_459175_.group(
            SwingAnimationType.CODEC.optionalFieldOf("type", DEFAULT.type).forGetter(SwingAnimation::type),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("duration", DEFAULT.duration).forGetter(SwingAnimation::duration)
         )
         .apply(p_459175_, SwingAnimation::new)
   );
   public static final StreamCodec<ByteBuf, SwingAnimation> STREAM_CODEC = StreamCodec.composite(
      SwingAnimationType.STREAM_CODEC, SwingAnimation::type, ByteBufCodecs.VAR_INT, SwingAnimation::duration, SwingAnimation::new
   );
}
