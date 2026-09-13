package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootContext;

public record TimeCheck(Optional<Long> period, IntRange value) implements LootItemCondition {
   public static final MapCodec<TimeCheck> CODEC = RecordCodecBuilder.mapCodec(
      p_327655_ -> p_327655_.group(
            Codec.LONG.optionalFieldOf("period").forGetter(TimeCheck::period), IntRange.CODEC.fieldOf("value").forGetter(TimeCheck::value)
         )
         .apply(p_327655_, TimeCheck::new)
   );

   @Override
   public LootItemConditionType getType() {
      return LootItemConditions.TIME_CHECK;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return this.value.getReferencedContextParams();
   }

   public boolean test(LootContext p_82033_) {
      ServerLevel serverlevel = p_82033_.getLevel();
      long i = serverlevel.getDayTime();
      if (this.period.isPresent()) {
         i %= this.period.get();
      }

      return this.value.test(p_82033_, (int)i);
   }

   public static TimeCheck.Builder time(IntRange p_165510_) {
      return new TimeCheck.Builder(p_165510_);
   }

   public static class Builder implements LootItemCondition.Builder {
      private Optional<Long> period = Optional.empty();
      private final IntRange value;

      public Builder(IntRange p_165515_) {
         this.value = p_165515_;
      }

      public TimeCheck.Builder setPeriod(long p_165517_) {
         this.period = Optional.of(p_165517_);
         return this;
      }

      public TimeCheck build() {
         return new TimeCheck(this.period, this.value);
      }
   }
}
