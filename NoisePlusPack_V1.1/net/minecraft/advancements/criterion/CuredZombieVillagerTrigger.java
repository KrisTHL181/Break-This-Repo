package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.storage.loot.LootContext;

public class CuredZombieVillagerTrigger extends SimpleCriterionTrigger<CuredZombieVillagerTrigger.TriggerInstance> {
   @Override
   public Codec<CuredZombieVillagerTrigger.TriggerInstance> codec() {
      return CuredZombieVillagerTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_460727_, Zombie p_455915_, Villager p_459647_) {
      LootContext lootcontext = EntityPredicate.createContext(p_460727_, p_455915_);
      LootContext lootcontext1 = EntityPredicate.createContext(p_460727_, p_459647_);
      this.trigger(p_460727_, p_456336_ -> p_456336_.matches(lootcontext, lootcontext1));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> zombie, Optional<ContextAwarePredicate> villager)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<CuredZombieVillagerTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_453853_ -> p_453853_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(CuredZombieVillagerTrigger.TriggerInstance::player),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("zombie").forGetter(CuredZombieVillagerTrigger.TriggerInstance::zombie),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("villager").forGetter(CuredZombieVillagerTrigger.TriggerInstance::villager)
            )
            .apply(p_453853_, CuredZombieVillagerTrigger.TriggerInstance::new)
      );

      public static Criterion<CuredZombieVillagerTrigger.TriggerInstance> curedZombieVillager() {
         return CriteriaTriggers.CURED_ZOMBIE_VILLAGER
            .createCriterion(new CuredZombieVillagerTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public boolean matches(LootContext p_456012_, LootContext p_450145_) {
         return this.zombie.isPresent() && !this.zombie.get().matches(p_456012_) ? false : !this.villager.isPresent() || this.villager.get().matches(p_450145_);
      }

      @Override
      public void validate(CriterionValidator p_450208_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_450208_);
         p_450208_.validateEntity(this.zombie, "zombie");
         p_450208_.validateEntity(this.villager, "villager");
      }
   }
}
