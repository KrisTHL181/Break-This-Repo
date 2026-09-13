package net.minecraft.world.entity.monster;

import java.util.Collection;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.jspecify.annotations.Nullable;

public class Creeper extends Monster {
   private static final EntityDataAccessor<Integer> DATA_SWELL_DIR = SynchedEntityData.defineId(Creeper.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DATA_IS_POWERED = SynchedEntityData.defineId(Creeper.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_IS_IGNITED = SynchedEntityData.defineId(Creeper.class, EntityDataSerializers.BOOLEAN);
   private static final boolean DEFAULT_IGNITED = false;
   private static final boolean DEFAULT_POWERED = false;
   private static final short DEFAULT_MAX_SWELL = 30;
   private static final byte DEFAULT_EXPLOSION_RADIUS = 3;
   private int oldSwell;
   private int swell;
   private int maxSwell = 30;
   private int explosionRadius = 3;
   private boolean droppedSkulls;

   public Creeper(EntityType<? extends Creeper> p_32278_, Level p_32279_) {
      super(p_32278_, p_32279_);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(1, new FloatGoal(this));
      this.goalSelector.addGoal(2, new SwellGoal(this));
      this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Ocelot.class, 6.0F, 1.0, 1.2));
      this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Cat.class, 6.0F, 1.0, 1.2));
      this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, false));
      this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
      this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MOVEMENT_SPEED, 0.25);
   }

   @Override
   public int getMaxFallDistance() {
      return this.getTarget() == null ? this.getComfortableFallDistance(0.0F) : this.getComfortableFallDistance(this.getHealth() - 1.0F);
   }

   @Override
   public boolean causeFallDamage(double p_396975_, float p_149687_, DamageSource p_149689_) {
      boolean flag = super.causeFallDamage(p_396975_, p_149687_, p_149689_);
      this.swell += (int)(p_396975_ * 1.5);
      if (this.swell > this.maxSwell - 5) {
         this.swell = this.maxSwell - 5;
      }

      return flag;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_330760_) {
      super.defineSynchedData(p_330760_);
      p_330760_.define(DATA_SWELL_DIR, -1);
      p_330760_.define(DATA_IS_POWERED, false);
      p_330760_.define(DATA_IS_IGNITED, false);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_408905_) {
      super.addAdditionalSaveData(p_408905_);
      p_408905_.putBoolean("powered", this.isPowered());
      p_408905_.putShort("Fuse", (short)this.maxSwell);
      p_408905_.putByte("ExplosionRadius", (byte)this.explosionRadius);
      p_408905_.putBoolean("ignited", this.isIgnited());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_408546_) {
      super.readAdditionalSaveData(p_408546_);
      this.entityData.set(DATA_IS_POWERED, p_408546_.getBooleanOr("powered", false));
      this.maxSwell = p_408546_.getShortOr("Fuse", (short)30);
      this.explosionRadius = p_408546_.getByteOr("ExplosionRadius", (byte)3);
      if (p_408546_.getBooleanOr("ignited", false)) {
         this.ignite();
      }
   }

   @Override
   public void tick() {
      if (this.isAlive()) {
         this.oldSwell = this.swell;
         if (this.isIgnited()) {
            this.setSwellDir(1);
         }

         int i = this.getSwellDir();
         if (i > 0 && this.swell == 0) {
            this.playSound(SoundEvents.CREEPER_PRIMED, 1.0F, 0.5F);
            this.gameEvent(GameEvent.PRIME_FUSE);
         }

         this.swell += i;
         if (this.swell < 0) {
            this.swell = 0;
         }

         if (this.swell >= this.maxSwell) {
            this.swell = this.maxSwell;
            this.explodeCreeper();
         }
      }

      super.tick();
   }

   @Override
   public void setTarget(@Nullable LivingEntity p_149691_) {
      if (!(p_149691_ instanceof Goat)) {
         super.setTarget(p_149691_);
      }
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_32309_) {
      return SoundEvents.CREEPER_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.CREEPER_DEATH;
   }

   @Override
   public boolean killedEntity(ServerLevel p_430136_, LivingEntity p_430268_, DamageSource p_423041_) {
      if (this.shouldDropLoot(p_430136_) && this.isPowered() && !this.droppedSkulls) {
         p_430268_.dropFromLootTable(p_430136_, p_423041_, false, BuiltInLootTables.CHARGED_CREEPER, p_423278_ -> {
            p_430268_.spawnAtLocation(p_430136_, p_423278_);
            this.droppedSkulls = true;
         });
      }

      return super.killedEntity(p_430136_, p_430268_, p_423041_);
   }

   @Override
   public boolean doHurtTarget(ServerLevel p_365514_, Entity p_32281_) {
      return true;
   }

   public boolean isPowered() {
      return this.entityData.get(DATA_IS_POWERED);
   }

   public float getSwelling(float p_32321_) {
      return Mth.lerp(p_32321_, this.oldSwell, this.swell) / (this.maxSwell - 2);
   }

   public int getSwellDir() {
      return this.entityData.get(DATA_SWELL_DIR);
   }

   public void setSwellDir(int p_32284_) {
      this.entityData.set(DATA_SWELL_DIR, p_32284_);
   }

   @Override
   public void thunderHit(ServerLevel p_32286_, LightningBolt p_32287_) {
      super.thunderHit(p_32286_, p_32287_);
      this.entityData.set(DATA_IS_POWERED, true);
   }

   @Override
   protected InteractionResult mobInteract(Player p_32301_, InteractionHand p_32302_) {
      ItemStack itemstack = p_32301_.getItemInHand(p_32302_);
      if (itemstack.is(ItemTags.CREEPER_IGNITERS)) {
         SoundEvent soundevent = itemstack.is(Items.FIRE_CHARGE) ? SoundEvents.FIRECHARGE_USE : SoundEvents.FLINTANDSTEEL_USE;
         this.level()
            .playSound(p_32301_, this.getX(), this.getY(), this.getZ(), soundevent, this.getSoundSource(), 1.0F, this.random.nextFloat() * 0.4F + 0.8F);
         if (!this.level().isClientSide()) {
            this.ignite();
            if (!itemstack.isDamageableItem()) {
               itemstack.shrink(1);
            } else {
               itemstack.hurtAndBreak(1, p_32301_, p_32302_.asEquipmentSlot());
            }
         }

         return InteractionResult.SUCCESS;
      } else {
         return super.mobInteract(p_32301_, p_32302_);
      }
   }

   private void explodeCreeper() {
      if (this.level() instanceof ServerLevel serverlevel) {
         float f = this.isPowered() ? 2.0F : 1.0F;
         this.dead = true;
         serverlevel.explode(this, this.getX(), this.getY(), this.getZ(), this.explosionRadius * f, Level.ExplosionInteraction.MOB);
         this.spawnLingeringCloud();
         this.triggerOnDeathMobEffects(serverlevel, Entity.RemovalReason.KILLED);
         this.discard();
      }
   }

   private void spawnLingeringCloud() {
      Collection<MobEffectInstance> collection = this.getActiveEffects();
      if (!collection.isEmpty()) {
         AreaEffectCloud areaeffectcloud = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());
         areaeffectcloud.setRadius(2.5F);
         areaeffectcloud.setRadiusOnUse(-0.5F);
         areaeffectcloud.setWaitTime(10);
         areaeffectcloud.setDuration(300);
         areaeffectcloud.setPotionDurationScale(0.25F);
         areaeffectcloud.setRadiusPerTick(-areaeffectcloud.getRadius() / areaeffectcloud.getDuration());

         for (MobEffectInstance mobeffectinstance : collection) {
            areaeffectcloud.addEffect(new MobEffectInstance(mobeffectinstance));
         }

         this.level().addFreshEntity(areaeffectcloud);
      }
   }

   public boolean isIgnited() {
      return this.entityData.get(DATA_IS_IGNITED);
   }

   public void ignite() {
      this.entityData.set(DATA_IS_IGNITED, true);
   }
}
