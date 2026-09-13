package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.util.datafix.schemas.NamespacedSchema;
import org.jspecify.annotations.Nullable;

public class ItemPotionFix extends DataFix {
   private static final int SPLASH = 16384;
   private static final @Nullable String[] POTIONS = (String[])DataFixUtils.make(new String[128], map -> {
      map[0] = "minecraft:water";
      map[1] = "minecraft:regeneration";
      map[2] = "minecraft:swiftness";
      map[3] = "minecraft:fire_resistance";
      map[4] = "minecraft:poison";
      map[5] = "minecraft:healing";
      map[6] = "minecraft:night_vision";
      map[7] = null;
      map[8] = "minecraft:weakness";
      map[9] = "minecraft:strength";
      map[10] = "minecraft:slowness";
      map[11] = "minecraft:leaping";
      map[12] = "minecraft:harming";
      map[13] = "minecraft:water_breathing";
      map[14] = "minecraft:invisibility";
      map[15] = null;
      map[16] = "minecraft:awkward";
      map[17] = "minecraft:regeneration";
      map[18] = "minecraft:swiftness";
      map[19] = "minecraft:fire_resistance";
      map[20] = "minecraft:poison";
      map[21] = "minecraft:healing";
      map[22] = "minecraft:night_vision";
      map[23] = null;
      map[24] = "minecraft:weakness";
      map[25] = "minecraft:strength";
      map[26] = "minecraft:slowness";
      map[27] = "minecraft:leaping";
      map[28] = "minecraft:harming";
      map[29] = "minecraft:water_breathing";
      map[30] = "minecraft:invisibility";
      map[31] = null;
      map[32] = "minecraft:thick";
      map[33] = "minecraft:strong_regeneration";
      map[34] = "minecraft:strong_swiftness";
      map[35] = "minecraft:fire_resistance";
      map[36] = "minecraft:strong_poison";
      map[37] = "minecraft:strong_healing";
      map[38] = "minecraft:night_vision";
      map[39] = null;
      map[40] = "minecraft:weakness";
      map[41] = "minecraft:strong_strength";
      map[42] = "minecraft:slowness";
      map[43] = "minecraft:strong_leaping";
      map[44] = "minecraft:strong_harming";
      map[45] = "minecraft:water_breathing";
      map[46] = "minecraft:invisibility";
      map[47] = null;
      map[48] = null;
      map[49] = "minecraft:strong_regeneration";
      map[50] = "minecraft:strong_swiftness";
      map[51] = "minecraft:fire_resistance";
      map[52] = "minecraft:strong_poison";
      map[53] = "minecraft:strong_healing";
      map[54] = "minecraft:night_vision";
      map[55] = null;
      map[56] = "minecraft:weakness";
      map[57] = "minecraft:strong_strength";
      map[58] = "minecraft:slowness";
      map[59] = "minecraft:strong_leaping";
      map[60] = "minecraft:strong_harming";
      map[61] = "minecraft:water_breathing";
      map[62] = "minecraft:invisibility";
      map[63] = null;
      map[64] = "minecraft:mundane";
      map[65] = "minecraft:long_regeneration";
      map[66] = "minecraft:long_swiftness";
      map[67] = "minecraft:long_fire_resistance";
      map[68] = "minecraft:long_poison";
      map[69] = "minecraft:healing";
      map[70] = "minecraft:long_night_vision";
      map[71] = null;
      map[72] = "minecraft:long_weakness";
      map[73] = "minecraft:long_strength";
      map[74] = "minecraft:long_slowness";
      map[75] = "minecraft:long_leaping";
      map[76] = "minecraft:harming";
      map[77] = "minecraft:long_water_breathing";
      map[78] = "minecraft:long_invisibility";
      map[79] = null;
      map[80] = "minecraft:awkward";
      map[81] = "minecraft:long_regeneration";
      map[82] = "minecraft:long_swiftness";
      map[83] = "minecraft:long_fire_resistance";
      map[84] = "minecraft:long_poison";
      map[85] = "minecraft:healing";
      map[86] = "minecraft:long_night_vision";
      map[87] = null;
      map[88] = "minecraft:long_weakness";
      map[89] = "minecraft:long_strength";
      map[90] = "minecraft:long_slowness";
      map[91] = "minecraft:long_leaping";
      map[92] = "minecraft:harming";
      map[93] = "minecraft:long_water_breathing";
      map[94] = "minecraft:long_invisibility";
      map[95] = null;
      map[96] = "minecraft:thick";
      map[97] = "minecraft:regeneration";
      map[98] = "minecraft:swiftness";
      map[99] = "minecraft:long_fire_resistance";
      map[100] = "minecraft:poison";
      map[101] = "minecraft:strong_healing";
      map[102] = "minecraft:long_night_vision";
      map[103] = null;
      map[104] = "minecraft:long_weakness";
      map[105] = "minecraft:strength";
      map[106] = "minecraft:long_slowness";
      map[107] = "minecraft:leaping";
      map[108] = "minecraft:strong_harming";
      map[109] = "minecraft:long_water_breathing";
      map[110] = "minecraft:long_invisibility";
      map[111] = null;
      map[112] = null;
      map[113] = "minecraft:regeneration";
      map[114] = "minecraft:swiftness";
      map[115] = "minecraft:long_fire_resistance";
      map[116] = "minecraft:poison";
      map[117] = "minecraft:strong_healing";
      map[118] = "minecraft:long_night_vision";
      map[119] = null;
      map[120] = "minecraft:long_weakness";
      map[121] = "minecraft:strength";
      map[122] = "minecraft:long_slowness";
      map[123] = "minecraft:leaping";
      map[124] = "minecraft:strong_harming";
      map[125] = "minecraft:long_water_breathing";
      map[126] = "minecraft:long_invisibility";
      map[127] = null;
   });
   public static final String DEFAULT = "minecraft:water";

   public ItemPotionFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType);
   }

   public TypeRewriteRule makeRule() {
      Type<?> itemStackType = this.getInputSchema().getType(References.ITEM_STACK);
      OpticFinder<Pair<String, String>> idFinder = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
      OpticFinder<?> tagFinder = itemStackType.findField("tag");
      return this.fixTypeEverywhereTyped(
         "ItemPotionFix",
         itemStackType,
         input -> {
            Optional<Pair<String, String>> id = input.getOptional(idFinder);
            if (id.isPresent() && Objects.equals(id.get().getSecond(), "minecraft:potion")) {
               Dynamic<?> rest = (Dynamic<?>)input.get(DSL.remainderFinder());
               Optional<? extends Typed<?>> tag = input.getOptionalTyped(tagFinder);
               short damage = rest.get("Damage").asShort((short)0);
               if (tag.isPresent()) {
                  Typed<?> output = input;
                  Dynamic<?> tagRest = (Dynamic<?>)tag.get().get(DSL.remainderFinder());
                  Optional<String> potion = tagRest.get("Potion").asString().result();
                  if (potion.isEmpty()) {
                     String potionName = POTIONS[damage & 127];
                     Typed<?> newTag = tag.get()
                        .set(DSL.remainderFinder(), tagRest.set("Potion", tagRest.createString(potionName == null ? "minecraft:water" : potionName)));
                     output = output.set(tagFinder, newTag);
                     if ((damage & 16384) == 16384) {
                        output = output.set(idFinder, Pair.of(References.ITEM_NAME.typeName(), "minecraft:splash_potion"));
                     }
                  }

                  if (damage != 0) {
                     rest = rest.set("Damage", rest.createShort((short)0));
                  }

                  return output.set(DSL.remainderFinder(), rest);
               }
            }

            return input;
         }
      );
   }
}
