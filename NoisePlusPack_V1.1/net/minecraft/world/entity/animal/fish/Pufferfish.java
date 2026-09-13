package net.minecraft.world.entity.animal.fish;

import java.util.List;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class Pufferfish extends AbstractFish {
   private static final EntityDataAccessor<Integer> PUFF_STATE = SynchedEntityData.defineId(Pufferfish.class, EntityDataSerializers.INT);
   int inflateCounter;
   int deflateTimer;
   private static final TargetingConditions.Selector SCARY_MOB = (p_450517_, p_451541_) -> p_450517_ instanceof Player player && player.isCreative()
      ? false
      : !p_450517_.getType().is(EntityTypeTags.NOT_SCARY_FOR_PUFFERFISH);
   static final TargetingConditions TARGETING_CONDITIONS = TargetingConditions.forNonCombat()
      .ignoreInvisibilityTesting()
      .ignoreLineOfSight()
      .selector(SCARY_MOB);
   public static final int STATE_SMALL = 0;
   public static final int STATE_MID = 1;
   public static final int STATE_FULL = 2;
   private static final int DEFAULT_PUFF_STATE = 0;

   public Pufferfish(EntityType<? extends Pufferfish> p_457444_, Level p_454316_) {
      super(p_457444_, p_454316_);
      this.refreshDimensions();
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_452970_) {
      super.defineSynchedData(p_452970_);
      p_452970_.define(PUFF_STATE, 0);
   }

   public int getPuffState() {
      return this.entityData.get(PUFF_STATE);
   }

   public void setPuffState(int p_458026_) {
      this.entityData.set(PUFF_STATE, p_458026_);
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> p_455083_) {
      if (PUFF_STATE.equals(p_455083_)) {
         this.refreshDimensions();
      }

      super.onSyncedDataUpdated(p_455083_);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_454934_) {
      super.addAdditionalSaveData(p_454934_);
      p_454934_.putInt("PuffState", this.getPuffState());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_460855_) {
      super.readAdditionalSaveData(p_460855_);
      this.setPuffState(Math.min(p_460855_.getIntOr("PuffState", 0), 2));
   }

   @Override
   public ItemStack getBucketItemStack() {
      return new ItemStack(Items.PUFFERFISH_BUCKET);
   }

   @Override
   protected void registerGoals() {
      super.registerGoals();
      this.goalSelector.addGoal(1, new Pufferfish.PufferfishPuffGoal(this));
   }

   @Override
   public void tick() {
      if (!this.level().isClientSide() && this.isAlive() && this.isEffectiveAi()) {
         if (this.inflateCounter > 0) {
            if (this.getPuffState() == 0) {
               this.makeSound(SoundEvents.PUFFER_FISH_BLOW_UP);
               this.setPuffState(1);
            } else if (this.inflateCounter > 40 && this.getPuffState() == 1) {
               this.makeSound(SoundEvents.PUFFER_FISH_BLOW_UP);
               this.setPuffState(2);
            }

            this.inflateCounter++;
         } else if (this.getPuffState() != 0) {
            if (this.deflateTimer > 60 && this.getPuffState() == 2) {
               this.makeSound(SoundEvents.PUFFER_FISH_BLOW_OUT);
               this.setPuffState(1);
            } else if (this.deflateTimer > 100 && this.getPuffState() == 1) {
               this.makeSound(SoundEvents.PUFFER_FISH_BLOW_OUT);
               this.setPuffState(0);
            }

            this.deflateTimer++;
         }
      }

      super.tick();
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (this.level() instanceof ServerLevel serverlevel && this.isAlive() && this.getPuffState() > 0) {
         for (Mob mob : this.level()
            .getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(0.3), p_455482_ -> TARGETING_CONDITIONS.test(serverlevel, this, p_455482_))) {
            if (mob.isAlive()) {
               this.touch(serverlevel, mob);
            }
         }
      }
   }

   private void touch(ServerLevel p_457051_, Mob p_450709_) {
      int i = this.getPuffState();
      if (p_450709_.hurtServer(p_457051_, this.damageSources().mobAttack(this), 1 + i)) {
         p_450709_.addEffect(new MobEffectInstance(MobEffects.POISON, 60 * i, 0), this);
         this.playSound(SoundEvents.PUFFER_FISH_STING, 1.0F, 1.0F);
      }
   }

   @Override
   public void playerTouch(Player p_452331_) {
      int i = this.getPuffState();
      if (p_452331_ instanceof ServerPlayer serverplayer && i > 0 && p_452331_.hurtServer(serverplayer.level(), this.damageSources().mobAttack(this), 1 + i)) {
         if (!this.isSilent()) {
            serverplayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.PUFFER_FISH_STING, 0.0F));
         }

         p_452331_.addEffect(new MobEffectInstance(MobEffects.POISON, 60 * i, 0), this);
      }
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.PUFFER_FISH_DEATH;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_457722_) {
      return SoundEvents.PUFFER_FISH_HURT;
   }

   @Override
   protected SoundEvent getFlopSound() {
      return SoundEvents.PUFFER_FISH_FLOP;
   }

   @Override
   public EntityDimensions getDefaultDimensions(Pose p_451909_) {
      return super.getDefaultDimensions(p_451909_).scale(getScale(this.getPuffState()));
   }

   private static float getScale(int p_457350_) {
      switch (p_457350_) {
         case 0:
            return 0.5F;
         case 1:
            return 0.7F;
         default:
            return 1.0F;
      }
   }

   static class PufferfishPuffGoal extends Goal {
      private final Pufferfish fish;

      public PufferfishPuffGoal(Pufferfish p_450670_) {
         this.fish = p_450670_;
      }

      @Override
      public boolean canUse() {
         List<LivingEntity> list = this.fish
            .level()
            .getEntitiesOfClass(
               LivingEntity.class,
               this.fish.getBoundingBox().inflate(2.0),
               p_450323_ -> Pufferfish.TARGETING_CONDITIONS.test(getServerLevel(this.fish), this.fish, p_450323_)
            );
         return !list.isEmpty();
      }

      @Override
      public void start() {
         this.fish.inflateCounter = 1;
         this.fish.deflateTimer = 0;
      }

      @Override
      public void stop() {
         this.fish.inflateCounter = 0;
      }
   }
}
