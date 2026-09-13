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

   public TooltipDisplayComponentFix(Schema p_394369_) {
      super(p_394369_, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.DATA_COMPONENTS);
      Type<?> type1 = this.getOutputSchema().getType(References.DATA_COMPONENTS);
      OpticFinder<?> opticfinder = type.findField("minecraft:can_place_on");
      OpticFinder<?> opticfinder1 = type.findField("minecraft:can_break");
      Type<?> type2 = type1.findFieldType("minecraft:can_place_on");
      Type<?> type3 = type1.findFieldType("minecraft:can_break");
      return this.fixTypeEverywhereTyped("TooltipDisplayComponentFix", type, type1, p_396988_ -> fix(p_396988_, opticfinder, opticfinder1, type2, type3));
   }

   private static Typed<?> fix(Typed<?> p_391754_, OpticFinder<?> p_396640_, OpticFinder<?> p_395831_, Type<?> p_393016_, Type<?> p_394371_) {
      Set<String> set = new HashSet<>();
      p_391754_ = fixAdventureModePredicate(p_391754_, p_396640_, p_393016_, "minecraft:can_place_on", set);
      p_391754_ = fixAdventureModePredicate(p_391754_, p_395831_, p_394371_, "minecraft:can_break", set);
      return p_391754_.update(
         DSL.remainderFinder(),
         p_405253_ -> {
            p_405253_ = fixSimpleComponent(p_405253_, "minecraft:trim", set);
            p_405253_ = fixSimpleComponent(p_405253_, "minecraft:unbreakable", set);
            p_405253_ = fixComponentAndUnwrap(p_405253_, "minecraft:dyed_color", "rgb", set);
            p_405253_ = fixComponentAndUnwrap(p_405253_, "minecraft:attribute_modifiers", "modifiers", set);
            p_405253_ = fixComponentAndUnwrap(p_405253_, "minecraft:enchantments", "levels", set);
            p_405253_ = fixComponentAndUnwrap(p_405253_, "minecraft:stored_enchantments", "levels", set);
            p_405253_ = fixComponentAndUnwrap(p_405253_, "minecraft:jukebox_playable", "song", set);
            boolean flag = p_405253_.get("minecraft:hide_tooltip").result().isPresent();
            p_405253_ = p_405253_.remove("minecraft:hide_tooltip");
            boolean flag1 = p_405253_.get("minecraft:hide_additional_tooltip").result().isPresent();
            p_405253_ = p_405253_.remove("minecraft:hide_additional_tooltip");
            if (flag1) {
               for (String s : CONVERTED_ADDITIONAL_TOOLTIP_TYPES) {
                  if (p_405253_.get(s).result().isPresent()) {
                     set.add(s);
                  }
               }
            }

            return set.isEmpty() && !flag
               ? p_405253_
               : p_405253_.set(
                  "minecraft:tooltip_display",
                  p_405253_.createMap(
                     Map.of(
                        p_405253_.createString("hide_tooltip"),
                        p_405253_.createBoolean(flag),
                        p_405253_.createString("hidden_components"),
                        p_405253_.createList(set.stream().map(p_405253_::createString))
                     )
                  )
               );
         }
      );
   }

   private static Dynamic<?> fixSimpleComponent(Dynamic<?> p_397935_, String p_396961_, Set<String> p_396362_) {
      return fixRemainderComponent(p_397935_, p_396961_, p_396362_, UnaryOperator.identity());
   }

   private static Dynamic<?> fixComponentAndUnwrap(Dynamic<?> p_393468_, String p_391976_, String p_391872_, Set<String> p_393150_) {
      return fixRemainderComponent(p_393468_, p_391976_, p_393150_, p_394957_ -> (Dynamic)DataFixUtils.orElse(p_394957_.get(p_391872_).result(), p_394957_));
   }

   private static Dynamic<?> fixRemainderComponent(Dynamic<?> p_397445_, String p_395650_, Set<String> p_392691_, UnaryOperator<Dynamic<?>> p_397967_) {
      return p_397445_.update(p_395650_, p_391205_ -> {
         boolean flag = p_391205_.get("show_in_tooltip").asBoolean(true);
         if (!flag) {
            p_392691_.add(p_395650_);
         }

         return p_397967_.apply(p_391205_.remove("show_in_tooltip"));
      });
   }

   private static Typed<?> fixAdventureModePredicate(Typed<?> p_391898_, OpticFinder<?> p_393084_, Type<?> p_392260_, String p_391537_, Set<String> p_395645_) {
      return p_391898_.updateTyped(p_393084_, p_392260_, p_449328_ -> Util.writeAndReadTypedOrThrow(p_449328_, p_392260_, p_397322_ -> {
         OptionalDynamic<?> optionaldynamic = p_397322_.get("predicates");
         if (optionaldynamic.result().isEmpty()) {
            return p_397322_;
         }

         boolean flag = p_397322_.get("show_in_tooltip").asBoolean(true);
         if (!flag) {
            p_395645_.add(p_391537_);
         }

         return (Dynamic)optionaldynamic.result().get();
      }));
   }
}
