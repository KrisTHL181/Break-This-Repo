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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSets;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class WanderingTrader extends AbstractVillager implements Consumable.OverrideConsumeSound {
   private static final int DEFAULT_DESPAWN_DELAY = 0;
   private @Nullable BlockPos wanderTarget;
   private int despawnDelay = 0;

   public WanderingTrader(final EntityType<? extends WanderingTrader> type, final Level level) {
      super(type, level);
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
               e -> this.level().isDarkOutside() && !e.isInvisible()
            )
         );
      this.goalSelector
         .addGoal(
            0,
            new UseItemGoal<>(
               this, new ItemStack(Items.MILK_BUCKET), SoundEvents.WANDERING_TRADER_REAPPEARED, e -> this.level().isBrightOutside() && e.isInvisible()
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
   public @Nullable AgeableMob getBreedOffspring(final ServerLevel level, final AgeableMob partner) {
      return null;
   }

   @Override
   public boolean showProgressBar() {
      return false;
   }

   @Override
   public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      if (!itemStack.is(Items.VILLAGER_SPAWN_EGG) && this.isAlive() && !this.isTrading() && !this.isBaby()) {
         if (hand == InteractionHand.MAIN_HAND) {
            player.awardStat(Stats.TALKED_TO_VILLAGER);
         }

         if (!this.level().isClientSide()) {
            if (this.getOffers().isEmpty()) {
               return InteractionResult.CONSUME;
            }

            this.setTradingPlayer(player);
            this.openTradingScreen(player, this.getDisplayName(), 1);
         }

         return InteractionResult.SUCCESS;
      } else {
         return super.mobInteract(player, hand);
      }
   }

   @Override
   protected void updateTrades(final ServerLevel level) {
      MerchantOffers offers = this.getOffers();
      this.addOffersFromTradeSet(level, offers, TradeSets.WANDERING_TRADER_BUYING);
      this.addOffersFromTradeSet(level, offers, TradeSets.WANDERING_TRADER_UNCOMMON);
      this.addOffersFromTradeSet(level, offers, TradeSets.WANDERING_TRADER_COMMON);
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putInt("DespawnDelay", this.despawnDelay);
      output.storeNullable("wander_target", BlockPos.CODEC, this.wanderTarget);
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.despawnDelay = input.getIntOr("DespawnDelay", 0);
      this.wanderTarget = input.<BlockPos>read("wander_target", BlockPos.CODEC).orElse(null);
      this.setAge(Math.max(0, this.getAge()));
   }

   @Override
   public boolean removeWhenFarAway(final double distSqr) {
      return false;
   }

   @Override
   protected void rewardTradeXp(final MerchantOffer offer) {
      if (offer.shouldRewardExp()) {
         int popXp = 3 + this.random.nextInt(4);
         this.level().addFreshEntity(new ExperienceOrb(this.level(), this.getX(), this.getY() + 0.5, this.getZ(), popXp));
      }
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return this.isTrading() ? SoundEvents.WANDERING_TRADER_TRADE : SoundEvents.WANDERING_TRADER_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(final DamageSource source) {
      return SoundEvents.WANDERING_TRADER_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.WANDERING_TRADER_DEATH;
   }

   @Override
   public SoundEvent getConsumeSound(final ItemStack itemStack) {
      return itemStack.is(Items.MILK_BUCKET) ? SoundEvents.WANDERING_TRADER_DRINK_MILK : SoundEvents.WANDERING_TRADER_DRINK_POTION;
   }

   @Override
   protected SoundEvent getTradeUpdatedSound(final boolean validTrade) {
      return validTrade ? SoundEvents.WANDERING_TRADER_YES : SoundEvents.WANDERING_TRADER_NO;
   }

   @Override
   public SoundEvent getNotifyTradeSound() {
      return SoundEvents.WANDERING_TRADER_YES;
   }

   public void setDespawnDelay(final int despawnDelay) {
      this.despawnDelay = despawnDelay;
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

   public void setWanderTarget(final @Nullable BlockPos pos) {
      this.wanderTarget = pos;
   }

   private @Nullable BlockPos getWanderTarget() {
      return this.wanderTarget;
   }

   private class WanderToPositionGoal extends Goal {
      private final WanderingTrader trader;
      private final double stopDistance;
      private final double speedModifier;

      public WanderToPositionGoal(final WanderingTrader trader, final double stopDistance, final double speedModifier) {
         this.trader = trader;
         this.stopDistance = stopDistance;
         this.speedModifier = speedModifier;
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public void stop() {
         this.trader.setWanderTarget(null);
         WanderingTrader.this.navigation.stop();
      }

      @Override
      public boolean canUse() {
         BlockPos wanderPosition = this.trader.getWanderTarget();
         return wanderPosition != null && this.isTooFarAway(wanderPosition, this.stopDistance);
      }

      @Override
      public void tick() {
         BlockPos wanderPosition = this.trader.getWanderTarget();
         if (wanderPosition != null && WanderingTrader.this.navigation.isDone()) {
            if (this.isTooFarAway(wanderPosition, 10.0)) {
               Vec3 dir = new Vec3(
                     wanderPosition.getX() - this.trader.getX(), wanderPosition.getY() - this.trader.getY(), wanderPosition.getZ() - this.trader.getZ()
                  )
                  .normalize();
               Vec3 targetPos = dir.scale(10.0).add(this.trader.getX(), this.trader.getY(), this.trader.getZ());
               WanderingTrader.this.navigation.moveTo(targetPos.x, targetPos.y, targetPos.z, this.speedModifier);
            } else {
               WanderingTrader.this.navigation.moveTo(wanderPosition.getX(), wanderPosition.getY(), wanderPosition.getZ(), this.speedModifier);
            }
         }
      }

      private boolean isTooFarAway(final BlockPos pos, final double distance) {
         return !pos.closerToCenterThan(this.trader.position(), distance);
      }
   }
}
