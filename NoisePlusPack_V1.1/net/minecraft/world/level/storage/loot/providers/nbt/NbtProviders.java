package net.minecraft.world.level.storage.loot.providers.nbt;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public class NbtProviders {
   private static final Codec<NbtProvider> TYPED_CODEC = BuiltInRegistries.LOOT_NBT_PROVIDER_TYPE
      .byNameCodec()
      .dispatch(NbtProvider::getType, LootNbtProviderType::codec);
   public static final Codec<NbtProvider> CODEC = Codec.lazyInitialized(
      () -> Codec.either(ContextNbtProvider.INLINE_CODEC, TYPED_CODEC)
         .xmap(
            Either::unwrap, p_298663_ -> p_298663_ instanceof ContextNbtProvider contextnbtprovider ? Either.left(contextnbtprovider) : Either.right(p_298663_)
         )
   );
   public static final LootNbtProviderType STORAGE = register("storage", StorageNbtProvider.CODEC);
   public static final LootNbtProviderType CONTEXT = register("context", ContextNbtProvider.MAP_CODEC);

   private static LootNbtProviderType register(String p_165629_, MapCodec<? extends NbtProvider> p_335713_) {
      return Registry.register(BuiltInRegistries.LOOT_NBT_PROVIDER_TYPE, Identifier.withDefaultNamespace(p_165629_), new LootNbtProviderType(p_335713_));
   }
}
