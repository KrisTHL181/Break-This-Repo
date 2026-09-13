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
import java.util.Set;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class SaddleEquipmentSlotFix extends DataFix {
   private static final Set<String> ENTITIES_WITH_SADDLE_ITEM = Set.of(
      "minecraft:horse",
      "minecraft:skeleton_horse",
      "minecraft:zombie_horse",
      "minecraft:donkey",
      "minecraft:mule",
      "minecraft:camel",
      "minecraft:llama",
      "minecraft:trader_llama"
   );
   private static final Set<String> ENTITIES_WITH_SADDLE_FLAG = Set.of("minecraft:pig", "minecraft:strider");
   private static final String SADDLE_FLAG = "Saddle";
   private static final String NEW_SADDLE = "saddle";

   public SaddleEquipmentSlotFix(final Schema outputSchema) {
      super(outputSchema, true);
   }

   protected TypeRewriteRule makeRule() {
      TaggedChoiceType<String> entityIdType = this.getInputSchema().findChoiceType(References.ENTITY);
      OpticFinder<Pair<String, ?>> entityIdF = DSL.typeFinder(entityIdType);
      Type<?> inputType = this.getInputSchema().getType(References.ENTITY);
      Type<?> outputType = this.getOutputSchema().getType(References.ENTITY);
      Type<?> patchedInputType = ExtraDataFixUtils.patchSubType(inputType, inputType, outputType);
      return this.fixTypeEverywhereTyped(
         "SaddleEquipmentSlotFix",
         inputType,
         outputType,
         input -> {
            String entityId = input.getOptional(entityIdF).<String>map(Pair::getFirst).map(NamespacedSchema::ensureNamespaced).orElse("");
            Typed<?> fixedInput = ExtraDataFixUtils.cast(patchedInputType, input);
            if (ENTITIES_WITH_SADDLE_ITEM.contains(entityId)) {
               return Util.writeAndReadTypedOrThrow(fixedInput, outputType, SaddleEquipmentSlotFix::fixEntityWithSaddleItem);
            } else {
               return ENTITIES_WITH_SADDLE_FLAG.contains(entityId)
                  ? Util.writeAndReadTypedOrThrow(fixedInput, outputType, SaddleEquipmentSlotFix::fixEntityWithSaddleFlag)
                  : ExtraDataFixUtils.cast(outputType, input);
            }
         }
      );
   }

   private static Dynamic<?> fixEntityWithSaddleItem(final Dynamic<?> input) {
      return input.get("SaddleItem").result().isEmpty() ? input : fixDropChances(input.renameField("SaddleItem", "saddle"));
   }

   private static Dynamic<?> fixEntityWithSaddleFlag(Dynamic<?> tag) {
      boolean hasSaddle = tag.get("Saddle").asBoolean(false);
      tag = tag.remove("Saddle");
      if (!hasSaddle) {
         return tag;
      }

      Dynamic<?> saddleItem = tag.emptyMap().set("id", tag.createString("minecraft:saddle")).set("count", tag.createInt(1));
      return fixDropChances(tag.set("saddle", saddleItem));
   }

   private static Dynamic<?> fixDropChances(final Dynamic<?> tag) {
      Dynamic<?> dropChances = tag.get("drop_chances").orElseEmptyMap().set("saddle", tag.createFloat(2.0F));
      return tag.set("drop_chances", dropChances);
   }
}
