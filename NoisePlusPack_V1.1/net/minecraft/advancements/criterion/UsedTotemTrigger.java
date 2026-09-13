package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.HolderGetter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class UsedTotemTrigger extends SimpleCriterionTrigger<UsedTotemTrigger.TriggerInstance> {
   @Override
   public Codec<UsedTotemTrigger.TriggerInstance> codec() {
      return UsedTotemTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_459385_, ItemStack p_460654_) {
      this.trigger(p_459385_, p_452322_ -> p_452322_.matches(p_460654_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<UsedTotemTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_457553_ -> p_457553_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(UsedTotemTrigger.TriggerInstance::player),
               ItemPredicate.CODEC.optionalFieldOf("item").forGetter(UsedTotemTrigger.TriggerInstance::item)
            )
            .apply(p_457553_, UsedTotemTrigger.TriggerInstance::new)
      );

      public static Criterion<UsedTotemTrigger.TriggerInstance> usedTotem(ItemPredicate p_461076_) {
         return CriteriaTriggers.USED_TOTEM.createCriterion(new UsedTotemTrigger.TriggerInstance(Optional.empty(), Optional.of(p_461076_)));
      }

      public static Criterion<UsedTotemTrigger.TriggerInstance> usedTotem(HolderGetter<Item> p_455080_, ItemLike p_452893_) {
         return CriteriaTriggers.USED_TOTEM
            .createCriterion(new UsedTotemTrigger.TriggerInstance(Optional.empty(), Optional.of(ItemPredicate.Builder.item().of(p_455080_, p_452893_).build())));
      }

      public boolean matches(ItemStack p_450389_) {
         return this.item.isEmpty() || this.item.get().test(p_450389_);
      }
   }
}
