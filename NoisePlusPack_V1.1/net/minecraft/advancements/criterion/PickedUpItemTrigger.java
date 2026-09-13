package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import org.jspecify.annotations.Nullable;

public class PickedUpItemTrigger extends SimpleCriterionTrigger<PickedUpItemTrigger.TriggerInstance> {
   @Override
   public Codec<PickedUpItemTrigger.TriggerInstance> codec() {
      return PickedUpItemTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_457730_, ItemStack p_460106_, @Nullable Entity p_455714_) {
      LootContext lootcontext = EntityPredicate.createContext(p_457730_, p_455714_);
      this.trigger(p_457730_, p_453011_ -> p_453011_.matches(p_457730_, p_460106_, lootcontext));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item, Optional<ContextAwarePredicate> entity)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<PickedUpItemTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_459014_ -> p_459014_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(PickedUpItemTrigger.TriggerInstance::player),
               ItemPredicate.CODEC.optionalFieldOf("item").forGetter(PickedUpItemTrigger.TriggerInstance::item),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(PickedUpItemTrigger.TriggerInstance::entity)
            )
            .apply(p_459014_, PickedUpItemTrigger.TriggerInstance::new)
      );

      public static Criterion<PickedUpItemTrigger.TriggerInstance> thrownItemPickedUpByEntity(
         ContextAwarePredicate p_452561_, Optional<ItemPredicate> p_456257_, Optional<ContextAwarePredicate> p_456825_
      ) {
         return CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_ENTITY
            .createCriterion(new PickedUpItemTrigger.TriggerInstance(Optional.of(p_452561_), p_456257_, p_456825_));
      }

      public static Criterion<PickedUpItemTrigger.TriggerInstance> thrownItemPickedUpByPlayer(
         Optional<ContextAwarePredicate> p_452239_, Optional<ItemPredicate> p_455804_, Optional<ContextAwarePredicate> p_455790_
      ) {
         return CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_PLAYER.createCriterion(new PickedUpItemTrigger.TriggerInstance(p_452239_, p_455804_, p_455790_));
      }

      public boolean matches(ServerPlayer p_455249_, ItemStack p_456570_, LootContext p_456936_) {
         return this.item.isPresent() && !this.item.get().test(p_456570_) ? false : !this.entity.isPresent() || this.entity.get().matches(p_456936_);
      }

      @Override
      public void validate(CriterionValidator p_459499_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_459499_);
         p_459499_.validateEntity(this.entity, "entity");
      }
   }
}
