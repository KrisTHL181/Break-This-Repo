package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;

public class SpearMobsTrigger extends SimpleCriterionTrigger<SpearMobsTrigger.TriggerInstance> {
   @Override
   public Codec<SpearMobsTrigger.TriggerInstance> codec() {
      return SpearMobsTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_455400_, int p_455291_) {
      this.trigger(p_455400_, p_451311_ -> p_451311_.matches(p_455291_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<Integer> count) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<SpearMobsTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_460806_ -> p_460806_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(SpearMobsTrigger.TriggerInstance::player),
               ExtraCodecs.POSITIVE_INT.optionalFieldOf("count").forGetter(SpearMobsTrigger.TriggerInstance::count)
            )
            .apply(p_460806_, SpearMobsTrigger.TriggerInstance::new)
      );

      public static Criterion<SpearMobsTrigger.TriggerInstance> spearMobs(int p_460244_) {
         return CriteriaTriggers.SPEAR_MOBS_TRIGGER.createCriterion(new SpearMobsTrigger.TriggerInstance(Optional.empty(), Optional.of(p_460244_)));
      }

      public boolean matches(int p_461006_) {
         return this.count.isEmpty() || p_461006_ >= this.count.get();
      }
   }
}
