package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class EmptyItemInVillagerTradeFix extends DataFix {
   public EmptyItemInVillagerTradeFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   public TypeRewriteRule makeRule() {
      Type<?> tradeType = this.getInputSchema().getType(References.VILLAGER_TRADE);
      return this.writeFixAndRead("EmptyItemInVillagerTradeFix", tradeType, tradeType, input -> {
         Dynamic<?> buyB = input.get("buyB").orElseEmptyMap();
         String id = NamespacedSchema.ensureNamespaced(buyB.get("id").asString("minecraft:air"));
         int count = buyB.get("count").asInt(0);
         return !id.equals("minecraft:air") && count != 0 ? input : input.remove("buyB");
      });
   }
}
