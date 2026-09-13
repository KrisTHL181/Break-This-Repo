package net.minecraft.advancements.triggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContextSource;

public class PlayerInteractTrigger extends SimpleCriterionTrigger<PlayerInteractTrigger.TriggerInstance> {
   @Override
   public Codec<PlayerInteractTrigger.TriggerInstance> codec() {
      return PlayerInteractTrigger.TriggerInstance.CODEC;
   }

   public void trigger(final ServerPlayer player, final ItemStack itemStack, final Entity interactedWith) {
      LootContext context = EntityPredicate.createContext(player, interactedWith);
      this.trigger(player, t -> t.matches(itemStack, context));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item, Optional<ContextAwarePredicate> entity)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<PlayerInteractTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(PlayerInteractTrigger.TriggerInstance::player),
               ItemPredicate.CODEC.optionalFieldOf("item").forGetter(PlayerInteractTrigger.TriggerInstance::item),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(PlayerInteractTrigger.TriggerInstance::entity)
            )
            .apply(i, PlayerInteractTrigger.TriggerInstance::new)
      );

      public static Criterion<PlayerInteractTrigger.TriggerInstance> itemUsedOnEntity(
         final Optional<ContextAwarePredicate> player, final ItemPredicate.Builder item, final Optional<ContextAwarePredicate> entity
      ) {
         return CriteriaTriggers.PLAYER_INTERACTED_WITH_ENTITY
            .createCriterion(new PlayerInteractTrigger.TriggerInstance(player, Optional.of(item.build()), entity));
      }

      public static Criterion<PlayerInteractTrigger.TriggerInstance> equipmentSheared(
         final Optional<ContextAwarePredicate> player, final ItemPredicate.Builder item, final Optional<ContextAwarePredicate> entity
      ) {
         return CriteriaTriggers.PLAYER_SHEARED_EQUIPMENT.createCriterion(new PlayerInteractTrigger.TriggerInstance(player, Optional.of(item.build()), entity));
      }

      public static Criterion<PlayerInteractTrigger.TriggerInstance> equipmentSheared(
         final ItemPredicate.Builder item, final Optional<ContextAwarePredicate> entity
      ) {
         return CriteriaTriggers.PLAYER_SHEARED_EQUIPMENT
            .createCriterion(new PlayerInteractTrigger.TriggerInstance(Optional.empty(), Optional.of(item.build()), entity));
      }

      public static Criterion<PlayerInteractTrigger.TriggerInstance> itemUsedOnEntity(
         final ItemPredicate.Builder item, final Optional<ContextAwarePredicate> entity
      ) {
         return itemUsedOnEntity(Optional.empty(), item, entity);
      }

      public boolean matches(final ItemStack itemStack, final LootContext interactedWith) {
         return this.item.isPresent() && !this.item.get().test(itemStack) ? false : this.entity.isEmpty() || this.entity.get().matches(interactedWith);
      }

      @Override
      public void validate(final ValidationContextSource validator) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(validator);
         Validatable.validate(validator.entityContext(), "entity", this.entity);
      }
   }
}
