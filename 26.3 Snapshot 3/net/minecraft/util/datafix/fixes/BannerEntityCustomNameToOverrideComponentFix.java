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
   public BannerEntityCustomNameToOverrideComponentFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   public TypeRewriteRule makeRule() {
      Type<?> blockEntityType = this.getInputSchema().getType(References.BLOCK_ENTITY);
      TaggedChoiceType<?> blockEntityIdFinder = this.getInputSchema().findChoiceType(References.BLOCK_ENTITY);
      OpticFinder<?> customNameFinder = blockEntityType.findField("CustomName");
      OpticFinder<Pair<String, String>> textComponentFinder = DSL.typeFinder(this.getInputSchema().getType(References.TEXT_COMPONENT));
      return this.fixTypeEverywhereTyped("Banner entity custom_name to item_name component fix", blockEntityType, input -> {
         Object blockEntityId = ((Pair)input.get(blockEntityIdFinder.finder())).getFirst();
         return blockEntityId.equals("minecraft:banner") ? this.fix(input, textComponentFinder, customNameFinder) : input;
      });
   }

   private Typed<?> fix(final Typed<?> input, final OpticFinder<Pair<String, String>> textComponentFinder, final OpticFinder<?> customNameFinder) {
      Optional<String> customName = input.getOptionalTyped(customNameFinder).flatMap(name -> name.getOptional(textComponentFinder).map(Pair::getSecond));
      boolean isOminousBanner = customName.flatMap(LegacyComponentDataFixUtils::extractTranslationString)
         .filter(e -> e.equals("block.minecraft.ominous_banner"))
         .isPresent();
      return isOminousBanner
         ? Util.writeAndReadTypedOrThrow(
            input,
            input.getType(),
            dynamic -> {
               Dynamic<?> components = dynamic.createMap(
                  Map.of(
                     dynamic.createString("minecraft:item_name"),
                     dynamic.createString(customName.get()),
                     dynamic.createString("minecraft:hide_additional_tooltip"),
                     dynamic.emptyMap()
                  )
               );
               return dynamic.set("components", components).remove("CustomName");
            }
         )
         : input;
   }
}
