package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.OptionalDynamic;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class ItemStackCustomNameToOverrideComponentFix extends DataFix {
   private static final Set<String> MAP_NAMES = Set.of(
      "filled_map.buried_treasure",
      "filled_map.explorer_jungle",
      "filled_map.explorer_swamp",
      "filled_map.mansion",
      "filled_map.monument",
      "filled_map.trial_chambers",
      "filled_map.village_desert",
      "filled_map.village_plains",
      "filled_map.village_savanna",
      "filled_map.village_snowy",
      "filled_map.village_taiga"
   );

   public ItemStackCustomNameToOverrideComponentFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   public final TypeRewriteRule makeRule() {
      Type<?> itemStackType = this.getInputSchema().getType(References.ITEM_STACK);
      OpticFinder<Pair<String, String>> idFinder = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
      OpticFinder<?> componentsFinder = itemStackType.findField("components");
      return this.fixTypeEverywhereTyped(
         "ItemStack custom_name to item_name component fix",
         itemStackType,
         input -> {
            Optional<Pair<String, String>> id = input.getOptional(idFinder);
            Optional<String> maybeId = id.map(Pair::getSecond);
            if (maybeId.filter(s -> s.equals("minecraft:white_banner")).isPresent()) {
               return input.updateTyped(componentsFinder, ItemStackCustomNameToOverrideComponentFix::fixBanner);
            } else {
               return maybeId.filter(s -> s.equals("minecraft:filled_map")).isPresent()
                  ? input.updateTyped(componentsFinder, ItemStackCustomNameToOverrideComponentFix::fixMap)
                  : input;
            }
         }
      );
   }

   private static <T> Typed<T> fixMap(final Typed<T> value) {
      return fixCustomName(value, MAP_NAMES::contains);
   }

   private static <T> Typed<T> fixBanner(final Typed<T> value) {
      return fixCustomName(value, e -> e.equals("block.minecraft.ominous_banner"));
   }

   private static <T> Typed<T> fixCustomName(final Typed<T> typed, final Predicate<String> expectedTranslationKey) {
      return Util.writeAndReadTypedOrThrow(
         typed,
         typed.getType(),
         value -> {
            OptionalDynamic<?> customNameTag = value.get("minecraft:custom_name");
            Optional<String> hasCorrectTranslationKey = customNameTag.asString()
               .result()
               .flatMap(LegacyComponentDataFixUtils::extractTranslationString)
               .filter(expectedTranslationKey);
            return hasCorrectTranslationKey.isPresent() ? value.renameField("minecraft:custom_name", "minecraft:item_name") : value;
         }
      );
   }
}
