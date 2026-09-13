package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;

public class AddFlagIfNotPresentFix extends DataFix {
   private final String name;
   private final boolean flagValue;
   private final String flagKey;
   private final TypeReference typeReference;

   public AddFlagIfNotPresentFix(final Schema outputSchema, final TypeReference typeReference, final String flagKey, final boolean flagValue) {
      super(outputSchema, true);
      this.flagValue = flagValue;
      this.flagKey = flagKey;
      this.name = "AddFlagIfNotPresentFix_" + this.flagKey + "=" + this.flagValue + " for " + outputSchema.getVersionKey();
      this.typeReference = typeReference;
   }

   protected TypeRewriteRule makeRule() {
      Type<?> worldGenSettingsType = this.getInputSchema().getType(this.typeReference);
      return this.fixTypeEverywhereTyped(
         this.name,
         worldGenSettingsType,
         settings -> settings.update(
            DSL.remainderFinder(),
            tag -> tag.set(this.flagKey, (Dynamic)DataFixUtils.orElseGet(tag.get(this.flagKey).result(), () -> tag.createBoolean(this.flagValue)))
         )
      );
   }
}
