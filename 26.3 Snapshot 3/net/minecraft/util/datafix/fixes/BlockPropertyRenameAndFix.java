package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.function.UnaryOperator;

public class BlockPropertyRenameAndFix extends AbstractBlockPropertyFix {
   private final String blockId;
   private final String oldPropertyName;
   private final String newPropertyName;
   private final UnaryOperator<String> valueFixer;

   public BlockPropertyRenameAndFix(
      final Schema outputSchema,
      final String name,
      final String blockId,
      final String oldPropertyName,
      final String newPropertyName,
      final UnaryOperator<String> valueFixer
   ) {
      super(outputSchema, name);
      this.blockId = blockId;
      this.oldPropertyName = oldPropertyName;
      this.newPropertyName = newPropertyName;
      this.valueFixer = valueFixer;
   }

   @Override
   protected boolean shouldFix(final String blockId) {
      return blockId.equals(this.blockId);
   }

   @Override
   protected <T> Dynamic<T> fixProperties(final String blockId, final Dynamic<T> properties) {
      return properties.renameAndFixField(
         this.oldPropertyName, this.newPropertyName, dynamic -> dynamic.createString(this.valueFixer.apply(dynamic.asString("")))
      );
   }
}
