package net.minecraft.advancements.criterion;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.HolderGetter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import org.jspecify.annotations.Nullable;

public class KilledByArrowTrigger extends SimpleCriterionTrigger<KilledByArrowTrigger.TriggerInstance> {
   @Override
   public Codec<KilledByArrowTrigger.TriggerInstance> codec() {
      return KilledByArrowTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_455331_, Collection<Entity> p_450146_, @Nullable ItemStack p_451333_) {
      List<LootContext> list = Lists.newArrayList();
      Set<EntityType<?>> set = Sets.newHashSet();

      for (Entity entity : p_450146_) {
         set.add(entity.getType());
         list.add(EntityPredicate.createContext(p_455331_, entity));
      }

      this.trigger(p_455331_, p_451449_ -> p_451449_.matches(list, set.size(), p_451333_));
   }

   public record TriggerInstance(
      Optional<ContextAwarePredicate> player, List<ContextAwarePredicate> victims, MinMaxBounds.Ints uniqueEntityTypes, Optional<ItemPredicate> firedFromWeapon
   ) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<KilledByArrowTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_454849_ -> p_454849_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(KilledByArrowTrigger.TriggerInstance::player),
               EntityPredicate.ADVANCEMENT_CODEC.listOf().optionalFieldOf("victims", List.of()).forGetter(KilledByArrowTrigger.TriggerInstance::victims),
               MinMaxBounds.Ints.CODEC
                  .optionalFieldOf("unique_entity_types", MinMaxBounds.Ints.ANY)
                  .forGetter(KilledByArrowTrigger.TriggerInstance::uniqueEntityTypes),
               ItemPredicate.CODEC.optionalFieldOf("fired_from_weapon").forGetter(KilledByArrowTrigger.TriggerInstance::firedFromWeapon)
            )
            .apply(p_454849_, KilledByArrowTrigger.TriggerInstance::new)
      );

      public static Criterion<KilledByArrowTrigger.TriggerInstance> crossbowKilled(HolderGetter<Item> p_459728_, EntityPredicate.Builder... p_451988_) {
         return CriteriaTriggers.KILLED_BY_ARROW
            .createCriterion(
               new KilledByArrowTrigger.TriggerInstance(
                  Optional.empty(),
                  EntityPredicate.wrap(p_451988_),
                  MinMaxBounds.Ints.ANY,
                  Optional.of(ItemPredicate.Builder.item().of(p_459728_, Items.CROSSBOW).build())
               )
            );
      }

      public static Criterion<KilledByArrowTrigger.TriggerInstance> crossbowKilled(HolderGetter<Item> p_458150_, MinMaxBounds.Ints p_457218_) {
         return CriteriaTriggers.KILLED_BY_ARROW
            .createCriterion(
               new KilledByArrowTrigger.TriggerInstance(
                  Optional.empty(), List.of(), p_457218_, Optional.of(ItemPredicate.Builder.item().of(p_458150_, Items.CROSSBOW).build())
               )
            );
      }

      public boolean matches(Collection<LootContext> p_454888_, int p_452473_, @Nullable ItemStack p_457922_) {
         if (!this.firedFromWeapon.isPresent() || p_457922_ != null && this.firedFromWeapon.get().test(p_457922_)) {
            if (!this.victims.isEmpty()) {
               List<LootContext> list = Lists.newArrayList(p_454888_);

               for (ContextAwarePredicate contextawarepredicate : this.victims) {
                  boolean flag = false;
                  Iterator<LootContext> iterator = list.iterator();

                  while (iterator.hasNext()) {
                     LootContext lootcontext = iterator.next();
                     if (contextawarepredicate.matches(lootcontext)) {
                        iterator.remove();
                        flag = true;
                        break;
                     }
                  }

                  if (!flag) {
                     return false;
                  }
               }
            }

            return this.uniqueEntityTypes.matches(p_452473_);
         } else {
            return false;
         }
      }

      @Override
      public void validate(CriterionValidator p_460835_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_460835_);
         p_460835_.validateEntities(this.victims, "victims");
      }
   }
}
