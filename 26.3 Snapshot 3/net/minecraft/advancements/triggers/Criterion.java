package net.minecraft.advancements.triggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.util.ExtraCodecs;

public record Criterion<T extends CriterionTriggerInstance>(CriterionTrigger<T> trigger, T triggerInstance) {
   private static final MapCodec<Criterion<?>> MAP_CODEC = ExtraCodecs.dispatchOptionalValue(
      "trigger", "conditions", (Codec<CriterionTrigger<T>>)CriteriaTriggers.CODEC, Criterion::trigger, Criterion::criterionCodec
   );
   public static final Codec<Criterion<?>> CODEC = MAP_CODEC.codec();

   private static <T extends CriterionTriggerInstance> Codec<Criterion<T>> criterionCodec(final CriterionTrigger<T> trigger) {
      return trigger.codec().xmap(instance -> new Criterion<>(trigger, (T)instance), Criterion::triggerInstance);
   }
}
