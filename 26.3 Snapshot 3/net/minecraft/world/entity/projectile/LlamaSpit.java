package net.minecraft.world.entity.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class LlamaSpit extends Projectile {
   public LlamaSpit(final EntityType<? extends LlamaSpit> type, final Level level) {
      super(type, level);
   }

   public LlamaSpit(final Level level, final Llama owner) {
      this(EntityTypes.LLAMA_SPIT, level);
      this.setOwner(owner);
      this.setPos(
         owner.getX() - (owner.getBbWidth() + 1.0F) * 0.5 * Mth.sin(owner.yBodyRot * (float) (Math.PI / 180.0)),
         owner.getEyeY() - 0.1F,
         owner.getZ() + (owner.getBbWidth() + 1.0F) * 0.5 * Mth.cos(owner.yBodyRot * (float) (Math.PI / 180.0))
      );
   }

   @Override
   protected double getDefaultGravity() {
      return 0.06;
   }

   @Override
   public void tick() {
      super.tick();
      Vec3 movement = this.getDeltaMovement();
      HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
      this.hitTargetOrDeflectSelf(hitResult);
      double x = this.getX() + movement.x;
      double y = this.getY() + movement.y;
      double z = this.getZ() + movement.z;
      this.updateRotation();
      boolean touchesNoAir = this.level().findBlocksIn(this.getBoundingBox()).filterState(BlockBehaviour.BlockStateBase::isAir).noneMatched();
      if (touchesNoAir) {
         this.discard();
      } else if (this.isInWater()) {
         this.discard();
      } else {
         this.setDeltaMovement(movement.scale(this.getAirDrag()));
         this.applyGravity();
         this.setPos(x, y, z);
      }
   }

   @Override
   protected float getAirDrag() {
      return 0.99F;
   }

   @Override
   protected void onHitEntity(final EntityHitResult hitResult) {
      super.onHitEntity(hitResult);
      if (this.getOwner() instanceof LivingEntity livingOwner) {
         Entity target = hitResult.getEntity();
         DamageSource damageSource = this.damageSources().spit(this, livingOwner);
         if (this.level() instanceof ServerLevel serverLevel && target.hurtServer(serverLevel, damageSource, 1.0F)) {
            EnchantmentHelper.doPostAttackEffects(serverLevel, target, damageSource);
         }
      }
   }

   @Override
   protected void onHitBlock(final BlockHitResult hitResult) {
      super.onHitBlock(hitResult);
      if (!this.level().isClientSide()) {
         this.discard();
      }
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
   }

   @Override
   public void recreateFromPacket(final ClientboundAddEntityPacket packet) {
      super.recreateFromPacket(packet);
      Vec3 movement = packet.getMovement();

      for (int i = 0; i < 7; i++) {
         double k = 0.4 + 0.1 * i;
         this.level().addParticle(ParticleTypes.SPIT, this.getX(), this.getY(), this.getZ(), movement.x * k, movement.y, movement.z * k);
      }

      this.setDeltaMovement(movement);
   }
}
