package net.minecraft.world.level.levelgen.feature.configurations;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderGetter;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.featuresize.FeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.rootplacers.RootPlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;

public class TreeConfiguration implements FeatureConfiguration {
    public static final BlockPredicate CAN_PLACE_BELOW_TREE_TRUNKS = BlockPredicate.not(BlockPredicate.matchesTag(BlockTags.CANNOT_REPLACE_BELOW_TREE_TRUNK));
    public static final Codec<TreeConfiguration> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                BlockStateProvider.CODEC.fieldOf("trunk_provider").forGetter(c -> c.trunkProvider),
                TrunkPlacer.CODEC.fieldOf("trunk_placer").forGetter(c -> c.trunkPlacer),
                BlockStateProvider.CODEC.fieldOf("foliage_provider").forGetter(c -> c.foliageProvider),
                FoliagePlacer.CODEC.fieldOf("foliage_placer").forGetter(c -> c.foliagePlacer),
                RootPlacer.CODEC.optionalFieldOf("root_placer").forGetter(c -> c.rootPlacer),
                FeatureSize.CODEC.fieldOf("minimum_size").forGetter(c -> c.minimumSize),
                TreeDecorator.CODEC.listOf().fieldOf("decorators").forGetter(c -> c.decorators),
                Codec.BOOL.fieldOf("ignore_vines").orElse(false).forGetter(c -> c.ignoreVines),
                BlockStateProvider.CODEC.fieldOf("below_trunk_provider").forGetter(c -> c.belowTrunkProvider)
            )
            .apply(i, TreeConfiguration::new)
    );
    public final BlockStateProvider trunkProvider;
    public final TrunkPlacer trunkPlacer;
    public final BlockStateProvider foliageProvider;
    public final FoliagePlacer foliagePlacer;
    public final Optional<RootPlacer> rootPlacer;
    public final FeatureSize minimumSize;
    public final List<TreeDecorator> decorators;
    public final boolean ignoreVines;
    public final BlockStateProvider belowTrunkProvider;

    protected TreeConfiguration(
        final BlockStateProvider trunkProvider,
        final TrunkPlacer trunkPlacer,
        final BlockStateProvider foliageProvider,
        final FoliagePlacer foliagePlacer,
        final Optional<RootPlacer> rootPlacer,
        final FeatureSize minimumSize,
        final List<TreeDecorator> decorators,
        final boolean ignoreVines,
        final BlockStateProvider belowTrunkProvider
    ) {
        this.trunkProvider = trunkProvider;
        this.trunkPlacer = trunkPlacer;
        this.foliageProvider = foliageProvider;
        this.foliagePlacer = foliagePlacer;
        this.rootPlacer = rootPlacer;
        this.minimumSize = minimumSize;
        this.decorators = decorators;
        this.ignoreVines = ignoreVines;
        this.belowTrunkProvider = belowTrunkProvider;
    }

    public static BlockStateProvider defaultPlaceBelowTreeTrunkProvider(final HolderGetter<Biome> biomes) {
        return RuleBasedStateProvider.ifTrueThenProvide(CAN_PLACE_BELOW_TREE_TRUNKS, Blocks.DIRT);
    }

    public static class TreeConfigurationBuilder {
        public final BlockStateProvider trunkProvider;
        private final TrunkPlacer trunkPlacer;
        public final BlockStateProvider foliageProvider;
        private final FoliagePlacer foliagePlacer;
        private final Optional<RootPlacer> rootPlacer;
        private final FeatureSize minimumSize;
        private List<TreeDecorator> decorators = ImmutableList.of();
        private boolean ignoreVines;
        private BlockStateProvider belowTrunkProvider;

        public TreeConfigurationBuilder(
            final BlockStateProvider trunkProvider,
            final TrunkPlacer trunkPlacer,
            final BlockStateProvider foliageProvider,
            final FoliagePlacer foliagePlacer,
            final Optional<RootPlacer> rootPlacer,
            final FeatureSize minimumSize,
            final BlockStateProvider belowTrunkProvider
        ) {
            this.trunkProvider = trunkProvider;
            this.trunkPlacer = trunkPlacer;
            this.foliageProvider = foliageProvider;
            this.foliagePlacer = foliagePlacer;
            this.rootPlacer = rootPlacer;
            this.minimumSize = minimumSize;
            this.belowTrunkProvider = belowTrunkProvider;
        }

        public TreeConfigurationBuilder(
            final BlockStateProvider trunkProvider,
            final TrunkPlacer trunkPlacer,
            final BlockStateProvider foliageProvider,
            final FoliagePlacer foliagePlacer,
            final FeatureSize minimumSize,
            final BlockStateProvider belowTrunkProvider
        ) {
            this(trunkProvider, trunkPlacer, foliageProvider, foliagePlacer, Optional.empty(), minimumSize, belowTrunkProvider);
        }

        public TreeConfiguration.TreeConfigurationBuilder belowTrunkProvider(final BlockStateProvider belowTrunkProvider) {
            this.belowTrunkProvider = belowTrunkProvider;
            return this;
        }

        public TreeConfiguration.TreeConfigurationBuilder decorators(final List<TreeDecorator> decorators) {
            this.decorators = decorators;
            return this;
        }

        public TreeConfiguration.TreeConfigurationBuilder ignoreVines() {
            this.ignoreVines = true;
            return this;
        }

        public TreeConfiguration build() {
            return new TreeConfiguration(
                this.trunkProvider,
                this.trunkPlacer,
                this.foliageProvider,
                this.foliagePlacer,
                this.rootPlacer,
                this.minimumSize,
                this.decorators,
                this.ignoreVines,
                this.belowTrunkProvider
            );
        }
    }
}