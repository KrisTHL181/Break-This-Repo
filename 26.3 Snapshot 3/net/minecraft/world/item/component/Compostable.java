package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

public record Compostable(ResourceKey<NumberProvider> layers) {
   public static final Codec<Compostable> CODEC = RecordCodecBuilder.create(
      i -> i.group(ResourceKey.codec(Registries.NUMBER_PROVIDER).fieldOf("layers").forGetter(Compostable::layers)).apply(i, Compostable::new)
   );
   public static final StreamCodec<ByteBuf, Compostable> STREAM_CODEC = StreamCodec.composite(
      ResourceKey.streamCodec(Registries.NUMBER_PROVIDER), Compostable::layers, Compostable::new
   );
}
