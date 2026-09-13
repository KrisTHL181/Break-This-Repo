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
   private static final Int2ObjectMap<String> ID_MAP = Util.make(new Int2ObjectOpenHashMap(), p_298157_ -> {
      p_298157_.put(1, "minecraft:speed");
      p_298157_.put(2, "minecraft:slowness");
      p_298157_.put(3, "minecraft:haste");
      p_298157_.put(4, "minecraft:mining_fatigue");
      p_298157_.put(5, "minecraft:strength");
      p_298157_.put(6, "minecraft:instant_health");
      p_298157_.put(7, "minecraft:instant_damage");
      p_298157_.put(8, "minecraft:jump_boost");
      p_298157_.put(9, "minecraft:nausea");
      p_298157_.put(10, "minecraft:regeneration");
      p_298157_.put(11, "minecraft:resistance");
      p_298157_.put(12, "minecraft:fire_resistance");
      p_298157_.put(13, "minecraft:water_breathing");
      p_298157_.put(14, "minecraft:invisibility");
      p_298157_.put(15, "minecraft:blindness");
      p_298157_.put(16, "minecraft:night_vision");
      p_298157_.put(17, "minecraft:hunger");
      p_298157_.put(18, "minecraft:weakness");
      p_298157_.put(19, "minecraft:poison");
      p_298157_.put(20, "minecraft:wither");
      p_298157_.put(21, "minecraft:health_boost");
      p_298157_.put(22, "minecraft:absorption");
      p_298157_.put(23, "minecraft:saturation");
      p_298157_.put(24, "minecraft:glowing");
      p_298157_.put(25, "minecraft:levitation");
      p_298157_.put(26, "minecraft:luck");
      p_298157_.put(27, "minecraft:unluck");
      p_298157_.put(28, "minecraft:slow_falling");
      p_298157_.put(29, "minecraft:conduit_power");
      p_298157_.put(30, "minecraft:dolphins_grace");
      p_298157_.put(31, "minecraft:bad_omen");
      p_298157_.put(32, "minecraft:hero_of_the_village");
      p_298157_.put(33, "minecraft:darkness");
   });
   private static final Set<String> MOB_EFFECT_INSTANCE_CARRIER_ITEMS = Set.of(
      "minecraft:potion", "minecraft:splash_potion", "minecraft:lingering_potion", "minecraft:tipped_arrow"
   );

   public MobEffectIdFix(Schema p_300797_) {
      super(p_300797_, false);
   }

   private static <T> Optional<Dynamic<T>> getAndConvertMobEffectId(Dynamic<T> p_300040_, String p_300405_) {
      return p_300040_.get(p_300405_).asNumber().result().map(p_298913_ -> (String)ID_MAP.get(p_298913_.intValue())).map(p_300040_::createString);
   }

   private static <T> Dynamic<T> updateMobEffectIdField(Dynamic<T> p_299189_, String p_301147_, Dynamic<T> p_297288_, String p_297619_) {
      Optional<Dynamic<T>> optional = getAndConvertMobEffectId(p_299189_, p_301147_);
      return p_297288_.replaceField(p_301147_, p_297619_, optional);
   }

   private static <T> Dynamic<T> updateMobEffectIdField(Dynamic<T> p_299905_, String p_299399_, String p_301048_) {
      return updateMobEffectIdField(p_299905_, p_299399_, p_299905_, p_301048_);
   }

   private static <T> Dynamic<T> updateMobEffectInstance(Dynamic<T> p_297886_) {
      p_297886_ = updateMobEffectIdField(p_297886_, "Id", "id");
      p_297886_ = p_297886_.renameField("Ambient", "ambient");
      p_297886_ = p_297886_.renameField("Amplifier", "amplifier");
      p_297886_ = p_297886_.renameField("Duration", "duration");
      p_297886_ = p_297886_.renameField("ShowParticles", "show_particles");
      p_297886_ = p_297886_.renameField("ShowIcon", "show_icon");
      Optional<Dynamic<T>> optional = p_297886_.get("HiddenEffect").result().map(MobEffectIdFix::updateMobEffectInstance);
      return p_297886_.replaceField("HiddenEffect", "hidden_effect", optional);
   }

   private static <T> Dynamic<T> updateMobEffectInstanceList(Dynamic<T> p_298694_, String p_298177_, String p_300921_) {
      Optional<Dynamic<T>> optional = p_298694_.get(p_298177_)
         .asStreamOpt()
         .result()
         .map(p_297707_ -> p_298694_.createList(p_297707_.map(MobEffectIdFix::updateMobEffectInstance)));
      return p_298694_.replaceField(p_298177_, p_300921_, optional);
   }

   private static <T> Dynamic<T> updateSuspiciousStewEntry(Dynamic<T> p_299220_, Dynamic<T> p_300010_) {
      p_300010_ = updateMobEffectIdField(p_299220_, "EffectId", p_300010_, "id");
      Optional<Dynamic<T>> optional = p_299220_.get("EffectDuration").result();
      return p_300010_.replaceField("EffectDuration", "duration", optional);
   }

   private static <T> Dynamic<T> updateSuspiciousStewEntry(Dynamic<T> p_297367_) {
      return updateSuspiciousStewEntry(p_297367_, p_297367_);
   }

   private Typed<?> updateNamedChoice(Typed<?> p_299605_, TypeReference p_299152_, String p_300042_, Function<Dynamic<?>, Dynamic<?>> p_300498_) {
      Type<?> type = this.getInputSchema().getChoiceType(p_299152_, p_300042_);
      Type<?> type1 = this.getOutputSchema().getChoiceType(p_299152_, p_300042_);
      return p_299605_.updateTyped(DSL.namedChoice(p_300042_, type), type1, p_299360_ -> p_299360_.update(DSL.remainderFinder(), p_300498_));
   }

   private TypeRewriteRule blockEntityFixer() {
      Type<?> type = this.getInputSchema().getType(References.BLOCK_ENTITY);
      return this.fixTypeEverywhereTyped(
         "BlockEntityMobEffectIdFix", type, p_297729_ -> this.updateNamedChoice(p_297729_, References.BLOCK_ENTITY, "minecraft:beacon", p_298165_ -> {
            p_298165_ = updateMobEffectIdField(p_298165_, "Primary", "primary_effect");
            return updateMobEffectIdField(p_298165_, "Secondary", "secondary_effect");
         })
      );
   }

   private static <T> Dynamic<T> fixMooshroomTag(Dynamic<T> p_298884_) {
      Dynamic<T> dynamic = p_298884_.emptyMap();
      Dynamic<T> dynamic1 = updateSuspiciousStewEntry(p_298884_, dynamic);
      if (!dynamic1.equals(dynamic)) {
         p_298884_ = p_298884_.set("stew_effects", p_298884_.createList(Stream.of(dynamic1)));
      }

      return p_298884_.remove("EffectId").remove("EffectDuration");
   }

   private static <T> Dynamic<T> fixArrowTag(Dynamic<T> p_298539_) {
      return updateMobEffectInstanceList(p_298539_, "CustomPotionEffects", "custom_potion_effects");
   }

   private static <T> Dynamic<T> fixAreaEffectCloudTag(Dynamic<T> p_300392_) {
      return updateMobEffectInstanceList(p_300392_, "Effects", "effects");
   }

   private static Dynamic<?> updateLivingEntityTag(Dynamic<?> p_299534_) {
      return updateMobEffectInstanceList(p_299534_, "ActiveEffects", "active_effects");
   }

   private TypeRewriteRule entityFixer() {
      Type<?> type = this.getInputSchema().getType(References.ENTITY);
      return this.fixTypeEverywhereTyped("EntityMobEffectIdFix", type, p_299717_ -> {
         p_299717_ = this.updateNamedChoice(p_299717_, References.ENTITY, "minecraft:mooshroom", MobEffectIdFix::fixMooshroomTag);
         p_299717_ = this.updateNamedChoice(p_299717_, References.ENTITY, "minecraft:arrow", MobEffectIdFix::fixArrowTag);
         p_299717_ = this.updateNamedChoice(p_299717_, References.ENTITY, "minecraft:area_effect_cloud", MobEffectIdFix::fixAreaEffectCloudTag);
         return p_299717_.update(DSL.remainderFinder(), MobEffectIdFix::updateLivingEntityTag);
      });
   }

   private TypeRewriteRule playerFixer() {
      Type<?> type = this.getInputSchema().getType(References.PLAYER);
      return this.fixTypeEverywhereTyped(
         "PlayerMobEffectIdFix", type, p_297935_ -> p_297935_.update(DSL.remainderFinder(), MobEffectIdFix::updateLivingEntityTag)
      );
   }

   private static <T> Dynamic<T> fixSuspiciousStewTag(Dynamic<T> p_301166_) {
      Optional<Dynamic<T>> optional = p_301166_.get("Effects")
         .asStreamOpt()
         .result()
         .map(p_299334_ -> p_301166_.createList(p_299334_.map(MobEffectIdFix::updateSuspiciousStewEntry)));
      return p_301166_.replaceField("Effects", "effects", optional);
   }

   private TypeRewriteRule itemStackFixer() {
      OpticFinder<Pair<String, String>> opticfinder = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
      Type<?> type = this.getInputSchema().getType(References.ITEM_STACK);
      OpticFinder<?> opticfinder1 = type.findField("tag");
      return this.fixTypeEverywhereTyped(
         "ItemStackMobEffectIdFix",
         type,
         p_300076_ -> {
            Optional<Pair<String, String>> optional = p_300076_.getOptional(opticfinder);
            if (optional.isPresent()) {
               String s = (String)optional.get().getSecond();
               if (s.equals("minecraft:suspicious_stew")) {
                  return p_300076_.updateTyped(opticfinder1, p_301412_ -> p_301412_.update(DSL.remainderFinder(), MobEffectIdFix::fixSuspiciousStewTag));
               }

               if (MOB_EFFECT_INSTANCE_CARRIER_ITEMS.contains(s)) {
                  return p_300076_.updateTyped(
                     opticfinder1,
                     p_299091_ -> p_299091_.update(
                        DSL.remainderFinder(), p_300524_ -> updateMobEffectInstanceList(p_300524_, "CustomPotionEffects", "custom_potion_effects")
                     )
                  );
               }
            }

            return p_300076_;
         }
      );
   }

   protected TypeRewriteRule makeRule() {
      return TypeRewriteRule.seq(this.blockEntityFixer(), new TypeRewriteRule[]{this.entityFixer(), this.playerFixer(), this.itemStackFixer()});
   }
}
