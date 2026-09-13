package net.minecraft.world.level.storage.loot.providers.nbt;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public class NbtProviders {
   private static final Codec<NbtProvider> TYPED_CODEC = BuiltInRegistries.LOOT_NBT_PROVIDER_TYPE.byNameCodec().dispatch(NbtProvider::codec, c -> c);
   public static final Codec<NbtProvider> CODEC = Codec.lazyInitialized(
      () -> Codec.either(ContextNbtProvider.INLINE_CODEC, TYPED_CODEC)
         .xmap(Either::unwrap, provider -> provider instanceof ContextNbtProvider context ? Either.left(context) : Either.right(provider))
   );

   public static MapCodec<? extends NbtProvider> bootstrap(final Registry<MapCodec<? extends NbtProvider>> registry) {
      Registry.register(registry, "storage", StorageNbtProvider.MAP_CODEC);
      return Registry.register(registry, "context", ContextNbtProvider.MAP_CODEC);
   }
}
