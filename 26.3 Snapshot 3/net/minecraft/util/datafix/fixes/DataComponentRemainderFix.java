package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public abstract class DataComponentRemainderFix extends DataFix {
   private final String name;
   private final String componentId;
   private final String newComponentId;

   public DataComponentRemainderFix(final Schema outputSchema, final String name, final String componentId) {
      this(outputSchema, name, componentId, componentId);
   }

   public DataComponentRemainderFix(final Schema outputSchema, final String name, final String componentId, final String newComponentId) {
      super(outputSchema, false);
      this.name = name;
      this.componentId = componentId;
      this.newComponentId = newComponentId;
   }

   public final TypeRewriteRule makeRule() {
      Type<?> dataComponentsType = this.getInputSchema().getType(References.DATA_COMPONENTS);
      return this.fixTypeEverywhereTyped(this.name, dataComponentsType, components -> components.update(DSL.remainderFinder(), remainder -> {
         Optional<? extends Dynamic<?>> component = remainder.get(this.componentId).result();
         if (component.isEmpty()) {
            return remainder;
         }

         Dynamic<?> newComponent = this.fixComponent(component.get());
         return remainder.remove(this.componentId).setFieldIfPresent(this.newComponentId, Optional.ofNullable(newComponent));
      }));
   }

   protected abstract <T> @Nullable Dynamic<T> fixComponent(Dynamic<T> input);
}
