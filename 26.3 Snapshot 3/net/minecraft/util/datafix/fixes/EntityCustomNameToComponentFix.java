package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class EntityCustomNameToComponentFix extends DataFix {
   public EntityCustomNameToComponentFix(final Schema outputSchema) {
      super(outputSchema, true);
   }

   public TypeRewriteRule makeRule() {
      Type<?> entityType = this.getInputSchema().getType(References.ENTITY);
      Type<?> newEntityType = this.getOutputSchema().getType(References.ENTITY);
      OpticFinder<String> idF = DSL.fieldFinder("id", NamespacedSchema.namespacedString());
      OpticFinder<String> customNameF = entityType.findField("CustomName");
      Type<?> newCustomNameType = newEntityType.findFieldType("CustomName");
      return this.fixTypeEverywhereTyped(
         "EntityCustomNameToComponentFix", entityType, newEntityType, entity -> fixEntity(entity, newEntityType, idF, customNameF, newCustomNameType)
      );
   }

   private static <T> Typed<?> fixEntity(
      final Typed<?> entity, final Type<?> newEntityType, final OpticFinder<String> idF, final OpticFinder<String> customNameF, final Type<T> newCustomNameType
   ) {
      Optional<String> customName = entity.getOptional(customNameF);
      if (customName.isEmpty()) {
         return ExtraDataFixUtils.cast(newEntityType, (Typed<T>)entity);
      }

      if (customName.get().isEmpty()) {
         return Util.writeAndReadTypedOrThrow(entity, newEntityType, dynamic -> dynamic.remove("CustomName"));
      }

      String id = entity.getOptional(idF).orElse("");
      Dynamic<?> component = fixCustomName(entity.getOps(), customName.get(), id);
      return entity.set(customNameF, Util.readTypedOrThrow(newCustomNameType, component));
   }

   private static <T> Dynamic<T> fixCustomName(final DynamicOps<T> ops, final String customName, final String id) {
      return "minecraft:commandblock_minecart".equals(id)
         ? new Dynamic(ops, ops.createString(customName))
         : LegacyComponentDataFixUtils.createPlainTextComponent(ops, customName);
   }
}
