package net.minecraft.world.entity.projectile.arrow;

import java.util.Collection;
import java.util.List;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ThrownTrident extends AbstractArrow {
   private static final EntityDataAccessor<Byte> ID_LOYALTY = SynchedEntityData.defineId(ThrownTrident.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Boolean> ID_FOIL = SynchedEntityData.defineId(ThrownTrident.class, EntityDataSerializers.BOOLEAN);
   private static final float WATER_INERTIA = 0.99F;
   private static final boolean DEFAULT_DEALT_DAMAGE = false;
   private boolean dealtDamage = false;
   public int clientSideReturnTridentTickCount;

   public ThrownTrident(EntityType<? extends ThrownTrident> p_455793_, Level p_457632_) {
      super(p_455793_, p_457632_);
   }

   public ThrownTrident(Level p_456878_, LivingEntity p_453655_, ItemStack p_459649_) {
      super(EntityType.TRIDENT, p_453655_, p_456878_, p_459649_, null);
      this.entityData.set(ID_LOYALTY, this.getLoyaltyFromItem(p_459649_));
      this.entityData.set(ID_FOIL, p_459649_.hasFoil());
   }

   public ThrownTrident(Level p_460291_, double p_456333_, double p_452272_, double p_451613_, ItemStack p_459880_) {
      super(EntityType.TRIDENT, p_456333_, p_452272_, p_451613_, p_460291_, p_459880_, p_459880_);
      this.entityData.set(ID_LOYALTY, this.getLoyaltyFromItem(p_459880_));
      this.entityData.set(ID_FOIL, p_459880_.hasFoil());
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_453380_) {
      super.defineSynchedData(p_453380_);
      p_453380_.define(ID_LOYALTY, (byte)0);
      p_453380_.define(ID_FOIL, false);
   }

   @Override
   public void tick() {
      if (this.inGroundTime > 4) {
         this.dealtDamage = true;
      }

      Entity entity = this.getOwner();
      int i = this.entityData.get(ID_LOYALTY);
      if (i > 0 && (this.dealtDamage || this.isNoPhysics()) && entity != null) {
         if (!this.isAcceptibleReturnOwner()) {
            if (this.level() instanceof ServerLevel serverlevel && this.pickup == AbstractArrow.Pickup.ALLOWED) {
               this.spawnAtLocation(serverlevel, this.getPickupItem(), 0.1F);
            }

            this.discard();
         } else {
            if (!(entity instanceof Player) && this.position().distanceTo(entity.getEyePosition()) < entity.getBbWidth() + 1.0) {
               this.discard();
               return;
            }

            this.setNoPhysics(true);
            Vec3 vec3 = entity.getEyePosition().subtract(this.position());
            this.setPosRaw(this.getX(), this.getY() + vec3.y * 0.015 * i, this.getZ());
            double d0 = 0.05 * i;
            this.setDeltaMovement(this.getDeltaMovement().scale(0.95).add(vec3.normalize().scale(d0)));
            if (this.clientSideReturnTridentTickCount == 0) {
               this.playSound(SoundEvents.TRIDENT_RETURN, 10.0F, 1.0F);
            }

            this.clientSideReturnTridentTickCount++;
         }
      }

      super.tick();
   }

   private boolean isAcceptibleReturnOwner() {
      Entity entity = this.getOwner();
      return entity != null && entity.isAlive() ? !(entity instanceof ServerPlayer) || !entity.isSpectator() : false;
   }

   public boolean isFoil() {
      return this.entityData.get(ID_FOIL);
   }

   @Override
   protected @Nullable EntityHitResult findHitEntity(Vec3 p_452505_, Vec3 p_453602_) {
      return this.dealtDamage ? null : super.findHitEntity(p_452505_, p_453602_);
   }

   @Override
   protected Collection<EntityHitResult> findHitEntities(Vec3 p_454424_, Vec3 p_460659_) {
      EntityHitResult entityhitresult = this.findHitEntity(p_454424_, p_460659_);
      return entityhitresult != null ? List.of(entityhitresult) : List.of();
   }

   @Override
   protected void onHitEntity(EntityHitResult p_458307_) {
      Entity entity = p_458307_.getEntity();
      float f = 8.0F;
      Entity entity1 = this.getOwner();
      DamageSource damagesource = this.damageSources().trident(this, entity1 == null ? this : entity1);
      if (this.level() instanceof ServerLevel serverlevel) {
         f = EnchantmentHelper.modifyDamage(serverlevel, this.getWeaponItem(), entity, damagesource, f);
      }

      this.dealtDamage = true;
      if (entity.hurtOrSimulate(damagesource, f)) {
         if (entity.getType() == EntityType.ENDERMAN) {
            return;
         }

         if (this.level() instanceof ServerLevel serverlevel1) {
            EnchantmentHelper.doPostAttackEffectsWithItemSourceOnBreak(
               serverlevel1, entity, damagesource, this.getWeaponItem(), p_459928_ -> this.kill(serverlevel1)
            );
         }

         if (entity instanceof LivingEntity livingentity) {
            this.doKnockback(livingentity, damagesource);
            this.doPostHurtEffects(livingentity);
         }
      }

      this.deflect(ProjectileDeflection.REVERSE, entity, this.owner, false);
      this.setDeltaMovement(this.getDeltaMovement().multiply(0.02, 0.2, 0.02));
      this.playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.0F);
   }

   @Override
   protected void hitBlockEnchantmentEffects(ServerLevel p_452220_, BlockHitResult p_451086_, ItemStack p_457307_) {
      Vec3 vec3 = p_451086_.getBlockPos().clampLocationWithin(p_451086_.getLocation());
      EnchantmentHelper.onHitBlock(
         p_452220_,
         p_457307_,
         this.getOwner() instanceof LivingEntity livingentity ? livingentity : null,
         this,
         null,
         vec3,
         p_452220_.getBlockState(p_451086_.getBlockPos()),
         p_456946_ -> this.kill(p_452220_)
      );
   }

   @Override
   public ItemStack getWeaponItem() {
      return this.getPickupItemStackOrigin();
   }

   @Override
   protected boolean tryPickup(Player p_458354_) {
      return super.tryPickup(p_458354_) || this.isNoPhysics() && this.ownedBy(p_458354_) && p_458354_.getInventory().add(this.getPickupItem());
   }

   @Override
   protected ItemStack getDefaultPickupItem() {
      return new ItemStack(Items.TRIDENT);
   }

   @Override
   protected SoundEvent getDefaultHitGroundSoundEvent() {
      return SoundEvents.TRIDENT_HIT_GROUND;
   }

   @Override
   public void playerTouch(Player p_459436_) {
      if (this.ownedBy(p_459436_) || this.getOwner() == null) {
         super.playerTouch(p_459436_);
      }
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_455365_) {
      super.readAdditionalSaveData(p_455365_);
      this.dealtDamage = p_455365_.getBooleanOr("DealtDamage", false);
      this.entityData.set(ID_LOYALTY, this.getLoyaltyFromItem(this.getPickupItemStackOrigin()));
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_458339_) {
      super.addAdditionalSaveData(p_458339_);
      p_458339_.putBoolean("DealtDamage", this.dealtDamage);
   }

   private byte getLoyaltyFromItem(ItemStack p_458522_) {
      return this.level() instanceof ServerLevel serverlevel
         ? (byte)Mth.clamp(EnchantmentHelper.getTridentReturnToOwnerAcceleration(serverlevel, p_458522_, this), 0, 127)
         : 0;
   }

   @Override
   public void tickDespawn() {
      int i = this.entityData.get(ID_LOYALTY);
      if (this.pickup != AbstractArrow.Pickup.ALLOWED || i <= 0) {
         super.tickDespawn();
      }
   }

   @Override
   protected float getWaterInertia() {
      return 0.99F;
   }

   @Override
   public boolean shouldRender(double p_452378_, double p_454560_, double p_452379_) {
      return true;
   }
}
