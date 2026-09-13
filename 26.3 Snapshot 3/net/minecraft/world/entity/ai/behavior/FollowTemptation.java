package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.player.Player;

public class FollowTemptation extends Behavior<PathfinderMob> {
   public static final int TEMPTATION_COOLDOWN = 100;
   public static final double DEFAULT_CLOSE_ENOUGH_DIST = 2.5;
   public static final double BACKED_UP_CLOSE_ENOUGH_DIST = 3.5;
   private final Function<LivingEntity, Float> speedModifier;
   private final Function<LivingEntity, Double> closeEnoughDistance;
   private final boolean lookInTheEyes;

   public FollowTemptation(final Function<LivingEntity, Float> speedModifier) {
      this(speedModifier, entity -> 2.5);
   }

   public FollowTemptation(final Function<LivingEntity, Float> speedModifier, final Function<LivingEntity, Double> closeEnoughDistance) {
      this(speedModifier, closeEnoughDistance, false);
   }

   public FollowTemptation(
      final Function<LivingEntity, Float> speedModifier, final Function<LivingEntity, Double> closeEnoughDistance, final boolean lookInTheEyes
   ) {
      super(Util.make(() -> {
         Builder<MemoryModuleType<?>, MemoryStatus> builder = ImmutableMap.builder();
         builder.put(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED);
         builder.put(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED);
         builder.put(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT);
         builder.put(MemoryModuleType.IS_TEMPTED, MemoryStatus.VALUE_ABSENT);
         builder.put(MemoryModuleType.TEMPTING_PLAYER, MemoryStatus.VALUE_PRESENT);
         builder.put(MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT);
         builder.put(MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT);
         return builder.build();
      }));
      this.speedModifier = speedModifier;
      this.closeEnoughDistance = closeEnoughDistance;
      this.lookInTheEyes = lookInTheEyes;
   }

   protected float getSpeedModifier(final PathfinderMob body) {
      return this.speedModifier.apply(body);
   }

   private Optional<Player> getTemptingPlayer(final PathfinderMob body) {
      return body.getBrain().getMemory(MemoryModuleType.TEMPTING_PLAYER);
   }

   @Override
   protected boolean timedOut(final long timestamp) {
      return false;
   }

   protected boolean canStillUse(final ServerLevel level, final PathfinderMob body, final long timestamp) {
      return this.getTemptingPlayer(body).isPresent()
         && !body.getBrain().hasMemoryValue(MemoryModuleType.BREED_TARGET)
         && !body.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING);
   }

   protected void start(final ServerLevel level, final PathfinderMob body, final long timestamp) {
      body.getBrain().setMemory(MemoryModuleType.IS_TEMPTED, true);
   }

   protected void stop(final ServerLevel level, final PathfinderMob body, final long timestamp) {
      Brain<?> brain = body.getBrain();
      brain.setMemory(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS, 100);
      brain.eraseMemory(MemoryModuleType.IS_TEMPTED);
      brain.eraseMemory(MemoryModuleType.WALK_TARGET);
      brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
   }

   protected void tick(final ServerLevel level, final PathfinderMob body, final long timestamp) {
      Player player = this.getTemptingPlayer(body).get();
      Brain<?> brain = body.getBrain();
      brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(player, true));
      double closeEnough = this.closeEnoughDistance.apply(body);
      if (body.distanceToSqr(player) < Mth.square(closeEnough)) {
         brain.eraseMemory(MemoryModuleType.WALK_TARGET);
      } else {
         brain.setMemory(
            MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityTracker(player, this.lookInTheEyes, this.lookInTheEyes), this.getSpeedModifier(body), 2)
         );
      }
   }
}
