package net.minecraft.world.entity.npc.villager;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class AbstractVillager extends AgeableMob implements InventoryCarrier, Npc, Merchant {
   private static final EntityDataAccessor<Integer> DATA_UNHAPPY_COUNTER = SynchedEntityData.defineId(AbstractVillager.class, EntityDataSerializers.INT);
   public static final int VILLAGER_SLOT_OFFSET = 300;
   private static final int VILLAGER_INVENTORY_SIZE = 8;
   private @Nullable Player tradingPlayer;
   protected @Nullable MerchantOffers offers;
   private final SimpleContainer inventory = new SimpleContainer(8);

   public AbstractVillager(EntityType<? extends AbstractVillager> p_451120_, Level p_453513_) {
      super(p_451120_, p_453513_);
      this.setPathfindingMalus(PathType.DANGER_FIRE, 16.0F);
      this.setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_458568_, DifficultyInstance p_458741_, EntitySpawnReason p_459582_, @Nullable SpawnGroupData p_450381_
   ) {
      if (p_450381_ == null) {
         p_450381_ = new AgeableMob.AgeableMobGroupData(false);
      }

      return super.finalizeSpawn(p_458568_, p_458741_, p_459582_, p_450381_);
   }

   public int getUnhappyCounter() {
      return this.entityData.get(DATA_UNHAPPY_COUNTER);
   }

   public void setUnhappyCounter(int p_451796_) {
      this.entityData.set(DATA_UNHAPPY_COUNTER, p_451796_);
   }

   @Override
   public int getVillagerXp() {
      return 0;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_455253_) {
      super.defineSynchedData(p_455253_);
      p_455253_.define(DATA_UNHAPPY_COUNTER, 0);
   }

   @Override
   public void setTradingPlayer(@Nullable Player p_458589_) {
      this.tradingPlayer = p_458589_;
   }

   @Override
   public @Nullable Player getTradingPlayer() {
      return this.tradingPlayer;
   }

   public boolean isTrading() {
      return this.tradingPlayer != null;
   }

   @Override
   public MerchantOffers getOffers() {
      if (this.level() instanceof ServerLevel serverlevel) {
         if (this.offers == null) {
            this.offers = new MerchantOffers();
            this.updateTrades(serverlevel);
         }

         return this.offers;
      } else {
         throw new IllegalStateException("Cannot load Villager offers on the client");
      }
   }

   @Override
   public void overrideOffers(@Nullable MerchantOffers p_459279_) {
   }

   @Override
   public void overrideXp(int p_459619_) {
   }

   @Override
   public void notifyTrade(MerchantOffer p_459364_) {
      p_459364_.increaseUses();
      this.ambientSoundTime = -this.getAmbientSoundInterval();
      this.rewardTradeXp(p_459364_);
      if (this.tradingPlayer instanceof ServerPlayer) {
         CriteriaTriggers.TRADE.trigger((ServerPlayer)this.tradingPlayer, this, p_459364_.getResult());
      }
   }

   protected abstract void rewardTradeXp(MerchantOffer var1);

   @Override
   public boolean showProgressBar() {
      return true;
   }

   @Override
   public void notifyTradeUpdated(ItemStack p_460042_) {
      if (!this.level().isClientSide() && this.ambientSoundTime > -this.getAmbientSoundInterval() + 20) {
         this.ambientSoundTime = -this.getAmbientSoundInterval();
         this.makeSound(this.getTradeUpdatedSound(!p_460042_.isEmpty()));
      }
   }

   @Override
   public SoundEvent getNotifyTradeSound() {
      return SoundEvents.VILLAGER_YES;
   }

   protected SoundEvent getTradeUpdatedSound(boolean p_458362_) {
      return p_458362_ ? SoundEvents.VILLAGER_YES : SoundEvents.VILLAGER_NO;
   }

   public void playCelebrateSound() {
      this.makeSound(SoundEvents.VILLAGER_CELEBRATE);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_460365_) {
      super.addAdditionalSaveData(p_460365_);
      if (!this.level().isClientSide()) {
         MerchantOffers merchantoffers = this.getOffers();
         if (!merchantoffers.isEmpty()) {
            p_460365_.store("Offers", MerchantOffers.CODEC, merchantoffers);
         }
      }

      this.writeInventoryToTag(p_460365_);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_455815_) {
      super.readAdditionalSaveData(p_455815_);
      this.offers = p_455815_.<MerchantOffers>read("Offers", MerchantOffers.CODEC).orElse(null);
      this.readInventoryFromTag(p_455815_);
   }

   @Override
   public @Nullable Entity teleport(TeleportTransition p_456625_) {
      this.stopTrading();
      return super.teleport(p_456625_);
   }

   protected void stopTrading() {
      this.setTradingPlayer(null);
   }

   @Override
   public void die(DamageSource p_456881_) {
      super.die(p_456881_);
      this.stopTrading();
   }

   protected void addParticlesAroundSelf(ParticleOptions p_457583_) {
      for (int i = 0; i < 5; i++) {
         double d0 = this.random.nextGaussian() * 0.02;
         double d1 = this.random.nextGaussian() * 0.02;
         double d2 = this.random.nextGaussian() * 0.02;
         this.level().addParticle(p_457583_, this.getRandomX(1.0), this.getRandomY() + 1.0, this.getRandomZ(1.0), d0, d1, d2);
      }
   }

   @Override
   public boolean canBeLeashed() {
      return false;
   }

   @Override
   public SimpleContainer getInventory() {
      return this.inventory;
   }

   @Override
   public @Nullable SlotAccess getSlot(int p_455286_) {
      int i = p_455286_ - 300;
      return i >= 0 && i < this.inventory.getContainerSize() ? this.inventory.getSlot(i) : super.getSlot(p_455286_);
   }

   protected abstract void updateTrades(ServerLevel var1);

   protected void addOffersFromItemListings(ServerLevel p_453111_, MerchantOffers p_457798_, VillagerTrades.ItemListing[] p_455373_, int p_456043_) {
      ArrayList<VillagerTrades.ItemListing> arraylist = Lists.newArrayList(p_455373_);
      int i = 0;

      while (i < p_456043_ && !arraylist.isEmpty()) {
         MerchantOffer merchantoffer = arraylist.remove(this.random.nextInt(arraylist.size())).getOffer(p_453111_, this, this.random);
         if (merchantoffer != null) {
            p_457798_.add(merchantoffer);
            i++;
         }
      }
   }

   @Override
   public Vec3 getRopeHoldPosition(float p_460357_) {
      float f = Mth.lerp(p_460357_, this.yBodyRotO, this.yBodyRot) * (float) (Math.PI / 180.0);
      Vec3 vec3 = new Vec3(0.0, this.getBoundingBox().getYsize() - 1.0, 0.2);
      return this.getPosition(p_460357_).add(vec3.yRot(-f));
   }

   @Override
   public boolean isClientSide() {
      return this.level().isClientSide();
   }

   @Override
   public boolean stillValid(Player p_457494_) {
      return this.getTradingPlayer() == p_457494_ && this.isAlive() && p_457494_.isWithinEntityInteractionRange(this, 4.0);
   }
}
