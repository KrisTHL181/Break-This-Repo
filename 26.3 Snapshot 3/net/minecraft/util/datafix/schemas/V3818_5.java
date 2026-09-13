package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.types.templates.Hook.HookFunction;
import com.mojang.serialization.DynamicOps;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3818_5 extends NamespacedSchema {
   protected static final HookFunction UNPACK_PLAIN_ID = new HookFunction() {
      public <T> T apply(final DynamicOps<T> ops, final T value) {
         Optional<String> maybePlainId = ops.getStringValue(value).result();
         return (T)(maybePlainId.isPresent() ? ops.createMap(Map.of(ops.createString("id"), value, ops.createString("count"), (T)ops.createInt(1))) : value);
      }
   };

   public V3818_5(final int versionKey, final Schema parent) {
      super(versionKey, parent);
   }

   public void registerTypes(
      final Schema schema, final Map<String, Supplier<TypeTemplate>> entityTypes, final Map<String, Supplier<TypeTemplate>> blockEntityTypes
   ) {
      super.registerTypes(schema, entityTypes, blockEntityTypes);
      schema.registerType(
         true,
         References.ITEM_STACK,
         () -> DSL.hook(
            DSL.optionalFields("id", References.ITEM_NAME.in(schema), "components", References.DATA_COMPONENTS.in(schema)),
            UNPACK_PLAIN_ID,
            HookFunction.IDENTITY
         )
      );
   }
}
