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
   public TextComponentHoverAndClickEventFix(final Schema outputSchema) {
      super(outputSchema, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<? extends Pair<String, ?>> hoverEventType = this.getInputSchema().getType(References.TEXT_COMPONENT).findFieldType("hoverEvent");
      return this.createFixer(
         this.getInputSchema().getTypeRaw(References.TEXT_COMPONENT), this.getOutputSchema().getType(References.TEXT_COMPONENT), hoverEventType
      );
   }

   private <C1, C2, H extends Pair<String, ?>> TypeRewriteRule createFixer(
      final Type<C1> oldRawTextComponentType, final Type<C2> newTextComponentType, final Type<H> hoverEventType
   ) {
      Type<Pair<String, Either<Either<String, List<C1>>, Pair<Either<List<C1>, Unit>, Pair<Either<C1, Unit>, Pair<Either<H, Unit>, Dynamic<?>>>>>>> oldTextComponentType = DSL.named(
         References.TEXT_COMPONENT.typeName(),
         DSL.or(
            DSL.or(DSL.string(), DSL.list(oldRawTextComponentType)),
            DSL.and(
               DSL.optional(DSL.field("extra", DSL.list(oldRawTextComponentType))),
               DSL.optional(DSL.field("separator", oldRawTextComponentType)),
               DSL.optional(DSL.field("hoverEvent", hoverEventType)),
               DSL.remainderType()
            )
         )
      );
      if (!oldTextComponentType.equals(this.getInputSchema().getType(References.TEXT_COMPONENT))) {
         throw new IllegalStateException(
            "Text component type did not match, expected " + oldTextComponentType + " but got " + this.getInputSchema().getType(References.TEXT_COMPONENT)
         );
      }

      Type<?> patchedInputType = ExtraDataFixUtils.patchSubType(oldTextComponentType, oldTextComponentType, newTextComponentType);
      return this.fixTypeEverywhere(
         "TextComponentHoverAndClickEventFix",
         oldTextComponentType,
         newTextComponentType,
         ops -> textComponent -> {
            boolean hasHoverOrClick = (Boolean)((Either)textComponent.getSecond()).map(simple -> false, full -> {
               Pair<Either<H, Unit>, Dynamic<?>> hoverAndRemainder = (Pair<Either<H, Unit>, Dynamic<?>>)((Pair)full.getSecond()).getSecond();
               boolean hasHover = ((Either)hoverAndRemainder.getFirst()).left().isPresent();
               boolean hasClick = ((Dynamic)hoverAndRemainder.getSecond()).get("clickEvent").result().isPresent();
               return hasHover || hasClick;
            });
            return !hasHoverOrClick
               ? textComponent
               : Util.writeAndReadTypedOrThrow(
                     ExtraDataFixUtils.cast(patchedInputType, textComponent, ops), newTextComponentType, TextComponentHoverAndClickEventFix::fixTextComponent
                  )
                  .getValue();
         }
      );
   }

   private static Dynamic<?> fixTextComponent(final Dynamic<?> dynamic) {
      return dynamic.renameAndFixField("hoverEvent", "hover_event", TextComponentHoverAndClickEventFix::fixHoverEvent)
         .renameAndFixField("clickEvent", "click_event", TextComponentHoverAndClickEventFix::fixClickEvent);
   }

   private static Dynamic<?> copyFields(Dynamic<?> target, final Dynamic<?> source, final String... fields) {
      for (String field : fields) {
         target = Dynamic.copyField(source, field, target, field);
      }

      return target;
   }

   private static Dynamic<?> fixHoverEvent(final Dynamic<?> dynamic) {
      String action = dynamic.get("action").asString("");

      return switch (action) {
         case "show_text" -> dynamic.renameField("contents", "value");
         case "show_item" -> {
            Dynamic<?> contents = dynamic.get("contents").orElseEmptyMap();
            yield copyFields(dynamic.remove("contents"), contents, "id", "count", "components");
         }
         case "show_entity" -> {
            Dynamic<?> contents = dynamic.get("contents").orElseEmptyMap();
            yield copyFields(dynamic.remove("contents"), contents, "id", "type", "name").renameField("id", "uuid").renameField("type", "id");
         }
         default -> dynamic;
      };
   }

   private static <T> @Nullable Dynamic<T> fixClickEvent(final Dynamic<T> dynamic) {
      String action = dynamic.get("action").asString("");
      String value = dynamic.get("value").asString("");

      return switch (action) {
         case "open_url" -> !validateUri(value) ? null : dynamic.renameField("value", "url");
         case "open_file" -> dynamic.renameField("value", "path");
         case "run_command", "suggest_command" -> !validateChat(value) ? null : dynamic.renameField("value", "command");
         case "change_page" -> {
            Integer oldPage = dynamic.get("value").result().map(TextComponentHoverAndClickEventFix::parseOldPage).orElse(null);
            if (oldPage == null) {
               yield null;
            } else {
               int page = Math.max(oldPage, 1);
               yield dynamic.remove("value").set("page", dynamic.createInt(page));
            }
         }
         default -> dynamic;
      };
   }

   private static @Nullable Integer parseOldPage(final Dynamic<?> value) {
      Optional<Number> numberValue = value.asNumber().result();
      if (numberValue.isPresent()) {
         return numberValue.get().intValue();
      }

      try {
         return Integer.parseInt(value.asString(""));
      } catch (Exception ignored) {
         return null;
      }
   }

   private static boolean validateUri(final String uri) {
      try {
         URI parsedUri = new URI(uri);
         String scheme = parsedUri.getScheme();
         if (scheme == null) {
            return false;
         }

         String protocol = scheme.toLowerCase(Locale.ROOT);
         return "http".equals(protocol) || "https".equals(protocol);
      } catch (URISyntaxException e) {
         return false;
      }
   }

   private static boolean validateChat(final String string) {
      for (int i = 0; i < string.length(); i++) {
         char c = string.charAt(i);
         if (c == 167 || c < ' ' || c == 127) {
            return false;
         }
      }

      return true;
   }
}
