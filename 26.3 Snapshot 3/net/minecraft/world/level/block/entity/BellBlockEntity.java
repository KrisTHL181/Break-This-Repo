package net.minecraft.world.level.block.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.mutable.MutableInt;

public class BellBlockEntity extends BlockEntity {
   private static final int DURATION = 50;
   private static final int GLOW_DURATION = 60;
   private static final int MIN_TICKS_BETWEEN_SEARCHES = 60;
   private static final int MAX_RESONATION_TICKS = 40;
   private static final int TICKS_BEFORE_RESONATION = 5;
   private static final int SEARCH_RADIUS = 48;
   private static final int HEAR_BELL_RADIUS = 32;
   private static final int HIGHLIGHT_RAIDERS_RADIUS = 48;
   private long lastRingTimestamp;
   public int ticks;
   public boolean shaking;
   public Direction clickDirection;
   private List<LivingEntity> nearbyEntities;
   private boolean resonating;
   private int resonationTicks;

   public BellBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      super(BlockEntityTypes.BELL, worldPosition, blockState);
   }

   @Override
   public boolean triggerEvent(final int b0, final int b1) {
      if (b0 == 1) {
         this.updateEntities();
         this.resonationTicks = 0;
         this.clickDirection = Direction.from3DDataValue(b1);
         this.ticks = 0;
         this.shaking = true;
         return true;
      } else {
         return super.triggerEvent(b0, b1);
      }
   }

   private static void tick(
      final Level level, final BlockPos pos, final BlockState state, final BellBlockEntity entity, final BellBlockEntity.ResonationEndAction onResonationEnd
   ) {
      if (entity.shaking) {
         entity.ticks++;
      }

      if (entity.ticks >= 50) {
         entity.shaking = false;
         entity.ticks = 0;
      }

      if (entity.ticks >= 5 && entity.resonationTicks == 0 && areRaidersNearby(pos, entity.nearbyEntities)) {
         entity.resonating = true;
         level.playSound(null, pos, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 1.0F, 1.0F);
      }

      if (entity.resonating) {
         if (entity.resonationTicks < 40) {
            entity.resonationTicks++;
         } else {
            onResonationEnd.run(level, pos, entity.nearbyEntities);
            entity.resonating = false;
         }
      }
   }

   public static void clientTick(final Level level, final BlockPos pos, final BlockState state, final BellBlockEntity entity) {
      tick(level, pos, state, entity, BellBlockEntity::showBellParticles);
   }

   public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final BellBlockEntity entity) {
      tick(level, pos, state, entity, BellBlockEntity::makeRaidersGlow);
   }

   public void onHit(final Direction clickDirection) {
      BlockPos bellPos = this.getBlockPos();
      this.clickDirection = clickDirection;
      if (this.shaking) {
         this.ticks = 0;
      } else {
         this.shaking = true;
      }

      this.level.blockEvent(bellPos, this.getBlockState().getBlock(), 1, clickDirection.get3DDataValue());
   }

   private void updateEntities() {
      BlockPos blockPos = this.getBlockPos();
      if (this.level.getGameTime() > this.lastRingTimestamp + 60L || this.nearbyEntities == null) {
         this.lastRingTimestamp = this.level.getGameTime();
         AABB aabb = new AABB(blockPos).inflate(48.0);
         this.nearbyEntities = this.level.getEntitiesOfClass(LivingEntity.class, aabb);
      }

      if (!this.level.isClientSide()) {
         for (LivingEntity entity : this.nearbyEntities) {
            if (entity.isAlive() && !entity.isRemoved() && blockPos.closerToCenterThan(entity.position(), 32.0)) {
               entity.getBrain().setMemory(MemoryModuleType.HEARD_BELL_TIME, this.level.getGameTime());
            }
         }
      }
   }

   private static boolean areRaidersNearby(final BlockPos bellPos, final List<LivingEntity> nearbyEntities) {
      for (LivingEntity entity : nearbyEntities) {
         if (entity.isAlive() && !entity.isRemoved() && bellPos.closerToCenterThan(entity.position(), 32.0) && entity.is(EntityTypeTags.RAIDERS)) {
            return true;
         }
      }

      return false;
   }

   private static void makeRaidersGlow(final Level level, final BlockPos blockPos, final List<LivingEntity> nearbyEntities) {
      nearbyEntities.stream().filter(e -> isRaiderWithinRange(blockPos, e)).forEach(BellBlockEntity::glow);
   }

   private static void showBellParticles(final Level level, final BlockPos bellPos, final List<LivingEntity> nearbyEntities) {
      MutableInt particleColor = new MutableInt(16700985);
      int nearbyRaiderCount = (int)nearbyEntities.stream().filter(p -> bellPos.closerToCenterThan(p.position(), 48.0)).count();
      nearbyEntities.stream()
         .filter(e -> isRaiderWithinRange(bellPos, e))
         .forEach(
            entity -> {
               float distAway = 1.0F;
               double distBtwn = Math.sqrt(
                  (entity.getX() - bellPos.getX()) * (entity.getX() - bellPos.getX()) + (entity.getZ() - bellPos.getZ()) * (entity.getZ() - bellPos.getZ())
               );
               double x3 = bellPos.getX() + 0.5F + 1.0 / distBtwn * (entity.getX() - bellPos.getX());
               double z3 = bellPos.getZ() + 0.5F + 1.0 / distBtwn * (entity.getZ() - bellPos.getZ());
               int particleCount = Mth.clamp((nearbyRaiderCount - 21) / -2, 3, 15);

               for (int i = 0; i < particleCount; i++) {
                  int color = particleColor.addAndGet(5);
                  level.addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, color), x3, bellPos.getY() + 0.5F, z3, 0.0, 0.0, 0.0);
               }
            }
         );
   }

   private static boolean isRaiderWithinRange(final BlockPos blockPos, final LivingEntity entity) {
      return entity.isAlive() && !entity.isRemoved() && blockPos.closerToCenterThan(entity.position(), 48.0) && entity.is(EntityTypeTags.RAIDERS);
   }

   private static void glow(final LivingEntity raider) {
      raider.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60));
   }

   @FunctionalInterface
   private interface ResonationEndAction {
      void run(final Level level, final BlockPos pos, List<LivingEntity> nearbyEntities);
   }
}
