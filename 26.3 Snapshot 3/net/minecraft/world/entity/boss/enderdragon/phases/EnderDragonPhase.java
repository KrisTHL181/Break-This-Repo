package net.minecraft.world.entity.boss.enderdragon.phases;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

public class EnderDragonPhase<T extends DragonPhaseInstance> {
   private static EnderDragonPhase<?>[] phases = new EnderDragonPhase[0];
   public static final EnderDragonPhase<DragonHoldingPatternPhase> HOLDING_PATTERN = create(DragonHoldingPatternPhase.class, "HoldingPattern");
   public static final EnderDragonPhase<DragonStrafePlayerPhase> STRAFE_PLAYER = create(DragonStrafePlayerPhase.class, "StrafePlayer");
   public static final EnderDragonPhase<DragonLandingApproachPhase> LANDING_APPROACH = create(DragonLandingApproachPhase.class, "LandingApproach");
   public static final EnderDragonPhase<DragonLandingPhase> LANDING = create(DragonLandingPhase.class, "Landing");
   public static final EnderDragonPhase<DragonTakeoffPhase> TAKEOFF = create(DragonTakeoffPhase.class, "Takeoff");
   public static final EnderDragonPhase<DragonSittingFlamingPhase> SITTING_FLAMING = create(DragonSittingFlamingPhase.class, "SittingFlaming");
   public static final EnderDragonPhase<DragonSittingScanningPhase> SITTING_SCANNING = create(DragonSittingScanningPhase.class, "SittingScanning");
   public static final EnderDragonPhase<DragonSittingAttackingPhase> SITTING_ATTACKING = create(DragonSittingAttackingPhase.class, "SittingAttacking");
   public static final EnderDragonPhase<DragonChargePlayerPhase> CHARGING_PLAYER = create(DragonChargePlayerPhase.class, "ChargingPlayer");
   public static final EnderDragonPhase<DragonDeathPhase> DYING = create(DragonDeathPhase.class, "Dying");
   public static final EnderDragonPhase<DragonHoverPhase> HOVERING = create(DragonHoverPhase.class, "Hover");
   private final Class<? extends DragonPhaseInstance> instanceClass;
   private final int id;
   private final String name;

   private EnderDragonPhase(final int id, final Class<? extends DragonPhaseInstance> instanceClass, final String name) {
      this.id = id;
      this.instanceClass = instanceClass;
      this.name = name;
   }

   public DragonPhaseInstance createInstance(final EnderDragon dragon) {
      try {
         Constructor<? extends DragonPhaseInstance> constructor = this.getConstructor();
         return constructor.newInstance(dragon);
      } catch (Exception e) {
         throw new Error(e);
      }
   }

   protected Constructor<? extends DragonPhaseInstance> getConstructor() throws NoSuchMethodException {
      return this.instanceClass.getConstructor(EnderDragon.class);
   }

   public int getId() {
      return this.id;
   }

   @Override
   public String toString() {
      return this.name + " (#" + this.id + ")";
   }

   public static EnderDragonPhase<?> getById(final int id) {
      return id >= 0 && id < phases.length ? phases[id] : HOLDING_PATTERN;
   }

   public static int getCount() {
      return phases.length;
   }

   private static <T extends DragonPhaseInstance> EnderDragonPhase<T> create(final Class<T> instanceClass, final String name) {
      EnderDragonPhase<T> phase = new EnderDragonPhase<>(phases.length, instanceClass, name);
      phases = Arrays.copyOf(phases, phases.length + 1);
      phases[phase.getId()] = phase;
      return phase;
   }
}
