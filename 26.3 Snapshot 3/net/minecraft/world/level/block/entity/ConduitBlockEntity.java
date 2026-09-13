package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ConduitBlockEntity extends BlockEntity {
   private static final int BLOCK_REFRESH_RATE = 2;
   private static final int EFFECT_DURATION = 13;
   private static final float ROTATION_SPEED = -0.0375F;
   private static final int MIN_ACTIVE_SIZE = 16;
   private static final int MIN_KILL_SIZE = 42;
   private static final int KILL_RANGE = 8;
   private static final Block[] VALID_BLOCKS = new Block[]{Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS, Blocks.SEA_LANTERN, Blocks.DARK_PRISMARINE};
   public int tickCount;
   private float activeRotation;
   private boolean isActive;
   private boolean isHunting;
   private final List<BlockPos> effectBlocks = Lists.newArrayList();
   private @Nullable EntityReference<LivingEntity> destroyTarget;
   private long nextAmbientSoundActivation;

   public ConduitBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      super(BlockEntityTypes.CONDUIT, worldPosition, blockState);
   }

   @Override
   protected void loadAdditional(final ValueInput input) {
      super.loadAdditional(input);
      this.destroyTarget = EntityReference.read(input, "Target");
   }

   @Override
   protected void saveAdditional(final ValueOutput output) {
      super.saveAdditional(output);
      EntityReference.store(this.destroyTarget, output, "Target");
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
      return this.saveCustomOnly(registries);
   }

   public static void clientTick(final Level level, final BlockPos pos, final BlockState state, final ConduitBlockEntity entity) {
      entity.tickCount++;
      long gameTime = level.getGameTime();
      List<BlockPos> effectBlocks = entity.effectBlocks;
      if (gameTime % 40L == 0L) {
         entity.isActive = updateShape(level, pos, effectBlocks);
         updateHunting(entity, effectBlocks);
      }

      LivingEntity destroyTarget = EntityReference.getLivingEntity(entity.destroyTarget, level);
      animationTick(level, pos, effectBlocks, destroyTarget, entity.tickCount);
      if (entity.isActive()) {
         entity.activeRotation++;
      }
   }

   public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final ConduitBlockEntity entity) {
      entity.tickCount++;
      long gameTime = level.getGameTime();
      List<BlockPos> effectBlocks = entity.effectBlocks;
      if (gameTime % 40L == 0L) {
         boolean active = updateShape(level, pos, effectBlocks);
         if (active != entity.isActive) {
            SoundEvent event = active ? SoundEvents.CONDUIT_ACTIVATE : SoundEvents.CONDUIT_DEACTIVATE;
            level.playSound(null, pos, event, SoundSource.BLOCKS, 1.0F, 1.0F);
         }

         entity.isActive = active;
         updateHunting(entity, effectBlocks);
         if (active) {
            applyEffects(level, pos, effectBlocks);
            updateAndAttackTarget((ServerLevel)level, pos, state, entity, effectBlocks.size() >= 42);
         }
      }

      if (entity.isActive()) {
         if (gameTime % 80L == 0L) {
            level.playSound(null, pos, SoundEvents.CONDUIT_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F);
         }

         if (gameTime > entity.nextAmbientSoundActivation) {
            entity.nextAmbientSoundActivation = gameTime + 60L + level.getRandom().nextInt(40);
            level.playSound(null, pos, SoundEvents.CONDUIT_AMBIENT_SHORT, SoundSource.BLOCKS, 1.0F, 1.0F);
         }
      }
   }

   private static void updateHunting(final ConduitBlockEntity entity, final List<BlockPos> effectBlocks) {
      entity.setHunting(effectBlocks.size() >= 42);
   }

   private static boolean updateShape(final Level level, final BlockPos worldPosition, final List<BlockPos> effectBlocks) {
      effectBlocks.clear();

      for (int ox = -1; ox <= 1; ox++) {
         for (int oy = -1; oy <= 1; oy++) {
            for (int oz = -1; oz <= 1; oz++) {
               BlockPos testPos = worldPosition.offset(ox, oy, oz);
               if (!level.isWaterAt(testPos)) {
                  return false;
               }
            }
         }
      }

      for (int ox = -2; ox <= 2; ox++) {
         for (int oy = -2; oy <= 2; oy++) {
            for (int oz = -2; oz <= 2; oz++) {
               int ax = Math.abs(ox);
               int ay = Math.abs(oy);
               int az = Math.abs(oz);
               if ((ax > 1 || ay > 1 || az > 1) && (ox == 0 && (ay == 2 || az == 2) || oy == 0 && (ax == 2 || az == 2) || oz == 0 && (ax == 2 || ay == 2))) {
                  BlockPos testPos = worldPosition.offset(ox, oy, oz);
                  BlockState testBlock = level.getBlockState(testPos);

                  for (Block type : VALID_BLOCKS) {
                     if (testBlock.is(type)) {
                        effectBlocks.add(testPos);
                     }
                  }
               }
            }
         }
      }

      return effectBlocks.size() >= 16;
   }

   private static void applyEffects(final Level level, final BlockPos worldPosition, final List<BlockPos> effectBlocks) {
      int activeSize = effectBlocks.size();
      int effectRange = activeSize / 7 * 16;
      int x = worldPosition.getX();
      int y = worldPosition.getY();
      int z = worldPosition.getZ();
      AABB bb = new AABB(x, y, z, x + 1, y + 1, z + 1).inflate(effectRange).expandTowards(0.0, level.getHeight(), 0.0);
      List<Player> players = level.getEntitiesOfClass(Player.class, bb);
      if (!players.isEmpty()) {
         for (Player player : players) {
            if (worldPosition.closerThan(player.blockPosition(), effectRange) && player.isInWaterOrRain()) {
               player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 260, 0, true, true));
            }
         }
      }
   }

   private static void updateAndAttackTarget(
      final ServerLevel level, final BlockPos worldPosition, final BlockState blockState, final ConduitBlockEntity entity, final boolean isActive
   ) {
      EntityReference<LivingEntity> newDestroyTarget = updateDestroyTarget(entity.destroyTarget, level, worldPosition, isActive);
      LivingEntity targetEntity = EntityReference.getLivingEntity(newDestroyTarget, level);
      if (targetEntity != null) {
         level.playSound(null, targetEntity.getX(), targetEntity.getY(), targetEntity.getZ(), SoundEvents.CONDUIT_ATTACK_TARGET, SoundSource.BLOCKS, 1.0F, 1.0F);
         targetEntity.hurtServer(level, level.damageSources().magic(), 4.0F);
      }

      if (!Objects.equals(newDestroyTarget, entity.destroyTarget)) {
         entity.destroyTarget = newDestroyTarget;
         level.sendBlockUpdated(worldPosition, blockState, blockState, 2);
      }
   }

   private static @Nullable EntityReference<LivingEntity> updateDestroyTarget(
      final @Nullable EntityReference<LivingEntity> target, final ServerLevel level, final BlockPos pos, final boolean isActive
   ) {
      if (!isActive) {
         return null;
      }

      if (target == null) {
         return selectNewTarget(level, pos);
      }

      LivingEntity targetEntity = EntityReference.getLivingEntity(target, level);
      return targetEntity != null && targetEntity.isAlive() && pos.closerThan(targetEntity.blockPosition(), 8.0) ? target : null;
   }

   private static @Nullable EntityReference<LivingEntity> selectNewTarget(final ServerLevel level, final BlockPos pos) {
      List<LivingEntity> candidates = level.getEntitiesOfClass(
         LivingEntity.class, getDestroyRangeAABB(pos), input -> input instanceof Enemy && input.isInWaterOrRain()
      );
      return candidates.isEmpty() ? null : EntityReference.of(Util.getRandom(candidates, level.getRandom()));
   }

   private static AABB getDestroyRangeAABB(final BlockPos worldPosition) {
      return new AABB(worldPosition).inflate(8.0);
   }

   private static void animationTick(
      final Level level, final BlockPos worldPosition, final List<BlockPos> effectBlocks, final @Nullable Entity destroyTarget, final int tickCount
   ) {
      RandomSource random = level.getRandom();
      double hh = Mth.sin((tickCount + 35) * 0.1F) / 2.0F + 0.5F;
      hh = (hh * hh + hh) * 0.3F;
      Vec3 particleEnd = new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 1.5 + hh, worldPosition.getZ() + 0.5);

      for (BlockPos pos : effectBlocks) {
         if (random.nextInt(50) == 0) {
            BlockPos delta = pos.subtract(worldPosition);
            float dx = -0.5F + random.nextFloat() + delta.getX();
            float dy = -2.0F + random.nextFloat() + delta.getY();
            float dz = -0.5F + random.nextFloat() + delta.getZ();
            level.addParticle(ParticleTypes.NAUTILUS, particleEnd.x, particleEnd.y, particleEnd.z, dx, dy, dz);
         }
      }

      if (destroyTarget != null) {
         Vec3 targetPosition = new Vec3(destroyTarget.getX(), destroyTarget.getEyeY(), destroyTarget.getZ());
         float randx = (-0.5F + random.nextFloat()) * (3.0F + destroyTarget.getBbWidth());
         float randy = -1.0F + random.nextFloat() * destroyTarget.getBbHeight();
         float randz = (-0.5F + random.nextFloat()) * (3.0F + destroyTarget.getBbWidth());
         Vec3 velocity = new Vec3(randx, randy, randz);
         level.addParticle(ParticleTypes.NAUTILUS, targetPosition.x, targetPosition.y, targetPosition.z, velocity.x, velocity.y, velocity.z);
      }
   }

   public boolean isActive() {
      return this.isActive;
   }

   public boolean isHunting() {
      return this.isHunting;
   }

   private void setHunting(final boolean hunting) {
      this.isHunting = hunting;
   }

   public float getActiveRotation(final float a) {
      return (this.activeRotation + a) * -0.0375F;
   }
}
