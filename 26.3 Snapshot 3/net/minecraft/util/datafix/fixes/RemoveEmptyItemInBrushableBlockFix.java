package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class RemoveEmptyItemInBrushableBlockFix extends NamedEntityWriteReadFix {
   public RemoveEmptyItemInBrushableBlockFix(final Schema outputSchema) {
      super(outputSchema, false, "RemoveEmptyItemInSuspiciousBlockFix", References.BLOCK_ENTITY, "minecraft:brushable_block");
   }

   @Override
   protected <T> Dynamic<T> fix(final Dynamic<T> input) {
      Optional<Dynamic<T>> item = input.get("item").result();
      return item.isPresent() && isEmptyStack(item.get()) ? input.remove("item") : input;
   }

   private static boolean isEmptyStack(final Dynamic<?> item) {
      String id = NamespacedSchema.ensureNamespaced(item.get("id").asString("minecraft:air"));
      int count = item.get("count").asInt(0);
      return id.equals("minecraft:air") || count == 0;
   }
}
