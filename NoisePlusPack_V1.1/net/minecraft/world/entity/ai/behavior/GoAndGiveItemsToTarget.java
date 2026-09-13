package net.minecraft.world.entity.ai.behavior;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.allay.AllayAi;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class GoAndGiveItemsToTarget<E extends LivingEntity & InventoryCarrier> extends Behavior<E> {
   private static final int CLOSE_ENOUGH_DISTANCE_TO_TARGET = 3;
   private static final int ITEM_PICKUP_COOLDOWN_AFTER_THROWING = 60;
   private final Function<LivingEntity, Optional<PositionTracker>> targetPositionGetter;
   private final float speedModifier;

   public GoAndGiveItemsToTarget(Function<LivingEntity, Optional<PositionTracker>> p_249894_, float p_249937_, int p_249620_) {
      super(
         Map.of(
            MemoryModuleType.LOOK_TARGET,
            MemoryStatus.REGISTERED,
            MemoryModuleType.WALK_TARGET,
            MemoryStatus.REGISTERED,
            MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS,
            MemoryStatus.REGISTERED
         ),
         p_249620_
      );
      this.targetPositionGetter = p_249894_;
      this.speedModifier = p_249937_;
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel p_217196_, E p_217197_) {
      return this.canThrowItemToTarget(p_217197_);
   }

   @Override
   protected boolean canStillUse(ServerLevel p_217218_, E p_217219_, long p_217220_) {
      return this.canThrowItemToTarget(p_217219_);
   }

   @Override
   protected void start(ServerLevel p_217199_, E p_217200_, long p_217201_) {
      this.targetPositionGetter
         .apply(p_217200_)
         .ifPresent(p_217206_ -> BehaviorUtils.setWalkAndLookTargetMemories(p_217200_, p_217206_, this.speedModifier, 3));
   }

   @Override
   protected void tick(ServerLevel p_217226_, E p_217227_, long p_217228_) {
      Optional<PositionTracker> optional = this.targetPositionGetter.apply(p_217227_);
      if (!optional.isEmpty()) {
         PositionTracker positiontracker = optional.get();
         double d0 = positiontracker.currentPosition().distanceTo(p_217227_.getEyePosition());
         if (d0 < 3.0) {
            ItemStack itemstack = p_217227_.getInventory().removeItem(0, 1);
            if (!itemstack.isEmpty()) {
               throwItem(p_217227_, itemstack, getThrowPosition(positiontracker));
               if (p_217227_ instanceof Allay allay) {
                  AllayAi.getLikedPlayer(allay).ifPresent(p_217224_ -> this.triggerDropItemOnBlock(positiontracker, itemstack, p_217224_));
               }

               p_217227_.getBrain().setMemory(MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS, 60);
            }
         }
      }
   }

   private void triggerDropItemOnBlock(PositionTracker p_217214_, ItemStack p_217215_, ServerPlayer p_217216_) {
      BlockPos blockpos = p_217214_.currentBlockPosition().below();
      CriteriaTriggers.ALLAY_DROP_ITEM_ON_BLOCK.trigger(p_217216_, blockpos, p_217215_);
   }

   private boolean canThrowItemToTarget(E p_217203_) {
      if (p_217203_.getInventory().isEmpty()) {
         return false;
      }

      Optional<PositionTracker> optional = this.targetPositionGetter.apply(p_217203_);
      return optional.isPresent();
   }

   private static Vec3 getThrowPosition(PositionTracker p_217212_) {
      return p_217212_.currentPosition().add(0.0, 1.0, 0.0);
   }

   public static void throwItem(LivingEntity p_217208_, ItemStack p_217209_, Vec3 p_217210_) {
      Vec3 vec3 = new Vec3(0.2F, 0.3F, 0.2F);
      BehaviorUtils.throwItem(p_217208_, p_217209_, p_217210_, vec3, 0.2F);
      Level level = p_217208_.level();
      if (level.getGameTime() % 7L == 0L && level.random.nextDouble() < 0.9) {
         float f = Util.<Float>getRandom(Allay.THROW_SOUND_PITCHES, level.getRandom());
         level.playSound(null, p_217208_, SoundEvents.ALLAY_THROW, SoundSource.NEUTRAL, 1.0F, f);
      }
   }
}
