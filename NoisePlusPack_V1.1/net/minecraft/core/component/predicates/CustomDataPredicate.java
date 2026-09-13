package net.minecraft.core.component.predicates;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.criterion.NbtPredicate;
import net.minecraft.core.component.DataComponentGetter;

public record CustomDataPredicate(NbtPredicate value) implements DataComponentPredicate {
   public static final Codec<CustomDataPredicate> CODEC = NbtPredicate.CODEC.xmap(CustomDataPredicate::new, CustomDataPredicate::value);

   @Override
   public boolean matches(DataComponentGetter p_393383_) {
      return this.value.matches(p_393383_);
   }

   public static CustomDataPredicate customData(NbtPredicate p_457215_) {
      return new CustomDataPredicate(p_457215_);
   }
}
