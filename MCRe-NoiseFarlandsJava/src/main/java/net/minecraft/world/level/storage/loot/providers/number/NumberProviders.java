package net.minecraft.world.level.storage.loot.providers.number;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public class NumberProviders {
    private static final Codec<NumberProvider> TYPED_CODEC = BuiltInRegistries.LOOT_NUMBER_PROVIDER_TYPE.byNameCodec().dispatch(NumberProvider::codec, c -> c);
    public static final Codec<NumberProvider> CODEC = Codec.lazyInitialized(
        () -> {
            Codec<NumberProvider> typedCodecWithFallback = Codec.withAlternative(TYPED_CODEC, UniformGenerator.MAP_CODEC.codec());
            return Codec.either(ConstantValue.INLINE_CODEC, typedCodecWithFallback)
                .xmap(Either::unwrap, provider -> provider instanceof ConstantValue constant ? Either.left(constant) : Either.right(provider));
        }
    );

    public static MapCodec<? extends NumberProvider> bootstrap(final Registry<MapCodec<? extends NumberProvider>> registry) {
        Registry.register(registry, "constant", ConstantValue.MAP_CODEC);
        Registry.register(registry, "uniform", UniformGenerator.MAP_CODEC);
        Registry.register(registry, "binomial", BinomialDistributionGenerator.MAP_CODEC);
        Registry.register(registry, "score", ScoreboardValue.MAP_CODEC);
        Registry.register(registry, "storage", StorageValue.MAP_CODEC);
        Registry.register(registry, "sum", Sum.MAP_CODEC);
        Registry.register(registry, "enchantment_level", EnchantmentLevelProvider.MAP_CODEC);
        return Registry.register(registry, "environment_attribute", EnvironmentAttributeValue.MAP_CODEC);
    }
}