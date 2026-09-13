package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class ItemBannerColorFix extends DataFix {
   public ItemBannerColorFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType);
   }

   public TypeRewriteRule makeRule() {
      Type<?> itemStackType = this.getInputSchema().getType(References.ITEM_STACK);
      OpticFinder<Pair<String, String>> idF = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
      OpticFinder<?> tagF = itemStackType.findField("tag");
      OpticFinder<?> blockEntityF = tagF.type().findField("BlockEntityTag");
      return this.fixTypeEverywhereTyped(
         "ItemBannerColorFix",
         itemStackType,
         input -> {
            Optional<Pair<String, String>> id = input.getOptional(idF);
            if (id.isPresent() && Objects.equals(id.get().getSecond(), "minecraft:banner")) {
               Dynamic<?> rest = (Dynamic<?>)input.get(DSL.remainderFinder());
               Optional<? extends Typed<?>> tagOpt = input.getOptionalTyped(tagF);
               if (tagOpt.isPresent()) {
                  Typed<?> tag = (Typed<?>)tagOpt.get();
                  Optional<? extends Typed<?>> blockEntityOpt = tag.getOptionalTyped(blockEntityF);
                  if (blockEntityOpt.isPresent()) {
                     Typed<?> blockEntity = (Typed<?>)blockEntityOpt.get();
                     Dynamic<?> tagRest = (Dynamic<?>)tag.get(DSL.remainderFinder());
                     Dynamic<?> blockEntityRest = (Dynamic<?>)blockEntity.getOrCreate(DSL.remainderFinder());
                     if (blockEntityRest.get("Base").asNumber().result().isPresent()) {
                        rest = rest.set("Damage", rest.createShort((short)(blockEntityRest.get("Base").asInt(0) & 15)));
                        Optional<? extends Dynamic<?>> displayOptional = tagRest.get("display").result();
                        if (displayOptional.isPresent()) {
                           Dynamic<?> display = (Dynamic<?>)displayOptional.get();
                           Dynamic<?> pickMarker = display.createMap(
                              ImmutableMap.of(display.createString("Lore"), display.createList(Stream.of(display.createString("(+NBT"))))
                           );
                           if (Objects.equals(display, pickMarker)) {
                              return input.set(DSL.remainderFinder(), rest);
                           }
                        }

                        blockEntityRest.remove("Base");
                        return input.set(DSL.remainderFinder(), rest).set(tagF, tag.set(blockEntityF, blockEntity.set(DSL.remainderFinder(), blockEntityRest)));
                     }
                  }
               }

               return input.set(DSL.remainderFinder(), rest);
            } else {
               return input;
            }
         }
      );
   }
}
