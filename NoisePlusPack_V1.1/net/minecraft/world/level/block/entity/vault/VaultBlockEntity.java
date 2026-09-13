package net.minecraft.world.level.block.entity.vault;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class VaultBlockEntity extends BlockEntity {
   private final VaultServerData serverData = new VaultServerData();
   private final VaultSharedData sharedData = new VaultSharedData();
   private final VaultClientData clientData = new VaultClientData();
   private VaultConfig config = VaultConfig.DEFAULT;

   public VaultBlockEntity(BlockPos p_329814_, BlockState p_335937_) {
      super(BlockEntityType.VAULT, p_329814_, p_335937_);
   }

   @Override
   public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider p_335952_) {
      return Util.make(
         new CompoundTag(),
         p_391005_ -> p_391005_.store("shared_data", VaultSharedData.CODEC, p_335952_.createSerializationContext(NbtOps.INSTANCE), this.sharedData)
      );
   }

   @Override
   protected void saveAdditional(ValueOutput p_405843_) {
      super.saveAdditional(p_405843_);
      p_405843_.store("config", VaultConfig.CODEC, this.config);
      p_405843_.store("shared_data", VaultSharedData.CODEC, this.sharedData);
      p_405843_.store("server_data", VaultServerData.CODEC, this.serverData);
   }

   @Override
   protected void loadAdditional(ValueInput p_409527_) {
      super.loadAdditional(p_409527_);
      p_409527_.<VaultServerData>read("server_data", VaultServerData.CODEC).ifPresent(this.serverData::set);
      this.config = p_409527_.<VaultConfig>read("config", VaultConfig.CODEC).orElse(VaultConfig.DEFAULT);
      p_409527_.<VaultSharedData>read("shared_data", VaultSharedData.CODEC).ifPresent(this.sharedData::set);
   }

   public @Nullable VaultServerData getServerData() {
      return this.level != null && !this.level.isClientSide() ? this.serverData : null;
   }

   public VaultSharedData getSharedData() {
      return this.sharedData;
   }

   public VaultClientData getClientData() {
      return this.clientData;
   }

   public VaultConfig getConfig() {
      return this.config;
   }

   @VisibleForTesting
   public void setConfig(VaultConfig p_332483_) {
      this.config = p_332483_;
   }

   public static final class Client {
      private static final int PARTICLE_TICK_RATE = 20;
      private static final float IDLE_PARTICLE_CHANCE = 0.5F;
      private static final float AMBIENT_SOUND_CHANCE = 0.02F;
      private static final int ACTIVATION_PARTICLE_COUNT = 20;
      private static final int DEACTIVATION_PARTICLE_COUNT = 20;

      public static void tick(Level p_331255_, BlockPos p_335715_, BlockState p_330773_, VaultClientData p_335986_, VaultSharedData p_333339_) {
         p_335986_.updateDisplayItemSpin();
         if (p_331255_.getGameTime() % 20L == 0L) {
            emitConnectionParticlesForNearbyPlayers(p_331255_, p_335715_, p_330773_, p_333339_);
         }

         emitIdleParticles(p_331255_, p_335715_, p_333339_, p_330773_.getValue(VaultBlock.OMINOUS) ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SMALL_FLAME);
         playIdleSounds(p_331255_, p_335715_, p_333339_);
      }

      public static void emitActivationParticles(
         Level p_329048_, BlockPos p_334504_, BlockState p_328465_, VaultSharedData p_331322_, ParticleOptions p_332937_
      ) {
         emitConnectionParticlesForNearbyPlayers(p_329048_, p_334504_, p_328465_, p_331322_);
         RandomSource randomsource = p_329048_.random;

         for (int i = 0; i < 20; i++) {
            Vec3 vec3 = randomPosInsideCage(p_334504_, randomsource);
            p_329048_.addParticle(ParticleTypes.SMOKE, vec3.x(), vec3.y(), vec3.z(), 0.0, 0.0, 0.0);
            p_329048_.addParticle(p_332937_, vec3.x(), vec3.y(), vec3.z(), 0.0, 0.0, 0.0);
         }
      }

      public static void emitDeactivationParticles(Level p_330549_, BlockPos p_334754_, ParticleOptions p_335199_) {
         RandomSource randomsource = p_330549_.random;

         for (int i = 0; i < 20; i++) {
            Vec3 vec3 = randomPosCenterOfCage(p_334754_, randomsource);
            Vec3 vec31 = new Vec3(randomsource.nextGaussian() * 0.02, randomsource.nextGaussian() * 0.02, randomsource.nextGaussian() * 0.02);
            p_330549_.addParticle(p_335199_, vec3.x(), vec3.y(), vec3.z(), vec31.x(), vec31.y(), vec31.z());
         }
      }

      private static void emitIdleParticles(Level p_329901_, BlockPos p_330744_, VaultSharedData p_332348_, ParticleOptions p_333563_) {
         RandomSource randomsource = p_329901_.getRandom();
         if (randomsource.nextFloat() <= 0.5F) {
            Vec3 vec3 = randomPosInsideCage(p_330744_, randomsource);
            p_329901_.addParticle(ParticleTypes.SMOKE, vec3.x(), vec3.y(), vec3.z(), 0.0, 0.0, 0.0);
            if (shouldDisplayActiveEffects(p_332348_)) {
               p_329901_.addParticle(p_333563_, vec3.x(), vec3.y(), vec3.z(), 0.0, 0.0, 0.0);
            }
         }
      }

      private static void emitConnectionParticlesForPlayer(Level p_327765_, Vec3 p_335116_, Player p_333131_) {
         RandomSource randomsource = p_327765_.random;
         Vec3 vec3 = p_335116_.vectorTo(p_333131_.position().add(0.0, p_333131_.getBbHeight() / 2.0F, 0.0));
         int i = Mth.nextInt(randomsource, 2, 5);

         for (int j = 0; j < i; j++) {
            Vec3 vec31 = vec3.offsetRandom(randomsource, 1.0F);
            p_327765_.addParticle(ParticleTypes.VAULT_CONNECTION, p_335116_.x(), p_335116_.y(), p_335116_.z(), vec31.x(), vec31.y(), vec31.z());
         }
      }

      private static void emitConnectionParticlesForNearbyPlayers(Level p_329933_, BlockPos p_335364_, BlockState p_330110_, VaultSharedData p_332177_) {
         Set<UUID> set = p_332177_.getConnectedPlayers();
         if (!set.isEmpty()) {
            Vec3 vec3 = keyholePos(p_335364_, p_330110_.getValue(VaultBlock.FACING));

            for (UUID uuid : set) {
               Player player = p_329933_.getPlayerByUUID(uuid);
               if (player != null && isWithinConnectionRange(p_335364_, p_332177_, player)) {
                  emitConnectionParticlesForPlayer(p_329933_, vec3, player);
               }
            }
         }
      }

      private static boolean isWithinConnectionRange(BlockPos p_334746_, VaultSharedData p_334927_, Player p_333038_) {
         return p_333038_.blockPosition().distSqr(p_334746_) <= Mth.square(p_334927_.connectedParticlesRange());
      }

      private static void playIdleSounds(Level p_329850_, BlockPos p_333501_, VaultSharedData p_332082_) {
         if (shouldDisplayActiveEffects(p_332082_)) {
            RandomSource randomsource = p_329850_.getRandom();
            if (randomsource.nextFloat() <= 0.02F) {
               p_329850_.playLocalSound(
                  p_333501_, SoundEvents.VAULT_AMBIENT, SoundSource.BLOCKS, randomsource.nextFloat() * 0.25F + 0.75F, randomsource.nextFloat() + 0.5F, false
               );
            }
         }
      }

      public static boolean shouldDisplayActiveEffects(VaultSharedData p_329617_) {
         return p_329617_.hasDisplayItem();
      }

      private static Vec3 randomPosCenterOfCage(BlockPos p_329856_, RandomSource p_333945_) {
         return Vec3.atLowerCornerOf(p_329856_)
            .add(Mth.nextDouble(p_333945_, 0.4, 0.6), Mth.nextDouble(p_333945_, 0.4, 0.6), Mth.nextDouble(p_333945_, 0.4, 0.6));
      }

      private static Vec3 randomPosInsideCage(BlockPos p_327884_, RandomSource p_332986_) {
         return Vec3.atLowerCornerOf(p_327884_)
            .add(Mth.nextDouble(p_332986_, 0.1, 0.9), Mth.nextDouble(p_332986_, 0.25, 0.75), Mth.nextDouble(p_332986_, 0.1, 0.9));
      }

      private static Vec3 keyholePos(BlockPos p_331540_, Direction p_333034_) {
         return Vec3.atBottomCenterOf(p_331540_).add(p_333034_.getStepX() * 0.5, 1.75, p_333034_.getStepZ() * 0.5);
      }
   }

   public static final class Server {
      private static final int UNLOCKING_DELAY_TICKS = 14;
      private static final int DISPLAY_CYCLE_TICK_RATE = 20;
      private static final int INSERT_FAIL_SOUND_BUFFER_TICKS = 15;

      public static void tick(
         ServerLevel p_327862_, BlockPos p_334036_, BlockState p_336094_, VaultConfig p_332912_, VaultServerData p_332613_, VaultSharedData p_336360_
      ) {
         VaultState vaultstate = p_336094_.getValue(VaultBlock.STATE);
         if (shouldCycleDisplayItem(p_327862_.getGameTime(), vaultstate)) {
            cycleDisplayItemFromLootTable(p_327862_, vaultstate, p_332912_, p_336360_, p_334036_);
         }

         BlockState blockstate = p_336094_;
         if (p_327862_.getGameTime() >= p_332613_.stateUpdatingResumesAt()) {
            blockstate = p_336094_.setValue(VaultBlock.STATE, vaultstate.tickAndGetNext(p_327862_, p_334036_, p_332912_, p_332613_, p_336360_));
            if (p_336094_ != blockstate) {
               setVaultState(p_327862_, p_334036_, p_336094_, blockstate, p_332912_, p_336360_);
            }
         }

         if (p_332613_.isDirty || p_336360_.isDirty) {
            VaultBlockEntity.setChanged(p_327862_, p_334036_, p_336094_);
            if (p_336360_.isDirty) {
               p_327862_.sendBlockUpdated(p_334036_, p_336094_, blockstate, 2);
            }

            p_332613_.isDirty = false;
            p_336360_.isDirty = false;
         }
      }

      public static void tryInsertKey(
         ServerLevel p_330813_,
         BlockPos p_333223_,
         BlockState p_331301_,
         VaultConfig p_333877_,
         VaultServerData p_334388_,
         VaultSharedData p_330336_,
         Player p_332764_,
         ItemStack p_329896_
      ) {
         VaultState vaultstate = p_331301_.getValue(VaultBlock.STATE);
         if (canEjectReward(p_333877_, vaultstate)) {
            if (!isValidToInsert(p_333877_, p_329896_)) {
               playInsertFailSound(p_330813_, p_334388_, p_333223_, SoundEvents.VAULT_INSERT_ITEM_FAIL);
            } else if (p_334388_.hasRewardedPlayer(p_332764_)) {
               playInsertFailSound(p_330813_, p_334388_, p_333223_, SoundEvents.VAULT_REJECT_REWARDED_PLAYER);
            } else {
               List<ItemStack> list = resolveItemsToEject(p_330813_, p_333877_, p_333223_, p_332764_, p_329896_);
               if (!list.isEmpty()) {
                  p_332764_.awardStat(Stats.ITEM_USED.get(p_329896_.getItem()));
                  p_329896_.consume(p_333877_.keyItem().getCount(), p_332764_);
                  unlock(p_330813_, p_331301_, p_333223_, p_333877_, p_334388_, p_330336_, list);
                  p_334388_.addToRewardedPlayers(p_332764_);
                  p_330336_.updateConnectedPlayersWithinRange(p_330813_, p_333223_, p_334388_, p_333877_, p_333877_.deactivationRange());
               }
            }
         }
      }

      static void setVaultState(
         ServerLevel p_327709_, BlockPos p_330897_, BlockState p_333801_, BlockState p_336357_, VaultConfig p_332945_, VaultSharedData p_328872_
      ) {
         VaultState vaultstate = p_333801_.getValue(VaultBlock.STATE);
         VaultState vaultstate1 = p_336357_.getValue(VaultBlock.STATE);
         p_327709_.setBlock(p_330897_, p_336357_, 3);
         vaultstate.onTransition(p_327709_, p_330897_, vaultstate1, p_332945_, p_328872_, p_336357_.getValue(VaultBlock.OMINOUS));
      }

      static void cycleDisplayItemFromLootTable(
         ServerLevel p_328186_, VaultState p_335064_, VaultConfig p_329242_, VaultSharedData p_336318_, BlockPos p_327920_
      ) {
         if (!canEjectReward(p_329242_, p_335064_)) {
            p_336318_.setDisplayItem(ItemStack.EMPTY);
         } else {
            ItemStack itemstack = getRandomDisplayItemFromLootTable(p_328186_, p_327920_, p_329242_.overrideLootTableToDisplay().orElse(p_329242_.lootTable()));
            p_336318_.setDisplayItem(itemstack);
         }
      }

      private static ItemStack getRandomDisplayItemFromLootTable(ServerLevel p_329309_, BlockPos p_331772_, ResourceKey<LootTable> p_327947_) {
         LootTable loottable = p_329309_.getServer().reloadableRegistries().getLootTable(p_327947_);
         LootParams lootparams = new LootParams.Builder(p_329309_)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(p_331772_))
            .create(LootContextParamSets.VAULT);
         List<ItemStack> list = loottable.getRandomItems(lootparams, p_329309_.getRandom());
         return list.isEmpty() ? ItemStack.EMPTY : Util.getRandom(list, p_329309_.getRandom());
      }

      private static void unlock(
         ServerLevel p_329025_,
         BlockState p_334542_,
         BlockPos p_331457_,
         VaultConfig p_328759_,
         VaultServerData p_329258_,
         VaultSharedData p_328090_,
         List<ItemStack> p_328105_
      ) {
         p_329258_.setItemsToEject(p_328105_);
         p_328090_.setDisplayItem(p_329258_.getNextItemToEject());
         p_329258_.pauseStateUpdatingUntil(p_329025_.getGameTime() + 14L);
         setVaultState(p_329025_, p_331457_, p_334542_, p_334542_.setValue(VaultBlock.STATE, VaultState.UNLOCKING), p_328759_, p_328090_);
      }

      private static List<ItemStack> resolveItemsToEject(
         ServerLevel p_332295_, VaultConfig p_329503_, BlockPos p_333443_, Player p_334837_, ItemStack p_369229_
      ) {
         LootTable loottable = p_332295_.getServer().reloadableRegistries().getLootTable(p_329503_.lootTable());
         LootParams lootparams = new LootParams.Builder(p_332295_)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(p_333443_))
            .withLuck(p_334837_.getLuck())
            .withParameter(LootContextParams.THIS_ENTITY, p_334837_)
            .withParameter(LootContextParams.TOOL, p_369229_)
            .create(LootContextParamSets.VAULT);
         return loottable.getRandomItems(lootparams);
      }

      private static boolean canEjectReward(VaultConfig p_333220_, VaultState p_335172_) {
         return !p_333220_.keyItem().isEmpty() && p_335172_ != VaultState.INACTIVE;
      }

      private static boolean isValidToInsert(VaultConfig p_334332_, ItemStack p_335056_) {
         return ItemStack.isSameItemSameComponents(p_335056_, p_334332_.keyItem()) && p_335056_.getCount() >= p_334332_.keyItem().getCount();
      }

      private static boolean shouldCycleDisplayItem(long p_334702_, VaultState p_332761_) {
         return p_334702_ % 20L == 0L && p_332761_ == VaultState.ACTIVE;
      }

      private static void playInsertFailSound(ServerLevel p_334677_, VaultServerData p_330421_, BlockPos p_330460_, SoundEvent p_342956_) {
         if (p_334677_.getGameTime() >= p_330421_.getLastInsertFailTimestamp() + 15L) {
            p_334677_.playSound(null, p_330460_, p_342956_, SoundSource.BLOCKS);
            p_330421_.setLastInsertFailTimestamp(p_334677_.getGameTime());
         }
      }
   }
}
