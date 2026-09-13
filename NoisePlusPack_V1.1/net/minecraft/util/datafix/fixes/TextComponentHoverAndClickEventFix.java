package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import org.jspecify.annotations.Nullable;

public class TextComponentHoverAndClickEventFix extends DataFix {
   public TextComponentHoverAndClickEventFix(Schema p_392221_) {
      super(p_392221_, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<? extends Pair<String, ?>> type = this.getInputSchema().getType(References.TEXT_COMPONENT).findFieldType("hoverEvent");
      return this.createFixer(this.getInputSchema().getTypeRaw(References.TEXT_COMPONENT), this.getOutputSchema().getType(References.TEXT_COMPONENT), type);
   }

   private <C1, C2, H extends Pair<String, ?>> TypeRewriteRule createFixer(Type<C1> p_394191_, Type<C2> p_396398_, Type<H> p_393938_) {
      Type<Pair<String, Either<Either<String, List<C1>>, Pair<Either<List<C1>, Unit>, Pair<Either<C1, Unit>, Pair<Either<H, Unit>, Dynamic<?>>>>>>> type = DSL.named(
         References.TEXT_COMPONENT.typeName(),
         DSL.or(
            DSL.or(DSL.string(), DSL.list(p_394191_)),
            DSL.and(
               DSL.optional(DSL.field("extra", DSL.list(p_394191_))),
               DSL.optional(DSL.field("separator", p_394191_)),
               DSL.optional(DSL.field("hoverEvent", p_393938_)),
               DSL.remainderType()
            )
         )
      );
      if (!type.equals(this.getInputSchema().getType(References.TEXT_COMPONENT))) {
         throw new IllegalStateException(
            "Text component type did not match, expected " + type + " but got " + this.getInputSchema().getType(References.TEXT_COMPONENT)
         );
      }

      Type<?> type1 = ExtraDataFixUtils.patchSubType(type, type, p_396398_);
      return this.fixTypeEverywhere(
         "TextComponentHoverAndClickEventFix",
         type,
         p_396398_,
         p_392390_ -> p_449324_ -> {
            boolean flag = (Boolean)((Either)p_449324_.getSecond()).map(p_396868_ -> false, p_392755_ -> {
               Pair<Either<H, Unit>, Dynamic<?>> pair = (Pair<Either<H, Unit>, Dynamic<?>>)((Pair)p_392755_.getSecond()).getSecond();
               boolean flag1 = ((Either)pair.getFirst()).left().isPresent();
               boolean flag2 = ((Dynamic)pair.getSecond()).get("clickEvent").result().isPresent();
               return flag1 || flag2;
            });
            return !flag
               ? p_449324_
               : Util.writeAndReadTypedOrThrow(
                     ExtraDataFixUtils.cast(type1, p_449324_, p_392390_), p_396398_, TextComponentHoverAndClickEventFix::fixTextComponent
                  )
                  .getValue();
         }
      );
   }

   private static Dynamic<?> fixTextComponent(Dynamic<?> p_393778_) {
      return p_393778_.renameAndFixField("hoverEvent", "hover_event", TextComponentHoverAndClickEventFix::fixHoverEvent)
         .renameAndFixField("clickEvent", "click_event", TextComponentHoverAndClickEventFix::fixClickEvent);
   }

   private static Dynamic<?> copyFields(Dynamic<?> p_394161_, Dynamic<?> p_393015_, String... p_396190_) {
      for (String s : p_396190_) {
         p_394161_ = Dynamic.copyField(p_393015_, s, p_394161_, s);
      }

      return p_394161_;
   }

   private static Dynamic<?> fixHoverEvent(Dynamic<?> p_393225_) {
      String s = p_393225_.get("action").asString("");

      return switch (s) {
         case "show_text" -> p_393225_.renameField("contents", "value");
         case "show_item" -> {
            Dynamic<?> dynamic1 = p_393225_.get("contents").orElseEmptyMap();
            Optional<String> optional = dynamic1.asString().result();
            yield optional.isPresent()
               ? p_393225_.renameField("contents", "id")
               : copyFields(p_393225_.remove("contents"), dynamic1, "id", "count", "components");
         }
         case "show_entity" -> {
            Dynamic<?> dynamic = p_393225_.get("contents").orElseEmptyMap();
            yield copyFields(p_393225_.remove("contents"), dynamic, "id", "type", "name").renameField("id", "uuid").renameField("type", "id");
         }
         default -> p_393225_;
      };
   }

   private static <T> @Nullable Dynamic<T> fixClickEvent(Dynamic<T> p_397814_) {
      String s = p_397814_.get("action").asString("");
      String s1 = p_397814_.get("value").asString("");

      return switch (s) {
         case "open_url" -> !validateUri(s1) ? null : p_397814_.renameField("value", "url");
         case "open_file" -> p_397814_.renameField("value", "path");
         case "run_command", "suggest_command" -> !validateChat(s1) ? null : p_397814_.renameField("value", "command");
         case "change_page" -> {
            Integer integer = p_397814_.get("value").result().map(TextComponentHoverAndClickEventFix::parseOldPage).orElse(null);
            if (integer == null) {
               yield null;
            } else {
               int i = Math.max(integer, 1);
               yield p_397814_.remove("value").set("page", p_397814_.createInt(i));
            }
         }
         default -> p_397814_;
      };
   }

   private static @Nullable Integer parseOldPage(Dynamic<?> p_396464_) {
      Optional<Number> optional = p_396464_.asNumber().result();
      if (optional.isPresent()) {
         return optional.get().intValue();
      }

      try {
         return Integer.parseInt(p_396464_.asString(""));
      } catch (Exception exception) {
         return null;
      }
   }

   private static boolean validateUri(String p_395339_) {
      try {
         URI uri = new URI(p_395339_);
         String s = uri.getScheme();
         if (s == null) {
            return false;
         }

         String s1 = s.toLowerCase(Locale.ROOT);
         return "http".equals(s1) || "https".equals(s1);
      } catch (URISyntaxException urisyntaxexception) {
         return false;
      }
   }

   private static boolean validateChat(String p_394600_) {
      for (int i = 0; i < p_394600_.length(); i++) {
         char c0 = p_394600_.charAt(i);
         if (c0 == 167 || c0 < ' ' || c0 == 127) {
            return false;
         }
      }

      return true;
   }
}
