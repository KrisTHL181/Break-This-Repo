package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;

public class SummonedEntityTrigger extends SimpleCriterionTrigger<SummonedEntityTrigger.TriggerInstance> {
   @Override
   public Codec<SummonedEntityTrigger.TriggerInstance> codec() {
      return SummonedEntityTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_460156_, Entity p_460990_) {
      LootContext lootcontext = EntityPredicate.createContext(p_460156_, p_460990_);
      this.trigger(p_460156_, p_453260_ -> p_453260_.matches(lootcontext));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> entity)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<SummonedEntityTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_459369_ -> p_459369_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(SummonedEntityTrigger.TriggerInstance::player),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(SummonedEntityTrigger.TriggerInstance::entity)
            )
            .apply(p_459369_, SummonedEntityTrigger.TriggerInstance::new)
      );

      public static Criterion<SummonedEntityTrigger.TriggerInstance> summonedEntity(EntityPredicate.Builder p_451443_) {
         return CriteriaTriggers.SUMMONED_ENTITY
            .createCriterion(new SummonedEntityTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(p_451443_))));
      }

      public boolean matches(LootContext p_460051_) {
         return this.entity.isEmpty() || this.entity.get().matches(p_460051_);
      }

      @Override
      public void validate(CriterionValidator p_451379_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_451379_);
         p_451379_.validateEntity(this.entity, "entity");
      }
   }
}
