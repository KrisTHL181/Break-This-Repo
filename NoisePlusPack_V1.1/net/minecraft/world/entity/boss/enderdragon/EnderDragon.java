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
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
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
   private @Nullable EndDragonFight dragonFight;
   private BlockPos fightOrigin = BlockPos.ZERO;
   private final EnderDragonPhaseManager phaseManager;
   private int growlTime = 100;
   private float sittingDamageReceived;
   private final Node[] nodes = new Node[24];
   private final int[] nodeAdjacency = new int[24];
   private final BinaryHeap openSet = new BinaryHeap();

   public EnderDragon(EntityType<? extends EnderDragon> p_31096_, Level p_31097_) {
      super(EntityType.ENDER_DRAGON, p_31097_);
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

   public void setDragonFight(EndDragonFight p_287736_) {
      this.dragonFight = p_287736_;
   }

   public void setFightOrigin(BlockPos p_287665_) {
      this.fightOrigin = p_287665_;
   }

   public BlockPos getFightOrigin() {
      return this.fightOrigin;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 200.0).add(Attributes.CAMERA_DISTANCE, 16.0);
   }

   @Override
   public boolean isFlapping() {
      float f = Mth.cos(this.flapTime * (float) (Math.PI * 2));
      float f1 = Mth.cos(this.oFlapTime * (float) (Math.PI * 2));
      return f1 <= -0.3F && f >= -0.3F;
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
   protected void defineSynchedData(SynchedEntityData.Builder p_330342_) {
      super.defineSynchedData(p_330342_);
      p_330342_.define(DATA_PHASE, EnderDragonPhase.HOVERING.getId());
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

      if (this.dragonFight == null && this.level() instanceof ServerLevel serverlevel) {
         EndDragonFight enddragonfight = serverlevel.getDragonFight();
         if (enddragonfight != null && this.getUUID().equals(enddragonfight.getDragonUUID())) {
            this.dragonFight = enddragonfight;
         }
      }

      this.oFlapTime = this.flapTime;
      if (this.isDeadOrDying()) {
         float f6 = (this.random.nextFloat() - 0.5F) * 8.0F;
         float f8 = (this.random.nextFloat() - 0.5F) * 4.0F;
         float f9 = (this.random.nextFloat() - 0.5F) * 8.0F;
         this.level().addParticle(ParticleTypes.EXPLOSION, this.getX() + f6, this.getY() + 2.0 + f8, this.getZ() + f9, 0.0, 0.0, 0.0);
      } else {
         this.checkCrystals();
         Vec3 vec34 = this.getDeltaMovement();
         float f7 = 0.2F / ((float)vec34.horizontalDistance() * 10.0F + 1.0F);
         f7 *= (float)Math.pow(2.0, vec34.y);
         if (this.phaseManager.getCurrentPhase().isSitting()) {
            this.flapTime += 0.1F;
         } else if (this.inWall) {
            this.flapTime += f7 * 0.5F;
         } else {
            this.flapTime += f7;
         }

         this.setYRot(Mth.wrapDegrees(this.getYRot()));
         if (this.isNoAi()) {
            this.flapTime = 0.5F;
         } else {
            this.flightHistory.record(this.getY(), this.getYRot());
            if (this.level() instanceof ServerLevel serverlevel1) {
               DragonPhaseInstance dragonphaseinstance = this.phaseManager.getCurrentPhase();
               dragonphaseinstance.doServerTick(serverlevel1);
               if (this.phaseManager.getCurrentPhase() != dragonphaseinstance) {
                  dragonphaseinstance = this.phaseManager.getCurrentPhase();
                  dragonphaseinstance.doServerTick(serverlevel1);
               }

               Vec3 vec3 = dragonphaseinstance.getFlyTargetLocation();
               if (vec3 != null) {
                  double d0 = vec3.x - this.getX();
                  double d1 = vec3.y - this.getY();
                  double d2 = vec3.z - this.getZ();
                  double d3 = d0 * d0 + d1 * d1 + d2 * d2;
                  float f2 = dragonphaseinstance.getFlySpeed();
                  double d4 = Math.sqrt(d0 * d0 + d2 * d2);
                  if (d4 > 0.0) {
                     d1 = Mth.clamp(d1 / d4, -f2, f2);
                  }

                  this.setDeltaMovement(this.getDeltaMovement().add(0.0, d1 * 0.01, 0.0));
                  this.setYRot(Mth.wrapDegrees(this.getYRot()));
                  Vec3 vec31 = vec3.subtract(this.getX(), this.getY(), this.getZ()).normalize();
                  Vec3 vec32 = new Vec3(
                        Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)), this.getDeltaMovement().y, -Mth.cos(this.getYRot() * (float) (Math.PI / 180.0))
                     )
                     .normalize();
                  float f3 = Math.max(((float)vec32.dot(vec31) + 0.5F) / 1.5F, 0.0F);
                  if (Math.abs(d0) > 1.0E-5F || Math.abs(d2) > 1.0E-5F) {
                     float f4 = Mth.clamp(Mth.wrapDegrees(180.0F - (float)Mth.atan2(d0, d2) * (180.0F / (float)Math.PI) - this.getYRot()), -50.0F, 50.0F);
                     this.yRotA *= 0.8F;
                     this.yRotA = this.yRotA + f4 * dragonphaseinstance.getTurnSpeed();
                     this.setYRot(this.getYRot() + this.yRotA * 0.1F);
                  }

                  float f20 = (float)(2.0 / (d3 + 1.0));
                  float f5 = 0.06F;
                  this.moveRelative(0.06F * (f3 * f20 + (1.0F - f20)), new Vec3(0.0, 0.0, -1.0));
                  if (this.inWall) {
                     this.move(MoverType.SELF, this.getDeltaMovement().scale(0.8F));
                  } else {
                     this.move(MoverType.SELF, this.getDeltaMovement());
                  }

                  Vec3 vec33 = this.getDeltaMovement().normalize();
                  double d5 = 0.8 + 0.15 * (vec33.dot(vec32) + 1.0) / 2.0;
                  this.setDeltaMovement(this.getDeltaMovement().multiply(d5, 0.91F, d5));
               }
            } else {
               this.interpolation.interpolate();
               this.phaseManager.getCurrentPhase().doClientTick();
            }

            if (!this.level().isClientSide()) {
               this.applyEffectsFromBlocks();
            }

            this.yBodyRot = this.getYRot();
            Vec3[] avec3 = new Vec3[this.subEntities.length];

            for (int i = 0; i < this.subEntities.length; i++) {
               avec3[i] = new Vec3(this.subEntities[i].getX(), this.subEntities[i].getY(), this.subEntities[i].getZ());
            }

            float f10 = (float)(this.flightHistory.get(5).y() - this.flightHistory.get(10).y()) * 10.0F * (float) (Math.PI / 180.0);
            float f11 = Mth.cos(f10);
            float f12 = Mth.sin(f10);
            float f = this.getYRot() * (float) (Math.PI / 180.0);
            float f13 = Mth.sin(f);
            float f1 = Mth.cos(f);
            this.tickPart(this.body, f13 * 0.5F, 0.0, -f1 * 0.5F);
            this.tickPart(this.wing1, f1 * 4.5F, 2.0, f13 * 4.5F);
            this.tickPart(this.wing2, f1 * -4.5F, 2.0, f13 * -4.5F);
            if (this.level() instanceof ServerLevel serverlevel2 && this.hurtTime == 0) {
               this.knockBack(
                  serverlevel2,
                  serverlevel2.getEntities(
                     this, this.wing1.getBoundingBox().inflate(4.0, 2.0, 4.0).move(0.0, -2.0, 0.0), EntitySelector.NO_CREATIVE_OR_SPECTATOR
                  )
               );
               this.knockBack(
                  serverlevel2,
                  serverlevel2.getEntities(
                     this, this.wing2.getBoundingBox().inflate(4.0, 2.0, 4.0).move(0.0, -2.0, 0.0), EntitySelector.NO_CREATIVE_OR_SPECTATOR
                  )
               );
               this.hurt(serverlevel2, serverlevel2.getEntities(this, this.head.getBoundingBox().inflate(1.0), EntitySelector.NO_CREATIVE_OR_SPECTATOR));
               this.hurt(serverlevel2, serverlevel2.getEntities(this, this.neck.getBoundingBox().inflate(1.0), EntitySelector.NO_CREATIVE_OR_SPECTATOR));
            }

            float f14 = Mth.sin(this.getYRot() * (float) (Math.PI / 180.0) - this.yRotA * 0.01F);
            float f15 = Mth.cos(this.getYRot() * (float) (Math.PI / 180.0) - this.yRotA * 0.01F);
            float f16 = this.getHeadYOffset();
            this.tickPart(this.head, f14 * 6.5F * f11, f16 + f12 * 6.5F, -f15 * 6.5F * f11);
            this.tickPart(this.neck, f14 * 5.5F * f11, f16 + f12 * 5.5F, -f15 * 5.5F * f11);
            DragonFlightHistory.Sample dragonflighthistory$sample = this.flightHistory.get(5);

            for (int j = 0; j < 3; j++) {
               EnderDragonPart enderdragonpart = null;
               if (j == 0) {
                  enderdragonpart = this.tail1;
               }

               if (j == 1) {
                  enderdragonpart = this.tail2;
               }

               if (j == 2) {
                  enderdragonpart = this.tail3;
               }

               DragonFlightHistory.Sample dragonflighthistory$sample1 = this.flightHistory.get(12 + j * 2);
               float f17 = this.getYRot() * (float) (Math.PI / 180.0)
                  + this.rotWrap(dragonflighthistory$sample1.yRot() - dragonflighthistory$sample.yRot()) * (float) (Math.PI / 180.0);
               float f18 = Mth.sin(f17);
               float f19 = Mth.cos(f17);
               float f21 = 1.5F;
               float f22 = (j + 1) * 2.0F;
               this.tickPart(
                  enderdragonpart,
                  -(f13 * 1.5F + f18 * f22) * f11,
                  dragonflighthistory$sample1.y() - dragonflighthistory$sample.y() - (f22 + 1.5F) * f12 + 1.5,
                  (f1 * 1.5F + f19 * f22) * f11
               );
            }

            if (this.level() instanceof ServerLevel serverlevel3) {
               this.inWall = this.checkWalls(serverlevel3, this.head.getBoundingBox())
                  | this.checkWalls(serverlevel3, this.neck.getBoundingBox())
                  | this.checkWalls(serverlevel3, this.body.getBoundingBox());
               if (this.dragonFight != null) {
                  this.dragonFight.updateDragon(this);
               }
            }

            for (int k = 0; k < this.subEntities.length; k++) {
               this.subEntities[k].xo = avec3[k].x;
               this.subEntities[k].yo = avec3[k].y;
               this.subEntities[k].zo = avec3[k].z;
               this.subEntities[k].xOld = avec3[k].x;
               this.subEntities[k].yOld = avec3[k].y;
               this.subEntities[k].zOld = avec3[k].z;
            }
         }
      }
   }

   private void tickPart(EnderDragonPart p_453034_, double p_31117_, double p_31118_, double p_31119_) {
      p_453034_.setPos(this.getX() + p_31117_, this.getY() + p_31118_, this.getZ() + p_31119_);
   }

   private float getHeadYOffset() {
      if (this.phaseManager.getCurrentPhase().isSitting()) {
         return -1.0F;
      }

      DragonFlightHistory.Sample dragonflighthistory$sample = this.flightHistory.get(5);
      DragonFlightHistory.Sample dragonflighthistory$sample1 = this.flightHistory.get(0);
      return (float)(dragonflighthistory$sample.y() - dragonflighthistory$sample1.y());
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
         List<EndCrystal> list = this.level().getEntitiesOfClass(EndCrystal.class, this.getBoundingBox().inflate(32.0));
         EndCrystal endcrystal = null;
         double d0 = Double.MAX_VALUE;

         for (EndCrystal endcrystal1 : list) {
            double d1 = endcrystal1.distanceToSqr(this);
            if (d1 < d0) {
               d0 = d1;
               endcrystal = endcrystal1;
            }
         }

         this.nearestCrystal = endcrystal;
      }
   }

   private void knockBack(ServerLevel p_343522_, List<Entity> p_31132_) {
      double d0 = (this.body.getBoundingBox().minX + this.body.getBoundingBox().maxX) / 2.0;
      double d1 = (this.body.getBoundingBox().minZ + this.body.getBoundingBox().maxZ) / 2.0;

      for (Entity entity : p_31132_) {
         if (entity instanceof LivingEntity livingentity) {
            double d2 = entity.getX() - d0;
            double d3 = entity.getZ() - d1;
            double d4 = Math.max(d2 * d2 + d3 * d3, 0.1);
            entity.push(d2 / d4 * 4.0, 0.2F, d3 / d4 * 4.0);
            if (!this.phaseManager.getCurrentPhase().isSitting() && livingentity.getLastHurtByMobTimestamp() < entity.tickCount - 2) {
               DamageSource damagesource = this.damageSources().mobAttack(this);
               entity.hurtServer(p_343522_, damagesource, 5.0F);
               EnchantmentHelper.doPostAttackEffects(p_343522_, entity, damagesource);
            }
         }
      }
   }

   private void hurt(ServerLevel p_366619_, List<Entity> p_361288_) {
      for (Entity entity : p_361288_) {
         if (entity instanceof LivingEntity) {
            DamageSource damagesource = this.damageSources().mobAttack(this);
            entity.hurtServer(p_366619_, damagesource, 10.0F);
            EnchantmentHelper.doPostAttackEffects(p_366619_, entity, damagesource);
         }
      }
   }

   private float rotWrap(double p_31165_) {
      return (float)Mth.wrapDegrees(p_31165_);
   }

   private boolean checkWalls(ServerLevel p_363273_, AABB p_31140_) {
      int i = Mth.floor(p_31140_.minX);
      int j = Mth.floor(p_31140_.minY);
      int k = Mth.floor(p_31140_.minZ);
      int l = Mth.floor(p_31140_.maxX);
      int i1 = Mth.floor(p_31140_.maxY);
      int j1 = Mth.floor(p_31140_.maxZ);
      boolean flag = false;
      boolean flag1 = false;

      for (int k1 = i; k1 <= l; k1++) {
         for (int l1 = j; l1 <= i1; l1++) {
            for (int i2 = k; i2 <= j1; i2++) {
               BlockPos blockpos = new BlockPos(k1, l1, i2);
               BlockState blockstate = p_363273_.getBlockState(blockpos);
               if (!blockstate.isAir() && !blockstate.is(BlockTags.DRAGON_TRANSPARENT)) {
                  if (p_363273_.getGameRules().get(GameRules.MOB_GRIEFING) && !blockstate.is(BlockTags.DRAGON_IMMUNE)) {
                     flag1 = p_363273_.removeBlock(blockpos, false) || flag1;
                  } else {
                     flag = true;
                  }
               }
            }
         }
      }

      if (flag1) {
         BlockPos blockpos1 = new BlockPos(i + this.random.nextInt(l - i + 1), j + this.random.nextInt(i1 - j + 1), k + this.random.nextInt(j1 - k + 1));
         p_363273_.levelEvent(2008, blockpos1, 0);
      }

      return flag;
   }

   public boolean hurt(ServerLevel p_363622_, EnderDragonPart p_454723_, DamageSource p_369686_, float p_362736_) {
      if (this.phaseManager.getCurrentPhase().getPhase() == EnderDragonPhase.DYING) {
         return false;
      }

      p_362736_ = this.phaseManager.getCurrentPhase().onHurt(p_369686_, p_362736_);
      if (p_454723_ != this.head) {
         p_362736_ = p_362736_ / 4.0F + Math.min(p_362736_, 1.0F);
      }

      if (p_362736_ < 0.01F) {
         return false;
      }

      if (p_369686_.getEntity() instanceof Player || p_369686_.is(DamageTypeTags.ALWAYS_HURTS_ENDER_DRAGONS)) {
         float f = this.getHealth();
         this.reallyHurt(p_363622_, p_369686_, p_362736_);
         if (this.isDeadOrDying() && !this.phaseManager.getCurrentPhase().isSitting()) {
            this.setHealth(1.0F);
            this.phaseManager.setPhase(EnderDragonPhase.DYING);
         }

         if (this.phaseManager.getCurrentPhase().isSitting()) {
            this.sittingDamageReceived = this.sittingDamageReceived + f - this.getHealth();
            if (this.sittingDamageReceived > 0.25F * this.getMaxHealth()) {
               this.sittingDamageReceived = 0.0F;
               this.phaseManager.setPhase(EnderDragonPhase.TAKEOFF);
            }
         }
      }

      return true;
   }

   @Override
   public boolean hurtServer(ServerLevel p_364327_, DamageSource p_363284_, float p_360908_) {
      return this.hurt(p_364327_, this.body, p_363284_, p_360908_);
   }

   protected void reallyHurt(ServerLevel p_360975_, DamageSource p_31162_, float p_31163_) {
      super.hurtServer(p_360975_, p_31162_, p_31163_);
   }

   @Override
   public void kill(ServerLevel p_364941_) {
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
         float f = (this.random.nextFloat() - 0.5F) * 8.0F;
         float f1 = (this.random.nextFloat() - 0.5F) * 4.0F;
         float f2 = (this.random.nextFloat() - 0.5F) * 8.0F;
         this.level().addParticle(ParticleTypes.EXPLOSION_EMITTER, this.getX() + f, this.getY() + 2.0 + f1, this.getZ() + f2, 0.0, 0.0, 0.0);
      }

      int i = 500;
      if (this.dragonFight != null && !this.dragonFight.hasPreviouslyKilledDragon()) {
         i = 12000;
      }

      if (this.level() instanceof ServerLevel serverlevel) {
         if (this.dragonDeathTime > 150 && this.dragonDeathTime % 5 == 0 && serverlevel.getGameRules().get(GameRules.MOB_DROPS)) {
            ExperienceOrb.award(serverlevel, this.position(), Mth.floor(i * 0.08F));
         }

         if (this.dragonDeathTime == 1 && !this.isSilent()) {
            serverlevel.globalLevelEvent(1028, this.blockPosition(), 0);
         }
      }

      Vec3 vec3 = new Vec3(0.0, 0.1F, 0.0);
      this.move(MoverType.SELF, vec3);

      for (EnderDragonPart enderdragonpart : this.subEntities) {
         enderdragonpart.setOldPosAndRot();
         enderdragonpart.setPos(enderdragonpart.position().add(vec3));
      }

      if (this.dragonDeathTime == 200 && this.level() instanceof ServerLevel serverlevel1) {
         if (serverlevel1.getGameRules().get(GameRules.MOB_DROPS)) {
            ExperienceOrb.award(serverlevel1, this.position(), Mth.floor(i * 0.2F));
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
            int j = 5;
            int l;
            int i1;
            if (i < 12) {
               l = Mth.floor(60.0F * Mth.cos(2.0F * ((float) -Math.PI + (float) (Math.PI / 12) * i)));
               i1 = Mth.floor(60.0F * Mth.sin(2.0F * ((float) -Math.PI + (float) (Math.PI / 12) * i)));
            } else if (i < 20) {
               int $$2 = i - 12;
               l = Mth.floor(40.0F * Mth.cos(2.0F * ((float) -Math.PI + (float) (Math.PI / 8) * $$2)));
               i1 = Mth.floor(40.0F * Mth.sin(2.0F * ((float) -Math.PI + (float) (Math.PI / 8) * $$2)));
               j += 10;
            } else {
               int k1 = i - 20;
               l = Mth.floor(20.0F * Mth.cos(2.0F * ((float) -Math.PI + (float) (Math.PI / 4) * k1)));
               i1 = Mth.floor(20.0F * Mth.sin(2.0F * ((float) -Math.PI + (float) (Math.PI / 4) * k1)));
            }

            int j1 = Math.max(73, this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(l, 0, i1)).getY() + j);
            this.nodes[i] = new Node(l, j1, i1);
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

   public int findClosestNode(double p_31171_, double p_31172_, double p_31173_) {
      float f = 10000.0F;
      int i = 0;
      Node node = new Node(Mth.floor(p_31171_), Mth.floor(p_31172_), Mth.floor(p_31173_));
      int j = 0;
      if (this.dragonFight == null || this.dragonFight.getCrystalsAlive() == 0) {
         j = 12;
      }

      for (int k = j; k < 24; k++) {
         if (this.nodes[k] != null) {
            float f1 = this.nodes[k].distanceToSqr(node);
            if (f1 < f) {
               f = f1;
               i = k;
            }
         }
      }

      return i;
   }

   public @Nullable Path findPath(int p_31105_, int p_31106_, @Nullable Node p_31107_) {
      for (int i = 0; i < 24; i++) {
         Node node = this.nodes[i];
         node.closed = false;
         node.f = 0.0F;
         node.g = 0.0F;
         node.h = 0.0F;
         node.cameFrom = null;
         node.heapIdx = -1;
      }

      Node node4 = this.nodes[p_31105_];
      Node node5 = this.nodes[p_31106_];
      node4.g = 0.0F;
      node4.h = node4.distanceTo(node5);
      node4.f = node4.h;
      this.openSet.clear();
      this.openSet.insert(node4);
      Node node1 = node4;
      int j = 0;
      if (this.dragonFight == null || this.dragonFight.getCrystalsAlive() == 0) {
         j = 12;
      }

      while (!this.openSet.isEmpty()) {
         Node node2 = this.openSet.pop();
         if (node2.equals(node5)) {
            if (p_31107_ != null) {
               p_31107_.cameFrom = node5;
               node5 = p_31107_;
            }

            return this.reconstructPath(node4, node5);
         }

         if (node2.distanceTo(node5) < node1.distanceTo(node5)) {
            node1 = node2;
         }

         node2.closed = true;
         int k = 0;

         for (int l = 0; l < 24; l++) {
            if (this.nodes[l] == node2) {
               k = l;
               break;
            }
         }

         for (int i1 = j; i1 < 24; i1++) {
            if ((this.nodeAdjacency[k] & 1 << i1) > 0) {
               Node node3 = this.nodes[i1];
               if (!node3.closed) {
                  float f = node2.g + node2.distanceTo(node3);
                  if (!node3.inOpenSet() || f < node3.g) {
                     node3.cameFrom = node2;
                     node3.g = f;
                     node3.h = node3.distanceTo(node5);
                     if (node3.inOpenSet()) {
                        this.openSet.changeCost(node3, node3.g + node3.h);
                     } else {
                        node3.f = node3.g + node3.h;
                        this.openSet.insert(node3);
                     }
                  }
               }
            }
         }
      }

      if (node1 == node4) {
         return null;
      }

      LOGGER.debug("Failed to find path from {} to {}", p_31105_, p_31106_);
      if (p_31107_ != null) {
         p_31107_.cameFrom = node1;
         node1 = p_31107_;
      }

      return this.reconstructPath(node4, node1);
   }

   private Path reconstructPath(Node p_31129_, Node p_31130_) {
      List<Node> list = Lists.newArrayList();
      Node node = p_31130_;
      list.add(0, p_31130_);

      while (node.cameFrom != null) {
         node = node.cameFrom;
         list.add(0, node);
      }

      return new Path(list, new BlockPos(p_31130_.x, p_31130_.y, p_31130_.z), true);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_406735_) {
      super.addAdditionalSaveData(p_406735_);
      p_406735_.putInt("DragonPhase", this.phaseManager.getCurrentPhase().getPhase().getId());
      p_406735_.putInt("DragonDeathTime", this.dragonDeathTime);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_406699_) {
      super.readAdditionalSaveData(p_406699_);
      p_406699_.getInt("DragonPhase").ifPresent(p_395552_ -> this.phaseManager.setPhase(EnderDragonPhase.getById(p_395552_)));
      this.dragonDeathTime = p_406699_.getIntOr("DragonDeathTime", 0);
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
   protected SoundEvent getHurtSound(DamageSource p_31154_) {
      return SoundEvents.ENDER_DRAGON_HURT;
   }

   @Override
   protected float getSoundVolume() {
      return 5.0F;
   }

   public Vec3 getHeadLookVector(float p_31175_) {
      DragonPhaseInstance dragonphaseinstance = this.phaseManager.getCurrentPhase();
      EnderDragonPhase<? extends DragonPhaseInstance> enderdragonphase = dragonphaseinstance.getPhase();
      Vec3 vec3;
      if (enderdragonphase == EnderDragonPhase.LANDING || enderdragonphase == EnderDragonPhase.TAKEOFF) {
         BlockPos blockpos = this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EndPodiumFeature.getLocation(this.fightOrigin));
         float f5 = Math.max((float)Math.sqrt(blockpos.distToCenterSqr(this.position())) / 4.0F, 1.0F);
         float f2 = 6.0F / f5;
         float f3 = this.getXRot();
         float f4 = 1.5F;
         this.setXRot(-f2 * 1.5F * 5.0F);
         vec3 = this.getViewVector(p_31175_);
         this.setXRot(f3);
      } else if (dragonphaseinstance.isSitting()) {
         float f = this.getXRot();
         float f1 = 1.5F;
         this.setXRot(-45.0F);
         vec3 = this.getViewVector(p_31175_);
         this.setXRot(f);
      } else {
         vec3 = this.getViewVector(p_31175_);
      }

      return vec3;
   }

   public void onCrystalDestroyed(ServerLevel p_365946_, EndCrystal p_31125_, BlockPos p_31126_, DamageSource p_31127_) {
      Player player;
      if (p_31127_.getEntity() instanceof Player player1) {
         player = player1;
      } else {
         player = p_365946_.getNearestPlayer(CRYSTAL_DESTROY_TARGETING, p_31126_.getX(), p_31126_.getY(), p_31126_.getZ());
      }

      if (p_31125_ == this.nearestCrystal) {
         this.hurt(p_365946_, this.head, this.damageSources().explosion(p_31125_, player), 10.0F);
      }

      this.phaseManager.getCurrentPhase().onCrystalDestroyed(p_31125_, p_31126_, p_31127_, player);
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> p_31136_) {
      if (DATA_PHASE.equals(p_31136_) && this.level().isClientSide()) {
         this.phaseManager.setPhase(EnderDragonPhase.getById(this.getEntityData().get(DATA_PHASE)));
      }

      super.onSyncedDataUpdated(p_31136_);
   }

   public EnderDragonPhaseManager getPhaseManager() {
      return this.phaseManager;
   }

   public @Nullable EndDragonFight getDragonFight() {
      return this.dragonFight;
   }

   @Override
   public boolean addEffect(MobEffectInstance p_182394_, @Nullable Entity p_182395_) {
      return false;
   }

   @Override
   protected boolean canRide(Entity p_31169_) {
      return false;
   }

   @Override
   public boolean canUsePortal(boolean p_342758_) {
      return false;
   }

   @Override
   public void recreateFromPacket(ClientboundAddEntityPacket p_218825_) {
      super.recreateFromPacket(p_218825_);
      EnderDragonPart[] aenderdragonpart = this.getSubEntities();

      for (int i = 0; i < aenderdragonpart.length; i++) {
         aenderdragonpart[i].setId(i + p_218825_.getId() + 1);
      }
   }

   @Override
   public boolean canAttack(LivingEntity p_149576_) {
      return p_149576_.canBeSeenAsEnemy();
   }

   @Override
   protected float sanitizeScale(float p_333905_) {
      return 1.0F;
   }
}
