package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class BeehiveFieldRenameFix extends DataFix {
   public BeehiveFieldRenameFix(Schema p_336393_) {
      super(p_336393_, true);
   }

   private Dynamic<?> fixBeehive(Dynamic<?> p_334771_) {
      return p_334771_.remove("Bees");
   }

   private Dynamic<?> fixBee(Dynamic<?> p_335732_) {
      p_335732_ = p_335732_.remove("EntityData");
      p_335732_ = p_335732_.renameField("TicksInHive", "ticks_in_hive");
      return p_335732_.renameField("MinOccupationTicks", "min_ticks_in_hive");
   }

   public TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getChoiceType(References.BLOCK_ENTITY, "minecraft:beehive");
      OpticFinder<?> opticfinder = DSL.namedChoice("minecraft:beehive", type);
      ListType<?> listtype = (ListType<?>)type.findFieldType("Bees");
      Type<?> type1 = listtype.getElement();
      OpticFinder<?> opticfinder1 = DSL.fieldFinder("Bees", listtype);
      OpticFinder<?> opticfinder2 = DSL.typeFinder(type1);
      Type<?> type2 = this.getInputSchema().getType(References.BLOCK_ENTITY);
      Type<?> type3 = this.getOutputSchema().getType(References.BLOCK_ENTITY);
      return this.fixTypeEverywhereTyped(
         "BeehiveFieldRenameFix",
         type2,
         type3,
         p_336050_ -> ExtraDataFixUtils.cast(
            type3,
            p_336050_.updateTyped(
               opticfinder,
               p_334762_ -> p_334762_.update(DSL.remainderFinder(), this::fixBeehive)
                  .updateTyped(
                     opticfinder1, p_334908_ -> p_334908_.updateTyped(opticfinder2, p_331823_ -> p_331823_.update(DSL.remainderFinder(), this::fixBee))
                  )
            )
         )
      );
   }
}
