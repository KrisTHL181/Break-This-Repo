package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class SetEnchantmentsFunction extends LootItemConditionalFunction {
   public static final MapCodec<SetEnchantmentsFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> commonFields(i)
         .and(
            i.group(
               Codec.unboundedMap(Enchantment.CODEC, NumberProviders.DIRECT_CODEC).optionalFieldOf("enchantments", Map.of()).forGetter(f -> f.enchantments),
               Codec.BOOL.optionalFieldOf("add", false).forGetter(f -> f.add)
            )
         )
         .apply(i, SetEnchantmentsFunction::new)
   );
   private final Map<Holder<Enchantment>, NumberProvider> enchantments;
   private final boolean add;

   private SetEnchantmentsFunction(final List<LootItemCondition> predicates, final Map<Holder<Enchantment>, NumberProvider> enchantments, final boolean add) {
      super(predicates);
      this.enchantments = Map.copyOf(enchantments);
      this.add = add;
   }

   @Override
   public MapCodec<SetEnchantmentsFunction> codec() {
      return MAP_CODEC;
   }

   @Override
   public void validate(final ValidationContext context) {
      super.validate(context);
      this.enchantments.forEach((enchantment, value) -> value.validate(context.forMapField("enchantments", enchantment.getRegisteredName())));
   }

   @Override
   public ItemStack run(ItemStack itemStack, final LootContext context) {
      if (itemStack.is(Items.BOOK)) {
         itemStack = itemStack.transmuteCopy(Items.ENCHANTED_BOOK);
      }

      EnchantmentHelper.updateEnchantments(
         itemStack,
         enchantments -> {
            if (this.add) {
               this.enchantments
                  .forEach(
                     (enchantment, levelProvider) -> enchantments.set(
                        (Holder<Enchantment>)enchantment,
                        Mth.clamp(enchantments.getLevel((Holder<Enchantment>)enchantment) + levelProvider.getInt(context), 0, 255)
                     )
                  );
            } else {
               this.enchantments
                  .forEach((enchantment, levelProvider) -> enchantments.set((Holder<Enchantment>)enchantment, Mth.clamp(levelProvider.getInt(context), 0, 255)));
            }
         }
      );
      return itemStack;
   }

   public static class Builder extends LootItemConditionalFunction.Builder<SetEnchantmentsFunction.Builder> {
      private final com.google.common.collect.ImmutableMap.Builder<Holder<Enchantment>, NumberProvider> enchantments = ImmutableMap.builder();
      private final boolean add;

      public Builder() {
         this(false);
      }

      public Builder(final boolean add) {
         this.add = add;
      }

      protected SetEnchantmentsFunction.Builder getThis() {
         return this;
      }

      public SetEnchantmentsFunction.Builder withEnchantment(final Holder<Enchantment> enchantment, final NumberProvider levelProvider) {
         this.enchantments.put(enchantment, levelProvider);
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new SetEnchantmentsFunction(this.getConditions(), this.enchantments.build(), this.add);
      }
   }
}
