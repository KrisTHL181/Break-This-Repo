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
import net.minecraft.world.entity.EntityTypes;
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

   public AbstractWindCharge(final EntityType<? extends AbstractWindCharge> type, final Level level) {
      super(type, level);
      this.accelerationPower = 0.0;
   }

   public AbstractWindCharge(
      final EntityType<? extends AbstractWindCharge> type, final Level level, final Entity owner, final double x, final double y, final double z
   ) {
      super(type, x, y, z, level);
      this.setOwner(owner);
      this.accelerationPower = 0.0;
   }

   protected AbstractWindCharge(
      final EntityType<? extends AbstractWindCharge> type, final double x, final double y, final double z, final Vec3 direction, final Level level
   ) {
      super(type, x, y, z, direction, level);
      this.accelerationPower = 0.0;
   }

   @Override
   protected AABB makeBoundingBox(final Vec3 position) {
      float width = this.getType().getDimensions().width() / 2.0F;
      float height = this.getType().getDimensions().height();
      float offset = 0.15F;
      return new AABB(position.x - width, position.y - 0.15F, position.z - width, position.x + width, position.y - 0.15F + height, position.z + width);
   }

   @Override
   public boolean canCollideWith(final Entity entity) {
      return entity instanceof AbstractWindCharge ? false : super.canCollideWith(entity);
   }

   @Override
   protected boolean canHitEntity(final Entity entity) {
      if (entity instanceof AbstractWindCharge) {
         return false;
      } else {
         return entity.is(EntityTypes.END_CRYSTAL) ? false : super.canHitEntity(entity);
      }
   }

   @Override
   protected void onHitEntity(final EntityHitResult hitResult) {
      super.onHitEntity(hitResult);
      if (this.level() instanceof ServerLevel serverLevel) {
         LivingEntity owner = this.getOwner() instanceof LivingEntity entity ? entity : null;
         Entity entity = hitResult.getEntity();
         if (owner != null) {
            owner.setLastHurtMob(entity);
         }

         DamageSource source = this.damageSources().windCharge(this, owner);
         if (entity.hurtServer(serverLevel, source, 1.0F) && entity instanceof LivingEntity mob) {
            EnchantmentHelper.doPostAttackEffects(serverLevel, mob, source);
         }

         this.explode(this.position());
      }
   }

   @Override
   public void push(final double xa, final double ya, final double za) {
   }

   protected abstract void explode(final Vec3 position);

   @Override
   protected void onHitBlock(final BlockHitResult hitResult) {
      super.onHitBlock(hitResult);
      if (!this.level().isClientSide()) {
         Vec3i collisionNormal = hitResult.getDirection().getUnitVec3i();
         Vec3 scaledNormal = Vec3.atLowerCornerOf(collisionNormal).multiply(0.25, 0.25, 0.25);
         Vec3 explosionPos = hitResult.getLocation().add(scaledNormal);
         this.explode(explosionPos);
         this.discard();
      }
   }

   @Override
   protected void onHit(final HitResult hitResult) {
      super.onHit(hitResult);
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
