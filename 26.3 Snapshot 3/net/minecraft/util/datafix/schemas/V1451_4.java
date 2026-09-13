package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1451_4 extends NamespacedSchema {
   public V1451_4(final int versionKey, final Schema parent) {
      super(versionKey, parent);
   }

   public void registerTypes(
      final Schema schema, final Map<String, Supplier<TypeTemplate>> entityTypes, final Map<String, Supplier<TypeTemplate>> blockEntityTypes
   ) {
      super.registerTypes(schema, entityTypes, blockEntityTypes);
      schema.registerType(false, References.BLOCK_NAME, () -> DSL.constType(namespacedString()));
   }
}
