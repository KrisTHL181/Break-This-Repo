package net.minecraft.world.entity.animal.equine;

import java.util.EnumSet;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class TraderLlama extends Llama {
   private static final int DEFAULT_DESPAWN_DELAY = 47999;
   private int despawnDelay = 47999;

   public TraderLlama(EntityType<? extends TraderLlama> p_454669_, Level p_459329_) {
      super(p_454669_, p_459329_);
   }

   @Override
   public boolean isTraderLlama() {
      return true;
   }

   @Override
   protected @Nullable Llama makeNewLlama() {
      return EntityType.TRADER_LLAMA.create(this.level(), EntitySpawnReason.BREEDING);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_459664_) {
      super.addAdditionalSaveData(p_459664_);
      p_459664_.putInt("DespawnDelay", this.despawnDelay);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_454804_) {
      super.readAdditionalSaveData(p_454804_);
      this.despawnDelay = p_454804_.getIntOr("DespawnDelay", 47999);
   }

   @Override
   protected void registerGoals() {
      super.registerGoals();
      this.goalSelector.addGoal(1, new PanicGoal(this, 2.0));
      this.targetSelector.addGoal(1, new TraderLlama.TraderLlamaDefendWanderingTraderGoal(this));
      this.targetSelector
         .addGoal(2, new NearestAttackableTargetGoal<>(this, Zombie.class, true, (p_454346_, p_459208_) -> p_454346_.getType() != EntityType.ZOMBIFIED_PIGLIN));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, AbstractIllager.class, true));
   }

   public void setDespawnDelay(int p_459767_) {
      this.despawnDelay = p_459767_;
   }

   @Override
   protected void doPlayerRide(Player p_452507_) {
      Entity entity = this.getLeashHolder();
      if (!(entity instanceof WanderingTrader)) {
         super.doPlayerRide(p_452507_);
      }
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (!this.level().isClientSide()) {
         this.maybeDespawn();
      }
   }

   private void maybeDespawn() {
      if (this.canDespawn()) {
         this.despawnDelay = this.isLeashedToWanderingTrader() ? ((WanderingTrader)this.getLeashHolder()).getDespawnDelay() - 1 : this.despawnDelay - 1;
         if (this.despawnDelay <= 0) {
            this.removeLeash();
            this.discard();
         }
      }
   }

   private boolean canDespawn() {
      return !this.isTamed() && !this.isLeashedToSomethingOtherThanTheWanderingTrader() && !this.hasExactlyOnePlayerPassenger();
   }

   private boolean isLeashedToWanderingTrader() {
      return this.getLeashHolder() instanceof WanderingTrader;
   }

   private boolean isLeashedToSomethingOtherThanTheWanderingTrader() {
      return this.isLeashed() && !this.isLeashedToWanderingTrader();
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_457953_, DifficultyInstance p_451224_, EntitySpawnReason p_458031_, @Nullable SpawnGroupData p_452147_
   ) {
      if (p_458031_ == EntitySpawnReason.EVENT) {
         this.setAge(0);
      }

      if (p_452147_ == null) {
         p_452147_ = new AgeableMob.AgeableMobGroupData(false);
      }

      return super.finalizeSpawn(p_457953_, p_451224_, p_458031_, p_452147_);
   }

   protected static class TraderLlamaDefendWanderingTraderGoal extends TargetGoal {
      private final Llama llama;
      private LivingEntity ownerLastHurtBy;
      private int timestamp;

      public TraderLlamaDefendWanderingTraderGoal(Llama p_457428_) {
         super(p_457428_, false);
         this.llama = p_457428_;
         this.setFlags(EnumSet.of(Goal.Flag.TARGET));
      }

      @Override
      public boolean canUse() {
         if (!this.llama.isLeashed()) {
            return false;
         } else if (!(this.llama.getLeashHolder() instanceof WanderingTrader wanderingtrader)) {
            return false;
         } else {
            this.ownerLastHurtBy = wanderingtrader.getLastHurtByMob();
            int i = wanderingtrader.getLastHurtByMobTimestamp();
            return i != this.timestamp && this.canAttack(this.ownerLastHurtBy, TargetingConditions.DEFAULT);
         }
      }

      @Override
      public void start() {
         this.mob.setTarget(this.ownerLastHurtBy);
         Entity entity = this.llama.getLeashHolder();
         if (entity instanceof WanderingTrader) {
            this.timestamp = ((WanderingTrader)entity).getLastHurtByMobTimestamp();
         }

         super.start();
      }
   }
}
