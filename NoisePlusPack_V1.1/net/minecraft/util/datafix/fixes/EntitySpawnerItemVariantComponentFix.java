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
   public EntitySpawnerItemVariantComponentFix(Schema p_392449_) {
      super(p_392449_, false);
   }

   public final TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.ITEM_STACK);
      OpticFinder<Pair<String, String>> opticfinder = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
      OpticFinder<?> opticfinder1 = type.findField("components");
      return this.fixTypeEverywhereTyped(
         "ItemStack bucket_entity_data variants to separate components",
         type,
         p_392674_ -> {
            String s = p_392674_.getOptional(opticfinder).<String>map(Pair::getSecond).orElse("");

            return switch (s) {
               case "minecraft:salmon_bucket" -> p_392674_.updateTyped(opticfinder1, EntitySpawnerItemVariantComponentFix::fixSalmonBucket);
               case "minecraft:axolotl_bucket" -> p_392674_.updateTyped(opticfinder1, EntitySpawnerItemVariantComponentFix::fixAxolotlBucket);
               case "minecraft:tropical_fish_bucket" -> p_392674_.updateTyped(opticfinder1, EntitySpawnerItemVariantComponentFix::fixTropicalFishBucket);
               case "minecraft:painting" -> p_392674_.updateTyped(
                  opticfinder1, p_449308_ -> Util.writeAndReadTypedOrThrow(p_449308_, p_449308_.getType(), EntitySpawnerItemVariantComponentFix::fixPainting)
               );
               default -> p_392674_;
            };
         }
      );
   }

   private static String getBaseColor(int p_394779_) {
      return ExtraDataFixUtils.dyeColorIdToName(p_394779_ >> 16 & 0xFF);
   }

   private static String getPatternColor(int p_393453_) {
      return ExtraDataFixUtils.dyeColorIdToName(p_393453_ >> 24 & 0xFF);
   }

   private static String getPattern(int p_391236_) {
      return switch (p_391236_ & 65535) {
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

   private static <T> Dynamic<T> fixTropicalFishBucket(Dynamic<T> p_392473_, Dynamic<T> p_394596_) {
      Optional<Number> optional = p_394596_.get("BucketVariantTag").asNumber().result();
      if (optional.isEmpty()) {
         return p_392473_;
      }

      int i = optional.get().intValue();
      String s = getPattern(i);
      String s1 = getBaseColor(i);
      String s2 = getPatternColor(i);
      return p_392473_.update("minecraft:bucket_entity_data", p_397862_ -> p_397862_.remove("BucketVariantTag"))
         .set("minecraft:tropical_fish/pattern", p_392473_.createString(s))
         .set("minecraft:tropical_fish/base_color", p_392473_.createString(s1))
         .set("minecraft:tropical_fish/pattern_color", p_392473_.createString(s2));
   }

   private static <T> Dynamic<T> fixAxolotlBucket(Dynamic<T> p_391982_, Dynamic<T> p_395344_) {
      Optional<Number> optional = p_395344_.get("Variant").asNumber().result();
      if (optional.isEmpty()) {
         return p_391982_;
      }

      String s = switch (optional.get().intValue()) {
         case 1 -> "wild";
         case 2 -> "gold";
         case 3 -> "cyan";
         case 4 -> "blue";
         default -> "lucy";
      };
      return p_391982_.update("minecraft:bucket_entity_data", p_395620_ -> p_395620_.remove("Variant"))
         .set("minecraft:axolotl/variant", p_391982_.createString(s));
   }

   private static <T> Dynamic<T> fixSalmonBucket(Dynamic<T> p_397584_, Dynamic<T> p_395123_) {
      Optional<Dynamic<T>> optional = p_395123_.get("type").result();
      return optional.isEmpty()
         ? p_397584_
         : p_397584_.update("minecraft:bucket_entity_data", p_394947_ -> p_394947_.remove("type")).set("minecraft:salmon/size", optional.get());
   }

   private static <T> Dynamic<T> fixPainting(Dynamic<T> p_392998_) {
      Optional<Dynamic<T>> optional = p_392998_.get("minecraft:entity_data").result();
      if (optional.isEmpty()) {
         return p_392998_;
      }

      if (optional.get().get("id").asString().result().filter(p_391705_ -> p_391705_.equals("minecraft:painting")).isEmpty()) {
         return p_392998_;
      }

      Optional<Dynamic<T>> optional1 = optional.get().get("variant").result();
      Dynamic<T> dynamic = optional.get().remove("variant");
      if (dynamic.remove("id").equals(dynamic.emptyMap())) {
         p_392998_ = p_392998_.remove("minecraft:entity_data");
      } else {
         p_392998_ = p_392998_.set("minecraft:entity_data", dynamic);
      }

      if (optional1.isPresent()) {
         p_392998_ = p_392998_.set("minecraft:painting/variant", optional1.get());
      }

      return p_392998_;
   }

   @FunctionalInterface
   interface Fixer extends Function<Typed<?>, Typed<?>> {
      default Typed<?> apply(Typed<?> p_393786_) {
         return p_393786_.update(DSL.remainderFinder(), this::fixRemainder);
      }

      default <T> Dynamic<T> fixRemainder(Dynamic<T> p_393764_) {
         return p_393764_.get("minecraft:bucket_entity_data").result().map(p_397629_ -> this.fixRemainder(p_393764_, (Dynamic<T>)p_397629_)).orElse(p_393764_);
      }

      <T> Dynamic<T> fixRemainder(Dynamic<T> var1, Dynamic<T> var2);
   }
}
