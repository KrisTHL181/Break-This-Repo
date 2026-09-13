package net.minecraft.world.entity.ai.attributes;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.animal.equine.ZombieHorse;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.animal.nautilus.Nautilus;
import net.minecraft.world.entity.animal.nautilus.ZombieNautilus;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.animal.squid.GlowSquid;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.monster.illager.Illusioner;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.Bogged;
import net.minecraft.world.entity.monster.skeleton.Parched;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

public class DefaultAttributes {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Map<EntityType<? extends LivingEntity>, AttributeSupplier> SUPPLIERS = ImmutableMap.builder()
      .put(EntityTypes.ALLAY, Allay.createAttributes().build())
      .put(EntityTypes.ARMADILLO, Armadillo.createAttributes().build())
      .put(EntityTypes.ARMOR_STAND, ArmorStand.createAttributes().build())
      .put(EntityTypes.AXOLOTL, Axolotl.createAttributes().build())
      .put(EntityTypes.BAT, Bat.createAttributes().build())
      .put(EntityTypes.BEE, Bee.createAttributes().build())
      .put(EntityTypes.BLAZE, Blaze.createAttributes().build())
      .put(EntityTypes.BOGGED, Bogged.createAttributes().build())
      .put(EntityTypes.CAT, Cat.createAttributes().build())
      .put(EntityTypes.CAMEL, Camel.createAttributes().build())
      .put(EntityTypes.CAMEL_HUSK, Camel.createAttributes().build())
      .put(EntityTypes.CAVE_SPIDER, CaveSpider.createCaveSpider().build())
      .put(EntityTypes.CHICKEN, Chicken.createAttributes().build())
      .put(EntityTypes.COD, AbstractFish.createAttributes().build())
      .put(EntityTypes.COPPER_GOLEM, CopperGolem.createAttributes().build())
      .put(EntityTypes.COW, Cow.createAttributes().build())
      .put(EntityTypes.CREAKING, Creaking.createAttributes().build())
      .put(EntityTypes.CREEPER, Creeper.createAttributes().build())
      .put(EntityTypes.DOLPHIN, Dolphin.createAttributes().build())
      .put(EntityTypes.DONKEY, AbstractChestedHorse.createBaseChestedHorseAttributes().build())
      .put(EntityTypes.DROWNED, Drowned.createAttributes().build())
      .put(EntityTypes.ELDER_GUARDIAN, ElderGuardian.createAttributes().build())
      .put(EntityTypes.ENDERMAN, EnderMan.createAttributes().build())
      .put(EntityTypes.ENDERMITE, Endermite.createAttributes().build())
      .put(EntityTypes.ENDER_DRAGON, EnderDragon.createAttributes().build())
      .put(EntityTypes.EVOKER, Evoker.createAttributes().build())
      .put(EntityTypes.BREEZE, Breeze.createAttributes().build())
      .put(EntityTypes.FOX, Fox.createAttributes().build())
      .put(EntityTypes.FROG, Frog.createAttributes().build())
      .put(EntityTypes.GHAST, Ghast.createAttributes().build())
      .put(EntityTypes.HAPPY_GHAST, HappyGhast.createAttributes().build())
      .put(EntityTypes.GIANT, Giant.createAttributes().build())
      .put(EntityTypes.GLOW_SQUID, GlowSquid.createAttributes().build())
      .put(EntityTypes.GOAT, Goat.createAttributes().build())
      .put(EntityTypes.GUARDIAN, Guardian.createAttributes().build())
      .put(EntityTypes.HOGLIN, Hoglin.createAttributes().build())
      .put(EntityTypes.HORSE, AbstractHorse.createBaseHorseAttributes().build())
      .put(EntityTypes.HUSK, Zombie.createAttributes().build())
      .put(EntityTypes.ILLUSIONER, Illusioner.createAttributes().build())
      .put(EntityTypes.IRON_GOLEM, IronGolem.createAttributes().build())
      .put(EntityTypes.LLAMA, Llama.createAttributes().build())
      .put(EntityTypes.MAGMA_CUBE, MagmaCube.createAttributes().build())
      .put(EntityTypes.SULFUR_CUBE, SulfurCube.createSulfurCubeAttributes().build())
      .put(EntityTypes.MANNEQUIN, LivingEntity.createLivingAttributes().build())
      .put(EntityTypes.MOOSHROOM, Cow.createAttributes().build())
      .put(EntityTypes.MULE, AbstractChestedHorse.createBaseChestedHorseAttributes().build())
      .put(EntityTypes.NAUTILUS, Nautilus.createAttributes().build())
      .put(EntityTypes.OCELOT, Ocelot.createAttributes().build())
      .put(EntityTypes.PANDA, Panda.createAttributes().build())
      .put(EntityTypes.PARCHED, Parched.createAttributes().build())
      .put(EntityTypes.PARROT, Parrot.createAttributes().build())
      .put(EntityTypes.PHANTOM, Monster.createMonsterAttributes().build())
      .put(EntityTypes.PIG, Pig.createAttributes().build())
      .put(EntityTypes.PIGLIN, Piglin.createAttributes().build())
      .put(EntityTypes.PIGLIN_BRUTE, PiglinBrute.createAttributes().build())
      .put(EntityTypes.PILLAGER, Pillager.createAttributes().build())
      .put(EntityTypes.PLAYER, Player.createAttributes().build())
      .put(EntityTypes.POLAR_BEAR, PolarBear.createAttributes().build())
      .put(EntityTypes.PUFFERFISH, AbstractFish.createAttributes().build())
      .put(EntityTypes.RABBIT, Rabbit.createAttributes().build())
      .put(EntityTypes.RAVAGER, Ravager.createAttributes().build())
      .put(EntityTypes.SALMON, AbstractFish.createAttributes().build())
      .put(EntityTypes.SHEEP, Sheep.createAttributes().build())
      .put(EntityTypes.SHULKER, Shulker.createAttributes().build())
      .put(EntityTypes.SILVERFISH, Silverfish.createAttributes().build())
      .put(EntityTypes.SKELETON, AbstractSkeleton.createAttributes().build())
      .put(EntityTypes.SKELETON_HORSE, SkeletonHorse.createAttributes().build())
      .put(EntityTypes.SLIME, Monster.createMonsterAttributes().build())
      .put(EntityTypes.SNIFFER, Sniffer.createAttributes().build())
      .put(EntityTypes.SNOW_GOLEM, SnowGolem.createAttributes().build())
      .put(EntityTypes.SPIDER, Spider.createAttributes().build())
      .put(EntityTypes.SQUID, Squid.createAttributes().build())
      .put(EntityTypes.STRAY, AbstractSkeleton.createAttributes().build())
      .put(EntityTypes.STRIDER, Strider.createAttributes().build())
      .put(EntityTypes.TADPOLE, Tadpole.createAttributes().build())
      .put(EntityTypes.TRADER_LLAMA, Llama.createAttributes().build())
      .put(EntityTypes.TROPICAL_FISH, AbstractFish.createAttributes().build())
      .put(EntityTypes.TURTLE, Turtle.createAttributes().build())
      .put(EntityTypes.VEX, Vex.createAttributes().build())
      .put(EntityTypes.VILLAGER, Villager.createAttributes().build())
      .put(EntityTypes.VINDICATOR, Vindicator.createAttributes().build())
      .put(EntityTypes.WARDEN, Warden.createAttributes().build())
      .put(EntityTypes.WANDERING_TRADER, Mob.createMobAttributes().build())
      .put(EntityTypes.WITCH, Witch.createAttributes().build())
      .put(EntityTypes.WITHER, WitherBoss.createAttributes().build())
      .put(EntityTypes.WITHER_SKELETON, AbstractSkeleton.createAttributes().build())
      .put(EntityTypes.WOLF, Wolf.createAttributes().build())
      .put(EntityTypes.ZOGLIN, Zoglin.createAttributes().build())
      .put(EntityTypes.ZOMBIE, Zombie.createAttributes().build())
      .put(EntityTypes.ZOMBIE_HORSE, ZombieHorse.createAttributes().build())
      .put(EntityTypes.ZOMBIE_NAUTILUS, ZombieNautilus.createAttributes().build())
      .put(EntityTypes.ZOMBIE_VILLAGER, Zombie.createAttributes().build())
      .put(EntityTypes.ZOMBIFIED_PIGLIN, ZombifiedPiglin.createAttributes().build())
      .build();

   public static AttributeSupplier getSupplier(final EntityType<? extends LivingEntity> type) {
      return SUPPLIERS.get(type);
   }

   public static boolean hasSupplier(final EntityType<?> type) {
      return SUPPLIERS.containsKey(type);
   }

   public static void validate() {
      BuiltInRegistries.ENTITY_TYPE
         .stream()
         .filter(entityType -> entityType.getCategory() != MobCategory.MISC)
         .filter(entityType -> !hasSupplier((EntityType<?>)entityType))
         .map(BuiltInRegistries.ENTITY_TYPE::getKey)
         .forEach(id -> Util.logAndPauseIfInIde("Entity " + id + " has no attributes"));
   }
}
