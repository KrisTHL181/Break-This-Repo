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

   public DropInvalidSignDataFix(Schema p_297458_, String p_300331_) {
      super(p_297458_, false);
      this.entityName = p_300331_;
   }

   private <T> Dynamic<T> fix(Dynamic<T> p_297398_) {
      p_297398_ = p_297398_.update("front_text", DropInvalidSignDataFix::fixText);
      p_297398_ = p_297398_.update("back_text", DropInvalidSignDataFix::fixText);

      for (String s : BlockEntitySignDoubleSidedEditableTextFix.FIELDS_TO_DROP) {
         p_297398_ = p_297398_.remove(s);
      }

      return p_297398_;
   }

   private static <T> Dynamic<T> fixText(Dynamic<T> p_299128_) {
      Optional<Stream<Dynamic<T>>> optional = p_299128_.get("filtered_messages").asStreamOpt().result();
      if (optional.isEmpty()) {
         return p_299128_;
      }

      Dynamic<T> dynamic = LegacyComponentDataFixUtils.createEmptyComponent(p_299128_.getOps());
      List<Dynamic<T>> list = p_299128_.get("messages").asStreamOpt().result().orElse(Stream.of()).toList();
      List<Dynamic<T>> list1 = Streams.mapWithIndex(optional.get(), (p_298117_, p_298041_) -> {
         Dynamic<T> dynamic1 = p_298041_ < list.size() ? list.get((int)p_298041_) : dynamic;
         return p_298117_.equals(dynamic) ? dynamic1 : p_298117_;
      }).toList();
      return list1.equals(list) ? p_299128_.remove("filtered_messages") : p_299128_.set("filtered_messages", p_299128_.createList(list1.stream()));
   }

   public TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.BLOCK_ENTITY);
      Type<?> type1 = this.getInputSchema().getChoiceType(References.BLOCK_ENTITY, this.entityName);
      OpticFinder<?> opticfinder = DSL.namedChoice(this.entityName, type1);
      return this.fixTypeEverywhereTyped(
         "DropInvalidSignDataFix for " + this.entityName,
         type,
         p_390233_ -> p_390233_.updateTyped(
            opticfinder,
            type1,
            p_449307_ -> {
               boolean flag = ((Dynamic)p_449307_.get(DSL.remainderFinder())).get("_filtered_correct").asBoolean(false);
               return flag
                  ? p_449307_.update(DSL.remainderFinder(), p_390228_ -> p_390228_.remove("_filtered_correct"))
                  : Util.writeAndReadTypedOrThrow(p_449307_, type1, this::fix);
            }
         )
      );
   }
}
