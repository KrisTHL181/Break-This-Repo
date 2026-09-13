package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class EmptyItemInVillagerTradeFix extends DataFix {
   public EmptyItemInVillagerTradeFix(Schema p_331010_) {
      super(p_331010_, false);
   }

   public TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.VILLAGER_TRADE);
      return this.writeFixAndRead("EmptyItemInVillagerTradeFix", type, type, p_333025_ -> {
         Dynamic<?> dynamic = p_333025_.get("buyB").orElseEmptyMap();
         String s = NamespacedSchema.ensureNamespaced(dynamic.get("id").asString("minecraft:air"));
         int i = dynamic.get("count").asInt(0);
         return !s.equals("minecraft:air") && i != 0 ? p_333025_ : p_333025_.remove("buyB");
      });
   }
}
