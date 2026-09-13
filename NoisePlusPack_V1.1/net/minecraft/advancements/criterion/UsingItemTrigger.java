package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class UsingItemTrigger extends SimpleCriterionTrigger<UsingItemTrigger.TriggerInstance> {
   @Override
   public Codec<UsingItemTrigger.TriggerInstance> codec() {
      return UsingItemTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_453923_, ItemStack p_454774_) {
      this.trigger(p_453923_, p_455718_ -> p_455718_.matches(p_454774_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<UsingItemTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_460946_ -> p_460946_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(UsingItemTrigger.TriggerInstance::player),
               ItemPredicate.CODEC.optionalFieldOf("item").forGetter(UsingItemTrigger.TriggerInstance::item)
            )
            .apply(p_460946_, UsingItemTrigger.TriggerInstance::new)
      );

      public static Criterion<UsingItemTrigger.TriggerInstance> lookingAt(EntityPredicate.Builder p_456755_, ItemPredicate.Builder p_450721_) {
         return CriteriaTriggers.USING_ITEM
            .createCriterion(new UsingItemTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(p_456755_)), Optional.of(p_450721_.build())));
      }

      public boolean matches(ItemStack p_456960_) {
         return !this.item.isPresent() || this.item.get().test(p_456960_);
      }
   }
}
