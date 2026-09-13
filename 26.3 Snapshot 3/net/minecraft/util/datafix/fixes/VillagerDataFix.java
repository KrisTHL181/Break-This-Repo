package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class VillagerDataFix extends NamedEntityFix {
   public VillagerDataFix(final Schema schema, final String entityType) {
      super(schema, false, "Villager profession data fix (" + entityType + ")", References.ENTITY, entityType);
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      Dynamic<?> remainder = (Dynamic<?>)entity.get(DSL.remainderFinder());
      return entity.set(
         DSL.remainderFinder(),
         remainder.remove("Profession")
            .remove("Career")
            .remove("CareerLevel")
            .set(
               "VillagerData",
               remainder.createMap(
                  ImmutableMap.of(
                     remainder.createString("type"),
                     remainder.createString("minecraft:plains"),
                     remainder.createString("profession"),
                     remainder.createString(upgradeData(remainder.get("Profession").asInt(0), remainder.get("Career").asInt(0))),
                     remainder.createString("level"),
                     (Dynamic)DataFixUtils.orElse(remainder.get("CareerLevel").result(), remainder.createInt(1))
                  )
               )
            )
      );
   }

   private static String upgradeData(final int profession, final int career) {
      if (profession == 0) {
         if (career == 2) {
            return "minecraft:fisherman";
         } else if (career == 3) {
            return "minecraft:shepherd";
         } else {
            return career == 4 ? "minecraft:fletcher" : "minecraft:farmer";
         }
      } else {
         if (profession == 1) {
            return career == 2 ? "minecraft:cartographer" : "minecraft:librarian";
         }

         if (profession == 2) {
            return "minecraft:cleric";
         }

         if (profession == 3) {
            if (career == 2) {
               return "minecraft:weaponsmith";
            } else {
               return career == 3 ? "minecraft:toolsmith" : "minecraft:armorer";
            }
         } else if (profession == 4) {
            return career == 2 ? "minecraft:leatherworker" : "minecraft:butcher";
         } else {
            return profession == 5 ? "minecraft:nitwit" : "minecraft:none";
         }
      }
   }
}
