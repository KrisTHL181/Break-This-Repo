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

   public WindCharge(EntityType<? extends AbstractWindCharge> p_452696_, Level p_450169_) {
      super(p_452696_, p_450169_);
   }

   public WindCharge(Player p_460521_, Level p_457947_, double p_455925_, double p_451374_, double p_451614_) {
      super(EntityType.WIND_CHARGE, p_457947_, p_460521_, p_455925_, p_451374_, p_451614_);
   }

   public WindCharge(Level p_454948_, double p_455356_, double p_450335_, double p_459033_, Vec3 p_455004_) {
      super(EntityType.WIND_CHARGE, p_455356_, p_450335_, p_459033_, p_455004_, p_454948_);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.noDeflectTicks > 0) {
         this.noDeflectTicks--;
      }
   }

   @Override
   public boolean deflect(ProjectileDeflection p_455945_, @Nullable Entity p_453932_, @Nullable EntityReference<Entity> p_452225_, boolean p_457957_) {
      return this.noDeflectTicks > 0 ? false : super.deflect(p_455945_, p_453932_, p_452225_, p_457957_);
   }

   @Override
   protected void explode(Vec3 p_450563_) {
      this.level()
         .explode(
            this,
            null,
            EXPLOSION_DAMAGE_CALCULATOR,
            p_450563_.x(),
            p_450563_.y(),
            p_450563_.z(),
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
   public boolean shouldRenderAtSqrDistance(double p_457564_) {
      return this.tickCount < 2 && p_457564_ < MIN_CAMERA_DISTANCE_SQUARED ? false : super.shouldRenderAtSqrDistance(p_457564_);
   }
}
