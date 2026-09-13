package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.HashMap;
import java.util.Map;

public class PlayerEquipmentFix extends DataFix {
   private static final Map<Integer, String> SLOT_TRANSLATIONS = Map.of(100, "feet", 101, "legs", 102, "chest", 103, "head", -106, "offhand");

   public PlayerEquipmentFix(Schema p_397454_) {
      super(p_397454_, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getTypeRaw(References.PLAYER);
      Type<?> type1 = this.getOutputSchema().getTypeRaw(References.PLAYER);
      return this.writeFixAndRead("Player Equipment Fix", type, type1, p_393768_ -> {
         Map<Dynamic<?>, Dynamic<?>> map = new HashMap<>();
         p_393768_ = p_393768_.update("Inventory", p_391651_ -> p_391651_.createList(p_391651_.asStream().filter(p_393067_ -> {
            int i = p_393067_.get("Slot").asInt(-1);
            String s = SLOT_TRANSLATIONS.get(i);
            if (s != null) {
               map.put(p_391651_.createString(s), p_393067_.remove("Slot"));
            }

            return s == null;
         })));
         return p_393768_.set("equipment", p_393768_.createMap(map));
      });
   }
}
