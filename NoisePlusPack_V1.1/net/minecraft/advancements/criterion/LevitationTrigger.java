package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class LevitationTrigger extends SimpleCriterionTrigger<LevitationTrigger.TriggerInstance> {
   @Override
   public Codec<LevitationTrigger.TriggerInstance> codec() {
      return LevitationTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_457563_, Vec3 p_455526_, int p_453298_) {
      this.trigger(p_457563_, p_456596_ -> p_456596_.matches(p_457563_, p_455526_, p_453298_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<DistancePredicate> distance, MinMaxBounds.Ints duration)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<LevitationTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_450708_ -> p_450708_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(LevitationTrigger.TriggerInstance::player),
               DistancePredicate.CODEC.optionalFieldOf("distance").forGetter(LevitationTrigger.TriggerInstance::distance),
               MinMaxBounds.Ints.CODEC.optionalFieldOf("duration", MinMaxBounds.Ints.ANY).forGetter(LevitationTrigger.TriggerInstance::duration)
            )
            .apply(p_450708_, LevitationTrigger.TriggerInstance::new)
      );

      public static Criterion<LevitationTrigger.TriggerInstance> levitated(DistancePredicate p_453303_) {
         return CriteriaTriggers.LEVITATION
            .createCriterion(new LevitationTrigger.TriggerInstance(Optional.empty(), Optional.of(p_453303_), MinMaxBounds.Ints.ANY));
      }

      public boolean matches(ServerPlayer p_455454_, Vec3 p_453946_, int p_458152_) {
         return this.distance.isPresent()
               && !this.distance.get().matches(p_453946_.x, p_453946_.y, p_453946_.z, p_455454_.getX(), p_455454_.getY(), p_455454_.getZ())
            ? false
            : this.duration.matches(p_458152_);
      }
   }
}
