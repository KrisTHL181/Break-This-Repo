package net.minecraft.world.entity.projectile.hurtingprojectile.windcharge;

import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class AbstractWindCharge extends AbstractHurtingProjectile implements ItemSupplier {
   public static final ExplosionDamageCalculator EXPLOSION_DAMAGE_CALCULATOR = new SimpleExplosionDamageCalculator(
      true, false, Optional.empty(), BuiltInRegistries.BLOCK.get(BlockTags.BLOCKS_WIND_CHARGE_EXPLOSIONS).map(Function.identity())
   );
   public static final double JUMP_SCALE = 0.25;

   public AbstractWindCharge(EntityType<? extends AbstractWindCharge> p_453074_, Level p_459786_) {
      super(p_453074_, p_459786_);
      this.accelerationPower = 0.0;
   }

   public AbstractWindCharge(
      EntityType<? extends AbstractWindCharge> p_451453_, Level p_456231_, Entity p_456583_, double p_457773_, double p_458724_, double p_455563_
   ) {
      super(p_451453_, p_457773_, p_458724_, p_455563_, p_456231_);
      this.setOwner(p_456583_);
      this.accelerationPower = 0.0;
   }

   AbstractWindCharge(EntityType<? extends AbstractWindCharge> p_454144_, double p_460954_, double p_451354_, double p_451778_, Vec3 p_458042_, Level p_454501_) {
      super(p_454144_, p_460954_, p_451354_, p_451778_, p_458042_, p_454501_);
      this.accelerationPower = 0.0;
   }

   @Override
   protected AABB makeBoundingBox(Vec3 p_454399_) {
      float f = this.getType().getDimensions().width() / 2.0F;
      float f1 = this.getType().getDimensions().height();
      float f2 = 0.15F;
      return new AABB(p_454399_.x - f, p_454399_.y - 0.15F, p_454399_.z - f, p_454399_.x + f, p_454399_.y - 0.15F + f1, p_454399_.z + f);
   }

   @Override
   public boolean canCollideWith(Entity p_457460_) {
      return p_457460_ instanceof AbstractWindCharge ? false : super.canCollideWith(p_457460_);
   }

   @Override
   protected boolean canHitEntity(Entity p_457557_) {
      if (p_457557_ instanceof AbstractWindCharge) {
         return false;
      } else {
         return p_457557_.getType() == EntityType.END_CRYSTAL ? false : super.canHitEntity(p_457557_);
      }
   }

   @Override
   protected void onHitEntity(EntityHitResult p_452643_) {
      super.onHitEntity(p_452643_);
      if (this.level() instanceof ServerLevel serverlevel) {
         LivingEntity livingentity2 = this.getOwner() instanceof LivingEntity livingentity ? livingentity : null;
         Entity entity = p_452643_.getEntity();
         if (livingentity2 != null) {
            livingentity2.setLastHurtMob(entity);
         }

         DamageSource damagesource = this.damageSources().windCharge(this, livingentity2);
         if (entity.hurtServer(serverlevel, damagesource, 1.0F) && entity instanceof LivingEntity livingentity1) {
            EnchantmentHelper.doPostAttackEffects(serverlevel, livingentity1, damagesource);
         }

         this.explode(this.position());
      }
   }

   @Override
   public void push(double p_454959_, double p_454422_, double p_457808_) {
   }

   protected abstract void explode(Vec3 var1);

   @Override
   protected void onHitBlock(BlockHitResult p_453843_) {
      super.onHitBlock(p_453843_);
      if (!this.level().isClientSide()) {
         Vec3i vec3i = p_453843_.getDirection().getUnitVec3i();
         Vec3 vec3 = Vec3.atLowerCornerOf(vec3i).multiply(0.25, 0.25, 0.25);
         Vec3 vec31 = p_453843_.getLocation().add(vec3);
         this.explode(vec31);
         this.discard();
      }
   }

   @Override
   protected void onHit(HitResult p_454402_) {
      super.onHit(p_454402_);
      if (!this.level().isClientSide()) {
         this.discard();
      }
   }

   @Override
   protected boolean shouldBurn() {
      return false;
   }

   @Override
   public ItemStack getItem() {
      return ItemStack.EMPTY;
   }

   @Override
   protected float getInertia() {
      return 1.0F;
   }

   @Override
   protected float getLiquidInertia() {
      return this.getInertia();
   }

   @Override
   protected @Nullable ParticleOptions getTrailParticle() {
      return null;
   }

   @Override
   public void tick() {
      if (!this.level().isClientSide() && this.getBlockY() > this.level().getMaxY() + 30) {
         this.explode(this.position());
         this.discard();
      } else {
         super.tick();
      }
   }
}
