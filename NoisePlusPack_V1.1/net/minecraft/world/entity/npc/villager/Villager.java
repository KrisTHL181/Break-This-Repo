package net.minecraft.world.entity.npc.villager;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.SpawnUtil;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ReputationEventHandler;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.VillagerGoalPackages;
import net.minecraft.world.entity.ai.gossip.GossipContainer;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.sensing.GolemSensor;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.ai.village.ReputationEventType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class Villager extends AbstractVillager implements ReputationEventHandler, VillagerDataHolder {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final EntityDataAccessor<VillagerData> DATA_VILLAGER_DATA = SynchedEntityData.defineId(Villager.class, EntityDataSerializers.VILLAGER_DATA);
   public static final int BREEDING_FOOD_THRESHOLD = 12;
   public static final Map<Item, Integer> FOOD_POINTS = ImmutableMap.of(Items.BREAD, 4, Items.POTATO, 1, Items.CARROT, 1, Items.BEETROOT, 1);
   private static final int TRADES_PER_LEVEL = 2;
   private static final int MAX_GOSSIP_TOPICS = 10;
   private static final int GOSSIP_COOLDOWN = 1200;
   private static final int GOSSIP_DECAY_INTERVAL = 24000;
   private static final int HOW_FAR_AWAY_TO_TALK_TO_OTHER_VILLAGERS_ABOUT_GOLEMS = 10;
   private static final int HOW_MANY_VILLAGERS_NEED_TO_AGREE_TO_SPAWN_A_GOLEM = 5;
   private static final long TIME_SINCE_SLEEPING_FOR_GOLEM_SPAWNING = 24000L;
   @VisibleForTesting
   public static final float SPEED_MODIFIER = 0.5F;
   private static final int DEFAULT_XP = 0;
   private static final byte DEFAULT_FOOD_LEVEL = 0;
   private static final int DEFAULT_LAST_RESTOCK = 0;
   private static final int DEFAULT_LAST_GOSSIP_DECAY = 0;
   private static final int DEFAULT_RESTOCKS_TODAY = 0;
   private static final boolean DEFAULT_ASSIGN_PROFESSION_WHEN_SPAWNED = false;
   private int updateMerchantTimer;
   private boolean increaseProfessionLevelOnUpdate;
   private @Nullable Player lastTradedPlayer;
   private boolean chasing;
   private int foodLevel = 0;
   private final GossipContainer gossips = new GossipContainer();
   private long lastGossipTime;
   private long lastGossipDecayTime = 0L;
   private int villagerXp = 0;
   private long lastRestockGameTime = 0L;
   private int numberOfRestocksToday = 0;
   private long lastRestockCheckDay;
   private boolean assignProfessionWhenSpawned = false;
   private static final ImmutableList<MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
      MemoryModuleType.HOME,
      MemoryModuleType.JOB_SITE,
      MemoryModuleType.POTENTIAL_JOB_SITE,
      MemoryModuleType.MEETING_POINT,
      MemoryModuleType.NEAREST_LIVING_ENTITIES,
      MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
      MemoryModuleType.VISIBLE_VILLAGER_BABIES,
      MemoryModuleType.NEAREST_PLAYERS,
      MemoryModuleType.NEAREST_VISIBLE_PLAYER,
      MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER,
      MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM,
      MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS,
      new MemoryModuleType[]{
         MemoryModuleType.WALK_TARGET,
         MemoryModuleType.LOOK_TARGET,
         MemoryModuleType.INTERACTION_TARGET,
         MemoryModuleType.BREED_TARGET,
         MemoryModuleType.PATH,
         MemoryModuleType.DOORS_TO_CLOSE,
         MemoryModuleType.NEAREST_BED,
         MemoryModuleType.HURT_BY,
         MemoryModuleType.HURT_BY_ENTITY,
         MemoryModuleType.NEAREST_HOSTILE,
         MemoryModuleType.SECONDARY_JOB_SITE,
         MemoryModuleType.HIDING_PLACE,
         MemoryModuleType.HEARD_BELL_TIME,
         MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
         MemoryModuleType.LAST_SLEPT,
         MemoryModuleType.LAST_WOKEN,
         MemoryModuleType.LAST_WORKED_AT_POI,
         MemoryModuleType.GOLEM_DETECTED_RECENTLY
      }
   );
   private static final ImmutableList<SensorType<? extends Sensor<? super Villager>>> SENSOR_TYPES = ImmutableList.of(
      SensorType.NEAREST_LIVING_ENTITIES,
      SensorType.NEAREST_PLAYERS,
      SensorType.NEAREST_ITEMS,
      SensorType.NEAREST_BED,
      SensorType.HURT_BY,
      SensorType.VILLAGER_HOSTILES,
      SensorType.VILLAGER_BABIES,
      SensorType.SECONDARY_POIS,
      SensorType.GOLEM_DETECTED
   );
   public static final Map<MemoryModuleType<GlobalPos>, BiPredicate<Villager, Holder<PoiType>>> POI_MEMORIES = ImmutableMap.of(
      MemoryModuleType.HOME,
      (BiPredicate<Villager, Holder>)(p_456108_, p_460639_) -> p_460639_.is(PoiTypes.HOME),
      MemoryModuleType.JOB_SITE,
      (BiPredicate<Villager, Holder>)(p_452566_, p_451152_) -> p_452566_.getVillagerData().profession().value().heldJobSite().test(p_451152_),
      MemoryModuleType.POTENTIAL_JOB_SITE,
      (BiPredicate<Villager, Holder>)(p_452954_, p_454683_) -> VillagerProfession.ALL_ACQUIRABLE_JOBS.test(p_454683_),
      MemoryModuleType.MEETING_POINT,
      (BiPredicate<Villager, Holder>)(p_453468_, p_455605_) -> p_455605_.is(PoiTypes.MEETING)
   );

   public Villager(EntityType<? extends Villager> p_453162_, Level p_452486_) {
      this(p_453162_, p_452486_, VillagerType.PLAINS);
   }

   public Villager(EntityType<? extends Villager> p_458695_, Level p_457472_, ResourceKey<VillagerType> p_454614_) {
      this(p_458695_, p_457472_, p_457472_.registryAccess().getOrThrow(p_454614_));
   }

   public Villager(EntityType<? extends Villager> p_456622_, Level p_453762_, Holder<VillagerType> p_453032_) {
      super(p_456622_, p_453762_);
      this.getNavigation().setCanOpenDoors(true);
      this.getNavigation().setCanFloat(true);
      this.getNavigation().setRequiredPathLength(48.0F);
      this.setCanPickUpLoot(true);
      this.setVillagerData(this.getVillagerData().withType(p_453032_).withProfession(p_453762_.registryAccess(), VillagerProfession.NONE));
   }

   @Override
   public Brain<Villager> getBrain() {
      return (Brain<Villager>)super.getBrain();
   }

   @Override
   protected Brain.Provider<Villager> brainProvider() {
      return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
   }

   @Override
   protected Brain<?> makeBrain(Dynamic<?> p_453634_) {
      Brain<Villager> brain = this.brainProvider().makeBrain(p_453634_);
      this.registerBrainGoals(brain);
      return brain;
   }

   public void refreshBrain(ServerLevel p_457985_) {
      Brain<Villager> brain = this.getBrain();
      brain.stopAll(p_457985_, this);
      this.brain = brain.copyWithoutBehaviors();
      this.registerBrainGoals(this.getBrain());
   }

   private void registerBrainGoals(Brain<Villager> p_454533_) {
      Holder<VillagerProfession> holder = this.getVillagerData().profession();
      if (this.isBaby()) {
         p_454533_.setSchedule(EnvironmentAttributes.BABY_VILLAGER_ACTIVITY);
         p_454533_.addActivity(Activity.PLAY, VillagerGoalPackages.getPlayPackage(0.5F));
      } else {
         p_454533_.setSchedule(EnvironmentAttributes.VILLAGER_ACTIVITY);
         p_454533_.addActivityWithConditions(
            Activity.WORK, VillagerGoalPackages.getWorkPackage(holder, 0.5F), ImmutableSet.of(Pair.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT))
         );
      }

      p_454533_.addActivity(Activity.CORE, VillagerGoalPackages.getCorePackage(holder, 0.5F));
      p_454533_.addActivityWithConditions(
         Activity.MEET, VillagerGoalPackages.getMeetPackage(holder, 0.5F), ImmutableSet.of(Pair.of(MemoryModuleType.MEETING_POINT, MemoryStatus.VALUE_PRESENT))
      );
      p_454533_.addActivity(Activity.REST, VillagerGoalPackages.getRestPackage(holder, 0.5F));
      p_454533_.addActivity(Activity.IDLE, VillagerGoalPackages.getIdlePackage(holder, 0.5F));
      p_454533_.addActivity(Activity.PANIC, VillagerGoalPackages.getPanicPackage(holder, 0.5F));
      p_454533_.addActivity(Activity.PRE_RAID, VillagerGoalPackages.getPreRaidPackage(holder, 0.5F));
      p_454533_.addActivity(Activity.RAID, VillagerGoalPackages.getRaidPackage(holder, 0.5F));
      p_454533_.addActivity(Activity.HIDE, VillagerGoalPackages.getHidePackage(holder, 0.5F));
      p_454533_.setCoreActivities(ImmutableSet.of(Activity.CORE));
      p_454533_.setDefaultActivity(Activity.IDLE);
      p_454533_.setActiveActivityIfPossible(Activity.IDLE);
      p_454533_.updateActivityFromSchedule(this.level().environmentAttributes(), this.level().getGameTime(), this.position());
   }

   @Override
   protected void ageBoundaryReached() {
      super.ageBoundaryReached();
      if (this.level() instanceof ServerLevel) {
         this.refreshBrain((ServerLevel)this.level());
      }
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.5);
   }

   public boolean assignProfessionWhenSpawned() {
      return this.assignProfessionWhenSpawned;
   }

   @Override
   protected void customServerAiStep(ServerLevel p_453665_) {
      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("villagerBrain");
      this.getBrain().tick(p_453665_, this);
      profilerfiller.pop();
      if (this.assignProfessionWhenSpawned) {
         this.assignProfessionWhenSpawned = false;
      }

      if (!this.isTrading() && this.updateMerchantTimer > 0) {
         this.updateMerchantTimer--;
         if (this.updateMerchantTimer <= 0) {
            if (this.increaseProfessionLevelOnUpdate) {
               this.increaseMerchantCareer(p_453665_);
               this.increaseProfessionLevelOnUpdate = false;
            }

            this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
         }
      }

      if (this.lastTradedPlayer != null) {
         p_453665_.onReputationEvent(ReputationEventType.TRADE, this.lastTradedPlayer, this);
         p_453665_.broadcastEntityEvent(this, (byte)14);
         this.lastTradedPlayer = null;
      }

      if (!this.isNoAi() && this.random.nextInt(100) == 0) {
         Raid raid = p_453665_.getRaidAt(this.blockPosition());
         if (raid != null && raid.isActive() && !raid.isOver()) {
            p_453665_.broadcastEntityEvent(this, (byte)42);
         }
      }

      if (this.getVillagerData().profession().is(VillagerProfession.NONE) && this.isTrading()) {
         this.stopTrading();
      }

      super.customServerAiStep(p_453665_);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.getUnhappyCounter() > 0) {
         this.setUnhappyCounter(this.getUnhappyCounter() - 1);
      }

      this.maybeDecayGossip();
   }

   @Override
   public InteractionResult mobInteract(Player p_458495_, InteractionHand p_455648_) {
      ItemStack itemstack = p_458495_.getItemInHand(p_455648_);
      if (itemstack.is(Items.VILLAGER_SPAWN_EGG) || !this.isAlive() || this.isTrading() || this.isSleeping()) {
         return super.mobInteract(p_458495_, p_455648_);
      }

      if (this.isBaby()) {
         this.setUnhappy();
         return InteractionResult.SUCCESS;
      }

      if (!this.level().isClientSide()) {
         boolean flag = this.getOffers().isEmpty();
         if (p_455648_ == InteractionHand.MAIN_HAND) {
            if (flag) {
               this.setUnhappy();
            }

            p_458495_.awardStat(Stats.TALKED_TO_VILLAGER);
         }

         if (flag) {
            return InteractionResult.CONSUME;
         }

         this.startTrading(p_458495_);
      }

      return InteractionResult.SUCCESS;
   }

   private void setUnhappy() {
      this.setUnhappyCounter(40);
      if (!this.level().isClientSide()) {
         this.makeSound(SoundEvents.VILLAGER_NO);
      }
   }

   private void startTrading(Player p_451591_) {
      this.updateSpecialPrices(p_451591_);
      this.setTradingPlayer(p_451591_);
      this.openTradingScreen(p_451591_, this.getDisplayName(), this.getVillagerData().level());
   }

   @Override
   public void setTradingPlayer(@Nullable Player p_452681_) {
      boolean flag = this.getTradingPlayer() != null && p_452681_ == null;
      super.setTradingPlayer(p_452681_);
      if (flag) {
         this.stopTrading();
      }
   }

   @Override
   protected void stopTrading() {
      super.stopTrading();
      this.resetSpecialPrices();
   }

   private void resetSpecialPrices() {
      if (!this.level().isClientSide()) {
         for (MerchantOffer merchantoffer : this.getOffers()) {
            merchantoffer.resetSpecialPriceDiff();
         }
      }
   }

   @Override
   public boolean canRestock() {
      return true;
   }

   public void restock() {
      this.updateDemand();

      for (MerchantOffer merchantoffer : this.getOffers()) {
         merchantoffer.resetUses();
      }

      this.resendOffersToTradingPlayer();
      this.lastRestockGameTime = this.level().getGameTime();
      this.numberOfRestocksToday++;
   }

   private void resendOffersToTradingPlayer() {
      MerchantOffers merchantoffers = this.getOffers();
      Player player = this.getTradingPlayer();
      if (player != null && !merchantoffers.isEmpty()) {
         player.sendMerchantOffers(
            player.containerMenu.containerId, merchantoffers, this.getVillagerData().level(), this.getVillagerXp(), this.showProgressBar(), this.canRestock()
         );
      }
   }

   private boolean needsToRestock() {
      for (MerchantOffer merchantoffer : this.getOffers()) {
         if (merchantoffer.needsRestock()) {
            return true;
         }
      }

      return false;
   }

   private boolean allowedToRestock() {
      return this.numberOfRestocksToday == 0 || this.numberOfRestocksToday < 2 && this.level().getGameTime() > this.lastRestockGameTime + 2400L;
   }

   public boolean shouldRestock(ServerLevel p_455283_) {
      long i = this.lastRestockGameTime + 12000L;
      long j = this.level().getGameTime();
      boolean flag = j > i;
      long k = p_455283_.getDayCount();
      flag |= this.lastRestockCheckDay > 0L && k > this.lastRestockCheckDay;
      this.lastRestockCheckDay = k;
      if (flag) {
         this.lastRestockGameTime = j;
         this.resetNumberOfRestocks();
      }

      return this.allowedToRestock() && this.needsToRestock();
   }

   private void catchUpDemand() {
      int i = 2 - this.numberOfRestocksToday;
      if (i > 0) {
         for (MerchantOffer merchantoffer : this.getOffers()) {
            merchantoffer.resetUses();
         }
      }

      for (int j = 0; j < i; j++) {
         this.updateDemand();
      }

      this.resendOffersToTradingPlayer();
   }

   private void updateDemand() {
      for (MerchantOffer merchantoffer : this.getOffers()) {
         merchantoffer.updateDemand();
      }
   }

   private void updateSpecialPrices(Player p_460482_) {
      int i = this.getPlayerReputation(p_460482_);
      if (i != 0) {
         for (MerchantOffer merchantoffer : this.getOffers()) {
            merchantoffer.addToSpecialPriceDiff(-Mth.floor(i * merchantoffer.getPriceMultiplier()));
         }
      }

      if (p_460482_.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) {
         MobEffectInstance mobeffectinstance = p_460482_.getEffect(MobEffects.HERO_OF_THE_VILLAGE);
         int k = mobeffectinstance.getAmplifier();

         for (MerchantOffer merchantoffer1 : this.getOffers()) {
            double d0 = 0.3 + 0.0625 * k;
            int j = (int)Math.floor(d0 * merchantoffer1.getBaseCostA().getCount());
            merchantoffer1.addToSpecialPriceDiff(-Math.max(j, 1));
         }
      }
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_453569_) {
      super.defineSynchedData(p_453569_);
      p_453569_.define(DATA_VILLAGER_DATA, createDefaultVillagerData());
   }

   public static VillagerData createDefaultVillagerData() {
      return new VillagerData(
         BuiltInRegistries.VILLAGER_TYPE.getOrThrow(VillagerType.PLAINS), BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.NONE), 1
      );
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_454810_) {
      super.addAdditionalSaveData(p_454810_);
      p_454810_.store("VillagerData", VillagerData.CODEC, this.getVillagerData());
      p_454810_.putByte("FoodLevel", (byte)this.foodLevel);
      p_454810_.store("Gossips", GossipContainer.CODEC, this.gossips);
      p_454810_.putInt("Xp", this.villagerXp);
      p_454810_.putLong("LastRestock", this.lastRestockGameTime);
      p_454810_.putLong("LastGossipDecay", this.lastGossipDecayTime);
      p_454810_.putInt("RestocksToday", this.numberOfRestocksToday);
      if (this.assignProfessionWhenSpawned) {
         p_454810_.putBoolean("AssignProfessionWhenSpawned", true);
      }
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_458434_) {
      super.readAdditionalSaveData(p_458434_);
      this.entityData.set(DATA_VILLAGER_DATA, p_458434_.<VillagerData>read("VillagerData", VillagerData.CODEC).orElseGet(Villager::createDefaultVillagerData));
      this.foodLevel = p_458434_.getByteOr("FoodLevel", (byte)0);
      this.gossips.clear();
      p_458434_.<GossipContainer>read("Gossips", GossipContainer.CODEC).ifPresent(this.gossips::putAll);
      this.villagerXp = p_458434_.getIntOr("Xp", 0);
      this.lastRestockGameTime = p_458434_.getLongOr("LastRestock", 0L);
      this.lastGossipDecayTime = p_458434_.getLongOr("LastGossipDecay", 0L);
      if (this.level() instanceof ServerLevel) {
         this.refreshBrain((ServerLevel)this.level());
      }

      this.numberOfRestocksToday = p_458434_.getIntOr("RestocksToday", 0);
      this.assignProfessionWhenSpawned = p_458434_.getBooleanOr("AssignProfessionWhenSpawned", false);
   }

   @Override
   public boolean removeWhenFarAway(double p_451316_) {
      return false;
   }

   @Override
   protected @Nullable SoundEvent getAmbientSound() {
      if (this.isSleeping()) {
         return null;
      } else {
         return this.isTrading() ? SoundEvents.VILLAGER_TRADE : SoundEvents.VILLAGER_AMBIENT;
      }
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_450761_) {
      return SoundEvents.VILLAGER_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.VILLAGER_DEATH;
   }

   public void playWorkSound() {
      this.makeSound(this.getVillagerData().profession().value().workSound());
   }

   @Override
   public void setVillagerData(VillagerData p_457478_) {
      VillagerData villagerdata = this.getVillagerData();
      if (!villagerdata.profession().equals(p_457478_.profession())) {
         this.offers = null;
      }

      this.entityData.set(DATA_VILLAGER_DATA, p_457478_);
   }

   @Override
   public VillagerData getVillagerData() {
      return this.entityData.get(DATA_VILLAGER_DATA);
   }

   @Override
   protected void rewardTradeXp(MerchantOffer p_450902_) {
      int i = 3 + this.random.nextInt(4);
      this.villagerXp = this.villagerXp + p_450902_.getXp();
      this.lastTradedPlayer = this.getTradingPlayer();
      if (this.shouldIncreaseLevel()) {
         this.updateMerchantTimer = 40;
         this.increaseProfessionLevelOnUpdate = true;
         i += 5;
      }

      if (p_450902_.shouldRewardExp()) {
         this.level().addFreshEntity(new ExperienceOrb(this.level(), this.getX(), this.getY() + 0.5, this.getZ(), i));
      }
   }

   @Override
   public void setLastHurtByMob(@Nullable LivingEntity p_460353_) {
      if (p_460353_ != null && this.level() instanceof ServerLevel) {
         ((ServerLevel)this.level()).onReputationEvent(ReputationEventType.VILLAGER_HURT, p_460353_, this);
         if (this.isAlive() && p_460353_ instanceof Player) {
            this.level().broadcastEntityEvent(this, (byte)13);
         }
      }

      super.setLastHurtByMob(p_460353_);
   }

   @Override
   public void die(DamageSource p_453861_) {
      LOGGER.info("Villager {} died, message: '{}'", this, p_453861_.getLocalizedDeathMessage(this).getString());
      Entity entity = p_453861_.getEntity();
      if (entity != null) {
         this.tellWitnessesThatIWasMurdered(entity);
      }

      this.releaseAllPois();
      super.die(p_453861_);
   }

   private void releaseAllPois() {
      this.releasePoi(MemoryModuleType.HOME);
      this.releasePoi(MemoryModuleType.JOB_SITE);
      this.releasePoi(MemoryModuleType.POTENTIAL_JOB_SITE);
      this.releasePoi(MemoryModuleType.MEETING_POINT);
   }

   private void tellWitnessesThatIWasMurdered(Entity p_459749_) {
      if (this.level() instanceof ServerLevel serverlevel) {
         Optional<NearestVisibleLivingEntities> optional = this.brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
         if (!optional.isEmpty()) {
            optional.get()
               .findAll(ReputationEventHandler.class::isInstance)
               .forEach(p_453729_ -> serverlevel.onReputationEvent(ReputationEventType.VILLAGER_KILLED, p_459749_, (ReputationEventHandler)p_453729_));
         }
      }
   }

   public void releasePoi(MemoryModuleType<GlobalPos> p_453763_) {
      if (this.level() instanceof ServerLevel) {
         MinecraftServer minecraftserver = ((ServerLevel)this.level()).getServer();
         this.brain.getMemory(p_453763_).ifPresent(p_455236_ -> {
            ServerLevel serverlevel = minecraftserver.getLevel(p_455236_.dimension());
            if (serverlevel != null) {
               PoiManager poimanager = serverlevel.getPoiManager();
               Optional<Holder<PoiType>> optional = poimanager.getType(p_455236_.pos());
               BiPredicate<Villager, Holder<PoiType>> bipredicate = POI_MEMORIES.get(p_453763_);
               if (optional.isPresent() && bipredicate.test(this, optional.get())) {
                  poimanager.release(p_455236_.pos());
                  serverlevel.debugSynchronizers().updatePoi(p_455236_.pos());
               }
            }
         });
      }
   }

   @Override
   public boolean canBreed() {
      return this.foodLevel + this.countFoodPointsInInventory() >= 12 && !this.isSleeping() && this.getAge() == 0;
   }

   private boolean hungry() {
      return this.foodLevel < 12;
   }

   private void eatUntilFull() {
      if (this.hungry() && this.countFoodPointsInInventory() != 0) {
         for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack itemstack = this.getInventory().getItem(i);
            if (!itemstack.isEmpty()) {
               Integer integer = FOOD_POINTS.get(itemstack.getItem());
               if (integer != null) {
                  int j = itemstack.getCount();

                  for (int k = j; k > 0; k--) {
                     this.foodLevel = this.foodLevel + integer;
                     this.getInventory().removeItem(i, 1);
                     if (!this.hungry()) {
                        return;
                     }
                  }
               }
            }
         }
      }
   }

   public int getPlayerReputation(Player p_456406_) {
      return this.gossips.getReputation(p_456406_.getUUID(), p_452429_ -> true);
   }

   private void digestFood(int p_454327_) {
      this.foodLevel -= p_454327_;
   }

   public void eatAndDigestFood() {
      this.eatUntilFull();
      this.digestFood(12);
   }

   public void setOffers(MerchantOffers p_453281_) {
      this.offers = p_453281_;
   }

   private boolean shouldIncreaseLevel() {
      int i = this.getVillagerData().level();
      return VillagerData.canLevelUp(i) && this.villagerXp >= VillagerData.getMaxXpPerLevel(i);
   }

   private void increaseMerchantCareer(ServerLevel p_455042_) {
      this.setVillagerData(this.getVillagerData().withLevel(this.getVillagerData().level() + 1));
      this.updateTrades(p_455042_);
   }

   @Override
   protected Component getTypeName() {
      return this.getVillagerData().profession().value().name();
   }

   @Override
   public void handleEntityEvent(byte p_453288_) {
      if (p_453288_ == 12) {
         this.addParticlesAroundSelf(ParticleTypes.HEART);
      } else if (p_453288_ == 13) {
         this.addParticlesAroundSelf(ParticleTypes.ANGRY_VILLAGER);
      } else if (p_453288_ == 14) {
         this.addParticlesAroundSelf(ParticleTypes.HAPPY_VILLAGER);
      } else if (p_453288_ == 42) {
         this.addParticlesAroundSelf(ParticleTypes.SPLASH);
      } else {
         super.handleEntityEvent(p_453288_);
      }
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_459515_, DifficultyInstance p_460937_, EntitySpawnReason p_460438_, @Nullable SpawnGroupData p_457007_
   ) {
      if (p_460438_ == EntitySpawnReason.BREEDING) {
         this.setVillagerData(this.getVillagerData().withProfession(p_459515_.registryAccess(), VillagerProfession.NONE));
      }

      if (p_460438_ == EntitySpawnReason.COMMAND
         || p_460438_ == EntitySpawnReason.SPAWN_ITEM_USE
         || EntitySpawnReason.isSpawner(p_460438_)
         || p_460438_ == EntitySpawnReason.DISPENSER) {
         this.setVillagerData(this.getVillagerData().withType(p_459515_.registryAccess(), VillagerType.byBiome(p_459515_.getBiome(this.blockPosition()))));
      }

      if (p_460438_ == EntitySpawnReason.STRUCTURE) {
         this.assignProfessionWhenSpawned = true;
      }

      return super.finalizeSpawn(p_459515_, p_460937_, p_460438_, p_457007_);
   }

   public @Nullable Villager getBreedOffspring(ServerLevel p_456048_, AgeableMob p_450420_) {
      double d0 = this.random.nextDouble();
      Holder<VillagerType> holder;
      if (d0 < 0.5) {
         holder = p_456048_.registryAccess().getOrThrow(VillagerType.byBiome(p_456048_.getBiome(this.blockPosition())));
      } else if (d0 < 0.75) {
         holder = this.getVillagerData().type();
      } else {
         holder = ((Villager)p_450420_).getVillagerData().type();
      }

      Villager villager = new Villager(EntityType.VILLAGER, p_456048_, holder);
      villager.finalizeSpawn(p_456048_, p_456048_.getCurrentDifficultyAt(villager.blockPosition()), EntitySpawnReason.BREEDING, null);
      return villager;
   }

   @Override
   public void thunderHit(ServerLevel p_455159_, LightningBolt p_455393_) {
      if (p_455159_.getDifficulty() != Difficulty.PEACEFUL) {
         LOGGER.info("Villager {} was struck by lightning {}.", this, p_455393_);
         Witch witch = this.convertTo(EntityType.WITCH, ConversionParams.single(this, false, false), p_451416_ -> {
            p_451416_.finalizeSpawn(p_455159_, p_455159_.getCurrentDifficultyAt(p_451416_.blockPosition()), EntitySpawnReason.CONVERSION, null);
            p_451416_.setPersistenceRequired();
            this.releaseAllPois();
         });
         if (witch == null) {
            super.thunderHit(p_455159_, p_455393_);
         }
      } else {
         super.thunderHit(p_455159_, p_455393_);
      }
   }

   @Override
   protected void pickUpItem(ServerLevel p_451162_, ItemEntity p_461016_) {
      InventoryCarrier.pickUpItem(p_451162_, this, this, p_461016_);
   }

   @Override
   public boolean wantsToPickUp(ServerLevel p_454099_, ItemStack p_452714_) {
      Item item = p_452714_.getItem();
      return (p_452714_.is(ItemTags.VILLAGER_PICKS_UP) || this.getVillagerData().profession().value().requestedItems().contains(item))
         && this.getInventory().canAddItem(p_452714_);
   }

   public boolean hasExcessFood() {
      return this.countFoodPointsInInventory() >= 24;
   }

   public boolean wantsMoreFood() {
      return this.countFoodPointsInInventory() < 12;
   }

   private int countFoodPointsInInventory() {
      SimpleContainer simplecontainer = this.getInventory();
      return FOOD_POINTS.entrySet().stream().mapToInt(p_457644_ -> simplecontainer.countItem(p_457644_.getKey()) * p_457644_.getValue()).sum();
   }

   public boolean hasFarmSeeds() {
      return this.getInventory().hasAnyMatching(p_450309_ -> p_450309_.is(ItemTags.VILLAGER_PLANTABLE_SEEDS));
   }

   @Override
   protected void updateTrades(ServerLevel p_452009_) {
      VillagerData villagerdata = this.getVillagerData();
      ResourceKey<VillagerProfession> resourcekey = villagerdata.profession().unwrapKey().orElse(null);
      if (resourcekey != null) {
         Int2ObjectMap<VillagerTrades.ItemListing[]> int2objectmap;
         if (this.level().enabledFeatures().contains(FeatureFlags.TRADE_REBALANCE)) {
            Int2ObjectMap<VillagerTrades.ItemListing[]> int2objectmap1 = VillagerTrades.EXPERIMENTAL_TRADES.get(resourcekey);
            int2objectmap = int2objectmap1 != null ? int2objectmap1 : VillagerTrades.TRADES.get(resourcekey);
         } else {
            int2objectmap = VillagerTrades.TRADES.get(resourcekey);
         }

         if (int2objectmap != null && !int2objectmap.isEmpty()) {
            VillagerTrades.ItemListing[] avillagertrades$itemlisting = (VillagerTrades.ItemListing[])int2objectmap.get(villagerdata.level());
            if (avillagertrades$itemlisting != null) {
               MerchantOffers merchantoffers = this.getOffers();
               this.addOffersFromItemListings(p_452009_, merchantoffers, avillagertrades$itemlisting, 2);
               if (SharedConstants.DEBUG_UNLOCK_ALL_TRADES && villagerdata.level() < int2objectmap.size()) {
                  this.increaseMerchantCareer(p_452009_);
               }
            }
         }
      }
   }

   public void gossip(ServerLevel p_453174_, Villager p_456943_, long p_454205_) {
      if ((p_454205_ < this.lastGossipTime || p_454205_ >= this.lastGossipTime + 1200L)
         && (p_454205_ < p_456943_.lastGossipTime || p_454205_ >= p_456943_.lastGossipTime + 1200L)) {
         this.gossips.transferFrom(p_456943_.gossips, this.random, 10);
         this.lastGossipTime = p_454205_;
         p_456943_.lastGossipTime = p_454205_;
         this.spawnGolemIfNeeded(p_453174_, p_454205_, 5);
      }
   }

   private void maybeDecayGossip() {
      long i = this.level().getGameTime();
      if (this.lastGossipDecayTime == 0L) {
         this.lastGossipDecayTime = i;
      } else if (i >= this.lastGossipDecayTime + 24000L) {
         this.gossips.decay();
         this.lastGossipDecayTime = i;
      }
   }

   public void spawnGolemIfNeeded(ServerLevel p_454868_, long p_451850_, int p_452678_) {
      if (this.wantsToSpawnGolem(p_451850_)) {
         AABB aabb = this.getBoundingBox().inflate(10.0, 10.0, 10.0);
         List<Villager> list = p_454868_.getEntitiesOfClass(Villager.class, aabb);
         List<Villager> list1 = list.stream().filter(p_456433_ -> p_456433_.wantsToSpawnGolem(p_451850_)).limit(5L).toList();
         if (list1.size() >= p_452678_
            && !SpawnUtil.trySpawnMob(
                  EntityType.IRON_GOLEM, EntitySpawnReason.MOB_SUMMONED, p_454868_, this.blockPosition(), 10, 8, 6, SpawnUtil.Strategy.LEGACY_IRON_GOLEM, false
               )
               .isEmpty()) {
            list.forEach(GolemSensor::golemDetected);
         }
      }
   }

   public boolean wantsToSpawnGolem(long p_458637_) {
      return !this.golemSpawnConditionsMet(this.level().getGameTime()) ? false : !this.brain.hasMemoryValue(MemoryModuleType.GOLEM_DETECTED_RECENTLY);
   }

   @Override
   public void onReputationEventFrom(ReputationEventType p_461011_, Entity p_453626_) {
      if (p_461011_ == ReputationEventType.ZOMBIE_VILLAGER_CURED) {
         this.gossips.add(p_453626_.getUUID(), GossipType.MAJOR_POSITIVE, 20);
         this.gossips.add(p_453626_.getUUID(), GossipType.MINOR_POSITIVE, 25);
      } else if (p_461011_ == ReputationEventType.TRADE) {
         this.gossips.add(p_453626_.getUUID(), GossipType.TRADING, 2);
      } else if (p_461011_ == ReputationEventType.VILLAGER_HURT) {
         this.gossips.add(p_453626_.getUUID(), GossipType.MINOR_NEGATIVE, 25);
      } else if (p_461011_ == ReputationEventType.VILLAGER_KILLED) {
         this.gossips.add(p_453626_.getUUID(), GossipType.MAJOR_NEGATIVE, 25);
      }
   }

   @Override
   public int getVillagerXp() {
      return this.villagerXp;
   }

   public void setVillagerXp(int p_453327_) {
      this.villagerXp = p_453327_;
   }

   private void resetNumberOfRestocks() {
      this.catchUpDemand();
      this.numberOfRestocksToday = 0;
   }

   public GossipContainer getGossips() {
      return this.gossips;
   }

   public void setGossips(GossipContainer p_457666_) {
      this.gossips.putAll(p_457666_);
   }

   @Override
   public void startSleeping(BlockPos p_459835_) {
      super.startSleeping(p_459835_);
      this.brain.setMemory(MemoryModuleType.LAST_SLEPT, this.level().getGameTime());
      this.brain.eraseMemory(MemoryModuleType.WALK_TARGET);
      this.brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
   }

   @Override
   public void stopSleeping() {
      super.stopSleeping();
      this.brain.setMemory(MemoryModuleType.LAST_WOKEN, this.level().getGameTime());
   }

   private boolean golemSpawnConditionsMet(long p_458674_) {
      Optional<Long> optional = this.brain.getMemory(MemoryModuleType.LAST_SLEPT);
      return optional.filter(p_450920_ -> p_458674_ - p_450920_ < 24000L).isPresent();
   }

   @Override
   public <T> @Nullable T get(DataComponentType<? extends T> p_455279_) {
      return p_455279_ == DataComponents.VILLAGER_VARIANT
         ? castComponentValue((DataComponentType<T>)p_455279_, this.getVillagerData().type())
         : super.get(p_455279_);
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_452111_) {
      this.applyImplicitComponentIfPresent(p_452111_, DataComponents.VILLAGER_VARIANT);
      super.applyImplicitComponents(p_452111_);
   }

   @Override
   protected <T> boolean applyImplicitComponent(DataComponentType<T> p_452416_, T p_458270_) {
      if (p_452416_ == DataComponents.VILLAGER_VARIANT) {
         Holder<VillagerType> holder = castComponentValue(DataComponents.VILLAGER_VARIANT, p_458270_);
         this.setVillagerData(this.getVillagerData().withType(holder));
         return true;
      } else {
         return super.applyImplicitComponent(p_452416_, p_458270_);
      }
   }
}
