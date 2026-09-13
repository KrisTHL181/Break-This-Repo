package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4292 extends NamespacedSchema {
   public V4292(int p_393326_, Schema p_391298_) {
      super(p_393326_, p_391298_);
   }

   public void registerTypes(Schema p_392792_, Map<String, Supplier<TypeTemplate>> p_392394_, Map<String, Supplier<TypeTemplate>> p_392244_) {
      super.registerTypes(p_392792_, p_392394_, p_392244_);
      p_392792_.registerType(
         true,
         References.TEXT_COMPONENT,
         () -> DSL.or(
            DSL.or(DSL.constType(DSL.string()), DSL.list(References.TEXT_COMPONENT.in(p_392792_))),
            DSL.optionalFields(
               "extra",
               DSL.list(References.TEXT_COMPONENT.in(p_392792_)),
               "separator",
               References.TEXT_COMPONENT.in(p_392792_),
               "hover_event",
               DSL.taggedChoice(
                  "action",
                  DSL.string(),
                  Map.of(
                     "show_text",
                     DSL.optionalFields("value", References.TEXT_COMPONENT.in(p_392792_)),
                     "show_item",
                     References.ITEM_STACK.in(p_392792_),
                     "show_entity",
                     DSL.optionalFields("id", References.ENTITY_NAME.in(p_392792_), "name", References.TEXT_COMPONENT.in(p_392792_))
                  )
               )
            )
         )
      );
   }
}
