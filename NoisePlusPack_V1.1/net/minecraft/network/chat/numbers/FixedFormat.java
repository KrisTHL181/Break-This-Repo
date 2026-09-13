package net.minecraft.network.chat.numbers;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.StreamCodec;

public record FixedFormat(Component value) implements NumberFormat {
   public static final NumberFormatType<FixedFormat> TYPE = new NumberFormatType<FixedFormat>() {
      private static final MapCodec<FixedFormat> CODEC = ComponentSerialization.CODEC.fieldOf("value").xmap(FixedFormat::new, FixedFormat::value);
      private static final StreamCodec<RegistryFriendlyByteBuf, FixedFormat> STREAM_CODEC = StreamCodec.composite(
         ComponentSerialization.TRUSTED_STREAM_CODEC, FixedFormat::value, FixedFormat::new
      );

      @Override
      public MapCodec<FixedFormat> mapCodec() {
         return CODEC;
      }

      @Override
      public StreamCodec<RegistryFriendlyByteBuf, FixedFormat> streamCodec() {
         return STREAM_CODEC;
      }
   };

   @Override
   public MutableComponent format(int p_311204_) {
      return this.value.copy();
   }

   @Override
   public NumberFormatType<FixedFormat> type() {
      return TYPE;
   }
}
