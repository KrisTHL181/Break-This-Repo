package net.minecraft.advancements.triggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.core.HolderGetter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class ShotCrossbowTrigger extends SimpleCriterionTrigger<ShotCrossbowTrigger.TriggerInstance> {
   @Override
   public Codec<ShotCrossbowTrigger.TriggerInstance> codec() {
      return ShotCrossbowTrigger.TriggerInstance.CODEC;
   }

   public void trigger(final ServerPlayer player, final ItemStack itemStack) {
      this.trigger(player, t -> t.matches(itemStack));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<ShotCrossbowTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(ShotCrossbowTrigger.TriggerInstance::player),
               ItemPredicate.CODEC.optionalFieldOf("item").forGetter(ShotCrossbowTrigger.TriggerInstance::item)
            )
            .apply(i, ShotCrossbowTrigger.TriggerInstance::new)
      );

      public static Criterion<ShotCrossbowTrigger.TriggerInstance> shotCrossbow(final Optional<ItemPredicate> item) {
         return CriteriaTriggers.SHOT_CROSSBOW.createCriterion(new ShotCrossbowTrigger.TriggerInstance(Optional.empty(), item));
      }

      public static Criterion<ShotCrossbowTrigger.TriggerInstance> shotCrossbow(final HolderGetter<Item> items, final ItemLike itemlike) {
         return CriteriaTriggers.SHOT_CROSSBOW
            .createCriterion(new ShotCrossbowTrigger.TriggerInstance(Optional.empty(), Optional.of(ItemPredicate.Builder.item().of(items, itemlike).build())));
      }

      public boolean matches(final ItemStack itemStack) {
         return this.item.isEmpty() || this.item.get().test(itemStack);
      }
   }
}
