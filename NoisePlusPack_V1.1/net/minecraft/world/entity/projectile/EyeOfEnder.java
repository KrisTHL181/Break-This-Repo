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

   public EyeOfEnder(EntityType<? extends EyeOfEnder> p_36957_, Level p_36958_) {
      super(p_36957_, p_36958_);
   }

   public EyeOfEnder(Level p_36960_, double p_36961_, double p_36962_, double p_36963_) {
      this(EntityType.EYE_OF_ENDER, p_36960_);
      this.setPos(p_36961_, p_36962_, p_36963_);
   }

   public void setItem(ItemStack p_36973_) {
      if (p_36973_.isEmpty()) {
         this.getEntityData().set(DATA_ITEM_STACK, this.getDefaultItem());
      } else {
         this.getEntityData().set(DATA_ITEM_STACK, p_36973_.copyWithCount(1));
      }
   }

   @Override
   public ItemStack getItem() {
      return this.getEntityData().get(DATA_ITEM_STACK);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_333578_) {
      p_333578_.define(DATA_ITEM_STACK, this.getDefaultItem());
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double p_36966_) {
      if (this.tickCount < 2 && p_36966_ < 12.25) {
         return false;
      }

      double d0 = this.getBoundingBox().getSize() * 4.0;
      if (Double.isNaN(d0)) {
         d0 = 4.0;
      }

      d0 *= 64.0;
      return p_36966_ < d0 * d0;
   }

   public void signalTo(Vec3 p_409103_) {
      Vec3 vec3 = p_409103_.subtract(this.position());
      double d0 = vec3.horizontalDistance();
      if (d0 > 12.0) {
         this.target = this.position().add(vec3.x / d0 * 12.0, 8.0, vec3.z / d0 * 12.0);
      } else {
         this.target = p_409103_;
      }

      this.life = 0;
      this.surviveAfterDeath = this.random.nextInt(5) > 0;
   }

   @Override
   public void tick() {
      super.tick();
      Vec3 vec3 = this.position().add(this.getDeltaMovement());
      if (!this.level().isClientSide() && this.target != null) {
         this.setDeltaMovement(updateDeltaMovement(this.getDeltaMovement(), vec3, this.target));
      }

      if (this.level().isClientSide()) {
         Vec3 vec31 = vec3.subtract(this.getDeltaMovement().scale(0.25));
         this.spawnParticles(vec31, this.getDeltaMovement());
      }

      this.setPos(vec3);
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

   private void spawnParticles(Vec3 p_409874_, Vec3 p_410112_) {
      if (this.isInWater()) {
         for (int i = 0; i < 4; i++) {
            this.level().addParticle(ParticleTypes.BUBBLE, p_409874_.x, p_409874_.y, p_409874_.z, p_410112_.x, p_410112_.y, p_410112_.z);
         }
      } else {
         this.level()
            .addParticle(
               ParticleTypes.PORTAL,
               p_409874_.x + this.random.nextDouble() * 0.6 - 0.3,
               p_409874_.y - 0.5,
               p_409874_.z + this.random.nextDouble() * 0.6 - 0.3,
               p_410112_.x,
               p_410112_.y,
               p_410112_.z
            );
      }
   }

   private static Vec3 updateDeltaMovement(Vec3 p_407352_, Vec3 p_409654_, Vec3 p_408081_) {
      Vec3 vec3 = new Vec3(p_408081_.x - p_409654_.x, 0.0, p_408081_.z - p_409654_.z);
      double d0 = vec3.length();
      double d1 = Mth.lerp(0.0025, p_407352_.horizontalDistance(), d0);
      double d2 = p_407352_.y;
      if (d0 < 1.0) {
         d1 *= 0.8;
         d2 *= 0.8;
      }

      double d3 = p_409654_.y - p_407352_.y < p_408081_.y ? 1.0 : -1.0;
      return vec3.scale(d1 / d0).add(0.0, d2 + (d3 - d2) * 0.015, 0.0);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_406691_) {
      p_406691_.store("Item", ItemStack.CODEC, this.getItem());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_408863_) {
      this.setItem(p_408863_.<ItemStack>read("Item", ItemStack.CODEC).orElse(this.getDefaultItem()));
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
   public boolean hurtServer(ServerLevel p_361156_, DamageSource p_361721_, float p_361974_) {
      return false;
   }
}
