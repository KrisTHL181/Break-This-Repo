package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class EnchantWithLevelsFunction extends LootItemConditionalFunction {
   public static final MapCodec<EnchantWithLevelsFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> commonFields(i)
         .and(
            i.group(
               NumberProviders.DIRECT_CODEC.fieldOf("levels").forGetter(f -> f.levels),
               RegistryCodecs.homogeneousList(Registries.ENCHANTMENT).optionalFieldOf("options").forGetter(f -> f.options),
               Codec.BOOL.optionalFieldOf("include_additional_cost_component", false).forGetter(f -> f.includeAdditionalCostComponent)
            )
         )
         .apply(i, EnchantWithLevelsFunction::new)
   );
   private final NumberProvider levels;
   private final Optional<HolderSet<Enchantment>> options;
   private final boolean includeAdditionalCostComponent;

   private EnchantWithLevelsFunction(
      final List<LootItemCondition> predicates,
      final NumberProvider levels,
      final Optional<HolderSet<Enchantment>> options,
      final boolean includeAdditionalCostComponent
   ) {
      super(predicates);
      this.levels = levels;
      this.options = options;
      this.includeAdditionalCostComponent = includeAdditionalCostComponent;
   }

   @Override
   public MapCodec<EnchantWithLevelsFunction> codec() {
      return MAP_CODEC;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return this.includeAdditionalCostComponent ? Set.of(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED) : Set.of();
   }

   @Override
   public void validate(final ValidationContext context) {
      super.validate(context);
      Validatable.validate(context, "levels", this.levels);
   }

   @Override
   public ItemStack run(final ItemStack itemStack, final LootContext context) {
      RandomSource random = context.getRandom();
      RegistryAccess registryAccess = context.getLevel().registryAccess();
      int enchantmentCost = this.levels.getInt(context);
      ItemStack result = EnchantmentHelper.enchantItem(random, itemStack, enchantmentCost, registryAccess, this.options);
      if (this.includeAdditionalCostComponent
         && context.hasParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED)
         && !result.isEmpty()
         && enchantmentCost > 0) {
         result.set(DataComponents.ADDITIONAL_TRADE_COST, enchantmentCost);
      }

      return result;
   }

   public static EnchantWithLevelsFunction.Builder enchantWithLevels(final HolderGetter<Enchantment> enchantments, final NumberProvider levels) {
      return new EnchantWithLevelsFunction.Builder(levels).withOptions(enchantments.getOrThrow(EnchantmentTags.ON_RANDOM_LOOT));
   }

   public static class Builder extends LootItemConditionalFunction.Builder<EnchantWithLevelsFunction.Builder> {
      private final NumberProvider levels;
      private Optional<HolderSet<Enchantment>> options = Optional.empty();
      private boolean includeAdditionalCostComponent = false;

      public Builder(final NumberProvider levels) {
         this.levels = levels;
      }

      protected EnchantWithLevelsFunction.Builder getThis() {
         return this;
      }

      public EnchantWithLevelsFunction.Builder withOptions(final HolderSet<Enchantment> tag) {
         this.options = Optional.of(tag);
         return this;
      }

      public EnchantWithLevelsFunction.Builder withOptions(final Optional<HolderSet<Enchantment>> options) {
         this.options = options;
         return this;
      }

      public EnchantWithLevelsFunction.Builder includeAdditionalCostComponent() {
         this.includeAdditionalCostComponent = true;
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new EnchantWithLevelsFunction(this.getConditions(), this.levels, this.options, this.includeAdditionalCostComponent);
      }
   }
}
