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
import java.util.Set;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class EffectDurationFix extends DataFix {
   private static final Set<String> POTION_ITEMS = Set.of("minecraft:potion", "minecraft:splash_potion", "minecraft:lingering_potion", "minecraft:tipped_arrow");

   public EffectDurationFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      Schema inputSchema = this.getInputSchema();
      Type<?> itemStackType = this.getInputSchema().getType(References.ITEM_STACK);
      OpticFinder<Pair<String, String>> idFinder = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
      OpticFinder<?> tagFinder = itemStackType.findField("tag");
      return TypeRewriteRule.seq(
         this.fixTypeEverywhereTyped(
            "EffectDurationEntity", inputSchema.getType(References.ENTITY), input -> input.update(DSL.remainderFinder(), this::updateEntity)
         ),
         new TypeRewriteRule[]{
            this.fixTypeEverywhereTyped(
               "EffectDurationPlayer", inputSchema.getType(References.PLAYER), input -> input.update(DSL.remainderFinder(), this::updateEntity)
            ),
            this.fixTypeEverywhereTyped("EffectDurationItem", itemStackType, input -> {
               if (input.getOptional(idFinder).filter(typeAndIdPair -> POTION_ITEMS.contains(typeAndIdPair.getSecond())).isPresent()) {
                  Optional<? extends Typed<?>> tag = input.getOptionalTyped(tagFinder);
                  if (tag.isPresent()) {
                     Dynamic<?> tagRest = (Dynamic<?>)tag.get().get(DSL.remainderFinder());
                     Typed<?> newTag = tag.get().set(DSL.remainderFinder(), tagRest.update("CustomPotionEffects", this::fix));
                     return input.set(tagFinder, newTag);
                  }
               }

               return input;
            })
         }
      );
   }

   private Dynamic<?> fixEffect(final Dynamic<?> effect) {
      return effect.update("FactorCalculationData", factorData -> {
         int timestamp = factorData.get("effect_changed_timestamp").asInt(-1);
         factorData = factorData.remove("effect_changed_timestamp");
         int duration = effect.get("Duration").asInt(-1);
         int ticksActive = timestamp - duration;
         return factorData.set("ticks_active", factorData.createInt(ticksActive));
      });
   }

   private Dynamic<?> fix(final Dynamic<?> input) {
      return input.createList(input.asStream().map(this::fixEffect));
   }

   private Dynamic<?> updateEntity(Dynamic<?> data) {
      data = data.update("Effects", this::fix);
      data = data.update("ActiveEffects", this::fix);
      return data.update("CustomPotionEffects", this::fix);
   }
}
