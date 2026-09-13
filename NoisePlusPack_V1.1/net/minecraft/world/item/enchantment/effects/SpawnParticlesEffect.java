package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.phys.Vec3;

public record SpawnParticlesEffect(
   ParticleOptions particle,
   SpawnParticlesEffect.PositionSource horizontalPosition,
   SpawnParticlesEffect.PositionSource verticalPosition,
   SpawnParticlesEffect.VelocitySource horizontalVelocity,
   SpawnParticlesEffect.VelocitySource verticalVelocity,
   FloatProvider speed
) implements EnchantmentEntityEffect {
   public static final MapCodec<SpawnParticlesEffect> CODEC = RecordCodecBuilder.mapCodec(
      p_342263_ -> p_342263_.group(
            ParticleTypes.CODEC.fieldOf("particle").forGetter(SpawnParticlesEffect::particle),
            SpawnParticlesEffect.PositionSource.CODEC.fieldOf("horizontal_position").forGetter(SpawnParticlesEffect::horizontalPosition),
            SpawnParticlesEffect.PositionSource.CODEC.fieldOf("vertical_position").forGetter(SpawnParticlesEffect::verticalPosition),
            SpawnParticlesEffect.VelocitySource.CODEC.fieldOf("horizontal_velocity").forGetter(SpawnParticlesEffect::horizontalVelocity),
            SpawnParticlesEffect.VelocitySource.CODEC.fieldOf("vertical_velocity").forGetter(SpawnParticlesEffect::verticalVelocity),
            FloatProvider.CODEC.optionalFieldOf("speed", ConstantFloat.ZERO).forGetter(SpawnParticlesEffect::speed)
         )
         .apply(p_342263_, SpawnParticlesEffect::new)
   );

   public static SpawnParticlesEffect.PositionSource offsetFromEntityPosition(float p_344734_) {
      return new SpawnParticlesEffect.PositionSource(SpawnParticlesEffect.PositionSourceType.ENTITY_POSITION, p_344734_, 1.0F);
   }

   public static SpawnParticlesEffect.PositionSource inBoundingBox() {
      return new SpawnParticlesEffect.PositionSource(SpawnParticlesEffect.PositionSourceType.BOUNDING_BOX, 0.0F, 1.0F);
   }

   public static SpawnParticlesEffect.VelocitySource movementScaled(float p_342848_) {
      return new SpawnParticlesEffect.VelocitySource(p_342848_, ConstantFloat.ZERO);
   }

   public static SpawnParticlesEffect.VelocitySource fixedVelocity(FloatProvider p_344992_) {
      return new SpawnParticlesEffect.VelocitySource(0.0F, p_344992_);
   }

   @Override
   public void apply(ServerLevel p_344629_, int p_343825_, EnchantedItemInUse p_342850_, Entity p_342334_, Vec3 p_344096_) {
      RandomSource randomsource = p_342334_.getRandom();
      Vec3 vec3 = p_342334_.getKnownMovement();
      float f = p_342334_.getBbWidth();
      float f1 = p_342334_.getBbHeight();
      p_344629_.sendParticles(
         this.particle,
         this.horizontalPosition.getCoordinate(p_344096_.x(), p_344096_.x(), f, randomsource),
         this.verticalPosition.getCoordinate(p_344096_.y(), p_344096_.y() + f1 / 2.0F, f1, randomsource),
         this.horizontalPosition.getCoordinate(p_344096_.z(), p_344096_.z(), f, randomsource),
         0,
         this.horizontalVelocity.getVelocity(vec3.x(), randomsource),
         this.verticalVelocity.getVelocity(vec3.y(), randomsource),
         this.horizontalVelocity.getVelocity(vec3.z(), randomsource),
         this.speed.sample(randomsource)
      );
   }

   @Override
   public MapCodec<SpawnParticlesEffect> codec() {
      return CODEC;
   }

   public record PositionSource(SpawnParticlesEffect.PositionSourceType type, float offset, float scale) {
      public static final MapCodec<SpawnParticlesEffect.PositionSource> CODEC = RecordCodecBuilder.mapCodec(
            p_344563_ -> p_344563_.group(
                  SpawnParticlesEffect.PositionSourceType.CODEC.fieldOf("type").forGetter(SpawnParticlesEffect.PositionSource::type),
                  Codec.FLOAT.optionalFieldOf("offset", 0.0F).forGetter(SpawnParticlesEffect.PositionSource::offset),
                  ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("scale", 1.0F).forGetter(SpawnParticlesEffect.PositionSource::scale)
               )
               .apply(p_344563_, SpawnParticlesEffect.PositionSource::new)
         )
         .validate(
            p_344468_ -> p_344468_.type() == SpawnParticlesEffect.PositionSourceType.ENTITY_POSITION && p_344468_.scale() != 1.0F
               ? DataResult.error(() -> "Cannot scale an entity position coordinate source")
               : DataResult.success(p_344468_)
         );

      public double getCoordinate(double p_344146_, double p_344860_, float p_343272_, RandomSource p_344191_) {
         return this.type.getCoordinate(p_344146_, p_344860_, p_343272_ * this.scale, p_344191_) + this.offset;
      }
   }

   public enum PositionSourceType implements StringRepresentable {
      ENTITY_POSITION("entity_position", (p_343048_, p_343091_, p_345054_, p_342606_) -> p_343048_),
      BOUNDING_BOX("in_bounding_box", (p_343212_, p_344879_, p_342916_, p_343170_) -> p_344879_ + (p_343170_.nextDouble() - 0.5) * p_342916_);

      public static final Codec<SpawnParticlesEffect.PositionSourceType> CODEC = StringRepresentable.fromEnum(SpawnParticlesEffect.PositionSourceType::values);
      private final String id;
      private final SpawnParticlesEffect.PositionSourceType.CoordinateSource source;

      PositionSourceType(final String p_343318_, final SpawnParticlesEffect.PositionSourceType.CoordinateSource p_343028_) {
         this.id = p_343318_;
         this.source = p_343028_;
      }

      public double getCoordinate(double p_343826_, double p_344958_, float p_345431_, RandomSource p_342492_) {
         return this.source.getCoordinate(p_343826_, p_344958_, p_345431_, p_342492_);
      }

      @Override
      public String getSerializedName() {
         return this.id;
      }

      @FunctionalInterface
      interface CoordinateSource {
         double getCoordinate(double var1, double var3, float var5, RandomSource var6);
      }
   }

   public record VelocitySource(float movementScale, FloatProvider base) {
      public static final MapCodec<SpawnParticlesEffect.VelocitySource> CODEC = RecordCodecBuilder.mapCodec(
         p_343024_ -> p_343024_.group(
               Codec.FLOAT.optionalFieldOf("movement_scale", 0.0F).forGetter(SpawnParticlesEffect.VelocitySource::movementScale),
               FloatProvider.CODEC.optionalFieldOf("base", ConstantFloat.ZERO).forGetter(SpawnParticlesEffect.VelocitySource::base)
            )
            .apply(p_343024_, SpawnParticlesEffect.VelocitySource::new)
      );

      public double getVelocity(double p_344775_, RandomSource p_342972_) {
         return p_344775_ * this.movementScale + this.base.sample(p_342972_);
      }
   }
}
