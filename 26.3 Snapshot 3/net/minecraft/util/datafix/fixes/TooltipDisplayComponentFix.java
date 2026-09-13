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
import com.mojang.serialization.OptionalDynamic;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import net.minecraft.util.Util;

public class TooltipDisplayComponentFix extends DataFix {
   private static final List<String> CONVERTED_ADDITIONAL_TOOLTIP_TYPES = List.of(
      "minecraft:banner_patterns",
      "minecraft:bees",
      "minecraft:block_entity_data",
      "minecraft:block_state",
      "minecraft:bundle_contents",
      "minecraft:charged_projectiles",
      "minecraft:container",
      "minecraft:container_loot",
      "minecraft:firework_explosion",
      "minecraft:fireworks",
      "minecraft:instrument",
      "minecraft:map_id",
      "minecraft:painting/variant",
      "minecraft:pot_decorations",
      "minecraft:potion_contents",
      "minecraft:tropical_fish/pattern",
      "minecraft:written_book_content"
   );

   public TooltipDisplayComponentFix(final Schema outputSchema) {
      super(outputSchema, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> componentsType = this.getInputSchema().getType(References.DATA_COMPONENTS);
      Type<?> newComponentsType = this.getOutputSchema().getType(References.DATA_COMPONENTS);
      OpticFinder<?> canPlaceOnFinder = componentsType.findField("minecraft:can_place_on");
      OpticFinder<?> canBreakFinder = componentsType.findField("minecraft:can_break");
      Type<?> newCanPlaceOnType = newComponentsType.findFieldType("minecraft:can_place_on");
      Type<?> newCanBreakType = newComponentsType.findFieldType("minecraft:can_break");
      return this.fixTypeEverywhereTyped(
         "TooltipDisplayComponentFix",
         componentsType,
         newComponentsType,
         typed -> fix(typed, canPlaceOnFinder, canBreakFinder, newCanPlaceOnType, newCanBreakType)
      );
   }

   private static Typed<?> fix(
      Typed<?> typed,
      final OpticFinder<?> canPlaceOnFinder,
      final OpticFinder<?> canBreakFinder,
      final Type<?> newCanPlaceOnType,
      final Type<?> newCanBreakType
   ) {
      Set<String> hiddenTooltips = new HashSet<>();
      typed = fixAdventureModePredicate(typed, canPlaceOnFinder, newCanPlaceOnType, "minecraft:can_place_on", hiddenTooltips);
      typed = fixAdventureModePredicate(typed, canBreakFinder, newCanBreakType, "minecraft:can_break", hiddenTooltips);
      return typed.update(
         DSL.remainderFinder(),
         remainder -> {
            remainder = fixSimpleComponent(remainder, "minecraft:trim", hiddenTooltips);
            remainder = fixSimpleComponent(remainder, "minecraft:unbreakable", hiddenTooltips);
            remainder = fixComponentAndUnwrap(remainder, "minecraft:dyed_color", "rgb", hiddenTooltips);
            remainder = fixComponentAndUnwrap(remainder, "minecraft:attribute_modifiers", "modifiers", hiddenTooltips);
            remainder = fixComponentAndUnwrap(remainder, "minecraft:enchantments", "levels", hiddenTooltips);
            remainder = fixComponentAndUnwrap(remainder, "minecraft:stored_enchantments", "levels", hiddenTooltips);
            remainder = fixComponentAndUnwrap(remainder, "minecraft:jukebox_playable", "song", hiddenTooltips);
            boolean hideTooltip = remainder.get("minecraft:hide_tooltip").result().isPresent();
            remainder = remainder.remove("minecraft:hide_tooltip");
            boolean hideAdditionalTooltip = remainder.get("minecraft:hide_additional_tooltip").result().isPresent();
            remainder = remainder.remove("minecraft:hide_additional_tooltip");
            if (hideAdditionalTooltip) {
               for (String componentId : CONVERTED_ADDITIONAL_TOOLTIP_TYPES) {
                  if (remainder.get(componentId).result().isPresent()) {
                     hiddenTooltips.add(componentId);
                  }
               }
            }

            return hiddenTooltips.isEmpty() && !hideTooltip
               ? remainder
               : remainder.set(
                  "minecraft:tooltip_display",
                  remainder.createMap(
                     Map.of(
                        remainder.createString("hide_tooltip"),
                        remainder.createBoolean(hideTooltip),
                        remainder.createString("hidden_components"),
                        remainder.createList(hiddenTooltips.stream().map(remainder::createString))
                     )
                  )
               );
         }
      );
   }

   private static Dynamic<?> fixSimpleComponent(final Dynamic<?> remainder, final String componentId, final Set<String> hiddenTooltips) {
      return fixRemainderComponent(remainder, componentId, hiddenTooltips, UnaryOperator.identity());
   }

   private static Dynamic<?> fixComponentAndUnwrap(
      final Dynamic<?> remainder, final String componentId, final String fieldName, final Set<String> hiddenTooltips
   ) {
      return fixRemainderComponent(
         remainder, componentId, hiddenTooltips, component -> (Dynamic)DataFixUtils.orElse(component.get(fieldName).result(), component)
      );
   }

   private static Dynamic<?> fixRemainderComponent(
      final Dynamic<?> remainder, final String componentId, final Set<String> hiddenTooltips, final UnaryOperator<Dynamic<?>> fixer
   ) {
      return remainder.update(componentId, component -> {
         boolean showInTooltip = component.get("show_in_tooltip").asBoolean(true);
         if (!showInTooltip) {
            hiddenTooltips.add(componentId);
         }

         return fixer.apply(component.remove("show_in_tooltip"));
      });
   }

   private static Typed<?> fixAdventureModePredicate(
      final Typed<?> typedComponents, final OpticFinder<?> componentFinder, final Type<?> newType, final String componentId, final Set<String> hiddenTooltips
   ) {
      return typedComponents.updateTyped(componentFinder, newType, typedComponent -> Util.writeAndReadTypedOrThrow(typedComponent, newType, component -> {
         OptionalDynamic<?> predicates = component.get("predicates");
         if (predicates.result().isEmpty()) {
            return component;
         }

         boolean showInTooltip = component.get("show_in_tooltip").asBoolean(true);
         if (!showInTooltip) {
            hiddenTooltips.add(componentId);
         }

         return (Dynamic)predicates.result().get();
      }));
   }
}
