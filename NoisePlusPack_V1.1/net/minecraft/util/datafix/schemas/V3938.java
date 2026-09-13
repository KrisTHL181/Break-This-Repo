package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3938 extends NamespacedSchema {
   public V3938(int p_345209_, Schema p_342762_) {
      super(p_345209_, p_342762_);
   }

   protected static TypeTemplate abstractArrow(Schema p_344870_) {
      return DSL.optionalFields(
         "inBlockState", References.BLOCK_STATE.in(p_344870_), "item", References.ITEM_STACK.in(p_344870_), "weapon", References.ITEM_STACK.in(p_344870_)
      );
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_344154_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_344154_);
      p_344154_.register(map, "minecraft:spectral_arrow", () -> abstractArrow(p_344154_));
      p_344154_.register(map, "minecraft:arrow", () -> abstractArrow(p_344154_));
      return map;
   }
}
