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

   public AttributesRenameLegacy(final Schema outputSchema, final String name, final UnaryOperator<String> renames) {
      super(outputSchema, false);
      this.name = name;
      this.renames = renames;
   }

   protected TypeRewriteRule makeRule() {
      Type<?> itemStackType = this.getInputSchema().getType(References.ITEM_STACK);
      OpticFinder<?> tagF = itemStackType.findField("tag");
      return TypeRewriteRule.seq(
         this.fixTypeEverywhereTyped(this.name + " (ItemStack)", itemStackType, itemStack -> itemStack.updateTyped(tagF, this::fixItemStackTag)),
         new TypeRewriteRule[]{
            this.fixTypeEverywhereTyped(this.name + " (Entity)", this.getInputSchema().getType(References.ENTITY), this::fixEntity),
            this.fixTypeEverywhereTyped(this.name + " (Player)", this.getInputSchema().getType(References.PLAYER), this::fixEntity)
         }
      );
   }

   private Dynamic<?> fixName(final Dynamic<?> name) {
      return (Dynamic<?>)DataFixUtils.orElse(name.asString().result().map(this.renames).map(name::createString), name);
   }

   private Typed<?> fixItemStackTag(final Typed<?> itemStack) {
      return itemStack.update(
         DSL.remainderFinder(),
         tag -> tag.update(
            "AttributeModifiers",
            modifiers -> (Dynamic)DataFixUtils.orElse(
               modifiers.asStreamOpt().result().map(s -> s.map(modifier -> modifier.update("AttributeName", this::fixName))).map(modifiers::createList),
               modifiers
            )
         )
      );
   }

   private Typed<?> fixEntity(final Typed<?> entity) {
      return entity.update(
         DSL.remainderFinder(),
         tag -> tag.update(
            "Attributes",
            attributeList -> (Dynamic)DataFixUtils.orElse(
               attributeList.asStreamOpt().result().map(s -> s.map(attribute -> attribute.update("Name", this::fixName))).map(attributeList::createList),
               attributeList
            )
         )
      );
   }
}
