package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class FishingRodHookedTrigger extends SimpleCriterionTrigger<FishingRodHookedTrigger.TriggerInstance> {
   @Override
   public Codec<FishingRodHookedTrigger.TriggerInstance> codec() {
      return FishingRodHookedTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_455462_, ItemStack p_460553_, FishingHook p_457362_, Collection<ItemStack> p_457565_) {
      LootContext lootcontext = EntityPredicate.createContext(p_455462_, p_457362_.getHookedIn() != null ? p_457362_.getHookedIn() : p_457362_);
      this.trigger(p_455462_, p_451562_ -> p_451562_.matches(p_460553_, lootcontext, p_457565_));
   }

   public record TriggerInstance(
      Optional<ContextAwarePredicate> player, Optional<ItemPredicate> rod, Optional<ContextAwarePredicate> entity, Optional<ItemPredicate> item
   ) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<FishingRodHookedTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_460958_ -> p_460958_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(FishingRodHookedTrigger.TriggerInstance::player),
               ItemPredicate.CODEC.optionalFieldOf("rod").forGetter(FishingRodHookedTrigger.TriggerInstance::rod),
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(FishingRodHookedTrigger.TriggerInstance::entity),
               ItemPredicate.CODEC.optionalFieldOf("item").forGetter(FishingRodHookedTrigger.TriggerInstance::item)
            )
            .apply(p_460958_, FishingRodHookedTrigger.TriggerInstance::new)
      );

      public static Criterion<FishingRodHookedTrigger.TriggerInstance> fishedItem(
         Optional<ItemPredicate> p_452720_, Optional<EntityPredicate> p_451876_, Optional<ItemPredicate> p_455196_
      ) {
         return CriteriaTriggers.FISHING_ROD_HOOKED
            .createCriterion(new FishingRodHookedTrigger.TriggerInstance(Optional.empty(), p_452720_, EntityPredicate.wrap(p_451876_), p_455196_));
      }

      public boolean matches(ItemStack p_458155_, LootContext p_452048_, Collection<ItemStack> p_454538_) {
         if (this.rod.isPresent() && !this.rod.get().test(p_458155_)) {
            return false;
         }

         if (this.entity.isPresent() && !this.entity.get().matches(p_452048_)) {
            return false;
         }

         if (this.item.isPresent()) {
            boolean flag = false;
            Entity entity = p_452048_.getOptionalParameter(LootContextParams.THIS_ENTITY);
            if (entity instanceof ItemEntity itementity && this.item.get().test(itementity.getItem())) {
               flag = true;
            }

            for (ItemStack itemstack : p_454538_) {
               if (this.item.get().test(itemstack)) {
                  flag = true;
                  break;
               }
            }

            if (!flag) {
               return false;
            }
         }

         return true;
      }

      @Override
      public void validate(CriterionValidator p_459360_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_459360_);
         p_459360_.validateEntity(this.entity, "entity");
      }
   }
}
