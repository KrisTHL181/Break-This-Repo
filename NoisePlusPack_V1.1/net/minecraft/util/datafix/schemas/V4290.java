package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4290 extends NamespacedSchema {
   public V4290(int p_394483_, Schema p_395200_) {
      super(p_394483_, p_395200_);
   }

   public void registerTypes(Schema p_393696_, Map<String, Supplier<TypeTemplate>> p_391173_, Map<String, Supplier<TypeTemplate>> p_396637_) {
      super.registerTypes(p_393696_, p_391173_, p_396637_);
      p_393696_.registerType(
         true,
         References.TEXT_COMPONENT,
         () -> DSL.or(
            DSL.or(DSL.constType(DSL.string()), DSL.list(References.TEXT_COMPONENT.in(p_393696_))),
            DSL.optionalFields(
               "extra",
               DSL.list(References.TEXT_COMPONENT.in(p_393696_)),
               "separator",
               References.TEXT_COMPONENT.in(p_393696_),
               "hoverEvent",
               DSL.taggedChoice(
                  "action",
                  DSL.string(),
                  Map.of(
                     "show_text",
                     DSL.optionalFields("contents", References.TEXT_COMPONENT.in(p_393696_)),
                     "show_item",
                     DSL.optionalFields("contents", DSL.or(References.ITEM_STACK.in(p_393696_), References.ITEM_NAME.in(p_393696_))),
                     "show_entity",
                     DSL.optionalFields("type", References.ENTITY_NAME.in(p_393696_), "name", References.TEXT_COMPONENT.in(p_393696_))
                  )
               )
            )
         )
      );
   }
}
