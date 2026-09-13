package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import org.jspecify.annotations.Nullable;

public class EffectsChangedTrigger extends SimpleCriterionTrigger<EffectsChangedTrigger.TriggerInstance> {
   @Override
   public Codec<EffectsChangedTrigger.TriggerInstance> codec() {
      return EffectsChangedTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_453407_, @Nullable Entity p_456999_) {
      LootContext lootcontext = p_456999_ != null ? EntityPredicate.createContext(p_453407_, p_456999_) : null;
      this.trigger(p_453407_, p_456347_ -> p_456347_.matches(p_453407_, lootcontext));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<MobEffectsPredicate> effects, Optional<ContextAwarePredicate> source)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<EffectsChangedTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_451849_ -> p_451849_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(EffectsChangedTrigger.TriggerInstance::player),
               MobEffectsPredicate.CODEC.optionalFieldOf("effects").forGetter(EffectsChangedTrigger.TriggerInstance::effects),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("source").forGetter(EffectsChangedTrigger.TriggerInstance::source)
            )
            .apply(p_451849_, EffectsChangedTrigger.TriggerInstance::new)
      );

      public static Criterion<EffectsChangedTrigger.TriggerInstance> hasEffects(MobEffectsPredicate.Builder p_460395_) {
         return CriteriaTriggers.EFFECTS_CHANGED
            .createCriterion(new EffectsChangedTrigger.TriggerInstance(Optional.empty(), p_460395_.build(), Optional.empty()));
      }

      public static Criterion<EffectsChangedTrigger.TriggerInstance> gotEffectsFrom(EntityPredicate.Builder p_455295_) {
         return CriteriaTriggers.EFFECTS_CHANGED
            .createCriterion(
               new EffectsChangedTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.of(EntityPredicate.wrap(p_455295_.build())))
            );
      }

      public boolean matches(ServerPlayer p_451315_, @Nullable LootContext p_455126_) {
         return this.effects.isPresent() && !this.effects.get().matches(p_451315_)
            ? false
            : !this.source.isPresent() || p_455126_ != null && this.source.get().matches(p_455126_);
      }

      @Override
      public void validate(CriterionValidator p_455396_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_455396_);
         p_455396_.validateEntity(this.source, "source");
      }
   }
}
