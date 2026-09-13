package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;

public class ScoreboardDisplayNameFix extends DataFix {
   private final String name;
   private final TypeReference type;

   public ScoreboardDisplayNameFix(Schema p_395509_, String p_393252_, TypeReference p_395348_) {
      super(p_395509_, false);
      this.name = p_393252_;
      this.type = p_395348_;
   }

   protected TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(this.type);
      OpticFinder<?> opticfinder = type.findField("DisplayName");
      OpticFinder<Pair<String, String>> opticfinder1 = DSL.typeFinder(this.getInputSchema().getType(References.TEXT_COMPONENT));
      return this.fixTypeEverywhereTyped(
         this.name,
         type,
         p_394780_ -> p_394780_.updateTyped(
            opticfinder, p_392050_ -> p_392050_.update(opticfinder1, p_395373_ -> p_395373_.mapSecond(LegacyComponentDataFixUtils::createTextComponentJson))
         )
      );
   }
}
