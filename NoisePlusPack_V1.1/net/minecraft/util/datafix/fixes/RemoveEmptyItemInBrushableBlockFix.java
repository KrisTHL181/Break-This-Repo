package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class RemoveEmptyItemInBrushableBlockFix extends NamedEntityWriteReadFix {
   public RemoveEmptyItemInBrushableBlockFix(Schema p_328124_) {
      super(p_328124_, false, "RemoveEmptyItemInSuspiciousBlockFix", References.BLOCK_ENTITY, "minecraft:brushable_block");
   }

   @Override
   protected <T> Dynamic<T> fix(Dynamic<T> p_330310_) {
      Optional<Dynamic<T>> optional = p_330310_.get("item").result();
      return optional.isPresent() && isEmptyStack(optional.get()) ? p_330310_.remove("item") : p_330310_;
   }

   private static boolean isEmptyStack(Dynamic<?> p_328874_) {
      String s = NamespacedSchema.ensureNamespaced(p_328874_.get("id").asString("minecraft:air"));
      int i = p_328874_.get("count").asInt(0);
      return s.equals("minecraft:air") || i == 0;
   }
}
