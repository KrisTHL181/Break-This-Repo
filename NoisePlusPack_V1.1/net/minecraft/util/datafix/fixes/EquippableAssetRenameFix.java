package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;

public class EquippableAssetRenameFix extends DataFix {
   public EquippableAssetRenameFix(Schema p_377108_) {
      super(p_377108_, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.DATA_COMPONENTS);
      OpticFinder<?> opticfinder = type.findField("minecraft:equippable");
      return this.fixTypeEverywhereTyped(
         "equippable asset rename fix",
         type,
         p_375525_ -> p_375525_.updateTyped(
            opticfinder, p_376406_ -> p_376406_.update(DSL.remainderFinder(), p_375389_ -> p_375389_.renameField("model", "asset_id"))
         )
      );
   }
}
