package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class LevelDatDifficultyFix extends DataFix {
   public LevelDatDifficultyFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "LevelDatDifficultyFix",
         this.getInputSchema().getType(References.LIGHTWEIGHT_LEVEL),
         input -> input.update(
            DSL.remainderFinder(),
            levelData -> {
               int difficulty = levelData.get("Difficulty").asInt(2);

               String newDifficulty = switch (difficulty) {
                  case 0 -> "peaceful";
                  case 1 -> "easy";
                  default -> "normal";
                  case 3 -> "hard";
               };
               Dynamic<?> difficultySettings = levelData.emptyMap()
                  .set("difficulty", levelData.createString(newDifficulty))
                  .set("hardcore", levelData.createBoolean(levelData.get("hardcore").asBoolean(false)))
                  .set("locked", levelData.createBoolean(levelData.get("DifficultyLocked").asBoolean(false)));
               return levelData.set("difficulty_settings", difficultySettings).remove("Difficulty").remove("hardcore").remove("DifficultyLocked");
            }
         )
      );
   }
}
