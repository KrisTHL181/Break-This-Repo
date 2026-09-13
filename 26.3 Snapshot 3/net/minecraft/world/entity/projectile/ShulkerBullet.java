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
import net.minecraft.world.entity.EntityTypes;
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

   public ShulkerBullet(final EntityType<? extends ShulkerBullet> type, final Level level) {
      super(type, level);
      this.noPhysics = true;
   }

   public ShulkerBullet(final Level level, final LivingEntity owner, final Entity target, final Direction.Axis invalidStartAxis) {
      this(EntityTypes.SHULKER_BULLET, level);
      this.setOwner(owner);
      Vec3 position = owner.getBoundingBox().getCenter();
      this.snapTo(position.x, position.y, position.z, this.getYRot(), this.getXRot());
      this.finalTarget = EntityReference.of(target);
      this.currentMoveDirection = Direction.UP;
      this.selectNextMoveDirection(invalidStartAxis, target);
   }

   @Override
   public SoundSource getSoundSource() {
      return SoundSource.HOSTILE;
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      if (this.finalTarget != null) {
         output.store("Target", UUIDUtil.CODEC, this.finalTarget.getUUID());
      }

      output.storeNullable("Dir", Direction.LEGACY_ID_CODEC, this.currentMoveDirection);
      output.putInt("Steps", this.flightSteps);
      output.putDouble("TXD", this.targetDeltaX);
      output.putDouble("TYD", this.targetDeltaY);
      output.putDouble("TZD", this.targetDeltaZ);
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.flightSteps = input.getIntOr("Steps", 0);
      this.targetDeltaX = input.getDoubleOr("TXD", 0.0);
      this.targetDeltaY = input.getDoubleOr("TYD", 0.0);
      this.targetDeltaZ = input.getDoubleOr("TZD", 0.0);
      this.currentMoveDirection = input.<Direction>read("Dir", Direction.LEGACY_ID_CODEC).orElse(null);
      this.finalTarget = EntityReference.read(input, "Target");
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
   }

   private @Nullable Direction getMoveDirection() {
      return this.currentMoveDirection;
   }

   private void setMoveDirection(final @Nullable Direction direction) {
      this.currentMoveDirection = direction;
   }

   private void selectNextMoveDirection(final Direction.@Nullable Axis avoidAxis, final @Nullable Entity target) {
      double yOffset = 0.5;
      BlockPos targetPos;
      if (target == null) {
         targetPos = this.blockPosition().below();
      } else {
         yOffset = target.getBbHeight() * 0.5;
         targetPos = BlockPos.containing(target.getX(), target.getY() + yOffset, target.getZ());
      }

      double targetX = targetPos.getX() + 0.5;
      double targetY = targetPos.getY() + yOffset;
      double targetZ = targetPos.getZ() + 0.5;
      Direction selection = null;
      if (!targetPos.closerToCenterThan(this.position(), 2.0)) {
         BlockPos current = this.blockPosition();
         List<Direction> options = Lists.newArrayList();
         if (avoidAxis != Direction.Axis.X) {
            if (current.getX() < targetPos.getX() && this.level().isEmptyBlock(current.east())) {
               options.add(Direction.EAST);
            } else if (current.getX() > targetPos.getX() && this.level().isEmptyBlock(current.west())) {
               options.add(Direction.WEST);
            }
         }

         if (avoidAxis != Direction.Axis.Y) {
            if (current.getY() < targetPos.getY() && this.level().isEmptyBlock(current.above())) {
               options.add(Direction.UP);
            } else if (current.getY() > targetPos.getY() && this.level().isEmptyBlock(current.below())) {
               options.add(Direction.DOWN);
            }
         }

         if (avoidAxis != Direction.Axis.Z) {
            if (current.getZ() < targetPos.getZ() && this.level().isEmptyBlock(current.south())) {
               options.add(Direction.SOUTH);
            } else if (current.getZ() > targetPos.getZ() && this.level().isEmptyBlock(current.north())) {
               options.add(Direction.NORTH);
            }
         }

         selection = Direction.getRandom(this.random);
         if (options.isEmpty()) {
            for (int attempts = 5; !this.level().isEmptyBlock(current.relative(selection)) && attempts > 0; attempts--) {
               selection = Direction.getRandom(this.random);
            }
         } else {
            selection = options.get(this.random.nextInt(options.size()));
         }

         targetX = this.getX() + selection.getStepX();
         targetY = this.getY() + selection.getStepY();
         targetZ = this.getZ() + selection.getStepZ();
      }

      this.setMoveDirection(selection);
      double xa = targetX - this.getX();
      double ya = targetY - this.getY();
      double za = targetZ - this.getZ();
      double distance = Math.sqrt(xa * xa + ya * ya + za * za);
      if (distance == 0.0) {
         this.targetDeltaX = 0.0;
         this.targetDeltaY = 0.0;
         this.targetDeltaZ = 0.0;
      } else {
         this.targetDeltaX = xa / distance * 0.15;
         this.targetDeltaY = ya / distance * 0.15;
         this.targetDeltaZ = za / distance * 0.15;
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
      Entity finalTarget = !this.level().isClientSide() ? EntityReference.getEntity(this.finalTarget, this.level()) : null;
      HitResult hitResult = null;
      if (!this.level().isClientSide()) {
         if (finalTarget == null) {
            this.finalTarget = null;
         }

         if (finalTarget == null || !finalTarget.isAlive() || finalTarget instanceof Player && finalTarget.isSpectator()) {
            this.applyGravity();
         } else {
            this.targetDeltaX = Mth.clamp(this.targetDeltaX * 1.025, -1.0, 1.0);
            this.targetDeltaY = Mth.clamp(this.targetDeltaY * 1.025, -1.0, 1.0);
            this.targetDeltaZ = Mth.clamp(this.targetDeltaZ * 1.025, -1.0, 1.0);
            Vec3 movement = this.getDeltaMovement();
            this.setDeltaMovement(
               movement.add((this.targetDeltaX - movement.x) * 0.2, (this.targetDeltaY - movement.y) * 0.2, (this.targetDeltaZ - movement.z) * 0.2)
            );
         }

         hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
      }

      Vec3 movement = this.getDeltaMovement();
      this.setPos(this.position().add(movement));
      this.applyEffectsFromBlocks();
      if (this.portalProcess != null && this.portalProcess.isInsidePortalThisTick()) {
         this.handlePortal();
      }

      if (hitResult != null && this.isAlive() && hitResult.getType() != HitResult.Type.MISS) {
         this.hitTargetOrDeflectSelf(hitResult);
      }

      ProjectileUtil.rotateTowardsMovement(this, 0.5F);
      if (this.level().isClientSide()) {
         this.level().addParticle(ParticleTypes.END_ROD, this.getX() - movement.x, this.getY() - movement.y + 0.15, this.getZ() - movement.z, 0.0, 0.0, 0.0);
      } else if (finalTarget != null) {
         if (this.flightSteps > 0) {
            this.flightSteps--;
            if (this.flightSteps == 0) {
               this.selectNextMoveDirection(this.currentMoveDirection == null ? null : this.currentMoveDirection.getAxis(), finalTarget);
            }
         }

         if (this.currentMoveDirection != null) {
            BlockPos current = this.blockPosition();
            Direction.Axis axis = this.currentMoveDirection.getAxis();
            if (this.level().loadedAndEntityCanStandOn(current.relative(this.currentMoveDirection), this)) {
               this.selectNextMoveDirection(axis, finalTarget);
            } else {
               BlockPos targetPos = finalTarget.blockPosition();
               if (axis == Direction.Axis.X && current.getX() == targetPos.getX()
                  || axis == Direction.Axis.Z && current.getZ() == targetPos.getZ()
                  || axis == Direction.Axis.Y && current.getY() == targetPos.getY()) {
                  this.selectNextMoveDirection(axis, finalTarget);
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
   protected boolean canHitEntity(final Entity entity) {
      return super.canHitEntity(entity) && !entity.noPhysics;
   }

   @Override
   public boolean isOnFire() {
      return false;
   }

   @Override
   public boolean shouldRenderAtSqrDistance(final double distance) {
      return distance < 16384.0;
   }

   @Override
   public float getLightLevelDependentMagicValue() {
      return 1.0F;
   }

   @Override
   protected void onHitEntity(final EntityHitResult hitResult) {
      super.onHitEntity(hitResult);
      Entity target = hitResult.getEntity();
      Entity owner = this.getOwner();
      LivingEntity livingOwner = owner instanceof LivingEntity livingEntity ? livingEntity : null;
      DamageSource damageSource = this.damageSources().mobProjectile(this, livingOwner);
      boolean wasHurt = target.hurtOrSimulate(damageSource, 4.0F);
      if (wasHurt) {
         if (this.level() instanceof ServerLevel serverLevel) {
            EnchantmentHelper.doPostAttackEffects(serverLevel, target, damageSource);
         }

         if (target instanceof LivingEntity livingTarget) {
            livingTarget.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 200), (Entity)MoreObjects.firstNonNull(owner, this));
         }
      }
   }

   @Override
   protected void onHitBlock(final BlockHitResult hitResult) {
      super.onHitBlock(hitResult);
      ((ServerLevel)this.level()).sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
      this.playSound(SoundEvents.SHULKER_BULLET_HIT, 1.0F, 1.0F);
   }

   private void destroy() {
      this.discard();
      this.level().gameEvent(GameEvent.ENTITY_DAMAGE, this.position(), GameEvent.Context.of(this));
   }

   @Override
   protected void onHit(final HitResult hitResult) {
      super.onHit(hitResult);
      this.destroy();
   }

   @Override
   public boolean isPickable() {
      return true;
   }

   @Override
   public boolean hurtClient(final DamageSource source) {
      return true;
   }

   @Override
   public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
      this.playSound(SoundEvents.SHULKER_BULLET_HURT, 1.0F, 1.0F);
      level.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 15, 0.2, 0.2, 0.2, 0.0);
      this.destroy();
      return true;
   }

   @Override
   public void recreateFromPacket(final ClientboundAddEntityPacket packet) {
      super.recreateFromPacket(packet);
      this.setDeltaMovement(packet.getMovement());
   }
}
