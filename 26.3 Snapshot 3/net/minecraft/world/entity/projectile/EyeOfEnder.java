package net.minecraft.world.entity.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EyeOfEnder extends Entity implements ItemSupplier {
   private static final float MIN_CAMERA_DISTANCE_SQUARED = 12.25F;
   private static final float TOO_FAR_SIGNAL_HEIGHT = 8.0F;
   private static final float TOO_FAR_DISTANCE = 12.0F;
   private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK = SynchedEntityData.defineId(EyeOfEnder.class, EntityDataSerializers.ITEM_STACK);
   private @Nullable Vec3 target;
   private int life;
   private boolean surviveAfterDeath;

   public EyeOfEnder(final EntityType<? extends EyeOfEnder> type, final Level level) {
      super(type, level);
   }

   public EyeOfEnder(final Level level, final double x, final double y, final double z) {
      this(EntityTypes.EYE_OF_ENDER, level);
      this.setPos(x, y, z);
   }

   public void setItem(final ItemStack source) {
      if (source.isEmpty()) {
         this.getEntityData().set(DATA_ITEM_STACK, this.getDefaultItem());
      } else {
         this.getEntityData().set(DATA_ITEM_STACK, source.copyWithCount(1));
      }
   }

   @Override
   public ItemStack getItem() {
      return this.getEntityData().get(DATA_ITEM_STACK);
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      entityData.define(DATA_ITEM_STACK, this.getDefaultItem());
   }

   @Override
   public boolean shouldRenderAtSqrDistance(final double distance) {
      if (this.tickCount < 2 && distance < 12.25) {
         return false;
      }

      double size = this.getBoundingBox().getSize() * 4.0;
      if (Double.isNaN(size)) {
         size = 4.0;
      }

      size *= 64.0;
      return distance < size * size;
   }

   public void signalTo(final Vec3 target) {
      Vec3 delta = target.subtract(this.position());
      double horizontalDistance = delta.horizontalDistance();
      if (horizontalDistance > 12.0) {
         this.target = this.position().add(delta.x / horizontalDistance * 12.0, 8.0, delta.z / horizontalDistance * 12.0);
      } else {
         this.target = target;
      }

      this.life = 0;
      this.surviveAfterDeath = this.random.nextInt(5) > 0;
   }

   @Override
   public void tick() {
      super.tick();
      Vec3 newPosition = this.position().add(this.getDeltaMovement());
      if (!this.level().isClientSide() && this.target != null) {
         this.setDeltaMovement(updateDeltaMovement(this.getDeltaMovement(), newPosition, this.target));
      }

      if (this.level().isClientSide()) {
         Vec3 particleOrigin = newPosition.subtract(this.getDeltaMovement().scale(0.25));
         this.spawnParticles(particleOrigin, this.getDeltaMovement());
      }

      this.setPos(newPosition);
      if (!this.level().isClientSide()) {
         this.life++;
         if (this.life > 80 && !this.level().isClientSide()) {
            this.playSound(SoundEvents.ENDER_EYE_DEATH, 1.0F, 1.0F);
            this.discard();
            if (this.surviveAfterDeath) {
               this.level().addFreshEntity(new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), this.getItem()));
            } else {
               this.level().levelEvent(2003, this.blockPosition(), 0);
            }
         }
      }
   }

   private void spawnParticles(final Vec3 origin, final Vec3 movement) {
      if (this.isInWater()) {
         for (int i = 0; i < 4; i++) {
            this.level().addParticle(ParticleTypes.BUBBLE, origin.x, origin.y, origin.z, movement.x, movement.y, movement.z);
         }
      } else {
         this.level()
            .addParticle(
               ParticleTypes.PORTAL,
               origin.x + this.random.nextDouble() * 0.6 - 0.3,
               origin.y - 0.5,
               origin.z + this.random.nextDouble() * 0.6 - 0.3,
               movement.x,
               movement.y,
               movement.z
            );
      }
   }

   private static Vec3 updateDeltaMovement(final Vec3 oldMovement, final Vec3 position, final Vec3 target) {
      Vec3 horizontalDelta = new Vec3(target.x - position.x, 0.0, target.z - position.z);
      double horizontalLength = horizontalDelta.length();
      double wantedSpeed = Mth.lerp(0.0025, oldMovement.horizontalDistance(), horizontalLength);
      double movementY = oldMovement.y;
      if (horizontalLength < 1.0) {
         wantedSpeed *= 0.8;
         movementY *= 0.8;
      }

      double wantedMovementY = position.y - oldMovement.y < target.y ? 1.0 : -1.0;
      return horizontalDelta.scale(wantedSpeed / horizontalLength).add(0.0, movementY + (wantedMovementY - movementY) * 0.015, 0.0);
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      output.store("Item", ItemStack.CODEC, this.getItem());
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      this.setItem(input.<ItemStack>read("Item", ItemStack.CODEC).orElse(this.getDefaultItem()));
   }

   private ItemStack getDefaultItem() {
      return new ItemStack(Items.ENDER_EYE);
   }

   @Override
   public float getLightLevelDependentMagicValue() {
      return 1.0F;
   }

   @Override
   public boolean isAttackable() {
      return false;
   }

   @Override
   public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
      return false;
   }
}
