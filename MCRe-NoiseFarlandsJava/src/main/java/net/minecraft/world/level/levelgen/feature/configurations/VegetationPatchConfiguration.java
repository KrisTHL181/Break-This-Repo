package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public record VegetationPatchConfiguration(
    HolderSet<Block> replaceable,
    BlockStateProvider groundState,
    Holder<PlacedFeature> vegetationFeature,
    CaveSurface surface,
    IntProvider depth,
    float extraBottomBlockChance,
    int verticalRange,
    float vegetationChance,
    IntProvider xzRadius,
    float extraEdgeColumnChance
) implements FeatureConfiguration {
    public static final Codec<VegetationPatchConfiguration> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("replaceable").forGetter(VegetationPatchConfiguration::replaceable),
                BlockStateProvider.CODEC.fieldOf("ground_state").forGetter(VegetationPatchConfiguration::groundState),
                PlacedFeature.CODEC.fieldOf("vegetation_feature").forGetter(VegetationPatchConfiguration::vegetationFeature),
                CaveSurface.CODEC.fieldOf("surface").forGetter(VegetationPatchConfiguration::surface),
                IntProviders.codec(1, 128).fieldOf("depth").forGetter(VegetationPatchConfiguration::depth),
                Codec.floatRange(0.0F, 1.0F).fieldOf("extra_bottom_block_chance").forGetter(VegetationPatchConfiguration::extraBottomBlockChance),
                Codec.intRange(1, 256).fieldOf("vertical_range").forGetter(VegetationPatchConfiguration::verticalRange),
                Codec.floatRange(0.0F, 1.0F).fieldOf("vegetation_chance").forGetter(VegetationPatchConfiguration::vegetationChance),
                IntProviders.CODEC.fieldOf("xz_radius").forGetter(VegetationPatchConfiguration::xzRadius),
                Codec.floatRange(0.0F, 1.0F).fieldOf("extra_edge_column_chance").forGetter(VegetationPatchConfiguration::extraEdgeColumnChance)
            )
            .apply(i, VegetationPatchConfiguration::new)
    );
}