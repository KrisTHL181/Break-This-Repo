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
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class OminousBannerRarityFix extends DataFix {
   public OminousBannerRarityFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   public TypeRewriteRule makeRule() {
      Type<?> blockEntityType = this.getInputSchema().getType(References.BLOCK_ENTITY);
      Type<?> itemStackType = this.getInputSchema().getType(References.ITEM_STACK);
      TaggedChoiceType<?> blockEntityIdFinder = this.getInputSchema().findChoiceType(References.BLOCK_ENTITY);
      OpticFinder<Pair<String, String>> itemStackIdFinder = DSL.fieldFinder(
         "id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString())
      );
      OpticFinder<?> blockEntityComponentsFieldFinder = blockEntityType.findField("components");
      OpticFinder<?> itemStackComponentsFieldFinder = itemStackType.findField("components");
      OpticFinder<?> itemNameFinder = blockEntityComponentsFieldFinder.type().findField("minecraft:item_name");
      OpticFinder<Pair<String, String>> textComponentFinder = DSL.typeFinder(this.getInputSchema().getType(References.TEXT_COMPONENT));
      return TypeRewriteRule.seq(this.fixTypeEverywhereTyped("Ominous Banner block entity common rarity to uncommon rarity fix", blockEntityType, input -> {
         Object blockEntityId = ((Pair)input.get(blockEntityIdFinder.finder())).getFirst();
         return blockEntityId.equals("minecraft:banner") ? this.fix(input, blockEntityComponentsFieldFinder, itemNameFinder, textComponentFinder) : input;
      }), this.fixTypeEverywhereTyped("Ominous Banner item stack common rarity to uncommon rarity fix", itemStackType, input -> {
         String itemStackId = input.getOptional(itemStackIdFinder).<String>map(Pair::getSecond).orElse("");
         return itemStackId.equals("minecraft:white_banner") ? this.fix(input, itemStackComponentsFieldFinder, itemNameFinder, textComponentFinder) : input;
      }));
   }

   private Typed<?> fix(
      final Typed<?> input,
      final OpticFinder<?> componentsFieldFinder,
      final OpticFinder<?> itemNameFinder,
      final OpticFinder<Pair<String, String>> textComponentFinder
   ) {
      return input.updateTyped(
         componentsFieldFinder,
         components -> {
            boolean isOminousBanner = components.getOptionalTyped(itemNameFinder)
               .flatMap(itemName -> itemName.getOptional(textComponentFinder))
               .<String>map(Pair::getSecond)
               .flatMap(LegacyComponentDataFixUtils::extractTranslationString)
               .filter(e -> e.equals("block.minecraft.ominous_banner"))
               .isPresent();
            return isOminousBanner
               ? components.updateTyped(
                     itemNameFinder,
                     itemName -> itemName.set(
                        textComponentFinder,
                        Pair.of(
                           References.TEXT_COMPONENT.typeName(), LegacyComponentDataFixUtils.createTranslatableComponentJson("block.minecraft.ominous_banner")
                        )
                     )
                  )
                  .update(DSL.remainderFinder(), remainder -> remainder.set("minecraft:rarity", remainder.createString("uncommon")))
               : components;
         }
      );
   }
}
