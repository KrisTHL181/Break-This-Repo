package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1488 extends NamespacedSchema {
   public V1488(int p_395175_, Schema p_397841_) {
      super(p_395175_, p_397841_);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_393076_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(p_393076_);
      p_393076_.register(
         map,
         "minecraft:command_block",
         () -> DSL.optionalFields("CustomName", References.TEXT_COMPONENT.in(p_393076_), "LastOutput", References.TEXT_COMPONENT.in(p_393076_))
      );
      return map;
   }
}
