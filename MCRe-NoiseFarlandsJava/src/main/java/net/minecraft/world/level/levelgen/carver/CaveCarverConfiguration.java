package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.FloatProviders;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;

public class CaveCarverConfiguration extends CarverConfiguration {
    public static final Codec<CaveCarverConfiguration> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                CarverConfiguration.CODEC.forGetter(c -> c),
                FloatProviders.CODEC.fieldOf("horizontal_radius_multiplier").forGetter(c -> c.horizontalRadiusMultiplier),
                FloatProviders.CODEC.fieldOf("vertical_radius_multiplier").forGetter(c -> c.verticalRadiusMultiplier),
                FloatProviders.codec(-1.0F, 1.0F).fieldOf("floor_level").forGetter(c -> c.floorLevel)
            )
            .apply(i, CaveCarverConfiguration::new)
    );
    public final FloatProvider horizontalRadiusMultiplier;
    public final FloatProvider verticalRadiusMultiplier;
    public final FloatProvider floorLevel;

    public CaveCarverConfiguration(
        final float probability,
        final HeightProvider y,
        final FloatProvider yScale,
        final VerticalAnchor lavaLevel,
        final CarverDebugSettings debugSettings,
        final HolderSet<Block> replaceable,
        final FloatProvider horizontalRadiusMultiplier,
        final FloatProvider verticalRadiusMultiplier,
        final FloatProvider floorLevel
    ) {
        super(probability, y, yScale, lavaLevel, debugSettings, replaceable);
        this.horizontalRadiusMultiplier = horizontalRadiusMultiplier;
        this.verticalRadiusMultiplier = verticalRadiusMultiplier;
        this.floorLevel = floorLevel;
    }

    public CaveCarverConfiguration(
        final float probability,
        final HeightProvider y,
        final FloatProvider yScale,
        final VerticalAnchor lavaLevel,
        final HolderSet<Block> replaceable,
        final FloatProvider horizontalRadiusMultiplier,
        final FloatProvider verticalRadiusMultiplier,
        final FloatProvider floorLevel
    ) {
        this(probability, y, yScale, lavaLevel, CarverDebugSettings.DEFAULT, replaceable, horizontalRadiusMultiplier, verticalRadiusMultiplier, floorLevel);
    }

    public CaveCarverConfiguration(
        final CarverConfiguration carver,
        final FloatProvider horizontalRadiusMultiplier,
        final FloatProvider verticalRadiusMultiplier,
        final FloatProvider floorLevel
    ) {
        this(
            carver.probability,
            carver.y,
            carver.yScale,
            carver.lavaLevel,
            carver.debugSettings,
            carver.replaceable,
            horizontalRadiusMultiplier,
            verticalRadiusMultiplier,
            floorLevel
        );
    }
}