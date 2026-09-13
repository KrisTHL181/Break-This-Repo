package net.minecraft.world.level.block.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryFileCodec;

public record BannerPattern(Identifier assetId, String translationKey) {
   public static final Codec<BannerPattern> DIRECT_CODEC = RecordCodecBuilder.create(
      p_449907_ -> p_449907_.group(
            Identifier.CODEC.fieldOf("asset_id").forGetter(BannerPattern::assetId),
            Codec.STRING.fieldOf("translation_key").forGetter(BannerPattern::translationKey)
         )
         .apply(p_449907_, BannerPattern::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, BannerPattern> DIRECT_STREAM_CODEC = StreamCodec.composite(
      Identifier.STREAM_CODEC, BannerPattern::assetId, ByteBufCodecs.STRING_UTF8, BannerPattern::translationKey, BannerPattern::new
   );
   public static final Codec<Holder<BannerPattern>> CODEC = RegistryFileCodec.create(Registries.BANNER_PATTERN, DIRECT_CODEC);
   public static final StreamCodec<RegistryFriendlyByteBuf, Holder<BannerPattern>> STREAM_CODEC = ByteBufCodecs.holder(
      Registries.BANNER_PATTERN, DIRECT_STREAM_CODEC
   );
}
