package net.minecraft.advancements.triggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContextSource;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class FishingRodHookedTrigger extends SimpleCriterionTrigger<FishingRodHookedTrigger.TriggerInstance> {
   @Override
   public Codec<FishingRodHookedTrigger.TriggerInstance> codec() {
      return FishingRodHookedTrigger.TriggerInstance.CODEC;
   }

   public void trigger(final ServerPlayer player, final ItemStack rod, final FishingHook hook, final Collection<ItemStack> items) {
      LootContext hookedInContext = EntityPredicate.createContext(player, hook.getHookedIn() != null ? hook.getHookedIn() : hook);
      this.trigger(player, t -> t.matches(rod, hookedInContext, items));
   }

   public record TriggerInstance(
      Optional<ContextAwarePredicate> player, Optional<ItemPredicate> rod, Optional<ContextAwarePredicate> entity, Optional<ItemPredicate> item
   ) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<FishingRodHookedTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(FishingRodHookedTrigger.TriggerInstance::player),
               ItemPredicate.CODEC.optionalFieldOf("rod").forGetter(FishingRodHookedTrigger.TriggerInstance::rod),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(FishingRodHookedTrigger.TriggerInstance::entity),
               ItemPredicate.CODEC.optionalFieldOf("item").forGetter(FishingRodHookedTrigger.TriggerInstance::item)
            )
            .apply(i, FishingRodHookedTrigger.TriggerInstance::new)
      );

      public static Criterion<FishingRodHookedTrigger.TriggerInstance> fishedItem(
         final Optional<ItemPredicate> rod, final Optional<EntityPredicate> entity, final Optional<ItemPredicate> item
      ) {
         return CriteriaTriggers.FISHING_ROD_HOOKED
            .createCriterion(new FishingRodHookedTrigger.TriggerInstance(Optional.empty(), rod, EntityPredicate.wrap(entity), item));
      }

      public boolean matches(final ItemStack rod, final LootContext hookedIn, final Collection<ItemStack> items) {
         if (this.rod.isPresent() && !this.rod.get().test(rod)) {
            return false;
         }

         if (this.entity.isPresent() && !this.entity.get().matches(hookedIn)) {
            return false;
         }

         if (this.item.isPresent()) {
            boolean matched = false;
            Entity hookedInEntity = hookedIn.getOptionalParameter(LootContextParams.THIS_ENTITY);
            if (hookedInEntity instanceof ItemEntity item && this.item.get().test(item.getItem())) {
               matched = true;
            }

            for (ItemStack item : items) {
               if (this.item.get().test(item)) {
                  matched = true;
                  break;
               }
            }

            if (!matched) {
               return false;
            }
         }

         return true;
      }

      @Override
      public void validate(final ValidationContextSource validator) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(validator);
         Validatable.validate(validator.entityContext(), "entity", this.entity);
      }
   }
}
