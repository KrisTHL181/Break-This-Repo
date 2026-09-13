package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Streams;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;

public class DropInvalidSignDataFix extends DataFix {
   private final String entityName;

   public DropInvalidSignDataFix(final Schema outputSchema, final String entityName) {
      super(outputSchema, false);
      this.entityName = entityName;
   }

   private <T> Dynamic<T> fix(Dynamic<T> tag) {
      tag = tag.update("front_text", DropInvalidSignDataFix::fixText);
      tag = tag.update("back_text", DropInvalidSignDataFix::fixText);

      for (String field : BlockEntitySignDoubleSidedEditableTextFix.FIELDS_TO_DROP) {
         tag = tag.remove(field);
      }

      return tag;
   }

   private static <T> Dynamic<T> fixText(final Dynamic<T> tag) {
      Optional<Stream<Dynamic<T>>> filteredLines = tag.get("filtered_messages").asStreamOpt().result();
      if (filteredLines.isEmpty()) {
         return tag;
      }

      Dynamic<T> emptyComponent = LegacyComponentDataFixUtils.createEmptyComponent(tag.getOps());
      List<Dynamic<T>> lines = tag.get("messages").asStreamOpt().result().orElse(Stream.of()).toList();
      List<Dynamic<T>> newFilteredLines = Streams.mapWithIndex(filteredLines.get(), (line, index) -> {
         Dynamic<T> fallbackLine = index < lines.size() ? lines.get((int)index) : emptyComponent;
         return line.equals(emptyComponent) ? fallbackLine : line;
      }).toList();
      return newFilteredLines.equals(lines) ? tag.remove("filtered_messages") : tag.set("filtered_messages", tag.createList(newFilteredLines.stream()));
   }

   public TypeRewriteRule makeRule() {
      Type<?> entityType = this.getInputSchema().getType(References.BLOCK_ENTITY);
      Type<?> entityChoiceType = this.getInputSchema().getChoiceType(References.BLOCK_ENTITY, this.entityName);
      OpticFinder<?> entityF = DSL.namedChoice(this.entityName, entityChoiceType);
      return this.fixTypeEverywhereTyped(
         "DropInvalidSignDataFix for " + this.entityName,
         entityType,
         input -> input.updateTyped(
            entityF,
            entityChoiceType,
            entity -> {
               boolean filteredCorrect = ((Dynamic)entity.get(DSL.remainderFinder())).get("_filtered_correct").asBoolean(false);
               return filteredCorrect
                  ? entity.update(DSL.remainderFinder(), remainder -> remainder.remove("_filtered_correct"))
                  : Util.writeAndReadTypedOrThrow(entity, entityChoiceType, this::fix);
            }
         )
      );
   }
}
