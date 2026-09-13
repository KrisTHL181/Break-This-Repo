package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.function.DoubleUnaryOperator;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class EntityAttributeBaseValueFix extends NamedEntityFix {
   private final String attributeId;
   private final DoubleUnaryOperator valueFixer;

   public EntityAttributeBaseValueFix(
      final Schema outputSchema, final String name, final String entityName, final String attributeId, final DoubleUnaryOperator valueFixer
   ) {
      super(outputSchema, false, name, References.ENTITY, entityName);
      this.attributeId = attributeId;
      this.valueFixer = valueFixer;
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(DSL.remainderFinder(), this::fixValue);
   }

   private Dynamic<?> fixValue(final Dynamic<?> tag) {
      return tag.update("attributes", attributes -> tag.createList(attributes.asStream().map(attribute -> {
         String attributeId = NamespacedSchema.ensureNamespaced(attribute.get("id").asString(""));
         if (!attributeId.equals(this.attributeId)) {
            return (Dynamic)attribute;
         }

         double base = attribute.get("base").asDouble(0.0);
         return attribute.set("base", attribute.createDouble(this.valueFixer.applyAsDouble(base)));
      })));
   }
}
