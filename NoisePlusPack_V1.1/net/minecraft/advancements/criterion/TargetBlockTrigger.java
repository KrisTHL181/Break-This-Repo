package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.Vec3;

public class TargetBlockTrigger extends SimpleCriterionTrigger<TargetBlockTrigger.TriggerInstance> {
   @Override
   public Codec<TargetBlockTrigger.TriggerInstance> codec() {
      return TargetBlockTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_458722_, Entity p_454833_, Vec3 p_456776_, int p_456285_) {
      LootContext lootcontext = EntityPredicate.createContext(p_458722_, p_454833_);
      this.trigger(p_458722_, p_451150_ -> p_451150_.matches(lootcontext, p_456776_, p_456285_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, MinMaxBounds.Ints signalStrength, Optional<ContextAwarePredicate> projectile)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<TargetBlockTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_451914_ -> p_451914_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TargetBlockTrigger.TriggerInstance::player),
               MinMaxBounds.Ints.CODEC.optionalFieldOf("signal_strength", MinMaxBounds.Ints.ANY).forGetter(TargetBlockTrigger.TriggerInstance::signalStrength),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("projectile").forGetter(TargetBlockTrigger.TriggerInstance::projectile)
            )
            .apply(p_451914_, TargetBlockTrigger.TriggerInstance::new)
      );

      public static Criterion<TargetBlockTrigger.TriggerInstance> targetHit(MinMaxBounds.Ints p_457199_, Optional<ContextAwarePredicate> p_457540_) {
         return CriteriaTriggers.TARGET_BLOCK_HIT.createCriterion(new TargetBlockTrigger.TriggerInstance(Optional.empty(), p_457199_, p_457540_));
      }

      public boolean matches(LootContext p_458035_, Vec3 p_452805_, int p_457712_) {
         return !this.signalStrength.matches(p_457712_) ? false : !this.projectile.isPresent() || this.projectile.get().matches(p_458035_);
      }

      @Override
      public void validate(CriterionValidator p_455225_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_455225_);
         p_455225_.validateEntity(this.projectile, "projectile");
      }
   }
}
