package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class ItemStackUUIDFix extends AbstractUUIDFix {
   public ItemStackUUIDFix(final Schema outputSchema) {
      super(outputSchema, References.ITEM_STACK);
   }

   public TypeRewriteRule makeRule() {
      OpticFinder<Pair<String, String>> idF = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
      return this.fixTypeEverywhereTyped("ItemStackUUIDFix", this.getInputSchema().getType(this.typeReference), input -> {
         OpticFinder<?> itemTagFinder = input.getType().findField("tag");
         return input.updateTyped(itemTagFinder, typedTag -> typedTag.update(DSL.remainderFinder(), tag -> {
            tag = this.updateAttributeModifiers(tag);
            if (input.getOptional(idF).map(idPair -> "minecraft:player_head".equals(idPair.getSecond())).orElse(false)) {
               tag = this.updateSkullOwner(tag);
            }

            return tag;
         }));
      });
   }

   private Dynamic<?> updateAttributeModifiers(final Dynamic<?> tag) {
      return tag.update(
         "AttributeModifiers",
         modifiers -> tag.createList(
            modifiers.asStream().map(modifier -> (Dynamic)replaceUUIDLeastMost((Dynamic<?>)modifier, "UUID", "UUID").orElse((Dynamic<?>)modifier))
         )
      );
   }

   private Dynamic<?> updateSkullOwner(final Dynamic<?> tag) {
      return tag.update("SkullOwner", skullOwner -> replaceUUIDString(skullOwner, "Id", "Id").orElse(skullOwner));
   }
}
