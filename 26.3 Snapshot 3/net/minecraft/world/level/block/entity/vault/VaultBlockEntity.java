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
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
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

   public VaultBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      super(BlockEntityTypes.VAULT, worldPosition, blockState);
   }

   @Override
   public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
      return Util.make(
         new CompoundTag(), tag -> tag.store("shared_data", VaultSharedData.CODEC, registries.createSerializationContext(NbtOps.INSTANCE), this.sharedData)
      );
   }

   @Override
   protected void saveAdditional(final ValueOutput output) {
      super.saveAdditional(output);
      output.store("config", VaultConfig.CODEC, this.config);
      output.store("shared_data", VaultSharedData.CODEC, this.sharedData);
      output.store("server_data", VaultServerData.CODEC, this.serverData);
   }

   @Override
   protected void loadAdditional(final ValueInput input) {
      super.loadAdditional(input);
      input.<VaultServerData>read("server_data", VaultServerData.CODEC).ifPresent(this.serverData::set);
      this.config = input.<VaultConfig>read("config", VaultConfig.CODEC).orElse(VaultConfig.DEFAULT);
      input.<VaultSharedData>read("shared_data", VaultSharedData.CODEC).ifPresent(this.sharedData::set);
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
   public void setConfig(final VaultConfig config) {
      this.config = config;
   }

   public static final class Client {
      private static final int PARTICLE_TICK_RATE = 20;
      private static final float IDLE_PARTICLE_CHANCE = 0.5F;
      private static final float AMBIENT_SOUND_CHANCE = 0.02F;
      private static final int ACTIVATION_PARTICLE_COUNT = 20;
      private static final int DEACTIVATION_PARTICLE_COUNT = 20;

      public static void tick(
         final Level clientLevel, final BlockPos pos, final BlockState blockState, final VaultClientData clientData, final VaultSharedData sharedData
      ) {
         clientData.updateDisplayItemSpin();
         if (clientLevel.getGameTime() % 20L == 0L) {
            emitConnectionParticlesForNearbyPlayers(clientLevel, pos, blockState, sharedData);
         }

         emitIdleParticles(clientLevel, pos, sharedData, blockState.getValue(VaultBlock.OMINOUS) ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SMALL_FLAME);
         playIdleSounds(clientLevel, pos, sharedData);
      }

      public static void emitActivationParticles(
         final Level clientLevel, final BlockPos pos, final BlockState blockState, final VaultSharedData sharedData, final ParticleOptions flameParticle
      ) {
         emitConnectionParticlesForNearbyPlayers(clientLevel, pos, blockState, sharedData);
         RandomSource random = clientLevel.getRandom();

         for (int i = 0; i < 20; i++) {
            Vec3 particlePos = randomPosInsideCage(pos, random);
            clientLevel.addParticle(ParticleTypes.SMOKE, particlePos.x(), particlePos.y(), particlePos.z(), 0.0, 0.0, 0.0);
            clientLevel.addParticle(flameParticle, particlePos.x(), particlePos.y(), particlePos.z(), 0.0, 0.0, 0.0);
         }
      }

      public static void emitDeactivationParticles(final Level clientLevel, final BlockPos pos, final ParticleOptions flameParticle) {
         RandomSource random = clientLevel.getRandom();

         for (int i = 0; i < 20; i++) {
            Vec3 particlePos = randomPosCenterOfCage(pos, random);
            Vec3 dir = new Vec3(random.nextGaussian() * 0.02, random.nextGaussian() * 0.02, random.nextGaussian() * 0.02);
            clientLevel.addParticle(flameParticle, particlePos.x(), particlePos.y(), particlePos.z(), dir.x(), dir.y(), dir.z());
         }
      }

      private static void emitIdleParticles(final Level clientLevel, final BlockPos pos, final VaultSharedData sharedData, final ParticleOptions flameParticle) {
         RandomSource random = clientLevel.getRandom();
         if (random.nextFloat() <= 0.5F) {
            Vec3 particlePos = randomPosInsideCage(pos, random);
            clientLevel.addParticle(ParticleTypes.SMOKE, particlePos.x(), particlePos.y(), particlePos.z(), 0.0, 0.0, 0.0);
            if (shouldDisplayActiveEffects(sharedData)) {
               clientLevel.addParticle(flameParticle, particlePos.x(), particlePos.y(), particlePos.z(), 0.0, 0.0, 0.0);
            }
         }
      }

      private static void emitConnectionParticlesForPlayer(final Level level, final Vec3 flyTowards, final Player player) {
         RandomSource random = level.getRandom();
         Vec3 direction = flyTowards.vectorTo(player.position().add(0.0, player.getBbHeight() / 2.0F, 0.0));
         int particleCount = Mth.nextInt(random, 2, 5);

         for (int i = 0; i < particleCount; i++) {
            Vec3 randomDirection = direction.offsetRandom(random, 1.0F);
            level.addParticle(
               ParticleTypes.VAULT_CONNECTION, flyTowards.x(), flyTowards.y(), flyTowards.z(), randomDirection.x(), randomDirection.y(), randomDirection.z()
            );
         }
      }

      private static void emitConnectionParticlesForNearbyPlayers(
         final Level level, final BlockPos pos, final BlockState blockState, final VaultSharedData sharedData
      ) {
         Set<UUID> connectedPlayers = sharedData.getConnectedPlayers();
         if (!connectedPlayers.isEmpty()) {
            Vec3 keyholePos = keyholePos(pos, blockState.getValue(VaultBlock.FACING));

            for (UUID uuid : connectedPlayers) {
               Player player = level.getPlayerByUUID(uuid);
               if (player != null && isWithinConnectionRange(pos, sharedData, player)) {
                  emitConnectionParticlesForPlayer(level, keyholePos, player);
               }
            }
         }
      }

      private static boolean isWithinConnectionRange(final BlockPos vaultPos, final VaultSharedData sharedData, final Player player) {
         return player.blockPosition().distSqr(vaultPos) <= Mth.square(sharedData.connectedParticlesRange());
      }

      private static void playIdleSounds(final Level clientLevel, final BlockPos pos, final VaultSharedData sharedData) {
         if (shouldDisplayActiveEffects(sharedData)) {
            RandomSource random = clientLevel.getRandom();
            if (random.nextFloat() <= 0.02F) {
               clientLevel.playLocalSound(
                  pos, SoundEvents.VAULT_AMBIENT, SoundSource.BLOCKS, random.nextFloat() * 0.25F + 0.75F, random.nextFloat() + 0.5F, false
               );
            }
         }
      }

      public static boolean shouldDisplayActiveEffects(final VaultSharedData sharedData) {
         return sharedData.hasDisplayItem();
      }

      private static Vec3 randomPosCenterOfCage(final BlockPos blockPos, final RandomSource random) {
         return Vec3.atLowerCornerOf(blockPos).add(Mth.nextDouble(random, 0.4, 0.6), Mth.nextDouble(random, 0.4, 0.6), Mth.nextDouble(random, 0.4, 0.6));
      }

      private static Vec3 randomPosInsideCage(final BlockPos blockPos, final RandomSource random) {
         return Vec3.atLowerCornerOf(blockPos).add(Mth.nextDouble(random, 0.1, 0.9), Mth.nextDouble(random, 0.25, 0.75), Mth.nextDouble(random, 0.1, 0.9));
      }

      private static Vec3 keyholePos(final BlockPos blockPos, final Direction blockFacing) {
         return Vec3.atBottomCenterOf(blockPos).add(blockFacing.getStepX() * 0.5, 1.75, blockFacing.getStepZ() * 0.5);
      }
   }

   public static final class Server {
      private static final int UNLOCKING_DELAY_TICKS = 14;
      private static final int DISPLAY_CYCLE_TICK_RATE = 20;
      private static final int INSERT_FAIL_SOUND_BUFFER_TICKS = 15;

      public static void tick(
         final ServerLevel serverLevel,
         final BlockPos pos,
         final BlockState blockState,
         final VaultConfig config,
         final VaultServerData serverData,
         final VaultSharedData sharedData
      ) {
         VaultState currentState = blockState.getValue(VaultBlock.STATE);
         if (shouldCycleDisplayItem(serverLevel.getGameTime(), currentState)) {
            cycleDisplayItemFromLootTable(serverLevel, currentState, config, sharedData, pos);
         }

         BlockState nextBlockState = blockState;
         if (serverLevel.getGameTime() >= serverData.stateUpdatingResumesAt()) {
            nextBlockState = nextBlockState.setValue(VaultBlock.STATE, currentState.tickAndGetNext(serverLevel, pos, config, serverData, sharedData));
            if (blockState != nextBlockState) {
               setVaultState(serverLevel, pos, blockState, nextBlockState, config, sharedData);
            }
         }

         if (serverData.isDirty || sharedData.isDirty) {
            VaultBlockEntity.setChanged(serverLevel, pos, blockState);
            if (sharedData.isDirty) {
               serverLevel.sendBlockUpdated(pos, blockState, nextBlockState, 2);
            }

            serverData.isDirty = false;
            sharedData.isDirty = false;
         }
      }

      public static void tryInsertKey(
         final ServerLevel serverLevel,
         final BlockPos pos,
         final BlockState blockState,
         final VaultConfig config,
         final VaultServerData serverData,
         final VaultSharedData sharedData,
         final Player player,
         final ItemStack stackToInsert
      ) {
         VaultState vaultState = blockState.getValue(VaultBlock.STATE);
         if (canEjectReward(config, vaultState)) {
            if (!isValidToInsert(config, stackToInsert)) {
               playInsertFailSound(serverLevel, serverData, pos, SoundEvents.VAULT_INSERT_ITEM_FAIL);
            } else if (serverData.hasRewardedPlayer(player)) {
               playInsertFailSound(serverLevel, serverData, pos, SoundEvents.VAULT_REJECT_REWARDED_PLAYER);
            } else {
               List<ItemStack> itemsToEject = resolveItemsToEject(serverLevel, config, pos, player, stackToInsert);
               if (!itemsToEject.isEmpty()) {
                  player.awardStat(Stats.ITEM_USED.get(stackToInsert.getItem()));
                  stackToInsert.consume(config.keyItem().getCount(), player);
                  unlock(serverLevel, blockState, pos, config, serverData, sharedData, itemsToEject);
                  serverData.addToRewardedPlayers(player);
                  sharedData.updateConnectedPlayersWithinRange(serverLevel, pos, serverData, config, config.deactivationRange());
               }
            }
         }
      }

      private static void setVaultState(
         final ServerLevel serverLevel,
         final BlockPos pos,
         final BlockState currentBlockState,
         final BlockState newBlockState,
         final VaultConfig config,
         final VaultSharedData sharedData
      ) {
         VaultState currentVaultState = currentBlockState.getValue(VaultBlock.STATE);
         VaultState newVaultState = newBlockState.getValue(VaultBlock.STATE);
         serverLevel.setBlockAndUpdate(pos, newBlockState);
         currentVaultState.onTransition(serverLevel, pos, newVaultState, config, sharedData, newBlockState.getValue(VaultBlock.OMINOUS));
      }

      static void cycleDisplayItemFromLootTable(
         final ServerLevel serverLevel, final VaultState vaultState, final VaultConfig config, final VaultSharedData sharedData, final BlockPos pos
      ) {
         if (!canEjectReward(config, vaultState)) {
            sharedData.setDisplayItem(ItemStack.EMPTY);
         } else {
            ItemStack displayItem = getRandomDisplayItemFromLootTable(serverLevel, pos, config.overrideLootTableToDisplay().orElse(config.lootTable()));
            sharedData.setDisplayItem(displayItem);
         }
      }

      private static ItemStack getRandomDisplayItemFromLootTable(final ServerLevel serverLevel, final BlockPos pos, final ResourceKey<LootTable> lootTableId) {
         LootTable lootTable = serverLevel.getServer().reloadableRegistries().getLootTable(lootTableId);
         LootParams params = new LootParams.Builder(serverLevel)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
            .create(LootContextParamSets.VAULT);
         List<ItemStack> results = lootTable.getRandomItems(params, serverLevel.getRandom());
         return results.isEmpty() ? ItemStack.EMPTY : Util.getRandom(results, serverLevel.getRandom());
      }

      private static void unlock(
         final ServerLevel serverLevel,
         final BlockState blockState,
         final BlockPos pos,
         final VaultConfig config,
         final VaultServerData serverData,
         final VaultSharedData sharedData,
         final List<ItemStack> itemsToEject
      ) {
         serverData.setItemsToEject(itemsToEject);
         sharedData.setDisplayItem(serverData.getNextItemToEject());
         serverData.pauseStateUpdatingUntil(serverLevel.getGameTime() + 14L);
         setVaultState(serverLevel, pos, blockState, blockState.setValue(VaultBlock.STATE, VaultState.UNLOCKING), config, sharedData);
      }

      private static List<ItemStack> resolveItemsToEject(
         final ServerLevel serverLevel, final VaultConfig config, final BlockPos pos, final Player player, final ItemInstance insertedStack
      ) {
         LootTable lootTable = serverLevel.getServer().reloadableRegistries().getLootTable(config.lootTable());
         LootParams params = new LootParams.Builder(serverLevel)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
            .withLuck(player.getLuck())
            .withParameter(LootContextParams.THIS_ENTITY, player)
            .withParameter(LootContextParams.TOOL, insertedStack)
            .create(LootContextParamSets.VAULT);
         return lootTable.getRandomItems(params);
      }

      private static boolean canEjectReward(final VaultConfig config, final VaultState vaultState) {
         return !config.keyItem().isEmpty() && vaultState != VaultState.INACTIVE;
      }

      private static boolean isValidToInsert(final VaultConfig config, final ItemStack stackToInsert) {
         return ItemStack.isSameItemSameComponents(stackToInsert, config.keyItem()) && stackToInsert.getCount() >= config.keyItem().getCount();
      }

      private static boolean shouldCycleDisplayItem(final long gameTime, final VaultState vaultState) {
         return gameTime % 20L == 0L && vaultState == VaultState.ACTIVE;
      }

      private static void playInsertFailSound(final ServerLevel serverLevel, final VaultServerData serverData, final BlockPos pos, final SoundEvent sound) {
         if (serverLevel.getGameTime() >= serverData.getLastInsertFailTimestamp() + 15L) {
            serverLevel.playSound(null, pos, sound, SoundSource.BLOCKS);
            serverData.setLastInsertFailTimestamp(serverLevel.getGameTime());
         }
      }
   }
}
