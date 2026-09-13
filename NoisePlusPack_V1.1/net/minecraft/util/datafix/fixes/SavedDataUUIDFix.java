package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import org.slf4j.Logger;

public class SavedDataUUIDFix extends AbstractUUIDFix {
   private static final Logger LOGGER = LogUtils.getLogger();

   public SavedDataUUIDFix(Schema p_16863_) {
      super(p_16863_, References.SAVED_DATA_RAIDS);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "SavedDataUUIDFix",
         this.getInputSchema().getType(this.typeReference),
         p_145672_ -> p_145672_.update(
            DSL.remainderFinder(),
            p_296635_ -> p_296635_.update(
               "data",
               p_145674_ -> p_145674_.update(
                  "Raids",
                  p_145676_ -> p_145676_.createList(
                     p_145676_.asStream()
                        .map(
                           p_145678_ -> p_145678_.update(
                              "HeroesOfTheVillage",
                              p_145680_ -> p_145680_.createList(
                                 p_145680_.asStream()
                                    .map(p_145682_ -> (Dynamic)createUUIDFromLongs((Dynamic<?>)p_145682_, "UUIDMost", "UUIDLeast").orElseGet(() -> {
                                       LOGGER.warn("HeroesOfTheVillage contained invalid UUIDs.");
                                       return (Dynamic<?>)p_145682_;
                                    }))
                              )
                           )
                        )
                  )
               )
            )
         )
      );
   }
}
