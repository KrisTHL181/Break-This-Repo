package net.minecraft.world.damagesource;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class DamageSources {
   private final Registry<DamageType> damageTypes;
   private final DamageSource inFire;
   private final DamageSource campfire;
   private final DamageSource lightningBolt;
   private final DamageSource onFire;
   private final DamageSource lava;
   private final DamageSource hotFloor;
   private final DamageSource inWall;
   private final DamageSource cramming;
   private final DamageSource drown;
   private final DamageSource starve;
   private final DamageSource cactus;
   private final DamageSource fall;
   private final DamageSource enderPearl;
   private final DamageSource flyIntoWall;
   private final DamageSource fellOutOfWorld;
   private final DamageSource generic;
   private final DamageSource magic;
   private final DamageSource wither;
   private final DamageSource dragonBreath;
   private final DamageSource dryOut;
   private final DamageSource sweetBerryBush;
   private final DamageSource freeze;
   private final DamageSource stalagmite;
   private final DamageSource outsideBorder;
   private final DamageSource genericKill;

   public DamageSources(final RegistryAccess registries) {
      this.damageTypes = registries.lookupOrThrow(Registries.DAMAGE_TYPE);
      this.inFire = this.source(DamageTypes.IN_FIRE);
      this.campfire = this.source(DamageTypes.CAMPFIRE);
      this.lightningBolt = this.source(DamageTypes.LIGHTNING_BOLT);
      this.onFire = this.source(DamageTypes.ON_FIRE);
      this.lava = this.source(DamageTypes.LAVA);
      this.hotFloor = this.source(DamageTypes.HOT_FLOOR);
      this.inWall = this.source(DamageTypes.IN_WALL);
      this.cramming = this.source(DamageTypes.CRAMMING);
      this.drown = this.source(DamageTypes.DROWN);
      this.starve = this.source(DamageTypes.STARVE);
      this.cactus = this.source(DamageTypes.CACTUS);
      this.fall = this.source(DamageTypes.FALL);
      this.enderPearl = this.source(DamageTypes.ENDER_PEARL);
      this.flyIntoWall = this.source(DamageTypes.FLY_INTO_WALL);
      this.fellOutOfWorld = this.source(DamageTypes.FELL_OUT_OF_WORLD);
      this.generic = this.source(DamageTypes.GENERIC);
      this.magic = this.source(DamageTypes.MAGIC);
      this.wither = this.source(DamageTypes.WITHER);
      this.dragonBreath = this.source(DamageTypes.DRAGON_BREATH);
      this.dryOut = this.source(DamageTypes.DRY_OUT);
      this.sweetBerryBush = this.source(DamageTypes.SWEET_BERRY_BUSH);
      this.freeze = this.source(DamageTypes.FREEZE);
      this.stalagmite = this.source(DamageTypes.STALAGMITE);
      this.outsideBorder = this.source(DamageTypes.OUTSIDE_BORDER);
      this.genericKill = this.source(DamageTypes.GENERIC_KILL);
   }

   private DamageSource source(final ResourceKey<DamageType> key) {
      return new DamageSource(this.damageTypes.getOrThrow(key));
   }

   private DamageSource source(final ResourceKey<DamageType> key, final @Nullable Entity cause) {
      return new DamageSource(this.damageTypes.getOrThrow(key), cause);
   }

   private DamageSource source(final ResourceKey<DamageType> key, final @Nullable Entity directEntity, final @Nullable Entity causingEntity) {
      return new DamageSource(this.damageTypes.getOrThrow(key), directEntity, causingEntity);
   }

   public DamageSource inFire() {
      return this.inFire;
   }

   public DamageSource campfire() {
      return this.campfire;
   }

   public DamageSource lightningBolt() {
      return this.lightningBolt;
   }

   public DamageSource onFire() {
      return this.onFire;
   }

   public DamageSource lava() {
      return this.lava;
   }

   public DamageSource hotFloor() {
      return this.hotFloor;
   }

   public DamageSource inWall() {
      return this.inWall;
   }

   public DamageSource cramming() {
      return this.cramming;
   }

   public DamageSource drown() {
      return this.drown;
   }

   public DamageSource starve() {
      return this.starve;
   }

   public DamageSource cactus() {
      return this.cactus;
   }

   public DamageSource fall() {
      return this.fall;
   }

   public DamageSource enderPearl() {
      return this.enderPearl;
   }

   public DamageSource flyIntoWall() {
      return this.flyIntoWall;
   }

   public DamageSource fellOutOfWorld() {
      return this.fellOutOfWorld;
   }

   public DamageSource generic() {
      return this.generic;
   }

   public DamageSource magic() {
      return this.magic;
   }

   public DamageSource wither() {
      return this.wither;
   }

   public DamageSource dragonBreath() {
      return this.dragonBreath;
   }

   public DamageSource dryOut() {
      return this.dryOut;
   }

   public DamageSource sweetBerryBush() {
      return this.sweetBerryBush;
   }

   public DamageSource freeze() {
      return this.freeze;
   }

   public DamageSource stalagmite() {
      return this.stalagmite;
   }

   public DamageSource fallingBlock(final Entity entity) {
      return this.source(DamageTypes.FALLING_BLOCK, entity);
   }

   public DamageSource anvil(final Entity entity) {
      return this.source(DamageTypes.FALLING_ANVIL, entity);
   }

   public DamageSource fallingStalactite(final Entity entity) {
      return this.source(DamageTypes.FALLING_STALACTITE, entity);
   }

   public DamageSource sting(final LivingEntity mob) {
      return this.source(DamageTypes.STING, mob);
   }

   public DamageSource mobAttack(final LivingEntity mob) {
      return this.source(DamageTypes.MOB_ATTACK, mob);
   }

   public DamageSource noAggroMobAttack(final LivingEntity mob) {
      return this.source(DamageTypes.MOB_ATTACK_NO_AGGRO, mob);
   }

   public DamageSource playerAttack(final Player player) {
      return this.source(DamageTypes.PLAYER_ATTACK, player);
   }

   public DamageSource arrow(final AbstractArrow arrow, final @Nullable Entity owner) {
      return this.source(DamageTypes.ARROW, arrow, owner);
   }

   public DamageSource trident(final Entity trident, final @Nullable Entity owner) {
      return this.source(DamageTypes.TRIDENT, trident, owner);
   }

   public DamageSource mobProjectile(final Entity entity, final @Nullable LivingEntity mob) {
      return this.source(DamageTypes.MOB_PROJECTILE, entity, mob);
   }

   public DamageSource spit(final Entity entity, final @Nullable LivingEntity mob) {
      return this.source(DamageTypes.SPIT, entity, mob);
   }

   public DamageSource windCharge(final Entity entity, final @Nullable LivingEntity mob) {
      return this.source(DamageTypes.WIND_CHARGE, entity, mob);
   }

   public DamageSource fireworks(final FireworkRocketEntity rocket, final @Nullable Entity owner) {
      return this.source(DamageTypes.FIREWORKS, rocket, owner);
   }

   public DamageSource fireball(final Fireball fireball, final @Nullable Entity owner) {
      return owner == null ? this.source(DamageTypes.UNATTRIBUTED_FIREBALL, fireball) : this.source(DamageTypes.FIREBALL, fireball, owner);
   }

   public DamageSource witherSkull(final WitherSkull witherSkull, final Entity owner) {
      return this.source(DamageTypes.WITHER_SKULL, witherSkull, owner);
   }

   public DamageSource thrown(final Entity entity, final @Nullable Entity owner) {
      return this.source(DamageTypes.THROWN, entity, owner);
   }

   public DamageSource indirectMagic(final Entity entity, final @Nullable Entity owner) {
      return this.source(DamageTypes.INDIRECT_MAGIC, entity, owner);
   }

   public DamageSource thorns(final Entity source) {
      return this.source(DamageTypes.THORNS, source);
   }

   public DamageSource explosion(final @Nullable Explosion explosion) {
      return explosion != null ? this.explosion(explosion.getDirectSourceEntity(), explosion.getIndirectSourceEntity()) : this.explosion(null, null);
   }

   public DamageSource explosion(final @Nullable Entity entity, final @Nullable Entity cause) {
      return this.source(cause != null && entity != null ? DamageTypes.PLAYER_EXPLOSION : DamageTypes.EXPLOSION, entity, cause);
   }

   public DamageSource sonicBoom(final Entity entity) {
      return this.source(DamageTypes.SONIC_BOOM, entity);
   }

   public DamageSource badRespawnPointExplosion(final Vec3 boomPos) {
      return new DamageSource(this.damageTypes.getOrThrow(DamageTypes.BAD_RESPAWN_POINT), boomPos);
   }

   public DamageSource outOfBorder() {
      return this.outsideBorder;
   }

   public DamageSource genericKill() {
      return this.genericKill;
   }

   public DamageSource mace(final Entity owner) {
      return this.source(DamageTypes.MACE_SMASH, owner);
   }
}
