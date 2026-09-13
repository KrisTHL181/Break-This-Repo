package net.minecraft.advancements.triggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootTable;

public class LootTableTrigger extends SimpleCriterionTrigger<LootTableTrigger.TriggerInstance> {
   @Override
   public Codec<LootTableTrigger.TriggerInstance> codec() {
      return LootTableTrigger.TriggerInstance.CODEC;
   }

   public void trigger(final ServerPlayer player, final ResourceKey<LootTable> lootTable) {
      this.trigger(player, t -> t.matches(lootTable));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, ResourceKey<LootTable> lootTable) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<LootTableTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(LootTableTrigger.TriggerInstance::player),
               LootTable.KEY_CODEC.fieldOf("loot_table").forGetter(LootTableTrigger.TriggerInstance::lootTable)
            )
            .apply(i, LootTableTrigger.TriggerInstance::new)
      );

      public static Criterion<LootTableTrigger.TriggerInstance> lootTableUsed(final ResourceKey<LootTable> lootTable) {
         return CriteriaTriggers.GENERATE_LOOT.createCriterion(new LootTableTrigger.TriggerInstance(Optional.empty(), lootTable));
      }

      public boolean matches(final ResourceKey<LootTable> lootTable) {
         return this.lootTable == lootTable;
      }
   }
}
