package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.storage.loot.LootContext;
import org.jspecify.annotations.Nullable;

public class BredAnimalsTrigger extends SimpleCriterionTrigger<BredAnimalsTrigger.TriggerInstance> {
   @Override
   public Codec<BredAnimalsTrigger.TriggerInstance> codec() {
      return BredAnimalsTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_459421_, Animal p_457662_, Animal p_454604_, @Nullable AgeableMob p_458213_) {
      LootContext lootcontext = EntityPredicate.createContext(p_459421_, p_457662_);
      LootContext lootcontext1 = EntityPredicate.createContext(p_459421_, p_454604_);
      LootContext lootcontext2 = p_458213_ != null ? EntityPredicate.createContext(p_459421_, p_458213_) : null;
      this.trigger(p_459421_, p_456971_ -> p_456971_.matches(lootcontext, lootcontext1, lootcontext2));
   }

   public record TriggerInstance(
      Optional<ContextAwarePredicate> player,
      Optional<ContextAwarePredicate> parent,
      Optional<ContextAwarePredicate> partner,
      Optional<ContextAwarePredicate> child
   ) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<BredAnimalsTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_451574_ -> p_451574_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(BredAnimalsTrigger.TriggerInstance::player),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("parent").forGetter(BredAnimalsTrigger.TriggerInstance::parent),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("partner").forGetter(BredAnimalsTrigger.TriggerInstance::partner),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("child").forGetter(BredAnimalsTrigger.TriggerInstance::child)
            )
            .apply(p_451574_, BredAnimalsTrigger.TriggerInstance::new)
      );

      public static Criterion<BredAnimalsTrigger.TriggerInstance> bredAnimals() {
         return CriteriaTriggers.BRED_ANIMALS
            .createCriterion(new BredAnimalsTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public static Criterion<BredAnimalsTrigger.TriggerInstance> bredAnimals(EntityPredicate.Builder p_460877_) {
         return CriteriaTriggers.BRED_ANIMALS
            .createCriterion(
               new BredAnimalsTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(EntityPredicate.wrap(p_460877_)))
            );
      }

      public static Criterion<BredAnimalsTrigger.TriggerInstance> bredAnimals(
         Optional<EntityPredicate> p_459473_, Optional<EntityPredicate> p_451073_, Optional<EntityPredicate> p_452539_
      ) {
         return CriteriaTriggers.BRED_ANIMALS
            .createCriterion(
               new BredAnimalsTrigger.TriggerInstance(
                  Optional.empty(), EntityPredicate.wrap(p_459473_), EntityPredicate.wrap(p_451073_), EntityPredicate.wrap(p_452539_)
               )
            );
      }

      public boolean matches(LootContext p_450262_, LootContext p_450776_, @Nullable LootContext p_453024_) {
         return !this.child.isPresent() || p_453024_ != null && this.child.get().matches(p_453024_)
            ? matches(this.parent, p_450262_) && matches(this.partner, p_450776_) || matches(this.parent, p_450776_) && matches(this.partner, p_450262_)
            : false;
      }

      private static boolean matches(Optional<ContextAwarePredicate> p_458767_, LootContext p_453140_) {
         return p_458767_.isEmpty() || p_458767_.get().matches(p_453140_);
      }

      @Override
      public void validate(CriterionValidator p_455787_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_455787_);
         p_455787_.validateEntity(this.parent, "parent");
         p_455787_.validateEntity(this.partner, "partner");
         p_455787_.validateEntity(this.child, "child");
      }
   }
}
