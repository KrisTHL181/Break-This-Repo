package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.Rotation;

public record TemplateFeatureConfiguration(WeightedList<TemplateFeatureConfiguration.TemplateEntry> templates) implements FeatureConfiguration {
    public static final Codec<TemplateFeatureConfiguration> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                WeightedList.codec(TemplateFeatureConfiguration.TemplateEntry.CODEC).fieldOf("templates").forGetter(TemplateFeatureConfiguration::templates)
            )
            .apply(i, TemplateFeatureConfiguration::new)
    );

    public record TemplateEntry(Identifier template, List<Rotation> rotations) {
        public static final Codec<TemplateFeatureConfiguration.TemplateEntry> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                    Identifier.CODEC.fieldOf("id").forGetter(TemplateFeatureConfiguration.TemplateEntry::template),
                    Rotation.CODEC
                        .listOf()
                        .optionalFieldOf("rotations", List.of(Rotation.values()))
                        .forGetter(TemplateFeatureConfiguration.TemplateEntry::rotations)
                )
                .apply(i, TemplateFeatureConfiguration.TemplateEntry::new)
        );

        public static TemplateFeatureConfiguration.TemplateEntry of(final Identifier template) {
            return new TemplateFeatureConfiguration.TemplateEntry(template, List.of(Rotation.values()));
        }
    }
}