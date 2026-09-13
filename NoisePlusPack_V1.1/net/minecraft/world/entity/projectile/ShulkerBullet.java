package net.minecraft.world.entity.projectile;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ShulkerBullet extends Projectile {
   private static final double SPEED = 0.15;
   private @Nullable EntityReference<Entity> finalTarget;
   private @Nullable Direction currentMoveDirection;
   private int flightSteps;
   private double targetDeltaX;
   private double targetDeltaY;
   private double targetDeltaZ;

   public ShulkerBullet(EntityType<? extends ShulkerBullet> p_37319_, Level p_37320_) {
      super(p_37319_, p_37320_);
      this.noPhysics = true;
   }

   public ShulkerBullet(Level p_37330_, LivingEntity p_37331_, Entity p_37332_, Direction.Axis p_37333_) {
      this(EntityType.SHULKER_BULLET, p_37330_);
      this.setOwner(p_37331_);
      Vec3 vec3 = p_37331_.getBoundingBox().getCenter();
      this.snapTo(vec3.x, vec3.y, vec3.z, this.getYRot(), this.getXRot());
      this.finalTarget = EntityReference.of(p_37332_);
      this.currentMoveDirection = Direction.UP;
      this.selectNextMoveDirection(p_37333_, p_37332_);
   }

   @Override
   public SoundSource getSoundSource() {
      return SoundSource.HOSTILE;
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_408802_) {
      super.addAdditionalSaveData(p_408802_);
      if (this.finalTarget != null) {
         p_408802_.store("Target", UUIDUtil.CODEC, this.finalTarget.getUUID());
      }

      p_408802_.storeNullable("Dir", Direction.LEGACY_ID_CODEC, this.currentMoveDirection);
      p_408802_.putInt("Steps", this.flightSteps);
      p_408802_.putDouble("TXD", this.targetDeltaX);
      p_408802_.putDouble("TYD", this.targetDeltaY);
      p_408802_.putDouble("TZD", this.targetDeltaZ);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_410107_) {
      super.readAdditionalSaveData(p_410107_);
      this.flightSteps = p_410107_.getIntOr("Steps", 0);
      this.targetDeltaX = p_410107_.getDoubleOr("TXD", 0.0);
      this.targetDeltaY = p_410107_.getDoubleOr("TYD", 0.0);
      this.targetDeltaZ = p_410107_.getDoubleOr("TZD", 0.0);
      this.currentMoveDirection = p_410107_.<Direction>read("Dir", Direction.LEGACY_ID_CODEC).orElse(null);
      this.finalTarget = EntityReference.read(p_410107_, "Target");
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_328285_) {
   }

   private @Nullable Direction getMoveDirection() {
      return this.currentMoveDirection;
   }

   private void setMoveDirection(@Nullable Direction p_37351_) {
      this.currentMoveDirection = p_37351_;
   }

   private void selectNextMoveDirection(Direction.@Nullable Axis p_37349_, @Nullable Entity p_407453_) {
      double d0 = 0.5;
      BlockPos blockpos;
      if (p_407453_ == null) {
         blockpos = this.blockPosition().below();
      } else {
         d0 = p_407453_.getBbHeight() * 0.5;
         blockpos = BlockPos.containing(p_407453_.getX(), p_407453_.getY() + d0, p_407453_.getZ());
      }

      double d1 = blockpos.getX() + 0.5;
      double d2 = blockpos.getY() + d0;
      double d3 = blockpos.getZ() + 0.5;
      Direction direction = null;
      if (!blockpos.closerToCenterThan(this.position(), 2.0)) {
         BlockPos blockpos1 = this.blockPosition();
         List<Direction> list = Lists.newArrayList();
         if (p_37349_ != Direction.Axis.X) {
            if (blockpos1.getX() < blockpos.getX() && this.level().isEmptyBlock(blockpos1.east())) {
               list.add(Direction.EAST);
            } else if (blockpos1.getX() > blockpos.getX() && this.level().isEmptyBlock(blockpos1.west())) {
               list.add(Direction.WEST);
            }
         }

         if (p_37349_ != Direction.Axis.Y) {
            if (blockpos1.getY() < blockpos.getY() && this.level().isEmptyBlock(blockpos1.above())) {
               list.add(Direction.UP);
            } else if (blockpos1.getY() > blockpos.getY() && this.level().isEmptyBlock(blockpos1.below())) {
               list.add(Direction.DOWN);
            }
         }

         if (p_37349_ != Direction.Axis.Z) {
            if (blockpos1.getZ() < blockpos.getZ() && this.level().isEmptyBlock(blockpos1.south())) {
               list.add(Direction.SOUTH);
            } else if (blockpos1.getZ() > blockpos.getZ() && this.level().isEmptyBlock(blockpos1.north())) {
               list.add(Direction.NORTH);
            }
         }

         direction = Direction.getRandom(this.random);
         if (list.isEmpty()) {
            for (int i = 5; !this.level().isEmptyBlock(blockpos1.relative(direction)) && i > 0; i--) {
               direction = Direction.getRandom(this.random);
            }
         } else {
            direction = list.get(this.random.nextInt(list.size()));
         }

         d1 = this.getX() + direction.getStepX();
         d2 = this.getY() + direction.getStepY();
         d3 = this.getZ() + direction.getStepZ();
      }

      this.setMoveDirection(direction);
      double d6 = d1 - this.getX();
      double d7 = d2 - this.getY();
      double d4 = d3 - this.getZ();
      double d5 = Math.sqrt(d6 * d6 + d7 * d7 + d4 * d4);
      if (d5 == 0.0) {
         this.targetDeltaX = 0.0;
         this.targetDeltaY = 0.0;
         this.targetDeltaZ = 0.0;
      } else {
         this.targetDeltaX = d6 / d5 * 0.15;
         this.targetDeltaY = d7 / d5 * 0.15;
         this.targetDeltaZ = d4 / d5 * 0.15;
      }

      this.needsSync = true;
      this.flightSteps = 10 + this.random.nextInt(5) * 10;
   }

   @Override
   public void checkDespawn() {
      if (this.level().getDifficulty() == Difficulty.PEACEFUL) {
         this.discard();
      }
   }

   @Override
   protected double getDefaultGravity() {
      return 0.04;
   }

   @Override
   public void tick() {
      super.tick();
      Entity entity = !this.level().isClientSide() ? EntityReference.getEntity(this.finalTarget, this.level()) : null;
      HitResult hitresult = null;
      if (!this.level().isClientSide()) {
         if (entity == null) {
            this.finalTarget = null;
         }

         if (entity != null && entity.isAlive() && (!(entity instanceof Player) || !entity.isSpectator())) {
            this.targetDeltaX = Mth.clamp(this.targetDeltaX * 1.025, -1.0, 1.0);
            this.targetDeltaY = Mth.clamp(this.targetDeltaY * 1.025, -1.0, 1.0);
            this.targetDeltaZ = Mth.clamp(this.targetDeltaZ * 1.025, -1.0, 1.0);
            Vec3 vec3 = this.getDeltaMovement();
            this.setDeltaMovement(vec3.add((this.targetDeltaX - vec3.x) * 0.2, (this.targetDeltaY - vec3.y) * 0.2, (this.targetDeltaZ - vec3.z) * 0.2));
         } else {
            this.applyGravity();
         }

         hitresult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
      }

      Vec3 vec31 = this.getDeltaMovement();
      this.setPos(this.position().add(vec31));
      this.applyEffectsFromBlocks();
      if (this.portalProcess != null && this.portalProcess.isInsidePortalThisTick()) {
         this.handlePortal();
      }

      if (hitresult != null && this.isAlive() && hitresult.getType() != HitResult.Type.MISS) {
         this.hitTargetOrDeflectSelf(hitresult);
      }

      ProjectileUtil.rotateTowardsMovement(this, 0.5F);
      if (this.level().isClientSide()) {
         this.level().addParticle(ParticleTypes.END_ROD, this.getX() - vec31.x, this.getY() - vec31.y + 0.15, this.getZ() - vec31.z, 0.0, 0.0, 0.0);
      } else if (entity != null) {
         if (this.flightSteps > 0) {
            this.flightSteps--;
            if (this.flightSteps == 0) {
               this.selectNextMoveDirection(this.currentMoveDirection == null ? null : this.currentMoveDirection.getAxis(), entity);
            }
         }

         if (this.currentMoveDirection != null) {
            BlockPos blockpos = this.blockPosition();
            Direction.Axis direction$axis = this.currentMoveDirection.getAxis();
            if (this.level().loadedAndEntityCanStandOn(blockpos.relative(this.currentMoveDirection), this)) {
               this.selectNextMoveDirection(direction$axis, entity);
            } else {
               BlockPos blockpos1 = entity.blockPosition();
               if (direction$axis == Direction.Axis.X && blockpos.getX() == blockpos1.getX()
                  || direction$axis == Direction.Axis.Z && blockpos.getZ() == blockpos1.getZ()
                  || direction$axis == Direction.Axis.Y && blockpos.getY() == blockpos1.getY()) {
                  this.selectNextMoveDirection(direction$axis, entity);
               }
            }
         }
      }
   }

   @Override
   protected boolean isAffectedByBlocks() {
      return !this.isRemoved();
   }

   @Override
   protected boolean canHitEntity(Entity p_37341_) {
      return super.canHitEntity(p_37341_) && !p_37341_.noPhysics;
   }

   @Override
   public boolean isOnFire() {
      return false;
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double p_37336_) {
      return p_37336_ < 16384.0;
   }

   @Override
   public float getLightLevelDependentMagicValue() {
      return 1.0F;
   }

   @Override
   protected void onHitEntity(EntityHitResult p_37345_) {
      super.onHitEntity(p_37345_);
      Entity entity = p_37345_.getEntity();
      Entity entity1 = this.getOwner();
      LivingEntity livingentity = entity1 instanceof LivingEntity ? (LivingEntity)entity1 : null;
      DamageSource damagesource = this.damageSources().mobProjectile(this, livingentity);
      boolean flag = entity.hurtOrSimulate(damagesource, 4.0F);
      if (flag) {
         if (this.level() instanceof ServerLevel serverlevel) {
            EnchantmentHelper.doPostAttackEffects(serverlevel, entity, damagesource);
         }

         if (entity instanceof LivingEntity livingentity1) {
            livingentity1.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 200), (Entity)MoreObjects.firstNonNull(entity1, this));
         }
      }
   }

   @Override
   protected void onHitBlock(BlockHitResult p_37343_) {
      super.onHitBlock(p_37343_);
      ((ServerLevel)this.level()).sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
      this.playSound(SoundEvents.SHULKER_BULLET_HIT, 1.0F, 1.0F);
   }

   private void destroy() {
      this.discard();
      this.level().gameEvent(GameEvent.ENTITY_DAMAGE, this.position(), GameEvent.Context.of(this));
   }

   @Override
   protected void onHit(HitResult p_37347_) {
      super.onHit(p_37347_);
      this.destroy();
   }

   @Override
   public boolean isPickable() {
      return true;
   }

   @Override
   public boolean hurtClient(DamageSource p_365713_) {
      return true;
   }

   @Override
   public boolean hurtServer(ServerLevel p_367903_, DamageSource p_368578_, float p_367428_) {
      this.playSound(SoundEvents.SHULKER_BULLET_HURT, 1.0F, 1.0F);
      p_367903_.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 15, 0.2, 0.2, 0.2, 0.0);
      this.destroy();
      return true;
   }

   @Override
   public void recreateFromPacket(ClientboundAddEntityPacket p_150185_) {
      super.recreateFromPacket(p_150185_);
      this.setDeltaMovement(p_150185_.getMovement());
   }
}
