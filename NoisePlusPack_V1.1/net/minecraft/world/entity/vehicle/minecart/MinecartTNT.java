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
   private static final byte EVENT_PRIME = 10;
   private static final String TAG_EXPLOSION_POWER = "explosion_power";
   private static final String TAG_EXPLOSION_SPEED_FACTOR = "explosion_speed_factor";
   private static final String TAG_FUSE = "fuse";
   private static final float DEFAULT_EXPLOSION_POWER_BASE = 4.0F;
   private static final float DEFAULT_EXPLOSION_SPEED_FACTOR = 1.0F;
   private static final int NO_FUSE = -1;
   private @Nullable DamageSource ignitionSource;
   private int fuse = -1;
   private float explosionPowerBase = 4.0F;
   private float explosionSpeedFactor = 1.0F;

   public MinecartTNT(EntityType<? extends MinecartTNT> p_456481_, Level p_457101_) {
      super(p_456481_, p_457101_);
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
         double d0 = this.getDeltaMovement().horizontalDistanceSqr();
         if (d0 >= 0.01F) {
            this.explode(this.ignitionSource, d0);
         }
      }
   }

   @Override
   public boolean hurtServer(ServerLevel p_456976_, DamageSource p_455434_, float p_451580_) {
      if (p_455434_.getDirectEntity() instanceof AbstractArrow abstractarrow && abstractarrow.isOnFire()) {
         DamageSource damagesource = this.damageSources().explosion(this, p_455434_.getEntity());
         this.explode(damagesource, abstractarrow.getDeltaMovement().lengthSqr());
      }

      return super.hurtServer(p_456976_, p_455434_, p_451580_);
   }

   @Override
   public void destroy(ServerLevel p_459039_, DamageSource p_458713_) {
      double d0 = this.getDeltaMovement().horizontalDistanceSqr();
      if (!damageSourceIgnitesTnt(p_458713_) && !(d0 >= 0.01F)) {
         this.destroy(p_459039_, this.getDropItem());
      } else if (this.fuse < 0) {
         this.primeFuse(p_458713_);
         this.fuse = this.random.nextInt(20) + this.random.nextInt(20);
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

   protected void explode(@Nullable DamageSource p_455684_, double p_458918_) {
      if (this.level() instanceof ServerLevel serverlevel) {
         if (serverlevel.getGameRules().get(GameRules.TNT_EXPLODES)) {
            double d0 = Math.min(Math.sqrt(p_458918_), 5.0);
            serverlevel.explode(
               this,
               p_455684_,
               null,
               this.getX(),
               this.getY(),
               this.getZ(),
               (float)(this.explosionPowerBase + this.explosionSpeedFactor * this.random.nextDouble() * 1.5 * d0),
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
   public boolean causeFallDamage(double p_450953_, float p_450492_, DamageSource p_458485_) {
      if (p_450953_ >= 3.0) {
         double d0 = p_450953_ / 10.0;
         this.explode(this.ignitionSource, d0 * d0);
      }

      return super.causeFallDamage(p_450953_, p_450492_, p_458485_);
   }

   @Override
   public void activateMinecart(ServerLevel p_459272_, int p_450376_, int p_451662_, int p_453250_, boolean p_459021_) {
      if (p_459021_ && this.fuse < 0) {
         this.primeFuse(null);
      }
   }

   @Override
   public void handleEntityEvent(byte p_453988_) {
      if (p_453988_ == 10) {
         this.primeFuse(null);
      } else {
         super.handleEntityEvent(p_453988_);
      }
   }

   public void primeFuse(@Nullable DamageSource p_460279_) {
      if (!(this.level() instanceof ServerLevel serverlevel && !serverlevel.getGameRules().get(GameRules.TNT_EXPLODES))) {
         this.fuse = 80;
         if (!this.level().isClientSide()) {
            if (p_460279_ != null && this.ignitionSource == null) {
               this.ignitionSource = this.damageSources().explosion(this, p_460279_.getEntity());
            }

            this.level().broadcastEntityEvent(this, (byte)10);
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
      Explosion p_459793_, BlockGetter p_459518_, BlockPos p_450444_, BlockState p_457326_, FluidState p_456082_, float p_458144_
   ) {
      return this.isPrimed() && (p_457326_.is(BlockTags.RAILS) || p_459518_.getBlockState(p_450444_.above()).is(BlockTags.RAILS))
         ? 0.0F
         : super.getBlockExplosionResistance(p_459793_, p_459518_, p_450444_, p_457326_, p_456082_, p_458144_);
   }

   @Override
   public boolean shouldBlockExplode(Explosion p_453597_, BlockGetter p_459946_, BlockPos p_455213_, BlockState p_460066_, float p_456206_) {
      return this.isPrimed() && (p_460066_.is(BlockTags.RAILS) || p_459946_.getBlockState(p_455213_.above()).is(BlockTags.RAILS))
         ? false
         : super.shouldBlockExplode(p_453597_, p_459946_, p_455213_, p_460066_, p_456206_);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_457654_) {
      super.readAdditionalSaveData(p_457654_);
      this.fuse = p_457654_.getIntOr("fuse", -1);
      this.explosionPowerBase = Mth.clamp(p_457654_.getFloatOr("explosion_power", 4.0F), 0.0F, 128.0F);
      this.explosionSpeedFactor = Mth.clamp(p_457654_.getFloatOr("explosion_speed_factor", 1.0F), 0.0F, 128.0F);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_452870_) {
      super.addAdditionalSaveData(p_452870_);
      p_452870_.putInt("fuse", this.fuse);
      if (this.explosionPowerBase != 4.0F) {
         p_452870_.putFloat("explosion_power", this.explosionPowerBase);
      }

      if (this.explosionSpeedFactor != 1.0F) {
         p_452870_.putFloat("explosion_speed_factor", this.explosionSpeedFactor);
      }
   }

   @Override
   protected boolean shouldSourceDestroy(DamageSource p_456145_) {
      return damageSourceIgnitesTnt(p_456145_);
   }

   private static boolean damageSourceIgnitesTnt(DamageSource p_451871_) {
      return p_451871_.getDirectEntity() instanceof Projectile projectile
         ? projectile.isOnFire()
         : p_451871_.is(DamageTypeTags.IS_FIRE) || p_451871_.is(DamageTypeTags.IS_EXPLOSION);
   }
}
