package net.minecraft.advancements.triggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BeeNestDestroyedTrigger extends SimpleCriterionTrigger<BeeNestDestroyedTrigger.TriggerInstance> {
   @Override
   public Codec<BeeNestDestroyedTrigger.TriggerInstance> codec() {
      return BeeNestDestroyedTrigger.TriggerInstance.CODEC;
   }

   public void trigger(final ServerPlayer player, final BlockState state, final ItemStack itemStack, final int numBeesInside) {
      this.trigger(player, t -> t.matches(state, itemStack, numBeesInside));
   }

   public record TriggerInstance(
      Optional<ContextAwarePredicate> player, Optional<Holder<Block>> block, Optional<ItemPredicate> item, MinMaxBounds.Ints beesInside
   ) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<BeeNestDestroyedTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(BeeNestDestroyedTrigger.TriggerInstance::player),
               BuiltInRegistries.BLOCK.holderByNameCodec().optionalFieldOf("block").forGetter(BeeNestDestroyedTrigger.TriggerInstance::block),
               ItemPredicate.CODEC.optionalFieldOf("item").forGetter(BeeNestDestroyedTrigger.TriggerInstance::item),
               MinMaxBounds.Ints.CODEC.optionalFieldOf("num_bees_inside", MinMaxBounds.Ints.ANY).forGetter(BeeNestDestroyedTrigger.TriggerInstance::beesInside)
            )
            .apply(i, BeeNestDestroyedTrigger.TriggerInstance::new)
      );

      public static Criterion<BeeNestDestroyedTrigger.TriggerInstance> destroyedBeeNest(
         final Block block, final ItemPredicate.Builder itemPredicate, final MinMaxBounds.Ints numBeesInside
      ) {
         return CriteriaTriggers.BEE_NEST_DESTROYED
            .createCriterion(
               new BeeNestDestroyedTrigger.TriggerInstance(
                  Optional.empty(), Optional.of(block.builtInRegistryHolder()), Optional.of(itemPredicate.build()), numBeesInside
               )
            );
      }

      public boolean matches(final BlockState state, final ItemStack itemStack, final int numBeesInside) {
         if (this.block.isPresent() && !state.is(this.block.get())) {
            return false;
         } else {
            return this.item.isPresent() && !this.item.get().test(itemStack) ? false : this.beesInside.matches(numBeesInside);
         }
      }
   }
}
