package net.minecraft.advancements.triggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.world.level.storage.loot.ValidationContextSource;

public class ImpossibleTrigger implements CriterionTrigger<ImpossibleTrigger.TriggerInstance> {
   @Override
   public Codec<ImpossibleTrigger.TriggerInstance> codec() {
      return ImpossibleTrigger.TriggerInstance.CODEC;
   }

   public record TriggerInstance() implements CriterionTriggerInstance {
      public static final Codec<ImpossibleTrigger.TriggerInstance> CODEC = MapCodec.unitCodec(new ImpossibleTrigger.TriggerInstance());

      @Override
      public void validate(final ValidationContextSource validator) {
      }
   }
}
