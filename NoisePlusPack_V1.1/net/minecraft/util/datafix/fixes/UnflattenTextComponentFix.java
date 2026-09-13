package net.minecraft.util.datafix.fixes;

import com.google.gson.JsonElement;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Util;
import org.slf4j.Logger;

public class UnflattenTextComponentFix extends DataFix {
   private static final Logger LOGGER = LogUtils.getLogger();

   public UnflattenTextComponentFix(Schema p_397820_) {
      super(p_397820_, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<Pair<String, String>> type = this.getInputSchema().getType(References.TEXT_COMPONENT);
      Type<?> type1 = this.getOutputSchema().getType(References.TEXT_COMPONENT);
      return this.createFixer(type, type1);
   }

   private <T> TypeRewriteRule createFixer(Type<Pair<String, String>> p_393379_, Type<T> p_394215_) {
      return this.fixTypeEverywhere(
         "UnflattenTextComponentFix",
         p_393379_,
         p_394215_,
         p_394708_ -> p_449331_ -> Util.readTypedOrThrow(p_394215_, unflattenJson(p_394708_, (String)p_449331_.getSecond()), true).getValue()
      );
   }

   private static <T> Dynamic<T> unflattenJson(DynamicOps<T> p_392385_, String p_391255_) {
      try {
         JsonElement jsonelement = LenientJsonParser.parse(p_391255_);
         if (!jsonelement.isJsonNull()) {
            return new Dynamic(p_392385_, JsonOps.INSTANCE.convertTo(p_392385_, jsonelement));
         }
      } catch (Exception exception) {
         LOGGER.error("Failed to unflatten text component json: {}", p_391255_, exception);
      }

      return new Dynamic(p_392385_, p_392385_.createString(p_391255_));
   }
}
