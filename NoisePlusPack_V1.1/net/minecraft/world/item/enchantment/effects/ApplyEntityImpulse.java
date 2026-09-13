package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

public record ApplyEntityImpulse(Vec3 direction, Vec3 coordinateScale, LevelBasedValue magnitude) implements EnchantmentEntityEffect {
   public static final MapCodec<ApplyEntityImpulse> CODEC = RecordCodecBuilder.mapCodec(
      p_450720_ -> p_450720_.group(
            Vec3.CODEC.fieldOf("direction").forGetter(ApplyEntityImpulse::direction),
            Vec3.CODEC.fieldOf("coordinate_scale").forGetter(ApplyEntityImpulse::coordinateScale),
            LevelBasedValue.CODEC.fieldOf("magnitude").forGetter(ApplyEntityImpulse::magnitude)
         )
         .apply(p_450720_, ApplyEntityImpulse::new)
   );
   private static final int POST_IMPULSE_CONTEXT_RESET_GRACE_TIME_TICKS = 10;

   @Override
   public void apply(ServerLevel p_460894_, int p_454940_, EnchantedItemInUse p_453714_, Entity p_458507_, Vec3 p_456407_) {
      Vec3 vec3 = p_458507_.getLookAngle();
      Vec3 vec31 = vec3.addLocalCoordinates(this.direction).multiply(this.coordinateScale).scale(this.magnitude.calculate(p_454940_));
      p_458507_.addDeltaMovement(vec31);
      p_458507_.hurtMarked = true;
      p_458507_.needsSync = true;
      if (p_458507_ instanceof Player player) {
         player.applyPostImpulseGraceTime(10);
      }
   }

   @Override
   public MapCodec<ApplyEntityImpulse> codec() {
      return CODEC;
   }
}
