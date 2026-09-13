package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class FilledBucketTrigger extends SimpleCriterionTrigger<FilledBucketTrigger.TriggerInstance> {
   @Override
   public Codec<FilledBucketTrigger.TriggerInstance> codec() {
      return FilledBucketTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_458681_, ItemStack p_451108_) {
      this.trigger(p_458681_, p_454654_ -> p_454654_.matches(p_451108_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<FilledBucketTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_451721_ -> p_451721_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(FilledBucketTrigger.TriggerInstance::player),
               ItemPredicate.CODEC.optionalFieldOf("item").forGetter(FilledBucketTrigger.TriggerInstance::item)
            )
            .apply(p_451721_, FilledBucketTrigger.TriggerInstance::new)
      );

      public static Criterion<FilledBucketTrigger.TriggerInstance> filledBucket(ItemPredicate.Builder p_455447_) {
         return CriteriaTriggers.FILLED_BUCKET.createCriterion(new FilledBucketTrigger.TriggerInstance(Optional.empty(), Optional.of(p_455447_.build())));
      }

      public boolean matches(ItemStack p_455632_) {
         return !this.item.isPresent() || this.item.get().test(p_455632_);
      }
   }
}
