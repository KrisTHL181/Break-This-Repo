package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public class UsedEnderEyeTrigger extends SimpleCriterionTrigger<UsedEnderEyeTrigger.TriggerInstance> {
   @Override
   public Codec<UsedEnderEyeTrigger.TriggerInstance> codec() {
      return UsedEnderEyeTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_454644_, BlockPos p_453325_) {
      double d0 = p_454644_.getX() - p_453325_.getX();
      double d1 = p_454644_.getZ() - p_453325_.getZ();
      double d2 = d0 * d0 + d1 * d1;
      this.trigger(p_454644_, p_461078_ -> p_461078_.matches(d2));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, MinMaxBounds.Doubles distance) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<UsedEnderEyeTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_452082_ -> p_452082_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(UsedEnderEyeTrigger.TriggerInstance::player),
               MinMaxBounds.Doubles.CODEC.optionalFieldOf("distance", MinMaxBounds.Doubles.ANY).forGetter(UsedEnderEyeTrigger.TriggerInstance::distance)
            )
            .apply(p_452082_, UsedEnderEyeTrigger.TriggerInstance::new)
      );

      public boolean matches(double p_459280_) {
         return this.distance.matchesSqr(p_459280_);
      }
   }
}
