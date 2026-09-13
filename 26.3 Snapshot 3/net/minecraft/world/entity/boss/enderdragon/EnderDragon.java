package net.minecraft.world.entity.boss.enderdragon;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoveSimulationType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhaseManager;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.BinaryHeap;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class EnderDragon extends Mob implements Enemy {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(EnderDragon.class, EntityDataSerializers.INT);
   private static final TargetingConditions CRYSTAL_DESTROY_TARGETING = TargetingConditions.forCombat().range(64.0);
   private static final int GROWL_INTERVAL_MIN = 200;
   private static final int GROWL_INTERVAL_MAX = 400;
   private static final float SITTING_ALLOWED_DAMAGE_PERCENTAGE = 0.25F;
   private static final String DRAGON_DEATH_TIME_KEY = "DragonDeathTime";
   private static final String DRAGON_PHASE_KEY = "DragonPhase";
   private static final String SITTING_DAMAGE_RECEIVED_KEY = "sitting_damage_received";
   private static final int DEFAULT_DEATH_TIME = 0;
   public final DragonFlightHistory flightHistory = new DragonFlightHistory();
   private final EnderDragonPart[] subEntities;
   public final EnderDragonPart head;
   private final EnderDragonPart neck;
   private final EnderDragonPart body;
   private final EnderDragonPart tail1;
   private final EnderDragonPart tail2;
   private final EnderDragonPart tail3;
   private final EnderDragonPart wing1;
   private final EnderDragonPart wing2;
   public float oFlapTime;
   public float flapTime;
   public boolean inWall;
   public int dragonDeathTime = 0;
   public float yRotA;
   public @Nullable EndCrystal nearestCrystal;
   private @Nullable EnderDragonFight dragonFight;
   private BlockPos fightOrigin = BlockPos.ZERO;
   private final EnderDragonPhaseManager phaseManager;
   private int growlTime = 100;
   private float sittingDamageReceived;
   private final Node[] nodes = new Node[24];
   private final int[] nodeAdjacency = new int[24];
   private final BinaryHeap openSet = new BinaryHeap();

   public EnderDragon(final EntityType<? extends EnderDragon> type, final Level level) {
      super(EntityTypes.ENDER_DRAGON, level);
      this.head = new EnderDragonPart(this, "head", 1.0F, 1.0F);
      this.neck = new EnderDragonPart(this, "neck", 3.0F, 3.0F);
      this.body = new EnderDragonPart(this, "body", 5.0F, 3.0F);
      this.tail1 = new EnderDragonPart(this, "tail", 2.0F, 2.0F);
      this.tail2 = new EnderDragonPart(this, "tail", 2.0F, 2.0F);
      this.tail3 = new EnderDragonPart(this, "tail", 2.0F, 2.0F);
      this.wing1 = new EnderDragonPart(this, "wing", 4.0F, 2.0F);
      this.wing2 = new EnderDragonPart(this, "wing", 4.0F, 2.0F);
      this.subEntities = new EnderDragonPart[]{this.head, this.neck, this.body, this.tail1, this.tail2, this.tail3, this.wing1, this.wing2};
      this.setHealth(this.getMaxHealth());
      this.noPhysics = true;
      this.phaseManager = new EnderDragonPhaseManager(this);
   }

   public void setDragonFight(final EnderDragonFight fight) {
      this.dragonFight = fight;
   }

   public void setFightOrigin(final BlockPos fightOrigin) {
      this.fightOrigin = fightOrigin;
   }

   public BlockPos getFightOrigin() {
      return this.fightOrigin;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 200.0).add(Attributes.CAMERA_DISTANCE, 16.0);
   }

   @Override
   public boolean isFlapping() {
      float flap = Mth.cos(this.flapTime * (float) (Math.PI * 2));
      float oldFlap = Mth.cos(this.oFlapTime * (float) (Math.PI * 2));
      return oldFlap <= -0.3F && flap >= -0.3F;
   }

   @Override
   public void onFlap() {
      if (this.level().isClientSide() && !this.isSilent()) {
         this.level()
            .playLocalSound(
               this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_FLAP, this.getSoundSource(), 5.0F, 0.8F + this.random.nextFloat() * 0.3F, false
            );
      }
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(DATA_PHASE, EnderDragonPhase.HOVERING.getId());
   }

   @Override
   public void aiStep() {
      this.processFlappingMovement();
      if (this.level().isClientSide()) {
         this.setHealth(this.getHealth());
         if (!this.isSilent() && !this.phaseManager.getCurrentPhase().isSitting() && --this.growlTime < 0) {
            this.level()
               .playLocalSound(
                  this.getX(),
                  this.getY(),
                  this.getZ(),
                  SoundEvents.ENDER_DRAGON_GROWL,
                  this.getSoundSource(),
                  2.5F,
                  0.8F + this.random.nextFloat() * 0.3F,
                  false
               );
            this.growlTime = 200 + this.random.nextInt(200);
         }
      }

      if (this.dragonFight == null && this.level() instanceof ServerLevel serverLevel) {
         EnderDragonFight maybeOurFight = serverLevel.getDragonFight();
         if (maybeOurFight != null && this.getUUID().equals(maybeOurFight.dragonUUID())) {
            this.dragonFight = maybeOurFight;
         }
      }

      this.oFlapTime = this.flapTime;
      if (this.isDeadOrDying()) {
         float xo = (this.random.nextFloat() - 0.5F) * 8.0F;
         float yo = (this.random.nextFloat() - 0.5F) * 4.0F;
         float zo = (this.random.nextFloat() - 0.5F) * 8.0F;
         this.level().addParticle(ParticleTypes.EXPLOSION, this.getX() + xo, this.getY() + 2.0 + yo, this.getZ() + zo, 0.0, 0.0, 0.0);
      } else {
         this.checkCrystals();
         Vec3 movement = this.getDeltaMovement();
         float flapSpeed = 0.2F / ((float)movement.horizontalDistance() * 10.0F + 1.0F);
         flapSpeed *= (float)Math.pow(2.0, movement.y);
         if (this.phaseManager.getCurrentPhase().isSitting()) {
            this.flapTime += 0.1F;
         } else if (this.inWall) {
            this.flapTime += flapSpeed * 0.5F;
         } else {
            this.flapTime += flapSpeed;
         }

         this.setYRot(Mth.wrapDegrees(this.getYRot()));
         if (this.isNoAi()) {
            this.flapTime = 0.5F;
         } else {
            this.flightHistory.record(this.getY(), this.getYRot());
            if (this.level() instanceof ServerLevel level) {
               DragonPhaseInstance currentPhase = this.phaseManager.getCurrentPhase();
               currentPhase.doServerTick(level);
               if (this.phaseManager.getCurrentPhase() != currentPhase) {
                  currentPhase = this.phaseManager.getCurrentPhase();
                  currentPhase.doServerTick(level);
               }

               Vec3 targetLocation = currentPhase.getFlyTargetLocation();
               if (targetLocation != null) {
                  double xdd = targetLocation.x - this.getX();
                  double ydd = targetLocation.y - this.getY();
                  double zdd = targetLocation.z - this.getZ();
                  double distToTarget = xdd * xdd + ydd * ydd + zdd * zdd;
                  float max = currentPhase.getFlySpeed();
                  double horizontalDist = Math.sqrt(xdd * xdd + zdd * zdd);
                  if (horizontalDist > 0.0) {
                     ydd = Mth.clamp(ydd / horizontalDist, -max, max);
                  }

                  this.setDeltaMovement(this.getDeltaMovement().add(0.0, ydd * 0.01, 0.0));
                  this.setYRot(Mth.wrapDegrees(this.getYRot()));
                  Vec3 aim = targetLocation.subtract(this.getX(), this.getY(), this.getZ()).normalize();
                  Vec3 dir = new Vec3(
                        Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)), this.getDeltaMovement().y, -Mth.cos(this.getYRot() * (float) (Math.PI / 180.0))
                     )
                     .normalize();
                  float dot = Math.max(((float)dir.dot(aim) + 0.5F) / 1.5F, 0.0F);
                  if (Math.abs(xdd) > 1.0E-5F || Math.abs(zdd) > 1.0E-5F) {
                     float yRotD = Mth.clamp(Mth.wrapDegrees(180.0F - (float)Mth.atan2(xdd, zdd) * (180.0F / (float)Math.PI) - this.getYRot()), -50.0F, 50.0F);
                     this.yRotA *= 0.8F;
                     this.yRotA = this.yRotA + yRotD * currentPhase.getTurnSpeed();
                     this.setYRot(this.getYRot() + this.yRotA * 0.1F);
                  }

                  float span = (float)(2.0 / (distToTarget + 1.0));
                  float speed = 0.06F;
                  this.moveRelative(0.06F * (dot * span + (1.0F - span)), new Vec3(0.0, 0.0, -1.0));
                  if (this.inWall) {
                     this.move(MoverType.SELF, this.getDeltaMovement().scale(0.8F));
                  } else {
                     this.move(MoverType.SELF, this.getDeltaMovement());
                  }

                  Vec3 actual = this.getDeltaMovement().normalize();
                  double slide = 0.8 + 0.15 * (actual.dot(dir) + 1.0) / 2.0;
                  this.setDeltaMovement(this.getDeltaMovement().multiply(slide, 0.91F, slide));
               }
            } else {
               this.interpolation.interpolate();
               this.phaseManager.getCurrentPhase().doClientTick();
            }

            if (!this.level().isClientSide()) {
               this.applyEffectsFromBlocks();
            }

            this.yBodyRot = this.getYRot();
            Vec3[] oldPos = new Vec3[this.subEntities.length];

            for (int i = 0; i < this.subEntities.length; i++) {
               oldPos[i] = new Vec3(this.subEntities[i].getX(), this.subEntities[i].getY(), this.subEntities[i].getZ());
            }

            float tilt = (float)(this.flightHistory.get(5).y() - this.flightHistory.get(10).y()) * 10.0F * (float) (Math.PI / 180.0);
            float ccTilt = Mth.cos(tilt);
            float ssTilt = Mth.sin(tilt);
            float rot1 = this.getYRot() * (float) (Math.PI / 180.0);
            float ss1 = Mth.sin(rot1);
            float cc1 = Mth.cos(rot1);
            this.tickPart(this.body, ss1 * 0.5F, 0.0, -cc1 * 0.5F);
            this.tickPart(this.wing1, cc1 * 4.5F, 2.0, ss1 * 4.5F);
            this.tickPart(this.wing2, cc1 * -4.5F, 2.0, ss1 * -4.5F);
            if (this.level() instanceof ServerLevel serverLevel && !this.wasHurtRecently()) {
               this.knockBack(
                  serverLevel,
                  serverLevel.getEntities(
                     this, this.wing1.getBoundingBox().inflate(4.0, 2.0, 4.0).move(0.0, -2.0, 0.0), EntitySelector.NO_CREATIVE_OR_SPECTATOR
                  )
               );
               this.knockBack(
                  serverLevel,
                  serverLevel.getEntities(
                     this, this.wing2.getBoundingBox().inflate(4.0, 2.0, 4.0).move(0.0, -2.0, 0.0), EntitySelector.NO_CREATIVE_OR_SPECTATOR
                  )
               );
               this.hurt(serverLevel, serverLevel.getEntities(this, this.head.getBoundingBox().inflate(1.0), EntitySelector.NO_CREATIVE_OR_SPECTATOR));
               this.hurt(serverLevel, serverLevel.getEntities(this, this.neck.getBoundingBox().inflate(1.0), EntitySelector.NO_CREATIVE_OR_SPECTATOR));
            }

            float ss2 = Mth.sin(this.getYRot() * (float) (Math.PI / 180.0) - this.yRotA * 0.01F);
            float cc2 = Mth.cos(this.getYRot() * (float) (Math.PI / 180.0) - this.yRotA * 0.01F);
            float yOffset = this.getHeadYOffset();
            this.tickPart(this.head, ss2 * 6.5F * ccTilt, yOffset + ssTilt * 6.5F, -cc2 * 6.5F * ccTilt);
            this.tickPart(this.neck, ss2 * 5.5F * ccTilt, yOffset + ssTilt * 5.5F, -cc2 * 5.5F * ccTilt);
            DragonFlightHistory.Sample p1 = this.flightHistory.get(5);

            for (int i = 0; i < 3; i++) {
               EnderDragonPart part = null;
               if (i == 0) {
                  part = this.tail1;
               }

               if (i == 1) {
                  part = this.tail2;
               }

               if (i == 2) {
                  part = this.tail3;
               }

               DragonFlightHistory.Sample p0 = this.flightHistory.get(12 + i * 2);
               float rot = this.getYRot() * (float) (Math.PI / 180.0) + this.rotWrap(p0.yRot() - p1.yRot()) * (float) (Math.PI / 180.0);
               float ss = Mth.sin(rot);
               float cc = Mth.cos(rot);
               float dd1 = 1.5F;
               float dd = (i + 1) * 2.0F;
               this.tickPart(part, -(ss1 * 1.5F + ss * dd) * ccTilt, p0.y() - p1.y() - (dd + 1.5F) * ssTilt + 1.5, (cc1 * 1.5F + cc * dd) * ccTilt);
            }

            if (this.level() instanceof ServerLevel level) {
               this.inWall = this.checkWalls(level, this.head.getBoundingBox())
                  | this.checkWalls(level, this.neck.getBoundingBox())
                  | this.checkWalls(level, this.body.getBoundingBox());
               if (this.dragonFight != null) {
                  this.dragonFight.updateDragon(this);
               }
            }

            for (int i = 0; i < this.subEntities.length; i++) {
               this.subEntities[i].xo = oldPos[i].x;
               this.subEntities[i].yo = oldPos[i].y;
               this.subEntities[i].zo = oldPos[i].z;
               this.subEntities[i].xOld = oldPos[i].x;
               this.subEntities[i].yOld = oldPos[i].y;
               this.subEntities[i].zOld = oldPos[i].z;
            }
         }
      }
   }

   private void tickPart(final EnderDragonPart part, final double x, final double y, final double z) {
      part.setPos(this.getX() + x, this.getY() + y, this.getZ() + z);
   }

   private float getHeadYOffset() {
      if (this.phaseManager.getCurrentPhase().isSitting()) {
         return -1.0F;
      }

      DragonFlightHistory.Sample p0 = this.flightHistory.get(5);
      DragonFlightHistory.Sample p1 = this.flightHistory.get(0);
      return (float)(p0.y() - p1.y());
   }

   private void checkCrystals() {
      if (this.nearestCrystal != null) {
         if (this.nearestCrystal.isRemoved()) {
            this.nearestCrystal = null;
         } else if (this.tickCount % 10 == 0 && this.getHealth() < this.getMaxHealth()) {
            this.setHealth(this.getHealth() + 1.0F);
         }
      }

      if (this.random.nextInt(10) == 0) {
         List<EndCrystal> crystals = this.level().getEntitiesOfClass(EndCrystal.class, this.getBoundingBox().inflate(32.0));
         EndCrystal nearest = null;
         double distance = Double.MAX_VALUE;

         for (EndCrystal crystal : crystals) {
            double dist = crystal.distanceToSqr(this);
            if (dist < distance) {
               distance = dist;
               nearest = crystal;
            }
         }

         this.nearestCrystal = nearest;
      }
   }

   private void knockBack(final ServerLevel serverLevel, final List<Entity> entities) {
      double xm = (this.body.getBoundingBox().minX + this.body.getBoundingBox().maxX) / 2.0;
      double zm = (this.body.getBoundingBox().minZ + this.body.getBoundingBox().maxZ) / 2.0;

      for (Entity entity : entities) {
         if (entity instanceof LivingEntity livingTarget) {
            double xd = entity.getX() - xm;
            double zd = entity.getZ() - zm;
            double dd = Math.max(xd * xd + zd * zd, 0.1);
            entity.push(xd / dd * 4.0, 0.2F, zd / dd * 4.0);
            if (!this.phaseManager.getCurrentPhase().isSitting()
               && livingTarget.getLastHurtByMobTimestamp() < entity.tickCount - 2
               && this.doTeamsAllowDamage(livingTarget)) {
               DamageSource damageSource = this.damageSources().mobAttack(this);
               entity.hurtServer(serverLevel, damageSource, 5.0F);
               EnchantmentHelper.doPostAttackEffects(serverLevel, entity, damageSource);
            }
         }
      }
   }

   private void hurt(final ServerLevel level, final List<Entity> entities) {
      for (Entity target : entities) {
         if (target instanceof LivingEntity && this.doTeamsAllowDamage(target)) {
            DamageSource damageSource = this.damageSources().mobAttack(this);
            target.hurtServer(level, damageSource, 10.0F);
            EnchantmentHelper.doPostAttackEffects(level, target, damageSource);
         }
      }
   }

   private float rotWrap(final double d) {
      return (float)Mth.wrapDegrees(d);
   }

   private boolean checkWalls(final ServerLevel level, final AABB bb) {
      int x0 = Mth.floor(bb.minX);
      int y0 = Mth.floor(bb.minY);
      int z0 = Mth.floor(bb.minZ);
      int x1 = Mth.floor(bb.maxX);
      int y1 = Mth.floor(bb.maxY);
      int z1 = Mth.floor(bb.maxZ);
      boolean hitWall = false;
      boolean destroyedBlock = false;

      for (int x = x0; x <= x1; x++) {
         for (int y = y0; y <= y1; y++) {
            for (int z = z0; z <= z1; z++) {
               BlockPos blockPos = new BlockPos(x, y, z);
               BlockState state = level.getBlockState(blockPos);
               if (!state.isAir() && !state.is(BlockTags.DRAGON_TRANSPARENT)) {
                  if (level.getGameRules().get(GameRules.MOB_GRIEFING) && !state.is(BlockTags.DRAGON_IMMUNE)) {
                     destroyedBlock = level.removeBlock(blockPos, false) || destroyedBlock;
                  } else {
                     hitWall = true;
                  }
               }
            }
         }
      }

      if (destroyedBlock) {
         BlockPos randomPos = new BlockPos(x0 + this.random.nextInt(x1 - x0 + 1), y0 + this.random.nextInt(y1 - y0 + 1), z0 + this.random.nextInt(z1 - z0 + 1));
         level.levelEvent(2008, randomPos, 0);
      }

      return hitWall;
   }

   public boolean hurt(final ServerLevel level, final EnderDragonPart part, final DamageSource source, float damage) {
      if (this.phaseManager.getCurrentPhase().getPhase() == EnderDragonPhase.DYING) {
         return false;
      }

      damage = this.phaseManager.getCurrentPhase().onHurt(source, damage);
      if (part != this.head) {
         damage = damage / 4.0F + Math.min(damage, 1.0F);
      }

      if (damage < 0.01F) {
         return false;
      }

      if (source.getEntity() instanceof Player || source.is(DamageTypeTags.ALWAYS_HURTS_ENDER_DRAGONS)) {
         float healthBefore = this.getHealth();
         this.reallyHurt(level, source, damage);
         if (this.phaseManager.getCurrentPhase().isSitting()) {
            this.sittingDamageReceived = this.sittingDamageReceived + healthBefore - this.getHealth();
            if (this.sittingDamageReceived > 0.25F * this.getMaxHealth()) {
               this.sittingDamageReceived = 0.0F;
               this.phaseManager.setPhase(EnderDragonPhase.TAKEOFF);
            }
         }
      }

      return true;
   }

   @Override
   public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
      return this.hurt(level, this.body, source, damage);
   }

   protected void reallyHurt(final ServerLevel level, final DamageSource source, final float damage) {
      super.hurtServer(level, source, damage);
   }

   @Override
   protected void handleKillingBlow() {
      if (!this.phaseManager.getCurrentPhase().isSitting()) {
         this.setHealth(1.0F);
         this.phaseManager.setPhase(EnderDragonPhase.DYING);
      }
   }

   @Override
   public void knockback(final double power, final double xd, final double zd, final DamageSource source, final float damage, final boolean comesFromEffect) {
      if (!this.phaseManager.getCurrentPhase().isSitting()) {
         super.knockback(power, xd, zd, source, damage, comesFromEffect);
      }
   }

   @Override
   public void kill(final ServerLevel level) {
      this.remove(Entity.RemovalReason.KILLED);
      this.gameEvent(GameEvent.ENTITY_DIE);
      if (this.dragonFight != null) {
         this.dragonFight.updateDragon(this);
         this.dragonFight.setDragonKilled(this);
      }
   }

   @Override
   protected void tickDeath() {
      if (this.dragonFight != null) {
         this.dragonFight.updateDragon(this);
      }

      this.dragonDeathTime++;
      if (this.dragonDeathTime >= 180 && this.dragonDeathTime <= 200) {
         float xo = (this.random.nextFloat() - 0.5F) * 8.0F;
         float yo = (this.random.nextFloat() - 0.5F) * 4.0F;
         float zo = (this.random.nextFloat() - 0.5F) * 8.0F;
         this.level().addParticle(ParticleTypes.EXPLOSION_EMITTER, this.getX() + xo, this.getY() + 2.0 + yo, this.getZ() + zo, 0.0, 0.0, 0.0);
      }

      int xpCount = 500;
      if (this.dragonFight != null && !this.dragonFight.hasPreviouslyKilledDragon()) {
         xpCount = 12000;
      }

      if (this.level() instanceof ServerLevel level) {
         if (this.dragonDeathTime > 150 && this.dragonDeathTime % 5 == 0 && level.getGameRules().get(GameRules.MOB_DROPS)) {
            ExperienceOrb.award(level, this.position(), Mth.floor(xpCount * 0.08F));
         }

         if (this.dragonDeathTime == 1 && !this.isSilent()) {
            level.globalLevelEvent(1028, this.blockPosition(), 0);
         }
      }

      Vec3 deathMove = new Vec3(0.0, 0.1F, 0.0);
      this.move(MoverType.SELF, deathMove);

      for (EnderDragonPart dragonPart : this.subEntities) {
         dragonPart.setOldPosAndRot();
         dragonPart.setPos(dragonPart.position().add(deathMove));
      }

      if (this.dragonDeathTime >= 200 && this.level() instanceof ServerLevel level) {
         if (level.getGameRules().get(GameRules.MOB_DROPS)) {
            ExperienceOrb.award(level, this.position(), Mth.floor(xpCount * 0.2F));
         }

         if (this.dragonFight != null) {
            this.dragonFight.setDragonKilled(this);
         }

         this.remove(Entity.RemovalReason.KILLED);
         this.gameEvent(GameEvent.ENTITY_DIE);
      }
   }

   public int findClosestNode() {
      if (this.nodes[0] == null) {
         for (int i = 0; i < 24; i++) {
            int yAdjustment = 5;
            int multiplier = i;
            int nodeX;
            int nodeZ;
            if (i < 12) {
               nodeX = Mth.floor(60.0F * Mth.cos(2.0F * ((float) -Math.PI + (float) (Math.PI / 12) * multiplier)));
               nodeZ = Mth.floor(60.0F * Mth.sin(2.0F * ((float) -Math.PI + (float) (Math.PI / 12) * multiplier)));
            } else if (i < 20) {
               multiplier -= 12;
               nodeX = Mth.floor(40.0F * Mth.cos(2.0F * ((float) -Math.PI + (float) (Math.PI / 8) * multiplier)));
               nodeZ = Mth.floor(40.0F * Mth.sin(2.0F * ((float) -Math.PI + (float) (Math.PI / 8) * multiplier)));
               yAdjustment += 10;
            } else {
               multiplier -= 20;
               nodeX = Mth.floor(20.0F * Mth.cos(2.0F * ((float) -Math.PI + (float) (Math.PI / 4) * multiplier)));
               nodeZ = Mth.floor(20.0F * Mth.sin(2.0F * ((float) -Math.PI + (float) (Math.PI / 4) * multiplier)));
            }

            int nodeY = Math.max(
               73, this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(nodeX, 0, nodeZ)).getY() + yAdjustment
            );
            this.nodes[i] = new Node(nodeX, nodeY, nodeZ);
         }

         this.nodeAdjacency[0] = 6146;
         this.nodeAdjacency[1] = 8197;
         this.nodeAdjacency[2] = 8202;
         this.nodeAdjacency[3] = 16404;
         this.nodeAdjacency[4] = 32808;
         this.nodeAdjacency[5] = 32848;
         this.nodeAdjacency[6] = 65696;
         this.nodeAdjacency[7] = 131392;
         this.nodeAdjacency[8] = 131712;
         this.nodeAdjacency[9] = 263424;
         this.nodeAdjacency[10] = 526848;
         this.nodeAdjacency[11] = 525313;
         this.nodeAdjacency[12] = 1581057;
         this.nodeAdjacency[13] = 3166214;
         this.nodeAdjacency[14] = 2138120;
         this.nodeAdjacency[15] = 6373424;
         this.nodeAdjacency[16] = 4358208;
         this.nodeAdjacency[17] = 12910976;
         this.nodeAdjacency[18] = 9044480;
         this.nodeAdjacency[19] = 9706496;
         this.nodeAdjacency[20] = 15216640;
         this.nodeAdjacency[21] = 13688832;
         this.nodeAdjacency[22] = 11763712;
         this.nodeAdjacency[23] = 8257536;
      }

      return this.findClosestNode(this.getX(), this.getY(), this.getZ());
   }

   public int findClosestNode(final double tX, final double tY, final double tZ) {
      float closestDist = 10000.0F;
      int closestIndex = 0;
      Node currentPos = new Node(Mth.floor(tX), Mth.floor(tY), Mth.floor(tZ));
      int startIndex = 0;
      if (this.dragonFight == null || this.dragonFight.aliveCrystals() == 0) {
         startIndex = 12;
      }

      for (int i = startIndex; i < 24; i++) {
         if (this.nodes[i] != null) {
            float dist = this.nodes[i].distanceToSqr(currentPos);
            if (dist < closestDist) {
               closestDist = dist;
               closestIndex = i;
            }
         }
      }

      return closestIndex;
   }

   public @Nullable Path findPath(final int startIndex, final int endIndex, final @Nullable Node finalNode) {
      for (int i = 0; i < 24; i++) {
         Node node = this.nodes[i];
         node.closed = false;
         node.f = 0.0F;
         node.g = 0.0F;
         node.h = 0.0F;
         node.cameFrom = null;
         node.heapIdx = -1;
      }

      Node from = this.nodes[startIndex];
      Node to = this.nodes[endIndex];
      from.g = 0.0F;
      from.h = from.distanceTo(to);
      from.f = from.h;
      this.openSet.clear();
      this.openSet.insert(from);
      Node closest = from;
      int minimumNodeIndex = 0;
      if (this.dragonFight == null || this.dragonFight.aliveCrystals() == 0) {
         minimumNodeIndex = 12;
      }

      while (!this.openSet.isEmpty()) {
         Node openNode = this.openSet.pop();
         if (openNode.equals(to)) {
            if (finalNode != null) {
               finalNode.cameFrom = to;
               to = finalNode;
            }

            return this.reconstructPath(from, to);
         }

         if (openNode.distanceTo(to) < closest.distanceTo(to)) {
            closest = openNode;
         }

         openNode.closed = true;
         int xIndex = 0;

         for (int i = 0; i < 24; i++) {
            if (this.nodes[i] == openNode) {
               xIndex = i;
               break;
            }
         }

         for (int i = minimumNodeIndex; i < 24; i++) {
            if ((this.nodeAdjacency[xIndex] & 1 << i) > 0) {
               Node adjacentNode = this.nodes[i];
               if (!adjacentNode.closed) {
                  float tentativeGScore = openNode.g + openNode.distanceTo(adjacentNode);
                  if (!adjacentNode.inOpenSet() || tentativeGScore < adjacentNode.g) {
                     adjacentNode.cameFrom = openNode;
                     adjacentNode.g = tentativeGScore;
                     adjacentNode.h = adjacentNode.distanceTo(to);
                     if (adjacentNode.inOpenSet()) {
                        this.openSet.changeCost(adjacentNode, adjacentNode.g + adjacentNode.h);
                     } else {
                        adjacentNode.f = adjacentNode.g + adjacentNode.h;
                        this.openSet.insert(adjacentNode);
                     }
                  }
               }
            }
         }
      }

      if (closest == from) {
         return null;
      }

      LOGGER.debug("Failed to find path from {} to {}", startIndex, endIndex);
      if (finalNode != null) {
         finalNode.cameFrom = closest;
         closest = finalNode;
      }

      return this.reconstructPath(from, closest);
   }

   private Path reconstructPath(final Node from, final Node to) {
      List<Node> nodes = Lists.newArrayList();
      Node node = to;
      nodes.add(0, node);

      while (node.cameFrom != null) {
         node = node.cameFrom;
         nodes.add(0, node);
      }

      return new Path(nodes, new BlockPos(to.x, to.y, to.z), true);
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putInt("DragonPhase", this.phaseManager.getCurrentPhase().getPhase().getId());
      output.putInt("DragonDeathTime", this.dragonDeathTime);
      output.putFloat("sitting_damage_received", this.sittingDamageReceived);
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      input.getInt("DragonPhase").ifPresent(phaseId -> this.phaseManager.setPhase(EnderDragonPhase.getById(phaseId)));
      this.dragonDeathTime = input.getIntOr("DragonDeathTime", 0);
      this.sittingDamageReceived = input.getFloatOr("sitting_damage_received", 0.0F);
   }

   @Override
   public void checkDespawn() {
   }

   public EnderDragonPart[] getSubEntities() {
      return this.subEntities;
   }

   @Override
   public boolean isPickable() {
      return false;
   }

   @Override
   public SoundSource getSoundSource() {
      return SoundSource.HOSTILE;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.ENDER_DRAGON_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(final DamageSource source) {
      return SoundEvents.ENDER_DRAGON_HURT;
   }

   @Override
   protected float getSoundVolume() {
      return 5.0F;
   }

   public Vec3 getHeadLookVector(final float a) {
      DragonPhaseInstance phaseInstance = this.phaseManager.getCurrentPhase();
      EnderDragonPhase<? extends DragonPhaseInstance> phase = phaseInstance.getPhase();
      Vec3 result;
      if (phase == EnderDragonPhase.LANDING || phase == EnderDragonPhase.TAKEOFF) {
         BlockPos egg = this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EnderDragonFight.getPodiumLocation(this.fightOrigin));
         float dist = Math.max((float)Math.sqrt(egg.distToCenterSqr(this.position())) / 4.0F, 1.0F);
         float yOffset = 6.0F / dist;
         float xRotOld = this.getXRot();
         float rotScale = 1.5F;
         this.setXRot(-yOffset * 1.5F * 5.0F);
         result = this.getViewVector(a);
         this.setXRot(xRotOld);
      } else if (phaseInstance.isSitting()) {
         float xRotOld = this.getXRot();
         float rotScale = 1.5F;
         this.setXRot(-45.0F);
         result = this.getViewVector(a);
         this.setXRot(xRotOld);
      } else {
         result = this.getViewVector(a);
      }

      return result;
   }

   public void onCrystalDestroyed(final ServerLevel level, final EndCrystal crystal, final BlockPos pos, final DamageSource source) {
      Player player;
      if (source.getEntity() instanceof Player playerSource) {
         player = playerSource;
      } else {
         player = level.getNearestPlayer(CRYSTAL_DESTROY_TARGETING, pos.getX(), pos.getY(), pos.getZ());
      }

      if (crystal == this.nearestCrystal) {
         this.hurt(level, this.head, this.damageSources().explosion(crystal, player), 10.0F);
      }

      this.phaseManager.getCurrentPhase().onCrystalDestroyed(crystal, pos, source, player);
   }

   @Override
   public void onSyncedDataUpdated(final EntityDataAccessor<?> accessor) {
      if (DATA_PHASE.equals(accessor) && this.level().isClientSide()) {
         this.phaseManager.setPhase(EnderDragonPhase.getById(this.getEntityData().get(DATA_PHASE)));
      }

      super.onSyncedDataUpdated(accessor);
   }

   public EnderDragonPhaseManager getPhaseManager() {
      return this.phaseManager;
   }

   public @Nullable EnderDragonFight getDragonFight() {
      return this.dragonFight;
   }

   @Override
   public boolean addEffect(final MobEffectInstance newEffect, final @Nullable Entity source) {
      return false;
   }

   @Override
   protected boolean canRide(final Entity vehicle) {
      return false;
   }

   @Override
   public boolean canUsePortal(final boolean ignorePassenger) {
      return false;
   }

   @Override
   public void recreateFromPacket(final ClientboundAddEntityPacket packet) {
      super.recreateFromPacket(packet);
      EnderDragonPart[] subEntities = this.getSubEntities();

      for (int i = 0; i < subEntities.length; i++) {
         subEntities[i].setId(i + packet.getId() + 1);
      }
   }

   @Override
   public boolean canAttack(final LivingEntity target) {
      return target.canBeSeenAsEnemy();
   }

   @Override
   protected float sanitizeScale(final float scale) {
      return 1.0F;
   }

   @Override
   public MoveSimulationType getMoveSimulationType() {
      return this.isDeadOrDying() ? MoveSimulationType.SERVER_AND_CLIENT : super.getMoveSimulationType();
   }
}
