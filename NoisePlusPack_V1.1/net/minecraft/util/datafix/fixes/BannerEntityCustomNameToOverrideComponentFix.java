package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.Optional;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;

public class BannerEntityCustomNameToOverrideComponentFix extends DataFix {
   public BannerEntityCustomNameToOverrideComponentFix(Schema p_335786_) {
      super(p_335786_, false);
   }

   public TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.BLOCK_ENTITY);
      TaggedChoiceType<?> taggedchoicetype = this.getInputSchema().findChoiceType(References.BLOCK_ENTITY);
      OpticFinder<?> opticfinder = type.findField("CustomName");
      OpticFinder<Pair<String, String>> opticfinder1 = DSL.typeFinder(this.getInputSchema().getType(References.TEXT_COMPONENT));
      return this.fixTypeEverywhereTyped("Banner entity custom_name to item_name component fix", type, p_390222_ -> {
         Object object = ((Pair)p_390222_.get(taggedchoicetype.finder())).getFirst();
         return object.equals("minecraft:banner") ? this.fix(p_390222_, opticfinder1, opticfinder) : p_390222_;
      });
   }

   private Typed<?> fix(Typed<?> p_328297_, OpticFinder<Pair<String, String>> p_334644_, OpticFinder<?> p_391749_) {
      Optional<String> optional = p_328297_.getOptionalTyped(p_391749_).flatMap(p_390218_ -> p_390218_.getOptional(p_334644_).map(Pair::getSecond));
      boolean flag = optional.flatMap(LegacyComponentDataFixUtils::extractTranslationString)
         .filter(p_334057_ -> p_334057_.equals("block.minecraft.ominous_banner"))
         .isPresent();
      return flag
         ? Util.writeAndReadTypedOrThrow(
            p_328297_,
            p_328297_.getType(),
            p_390216_ -> {
               Dynamic<?> dynamic = p_390216_.createMap(
                  Map.of(
                     p_390216_.createString("minecraft:item_name"),
                     p_390216_.createString(optional.get()),
                     p_390216_.createString("minecraft:hide_additional_tooltip"),
                     p_390216_.emptyMap()
                  )
               );
               return p_390216_.set("components", dynamic).remove("CustomName");
            }
         )
         : p_328297_;
   }
}
