package net.minecraft.world.entity.vehicle.minecart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class MinecartTNT extends AbstractMinecart {
   private static final String TAG_EXPLOSION_POWER = "explosion_power";
   private static final String TAG_EXPLOSION_SPEED_FACTOR = "explosion_speed_factor";
   private static final float DEFAULT_EXPLOSION_POWER_BASE = 4.0F;
   private static final float DEFAULT_EXPLOSION_SPEED_FACTOR = 1.0F;
   private @Nullable DamageSource ignitionSource;
   private int fuse = -1;
   private float explosionPowerBase = 4.0F;
   private float explosionSpeedFactor = 1.0F;

   public MinecartTNT(final EntityType<? extends MinecartTNT> type, final Level level) {
      super(type, level);
   }

   @Override
   public BlockState getDefaultDisplayBlockState() {
      return Blocks.TNT.defaultBlockState();
   }

   @Override
   public void tick() {
      super.tick();
      if (this.fuse > 0) {
         this.fuse--;
         this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 0.0, 0.0, 0.0);
      } else if (this.fuse == 0) {
         this.explode(this.ignitionSource, this.getDeltaMovement().horizontalDistanceSqr());
      }

      if (this.horizontalCollision) {
         double speedSqr = this.getDeltaMovement().horizontalDistanceSqr();
         if (speedSqr >= 0.01F) {
            this.explode(this.ignitionSource, speedSqr);
         }
      }
   }

   @Override
   public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
      if (source.getDirectEntity() instanceof AbstractArrow projectile && projectile.isOnFire()) {
         DamageSource damageSource = this.damageSources().explosion(this, source.getEntity());
         this.explode(damageSource, projectile.getDeltaMovement().lengthSqr());
      }

      return super.hurtServer(level, source, damage);
   }

   @Override
   public void destroy(final ServerLevel level, final DamageSource source) {
      double speedSqr = this.getDeltaMovement().horizontalDistanceSqr();
      if (!damageSourceIgnitesTnt(source) && !(speedSqr >= 0.01F)) {
         this.destroy(level, this.getDropItem());
      } else {
         if (this.fuse < 0) {
            this.primeFuse(source);
            this.fuse = this.random.nextInt(20) + this.random.nextInt(20);
         }
      }
   }

   @Override
   protected Item getDropItem() {
      return Items.TNT_MINECART;
   }

   @Override
   public ItemStack getPickResult() {
      return new ItemStack(Items.TNT_MINECART);
   }

   protected void explode(final @Nullable DamageSource damageSource, final double speedSqr) {
      if (this.level() instanceof ServerLevel level) {
         if (level.getGameRules().get(GameRules.TNT_EXPLODES)) {
            double speed = Math.min(Math.sqrt(speedSqr), 5.0);
            level.explode(
               this,
               damageSource,
               null,
               this.getX(),
               this.getY(),
               this.getZ(),
               (float)(this.explosionPowerBase + this.explosionSpeedFactor * this.random.nextDouble() * 1.5 * speed),
               false,
               Level.ExplosionInteraction.TNT
            );
            this.discard();
         } else if (this.isPrimed()) {
            this.discard();
         }
      }
   }

   @Override
   public boolean causeFallDamage(final double fallDistance, final float damageModifier, final DamageSource damageSource) {
      if (fallDistance >= 3.0) {
         double power = fallDistance / 10.0;
         this.explode(this.ignitionSource, power * power);
      }

      return super.causeFallDamage(fallDistance, damageModifier, damageSource);
   }

   @Override
   public void activateMinecart(final ServerLevel level, final int xt, final int yt, final int zt, final boolean state) {
      if (state && this.fuse < 0) {
         this.primeFuse(null);
      }
   }

   @Override
   public void handleEntityEvent(final byte id) {
      if (id == 70) {
         this.primeFuse(null);
      } else {
         super.handleEntityEvent(id);
      }
   }

   public void primeFuse(final @Nullable DamageSource source) {
      if (!(this.level() instanceof ServerLevel serverLevel && !serverLevel.getGameRules().get(GameRules.TNT_EXPLODES))) {
         this.fuse = 80;
         if (!this.level().isClientSide()) {
            if (source != null && this.ignitionSource == null) {
               this.ignitionSource = this.damageSources().explosion(this, source.getEntity());
            }

            this.level().broadcastEntityEvent(this, (byte)70);
            if (!this.isSilent()) {
               this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
         }
      }
   }

   public int getFuse() {
      return this.fuse;
   }

   public boolean isPrimed() {
      return this.fuse > -1;
   }

   @Override
   public float getBlockExplosionResistance(
      final Explosion explosion, final BlockGetter level, final BlockPos pos, final BlockState block, final FluidState fluid, final float resistance
   ) {
      return !this.isPrimed() || !block.is(BlockTags.RAILS) && !level.getBlockState(pos.above()).is(BlockTags.RAILS)
         ? super.getBlockExplosionResistance(explosion, level, pos, block, fluid, resistance)
         : 0.0F;
   }

   @Override
   public boolean shouldBlockExplode(final Explosion explosion, final BlockGetter level, final BlockPos pos, final BlockState state, final float power) {
      return !this.isPrimed() || !state.is(BlockTags.RAILS) && !level.getBlockState(pos.above()).is(BlockTags.RAILS)
         ? super.shouldBlockExplode(explosion, level, pos, state, power)
         : false;
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.fuse = input.getIntOr("fuse", -1);
      this.explosionPowerBase = Mth.clamp(input.getFloatOr("explosion_power", 4.0F), 0.0F, 128.0F);
      this.explosionSpeedFactor = Mth.clamp(input.getFloatOr("explosion_speed_factor", 1.0F), 0.0F, 128.0F);
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putInt("fuse", this.fuse);
      if (this.explosionPowerBase != 4.0F) {
         output.putFloat("explosion_power", this.explosionPowerBase);
      }

      if (this.explosionSpeedFactor != 1.0F) {
         output.putFloat("explosion_speed_factor", this.explosionSpeedFactor);
      }
   }

   @Override
   protected boolean shouldSourceDestroy(final DamageSource source) {
      return damageSourceIgnitesTnt(source);
   }

   private static boolean damageSourceIgnitesTnt(final DamageSource source) {
      return source.getDirectEntity() instanceof Projectile projectile
         ? projectile.isOnFire()
         : source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_EXPLOSION);
   }
}
