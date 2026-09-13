package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Sets;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;

public class EntityUUIDFix extends AbstractUUIDFix {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Set<String> ABSTRACT_HORSES = Sets.newHashSet();
   private static final Set<String> TAMEABLE_ANIMALS = Sets.newHashSet();
   private static final Set<String> ANIMALS = Sets.newHashSet();
   private static final Set<String> MOBS = Sets.newHashSet();
   private static final Set<String> LIVING_ENTITIES = Sets.newHashSet();
   private static final Set<String> PROJECTILES = Sets.newHashSet();

   public EntityUUIDFix(final Schema outputSchema) {
      super(outputSchema, References.ENTITY);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped("EntityUUIDFixes", this.getInputSchema().getType(this.typeReference), input -> {
         input = input.update(DSL.remainderFinder(), EntityUUIDFix::updateEntityUUID);

         for (String name : ABSTRACT_HORSES) {
            input = this.updateNamedChoice(input, name, EntityUUIDFix::updateAnimalOwner);
         }

         for (String name : TAMEABLE_ANIMALS) {
            input = this.updateNamedChoice(input, name, EntityUUIDFix::updateAnimalOwner);
         }

         for (String name : ANIMALS) {
            input = this.updateNamedChoice(input, name, EntityUUIDFix::updateAnimal);
         }

         for (String name : MOBS) {
            input = this.updateNamedChoice(input, name, EntityUUIDFix::updateMob);
         }

         for (String name : LIVING_ENTITIES) {
            input = this.updateNamedChoice(input, name, EntityUUIDFix::updateLivingEntity);
         }

         for (String name : PROJECTILES) {
            input = this.updateNamedChoice(input, name, EntityUUIDFix::updateProjectile);
         }

         input = this.updateNamedChoice(input, "minecraft:bee", EntityUUIDFix::updateHurtBy);
         input = this.updateNamedChoice(input, "minecraft:zombified_piglin", EntityUUIDFix::updateHurtBy);
         input = this.updateNamedChoice(input, "minecraft:fox", EntityUUIDFix::updateFox);
         input = this.updateNamedChoice(input, "minecraft:item", EntityUUIDFix::updateItem);
         input = this.updateNamedChoice(input, "minecraft:shulker_bullet", EntityUUIDFix::updateShulkerBullet);
         input = this.updateNamedChoice(input, "minecraft:area_effect_cloud", EntityUUIDFix::updateAreaEffectCloud);
         input = this.updateNamedChoice(input, "minecraft:zombie_villager", EntityUUIDFix::updateZombieVillager);
         input = this.updateNamedChoice(input, "minecraft:evoker_fangs", EntityUUIDFix::updateEvokerFangs);
         return this.updateNamedChoice(input, "minecraft:piglin", EntityUUIDFix::updatePiglin);
      });
   }

   private static Dynamic<?> updatePiglin(final Dynamic<?> tag) {
      return tag.update(
         "Brain",
         brain -> brain.update(
            "memories", memories -> memories.update("minecraft:angry_at", angryAt -> replaceUUIDString(angryAt, "value", "value").orElseGet(() -> {
               LOGGER.warn("angry_at has no value.");
               return angryAt;
            }))
         )
      );
   }

   private static Dynamic<?> updateEvokerFangs(final Dynamic<?> tag) {
      return replaceUUIDLeastMost(tag, "OwnerUUID", "Owner").orElse(tag);
   }

   private static Dynamic<?> updateZombieVillager(final Dynamic<?> tag) {
      return replaceUUIDLeastMost(tag, "ConversionPlayer", "ConversionPlayer").orElse(tag);
   }

   private static Dynamic<?> updateAreaEffectCloud(final Dynamic<?> tag) {
      return replaceUUIDLeastMost(tag, "OwnerUUID", "Owner").orElse(tag);
   }

   private static Dynamic<?> updateShulkerBullet(Dynamic<?> tag) {
      tag = replaceUUIDMLTag(tag, "Owner", "Owner").orElse(tag);
      return replaceUUIDMLTag(tag, "Target", "Target").orElse(tag);
   }

   private static Dynamic<?> updateItem(Dynamic<?> tag) {
      tag = replaceUUIDMLTag(tag, "Owner", "Owner").orElse(tag);
      return replaceUUIDMLTag(tag, "Thrower", "Thrower").orElse(tag);
   }

   private static Dynamic<?> updateFox(final Dynamic<?> tag) {
      Optional<Dynamic<?>> trustedUUIDs = tag.get("TrustedUUIDs")
         .result()
         .map(uuidTags -> tag.createList(uuidTags.asStream().map(uuidTag -> (Dynamic)createUUIDFromML((Dynamic<?>)uuidTag).orElseGet(() -> {
            LOGGER.warn("Trusted contained invalid data.");
            return (Dynamic<?>)uuidTag;
         }))));
      return (Dynamic<?>)DataFixUtils.orElse(trustedUUIDs.map(trusted -> tag.remove("TrustedUUIDs").set("Trusted", trusted)), tag);
   }

   private static Dynamic<?> updateHurtBy(final Dynamic<?> tag) {
      return replaceUUIDString(tag, "HurtBy", "HurtBy").orElse(tag);
   }

   private static Dynamic<?> updateAnimalOwner(final Dynamic<?> tag) {
      Dynamic<?> fixed = updateAnimal(tag);
      return replaceUUIDString(fixed, "OwnerUUID", "Owner").orElse(fixed);
   }

   private static Dynamic<?> updateAnimal(final Dynamic<?> tag) {
      Dynamic<?> fixed = updateMob(tag);
      return replaceUUIDLeastMost(fixed, "LoveCause", "LoveCause").orElse(fixed);
   }

   private static Dynamic<?> updateMob(final Dynamic<?> tag) {
      return updateLivingEntity(tag).update("Leash", leashTag -> replaceUUIDLeastMost(leashTag, "UUID", "UUID").orElse(leashTag));
   }

   public static Dynamic<?> updateLivingEntity(final Dynamic<?> tag) {
      return tag.update(
         "Attributes",
         attributes -> tag.createList(
            attributes.asStream()
               .map(
                  attribute -> attribute.update(
                     "Modifiers",
                     modifiers -> attribute.createList(
                        modifiers.asStream().map(modifier -> (Dynamic)replaceUUIDLeastMost((Dynamic<?>)modifier, "UUID", "UUID").orElse((Dynamic<?>)modifier))
                     )
                  )
               )
         )
      );
   }

   private static Dynamic<?> updateProjectile(final Dynamic<?> tag) {
      return (Dynamic<?>)DataFixUtils.orElse(tag.get("OwnerUUID").result().map(owner -> tag.remove("OwnerUUID").set("Owner", owner)), tag);
   }

   public static Dynamic<?> updateEntityUUID(final Dynamic<?> tag) {
      return replaceUUIDLeastMost(tag, "UUID", "UUID").orElse(tag);
   }

   static {
      ABSTRACT_HORSES.add("minecraft:donkey");
      ABSTRACT_HORSES.add("minecraft:horse");
      ABSTRACT_HORSES.add("minecraft:llama");
      ABSTRACT_HORSES.add("minecraft:mule");
      ABSTRACT_HORSES.add("minecraft:skeleton_horse");
      ABSTRACT_HORSES.add("minecraft:trader_llama");
      ABSTRACT_HORSES.add("minecraft:zombie_horse");
      TAMEABLE_ANIMALS.add("minecraft:cat");
      TAMEABLE_ANIMALS.add("minecraft:parrot");
      TAMEABLE_ANIMALS.add("minecraft:wolf");
      ANIMALS.add("minecraft:bee");
      ANIMALS.add("minecraft:chicken");
      ANIMALS.add("minecraft:cow");
      ANIMALS.add("minecraft:fox");
      ANIMALS.add("minecraft:mooshroom");
      ANIMALS.add("minecraft:ocelot");
      ANIMALS.add("minecraft:panda");
      ANIMALS.add("minecraft:pig");
      ANIMALS.add("minecraft:polar_bear");
      ANIMALS.add("minecraft:rabbit");
      ANIMALS.add("minecraft:sheep");
      ANIMALS.add("minecraft:turtle");
      ANIMALS.add("minecraft:hoglin");
      MOBS.add("minecraft:bat");
      MOBS.add("minecraft:blaze");
      MOBS.add("minecraft:cave_spider");
      MOBS.add("minecraft:cod");
      MOBS.add("minecraft:creeper");
      MOBS.add("minecraft:dolphin");
      MOBS.add("minecraft:drowned");
      MOBS.add("minecraft:elder_guardian");
      MOBS.add("minecraft:ender_dragon");
      MOBS.add("minecraft:enderman");
      MOBS.add("minecraft:endermite");
      MOBS.add("minecraft:evoker");
      MOBS.add("minecraft:ghast");
      MOBS.add("minecraft:giant");
      MOBS.add("minecraft:guardian");
      MOBS.add("minecraft:husk");
      MOBS.add("minecraft:illusioner");
      MOBS.add("minecraft:magma_cube");
      MOBS.add("minecraft:pufferfish");
      MOBS.add("minecraft:zombified_piglin");
      MOBS.add("minecraft:salmon");
      MOBS.add("minecraft:shulker");
      MOBS.add("minecraft:silverfish");
      MOBS.add("minecraft:skeleton");
      MOBS.add("minecraft:slime");
      MOBS.add("minecraft:snow_golem");
      MOBS.add("minecraft:spider");
      MOBS.add("minecraft:squid");
      MOBS.add("minecraft:stray");
      MOBS.add("minecraft:tropical_fish");
      MOBS.add("minecraft:vex");
      MOBS.add("minecraft:villager");
      MOBS.add("minecraft:iron_golem");
      MOBS.add("minecraft:vindicator");
      MOBS.add("minecraft:pillager");
      MOBS.add("minecraft:wandering_trader");
      MOBS.add("minecraft:witch");
      MOBS.add("minecraft:wither");
      MOBS.add("minecraft:wither_skeleton");
      MOBS.add("minecraft:zombie");
      MOBS.add("minecraft:zombie_villager");
      MOBS.add("minecraft:phantom");
      MOBS.add("minecraft:ravager");
      MOBS.add("minecraft:piglin");
      LIVING_ENTITIES.add("minecraft:armor_stand");
      PROJECTILES.add("minecraft:arrow");
      PROJECTILES.add("minecraft:dragon_fireball");
      PROJECTILES.add("minecraft:firework_rocket");
      PROJECTILES.add("minecraft:fireball");
      PROJECTILES.add("minecraft:llama_spit");
      PROJECTILES.add("minecraft:small_fireball");
      PROJECTILES.add("minecraft:snowball");
      PROJECTILES.add("minecraft:spectral_arrow");
      PROJECTILES.add("minecraft:egg");
      PROJECTILES.add("minecraft:ender_pearl");
      PROJECTILES.add("minecraft:experience_bottle");
      PROJECTILES.add("minecraft:potion");
      PROJECTILES.add("minecraft:trident");
      PROJECTILES.add("minecraft:wither_skull");
   }
}
