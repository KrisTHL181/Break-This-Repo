package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.GeodeConfiguration;

public class GeodeCrackSettings {
    public static final Codec<GeodeCrackSettings> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                GeodeConfiguration.CHANCE_RANGE.optionalFieldOf("generate_crack_chance", 1.0).forGetter(c -> c.generateCrackChance),
                Codec.doubleRange(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY).optionalFieldOf("base_crack_size", 2.0).forGetter(c -> c.baseCrackSize),
                Codec.intRange(0, 10).optionalFieldOf("crack_point_offset", 2).forGetter(c -> c.crackPointOffset)
            )
            .apply(i, GeodeCrackSettings::new)
    );
    public final double generateCrackChance;
    public final double baseCrackSize;
    public final int crackPointOffset;

    public GeodeCrackSettings(final double generateCrackChance, final double baseCrackSize, final int crackPointOffset) {
        this.generateCrackChance = generateCrackChance;
        this.baseCrackSize = baseCrackSize;
        this.crackPointOffset = crackPointOffset;
    }
}