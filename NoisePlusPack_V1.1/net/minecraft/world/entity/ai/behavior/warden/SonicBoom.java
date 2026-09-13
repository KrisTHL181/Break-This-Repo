package net.minecraft.world.entity.ai.behavior.warden;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.Vec3;

public class SonicBoom extends Behavior<Warden> {
   private static final int DISTANCE_XZ = 15;
   private static final int DISTANCE_Y = 20;
   private static final double KNOCKBACK_VERTICAL = 0.5;
   private static final double KNOCKBACK_HORIZONTAL = 2.5;
   public static final int COOLDOWN = 40;
   private static final int TICKS_BEFORE_PLAYING_SOUND = Mth.ceil(34.0);
   private static final int DURATION = Mth.ceil(60.0F);

   public SonicBoom() {
      super(
         ImmutableMap.of(
            MemoryModuleType.ATTACK_TARGET,
            MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.SONIC_BOOM_COOLDOWN,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.SONIC_BOOM_SOUND_COOLDOWN,
            MemoryStatus.REGISTERED,
            MemoryModuleType.SONIC_BOOM_SOUND_DELAY,
            MemoryStatus.REGISTERED
         ),
         DURATION
      );
   }

   protected boolean checkExtraStartConditions(ServerLevel p_217692_, Warden p_217693_) {
      return p_217693_.closerThan(p_217693_.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get(), 15.0, 20.0);
   }

   protected boolean canStillUse(ServerLevel p_217695_, Warden p_217696_, long p_217697_) {
      return true;
   }

   protected void start(ServerLevel p_217713_, Warden p_217714_, long p_217715_) {
      p_217714_.getBrain().setMemoryWithExpiry(MemoryModuleType.ATTACK_COOLING_DOWN, true, DURATION);
      p_217714_.getBrain().setMemoryWithExpiry(MemoryModuleType.SONIC_BOOM_SOUND_DELAY, Unit.INSTANCE, TICKS_BEFORE_PLAYING_SOUND);
      p_217713_.broadcastEntityEvent(p_217714_, (byte)62);
      p_217714_.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 3.0F, 1.0F);
   }

   protected void tick(ServerLevel p_217724_, Warden p_217725_, long p_217726_) {
      p_217725_.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(p_449588_ -> p_217725_.getLookControl().setLookAt(p_449588_.position()));
      if (!p_217725_.getBrain().hasMemoryValue(MemoryModuleType.SONIC_BOOM_SOUND_DELAY)
         && !p_217725_.getBrain().hasMemoryValue(MemoryModuleType.SONIC_BOOM_SOUND_COOLDOWN)) {
         p_217725_.getBrain().setMemoryWithExpiry(MemoryModuleType.SONIC_BOOM_SOUND_COOLDOWN, Unit.INSTANCE, DURATION - TICKS_BEFORE_PLAYING_SOUND);
         p_217725_.getBrain()
            .getMemory(MemoryModuleType.ATTACK_TARGET)
            .filter(p_217725_::canTargetEntity)
            .filter(p_217707_ -> p_217725_.closerThan(p_217707_, 15.0, 20.0))
            .ifPresent(p_359079_ -> {
               Vec3 vec3 = p_217725_.position().add(p_217725_.getAttachments().get(EntityAttachment.WARDEN_CHEST, 0, p_217725_.getYRot()));
               Vec3 vec31 = p_359079_.getEyePosition().subtract(vec3);
               Vec3 vec32 = vec31.normalize();
               int i = Mth.floor(vec31.length()) + 7;

               for (int j = 1; j < i; j++) {
                  Vec3 vec33 = vec3.add(vec32.scale(j));
                  p_217724_.sendParticles(ParticleTypes.SONIC_BOOM, vec33.x, vec33.y, vec33.z, 1, 0.0, 0.0, 0.0, 0.0);
               }

               p_217725_.playSound(SoundEvents.WARDEN_SONIC_BOOM, 3.0F, 1.0F);
               if (p_359079_.hurtServer(p_217724_, p_217724_.damageSources().sonicBoom(p_217725_), 10.0F)) {
                  double d1 = 0.5 * (1.0 - p_359079_.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                  double d0 = 2.5 * (1.0 - p_359079_.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                  p_359079_.push(vec32.x() * d0, vec32.y() * d1, vec32.z() * d0);
               }
            });
      }
   }

   protected void stop(ServerLevel p_217732_, Warden p_217733_, long p_217734_) {
      setCooldown(p_217733_, 40);
   }

   public static void setCooldown(LivingEntity p_217699_, int p_217700_) {
      p_217699_.getBrain().setMemoryWithExpiry(MemoryModuleType.SONIC_BOOM_COOLDOWN, Unit.INSTANCE, p_217700_);
   }
}
