package net.minecraft.advancements.triggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.alchemy.Potion;

public class BrewedPotionTrigger extends SimpleCriterionTrigger<BrewedPotionTrigger.TriggerInstance> {
   @Override
   public Codec<BrewedPotionTrigger.TriggerInstance> codec() {
      return BrewedPotionTrigger.TriggerInstance.CODEC;
   }

   public void trigger(final ServerPlayer player, final Holder<Potion> potion) {
      this.trigger(player, t -> t.matches(potion));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<Holder<Potion>> potion) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<BrewedPotionTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(BrewedPotionTrigger.TriggerInstance::player),
               Potion.CODEC.optionalFieldOf("potion").forGetter(BrewedPotionTrigger.TriggerInstance::potion)
            )
            .apply(i, BrewedPotionTrigger.TriggerInstance::new)
      );

      public static Criterion<BrewedPotionTrigger.TriggerInstance> brewedPotion() {
         return CriteriaTriggers.BREWED_POTION.createCriterion(new BrewedPotionTrigger.TriggerInstance(Optional.empty(), Optional.empty()));
      }

      public boolean matches(final Holder<Potion> potion) {
         return !this.potion.isPresent() || this.potion.get().equals(potion);
      }
   }
}
