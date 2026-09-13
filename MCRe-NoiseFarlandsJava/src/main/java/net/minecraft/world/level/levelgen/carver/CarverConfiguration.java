package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.FloatProviders;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;

public class CarverConfiguration extends ProbabilityFeatureConfiguration {
    public static final MapCodec<CarverConfiguration> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
                Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter(c -> c.probability),
                HeightProvider.CODEC.fieldOf("y").forGetter(c -> c.y),
                FloatProviders.CODEC.fieldOf("yScale").forGetter(c -> c.yScale),
                VerticalAnchor.CODEC.fieldOf("lava_level").forGetter(c -> c.lavaLevel),
                CarverDebugSettings.CODEC.optionalFieldOf("debug_settings", CarverDebugSettings.DEFAULT).forGetter(c -> c.debugSettings),
                RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("replaceable").forGetter(c -> c.replaceable)
            )
            .apply(i, CarverConfiguration::new)
    );
    public final HeightProvider y;
    public final FloatProvider yScale;
    public final VerticalAnchor lavaLevel;
    public final CarverDebugSettings debugSettings;
    public final HolderSet<Block> replaceable;

    public CarverConfiguration(
        final float probability,
        final HeightProvider y,
        final FloatProvider yScale,
        final VerticalAnchor lavaLevel,
        final CarverDebugSettings debugSettings,
        final HolderSet<Block> replaceable
    ) {
        super(probability);
        this.y = y;
        this.yScale = yScale;
        this.lavaLevel = lavaLevel;
        this.debugSettings = debugSettings;
        this.replaceable = replaceable;
    }
}