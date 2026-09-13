package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class MobEffectIdFix extends DataFix {
   private static final Int2ObjectMap<String> ID_MAP = Util.make(new Int2ObjectOpenHashMap(), m -> {
      m.put(1, "minecraft:speed");
      m.put(2, "minecraft:slowness");
      m.put(3, "minecraft:haste");
      m.put(4, "minecraft:mining_fatigue");
      m.put(5, "minecraft:strength");
      m.put(6, "minecraft:instant_health");
      m.put(7, "minecraft:instant_damage");
      m.put(8, "minecraft:jump_boost");
      m.put(9, "minecraft:nausea");
      m.put(10, "minecraft:regeneration");
      m.put(11, "minecraft:resistance");
      m.put(12, "minecraft:fire_resistance");
      m.put(13, "minecraft:water_breathing");
      m.put(14, "minecraft:invisibility");
      m.put(15, "minecraft:blindness");
      m.put(16, "minecraft:night_vision");
      m.put(17, "minecraft:hunger");
      m.put(18, "minecraft:weakness");
      m.put(19, "minecraft:poison");
      m.put(20, "minecraft:wither");
      m.put(21, "minecraft:health_boost");
      m.put(22, "minecraft:absorption");
      m.put(23, "minecraft:saturation");
      m.put(24, "minecraft:glowing");
      m.put(25, "minecraft:levitation");
      m.put(26, "minecraft:luck");
      m.put(27, "minecraft:unluck");
      m.put(28, "minecraft:slow_falling");
      m.put(29, "minecraft:conduit_power");
      m.put(30, "minecraft:dolphins_grace");
      m.put(31, "minecraft:bad_omen");
      m.put(32, "minecraft:hero_of_the_village");
      m.put(33, "minecraft:darkness");
   });
   private static final Set<String> MOB_EFFECT_INSTANCE_CARRIER_ITEMS = Set.of(
      "minecraft:potion", "minecraft:splash_potion", "minecraft:lingering_potion", "minecraft:tipped_arrow"
   );

   public MobEffectIdFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   private static <T> Optional<Dynamic<T>> getAndConvertMobEffectId(final Dynamic<T> obj, final String fieldName) {
      return obj.get(fieldName).asNumber().result().map(id -> (String)ID_MAP.get(id.intValue())).map(obj::createString);
   }

   private static <T> Dynamic<T> updateMobEffectIdField(final Dynamic<T> input, final String oldFieldName, final Dynamic<T> output, final String newFieldName) {
      Optional<Dynamic<T>> mappedId = getAndConvertMobEffectId(input, oldFieldName);
      return output.replaceField(oldFieldName, newFieldName, mappedId);
   }

   private static <T> Dynamic<T> updateMobEffectIdField(final Dynamic<T> input, final String oldFieldName, final String newFieldName) {
      return updateMobEffectIdField(input, oldFieldName, input, newFieldName);
   }

   private static <T> Dynamic<T> updateMobEffectInstance(Dynamic<T> input) {
      input = updateMobEffectIdField(input, "Id", "id");
      input = input.renameField("Ambient", "ambient");
      input = input.renameField("Amplifier", "amplifier");
      input = input.renameField("Duration", "duration");
      input = input.renameField("ShowParticles", "show_particles");
      input = input.renameField("ShowIcon", "show_icon");
      Optional<Dynamic<T>> hiddenEffect = input.get("HiddenEffect").result().map(MobEffectIdFix::updateMobEffectInstance);
      return input.replaceField("HiddenEffect", "hidden_effect", hiddenEffect);
   }

   private static <T> Dynamic<T> updateMobEffectInstanceList(final Dynamic<T> input, final String oldField, final String newField) {
      Optional<Dynamic<T>> newValue = input.get(oldField)
         .asStreamOpt()
         .result()
         .map(effects -> input.createList(effects.map(MobEffectIdFix::updateMobEffectInstance)));
      return input.replaceField(oldField, newField, newValue);
   }

   private static <T> Dynamic<T> updateSuspiciousStewEntry(final Dynamic<T> input, Dynamic<T> output) {
      output = updateMobEffectIdField(input, "EffectId", output, "id");
      Optional<Dynamic<T>> duration = input.get("EffectDuration").result();
      return output.replaceField("EffectDuration", "duration", duration);
   }

   private static <T> Dynamic<T> updateSuspiciousStewEntry(final Dynamic<T> input) {
      return updateSuspiciousStewEntry(input, input);
   }

   private Typed<?> updateNamedChoice(
      final Typed<?> input, final TypeReference typeReference, final String name, final Function<Dynamic<?>, Dynamic<?>> function
   ) {
      Type<?> oldType = this.getInputSchema().getChoiceType(typeReference, name);
      Type<?> newType = this.getOutputSchema().getChoiceType(typeReference, name);
      return input.updateTyped(DSL.namedChoice(name, oldType), newType, typedTag -> typedTag.update(DSL.remainderFinder(), function));
   }

   private TypeRewriteRule blockEntityFixer() {
      Type<?> blockEntityType = this.getInputSchema().getType(References.BLOCK_ENTITY);
      return this.fixTypeEverywhereTyped(
         "BlockEntityMobEffectIdFix", blockEntityType, input -> this.updateNamedChoice(input, References.BLOCK_ENTITY, "minecraft:beacon", tag -> {
            tag = updateMobEffectIdField(tag, "Primary", "primary_effect");
            return updateMobEffectIdField(tag, "Secondary", "secondary_effect");
         })
      );
   }

   private static <T> Dynamic<T> fixMooshroomTag(Dynamic<T> entityTag) {
      Dynamic<T> initialEntry = entityTag.emptyMap();
      Dynamic<T> entry = updateSuspiciousStewEntry(entityTag, initialEntry);
      if (!entry.equals(initialEntry)) {
         entityTag = entityTag.set("stew_effects", entityTag.createList(Stream.of(entry)));
      }

      return entityTag.remove("EffectId").remove("EffectDuration");
   }

   private static <T> Dynamic<T> fixArrowTag(final Dynamic<T> data) {
      return updateMobEffectInstanceList(data, "CustomPotionEffects", "custom_potion_effects");
   }

   private static <T> Dynamic<T> fixAreaEffectCloudTag(final Dynamic<T> data) {
      return updateMobEffectInstanceList(data, "Effects", "effects");
   }

   private static Dynamic<?> updateLivingEntityTag(final Dynamic<?> data) {
      return updateMobEffectInstanceList(data, "ActiveEffects", "active_effects");
   }

   private TypeRewriteRule entityFixer() {
      Type<?> entityType = this.getInputSchema().getType(References.ENTITY);
      return this.fixTypeEverywhereTyped("EntityMobEffectIdFix", entityType, input -> {
         input = this.updateNamedChoice(input, References.ENTITY, "minecraft:mooshroom", MobEffectIdFix::fixMooshroomTag);
         input = this.updateNamedChoice(input, References.ENTITY, "minecraft:arrow", MobEffectIdFix::fixArrowTag);
         input = this.updateNamedChoice(input, References.ENTITY, "minecraft:area_effect_cloud", MobEffectIdFix::fixAreaEffectCloudTag);
         return input.update(DSL.remainderFinder(), MobEffectIdFix::updateLivingEntityTag);
      });
   }

   private TypeRewriteRule playerFixer() {
      Type<?> playerType = this.getInputSchema().getType(References.PLAYER);
      return this.fixTypeEverywhereTyped(
         "PlayerMobEffectIdFix", playerType, input -> input.update(DSL.remainderFinder(), MobEffectIdFix::updateLivingEntityTag)
      );
   }

   private static <T> Dynamic<T> fixSuspiciousStewTag(final Dynamic<T> tag) {
      Optional<Dynamic<T>> effectsList = tag.get("Effects")
         .asStreamOpt()
         .result()
         .map(list -> tag.createList(list.map(MobEffectIdFix::updateSuspiciousStewEntry)));
      return tag.replaceField("Effects", "effects", effectsList);
   }

   private TypeRewriteRule itemStackFixer() {
      OpticFinder<Pair<String, String>> idF = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
      Type<?> itemStackType = this.getInputSchema().getType(References.ITEM_STACK);
      OpticFinder<?> tagF = itemStackType.findField("tag");
      return this.fixTypeEverywhereTyped(
         "ItemStackMobEffectIdFix",
         itemStackType,
         input -> {
            Optional<Pair<String, String>> idOpt = input.getOptional(idF);
            if (idOpt.isPresent()) {
               String id = (String)idOpt.get().getSecond();
               if (id.equals("minecraft:suspicious_stew")) {
                  return input.updateTyped(tagF, itemTag -> itemTag.update(DSL.remainderFinder(), MobEffectIdFix::fixSuspiciousStewTag));
               }

               if (MOB_EFFECT_INSTANCE_CARRIER_ITEMS.contains(id)) {
                  return input.updateTyped(
                     tagF,
                     itemTag -> itemTag.update(DSL.remainderFinder(), tag -> updateMobEffectInstanceList(tag, "CustomPotionEffects", "custom_potion_effects"))
                  );
               }
            }

            return input;
         }
      );
   }

   protected TypeRewriteRule makeRule() {
      return TypeRewriteRule.seq(this.blockEntityFixer(), new TypeRewriteRule[]{this.entityFixer(), this.playerFixer(), this.itemStackFixer()});
   }
}
