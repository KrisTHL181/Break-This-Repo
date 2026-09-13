package net.minecraft.world.entity.projectile.hurtingprojectile.windcharge;

import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class WindCharge extends AbstractWindCharge {
   private static final ExplosionDamageCalculator EXPLOSION_DAMAGE_CALCULATOR = new SimpleExplosionDamageCalculator(
      true, false, Optional.of(1.22F), BuiltInRegistries.BLOCK.get(BlockTags.BLOCKS_WIND_CHARGE_EXPLOSIONS).map(Function.identity())
   );
   private static final float RADIUS = 1.2F;
   private static final float MIN_CAMERA_DISTANCE_SQUARED = Mth.square(3.5F);
   private int noDeflectTicks = 5;

   public WindCharge(final EntityType<? extends AbstractWindCharge> type, final Level level) {
      super(type, level);
   }

   public WindCharge(final Player player, final Level level, final double x, final double y, final double z) {
      super(EntityTypes.WIND_CHARGE, level, player, x, y, z);
   }

   public WindCharge(final Level level, final double x, final double y, final double z, final Vec3 direction) {
      super(EntityTypes.WIND_CHARGE, x, y, z, direction, level);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.noDeflectTicks > 0) {
         this.noDeflectTicks--;
      }
   }

   @Override
   public boolean deflect(
      final ProjectileDeflection deflection, final @Nullable Entity deflectingEntity, final @Nullable EntityReference<Entity> newOwner, final boolean byAttack
   ) {
      return this.noDeflectTicks > 0 ? false : super.deflect(deflection, deflectingEntity, newOwner, byAttack);
   }

   @Override
   protected void explode(final Vec3 position) {
      this.level()
         .explode(
            this,
            null,
            EXPLOSION_DAMAGE_CALCULATOR,
            position.x(),
            position.y(),
            position.z(),
            1.2F,
            false,
            Level.ExplosionInteraction.TRIGGER,
            ParticleTypes.GUST_EMITTER_SMALL,
            ParticleTypes.GUST_EMITTER_LARGE,
            WeightedList.of(),
            SoundEvents.WIND_CHARGE_BURST
         );
   }

   @Override
   public boolean shouldRenderAtSqrDistance(final double distance) {
      return this.tickCount < 2 && distance < MIN_CAMERA_DISTANCE_SQUARED ? false : super.shouldRenderAtSqrDistance(distance);
   }
}
