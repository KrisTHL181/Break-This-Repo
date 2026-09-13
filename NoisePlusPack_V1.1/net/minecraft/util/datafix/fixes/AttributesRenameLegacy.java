package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.function.UnaryOperator;

public class AttributesRenameLegacy extends DataFix {
   private final String name;
   private final UnaryOperator<String> renames;

   public AttributesRenameLegacy(Schema p_369510_, String p_366609_, UnaryOperator<String> p_366659_) {
      super(p_369510_, false);
      this.name = p_366609_;
      this.renames = p_366659_;
   }

   protected TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.ITEM_STACK);
      OpticFinder<?> opticfinder = type.findField("tag");
      return TypeRewriteRule.seq(
         this.fixTypeEverywhereTyped(this.name + " (ItemStack)", type, p_361468_ -> p_361468_.updateTyped(opticfinder, this::fixItemStackTag)),
         new TypeRewriteRule[]{
            this.fixTypeEverywhereTyped(this.name + " (Entity)", this.getInputSchema().getType(References.ENTITY), this::fixEntity),
            this.fixTypeEverywhereTyped(this.name + " (Player)", this.getInputSchema().getType(References.PLAYER), this::fixEntity)
         }
      );
   }

   private Dynamic<?> fixName(Dynamic<?> p_362328_) {
      return (Dynamic<?>)DataFixUtils.orElse(p_362328_.asString().result().map(this.renames).map(p_362328_::createString), p_362328_);
   }

   private Typed<?> fixItemStackTag(Typed<?> p_369182_) {
      return p_369182_.update(
         DSL.remainderFinder(),
         p_365782_ -> p_365782_.update(
            "AttributeModifiers",
            p_361291_ -> (Dynamic)DataFixUtils.orElse(
               p_361291_.asStreamOpt()
                  .result()
                  .map(p_368448_ -> p_368448_.map(p_363415_ -> p_363415_.update("AttributeName", this::fixName)))
                  .map(p_361291_::createList),
               p_361291_
            )
         )
      );
   }

   private Typed<?> fixEntity(Typed<?> p_360706_) {
      return p_360706_.update(
         DSL.remainderFinder(),
         p_370146_ -> p_370146_.update(
            "Attributes",
            p_369341_ -> (Dynamic)DataFixUtils.orElse(
               p_369341_.asStreamOpt()
                  .result()
                  .map(p_361263_ -> p_361263_.map(p_362038_ -> p_362038_.update("Name", this::fixName)))
                  .map(p_369341_::createList),
               p_369341_
            )
         )
      );
   }
}
