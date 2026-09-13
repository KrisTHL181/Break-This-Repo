package net.minecraft.world.entity.decoration.painting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.util.ExtraCodecs;

public record PaintingVariant(int width, int height, Identifier assetId, Optional<Component> title, Optional<Component> author) {
   public static final Codec<PaintingVariant> DIRECT_CODEC = RecordCodecBuilder.create(
      p_460916_ -> p_460916_.group(
            ExtraCodecs.intRange(1, 16).fieldOf("width").forGetter(PaintingVariant::width),
            ExtraCodecs.intRange(1, 16).fieldOf("height").forGetter(PaintingVariant::height),
            Identifier.CODEC.fieldOf("asset_id").forGetter(PaintingVariant::assetId),
            ComponentSerialization.CODEC.optionalFieldOf("title").forGetter(PaintingVariant::title),
            ComponentSerialization.CODEC.optionalFieldOf("author").forGetter(PaintingVariant::author)
         )
         .apply(p_460916_, PaintingVariant::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, PaintingVariant> DIRECT_STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      PaintingVariant::width,
      ByteBufCodecs.VAR_INT,
      PaintingVariant::height,
      Identifier.STREAM_CODEC,
      PaintingVariant::assetId,
      ComponentSerialization.TRUSTED_OPTIONAL_STREAM_CODEC,
      PaintingVariant::title,
      ComponentSerialization.TRUSTED_OPTIONAL_STREAM_CODEC,
      PaintingVariant::author,
      PaintingVariant::new
   );
   public static final Codec<Holder<PaintingVariant>> CODEC = RegistryFixedCodec.create(Registries.PAINTING_VARIANT);
   public static final StreamCodec<RegistryFriendlyByteBuf, Holder<PaintingVariant>> STREAM_CODEC = ByteBufCodecs.holder(
      Registries.PAINTING_VARIANT, DIRECT_STREAM_CODEC
   );

   public int area() {
      return this.width() * this.height();
   }
}
