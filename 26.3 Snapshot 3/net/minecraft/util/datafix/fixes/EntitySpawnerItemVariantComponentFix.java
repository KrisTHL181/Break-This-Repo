package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class EntitySpawnerItemVariantComponentFix extends DataFix {
   public EntitySpawnerItemVariantComponentFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   public final TypeRewriteRule makeRule() {
      Type<?> itemStackType = this.getInputSchema().getType(References.ITEM_STACK);
      OpticFinder<Pair<String, String>> idFinder = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
      OpticFinder<?> componentsFinder = itemStackType.findField("components");
      return this.fixTypeEverywhereTyped(
         "ItemStack bucket_entity_data variants to separate components",
         itemStackType,
         input -> {
            String id = input.getOptional(idFinder).<String>map(Pair::getSecond).orElse("");

            return switch (id) {
               case "minecraft:salmon_bucket" -> input.updateTyped(componentsFinder, EntitySpawnerItemVariantComponentFix::fixSalmonBucket);
               case "minecraft:axolotl_bucket" -> input.updateTyped(componentsFinder, EntitySpawnerItemVariantComponentFix::fixAxolotlBucket);
               case "minecraft:tropical_fish_bucket" -> input.updateTyped(componentsFinder, EntitySpawnerItemVariantComponentFix::fixTropicalFishBucket);
               case "minecraft:painting" -> input.updateTyped(
                  componentsFinder,
                  components -> Util.writeAndReadTypedOrThrow(components, components.getType(), EntitySpawnerItemVariantComponentFix::fixPainting)
               );
               default -> input;
            };
         }
      );
   }

   private static String getBaseColor(final int packedVariant) {
      return ExtraDataFixUtils.dyeColorIdToName(packedVariant >> 16 & 0xFF);
   }

   private static String getPatternColor(final int packedVariant) {
      return ExtraDataFixUtils.dyeColorIdToName(packedVariant >> 24 & 0xFF);
   }

   private static String getPattern(final int packedVariant) {
      return switch (packedVariant & 65535) {
         case 1 -> "flopper";
         case 256 -> "sunstreak";
         case 257 -> "stripey";
         case 512 -> "snooper";
         case 513 -> "glitter";
         case 768 -> "dasher";
         case 769 -> "blockfish";
         case 1024 -> "brinely";
         case 1025 -> "betty";
         case 1280 -> "spotty";
         case 1281 -> "clayfish";
         default -> "kob";
      };
   }

   private static <T> Dynamic<T> fixTropicalFishBucket(final Dynamic<T> remainder, final Dynamic<T> bucketData) {
      Optional<Number> oldVariant = bucketData.get("BucketVariantTag").asNumber().result();
      if (oldVariant.isEmpty()) {
         return remainder;
      }

      int packedVariant = oldVariant.get().intValue();
      String pattern = getPattern(packedVariant);
      String baseColor = getBaseColor(packedVariant);
      String patternColor = getPatternColor(packedVariant);
      return remainder.update("minecraft:bucket_entity_data", b -> b.remove("BucketVariantTag"))
         .set("minecraft:tropical_fish/pattern", remainder.createString(pattern))
         .set("minecraft:tropical_fish/base_color", remainder.createString(baseColor))
         .set("minecraft:tropical_fish/pattern_color", remainder.createString(patternColor));
   }

   private static <T> Dynamic<T> fixAxolotlBucket(final Dynamic<T> remainder, final Dynamic<T> bucketData) {
      Optional<Number> oldVariant = bucketData.get("Variant").asNumber().result();
      if (oldVariant.isEmpty()) {
         return remainder;
      }

      String newVariant = switch (oldVariant.get().intValue()) {
         case 1 -> "wild";
         case 2 -> "gold";
         case 3 -> "cyan";
         case 4 -> "blue";
         default -> "lucy";
      };
      return remainder.update("minecraft:bucket_entity_data", b -> b.remove("Variant")).set("minecraft:axolotl/variant", remainder.createString(newVariant));
   }

   private static <T> Dynamic<T> fixSalmonBucket(final Dynamic<T> remainder, final Dynamic<T> bucketData) {
      Optional<Dynamic<T>> type = bucketData.get("type").result();
      return type.isEmpty() ? remainder : remainder.update("minecraft:bucket_entity_data", b -> b.remove("type")).set("minecraft:salmon/size", type.get());
   }

   private static <T> Dynamic<T> fixPainting(Dynamic<T> components) {
      Optional<Dynamic<T>> entityData = components.get("minecraft:entity_data").result();
      if (entityData.isEmpty()) {
         return components;
      }

      if (entityData.get().get("id").asString().result().filter(id -> id.equals("minecraft:painting")).isEmpty()) {
         return components;
      }

      Optional<Dynamic<T>> result = entityData.get().get("variant").result();
      Dynamic<T> entityDataRemainder = entityData.get().remove("variant");
      if (entityDataRemainder.remove("id").equals(entityDataRemainder.emptyMap())) {
         components = components.remove("minecraft:entity_data");
      } else {
         components = components.set("minecraft:entity_data", entityDataRemainder);
      }

      if (result.isPresent()) {
         components = components.set("minecraft:painting/variant", result.get());
      }

      return components;
   }

   @FunctionalInterface
   private interface Fixer extends Function<Typed<?>, Typed<?>> {
      default Typed<?> apply(final Typed<?> components) {
         return components.update(DSL.remainderFinder(), this::fixRemainder);
      }

      default <T> Dynamic<T> fixRemainder(final Dynamic<T> remainder) {
         return remainder.get("minecraft:bucket_entity_data")
            .result()
            .map(bucketData -> this.fixRemainder(remainder, (Dynamic<T>)bucketData))
            .orElse(remainder);
      }

      <T> Dynamic<T> fixRemainder(Dynamic<T> remainder, Dynamic<T> bucketData);
   }
}
