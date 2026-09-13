package net.minecraft.world.entity.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
   public LlamaSpit(EntityType<? extends LlamaSpit> p_37224_, Level p_37225_) {
      super(p_37224_, p_37225_);
   }

   public LlamaSpit(Level p_37235_, Llama p_458609_) {
      this(EntityType.LLAMA_SPIT, p_37235_);
      this.setOwner(p_458609_);
      this.setPos(
         p_458609_.getX() - (p_458609_.getBbWidth() + 1.0F) * 0.5 * Mth.sin(p_458609_.yBodyRot * (float) (Math.PI / 180.0)),
         p_458609_.getEyeY() - 0.1F,
         p_458609_.getZ() + (p_458609_.getBbWidth() + 1.0F) * 0.5 * Mth.cos(p_458609_.yBodyRot * (float) (Math.PI / 180.0))
      );
   }

   @Override
   protected double getDefaultGravity() {
      return 0.06;
   }

   @Override
   public void tick() {
      super.tick();
      Vec3 vec3 = this.getDeltaMovement();
      HitResult hitresult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
      this.hitTargetOrDeflectSelf(hitresult);
      double d0 = this.getX() + vec3.x;
      double d1 = this.getY() + vec3.y;
      double d2 = this.getZ() + vec3.z;
      this.updateRotation();
      float f = 0.99F;
      if (this.level().getBlockStates(this.getBoundingBox()).noneMatch(BlockBehaviour.BlockStateBase::isAir)) {
         this.discard();
      } else if (this.isInWater()) {
         this.discard();
      } else {
         this.setDeltaMovement(vec3.scale(0.99F));
         this.applyGravity();
         this.setPos(d0, d1, d2);
      }
   }

   @Override
   protected void onHitEntity(EntityHitResult p_37241_) {
      super.onHitEntity(p_37241_);
      if (this.getOwner() instanceof LivingEntity livingentity) {
         Entity entity = p_37241_.getEntity();
         DamageSource damagesource = this.damageSources().spit(this, livingentity);
         if (this.level() instanceof ServerLevel serverlevel && entity.hurtServer(serverlevel, damagesource, 1.0F)) {
            EnchantmentHelper.doPostAttackEffects(serverlevel, entity, damagesource);
         }
      }
   }

   @Override
   protected void onHitBlock(BlockHitResult p_37239_) {
      super.onHitBlock(p_37239_);
      if (!this.level().isClientSide()) {
         this.discard();
      }
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_335399_) {
   }

   @Override
   public void recreateFromPacket(ClientboundAddEntityPacket p_150162_) {
      super.recreateFromPacket(p_150162_);
      Vec3 vec3 = p_150162_.getMovement();

      for (int i = 0; i < 7; i++) {
         double d0 = 0.4 + 0.1 * i;
         this.level().addParticle(ParticleTypes.SPIT, this.getX(), this.getY(), this.getZ(), vec3.x * d0, vec3.y, vec3.z * d0);
      }

      this.setDeltaMovement(vec3);
   }
}
