package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.FloatProviders;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;

public class CanyonCarverConfiguration extends CarverConfiguration {
    public static final Codec<CanyonCarverConfiguration> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                CarverConfiguration.CODEC.forGetter(c -> c),
                FloatProviders.CODEC.fieldOf("vertical_rotation").forGetter(c -> c.verticalRotation),
                CanyonCarverConfiguration.CanyonShapeConfiguration.CODEC.fieldOf("shape").forGetter(c -> c.shape)
            )
            .apply(i, CanyonCarverConfiguration::new)
    );
    public final FloatProvider verticalRotation;
    public final CanyonCarverConfiguration.CanyonShapeConfiguration shape;

    public CanyonCarverConfiguration(
        final float probability,
        final HeightProvider y,
        final FloatProvider yScale,
        final VerticalAnchor lavaLevel,
        final CarverDebugSettings debugSettings,
        final HolderSet<Block> replaceable,
        final FloatProvider verticalRotation,
        final CanyonCarverConfiguration.CanyonShapeConfiguration shape
    ) {
        super(probability, y, yScale, lavaLevel, debugSettings, replaceable);
        this.verticalRotation = verticalRotation;
        this.shape = shape;
    }

    public CanyonCarverConfiguration(
        final CarverConfiguration carver, final FloatProvider distanceFactor, final CanyonCarverConfiguration.CanyonShapeConfiguration shape
    ) {
        this(carver.probability, carver.y, carver.yScale, carver.lavaLevel, carver.debugSettings, carver.replaceable, distanceFactor, shape);
    }

    public static class CanyonShapeConfiguration {
        public static final Codec<CanyonCarverConfiguration.CanyonShapeConfiguration> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                    FloatProviders.CODEC.fieldOf("distance_factor").forGetter(c -> c.distanceFactor),
                    FloatProviders.CODEC.fieldOf("thickness").forGetter(c -> c.thickness),
                    ExtraCodecs.POSITIVE_INT.fieldOf("width_smoothness").forGetter(c -> c.widthSmoothness),
                    FloatProviders.CODEC.fieldOf("horizontal_radius_factor").forGetter(c -> c.horizontalRadiusFactor),
                    Codec.FLOAT.fieldOf("vertical_radius_default_factor").forGetter(c -> c.verticalRadiusDefaultFactor),
                    Codec.FLOAT.fieldOf("vertical_radius_center_factor").forGetter(c -> c.verticalRadiusCenterFactor)
                )
                .apply(i, CanyonCarverConfiguration.CanyonShapeConfiguration::new)
        );
        public final FloatProvider distanceFactor;
        public final FloatProvider thickness;
        public final int widthSmoothness;
        public final FloatProvider horizontalRadiusFactor;
        public final float verticalRadiusDefaultFactor;
        public final float verticalRadiusCenterFactor;

        public CanyonShapeConfiguration(
            final FloatProvider distanceFactor,
            final FloatProvider thickness,
            final int widthSmoothness,
            final FloatProvider horizontalRadiusFactor,
            final float verticalRadiusDefaultFactor,
            final float verticalRadiusCenterFactor
        ) {
            this.widthSmoothness = widthSmoothness;
            this.horizontalRadiusFactor = horizontalRadiusFactor;
            this.verticalRadiusDefaultFactor = verticalRadiusDefaultFactor;
            this.verticalRadiusCenterFactor = verticalRadiusCenterFactor;
            this.distanceFactor = distanceFactor;
            this.thickness = thickness;
        }
    }
}