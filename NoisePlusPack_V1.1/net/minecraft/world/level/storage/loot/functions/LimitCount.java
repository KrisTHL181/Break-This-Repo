package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LimitCount extends LootItemConditionalFunction {
   public static final MapCodec<LimitCount> CODEC = RecordCodecBuilder.mapCodec(
      p_297107_ -> commonFields(p_297107_).and(IntRange.CODEC.fieldOf("limit").forGetter(p_297106_ -> p_297106_.limiter)).apply(p_297107_, LimitCount::new)
   );
   private final IntRange limiter;

   private LimitCount(List<LootItemCondition> p_298546_, IntRange p_165214_) {
      super(p_298546_);
      this.limiter = p_165214_;
   }

   @Override
   public LootItemFunctionType<LimitCount> getType() {
      return LootItemFunctions.LIMIT_COUNT;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return this.limiter.getReferencedContextParams();
   }

   @Override
   public ItemStack run(ItemStack p_80644_, LootContext p_80645_) {
      int i = this.limiter.clamp(p_80645_, p_80644_.getCount());
      p_80644_.setCount(i);
      return p_80644_;
   }

   public static LootItemConditionalFunction.Builder<?> limitCount(IntRange p_165216_) {
      return simpleBuilder(p_297105_ -> new LimitCount(p_297105_, p_165216_));
   }
}
