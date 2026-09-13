package net.minecraft.util.datafix.fixes;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public class LockComponentPredicateFix extends DataComponentRemainderFix {
   public static final Escaper ESCAPER = Escapers.builder().addEscape('"', "\\\"").addEscape('\\', "\\\\").build();

   public LockComponentPredicateFix(Schema p_370065_) {
      super(p_370065_, "LockComponentPredicateFix", "minecraft:lock");
   }

   @Override
   protected <T> @Nullable Dynamic<T> fixComponent(Dynamic<T> p_360989_) {
      return fixLock(p_360989_);
   }

   public static <T> @Nullable Dynamic<T> fixLock(Dynamic<T> p_369566_) {
      Optional<String> optional = p_369566_.asString().result();
      if (optional.isEmpty()) {
         return null;
      }

      if (optional.get().isEmpty()) {
         return null;
      }

      Dynamic<T> dynamic = p_369566_.createString("\"" + ESCAPER.escape(optional.get()) + "\"");
      Dynamic<T> dynamic1 = p_369566_.emptyMap().set("minecraft:custom_name", dynamic);
      return p_369566_.emptyMap().set("components", dynamic1);
   }
}
