package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LimitCount extends LootItemConditionalFunction {
   public static final MapCodec<LimitCount> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> commonFields(i).and(IntRange.CODEC.fieldOf("limit").forGetter(f -> f.limit)).apply(i, LimitCount::new)
   );
   private final IntRange limit;

   private LimitCount(final List<LootItemCondition> predicates, final IntRange limit) {
      super(predicates);
      this.limit = limit;
   }

   @Override
   public MapCodec<LimitCount> codec() {
      return MAP_CODEC;
   }

   @Override
   public void validate(final ValidationContext context) {
      super.validate(context);
      Validatable.validate(context, "limit", this.limit);
   }

   @Override
   public ItemStack run(final ItemStack itemStack, final LootContext context) {
      int newCount = this.limit.clamp(context, itemStack.getCount());
      itemStack.setCount(newCount);
      return itemStack;
   }

   public static LootItemConditionalFunction.Builder<?> limitCount(final IntRange limit) {
      return simpleBuilder(conditions -> new LimitCount(conditions, limit));
   }
}
