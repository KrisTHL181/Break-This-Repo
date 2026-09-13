package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import java.util.List;

public class DropChancesFormatFix extends DataFix {
   private static final List<String> ARMOR_SLOT_NAMES = List.of("feet", "legs", "chest", "head");
   private static final List<String> HAND_SLOT_NAMES = List.of("mainhand", "offhand");
   private static final float DEFAULT_CHANCE = 0.085F;

   public DropChancesFormatFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "DropChancesFormatFix", this.getInputSchema().getType(References.ENTITY), input -> input.update(DSL.remainderFinder(), remainder -> {
            List<Float> armorDropChances = parseDropChances(remainder.get("ArmorDropChances"));
            List<Float> handDropChances = parseDropChances(remainder.get("HandDropChances"));
            float bodyArmorDropChance = remainder.get("body_armor_drop_chance").asNumber().result().map(Number::floatValue).orElse(0.085F);
            remainder = remainder.remove("ArmorDropChances").remove("HandDropChances").remove("body_armor_drop_chance");
            Dynamic<?> slotChances = remainder.emptyMap();
            slotChances = addSlotChances(slotChances, armorDropChances, ARMOR_SLOT_NAMES);
            slotChances = addSlotChances(slotChances, handDropChances, HAND_SLOT_NAMES);
            if (bodyArmorDropChance != 0.085F) {
               slotChances = slotChances.set("body", remainder.createFloat(bodyArmorDropChance));
            }

            return !slotChances.equals(remainder.emptyMap()) ? remainder.set("drop_chances", slotChances) : remainder;
         })
      );
   }

   private static Dynamic<?> addSlotChances(Dynamic<?> output, final List<Float> chances, final List<String> slotNames) {
      for (int i = 0; i < slotNames.size() && i < chances.size(); i++) {
         String slot = slotNames.get(i);
         float chance = chances.get(i);
         if (chance != 0.085F) {
            output = output.set(slot, output.createFloat(chance));
         }
      }

      return output;
   }

   private static List<Float> parseDropChances(final OptionalDynamic<?> value) {
      return value.asStream().map(dynamic -> dynamic.asFloat(0.085F)).toList();
   }
}
