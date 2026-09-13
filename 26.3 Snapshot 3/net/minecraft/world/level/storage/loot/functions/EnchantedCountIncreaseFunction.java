package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class EnchantedCountIncreaseFunction extends LootItemConditionalFunction {
   public static final int NO_LIMIT = 0;
   public static final MapCodec<EnchantedCountIncreaseFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> commonFields(i)
         .and(
            i.group(
               Enchantment.CODEC.fieldOf("enchantment").forGetter(f -> f.enchantment),
               NumberProviders.DIRECT_CODEC.fieldOf("count").forGetter(f -> f.count),
               Codec.INT.optionalFieldOf("limit", 0).forGetter(f -> f.limit)
            )
         )
         .apply(i, EnchantedCountIncreaseFunction::new)
   );
   private final Holder<Enchantment> enchantment;
   private final NumberProvider count;
   private final int limit;

   private EnchantedCountIncreaseFunction(
      final List<LootItemCondition> predicates, final Holder<Enchantment> enchantment, final NumberProvider count, final int limit
   ) {
      super(predicates);
      this.enchantment = enchantment;
      this.count = count;
      this.limit = limit;
   }

   @Override
   public MapCodec<EnchantedCountIncreaseFunction> codec() {
      return MAP_CODEC;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of(LootContextParams.ATTACKING_ENTITY);
   }

   @Override
   public void validate(final ValidationContext context) {
      super.validate(context);
      Validatable.validate(context, "count", this.count);
   }

   private boolean hasLimit() {
      return this.limit > 0;
   }

   @Override
   public ItemStack run(final ItemStack itemStack, final LootContext context) {
      Entity killer = context.getOptionalParameter(LootContextParams.ATTACKING_ENTITY);
      if (killer instanceof LivingEntity entity) {
         int level = EnchantmentHelper.getEnchantmentLevel(this.enchantment, entity);
         if (level == 0) {
            return itemStack;
         }

         float addition = level * this.count.getFloat(context);
         itemStack.grow(Math.round(addition));
         if (this.hasLimit()) {
            itemStack.limitSize(this.limit);
         }
      }

      return itemStack;
   }

   public static EnchantedCountIncreaseFunction.Builder lootingMultiplier(final HolderGetter<Enchantment> enchantments, final NumberProvider count) {
      return new EnchantedCountIncreaseFunction.Builder(enchantments.getOrThrow(Enchantments.LOOTING), count);
   }

   public static class Builder extends LootItemConditionalFunction.Builder<EnchantedCountIncreaseFunction.Builder> {
      private final Holder<Enchantment> enchantment;
      private final NumberProvider count;
      private int limit = 0;

      public Builder(final Holder<Enchantment> enchantment, final NumberProvider count) {
         this.enchantment = enchantment;
         this.count = count;
      }

      protected EnchantedCountIncreaseFunction.Builder getThis() {
         return this;
      }

      public EnchantedCountIncreaseFunction.Builder setLimit(final int limit) {
         this.limit = limit;
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new EnchantedCountIncreaseFunction(this.getConditions(), this.enchantment, this.count, this.limit);
      }
   }
}
