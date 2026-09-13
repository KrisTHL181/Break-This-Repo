package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.server.PlayerAdvancements;

public class ImpossibleTrigger implements CriterionTrigger<ImpossibleTrigger.TriggerInstance> {
   @Override
   public void addPlayerListener(PlayerAdvancements p_456766_, CriterionTrigger.Listener<ImpossibleTrigger.TriggerInstance> p_460303_) {
   }

   @Override
   public void removePlayerListener(PlayerAdvancements p_457905_, CriterionTrigger.Listener<ImpossibleTrigger.TriggerInstance> p_460466_) {
   }

   @Override
   public void removePlayerListeners(PlayerAdvancements p_457713_) {
   }

   @Override
   public Codec<ImpossibleTrigger.TriggerInstance> codec() {
      return ImpossibleTrigger.TriggerInstance.CODEC;
   }

   public record TriggerInstance() implements CriterionTriggerInstance {
      public static final Codec<ImpossibleTrigger.TriggerInstance> CODEC = MapCodec.unitCodec(new ImpossibleTrigger.TriggerInstance());

      @Override
      public void validate(CriterionValidator p_457990_) {
      }
   }
}
