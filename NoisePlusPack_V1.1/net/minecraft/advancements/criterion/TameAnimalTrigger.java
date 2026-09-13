package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.storage.loot.LootContext;

public class TameAnimalTrigger extends SimpleCriterionTrigger<TameAnimalTrigger.TriggerInstance> {
   @Override
   public Codec<TameAnimalTrigger.TriggerInstance> codec() {
      return TameAnimalTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_450803_, Animal p_458406_) {
      LootContext lootcontext = EntityPredicate.createContext(p_450803_, p_458406_);
      this.trigger(p_450803_, p_458276_ -> p_458276_.matches(lootcontext));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> entity)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<TameAnimalTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_458196_ -> p_458196_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TameAnimalTrigger.TriggerInstance::player),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(TameAnimalTrigger.TriggerInstance::entity)
            )
            .apply(p_458196_, TameAnimalTrigger.TriggerInstance::new)
      );

      public static Criterion<TameAnimalTrigger.TriggerInstance> tamedAnimal() {
         return CriteriaTriggers.TAME_ANIMAL.createCriterion(new TameAnimalTrigger.TriggerInstance(Optional.empty(), Optional.empty()));
      }

      public static Criterion<TameAnimalTrigger.TriggerInstance> tamedAnimal(EntityPredicate.Builder p_456595_) {
         return CriteriaTriggers.TAME_ANIMAL
            .createCriterion(new TameAnimalTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(p_456595_))));
      }

      public boolean matches(LootContext p_457233_) {
         return this.entity.isEmpty() || this.entity.get().matches(p_457233_);
      }

      @Override
      public void validate(CriterionValidator p_458992_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_458992_);
         p_458992_.validateEntity(this.entity, "entity");
      }
   }
}
