package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.function.UnaryOperator;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class AttributesRenameFix extends DataFix {
   private final String name;
   private final UnaryOperator<String> renames;

   public AttributesRenameFix(Schema p_364410_, String p_366408_, UnaryOperator<String> p_366138_) {
      super(p_364410_, false);
      this.name = p_366408_;
      this.renames = p_366138_;
   }

   protected TypeRewriteRule makeRule() {
      return TypeRewriteRule.seq(
         this.fixTypeEverywhereTyped(this.name + " (Components)", this.getInputSchema().getType(References.DATA_COMPONENTS), this::fixDataComponents),
         new TypeRewriteRule[]{
            this.fixTypeEverywhereTyped(this.name + " (Entity)", this.getInputSchema().getType(References.ENTITY), this::fixEntity),
            this.fixTypeEverywhereTyped(this.name + " (Player)", this.getInputSchema().getType(References.PLAYER), this::fixEntity)
         }
      );
   }

   private Typed<?> fixDataComponents(Typed<?> p_365772_) {
      return p_365772_.update(
         DSL.remainderFinder(),
         p_366382_ -> p_366382_.update(
            "minecraft:attribute_modifiers",
            p_369365_ -> p_369365_.update(
               "modifiers",
               p_366575_ -> (Dynamic)DataFixUtils.orElse(
                  p_366575_.asStreamOpt().result().map(p_363756_ -> p_363756_.map(this::fixTypeField)).map(p_366575_::createList), p_366575_
               )
            )
         )
      );
   }

   private Typed<?> fixEntity(Typed<?> p_364704_) {
      return p_364704_.update(
         DSL.remainderFinder(),
         p_362710_ -> p_362710_.update(
            "attributes",
            p_366472_ -> (Dynamic)DataFixUtils.orElse(
               p_366472_.asStreamOpt().result().map(p_362930_ -> p_362930_.map(this::fixIdField)).map(p_366472_::createList), p_366472_
            )
         )
      );
   }

   private Dynamic<?> fixIdField(Dynamic<?> p_366579_) {
      return ExtraDataFixUtils.fixStringField(p_366579_, "id", this.renames);
   }

   private Dynamic<?> fixTypeField(Dynamic<?> p_367667_) {
      return ExtraDataFixUtils.fixStringField(p_367667_, "type", this.renames);
   }
}
