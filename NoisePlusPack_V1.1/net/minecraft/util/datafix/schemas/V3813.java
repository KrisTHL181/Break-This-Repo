package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3813 extends NamespacedSchema {
   public V3813(int p_391534_, Schema p_394133_) {
      super(p_391534_, p_394133_);
   }

   public void registerTypes(Schema p_393905_, Map<String, Supplier<TypeTemplate>> p_395954_, Map<String, Supplier<TypeTemplate>> p_391475_) {
      super.registerTypes(p_393905_, p_395954_, p_391475_);
      p_393905_.registerType(
         false,
         References.SAVED_DATA_MAP_DATA,
         () -> DSL.optionalFields("data", DSL.optionalFields("banners", DSL.list(DSL.optionalFields("name", References.TEXT_COMPONENT.in(p_393905_)))))
      );
   }
}
