package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

public record ApplyEntityImpulse(Vec3 direction, Vec3 coordinateScale, LevelBasedValue magnitude) implements EnchantmentEntityEffect {
   public static final MapCodec<ApplyEntityImpulse> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Vec3.CODEC.fieldOf("direction").forGetter(ApplyEntityImpulse::direction),
            Vec3.CODEC.fieldOf("coordinate_scale").forGetter(ApplyEntityImpulse::coordinateScale),
            LevelBasedValue.CODEC.fieldOf("magnitude").forGetter(ApplyEntityImpulse::magnitude)
         )
         .apply(i, ApplyEntityImpulse::new)
   );
   private static final int POST_IMPULSE_CONTEXT_RESET_GRACE_TIME_TICKS = 10;

   @Override
   public void apply(final ServerLevel serverLevel, final int enchantmentLevel, final EnchantedItemInUse item, final Entity entity, final Vec3 position) {
      Vec3 look = entity.getLookAngle();
      Vec3 direction = look.addLocalCoordinates(this.direction).multiply(this.coordinateScale).scale(this.magnitude.calculate(enchantmentLevel));
      entity.addDeltaMovement(direction);
      entity.syncVelocity = true;
      entity.needsSync = true;
      if (entity instanceof LivingEntity livingEntity) {
         livingEntity.applyPostImpulseGraceTime(10);
      }
   }

   @Override
   public MapCodec<ApplyEntityImpulse> codec() {
      return CODEC;
   }
}
