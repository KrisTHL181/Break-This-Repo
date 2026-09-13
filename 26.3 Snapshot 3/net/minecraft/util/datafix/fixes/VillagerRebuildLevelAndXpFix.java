package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.Mth;

public class VillagerRebuildLevelAndXpFix extends DataFix {
   private static final int TRADES_PER_LEVEL = 2;
   private static final int[] LEVEL_XP_THRESHOLDS = new int[]{0, 10, 50, 100, 150};

   public static int getMinXpPerLevel(final int level) {
      return LEVEL_XP_THRESHOLDS[Mth.clamp(level - 1, 0, LEVEL_XP_THRESHOLDS.length - 1)];
   }

   public VillagerRebuildLevelAndXpFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType);
   }

   public TypeRewriteRule makeRule() {
      Type<?> villagerType = this.getInputSchema().getChoiceType(References.ENTITY, "minecraft:villager");
      OpticFinder<?> entityF = DSL.namedChoice("minecraft:villager", villagerType);
      OpticFinder<?> offersF = villagerType.findField("Offers");
      Type<?> offersType = offersF.type();
      OpticFinder<?> recipeListF = offersType.findField("Recipes");
      ListType<?> recipeListType = (ListType<?>)recipeListF.type();
      OpticFinder<?> recipeF = recipeListType.getElement().finder();
      return this.fixTypeEverywhereTyped(
         "Villager level and xp rebuild",
         this.getInputSchema().getType(References.ENTITY),
         input -> input.updateTyped(
            entityF,
            villagerType,
            villager -> {
               Dynamic<?> remainder = (Dynamic<?>)villager.get(DSL.remainderFinder());
               int level = remainder.get("VillagerData").get("level").asInt(0);
               Typed<?> modifiedVillager = villager;
               if (level == 0 || level == 1) {
                  int offerCount = villager.getOptionalTyped(offersF)
                     .flatMap(o -> o.getOptionalTyped(recipeListF))
                     .map(recipeList -> recipeList.getAllTyped(recipeF).size())
                     .orElse(0);
                  level = Mth.clamp(offerCount / 2, 1, 5);
                  if (level > 1) {
                     modifiedVillager = addLevel(modifiedVillager, level);
                  }
               }

               Optional<Number> xp = remainder.get("Xp").asNumber().result();
               if (xp.isEmpty()) {
                  modifiedVillager = addXpFromLevel(modifiedVillager, level);
               }

               return modifiedVillager;
            }
         )
      );
   }

   private static Typed<?> addLevel(final Typed<?> villager, final int level) {
      return villager.update(
         DSL.remainderFinder(), remainder -> remainder.update("VillagerData", villagerData -> villagerData.set("level", villagerData.createInt(level)))
      );
   }

   private static Typed<?> addXpFromLevel(final Typed<?> villager, final int level) {
      int xp = getMinXpPerLevel(level);
      return villager.update(DSL.remainderFinder(), remainder -> remainder.set("Xp", remainder.createInt(xp)));
   }
}
