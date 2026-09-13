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
   public BeehiveFieldRenameFix(final Schema outputSchema) {
      super(outputSchema, true);
   }

   private Dynamic<?> fixBeehive(final Dynamic<?> beehive) {
      return beehive.remove("Bees");
   }

   private Dynamic<?> fixBee(Dynamic<?> bee) {
      bee = bee.remove("EntityData");
      bee = bee.renameField("TicksInHive", "ticks_in_hive");
      return bee.renameField("MinOccupationTicks", "min_ticks_in_hive");
   }

   public TypeRewriteRule makeRule() {
      Type<?> beehiveType = this.getInputSchema().getChoiceType(References.BLOCK_ENTITY, "minecraft:beehive");
      OpticFinder<?> beehiveF = DSL.namedChoice("minecraft:beehive", beehiveType);
      ListType<?> beesType = (ListType<?>)beehiveType.findFieldType("Bees");
      Type<?> beeType = beesType.getElement();
      OpticFinder<?> beesF = DSL.fieldFinder("Bees", beesType);
      OpticFinder<?> beeF = DSL.typeFinder(beeType);
      Type<?> entityType = this.getInputSchema().getType(References.BLOCK_ENTITY);
      Type<?> newEntityType = this.getOutputSchema().getType(References.BLOCK_ENTITY);
      return this.fixTypeEverywhereTyped(
         "BeehiveFieldRenameFix",
         entityType,
         newEntityType,
         input -> ExtraDataFixUtils.cast(
            newEntityType,
            input.updateTyped(
               beehiveF,
               beehive -> beehive.update(DSL.remainderFinder(), this::fixBeehive)
                  .updateTyped(beesF, bees -> bees.updateTyped(beeF, bee -> bee.update(DSL.remainderFinder(), this::fixBee)))
            )
         )
      );
   }
}
