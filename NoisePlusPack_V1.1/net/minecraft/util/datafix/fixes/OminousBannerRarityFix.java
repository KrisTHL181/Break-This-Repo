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
   public OminousBannerRarityFix(Schema p_362925_) {
      super(p_362925_, false);
   }

   public TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.BLOCK_ENTITY);
      Type<?> type1 = this.getInputSchema().getType(References.ITEM_STACK);
      TaggedChoiceType<?> taggedchoicetype = this.getInputSchema().findChoiceType(References.BLOCK_ENTITY);
      OpticFinder<Pair<String, String>> opticfinder = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
      OpticFinder<?> opticfinder1 = type.findField("components");
      OpticFinder<?> opticfinder2 = type1.findField("components");
      OpticFinder<?> opticfinder3 = opticfinder1.type().findField("minecraft:item_name");
      OpticFinder<Pair<String, String>> opticfinder4 = DSL.typeFinder(this.getInputSchema().getType(References.TEXT_COMPONENT));
      return TypeRewriteRule.seq(this.fixTypeEverywhereTyped("Ominous Banner block entity common rarity to uncommon rarity fix", type, p_390330_ -> {
         Object object = ((Pair)p_390330_.get(taggedchoicetype.finder())).getFirst();
         return object.equals("minecraft:banner") ? this.fix(p_390330_, opticfinder1, opticfinder3, opticfinder4) : p_390330_;
      }), this.fixTypeEverywhereTyped("Ominous Banner item stack common rarity to uncommon rarity fix", type1, p_390324_ -> {
         String s = p_390324_.getOptional(opticfinder).<String>map(Pair::getSecond).orElse("");
         return s.equals("minecraft:white_banner") ? this.fix(p_390324_, opticfinder2, opticfinder3, opticfinder4) : p_390324_;
      }));
   }

   private Typed<?> fix(Typed<?> p_362105_, OpticFinder<?> p_363836_, OpticFinder<?> p_396185_, OpticFinder<Pair<String, String>> p_392457_) {
      return p_362105_.updateTyped(
         p_363836_,
         p_390319_ -> {
            boolean flag = p_390319_.getOptionalTyped(p_396185_)
               .flatMap(p_390332_ -> p_390332_.getOptional(p_392457_))
               .<String>map(Pair::getSecond)
               .flatMap(LegacyComponentDataFixUtils::extractTranslationString)
               .filter(p_368287_ -> p_368287_.equals("block.minecraft.ominous_banner"))
               .isPresent();
            return flag
               ? p_390319_.updateTyped(
                     p_396185_,
                     p_390316_ -> p_390316_.set(
                        p_392457_,
                        Pair.of(
                           References.TEXT_COMPONENT.typeName(), LegacyComponentDataFixUtils.createTranslatableComponentJson("block.minecraft.ominous_banner")
                        )
                     )
                  )
                  .update(DSL.remainderFinder(), p_390325_ -> p_390325_.set("minecraft:rarity", p_390325_.createString("uncommon")))
               : p_390319_;
         }
      );
   }
}
