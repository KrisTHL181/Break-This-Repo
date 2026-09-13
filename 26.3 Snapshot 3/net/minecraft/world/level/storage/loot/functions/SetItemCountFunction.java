package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class SetItemCountFunction extends LootItemConditionalFunction {
   public static final MapCodec<SetItemCountFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> commonFields(i)
         .and(i.group(NumberProviders.DIRECT_CODEC.fieldOf("count").forGetter(f -> f.count), Codec.BOOL.optionalFieldOf("add", false).forGetter(f -> f.add)))
         .apply(i, SetItemCountFunction::new)
   );
   private final NumberProvider count;
   private final boolean add;

   private SetItemCountFunction(final List<LootItemCondition> predicates, final NumberProvider count, final boolean add) {
      super(predicates);
      this.count = count;
      this.add = add;
   }

   @Override
   public MapCodec<SetItemCountFunction> codec() {
      return MAP_CODEC;
   }

   @Override
   public void validate(final ValidationContext context) {
      super.validate(context);
      Validatable.validate(context, "count", this.count);
   }

   @Override
   public ItemStack run(final ItemStack itemStack, final LootContext context) {
      int base = this.add ? itemStack.getCount() : 0;
      itemStack.setCount(base + this.count.getInt(context));
      return itemStack;
   }

   public static LootItemConditionalFunction.Builder<?> setCount(final NumberProvider count) {
      return simpleBuilder(conditions -> new SetItemCountFunction(conditions, count, false));
   }

   public static LootItemConditionalFunction.Builder<?> setCount(final NumberProvider count, final boolean add) {
      return simpleBuilder(conditions -> new SetItemCountFunction(conditions, count, add));
   }
}
