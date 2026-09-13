package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class EnchantWithLevelsFunction extends LootItemConditionalFunction {
   public static final MapCodec<EnchantWithLevelsFunction> CODEC = RecordCodecBuilder.mapCodec(
      p_341997_ -> commonFields(p_341997_)
         .and(
            p_341997_.group(
               NumberProviders.CODEC.fieldOf("levels").forGetter(p_298991_ -> p_298991_.levels),
               RegistryCodecs.homogeneousList(Registries.ENCHANTMENT).optionalFieldOf("options").forGetter(p_341998_ -> p_341998_.options)
            )
         )
         .apply(p_341997_, EnchantWithLevelsFunction::new)
   );
   private final NumberProvider levels;
   private final Optional<HolderSet<Enchantment>> options;

   EnchantWithLevelsFunction(List<LootItemCondition> p_300816_, NumberProvider p_165194_, Optional<HolderSet<Enchantment>> p_342551_) {
      super(p_300816_);
      this.levels = p_165194_;
      this.options = p_342551_;
   }

   @Override
   public LootItemFunctionType<EnchantWithLevelsFunction> getType() {
      return LootItemFunctions.ENCHANT_WITH_LEVELS;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return this.levels.getReferencedContextParams();
   }

   @Override
   public ItemStack run(ItemStack p_80483_, LootContext p_80484_) {
      RandomSource randomsource = p_80484_.getRandom();
      RegistryAccess registryaccess = p_80484_.getLevel().registryAccess();
      return EnchantmentHelper.enchantItem(randomsource, p_80483_, this.levels.getInt(p_80484_), registryaccess, this.options);
   }

   public static EnchantWithLevelsFunction.Builder enchantWithLevels(HolderLookup.Provider p_342807_, NumberProvider p_165197_) {
      return new EnchantWithLevelsFunction.Builder(p_165197_)
         .fromOptions(p_342807_.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(EnchantmentTags.ON_RANDOM_LOOT));
   }

   public static class Builder extends LootItemConditionalFunction.Builder<EnchantWithLevelsFunction.Builder> {
      private final NumberProvider levels;
      private Optional<HolderSet<Enchantment>> options = Optional.empty();

      public Builder(NumberProvider p_165200_) {
         this.levels = p_165200_;
      }

      protected EnchantWithLevelsFunction.Builder getThis() {
         return this;
      }

      public EnchantWithLevelsFunction.Builder fromOptions(HolderSet<Enchantment> p_343865_) {
         this.options = Optional.of(p_343865_);
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new EnchantWithLevelsFunction(this.getConditions(), this.levels, this.options);
      }
   }
}
