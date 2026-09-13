package net.minecraft.world.level.storage.loot;

import com.mojang.serialization.Codec;
import java.util.stream.Stream;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public record LootDataType<T>(ResourceKey<Registry<T>> registryKey, Codec<T> codec, LootDataType.Validator<T> validator) {
   public static final LootDataType<LootItemCondition> PREDICATE = new LootDataType<>(
      Registries.PREDICATE, LootItemCondition.DIRECT_CODEC, createSimpleValidator()
   );
   public static final LootDataType<LootItemFunction> MODIFIER = new LootDataType<>(
      Registries.ITEM_MODIFIER, LootItemFunctions.ROOT_CODEC, createSimpleValidator()
   );
   public static final LootDataType<LootTable> TABLE = new LootDataType<>(Registries.LOOT_TABLE, LootTable.DIRECT_CODEC, createLootTableValidator());

   public void runValidation(ValidationContext p_279366_, ResourceKey<T> p_329223_, T p_279124_) {
      this.validator.run(p_279366_, p_329223_, p_279124_);
   }

   public static Stream<LootDataType<?>> values() {
      return Stream.of(PREDICATE, MODIFIER, TABLE);
   }

   private static <T extends LootContextUser> LootDataType.Validator<T> createSimpleValidator() {
      return (p_405778_, p_405779_, p_405780_) -> p_405780_.validate(p_405778_.enterElement(new ProblemReporter.RootElementPathElement(p_405779_), p_405779_));
   }

   private static LootDataType.Validator<LootTable> createLootTableValidator() {
      return (p_405781_, p_405782_, p_405783_) -> p_405783_.validate(
         p_405781_.setContextKeySet(p_405783_.getParamSet()).enterElement(new ProblemReporter.RootElementPathElement(p_405782_), p_405782_)
      );
   }

   @FunctionalInterface
   public interface Validator<T> {
      void run(ValidationContext var1, ResourceKey<T> var2, T var3);
   }
}
