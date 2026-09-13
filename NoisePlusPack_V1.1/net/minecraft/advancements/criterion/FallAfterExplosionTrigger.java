package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class FallAfterExplosionTrigger extends SimpleCriterionTrigger<FallAfterExplosionTrigger.TriggerInstance> {
   @Override
   public Codec<FallAfterExplosionTrigger.TriggerInstance> codec() {
      return FallAfterExplosionTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_458788_, Vec3 p_454464_, @Nullable Entity p_456430_) {
      Vec3 vec3 = p_458788_.position();
      LootContext lootcontext = p_456430_ != null ? EntityPredicate.createContext(p_458788_, p_456430_) : null;
      this.trigger(p_458788_, p_452617_ -> p_452617_.matches(p_458788_.level(), p_454464_, vec3, lootcontext));
   }

   public record TriggerInstance(
      Optional<ContextAwarePredicate> player,
      Optional<LocationPredicate> startPosition,
      Optional<DistancePredicate> distance,
      Optional<ContextAwarePredicate> cause
   ) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<FallAfterExplosionTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_453673_ -> p_453673_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(FallAfterExplosionTrigger.TriggerInstance::player),
               LocationPredicate.CODEC.optionalFieldOf("start_position").forGetter(FallAfterExplosionTrigger.TriggerInstance::startPosition),
               DistancePredicate.CODEC.optionalFieldOf("distance").forGetter(FallAfterExplosionTrigger.TriggerInstance::distance),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("cause").forGetter(FallAfterExplosionTrigger.TriggerInstance::cause)
            )
            .apply(p_453673_, FallAfterExplosionTrigger.TriggerInstance::new)
      );

      public static Criterion<FallAfterExplosionTrigger.TriggerInstance> fallAfterExplosion(DistancePredicate p_458327_, EntityPredicate.Builder p_451845_) {
         return CriteriaTriggers.FALL_AFTER_EXPLOSION
            .createCriterion(
               new FallAfterExplosionTrigger.TriggerInstance(
                  Optional.empty(), Optional.empty(), Optional.of(p_458327_), Optional.of(EntityPredicate.wrap(p_451845_))
               )
            );
      }

      @Override
      public void validate(CriterionValidator p_460417_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_460417_);
         p_460417_.validateEntity(this.cause(), "cause");
      }

      public boolean matches(ServerLevel p_450956_, Vec3 p_460029_, Vec3 p_451422_, @Nullable LootContext p_453771_) {
         if (this.startPosition.isPresent() && !this.startPosition.get().matches(p_450956_, p_460029_.x, p_460029_.y, p_460029_.z)) {
            return false;
         } else {
            return this.distance.isPresent() && !this.distance.get().matches(p_460029_.x, p_460029_.y, p_460029_.z, p_451422_.x, p_451422_.y, p_451422_.z)
               ? false
               : !this.cause.isPresent() || p_453771_ != null && this.cause.get().matches(p_453771_);
         }
      }
   }
}
