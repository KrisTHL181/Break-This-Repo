package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
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

   public void trigger(ServerPlayer p_456107_, BlockState p_453003_, ItemStack p_452135_, int p_450284_) {
      this.trigger(p_456107_, p_452858_ -> p_452858_.matches(p_453003_, p_452135_, p_450284_));
   }

   public record TriggerInstance(
      Optional<ContextAwarePredicate> player, Optional<Holder<Block>> block, Optional<ItemPredicate> item, MinMaxBounds.Ints beesInside
   ) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<BeeNestDestroyedTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_460448_ -> p_460448_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(BeeNestDestroyedTrigger.TriggerInstance::player),
               BuiltInRegistries.BLOCK.holderByNameCodec().optionalFieldOf("block").forGetter(BeeNestDestroyedTrigger.TriggerInstance::block),
               ItemPredicate.CODEC.optionalFieldOf("item").forGetter(BeeNestDestroyedTrigger.TriggerInstance::item),
               MinMaxBounds.Ints.CODEC.optionalFieldOf("num_bees_inside", MinMaxBounds.Ints.ANY).forGetter(BeeNestDestroyedTrigger.TriggerInstance::beesInside)
            )
            .apply(p_460448_, BeeNestDestroyedTrigger.TriggerInstance::new)
      );

      public static Criterion<BeeNestDestroyedTrigger.TriggerInstance> destroyedBeeNest(
         Block p_454318_, ItemPredicate.Builder p_455912_, MinMaxBounds.Ints p_460389_
      ) {
         return CriteriaTriggers.BEE_NEST_DESTROYED
            .createCriterion(
               new BeeNestDestroyedTrigger.TriggerInstance(
                  Optional.empty(), Optional.of(p_454318_.builtInRegistryHolder()), Optional.of(p_455912_.build()), p_460389_
               )
            );
      }

      public boolean matches(BlockState p_457537_, ItemStack p_458571_, int p_450339_) {
         if (this.block.isPresent() && !p_457537_.is(this.block.get())) {
            return false;
         } else {
            return this.item.isPresent() && !this.item.get().test(p_458571_) ? false : this.beesInside.matches(p_450339_);
         }
      }
   }
}
