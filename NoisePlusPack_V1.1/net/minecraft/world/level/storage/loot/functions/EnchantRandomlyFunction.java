package net.minecraft.world.level.storage.loot.functions;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;

public class EnchantRandomlyFunction extends LootItemConditionalFunction {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final MapCodec<EnchantRandomlyFunction> CODEC = RecordCodecBuilder.mapCodec(
      p_341996_ -> commonFields(p_341996_)
         .and(
            p_341996_.group(
               RegistryCodecs.homogeneousList(Registries.ENCHANTMENT).optionalFieldOf("options").forGetter(p_341991_ -> p_341991_.options),
               Codec.BOOL.optionalFieldOf("only_compatible", true).forGetter(p_341992_ -> p_341992_.onlyCompatible)
            )
         )
         .apply(p_341996_, EnchantRandomlyFunction::new)
   );
   private final Optional<HolderSet<Enchantment>> options;
   private final boolean onlyCompatible;

   EnchantRandomlyFunction(List<LootItemCondition> p_298352_, Optional<HolderSet<Enchantment>> p_297532_, boolean p_344714_) {
      super(p_298352_);
      this.options = p_297532_;
      this.onlyCompatible = p_344714_;
   }

   @Override
   public LootItemFunctionType<EnchantRandomlyFunction> getType() {
      return LootItemFunctions.ENCHANT_RANDOMLY;
   }

   @Override
   public ItemStack run(ItemStack p_80429_, LootContext p_80430_) {
      RandomSource randomsource = p_80430_.getRandom();
      boolean flag = p_80429_.is(Items.BOOK);
      boolean flag1 = !flag && this.onlyCompatible;
      Stream<Holder<Enchantment>> stream = this.options
         .map(HolderSet::stream)
         .orElseGet(() -> p_80430_.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().map(Function.identity()))
         .filter(p_341995_ -> !flag1 || p_341995_.value().canEnchant(p_80429_));
      List<Holder<Enchantment>> list = stream.toList();
      Optional<Holder<Enchantment>> optional = Util.getRandomSafe(list, randomsource);
      if (optional.isEmpty()) {
         LOGGER.warn("Couldn't find a compatible enchantment for {}", p_80429_);
         return p_80429_;
      } else {
         return enchantItem(p_80429_, optional.get(), randomsource);
      }
   }

   private static ItemStack enchantItem(ItemStack p_230980_, Holder<Enchantment> p_343939_, RandomSource p_230982_) {
      int i = Mth.nextInt(p_230982_, p_343939_.value().getMinLevel(), p_343939_.value().getMaxLevel());
      if (p_230980_.is(Items.BOOK)) {
         p_230980_ = new ItemStack(Items.ENCHANTED_BOOK);
      }

      p_230980_.enchant(p_343939_, i);
      return p_230980_;
   }

   public static EnchantRandomlyFunction.Builder randomEnchantment() {
      return new EnchantRandomlyFunction.Builder();
   }

   public static EnchantRandomlyFunction.Builder randomApplicableEnchantment(HolderLookup.Provider p_343847_) {
      return randomEnchantment().withOneOf(p_343847_.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(EnchantmentTags.ON_RANDOM_LOOT));
   }

   public static class Builder extends LootItemConditionalFunction.Builder<EnchantRandomlyFunction.Builder> {
      private Optional<HolderSet<Enchantment>> options = Optional.empty();
      private boolean onlyCompatible = true;

      protected EnchantRandomlyFunction.Builder getThis() {
         return this;
      }

      public EnchantRandomlyFunction.Builder withEnchantment(Holder<Enchantment> p_342993_) {
         this.options = Optional.of(HolderSet.direct(p_342993_));
         return this;
      }

      public EnchantRandomlyFunction.Builder withOneOf(HolderSet<Enchantment> p_344102_) {
         this.options = Optional.of(p_344102_);
         return this;
      }

      public EnchantRandomlyFunction.Builder allowingIncompatibleEnchantments() {
         this.onlyCompatible = false;
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new EnchantRandomlyFunction(this.getConditions(), this.options, this.onlyCompatible);
      }
   }
}
