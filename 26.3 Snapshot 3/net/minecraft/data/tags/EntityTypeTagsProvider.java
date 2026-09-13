package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypeIds;

public class EntityTypeTagsProvider extends TagsProvider<EntityType<?>> {
   public EntityTypeTagsProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> lookupProvider) {
      super(output, Registries.ENTITY_TYPE, lookupProvider);
   }

   @Override
   protected void addTags(final HolderLookup.Provider registries) {
      this.tag(EntityTypeTags.SKELETONS)
         .add(
            EntityTypeIds.SKELETON,
            EntityTypeIds.STRAY,
            EntityTypeIds.WITHER_SKELETON,
            EntityTypeIds.SKELETON_HORSE,
            EntityTypeIds.BOGGED,
            EntityTypeIds.PARCHED
         );
      this.tag(EntityTypeTags.ZOMBIES)
         .add(
            EntityTypeIds.ZOMBIE_HORSE,
            EntityTypeIds.CAMEL_HUSK,
            EntityTypeIds.ZOMBIE,
            EntityTypeIds.ZOMBIE_VILLAGER,
            EntityTypeIds.ZOMBIFIED_PIGLIN,
            EntityTypeIds.ZOGLIN,
            EntityTypeIds.DROWNED,
            EntityTypeIds.HUSK,
            EntityTypeIds.ZOMBIE_NAUTILUS
         );
      this.tag(EntityTypeTags.RAIDERS)
         .add(EntityTypeIds.EVOKER, EntityTypeIds.PILLAGER, EntityTypeIds.RAVAGER, EntityTypeIds.VINDICATOR, EntityTypeIds.ILLUSIONER, EntityTypeIds.WITCH);
      this.tag(EntityTypeTags.UNDEAD).addTag(EntityTypeTags.SKELETONS).addTag(EntityTypeTags.ZOMBIES).add(EntityTypeIds.WITHER).add(EntityTypeIds.PHANTOM);
      this.tag(EntityTypeTags.BURN_IN_DAYLIGHT)
         .add(EntityTypeIds.SKELETON, EntityTypeIds.STRAY, EntityTypeIds.WITHER_SKELETON, EntityTypeIds.BOGGED)
         .add(EntityTypeIds.ZOMBIE, EntityTypeIds.ZOMBIE_HORSE, EntityTypeIds.ZOMBIE_VILLAGER, EntityTypeIds.DROWNED, EntityTypeIds.ZOMBIE_NAUTILUS)
         .add(EntityTypeIds.PHANTOM);
      this.tag(EntityTypeTags.BEEHIVE_INHABITORS).add(EntityTypeIds.BEE);
      this.tag(EntityTypeTags.ARROWS).add(EntityTypeIds.ARROW, EntityTypeIds.SPECTRAL_ARROW);
      this.tag(EntityTypeTags.IMPACT_PROJECTILES)
         .addTag(EntityTypeTags.ARROWS)
         .add(EntityTypeIds.FIREWORK_ROCKET)
         .add(
            EntityTypeIds.SNOWBALL,
            EntityTypeIds.FIREBALL,
            EntityTypeIds.SMALL_FIREBALL,
            EntityTypeIds.EGG,
            EntityTypeIds.TRIDENT,
            EntityTypeIds.DRAGON_FIREBALL,
            EntityTypeIds.WITHER_SKULL,
            EntityTypeIds.WIND_CHARGE,
            EntityTypeIds.BREEZE_WIND_CHARGE
         );
      this.tag(EntityTypeTags.POWDER_SNOW_WALKABLE_MOBS).add(EntityTypeIds.RABBIT, EntityTypeIds.ENDERMITE, EntityTypeIds.SILVERFISH, EntityTypeIds.FOX);
      this.tag(EntityTypeTags.AXOLOTL_HUNT_TARGETS)
         .add(
            EntityTypeIds.TROPICAL_FISH,
            EntityTypeIds.PUFFERFISH,
            EntityTypeIds.SALMON,
            EntityTypeIds.COD,
            EntityTypeIds.SQUID,
            EntityTypeIds.GLOW_SQUID,
            EntityTypeIds.TADPOLE
         );
      this.tag(EntityTypeTags.AXOLOTL_ALWAYS_HOSTILES).add(EntityTypeIds.DROWNED, EntityTypeIds.GUARDIAN, EntityTypeIds.ELDER_GUARDIAN);
      this.tag(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES).add(EntityTypeIds.STRAY, EntityTypeIds.POLAR_BEAR, EntityTypeIds.SNOW_GOLEM, EntityTypeIds.WITHER);
      this.tag(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES).add(EntityTypeIds.STRIDER, EntityTypeIds.BLAZE, EntityTypeIds.MAGMA_CUBE);
      this.tag(EntityTypeTags.CAN_BREATHE_UNDER_WATER)
         .addTag(EntityTypeTags.UNDEAD)
         .add(
            EntityTypeIds.AXOLOTL,
            EntityTypeIds.FROG,
            EntityTypeIds.GUARDIAN,
            EntityTypeIds.ELDER_GUARDIAN,
            EntityTypeIds.TURTLE,
            EntityTypeIds.GLOW_SQUID,
            EntityTypeIds.COD,
            EntityTypeIds.PUFFERFISH,
            EntityTypeIds.SALMON,
            EntityTypeIds.SQUID,
            EntityTypeIds.TROPICAL_FISH,
            EntityTypeIds.TADPOLE,
            EntityTypeIds.ARMOR_STAND,
            EntityTypeIds.COPPER_GOLEM,
            EntityTypeIds.NAUTILUS
         );
      this.tag(EntityTypeTags.FROG_FOOD).add(EntityTypeIds.SLIME, EntityTypeIds.MAGMA_CUBE);
      this.tag(EntityTypeTags.FALL_DAMAGE_IMMUNE)
         .add(
            EntityTypeIds.COPPER_GOLEM,
            EntityTypeIds.IRON_GOLEM,
            EntityTypeIds.SNOW_GOLEM,
            EntityTypeIds.SHULKER,
            EntityTypeIds.ALLAY,
            EntityTypeIds.BAT,
            EntityTypeIds.BEE,
            EntityTypeIds.BLAZE,
            EntityTypeIds.CAT,
            EntityTypeIds.CHICKEN,
            EntityTypeIds.GHAST,
            EntityTypeIds.HAPPY_GHAST,
            EntityTypeIds.PHANTOM,
            EntityTypeIds.MAGMA_CUBE,
            EntityTypeIds.OCELOT,
            EntityTypeIds.PARROT,
            EntityTypeIds.WITHER,
            EntityTypeIds.BREEZE
         );
      this.tag(EntityTypeTags.DISMOUNTS_UNDERWATER)
         .add(
            EntityTypeIds.CAMEL,
            EntityTypeIds.CHICKEN,
            EntityTypeIds.DONKEY,
            EntityTypeIds.HAPPY_GHAST,
            EntityTypeIds.HORSE,
            EntityTypeIds.LLAMA,
            EntityTypeIds.MULE,
            EntityTypeIds.PIG,
            EntityTypeIds.RAVAGER,
            EntityTypeIds.SPIDER,
            EntityTypeIds.STRIDER,
            EntityTypeIds.TRADER_LLAMA,
            EntityTypeIds.ZOMBIE_HORSE
         );
      this.tag(EntityTypeTags.NON_CONTROLLING_RIDER).add(EntityTypeIds.SLIME, EntityTypeIds.MAGMA_CUBE, EntityTypeIds.SULFUR_CUBE);
      this.tag(EntityTypeTags.ILLAGER).add(EntityTypeIds.EVOKER).add(EntityTypeIds.ILLUSIONER).add(EntityTypeIds.PILLAGER).add(EntityTypeIds.VINDICATOR);
      this.tag(EntityTypeTags.AQUATIC)
         .add(EntityTypeIds.TURTLE)
         .add(EntityTypeIds.AXOLOTL)
         .add(EntityTypeIds.GUARDIAN)
         .add(EntityTypeIds.ELDER_GUARDIAN)
         .add(EntityTypeIds.COD)
         .add(EntityTypeIds.PUFFERFISH)
         .add(EntityTypeIds.SALMON)
         .add(EntityTypeIds.TROPICAL_FISH)
         .add(EntityTypeIds.DOLPHIN)
         .add(EntityTypeIds.SQUID)
         .add(EntityTypeIds.GLOW_SQUID)
         .add(EntityTypeIds.TADPOLE)
         .add(EntityTypeIds.NAUTILUS)
         .add(EntityTypeIds.ZOMBIE_NAUTILUS);
      this.tag(EntityTypeTags.ARTHROPOD)
         .add(EntityTypeIds.BEE)
         .add(EntityTypeIds.ENDERMITE)
         .add(EntityTypeIds.SILVERFISH)
         .add(EntityTypeIds.SPIDER)
         .add(EntityTypeIds.CAVE_SPIDER);
      this.tag(EntityTypeTags.IGNORES_POISON_AND_REGEN).addTag(EntityTypeTags.UNDEAD);
      this.tag(EntityTypeTags.INVERTED_HEALING_AND_HARM).addTag(EntityTypeTags.UNDEAD);
      this.tag(EntityTypeTags.WITHER_FRIENDS).addTag(EntityTypeTags.UNDEAD);
      this.tag(EntityTypeTags.ILLAGER_FRIENDS).addTag(EntityTypeTags.ILLAGER);
      this.tag(EntityTypeTags.NOT_SCARY_FOR_PUFFERFISH)
         .add(EntityTypeIds.TURTLE)
         .add(EntityTypeIds.GUARDIAN)
         .add(EntityTypeIds.ELDER_GUARDIAN)
         .add(EntityTypeIds.COD)
         .add(EntityTypeIds.PUFFERFISH)
         .add(EntityTypeIds.SALMON)
         .add(EntityTypeIds.TROPICAL_FISH)
         .add(EntityTypeIds.DOLPHIN)
         .add(EntityTypeIds.SQUID)
         .add(EntityTypeIds.GLOW_SQUID)
         .add(EntityTypeIds.TADPOLE)
         .add(EntityTypeIds.NAUTILUS)
         .add(EntityTypeIds.ZOMBIE_NAUTILUS)
         .add(EntityTypeIds.SULFUR_CUBE);
      this.tag(EntityTypeTags.SENSITIVE_TO_IMPALING).addTag(EntityTypeTags.AQUATIC);
      this.tag(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS).addTag(EntityTypeTags.ARTHROPOD);
      this.tag(EntityTypeTags.SENSITIVE_TO_SMITE).addTag(EntityTypeTags.UNDEAD);
      this.tag(EntityTypeTags.REDIRECTABLE_PROJECTILE).add(EntityTypeIds.FIREBALL, EntityTypeIds.WIND_CHARGE, EntityTypeIds.BREEZE_WIND_CHARGE);
      this.tag(EntityTypeTags.DEFLECTS_PROJECTILES).add(EntityTypeIds.BREEZE);
      this.tag(EntityTypeTags.CAN_TURN_IN_BOATS).add(EntityTypeIds.BREEZE);
      this.tag(EntityTypeTags.NO_ANGER_FROM_WIND_CHARGE)
         .add(
            EntityTypeIds.BREEZE,
            EntityTypeIds.SKELETON,
            EntityTypeIds.BOGGED,
            EntityTypeIds.STRAY,
            EntityTypeIds.ZOMBIE,
            EntityTypeIds.HUSK,
            EntityTypeIds.SPIDER,
            EntityTypeIds.CAVE_SPIDER,
            EntityTypeIds.SLIME
         );
      this.tag(EntityTypeTags.IMMUNE_TO_INFESTED).add(EntityTypeIds.SILVERFISH);
      this.tag(EntityTypeTags.IMMUNE_TO_OOZING).add(EntityTypeIds.SLIME);
      this.tag(EntityTypeTags.BOAT)
         .add(
            EntityTypeIds.OAK_BOAT,
            EntityTypeIds.SPRUCE_BOAT,
            EntityTypeIds.BIRCH_BOAT,
            EntityTypeIds.JUNGLE_BOAT,
            EntityTypeIds.ACACIA_BOAT,
            EntityTypeIds.CHERRY_BOAT,
            EntityTypeIds.DARK_OAK_BOAT,
            EntityTypeIds.PALE_OAK_BOAT,
            EntityTypeIds.MANGROVE_BOAT,
            EntityTypeIds.BAMBOO_RAFT,
            EntityTypeIds.POPLAR_BOAT
         );
      this.tag(EntityTypeTags.CAN_EQUIP_SADDLE)
         .add(
            EntityTypeIds.HORSE,
            EntityTypeIds.SKELETON_HORSE,
            EntityTypeIds.ZOMBIE_HORSE,
            EntityTypeIds.DONKEY,
            EntityTypeIds.MULE,
            EntityTypeIds.PIG,
            EntityTypeIds.STRIDER,
            EntityTypeIds.CAMEL,
            EntityTypeIds.CAMEL_HUSK,
            EntityTypeIds.NAUTILUS,
            EntityTypeIds.ZOMBIE_NAUTILUS
         );
      this.tag(EntityTypeTags.CAN_EQUIP_HARNESS).add(EntityTypeIds.HAPPY_GHAST);
      this.tag(EntityTypeTags.CAN_WEAR_HORSE_ARMOR).add(EntityTypeIds.HORSE).add(EntityTypeIds.ZOMBIE_HORSE);
      this.tag(EntityTypeTags.CAN_WEAR_NAUTILUS_ARMOR).add(EntityTypeIds.NAUTILUS, EntityTypeIds.ZOMBIE_NAUTILUS);
      this.tag(EntityTypeTags.FOLLOWABLE_FRIENDLY_MOBS)
         .add(
            EntityTypeIds.ARMADILLO,
            EntityTypeIds.BEE,
            EntityTypeIds.CAMEL,
            EntityTypeIds.CAT,
            EntityTypeIds.CHICKEN,
            EntityTypeIds.COW,
            EntityTypeIds.DONKEY,
            EntityTypeIds.FOX,
            EntityTypeIds.GOAT,
            EntityTypeIds.HAPPY_GHAST,
            EntityTypeIds.HORSE,
            EntityTypeIds.SKELETON_HORSE,
            EntityTypeIds.LLAMA,
            EntityTypeIds.MULE,
            EntityTypeIds.OCELOT,
            EntityTypeIds.PANDA,
            EntityTypeIds.PARROT,
            EntityTypeIds.PIG,
            EntityTypeIds.POLAR_BEAR,
            EntityTypeIds.RABBIT,
            EntityTypeIds.SHEEP,
            EntityTypeIds.SNIFFER,
            EntityTypeIds.STRIDER,
            EntityTypeIds.VILLAGER,
            EntityTypeIds.WOLF
         );
      this.tag(EntityTypeTags.CANNOT_BE_PUSHED_ONTO_BOATS)
         .add(EntityTypeIds.PLAYER)
         .add(EntityTypeIds.ELDER_GUARDIAN)
         .add(EntityTypeIds.COD)
         .add(EntityTypeIds.PUFFERFISH)
         .add(EntityTypeIds.SALMON)
         .add(EntityTypeIds.TROPICAL_FISH)
         .add(EntityTypeIds.DOLPHIN)
         .add(EntityTypeIds.SQUID)
         .add(EntityTypeIds.GLOW_SQUID)
         .add(EntityTypeIds.TADPOLE)
         .add(EntityTypeIds.CREAKING)
         .add(EntityTypeIds.NAUTILUS)
         .add(EntityTypeIds.ZOMBIE_NAUTILUS)
         .add(EntityTypeIds.SULFUR_CUBE);
      this.tag(EntityTypeTags.ACCEPTS_IRON_GOLEM_GIFT).add(EntityTypeIds.COPPER_GOLEM);
      this.tag(EntityTypeTags.CANDIDATE_FOR_IRON_GOLEM_GIFT).add(EntityTypeIds.VILLAGER).addTag(EntityTypeTags.ACCEPTS_IRON_GOLEM_GIFT);
      this.tag(EntityTypeTags.NAUTILUS_HOSTILES).add(EntityTypeIds.PUFFERFISH);
      this.tag(EntityTypeTags.CAN_FLOAT_WHILE_RIDDEN)
         .add(EntityTypeIds.HORSE, EntityTypeIds.ZOMBIE_HORSE, EntityTypeIds.MULE, EntityTypeIds.DONKEY, EntityTypeIds.CAMEL, EntityTypeIds.CAMEL_HUSK);
      this.tag(EntityTypeTags.CANNOT_BE_AGE_LOCKED).add(EntityTypeIds.ZOMBIE_HORSE, EntityTypeIds.SKELETON_HORSE, EntityTypeIds.VILLAGER);
      this.tag(EntityTypeTags.NOT_AFFECTED_BY_GEYSERS).add(EntityTypeIds.ENDER_DRAGON);
      this.tag(EntityTypeTags.CANNOT_BE_DISMOUNTED_BY_ITEM_USAGE).add(EntityTypeIds.INTERACTION);
   }
}
