package net.minecraft.world.entity.monster.zombie;

import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.gossip.GossipContainer;
import net.minecraft.world.entity.ai.village.ReputationEventType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerDataHolder;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class ZombieVillager extends Zombie implements VillagerDataHolder {
   private static final EntityDataAccessor<Boolean> DATA_CONVERTING_ID = SynchedEntityData.defineId(ZombieVillager.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<VillagerData> DATA_VILLAGER_DATA = SynchedEntityData.defineId(
      ZombieVillager.class, EntityDataSerializers.VILLAGER_DATA
   );
   private static final EntityDataAccessor<Boolean> DATA_VILLAGER_DATA_FINALIZED = SynchedEntityData.defineId(
      ZombieVillager.class, EntityDataSerializers.BOOLEAN
   );
   private static final int VILLAGER_CONVERSION_WAIT_MIN = 3600;
   private static final int VILLAGER_CONVERSION_WAIT_MAX = 6000;
   private static final int MAX_SPECIAL_BLOCKS_COUNT = 14;
   private static final int SPECIAL_BLOCK_RADIUS = 4;
   private static final int NOT_CONVERTING = -1;
   private static final int DEFAULT_XP = 0;
   private int villagerConversionTime;
   private @Nullable UUID conversionStarter;
   private @Nullable GossipContainer gossips;
   private @Nullable MerchantOffers tradeOffers;
   private int villagerXp = 0;
   private static final EntityDimensions BABY_DIMENSIONS = EntityDimensions.scalable(0.49F, 0.98F)
      .withEyeHeight(0.67F)
      .withAttachments(EntityAttachments.builder().attach(EntityAttachment.VEHICLE, 0.0F, 0.125F, 0.0F));

   public ZombieVillager(final EntityType<? extends ZombieVillager> type, final Level level) {
      super(type, level);
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(DATA_CONVERTING_ID, false);
      entityData.define(DATA_VILLAGER_DATA, initializeZombieVillagerData(this.random));
      entityData.define(DATA_VILLAGER_DATA_FINALIZED, false);
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.store("VillagerData", VillagerData.CODEC, this.getVillagerData());
      output.putBoolean("VillagerDataFinalized", this.getVillagerDataFinalized());
      output.storeNullable("Offers", MerchantOffers.CODEC, this.tradeOffers);
      output.storeNullable("Gossips", GossipContainer.CODEC, this.gossips);
      output.putInt("ConversionTime", this.isConverting() ? this.villagerConversionTime : -1);
      output.storeNullable("ConversionPlayer", UUIDUtil.CODEC, this.conversionStarter);
      output.putInt("Xp", this.villagerXp);
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      Optional<VillagerData> villagerDataOptional = input.read("VillagerData", VillagerData.CODEC);
      if (input.getBooleanOr("VillagerDataFinalized", false) || villagerDataOptional.isPresent()) {
         this.setVillagerDataFinalized(true);
         VillagerData villagerData = villagerDataOptional.orElseGet(() -> initializeZombieVillagerData(this.random));
         this.entityData.set(DATA_VILLAGER_DATA, villagerData);
      }

      this.tradeOffers = input.<MerchantOffers>read("Offers", MerchantOffers.CODEC).orElse(null);
      this.gossips = input.<GossipContainer>read("Gossips", GossipContainer.CODEC).orElse(null);
      int conversionTime = input.getIntOr("ConversionTime", -1);
      if (conversionTime != -1) {
         UUID conversionStarter = input.<UUID>read("ConversionPlayer", UUIDUtil.CODEC).orElse(null);
         this.startConverting(conversionStarter, conversionTime);
      } else {
         this.getEntityData().set(DATA_CONVERTING_ID, false);
         this.villagerConversionTime = -1;
      }

      this.villagerXp = input.getIntOr("Xp", 0);
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, final @Nullable SpawnGroupData groupData
   ) {
      this.finalizeVillagerType(level, this.blockPosition());
      return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
   }

   private static VillagerData initializeZombieVillagerData(final RandomSource random) {
      VillagerData villagerData = Villager.createDefaultVillagerData();
      Optional<Holder.Reference<VillagerProfession>> profession = BuiltInRegistries.VILLAGER_PROFESSION.getRandom(random);
      if (profession.isPresent()) {
         villagerData = villagerData.withProfession(profession.get());
      }

      return villagerData;
   }

   @Override
   public void tick() {
      if (!this.level().isClientSide() && this.isAlive() && this.isConverting()) {
         int amount = this.getConversionProgress();
         this.villagerConversionTime -= amount;
         if (this.villagerConversionTime <= 0) {
            this.finishConversion((ServerLevel)this.level());
         }
      }

      super.tick();
   }

   @Override
   public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      if (itemStack.is(Items.GOLDEN_APPLE)) {
         if (this.hasEffect(MobEffects.WEAKNESS)) {
            itemStack.consume(1, player);
            if (!this.level().isClientSide()) {
               this.startConverting(player.getUUID(), this.random.nextInt(2401) + 3600);
            }

            return InteractionResult.SUCCESS_SERVER;
         } else {
            return InteractionResult.CONSUME;
         }
      } else {
         return super.mobInteract(player, hand);
      }
   }

   @Override
   protected boolean convertsInWater() {
      return false;
   }

   @Override
   public boolean removeWhenFarAway(final double distSqr) {
      return !this.isConverting() && this.villagerXp == 0;
   }

   public boolean isConverting() {
      return this.getEntityData().get(DATA_CONVERTING_ID);
   }

   private void startConverting(final @Nullable UUID player, final int time) {
      this.conversionStarter = player;
      this.villagerConversionTime = time;
      this.getEntityData().set(DATA_CONVERTING_ID, true);
      this.removeEffect(MobEffects.WEAKNESS);
      this.addEffect(new MobEffectInstance(MobEffects.STRENGTH, time, Math.min(this.level().getDifficulty().getId() - 1, 0)));
      this.level().broadcastEntityEvent(this, (byte)16);
   }

   @Override
   public EntityDimensions getDefaultDimensions(final Pose pose) {
      return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
   }

   @Override
   public void handleEntityEvent(final byte id) {
      if (id == 16) {
         if (!this.isSilent()) {
            this.level()
               .playLocalSound(
                  this.getX(),
                  this.getEyeY(),
                  this.getZ(),
                  SoundEvents.ZOMBIE_VILLAGER_CURE,
                  this.getSoundSource(),
                  1.0F + this.random.nextFloat(),
                  this.random.nextFloat() * 0.7F + 0.3F,
                  false
               );
         }
      } else {
         super.handleEntityEvent(id);
      }
   }

   private void finishConversion(final ServerLevel level) {
      this.convertTo(
         EntityTypes.VILLAGER,
         ConversionParams.single(this, false, false),
         villager -> {
            for (EquipmentSlot undroppedSlot : this.dropPreservedEquipment(
               level, stack -> !EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)
            )) {
               SlotAccess offsetSlot = villager.getSlot(undroppedSlot.getIndex() + 300);
               if (offsetSlot != null) {
                  offsetSlot.set(this.getItemBySlot(undroppedSlot));
               }
            }

            villager.setVillagerDataFinalized(this.getVillagerDataFinalized());
            villager.setVillagerData(this.getVillagerData());
            if (this.gossips != null) {
               villager.setGossips(this.gossips);
            }

            if (this.tradeOffers != null) {
               villager.setOffers(this.tradeOffers.copy());
            }

            villager.setVillagerXp(this.villagerXp);
            villager.finalizeSpawn(level, level.getCurrentDifficultyAt(villager.blockPosition()), EntitySpawnReason.CONVERSION, null);
            villager.refreshBrain(level);
            if (this.conversionStarter != null) {
               Player player = level.getPlayerByUUID(this.conversionStarter);
               if (player instanceof ServerPlayer serverPlayer) {
                  CriteriaTriggers.CURED_ZOMBIE_VILLAGER.trigger(serverPlayer, this, villager);
                  level.onReputationEvent(ReputationEventType.ZOMBIE_VILLAGER_CURED, player, villager);
               }
            }

            villager.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
            if (!this.isSilent()) {
               level.levelEvent(null, 1027, this.blockPosition(), 0);
            }
         }
      );
   }

   @VisibleForTesting
   public void setVillagerConversionTime(final int conversionTime) {
      this.villagerConversionTime = conversionTime;
   }

   private int getConversionProgress() {
      int amount = 1;
      if (this.random.nextFloat() < 0.01F) {
         int specialBlocksCount = 0;
         BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

         for (int xx = (int)this.getX() - 4; xx < (int)this.getX() + 4 && specialBlocksCount < 14; xx++) {
            for (int yy = (int)this.getY() - 4; yy < (int)this.getY() + 4 && specialBlocksCount < 14; yy++) {
               for (int zz = (int)this.getZ() - 4; zz < (int)this.getZ() + 4 && specialBlocksCount < 14; zz++) {
                  BlockState state = this.level().getBlockState(blockPos.set(xx, yy, zz));
                  if (state.is(BlockTags.SPEEDS_UP_ZOMBIE_VILLAGER_CURING)) {
                     if (this.random.nextFloat() < 0.3F) {
                        amount++;
                     }

                     specialBlocksCount++;
                  }
               }
            }
         }
      }

      return amount;
   }

   @Override
   public float getVoicePitch() {
      return this.isBaby()
         ? (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 2.0F
         : (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F;
   }

   @Override
   public SoundEvent getAmbientSound() {
      return SoundEvents.ZOMBIE_VILLAGER_AMBIENT;
   }

   @Override
   public SoundEvent getHurtSound(final DamageSource source) {
      return SoundEvents.ZOMBIE_VILLAGER_HURT;
   }

   @Override
   public SoundEvent getDeathSound() {
      return SoundEvents.ZOMBIE_VILLAGER_DEATH;
   }

   @Override
   public SoundEvent getStepSound() {
      return SoundEvents.ZOMBIE_VILLAGER_STEP;
   }

   public void setTradeOffers(final MerchantOffers tradeOffers) {
      this.tradeOffers = tradeOffers;
   }

   public void setGossips(final GossipContainer gossips) {
      this.gossips = gossips;
   }

   @Override
   public void setVillagerData(final VillagerData villagerData) {
      VillagerData currentData = this.getVillagerData();
      if (!currentData.profession().equals(villagerData.profession())) {
         this.tradeOffers = null;
      }

      this.entityData.set(DATA_VILLAGER_DATA, villagerData);
   }

   @Override
   public boolean getVillagerDataFinalized() {
      return this.entityData.get(DATA_VILLAGER_DATA_FINALIZED);
   }

   @Override
   public void setVillagerDataFinalized(final boolean villagerDataFinalized) {
      this.entityData.set(DATA_VILLAGER_DATA_FINALIZED, villagerDataFinalized);
   }

   @Override
   public VillagerData getVillagerData() {
      return this.entityData.get(DATA_VILLAGER_DATA);
   }

   public int getVillagerXp() {
      return this.villagerXp;
   }

   public void setVillagerXp(final int villagerXp) {
      this.villagerXp = villagerXp;
   }

   @Override
   public <T> @Nullable T get(final DataComponentType<? extends T> type) {
      return type == DataComponents.VILLAGER_VARIANT ? castComponentValue((DataComponentType<T>)type, this.getVillagerData().type()) : super.get(type);
   }

   @Override
   protected void applyImplicitComponents(final DataComponentGetter components) {
      this.applyImplicitComponentIfPresent(components, DataComponents.VILLAGER_VARIANT);
      super.applyImplicitComponents(components);
   }

   @Override
   protected <T> boolean applyImplicitComponent(final DataComponentType<T> type, final T value) {
      if (type == DataComponents.VILLAGER_VARIANT) {
         Holder<VillagerType> variant = castComponentValue(DataComponents.VILLAGER_VARIANT, value);
         this.setVillagerData(this.getVillagerData().withType(variant));
         this.setVillagerDataFinalized(true);
         return true;
      } else {
         return super.applyImplicitComponent(type, value);
      }
   }
}
