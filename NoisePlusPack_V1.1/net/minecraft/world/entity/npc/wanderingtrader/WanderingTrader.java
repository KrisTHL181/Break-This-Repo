package net.minecraft.world.entity.npc.wanderingtrader;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.UseItemGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.monster.illager.Illusioner;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;
import org.jspecify.annotations.Nullable;

public class WanderingTrader extends AbstractVillager implements Consumable.OverrideConsumeSound {
   private static final int DEFAULT_DESPAWN_DELAY = 0;
   private @Nullable BlockPos wanderTarget;
   private int despawnDelay = 0;

   public WanderingTrader(EntityType<? extends WanderingTrader> p_456065_, Level p_454049_) {
      super(p_456065_, p_454049_);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector
         .addGoal(
            0,
            new UseItemGoal<>(
               this,
               PotionContents.createItemStack(Items.POTION, Potions.INVISIBILITY),
               SoundEvents.WANDERING_TRADER_DISAPPEARED,
               p_458931_ -> this.level().isDarkOutside() && !p_458931_.isInvisible()
            )
         );
      this.goalSelector
         .addGoal(
            0,
            new UseItemGoal<>(
               this,
               new ItemStack(Items.MILK_BUCKET),
               SoundEvents.WANDERING_TRADER_REAPPEARED,
               p_450276_ -> this.level().isBrightOutside() && p_450276_.isInvisible()
            )
         );
      this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
      this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Zombie.class, 8.0F, 0.5, 0.5));
      this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Evoker.class, 12.0F, 0.5, 0.5));
      this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Vindicator.class, 8.0F, 0.5, 0.5));
      this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Vex.class, 8.0F, 0.5, 0.5));
      this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Pillager.class, 15.0F, 0.5, 0.5));
      this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Illusioner.class, 12.0F, 0.5, 0.5));
      this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Zoglin.class, 10.0F, 0.5, 0.5));
      this.goalSelector.addGoal(1, new PanicGoal(this, 0.5));
      this.goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
      this.goalSelector.addGoal(2, new WanderingTrader.WanderToPositionGoal(this, 2.0, 0.35));
      this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.35));
      this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.35));
      this.goalSelector.addGoal(9, new InteractGoal(this, Player.class, 3.0F, 1.0F));
      this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
   }

   @Override
   public @Nullable AgeableMob getBreedOffspring(ServerLevel p_458075_, AgeableMob p_457715_) {
      return null;
   }

   @Override
   public boolean showProgressBar() {
      return false;
   }

   @Override
   public InteractionResult mobInteract(Player p_450828_, InteractionHand p_450407_) {
      ItemStack itemstack = p_450828_.getItemInHand(p_450407_);
      if (!itemstack.is(Items.VILLAGER_SPAWN_EGG) && this.isAlive() && !this.isTrading() && !this.isBaby()) {
         if (p_450407_ == InteractionHand.MAIN_HAND) {
            p_450828_.awardStat(Stats.TALKED_TO_VILLAGER);
         }

         if (!this.level().isClientSide()) {
            if (this.getOffers().isEmpty()) {
               return InteractionResult.CONSUME;
            }

            this.setTradingPlayer(p_450828_);
            this.openTradingScreen(p_450828_, this.getDisplayName(), 1);
         }

         return InteractionResult.SUCCESS;
      } else {
         return super.mobInteract(p_450828_, p_450407_);
      }
   }

   @Override
   protected void updateTrades(ServerLevel p_451494_) {
      MerchantOffers merchantoffers = this.getOffers();

      for (Pair<VillagerTrades.ItemListing[], Integer> pair : VillagerTrades.WANDERING_TRADER_TRADES) {
         VillagerTrades.ItemListing[] avillagertrades$itemlisting = (VillagerTrades.ItemListing[])pair.getLeft();
         this.addOffersFromItemListings(p_451494_, merchantoffers, avillagertrades$itemlisting, (Integer)pair.getRight());
      }
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_452058_) {
      super.addAdditionalSaveData(p_452058_);
      p_452058_.putInt("DespawnDelay", this.despawnDelay);
      p_452058_.storeNullable("wander_target", BlockPos.CODEC, this.wanderTarget);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_451244_) {
      super.readAdditionalSaveData(p_451244_);
      this.despawnDelay = p_451244_.getIntOr("DespawnDelay", 0);
      this.wanderTarget = p_451244_.<BlockPos>read("wander_target", BlockPos.CODEC).orElse(null);
      this.setAge(Math.max(0, this.getAge()));
   }

   @Override
   public boolean removeWhenFarAway(double p_456300_) {
      return false;
   }

   @Override
   protected void rewardTradeXp(MerchantOffer p_451899_) {
      if (p_451899_.shouldRewardExp()) {
         int i = 3 + this.random.nextInt(4);
         this.level().addFreshEntity(new ExperienceOrb(this.level(), this.getX(), this.getY() + 0.5, this.getZ(), i));
      }
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return this.isTrading() ? SoundEvents.WANDERING_TRADER_TRADE : SoundEvents.WANDERING_TRADER_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_451327_) {
      return SoundEvents.WANDERING_TRADER_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.WANDERING_TRADER_DEATH;
   }

   @Override
   public SoundEvent getConsumeSound(ItemStack p_454789_) {
      return p_454789_.is(Items.MILK_BUCKET) ? SoundEvents.WANDERING_TRADER_DRINK_MILK : SoundEvents.WANDERING_TRADER_DRINK_POTION;
   }

   @Override
   protected SoundEvent getTradeUpdatedSound(boolean p_458189_) {
      return p_458189_ ? SoundEvents.WANDERING_TRADER_YES : SoundEvents.WANDERING_TRADER_NO;
   }

   @Override
   public SoundEvent getNotifyTradeSound() {
      return SoundEvents.WANDERING_TRADER_YES;
   }

   public void setDespawnDelay(int p_453215_) {
      this.despawnDelay = p_453215_;
   }

   public int getDespawnDelay() {
      return this.despawnDelay;
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (!this.level().isClientSide()) {
         this.maybeDespawn();
      }
   }

   private void maybeDespawn() {
      if (this.despawnDelay > 0 && !this.isTrading() && --this.despawnDelay == 0) {
         this.discard();
      }
   }

   public void setWanderTarget(@Nullable BlockPos p_458924_) {
      this.wanderTarget = p_458924_;
   }

   @Nullable BlockPos getWanderTarget() {
      return this.wanderTarget;
   }

   class WanderToPositionGoal extends Goal {
      final WanderingTrader trader;
      final double stopDistance;
      final double speedModifier;

      WanderToPositionGoal(final WanderingTrader p_450649_, final double p_451728_, final double p_458783_) {
         this.trader = p_450649_;
         this.stopDistance = p_451728_;
         this.speedModifier = p_458783_;
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public void stop() {
         this.trader.setWanderTarget(null);
         WanderingTrader.this.navigation.stop();
      }

      @Override
      public boolean canUse() {
         BlockPos blockpos = this.trader.getWanderTarget();
         return blockpos != null && this.isTooFarAway(blockpos, this.stopDistance);
      }

      @Override
      public void tick() {
         BlockPos blockpos = this.trader.getWanderTarget();
         if (blockpos != null && WanderingTrader.this.navigation.isDone()) {
            if (this.isTooFarAway(blockpos, 10.0)) {
               Vec3 vec3 = new Vec3(blockpos.getX() - this.trader.getX(), blockpos.getY() - this.trader.getY(), blockpos.getZ() - this.trader.getZ())
                  .normalize();
               Vec3 vec31 = vec3.scale(10.0).add(this.trader.getX(), this.trader.getY(), this.trader.getZ());
               WanderingTrader.this.navigation.moveTo(vec31.x, vec31.y, vec31.z, this.speedModifier);
            } else {
               WanderingTrader.this.navigation.moveTo(blockpos.getX(), blockpos.getY(), blockpos.getZ(), this.speedModifier);
            }
         }
      }

      private boolean isTooFarAway(BlockPos p_456686_, double p_452741_) {
         return !p_456686_.closerToCenterThan(this.trader.position(), p_452741_);
      }
   }
}
