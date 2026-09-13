package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class BoatSplitFix extends DataFix {
   public BoatSplitFix(final Schema outputSchema) {
      super(outputSchema, true);
   }

   private static boolean isNormalBoat(final String id) {
      return id.equals("minecraft:boat");
   }

   private static boolean isChestBoat(final String id) {
      return id.equals("minecraft:chest_boat");
   }

   private static boolean isAnyBoat(final String id) {
      return isNormalBoat(id) || isChestBoat(id);
   }

   private static String mapVariantToNormalBoat(final String id) {
      return switch (id) {
         case "spruce" -> "minecraft:spruce_boat";
         case "birch" -> "minecraft:birch_boat";
         case "jungle" -> "minecraft:jungle_boat";
         case "acacia" -> "minecraft:acacia_boat";
         case "cherry" -> "minecraft:cherry_boat";
         case "dark_oak" -> "minecraft:dark_oak_boat";
         case "mangrove" -> "minecraft:mangrove_boat";
         case "bamboo" -> "minecraft:bamboo_raft";
         default -> "minecraft:oak_boat";
      };
   }

   private static String mapVariantToChestBoat(final String id) {
      return switch (id) {
         case "spruce" -> "minecraft:spruce_chest_boat";
         case "birch" -> "minecraft:birch_chest_boat";
         case "jungle" -> "minecraft:jungle_chest_boat";
         case "acacia" -> "minecraft:acacia_chest_boat";
         case "cherry" -> "minecraft:cherry_chest_boat";
         case "dark_oak" -> "minecraft:dark_oak_chest_boat";
         case "mangrove" -> "minecraft:mangrove_chest_boat";
         case "bamboo" -> "minecraft:bamboo_chest_raft";
         default -> "minecraft:oak_chest_boat";
      };
   }

   public TypeRewriteRule makeRule() {
      OpticFinder<String> idF = DSL.fieldFinder("id", NamespacedSchema.namespacedString());
      Type<?> oldType = this.getInputSchema().getType(References.ENTITY);
      Type<?> newType = this.getOutputSchema().getType(References.ENTITY);
      return this.fixTypeEverywhereTyped("BoatSplitFix", oldType, newType, input -> {
         Optional<String> id = input.getOptional(idF);
         if (id.isPresent() && isAnyBoat(id.get())) {
            Dynamic<?> tag = (Dynamic<?>)input.getOrCreate(DSL.remainderFinder());
            Optional<String> maybeBoatId = tag.get("Type").asString().result();
            String newId;
            if (isChestBoat(id.get())) {
               newId = maybeBoatId.map(BoatSplitFix::mapVariantToChestBoat).orElse("minecraft:oak_chest_boat");
            } else {
               newId = maybeBoatId.map(BoatSplitFix::mapVariantToNormalBoat).orElse("minecraft:oak_boat");
            }

            return ExtraDataFixUtils.cast(newType, input).update(DSL.remainderFinder(), remainder -> remainder.remove("Type")).set(idF, newId);
         } else {
            return ExtraDataFixUtils.cast(newType, input);
         }
      });
   }
}
