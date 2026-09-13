package net.minecraft.world.entity.projectile.arrow;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.OminousItemSpawner;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class AbstractArrow extends Projectile {
   private static final double ARROW_BASE_DAMAGE = 2.0;
   private static final int SHAKE_TIME = 7;
   private static final float WATER_INERTIA = 0.6F;
   private static final float INERTIA = 0.99F;
   private static final short DEFAULT_LIFE = 0;
   private static final byte DEFAULT_SHAKE = 0;
   private static final boolean DEFAULT_IN_GROUND = false;
   private static final boolean DEFAULT_CRIT = false;
   private static final byte DEFAULT_PIERCE_LEVEL = 0;
   private static final EntityDataAccessor<Byte> ID_FLAGS = SynchedEntityData.defineId(AbstractArrow.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Byte> PIERCE_LEVEL = SynchedEntityData.defineId(AbstractArrow.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Boolean> IN_GROUND = SynchedEntityData.defineId(AbstractArrow.class, EntityDataSerializers.BOOLEAN);
   private static final int FLAG_CRIT = 1;
   private static final int FLAG_NOPHYSICS = 2;
   private @Nullable BlockState lastState;
   protected int inGroundTime;
   public AbstractArrow.Pickup pickup = AbstractArrow.Pickup.DISALLOWED;
   public int shakeTime = 0;
   private int life = 0;
   private double baseDamage = 2.0;
   private SoundEvent soundEvent = this.getDefaultHitGroundSoundEvent();
   private @Nullable IntOpenHashSet piercingIgnoreEntityIds;
   private @Nullable List<Entity> piercedAndKilledEntities;
   private ItemStack pickupItemStack = this.getDefaultPickupItem();
   private @Nullable ItemStack firedFromWeapon = null;

   protected AbstractArrow(EntityType<? extends AbstractArrow> p_450750_, Level p_450850_) {
      super(p_450750_, p_450850_);
   }

   protected AbstractArrow(
      EntityType<? extends AbstractArrow> p_454328_,
      double p_451448_,
      double p_454253_,
      double p_457339_,
      Level p_450792_,
      ItemStack p_456021_,
      @Nullable ItemStack p_455495_
   ) {
      this(p_454328_, p_450792_);
      this.pickupItemStack = p_456021_.copy();
      this.applyComponentsFromItemStack(p_456021_);
      Unit unit = p_456021_.remove(DataComponents.INTANGIBLE_PROJECTILE);
      if (unit != null) {
         this.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
      }

      this.setPos(p_451448_, p_454253_, p_457339_);
      if (p_455495_ != null && p_450792_ instanceof ServerLevel serverlevel) {
         if (p_455495_.isEmpty()) {
            throw new IllegalArgumentException("Invalid weapon firing an arrow");
         }

         this.firedFromWeapon = p_455495_.copy();
         int i = EnchantmentHelper.getPiercingCount(serverlevel, p_455495_, this.pickupItemStack);
         if (i > 0) {
            this.setPierceLevel((byte)i);
         }
      }
   }

   protected AbstractArrow(
      EntityType<? extends AbstractArrow> p_455307_, LivingEntity p_452664_, Level p_459444_, ItemStack p_450891_, @Nullable ItemStack p_456788_
   ) {
      this(p_455307_, p_452664_.getX(), p_452664_.getEyeY() - 0.1F, p_452664_.getZ(), p_459444_, p_450891_, p_456788_);
      this.setOwner(p_452664_);
   }

   public void setSoundEvent(SoundEvent p_453944_) {
      this.soundEvent = p_453944_;
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double p_452120_) {
      double d0 = this.getBoundingBox().getSize() * 10.0;
      if (Double.isNaN(d0)) {
         d0 = 1.0;
      }

      d0 *= 64.0 * getViewScale();
      return p_452120_ < d0 * d0;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_460288_) {
      p_460288_.define(ID_FLAGS, (byte)0);
      p_460288_.define(PIERCE_LEVEL, (byte)0);
      p_460288_.define(IN_GROUND, false);
   }

   @Override
   public void shoot(double p_460227_, double p_460180_, double p_460371_, float p_457167_, float p_455353_) {
      super.shoot(p_460227_, p_460180_, p_460371_, p_457167_, p_455353_);
      this.life = 0;
   }

   @Override
   public void lerpMotion(Vec3 p_460735_) {
      super.lerpMotion(p_460735_);
      this.life = 0;
      if (this.isInGround() && p_460735_.lengthSqr() > 0.0) {
         this.setInGround(false);
      }
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> p_460586_) {
      super.onSyncedDataUpdated(p_460586_);
      if (!this.firstTick && this.shakeTime <= 0 && p_460586_.equals(IN_GROUND) && this.isInGround()) {
         this.shakeTime = 7;
      }
   }

   @Override
   public void tick() {
      boolean flag = !this.isNoPhysics();
      Vec3 vec3 = this.getDeltaMovement();
      BlockPos blockpos = this.blockPosition();
      BlockState blockstate = this.level().getBlockState(blockpos);
      if (!blockstate.isAir() && flag) {
         VoxelShape voxelshape = blockstate.getCollisionShape(this.level(), blockpos);
         if (!voxelshape.isEmpty()) {
            Vec3 vec31 = this.position();

            for (AABB aabb : voxelshape.toAabbs()) {
               if (aabb.move(blockpos).contains(vec31)) {
                  this.setDeltaMovement(Vec3.ZERO);
                  this.setInGround(true);
                  break;
               }
            }
         }
      }

      if (this.shakeTime > 0) {
         this.shakeTime--;
      }

      if (this.isInWaterOrRain()) {
         this.clearFire();
      }

      if (this.isInGround() && flag) {
         if (!this.level().isClientSide()) {
            if (this.lastState != blockstate && this.shouldFall()) {
               this.startFalling();
            } else {
               this.tickDespawn();
            }
         }

         this.inGroundTime++;
         if (this.isAlive()) {
            this.applyEffectsFromBlocks();
         }

         if (!this.level().isClientSide()) {
            this.setSharedFlagOnFire(this.getRemainingFireTicks() > 0);
         }
      } else {
         this.inGroundTime = 0;
         Vec3 vec32 = this.position();
         if (this.isInWater()) {
            this.applyInertia(this.getWaterInertia());
            this.addBubbleParticles(vec32);
         }

         if (this.isCritArrow()) {
            for (int i = 0; i < 4; i++) {
               this.level()
                  .addParticle(
                     ParticleTypes.CRIT, vec32.x + vec3.x * i / 4.0, vec32.y + vec3.y * i / 4.0, vec32.z + vec3.z * i / 4.0, -vec3.x, -vec3.y + 0.2, -vec3.z
                  );
            }
         }

         float f;
         if (!flag) {
            f = (float)(Mth.atan2(-vec3.x, -vec3.z) * 180.0 / (float) Math.PI);
         } else {
            f = (float)(Mth.atan2(vec3.x, vec3.z) * 180.0 / (float) Math.PI);
         }

         float f1 = (float)(Mth.atan2(vec3.y, vec3.horizontalDistance()) * 180.0 / (float) Math.PI);
         this.setXRot(lerpRotation(this.getXRot(), f1));
         this.setYRot(lerpRotation(this.getYRot(), f));
         this.checkLeftOwner();
         if (flag) {
            BlockHitResult blockhitresult = this.level()
               .clipIncludingBorder(new ClipContext(vec32, vec32.add(vec3), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            this.stepMoveAndHit(blockhitresult);
         } else {
            this.setPos(vec32.add(vec3));
            this.applyEffectsFromBlocks();
         }

         if (!this.isInWater()) {
            this.applyInertia(0.99F);
         }

         if (flag && !this.isInGround()) {
            this.applyGravity();
         }

         super.tick();
      }
   }

   private void stepMoveAndHit(BlockHitResult p_455771_) {
      while (this.isAlive()) {
         Vec3 vec3 = this.position();
         ArrayList<EntityHitResult> arraylist = new ArrayList<>(this.findHitEntities(vec3, p_455771_.getLocation()));
         arraylist.sort(Comparator.comparingDouble(p_451887_ -> vec3.distanceToSqr(p_451887_.getEntity().position())));
         EntityHitResult entityhitresult = arraylist.isEmpty() ? null : arraylist.getFirst();
         Vec3 vec31 = Objects.requireNonNullElse(entityhitresult, p_455771_).getLocation();
         this.setPos(vec31);
         this.applyEffectsFromBlocks(vec3, vec31);
         if (this.portalProcess != null && this.portalProcess.isInsidePortalThisTick()) {
            this.handlePortal();
         }

         if (arraylist.isEmpty()) {
            if (this.isAlive() && p_455771_.getType() != HitResult.Type.MISS) {
               this.hitTargetOrDeflectSelf(p_455771_);
               this.needsSync = true;
            }
            break;
         } else if (this.isAlive() && !this.noPhysics) {
            ProjectileDeflection projectiledeflection = this.hitTargetsOrDeflectSelf(arraylist);
            this.needsSync = true;
            if (this.getPierceLevel() > 0 && projectiledeflection == ProjectileDeflection.NONE) {
               continue;
            }
            break;
         }
      }
   }

   private ProjectileDeflection hitTargetsOrDeflectSelf(Collection<EntityHitResult> p_452868_) {
      for (EntityHitResult entityhitresult : p_452868_) {
         ProjectileDeflection projectiledeflection = this.hitTargetOrDeflectSelf(entityhitresult);
         if (!this.isAlive() || projectiledeflection != ProjectileDeflection.NONE) {
            return projectiledeflection;
         }
      }

      return ProjectileDeflection.NONE;
   }

   private void applyInertia(float p_459357_) {
      Vec3 vec3 = this.getDeltaMovement();
      this.setDeltaMovement(vec3.scale(p_459357_));
   }

   private void addBubbleParticles(Vec3 p_451828_) {
      Vec3 vec3 = this.getDeltaMovement();

      for (int i = 0; i < 4; i++) {
         float f = 0.25F;
         this.level()
            .addParticle(ParticleTypes.BUBBLE, p_451828_.x - vec3.x * 0.25, p_451828_.y - vec3.y * 0.25, p_451828_.z - vec3.z * 0.25, vec3.x, vec3.y, vec3.z);
      }
   }

   @Override
   protected double getDefaultGravity() {
      return 0.05;
   }

   private boolean shouldFall() {
      return this.isInGround() && this.level().noCollision(new AABB(this.position(), this.position()).inflate(0.06));
   }

   private void startFalling() {
      this.setInGround(false);
      Vec3 vec3 = this.getDeltaMovement();
      this.setDeltaMovement(vec3.multiply(this.random.nextFloat() * 0.2F, this.random.nextFloat() * 0.2F, this.random.nextFloat() * 0.2F));
      this.life = 0;
   }

   protected boolean isInGround() {
      return this.entityData.get(IN_GROUND);
   }

   protected void setInGround(boolean p_455813_) {
      this.entityData.set(IN_GROUND, p_455813_);
   }

   @Override
   public boolean isPushedByFluid() {
      return !this.isInGround();
   }

   @Override
   public void move(MoverType p_457861_, Vec3 p_459918_) {
      super.move(p_457861_, p_459918_);
      if (p_457861_ != MoverType.SELF && this.shouldFall()) {
         this.startFalling();
      }
   }

   protected void tickDespawn() {
      this.life++;
      if (this.life >= 1200) {
         this.discard();
      }
   }

   private void resetPiercedEntities() {
      if (this.piercedAndKilledEntities != null) {
         this.piercedAndKilledEntities.clear();
      }

      if (this.piercingIgnoreEntityIds != null) {
         this.piercingIgnoreEntityIds.clear();
      }
   }

   @Override
   public void onItemBreak(Item p_459092_) {
      this.firedFromWeapon = null;
   }

   @Override
   public void onAboveBubbleColumn(boolean p_457070_, BlockPos p_456080_) {
      if (!this.isInGround()) {
         super.onAboveBubbleColumn(p_457070_, p_456080_);
      }
   }

   @Override
   public void onInsideBubbleColumn(boolean p_450152_) {
      if (!this.isInGround()) {
         super.onInsideBubbleColumn(p_450152_);
      }
   }

   @Override
   public void push(double p_460266_, double p_450202_, double p_451620_) {
      if (!this.isInGround()) {
         super.push(p_460266_, p_450202_, p_451620_);
      }
   }

   @Override
   protected void onHitEntity(EntityHitResult p_460593_) {
      super.onHitEntity(p_460593_);
      Entity entity = p_460593_.getEntity();
      float f = (float)this.getDeltaMovement().length();
      double d0 = this.baseDamage;
      Entity entity1 = this.getOwner();
      DamageSource damagesource = this.damageSources().arrow(this, entity1 != null ? entity1 : this);
      if (this.getWeaponItem() != null && this.level() instanceof ServerLevel serverlevel) {
         d0 = EnchantmentHelper.modifyDamage(serverlevel, this.getWeaponItem(), entity, damagesource, (float)d0);
      }

      int j = Mth.ceil(Mth.clamp(f * d0, 0.0, 2.147483647E9));
      if (this.getPierceLevel() > 0) {
         if (this.piercingIgnoreEntityIds == null) {
            this.piercingIgnoreEntityIds = new IntOpenHashSet(5);
         }

         if (this.piercedAndKilledEntities == null) {
            this.piercedAndKilledEntities = Lists.newArrayListWithCapacity(5);
         }

         if (this.piercingIgnoreEntityIds.size() >= this.getPierceLevel() + 1) {
            this.discard();
            return;
         }

         this.piercingIgnoreEntityIds.add(entity.getId());
      }

      if (this.isCritArrow()) {
         long k = this.random.nextInt(j / 2 + 2);
         j = (int)Math.min(k + j, 2147483647L);
      }

      if (entity1 instanceof LivingEntity livingentity1) {
         livingentity1.setLastHurtMob(entity);
      }

      boolean flag = entity.getType() == EntityType.ENDERMAN;
      int i = entity.getRemainingFireTicks();
      if (this.isOnFire() && !flag) {
         entity.igniteForSeconds(5.0F);
      }

      if (entity.hurtOrSimulate(damagesource, j)) {
         if (flag) {
            return;
         }

         if (entity instanceof LivingEntity livingentity) {
            if (!this.level().isClientSide() && this.getPierceLevel() <= 0) {
               livingentity.setArrowCount(livingentity.getArrowCount() + 1);
            }

            this.doKnockback(livingentity, damagesource);
            if (this.level() instanceof ServerLevel serverlevel1) {
               EnchantmentHelper.doPostAttackEffectsWithItemSource(serverlevel1, livingentity, damagesource, this.getWeaponItem());
            }

            this.doPostHurtEffects(livingentity);
            if (livingentity instanceof Player && entity1 instanceof ServerPlayer serverplayer && !this.isSilent() && livingentity != serverplayer) {
               serverplayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.PLAY_ARROW_HIT_SOUND, 0.0F));
            }

            if (!entity.isAlive() && this.piercedAndKilledEntities != null) {
               this.piercedAndKilledEntities.add(livingentity);
            }

            if (!this.level().isClientSide() && entity1 instanceof ServerPlayer serverplayer1) {
               if (this.piercedAndKilledEntities != null) {
                  CriteriaTriggers.KILLED_BY_ARROW.trigger(serverplayer1, this.piercedAndKilledEntities, this.firedFromWeapon);
               } else if (!entity.isAlive()) {
                  CriteriaTriggers.KILLED_BY_ARROW.trigger(serverplayer1, List.of(entity), this.firedFromWeapon);
               }
            }
         }

         this.playSound(this.soundEvent, 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
         if (this.getPierceLevel() <= 0) {
            this.discard();
         }
      } else {
         entity.setRemainingFireTicks(i);
         this.deflect(ProjectileDeflection.REVERSE, entity, this.owner, false);
         this.setDeltaMovement(this.getDeltaMovement().scale(0.2));
         if (this.level() instanceof ServerLevel serverlevel2 && this.getDeltaMovement().lengthSqr() < 1.0E-7) {
            if (this.pickup == AbstractArrow.Pickup.ALLOWED) {
               this.spawnAtLocation(serverlevel2, this.getPickupItem(), 0.1F);
            }

            this.discard();
         }
      }
   }

   protected void doKnockback(LivingEntity p_458476_, DamageSource p_457144_) {
      double d0 = this.firedFromWeapon != null && this.level() instanceof ServerLevel serverlevel
         ? EnchantmentHelper.modifyKnockback(serverlevel, this.firedFromWeapon, p_458476_, p_457144_, 0.0F)
         : 0.0F;
      if (d0 > 0.0) {
         double d1 = Math.max(0.0, 1.0 - p_458476_.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
         Vec3 vec3 = this.getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize().scale(d0 * 0.6 * d1);
         if (vec3.lengthSqr() > 0.0) {
            p_458476_.push(vec3.x, 0.1, vec3.z);
         }
      }
   }

   @Override
   protected void onHitBlock(BlockHitResult p_457257_) {
      this.lastState = this.level().getBlockState(p_457257_.getBlockPos());
      super.onHitBlock(p_457257_);
      ItemStack itemstack = this.getWeaponItem();
      if (this.level() instanceof ServerLevel serverlevel && itemstack != null) {
         this.hitBlockEnchantmentEffects(serverlevel, p_457257_, itemstack);
      }

      Vec3 vec31 = this.getDeltaMovement();
      Vec3 vec32 = new Vec3(Math.signum(vec31.x), Math.signum(vec31.y), Math.signum(vec31.z));
      Vec3 vec3 = vec32.scale(0.05F);
      this.setPos(this.position().subtract(vec3));
      this.setDeltaMovement(Vec3.ZERO);
      this.playSound(this.getHitGroundSoundEvent(), 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
      this.setInGround(true);
      this.shakeTime = 7;
      this.setCritArrow(false);
      this.setPierceLevel((byte)0);
      this.setSoundEvent(SoundEvents.ARROW_HIT);
      this.resetPiercedEntities();
   }

   protected void hitBlockEnchantmentEffects(ServerLevel p_452968_, BlockHitResult p_451398_, ItemStack p_451419_) {
      Vec3 vec3 = p_451398_.getBlockPos().clampLocationWithin(p_451398_.getLocation());
      EnchantmentHelper.onHitBlock(
         p_452968_,
         p_451419_,
         this.getOwner() instanceof LivingEntity livingentity ? livingentity : null,
         this,
         null,
         vec3,
         p_452968_.getBlockState(p_451398_.getBlockPos()),
         p_459675_ -> this.firedFromWeapon = null
      );
   }

   @Override
   public @Nullable ItemStack getWeaponItem() {
      return this.firedFromWeapon;
   }

   protected SoundEvent getDefaultHitGroundSoundEvent() {
      return SoundEvents.ARROW_HIT;
   }

   protected final SoundEvent getHitGroundSoundEvent() {
      return this.soundEvent;
   }

   protected void doPostHurtEffects(LivingEntity p_453628_) {
   }

   protected @Nullable EntityHitResult findHitEntity(Vec3 p_457196_, Vec3 p_459960_) {
      return ProjectileUtil.getEntityHitResult(
         this.level(), this, p_457196_, p_459960_, this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0), this::canHitEntity
      );
   }

   protected Collection<EntityHitResult> findHitEntities(Vec3 p_458868_, Vec3 p_458662_) {
      return ProjectileUtil.getManyEntityHitResult(
         this.level(), this, p_458868_, p_458662_, this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0), this::canHitEntity, false
      );
   }

   @Override
   protected boolean canHitEntity(Entity p_454713_) {
      return p_454713_ instanceof Player && this.getOwner() instanceof Player player && !player.canHarmPlayer((Player)p_454713_)
         ? false
         : super.canHitEntity(p_454713_) && (this.piercingIgnoreEntityIds == null || !this.piercingIgnoreEntityIds.contains(p_454713_.getId()));
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_459438_) {
      super.addAdditionalSaveData(p_459438_);
      p_459438_.putShort("life", (short)this.life);
      p_459438_.storeNullable("inBlockState", BlockState.CODEC, this.lastState);
      p_459438_.putByte("shake", (byte)this.shakeTime);
      p_459438_.putBoolean("inGround", this.isInGround());
      p_459438_.store("pickup", AbstractArrow.Pickup.LEGACY_CODEC, this.pickup);
      p_459438_.putDouble("damage", this.baseDamage);
      p_459438_.putBoolean("crit", this.isCritArrow());
      p_459438_.putByte("PierceLevel", this.getPierceLevel());
      p_459438_.store("SoundEvent", BuiltInRegistries.SOUND_EVENT.byNameCodec(), this.soundEvent);
      p_459438_.store("item", ItemStack.CODEC, this.pickupItemStack);
      p_459438_.storeNullable("weapon", ItemStack.CODEC, this.firedFromWeapon);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_451310_) {
      super.readAdditionalSaveData(p_451310_);
      this.life = p_451310_.getShortOr("life", (short)0);
      this.lastState = p_451310_.<BlockState>read("inBlockState", BlockState.CODEC).orElse(null);
      this.shakeTime = p_451310_.getByteOr("shake", (byte)0) & 255;
      this.setInGround(p_451310_.getBooleanOr("inGround", false));
      this.baseDamage = p_451310_.getDoubleOr("damage", 2.0);
      this.pickup = p_451310_.<AbstractArrow.Pickup>read("pickup", AbstractArrow.Pickup.LEGACY_CODEC).orElse(AbstractArrow.Pickup.DISALLOWED);
      this.setCritArrow(p_451310_.getBooleanOr("crit", false));
      this.setPierceLevel(p_451310_.getByteOr("PierceLevel", (byte)0));
      this.soundEvent = p_451310_.<SoundEvent>read("SoundEvent", BuiltInRegistries.SOUND_EVENT.byNameCodec()).orElse(this.getDefaultHitGroundSoundEvent());
      this.setPickupItemStack(p_451310_.<ItemStack>read("item", ItemStack.CODEC).orElse(this.getDefaultPickupItem()));
      this.firedFromWeapon = p_451310_.<ItemStack>read("weapon", ItemStack.CODEC).orElse(null);
   }

   @Override
   public void setOwner(@Nullable Entity p_453693_) {
      super.setOwner(p_453693_);

      this.pickup = switch (p_453693_) {
         case Player player when this.pickup == AbstractArrow.Pickup.DISALLOWED -> AbstractArrow.Pickup.ALLOWED;
         case OminousItemSpawner ominousitemspawner -> AbstractArrow.Pickup.DISALLOWED;
         case null, default -> this.pickup;
      };
   }

   @Override
   public void playerTouch(Player p_455357_) {
      if (!this.level().isClientSide() && (this.isInGround() || this.isNoPhysics()) && this.shakeTime <= 0 && this.tryPickup(p_455357_)) {
         p_455357_.take(this, 1);
         this.discard();
      }
   }

   protected boolean tryPickup(Player p_459000_) {
      return switch (this.pickup) {
         case DISALLOWED -> false;
         case ALLOWED -> p_459000_.getInventory().add(this.getPickupItem());
         case CREATIVE_ONLY -> p_459000_.hasInfiniteMaterials();
      };
   }

   protected ItemStack getPickupItem() {
      return this.pickupItemStack.copy();
   }

   protected abstract ItemStack getDefaultPickupItem();

   @Override
   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.NONE;
   }

   public ItemStack getPickupItemStackOrigin() {
      return this.pickupItemStack;
   }

   public void setBaseDamage(double p_457883_) {
      this.baseDamage = p_457883_;
   }

   @Override
   public boolean isAttackable() {
      return this.getType().is(EntityTypeTags.REDIRECTABLE_PROJECTILE);
   }

   public void setCritArrow(boolean p_451209_) {
      this.setFlag(1, p_451209_);
   }

   private void setPierceLevel(byte p_459301_) {
      this.entityData.set(PIERCE_LEVEL, p_459301_);
   }

   private void setFlag(int p_457802_, boolean p_460218_) {
      byte b0 = this.entityData.get(ID_FLAGS);
      if (p_460218_) {
         this.entityData.set(ID_FLAGS, (byte)(b0 | p_457802_));
      } else {
         this.entityData.set(ID_FLAGS, (byte)(b0 & ~p_457802_));
      }
   }

   protected void setPickupItemStack(ItemStack p_456102_) {
      if (!p_456102_.isEmpty()) {
         this.pickupItemStack = p_456102_;
      } else {
         this.pickupItemStack = this.getDefaultPickupItem();
      }
   }

   public boolean isCritArrow() {
      byte b0 = this.entityData.get(ID_FLAGS);
      return (b0 & 1) != 0;
   }

   public byte getPierceLevel() {
      return this.entityData.get(PIERCE_LEVEL);
   }

   public void setBaseDamageFromMob(float p_457546_) {
      this.setBaseDamage(p_457546_ * 2.0F + this.random.triangle(this.level().getDifficulty().getId() * 0.11, 0.57425));
   }

   protected float getWaterInertia() {
      return 0.6F;
   }

   public void setNoPhysics(boolean p_456576_) {
      this.noPhysics = p_456576_;
      this.setFlag(2, p_456576_);
   }

   public boolean isNoPhysics() {
      return !this.level().isClientSide() ? this.noPhysics : (this.entityData.get(ID_FLAGS) & 2) != 0;
   }

   @Override
   public boolean isPickable() {
      return super.isPickable() && !this.isInGround();
   }

   @Override
   public @Nullable SlotAccess getSlot(int p_458253_) {
      return p_458253_ == 0 ? SlotAccess.of(this::getPickupItemStackOrigin, this::setPickupItemStack) : super.getSlot(p_458253_);
   }

   @Override
   protected boolean shouldBounceOnWorldBorder() {
      return true;
   }

   public enum Pickup {
      DISALLOWED,
      ALLOWED,
      CREATIVE_ONLY;

      public static final Codec<AbstractArrow.Pickup> LEGACY_CODEC = Codec.BYTE.xmap(AbstractArrow.Pickup::byOrdinal, p_453045_ -> (byte)p_453045_.ordinal());

      public static AbstractArrow.Pickup byOrdinal(int p_459536_) {
         if (p_459536_ < 0 || p_459536_ > values().length) {
            p_459536_ = 0;
         }

         return values()[p_459536_];
      }
   }
}
