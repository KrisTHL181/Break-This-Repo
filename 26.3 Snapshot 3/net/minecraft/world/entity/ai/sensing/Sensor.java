package net.minecraft.world.entity.ai.sensing;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

public abstract class Sensor<E extends LivingEntity> {
   private static final int DEFAULT_SCAN_RATE = 20;
   private static final int DEFAULT_TARGETING_RANGE = 16;
   private static final TargetingConditions TARGET_CONDITIONS = TargetingConditions.forNonCombat().range(16.0);
   private static final TargetingConditions TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING = TargetingConditions.forNonCombat()
      .range(16.0)
      .ignoreInvisibilityTesting();
   private static final TargetingConditions ATTACK_TARGET_CONDITIONS = TargetingConditions.forCombat().range(16.0);
   private static final TargetingConditions ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING = TargetingConditions.forCombat()
      .range(16.0)
      .ignoreInvisibilityTesting();
   private static final TargetingConditions ATTACK_TARGET_CONDITIONS_IGNORE_LINE_OF_SIGHT = TargetingConditions.forCombat().range(16.0).ignoreLineOfSight();
   private static final TargetingConditions ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_AND_LINE_OF_SIGHT = TargetingConditions.forCombat()
      .range(16.0)
      .ignoreLineOfSight()
      .ignoreInvisibilityTesting();
   private final int scanRate;
   private long timeToTick;

   public Sensor(final int scanRate) {
      this.scanRate = scanRate;
   }

   public Sensor() {
      this(20);
   }

   public void randomlyDelayStart(final RandomSource randomSource) {
      this.timeToTick = randomSource.nextInt(this.scanRate);
   }

   public final void tick(final ServerLevel level, final E body) {
      if (--this.timeToTick <= 0L) {
         this.timeToTick = this.scanRate;
         this.updateTargetingConditionRanges(body);
         this.doTick(level, body);
      }
   }

   private void updateTargetingConditionRanges(final E body) {
      double followRange = body.getAttributeValue(Attributes.FOLLOW_RANGE);
      TARGET_CONDITIONS.range(followRange);
      TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING.range(followRange);
      ATTACK_TARGET_CONDITIONS.range(followRange);
      ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING.range(followRange);
      ATTACK_TARGET_CONDITIONS_IGNORE_LINE_OF_SIGHT.range(followRange);
      ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_AND_LINE_OF_SIGHT.range(followRange);
   }

   protected abstract void doTick(final ServerLevel level, final E body);

   public abstract Set<MemoryModuleType<?>> requires();

   public static boolean isEntityTargetable(final ServerLevel level, final LivingEntity body, final LivingEntity entity) {
      return body.getBrain().isMemoryValue(MemoryModuleType.ATTACK_TARGET, entity)
         ? TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING.test(level, body, entity)
         : TARGET_CONDITIONS.test(level, body, entity);
   }

   public static boolean isEntityAttackable(final ServerLevel level, final LivingEntity body, final LivingEntity target) {
      return body.getBrain().isMemoryValue(MemoryModuleType.ATTACK_TARGET, target)
         ? ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING.test(level, body, target)
         : ATTACK_TARGET_CONDITIONS.test(level, body, target);
   }

   public static BiPredicate<ServerLevel, LivingEntity> wasEntityAttackableLastNTicks(final LivingEntity body, final int ticks) {
      return rememberPositives(ticks, (level, target) -> isEntityAttackable(level, body, target));
   }

   public static boolean isEntityAttackableIgnoringLineOfSight(final ServerLevel level, final LivingEntity body, final LivingEntity target) {
      return body.getBrain().isMemoryValue(MemoryModuleType.ATTACK_TARGET, target)
         ? ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_AND_LINE_OF_SIGHT.test(level, body, target)
         : ATTACK_TARGET_CONDITIONS_IGNORE_LINE_OF_SIGHT.test(level, body, target);
   }

   public static <T, U> BiPredicate<T, U> rememberPositives(final int invocations, final BiPredicate<T, U> predicate) {
      AtomicInteger positivesLeft = new AtomicInteger(0);
      return (t, u) -> {
         if (predicate.test(t, u)) {
            positivesLeft.set(invocations);
            return true;
         } else {
            return positivesLeft.decrementAndGet() >= 0;
         }
      };
   }
}
