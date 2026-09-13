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

   public DropChancesFormatFix(Schema p_395224_) {
      super(p_395224_, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "DropChancesFormatFix", this.getInputSchema().getType(References.ENTITY), p_392460_ -> p_392460_.update(DSL.remainderFinder(), p_395033_ -> {
            List<Float> list = parseDropChances(p_395033_.get("ArmorDropChances"));
            List<Float> list1 = parseDropChances(p_395033_.get("HandDropChances"));
            float f = p_395033_.get("body_armor_drop_chance").asNumber().result().map(Number::floatValue).orElse(0.085F);
            p_395033_ = p_395033_.remove("ArmorDropChances").remove("HandDropChances").remove("body_armor_drop_chance");
            Dynamic<?> dynamic = p_395033_.emptyMap();
            dynamic = addSlotChances(dynamic, list, ARMOR_SLOT_NAMES);
            dynamic = addSlotChances(dynamic, list1, HAND_SLOT_NAMES);
            if (f != 0.085F) {
               dynamic = dynamic.set("body", p_395033_.createFloat(f));
            }

            return !dynamic.equals(p_395033_.emptyMap()) ? p_395033_.set("drop_chances", dynamic) : p_395033_;
         })
      );
   }

   private static Dynamic<?> addSlotChances(Dynamic<?> p_395257_, List<Float> p_392343_, List<String> p_396856_) {
      for (int i = 0; i < p_396856_.size() && i < p_392343_.size(); i++) {
         String s = p_396856_.get(i);
         float f = p_392343_.get(i);
         if (f != 0.085F) {
            p_395257_ = p_395257_.set(s, p_395257_.createFloat(f));
         }
      }

      return p_395257_;
   }

   private static List<Float> parseDropChances(OptionalDynamic<?> p_391345_) {
      return p_391345_.asStream().map(p_397948_ -> p_397948_.asFloat(0.085F)).toList();
   }
}
