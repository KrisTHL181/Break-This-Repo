package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootTable;

public class LootTableTrigger extends SimpleCriterionTrigger<LootTableTrigger.TriggerInstance> {
   @Override
   public Codec<LootTableTrigger.TriggerInstance> codec() {
      return LootTableTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_455149_, ResourceKey<LootTable> p_455581_) {
      this.trigger(p_455149_, p_454685_ -> p_454685_.matches(p_455581_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, ResourceKey<LootTable> lootTable) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<LootTableTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_455885_ -> p_455885_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(LootTableTrigger.TriggerInstance::player),
               LootTable.KEY_CODEC.fieldOf("loot_table").forGetter(LootTableTrigger.TriggerInstance::lootTable)
            )
            .apply(p_455885_, LootTableTrigger.TriggerInstance::new)
      );

      public static Criterion<LootTableTrigger.TriggerInstance> lootTableUsed(ResourceKey<LootTable> p_455346_) {
         return CriteriaTriggers.GENERATE_LOOT.createCriterion(new LootTableTrigger.TriggerInstance(Optional.empty(), p_455346_));
      }

      public boolean matches(ResourceKey<LootTable> p_453787_) {
         return this.lootTable == p_453787_;
      }
   }
}
