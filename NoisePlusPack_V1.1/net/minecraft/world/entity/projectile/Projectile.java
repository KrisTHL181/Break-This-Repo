package net.minecraft.world.entity.projectile;

import com.google.common.base.MoreObjects;
import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class Projectile extends Entity implements TraceableEntity {
   private static final boolean DEFAULT_LEFT_OWNER = false;
   private static final boolean DEFAULT_HAS_BEEN_SHOT = false;
   protected @Nullable EntityReference<Entity> owner;
   private boolean leftOwner = false;
   private boolean leftOwnerChecked;
   private boolean hasBeenShot = false;
   private @Nullable Entity lastDeflectedBy;

   protected Projectile(EntityType<? extends Projectile> p_37248_, Level p_37249_) {
      super(p_37248_, p_37249_);
   }

   protected void setOwner(@Nullable EntityReference<Entity> p_407728_) {
      this.owner = p_407728_;
   }

   public void setOwner(@Nullable Entity p_37263_) {
      this.setOwner(EntityReference.of(p_37263_));
   }

   @Override
   public @Nullable Entity getOwner() {
      return EntityReference.getEntity(this.owner, this.level());
   }

   public Entity getEffectSource() {
      return (Entity)MoreObjects.firstNonNull(this.getOwner(), this);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_409152_) {
      EntityReference.store(this.owner, p_409152_, "Owner");
      if (this.leftOwner) {
         p_409152_.putBoolean("LeftOwner", true);
      }

      p_409152_.putBoolean("HasBeenShot", this.hasBeenShot);
   }

   protected boolean ownedBy(Entity p_150172_) {
      return this.owner != null && this.owner.matches(p_150172_);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_410006_) {
      this.setOwner(EntityReference.read(p_410006_, "Owner"));
      this.leftOwner = p_410006_.getBooleanOr("LeftOwner", false);
      this.hasBeenShot = p_410006_.getBooleanOr("HasBeenShot", false);
   }

   @Override
   public void restoreFrom(Entity p_310133_) {
      super.restoreFrom(p_310133_);
      if (p_310133_ instanceof Projectile projectile) {
         this.owner = projectile.owner;
      }
   }

   @Override
   public void tick() {
      if (!this.hasBeenShot) {
         this.gameEvent(GameEvent.PROJECTILE_SHOOT, this.getOwner());
         this.hasBeenShot = true;
      }

      this.checkLeftOwner();
      super.tick();
      this.leftOwnerChecked = false;
   }

   protected void checkLeftOwner() {
      if (!this.leftOwner && !this.leftOwnerChecked) {
         this.leftOwner = this.isOutsideOwnerCollisionRange();
         this.leftOwnerChecked = true;
      }
   }

   private boolean isOutsideOwnerCollisionRange() {
      Entity entity = this.getOwner();
      if (entity != null) {
         AABB aabb = this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0);
         return entity.getRootVehicle()
            .getSelfAndPassengers()
            .filter(EntitySelector.CAN_BE_PICKED)
            .noneMatch(p_359340_ -> aabb.intersects(p_359340_.getBoundingBox()));
      } else {
         return true;
      }
   }

   public Vec3 getMovementToShoot(double p_335302_, double p_334829_, double p_334312_, float p_331363_, float p_330173_) {
      return new Vec3(p_335302_, p_334829_, p_334312_)
         .normalize()
         .add(
            this.random.triangle(0.0, 0.0172275 * p_330173_),
            this.random.triangle(0.0, 0.0172275 * p_330173_),
            this.random.triangle(0.0, 0.0172275 * p_330173_)
         )
         .scale(p_331363_);
   }

   public void shoot(double p_37266_, double p_37267_, double p_37268_, float p_37269_, float p_37270_) {
      Vec3 vec3 = this.getMovementToShoot(p_37266_, p_37267_, p_37268_, p_37269_, p_37270_);
      this.setDeltaMovement(vec3);
      this.needsSync = true;
      double d0 = vec3.horizontalDistance();
      this.setYRot((float)(Mth.atan2(vec3.x, vec3.z) * 180.0 / (float) Math.PI));
      this.setXRot((float)(Mth.atan2(vec3.y, d0) * 180.0 / (float) Math.PI));
      this.yRotO = this.getYRot();
      this.xRotO = this.getXRot();
   }

   public void shootFromRotation(Entity p_37252_, float p_37253_, float p_37254_, float p_37255_, float p_37256_, float p_37257_) {
      float f = -Mth.sin(p_37254_ * (float) (Math.PI / 180.0)) * Mth.cos(p_37253_ * (float) (Math.PI / 180.0));
      float f1 = -Mth.sin((p_37253_ + p_37255_) * (float) (Math.PI / 180.0));
      float f2 = Mth.cos(p_37254_ * (float) (Math.PI / 180.0)) * Mth.cos(p_37253_ * (float) (Math.PI / 180.0));
      this.shoot(f, f1, f2, p_37256_, p_37257_);
      Vec3 vec3 = p_37252_.getKnownMovement();
      this.setDeltaMovement(this.getDeltaMovement().add(vec3.x, p_37252_.onGround() ? 0.0 : vec3.y, vec3.z));
   }

   @Override
   public void onAboveBubbleColumn(boolean p_395187_, BlockPos p_397623_) {
      double d0 = p_395187_ ? -0.03 : 0.1;
      this.setDeltaMovement(this.getDeltaMovement().add(0.0, d0, 0.0));
      sendBubbleColumnParticles(this.level(), p_397623_);
   }

   @Override
   public void onInsideBubbleColumn(boolean p_395217_) {
      double d0 = p_395217_ ? -0.03 : 0.06;
      this.setDeltaMovement(this.getDeltaMovement().add(0.0, d0, 0.0));
      this.resetFallDistance();
   }

   public static <T extends Projectile> T spawnProjectileFromRotation(
      Projectile.ProjectileFactory<T> p_364630_,
      ServerLevel p_369390_,
      ItemStack p_367599_,
      LivingEntity p_361588_,
      float p_367396_,
      float p_363677_,
      float p_365637_
   ) {
      return spawnProjectile(
         p_364630_.create(p_369390_, p_361588_, p_367599_),
         p_369390_,
         p_367599_,
         p_449736_ -> p_449736_.shootFromRotation(p_361588_, p_361588_.getXRot(), p_361588_.getYRot(), p_367396_, p_363677_, p_365637_)
      );
   }

   public static <T extends Projectile> T spawnProjectileUsingShoot(
      Projectile.ProjectileFactory<T> p_362783_,
      ServerLevel p_362807_,
      ItemStack p_361126_,
      LivingEntity p_368296_,
      double p_367312_,
      double p_361634_,
      double p_367734_,
      float p_361151_,
      float p_368071_
   ) {
      return spawnProjectile(
         p_362783_.create(p_362807_, p_368296_, p_361126_),
         p_362807_,
         p_361126_,
         p_359337_ -> p_359337_.shoot(p_367312_, p_361634_, p_367734_, p_361151_, p_368071_)
      );
   }

   public static <T extends Projectile> T spawnProjectileUsingShoot(
      T p_367886_, ServerLevel p_360818_, ItemStack p_364412_, double p_362828_, double p_361067_, double p_368213_, float p_366268_, float p_361310_
   ) {
      return spawnProjectile(p_367886_, p_360818_, p_364412_, p_359347_ -> p_367886_.shoot(p_362828_, p_361067_, p_368213_, p_366268_, p_361310_));
   }

   public static <T extends Projectile> T spawnProjectile(T p_361503_, ServerLevel p_367711_, ItemStack p_361747_) {
      return spawnProjectile(p_361503_, p_367711_, p_361747_, p_359326_ -> {});
   }

   public static <T extends Projectile> T spawnProjectile(T p_365177_, ServerLevel p_365242_, ItemStack p_366479_, Consumer<T> p_360962_) {
      p_360962_.accept(p_365177_);
      p_365242_.addFreshEntity(p_365177_);
      p_365177_.applyOnProjectileSpawned(p_365242_, p_366479_);
      return p_365177_;
   }

   public void applyOnProjectileSpawned(ServerLevel p_363701_, ItemStack p_365738_) {
      EnchantmentHelper.onProjectileSpawned(p_363701_, p_365738_, this, p_359338_ -> {});
      if (this instanceof AbstractArrow abstractarrow) {
         ItemStack itemstack = abstractarrow.getWeaponItem();
         if (itemstack != null && !itemstack.isEmpty() && !p_365738_.getItem().equals(itemstack.getItem())) {
            EnchantmentHelper.onProjectileSpawned(p_363701_, itemstack, this, abstractarrow::onItemBreak);
         }
      }
   }

   protected ProjectileDeflection hitTargetOrDeflectSelf(HitResult p_329816_) {
      if (p_329816_.getType() == HitResult.Type.ENTITY) {
         EntityHitResult entityhitresult = (EntityHitResult)p_329816_;
         Entity entity = entityhitresult.getEntity();
         ProjectileDeflection projectiledeflection = entity.deflection(this);
         if (projectiledeflection != ProjectileDeflection.NONE) {
            if (entity != this.lastDeflectedBy && this.deflect(projectiledeflection, entity, this.owner, false)) {
               this.lastDeflectedBy = entity;
            }

            return projectiledeflection;
         }
      } else if (this.shouldBounceOnWorldBorder() && p_329816_ instanceof BlockHitResult blockhitresult && blockhitresult.isWorldBorderHit()) {
         ProjectileDeflection projectiledeflection1 = ProjectileDeflection.REVERSE;
         if (this.deflect(projectiledeflection1, null, this.owner, false)) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.2));
            return projectiledeflection1;
         }
      }

      this.onHit(p_329816_);
      return ProjectileDeflection.NONE;
   }

   protected boolean shouldBounceOnWorldBorder() {
      return false;
   }

   public boolean deflect(ProjectileDeflection p_328550_, @Nullable Entity p_330074_, @Nullable EntityReference<Entity> p_424318_, boolean p_328333_) {
      p_328550_.deflect(this, p_330074_, this.random);
      if (!this.level().isClientSide()) {
         this.setOwner(p_424318_);
         this.onDeflection(p_328333_);
      }

      return true;
   }

   protected void onDeflection(boolean p_335911_) {
   }

   protected void onItemBreak(Item p_366262_) {
   }

   protected void onHit(HitResult p_37260_) {
      HitResult.Type hitresult$type = p_37260_.getType();
      if (hitresult$type == HitResult.Type.ENTITY) {
         EntityHitResult entityhitresult = (EntityHitResult)p_37260_;
         Entity entity = entityhitresult.getEntity();
         if (entity.getType().is(EntityTypeTags.REDIRECTABLE_PROJECTILE) && entity instanceof Projectile projectile) {
            projectile.deflect(ProjectileDeflection.AIM_DEFLECT, this.getOwner(), this.owner, true);
         }

         this.onHitEntity(entityhitresult);
         this.level().gameEvent(GameEvent.PROJECTILE_LAND, p_37260_.getLocation(), GameEvent.Context.of(this, null));
      } else if (hitresult$type == HitResult.Type.BLOCK) {
         BlockHitResult blockhitresult = (BlockHitResult)p_37260_;
         this.onHitBlock(blockhitresult);
         BlockPos blockpos = blockhitresult.getBlockPos();
         this.level().gameEvent(GameEvent.PROJECTILE_LAND, blockpos, GameEvent.Context.of(this, this.level().getBlockState(blockpos)));
      }
   }

   protected void onHitEntity(EntityHitResult p_37259_) {
   }

   protected void onHitBlock(BlockHitResult p_37258_) {
      BlockState blockstate = this.level().getBlockState(p_37258_.getBlockPos());
      blockstate.onProjectileHit(this.level(), blockstate, p_37258_, this);
   }

   protected boolean canHitEntity(Entity p_37250_) {
      if (!p_37250_.canBeHitByProjectile()) {
         return false;
      }

      Entity entity = this.getOwner();
      return entity == null || this.leftOwner || !entity.isPassengerOfSameVehicle(p_37250_);
   }

   protected void updateRotation() {
      Vec3 vec3 = this.getDeltaMovement();
      double d0 = vec3.horizontalDistance();
      this.setXRot(lerpRotation(this.xRotO, (float)(Mth.atan2(vec3.y, d0) * 180.0 / (float) Math.PI)));
      this.setYRot(lerpRotation(this.yRotO, (float)(Mth.atan2(vec3.x, vec3.z) * 180.0 / (float) Math.PI)));
   }

   protected static float lerpRotation(float p_37274_, float p_37275_) {
      while (p_37275_ - p_37274_ < -180.0F) {
         p_37274_ -= 360.0F;
      }

      while (p_37275_ - p_37274_ >= 180.0F) {
         p_37274_ += 360.0F;
      }

      return Mth.lerp(0.2F, p_37274_, p_37275_);
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity p_345233_) {
      Entity entity = this.getOwner();
      return new ClientboundAddEntityPacket(this, p_345233_, entity == null ? 0 : entity.getId());
   }

   @Override
   public void recreateFromPacket(ClientboundAddEntityPacket p_150170_) {
      super.recreateFromPacket(p_150170_);
      Entity entity = this.level().getEntity(p_150170_.getData());
      if (entity != null) {
         this.setOwner(entity);
      }
   }

   @Override
   public boolean mayInteract(ServerLevel p_364907_, BlockPos p_150168_) {
      Entity entity = this.getOwner();
      return entity instanceof Player ? entity.mayInteract(p_364907_, p_150168_) : entity == null || p_364907_.getGameRules().get(GameRules.MOB_GRIEFING);
   }

   public boolean mayBreak(ServerLevel p_361134_) {
      return this.getType().is(EntityTypeTags.IMPACT_PROJECTILES) && p_361134_.getGameRules().get(GameRules.PROJECTILES_CAN_BREAK_BLOCKS);
   }

   @Override
   public boolean isPickable() {
      return this.getType().is(EntityTypeTags.REDIRECTABLE_PROJECTILE);
   }

   @Override
   public float getPickRadius() {
      return this.isPickable() ? 1.0F : 0.0F;
   }

   public DoubleDoubleImmutablePair calculateHorizontalHurtKnockbackDirection(LivingEntity p_343703_, DamageSource p_343506_) {
      double d0 = this.getDeltaMovement().x;
      double d1 = this.getDeltaMovement().z;
      return DoubleDoubleImmutablePair.of(d0, d1);
   }

   @Override
   public int getDimensionChangingDelay() {
      return 2;
   }

   @Override
   public boolean hurtServer(ServerLevel p_367356_, DamageSource p_368526_, float p_366624_) {
      if (!this.isInvulnerableToBase(p_368526_)) {
         this.markHurt();
      }

      return false;
   }

   @FunctionalInterface
   public interface ProjectileFactory<T extends Projectile> {
      T create(ServerLevel var1, LivingEntity var2, ItemStack var3);
   }
}
