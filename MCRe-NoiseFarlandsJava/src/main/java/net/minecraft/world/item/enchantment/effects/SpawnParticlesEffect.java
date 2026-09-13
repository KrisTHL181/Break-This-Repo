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
import net.minecraft.util.valueproviders.FloatProviders;
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
        i -> i.group(
                ParticleTypes.CODEC.fieldOf("particle").forGetter(SpawnParticlesEffect::particle),
                SpawnParticlesEffect.PositionSource.CODEC.fieldOf("horizontal_position").forGetter(SpawnParticlesEffect::horizontalPosition),
                SpawnParticlesEffect.PositionSource.CODEC.fieldOf("vertical_position").forGetter(SpawnParticlesEffect::verticalPosition),
                SpawnParticlesEffect.VelocitySource.CODEC.fieldOf("horizontal_velocity").forGetter(SpawnParticlesEffect::horizontalVelocity),
                SpawnParticlesEffect.VelocitySource.CODEC.fieldOf("vertical_velocity").forGetter(SpawnParticlesEffect::verticalVelocity),
                FloatProviders.CODEC.optionalFieldOf("speed", ConstantFloat.ZERO).forGetter(SpawnParticlesEffect::speed)
            )
            .apply(i, SpawnParticlesEffect::new)
    );

    public static SpawnParticlesEffect.PositionSource offsetFromEntityPosition(final float offset) {
        return new SpawnParticlesEffect.PositionSource(SpawnParticlesEffect.PositionSourceType.ENTITY_POSITION, offset, 1.0F);
    }

    public static SpawnParticlesEffect.PositionSource inBoundingBox() {
        return new SpawnParticlesEffect.PositionSource(SpawnParticlesEffect.PositionSourceType.BOUNDING_BOX, 0.0F, 1.0F);
    }

    public static SpawnParticlesEffect.VelocitySource movementScaled(final float scale) {
        return new SpawnParticlesEffect.VelocitySource(scale, ConstantFloat.ZERO);
    }

    public static SpawnParticlesEffect.VelocitySource fixedVelocity(final FloatProvider provider) {
        return new SpawnParticlesEffect.VelocitySource(0.0F, provider);
    }

    @Override
    public void apply(final ServerLevel serverLevel, final int enchantmentLevel, final EnchantedItemInUse item, final Entity entity, final Vec3 position) {
        RandomSource random = entity.getRandom();
        Vec3 movement = entity.getKnownMovement();
        float bbWidth = entity.getBbWidth();
        float bbHeight = entity.getBbHeight();
        serverLevel.sendParticles(
            this.particle,
            this.horizontalPosition.getCoordinate(position.x(), position.x(), bbWidth, random),
            this.verticalPosition.getCoordinate(position.y(), position.y() + bbHeight / 2.0F, bbHeight, random),
            this.horizontalPosition.getCoordinate(position.z(), position.z(), bbWidth, random),
            0,
            this.horizontalVelocity.getVelocity(movement.x(), random),
            this.verticalVelocity.getVelocity(movement.y(), random),
            this.horizontalVelocity.getVelocity(movement.z(), random),
            this.speed.sample(random)
        );
    }

    @Override
    public MapCodec<SpawnParticlesEffect> codec() {
        return CODEC;
    }

    public record PositionSource(SpawnParticlesEffect.PositionSourceType type, float offset, float scale) {
        public static final MapCodec<SpawnParticlesEffect.PositionSource> CODEC = RecordCodecBuilder.<SpawnParticlesEffect.PositionSource>mapCodec(
                i -> i.group(
                        SpawnParticlesEffect.PositionSourceType.CODEC.fieldOf("type").forGetter(SpawnParticlesEffect.PositionSource::type),
                        Codec.FLOAT.optionalFieldOf("offset", 0.0F).forGetter(SpawnParticlesEffect.PositionSource::offset),
                        ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("scale", 1.0F).forGetter(SpawnParticlesEffect.PositionSource::scale)
                    )
                    .apply(i, SpawnParticlesEffect.PositionSource::new)
            )
            .validate(
                positioning -> positioning.type() == SpawnParticlesEffect.PositionSourceType.ENTITY_POSITION && positioning.scale() != 1.0F
                    ? DataResult.error(() -> "Cannot scale an entity position coordinate source")
                    : DataResult.success(positioning)
            );

        public double getCoordinate(final double position, final double center, final float boundingBoxSpan, final RandomSource random) {
            return this.type.getCoordinate(position, center, boundingBoxSpan * this.scale, random) + this.offset;
        }
    }

    public enum PositionSourceType implements StringRepresentable {
        ENTITY_POSITION("entity_position", (pos, center, bbSpan, random) -> pos),
        BOUNDING_BOX("in_bounding_box", (pos, center, bbSpan, random) -> center + (random.nextDouble() - 0.5) * bbSpan);

        public static final Codec<SpawnParticlesEffect.PositionSourceType> CODEC = StringRepresentable.fromEnum(SpawnParticlesEffect.PositionSourceType::values);
        private final String id;
        private final SpawnParticlesEffect.PositionSourceType.CoordinateSource source;

        PositionSourceType(final String id, final SpawnParticlesEffect.PositionSourceType.CoordinateSource source) {
            this.id = id;
            this.source = source;
        }

        public double getCoordinate(final double position, final double center, final float boundingBoxSpan, final RandomSource random) {
            return this.source.getCoordinate(position, center, boundingBoxSpan, random);
        }

        @Override
        public String getSerializedName() {
            return this.id;
        }

        @FunctionalInterface
        private interface CoordinateSource {
            double getCoordinate(double pos, double center, float boundingBoxSpan, RandomSource random);
        }
    }

    public record VelocitySource(float movementScale, FloatProvider base) {
        public static final MapCodec<SpawnParticlesEffect.VelocitySource> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    Codec.FLOAT.optionalFieldOf("movement_scale", 0.0F).forGetter(SpawnParticlesEffect.VelocitySource::movementScale),
                    FloatProviders.CODEC.optionalFieldOf("base", ConstantFloat.ZERO).forGetter(SpawnParticlesEffect.VelocitySource::base)
                )
                .apply(i, SpawnParticlesEffect.VelocitySource::new)
        );

        public double getVelocity(final double movement, final RandomSource random) {
            return movement * this.movementScale + this.base.sample(random);
        }
    }
}