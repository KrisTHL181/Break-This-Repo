package net.minecraft.world.level.storage.loot.providers.number;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;

public class NumberProviders {
   public static final Codec<NumberProvider> DIRECT_CODEC = Codec.lazyInitialized(
      () -> {
         Codec<NumberProvider> typedCodecWithFallback = Codec.withAlternative(
            BuiltInRegistries.LOOT_NUMBER_PROVIDER_TYPE.byNameCodec().dispatch(NumberProvider::codec, c -> c), UniformGenerator.MAP_CODEC.codec()
         );
         return Codec.either(ConstantValue.INLINE_CODEC, typedCodecWithFallback)
            .xmap(Either::unwrap, provider -> provider instanceof ConstantValue constant ? Either.left(constant) : Either.right(provider));
      }
   );
   public static final Codec<Holder<NumberProvider>> CODEC = RegistryFileCodec.create(Registries.NUMBER_PROVIDER, DIRECT_CODEC);
   public static final ResourceKey<NumberProvider> COMPOSTABLE_LOW = createKey("compostable/low");
   public static final ResourceKey<NumberProvider> COMPOSTABLE_LOW_MEDIUM = createKey("compostable/low_medium");
   public static final ResourceKey<NumberProvider> COMPOSTABLE_MEDIUM = createKey("compostable/medium");
   public static final ResourceKey<NumberProvider> COMPOSTABLE_MEDIUM_HIGH = createKey("compostable/medium_high");
   public static final ResourceKey<NumberProvider> COMPOSTABLE_ALWAYS_ADD_ONE = createKey("compostable/always_add_one");

   private static ResourceKey<NumberProvider> createKey(final String location) {
      return ResourceKey.create(Registries.NUMBER_PROVIDER, Identifier.withDefaultNamespace(location));
   }

   public static void bootstrap(final BootstrapContext<NumberProvider> context) {
      context.register(COMPOSTABLE_LOW, compostable(30));
      context.register(COMPOSTABLE_LOW_MEDIUM, compostable(50));
      context.register(COMPOSTABLE_MEDIUM, compostable(65));
      context.register(COMPOSTABLE_MEDIUM_HIGH, compostable(85));
      context.register(COMPOSTABLE_ALWAYS_ADD_ONE, compostable(100));
   }

   private static NumberProvider compostable(final int layerIncreaseChance) {
      if (layerIncreaseChance >= 100) {
         return ConstantValue.exactly(1.0F);
      }

      NumberDispatcher.Case emptyCase = new NumberDispatcher.Case(
         new LootItemBlockStatePropertyCondition.Builder(Blocks.COMPOSTER)
            .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(ComposterBlock.LEVEL, 0))
            .build(),
         ConstantValue.exactly(1.0F)
      );
      return new NumberDispatcher(
         List.of(emptyCase),
         new WeightedListValue(
            WeightedList.<NumberProvider>builder()
               .add(ConstantValue.exactly(1.0F), layerIncreaseChance)
               .add(ConstantValue.exactly(0.0F), 100 - layerIncreaseChance)
               .build()
         )
      );
   }
}
