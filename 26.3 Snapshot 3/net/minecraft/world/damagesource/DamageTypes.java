package net.minecraft.world.damagesource;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public interface DamageTypes {
   ResourceKey<DamageType> IN_FIRE = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("in_fire"));
   ResourceKey<DamageType> CAMPFIRE = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("campfire"));
   ResourceKey<DamageType> LIGHTNING_BOLT = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("lightning_bolt"));
   ResourceKey<DamageType> ON_FIRE = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("on_fire"));
   ResourceKey<DamageType> LAVA = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("lava"));
   ResourceKey<DamageType> HOT_FLOOR = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("hot_floor"));
   ResourceKey<DamageType> SULFUR_CUBE_HOT = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("sulfur_cube_hot"));
   ResourceKey<DamageType> IN_WALL = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("in_wall"));
   ResourceKey<DamageType> CRAMMING = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("cramming"));
   ResourceKey<DamageType> DROWN = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("drown"));
   ResourceKey<DamageType> STARVE = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("starve"));
   ResourceKey<DamageType> CACTUS = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("cactus"));
   ResourceKey<DamageType> FALL = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("fall"));
   ResourceKey<DamageType> ENDER_PEARL = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("ender_pearl"));
   ResourceKey<DamageType> FLY_INTO_WALL = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("fly_into_wall"));
   ResourceKey<DamageType> FELL_OUT_OF_WORLD = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("out_of_world"));
   ResourceKey<DamageType> GENERIC = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("generic"));
   ResourceKey<DamageType> MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("magic"));
   ResourceKey<DamageType> WITHER = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("wither"));
   ResourceKey<DamageType> DRAGON_BREATH = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("dragon_breath"));
   ResourceKey<DamageType> DRY_OUT = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("dry_out"));
   ResourceKey<DamageType> SWEET_BERRY_BUSH = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("sweet_berry_bush"));
   ResourceKey<DamageType> FREEZE = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("freeze"));
   ResourceKey<DamageType> STALAGMITE = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("stalagmite"));
   ResourceKey<DamageType> FALLING_BLOCK = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("falling_block"));
   ResourceKey<DamageType> FALLING_ANVIL = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("falling_anvil"));
   ResourceKey<DamageType> FALLING_STALACTITE = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("falling_stalactite"));
   ResourceKey<DamageType> STING = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("sting"));
   ResourceKey<DamageType> MOB_ATTACK = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("mob_attack"));
   ResourceKey<DamageType> MOB_ATTACK_NO_AGGRO = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("mob_attack_no_aggro"));
   ResourceKey<DamageType> PLAYER_ATTACK = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("player_attack"));
   ResourceKey<DamageType> SPEAR = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("spear"));
   ResourceKey<DamageType> ARROW = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("arrow"));
   ResourceKey<DamageType> TRIDENT = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("trident"));
   ResourceKey<DamageType> MOB_PROJECTILE = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("mob_projectile"));
   ResourceKey<DamageType> SPIT = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("spit"));
   ResourceKey<DamageType> WIND_CHARGE = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("wind_charge"));
   ResourceKey<DamageType> FIREWORKS = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("fireworks"));
   ResourceKey<DamageType> FIREBALL = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("fireball"));
   ResourceKey<DamageType> UNATTRIBUTED_FIREBALL = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("unattributed_fireball"));
   ResourceKey<DamageType> WITHER_SKULL = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("wither_skull"));
   ResourceKey<DamageType> THROWN = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("thrown"));
   ResourceKey<DamageType> INDIRECT_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("indirect_magic"));
   ResourceKey<DamageType> THORNS = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("thorns"));
   ResourceKey<DamageType> EXPLOSION = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("explosion"));
   ResourceKey<DamageType> PLAYER_EXPLOSION = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("player_explosion"));
   ResourceKey<DamageType> SONIC_BOOM = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("sonic_boom"));
   ResourceKey<DamageType> BAD_RESPAWN_POINT = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("bad_respawn_point"));
   ResourceKey<DamageType> OUTSIDE_BORDER = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("outside_border"));
   ResourceKey<DamageType> GENERIC_KILL = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("generic_kill"));
   ResourceKey<DamageType> MACE_SMASH = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("mace_smash"));

   static void bootstrap(final BootstrapContext<DamageType> context) {
      context.register(IN_FIRE, new DamageType("inFire", 0.1F, DamageEffects.BURNING));
      context.register(CAMPFIRE, new DamageType("inFire", 0.1F, DamageEffects.BURNING));
      context.register(LIGHTNING_BOLT, new DamageType("lightningBolt", 0.1F));
      context.register(ON_FIRE, new DamageType("onFire", 0.0F, DamageEffects.BURNING));
      context.register(LAVA, new DamageType("lava", 0.1F, DamageEffects.BURNING));
      context.register(HOT_FLOOR, new DamageType("hotFloor", 0.1F, DamageEffects.BURNING));
      context.register(SULFUR_CUBE_HOT, new DamageType("sulfurCubeHot", 0.1F, DamageEffects.BURNING));
      context.register(IN_WALL, new DamageType("inWall", 0.0F));
      context.register(CRAMMING, new DamageType("cramming", 0.0F));
      context.register(DROWN, new DamageType("drown", 0.0F, DamageEffects.DROWNING));
      context.register(STARVE, new DamageType("starve", 0.0F));
      context.register(CACTUS, new DamageType("cactus", 0.1F));
      context.register(FALL, new DamageType("fall", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.0F, DamageEffects.HURT, DeathMessageType.FALL_VARIANTS));
      context.register(
         ENDER_PEARL, new DamageType("fall", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.0F, DamageEffects.HURT, DeathMessageType.FALL_VARIANTS)
      );
      context.register(FLY_INTO_WALL, new DamageType("flyIntoWall", 0.0F));
      context.register(FELL_OUT_OF_WORLD, new DamageType("outOfWorld", 0.0F));
      context.register(GENERIC, new DamageType("generic", 0.0F));
      context.register(MAGIC, new DamageType("magic", 0.0F));
      context.register(WITHER, new DamageType("wither", 0.0F));
      context.register(DRAGON_BREATH, new DamageType("dragonBreath", 0.0F));
      context.register(DRY_OUT, new DamageType("dryout", 0.1F));
      context.register(SWEET_BERRY_BUSH, new DamageType("sweetBerryBush", 0.1F, DamageEffects.POKING));
      context.register(FREEZE, new DamageType("freeze", 0.0F, DamageEffects.FREEZING));
      context.register(STALAGMITE, new DamageType("stalagmite", 0.0F));
      context.register(FALLING_BLOCK, new DamageType("fallingBlock", 0.1F));
      context.register(FALLING_ANVIL, new DamageType("anvil", 0.1F));
      context.register(FALLING_STALACTITE, new DamageType("fallingStalactite", 0.1F));
      context.register(STING, new DamageType("sting", 0.1F));
      context.register(MOB_ATTACK, new DamageType("mob", 0.1F));
      context.register(MOB_ATTACK_NO_AGGRO, new DamageType("mob", 0.1F));
      context.register(PLAYER_ATTACK, new DamageType("player", 0.1F));
      context.register(SPEAR, new DamageType("spear", 0.1F));
      context.register(ARROW, new DamageType("arrow", 0.1F));
      context.register(TRIDENT, new DamageType("trident", 0.1F));
      context.register(MOB_PROJECTILE, new DamageType("mob", 0.1F));
      context.register(SPIT, new DamageType("mob", 0.1F));
      context.register(FIREWORKS, new DamageType("fireworks", 0.1F));
      context.register(UNATTRIBUTED_FIREBALL, new DamageType("onFire", 0.1F, DamageEffects.BURNING));
      context.register(FIREBALL, new DamageType("fireball", 0.1F, DamageEffects.BURNING));
      context.register(WITHER_SKULL, new DamageType("witherSkull", 0.1F));
      context.register(THROWN, new DamageType("thrown", 0.1F));
      context.register(INDIRECT_MAGIC, new DamageType("indirectMagic", 0.0F));
      context.register(THORNS, new DamageType("thorns", 0.1F, DamageEffects.THORNS));
      context.register(EXPLOSION, new DamageType("explosion", DamageScaling.ALWAYS, 0.1F));
      context.register(PLAYER_EXPLOSION, new DamageType("explosion.player", DamageScaling.ALWAYS, 0.1F));
      context.register(SONIC_BOOM, new DamageType("sonic_boom", DamageScaling.ALWAYS, 0.0F));
      context.register(
         BAD_RESPAWN_POINT, new DamageType("badRespawnPoint", DamageScaling.ALWAYS, 0.1F, DamageEffects.HURT, DeathMessageType.INTENTIONAL_GAME_DESIGN)
      );
      context.register(OUTSIDE_BORDER, new DamageType("outsideBorder", 0.0F));
      context.register(GENERIC_KILL, new DamageType("genericKill", 0.0F));
      context.register(WIND_CHARGE, new DamageType("mob", 0.1F));
      context.register(MACE_SMASH, new DamageType("mace_smash", 0.1F));
   }
}
