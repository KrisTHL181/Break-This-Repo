package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;

public class EquippableAssetRenameFix extends DataFix {
   public EquippableAssetRenameFix(final Schema outputSchema) {
      super(outputSchema, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> componentsType = this.getInputSchema().getType(References.DATA_COMPONENTS);
      OpticFinder<?> equippableField = componentsType.findField("minecraft:equippable");
      return this.fixTypeEverywhereTyped(
         "equippable asset rename fix",
         componentsType,
         components -> components.updateTyped(
            equippableField, equippable -> equippable.update(DSL.remainderFinder(), tag -> tag.renameField("model", "asset_id"))
         )
      );
   }
}
