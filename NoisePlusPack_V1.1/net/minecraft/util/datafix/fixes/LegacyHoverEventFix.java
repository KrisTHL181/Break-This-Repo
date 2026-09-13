package net.minecraft.util.datafix.fixes;

import com.google.gson.JsonElement;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Util;

public class LegacyHoverEventFix extends DataFix {
   public LegacyHoverEventFix(Schema p_393665_) {
      super(p_393665_, false);
   }

   protected TypeRewriteRule makeRule() {
      Type<? extends Pair<String, ?>> type = this.getInputSchema().getType(References.TEXT_COMPONENT).findFieldType("hoverEvent");
      return this.createFixer(this.getInputSchema().getTypeRaw(References.TEXT_COMPONENT), type);
   }

   private <C, H extends Pair<String, ?>> TypeRewriteRule createFixer(Type<C> p_397270_, Type<H> p_394348_) {
      Type<Pair<String, Either<Either<String, List<C>>, Pair<Either<List<C>, Unit>, Pair<Either<C, Unit>, Pair<Either<H, Unit>, Dynamic<?>>>>>>> type = DSL.named(
         References.TEXT_COMPONENT.typeName(),
         DSL.or(
            DSL.or(DSL.string(), DSL.list(p_397270_)),
            DSL.and(
               DSL.optional(DSL.field("extra", DSL.list(p_397270_))),
               DSL.optional(DSL.field("separator", p_397270_)),
               DSL.optional(DSL.field("hoverEvent", p_394348_)),
               DSL.remainderType()
            )
         )
      );
      if (!type.equals(this.getInputSchema().getType(References.TEXT_COMPONENT))) {
         throw new IllegalStateException(
            "Text component type did not match, expected " + type + " but got " + this.getInputSchema().getType(References.TEXT_COMPONENT)
         );
      } else {
         return this.fixTypeEverywhere(
            "LegacyHoverEventFix",
            type,
            p_394382_ -> p_395778_ -> p_395778_.mapSecond(
               p_391228_ -> p_391228_.mapRight(p_395158_ -> p_395158_.mapSecond(p_395579_ -> p_395579_.mapSecond(p_395788_ -> {
                  Dynamic<?> dynamic = (Dynamic<?>)p_395788_.getSecond();
                  Optional<? extends Dynamic<?>> optional = dynamic.get("hoverEvent").result();
                  if (optional.isEmpty()) {
                     return p_395788_;
                  }

                  Optional<? extends Dynamic<?>> optional1 = optional.get().get("value").result();
                  if (optional1.isEmpty()) {
                     return p_395788_;
                  }

                  String s = ((Either)p_395788_.getFirst()).left().<String>map(Pair::getFirst).orElse("");
                  H h = this.fixHoverEvent(p_394348_, s, (Dynamic<?>)optional.get());
                  return p_395788_.mapFirst(p_391455_ -> Either.left(h));
               })))
            )
         );
      }
   }

   private <H> H fixHoverEvent(Type<H> p_393466_, String p_396088_, Dynamic<?> p_392996_) {
      return "show_text".equals(p_396088_) ? fixShowTextHover(p_393466_, p_392996_) : createPlaceholderHover(p_393466_, p_392996_);
   }

   private static <H> H fixShowTextHover(Type<H> p_395847_, Dynamic<?> p_393935_) {
      Dynamic<?> dynamic = p_393935_.renameField("value", "contents");
      return (H)Util.readTypedOrThrow(p_395847_, dynamic).getValue();
   }

   private static <H> H createPlaceholderHover(Type<H> p_394355_, Dynamic<?> p_393524_) {
      JsonElement jsonelement = (JsonElement)p_393524_.convert(JsonOps.INSTANCE).getValue();
      Dynamic<?> dynamic = new Dynamic(
         JavaOps.INSTANCE,
         Map.of("action", "show_text", "contents", Map.<String, String>of("text", "Legacy hoverEvent: " + GsonHelper.toStableString(jsonelement)))
      );
      return (H)Util.readTypedOrThrow(p_394355_, dynamic).getValue();
   }
}
