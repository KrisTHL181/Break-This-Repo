package net.minecraft.world.level.block.grower;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import org.jspecify.annotations.Nullable;

public final class TreeGrower {
    private static final Map<String, TreeGrower> GROWERS = new Object2ObjectArrayMap<>();
    public static final Codec<TreeGrower> CODEC = Codec.stringResolver(g -> g.name, GROWERS::get);
    public static final TreeGrower OAK = new TreeGrower(
        "oak",
        0.1F,
        Optional.empty(),
        Optional.empty(),
        Optional.of(TreeFeatures.OAK),
        Optional.of(TreeFeatures.FANCY_OAK),
        Optional.of(TreeFeatures.OAK_BEES_005),
        Optional.of(TreeFeatures.FANCY_OAK_BEES_005)
    );
    public static final TreeGrower SPRUCE = new TreeGrower(
        "spruce",
        0.5F,
        Optional.of(TreeFeatures.MEGA_SPRUCE),
        Optional.of(TreeFeatures.MEGA_PINE),
        Optional.of(TreeFeatures.SPRUCE),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
    );
    public static final TreeGrower MANGROVE = new TreeGrower(
        "mangrove",
        0.85F,
        Optional.empty(),
        Optional.empty(),
        Optional.of(TreeFeatures.MANGROVE),
        Optional.of(TreeFeatures.TALL_MANGROVE),
        Optional.empty(),
        Optional.empty()
    );
    public static final TreeGrower AZALEA = new TreeGrower("azalea", Optional.empty(), Optional.of(TreeFeatures.AZALEA_TREE), Optional.empty());
    public static final TreeGrower BIRCH = new TreeGrower("birch", Optional.empty(), Optional.of(TreeFeatures.BIRCH), Optional.of(TreeFeatures.BIRCH_BEES_005));
    public static final TreeGrower JUNGLE = new TreeGrower(
        "jungle", Optional.of(TreeFeatures.MEGA_JUNGLE_TREE), Optional.of(TreeFeatures.JUNGLE_TREE_NO_VINE), Optional.empty()
    );
    public static final TreeGrower ACACIA = new TreeGrower("acacia", Optional.empty(), Optional.of(TreeFeatures.ACACIA), Optional.empty());
    public static final TreeGrower CHERRY = new TreeGrower(
        "cherry", Optional.empty(), Optional.of(TreeFeatures.CHERRY), Optional.of(TreeFeatures.CHERRY_BEES_005)
    );
    public static final TreeGrower DARK_OAK = new TreeGrower("dark_oak", Optional.of(TreeFeatures.DARK_OAK), Optional.empty(), Optional.empty());
    public static final TreeGrower PALE_OAK = new TreeGrower("pale_oak", Optional.of(TreeFeatures.PALE_OAK_BONEMEAL), Optional.empty(), Optional.empty());
    private final String name;
    private final float secondaryChance;
    private final Optional<ResourceKey<ConfiguredFeature<?, ?>>> megaTree;
    private final Optional<ResourceKey<ConfiguredFeature<?, ?>>> secondaryMegaTree;
    private final Optional<ResourceKey<ConfiguredFeature<?, ?>>> tree;
    private final Optional<ResourceKey<ConfiguredFeature<?, ?>>> secondaryTree;
    private final Optional<ResourceKey<ConfiguredFeature<?, ?>>> flowers;
    private final Optional<ResourceKey<ConfiguredFeature<?, ?>>> secondaryFlowers;

    public TreeGrower(
        final String name,
        final Optional<ResourceKey<ConfiguredFeature<?, ?>>> megaTree,
        final Optional<ResourceKey<ConfiguredFeature<?, ?>>> tree,
        final Optional<ResourceKey<ConfiguredFeature<?, ?>>> flowers
    ) {
        this(name, 0.0F, megaTree, Optional.empty(), tree, Optional.empty(), flowers, Optional.empty());
    }

    public TreeGrower(
        final String name,
        final float secondaryChance,
        final Optional<ResourceKey<ConfiguredFeature<?, ?>>> megaTree,
        final Optional<ResourceKey<ConfiguredFeature<?, ?>>> secondaryMegaTree,
        final Optional<ResourceKey<ConfiguredFeature<?, ?>>> tree,
        final Optional<ResourceKey<ConfiguredFeature<?, ?>>> secondaryTree,
        final Optional<ResourceKey<ConfiguredFeature<?, ?>>> flowers,
        final Optional<ResourceKey<ConfiguredFeature<?, ?>>> secondaryFlowers
    ) {
        this.name = name;
        this.secondaryChance = secondaryChance;
        this.megaTree = megaTree;
        this.secondaryMegaTree = secondaryMegaTree;
        this.tree = tree;
        this.secondaryTree = secondaryTree;
        this.flowers = flowers;
        this.secondaryFlowers = secondaryFlowers;
        GROWERS.put(name, this);
    }

    private @Nullable ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(final RandomSource random, final boolean hasFlowers) {
        if (random.nextFloat() < this.secondaryChance) {
            if (hasFlowers && this.secondaryFlowers.isPresent()) {
                return this.secondaryFlowers.get();
            }

            if (this.secondaryTree.isPresent()) {
                return this.secondaryTree.get();
            }
        }

        return hasFlowers && this.flowers.isPresent() ? this.flowers.get() : this.tree.orElse(null);
    }

    private @Nullable ResourceKey<ConfiguredFeature<?, ?>> getConfiguredMegaFeature(final RandomSource random) {
        return this.secondaryMegaTree.isPresent() && random.nextFloat() < this.secondaryChance ? this.secondaryMegaTree.get() : this.megaTree.orElse(null);
    }

    public boolean growTree(final ServerLevel level, final ChunkGenerator generator, final BlockPos pos, final BlockState state, final RandomSource random) {
        ResourceKey<ConfiguredFeature<?, ?>> megaFeatureKey = this.getConfiguredMegaFeature(random);
        if (megaFeatureKey != null) {
            Holder<ConfiguredFeature<?, ?>> featureHolder = level.registryAccess()
                .lookupOrThrow(Registries.CONFIGURED_FEATURE)
                .get(megaFeatureKey)
                .orElse(null);
            if (featureHolder != null) {
                for (int dx = 0; dx >= -1; dx--) {
                    for (int dz = 0; dz >= -1; dz--) {
                        if (isTwoByTwoSapling(state, level, pos, dx, dz)) {
                            ConfiguredFeature<?, ?> feature = featureHolder.value();
                            BlockState air = Blocks.AIR.defaultBlockState();
                            level.setBlock(pos.offset(dx, 0, dz), air, 260);
                            level.setBlock(pos.offset(dx + 1, 0, dz), air, 260);
                            level.setBlock(pos.offset(dx, 0, dz + 1), air, 260);
                            level.setBlock(pos.offset(dx + 1, 0, dz + 1), air, 260);
                            if (feature.place(level, generator, random, pos.offset(dx, 0, dz))) {
                                return true;
                            }

                            level.setBlock(pos.offset(dx, 0, dz), state, 260);
                            level.setBlock(pos.offset(dx + 1, 0, dz), state, 260);
                            level.setBlock(pos.offset(dx, 0, dz + 1), state, 260);
                            level.setBlock(pos.offset(dx + 1, 0, dz + 1), state, 260);
                            return false;
                        }
                    }
                }
            }
        }

        ResourceKey<ConfiguredFeature<?, ?>> featureKey = this.getConfiguredFeature(random, this.hasFlowers(level, pos));
        if (featureKey == null) {
            return false;
        }

        Holder<ConfiguredFeature<?, ?>> featureHolder = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).get(featureKey).orElse(null);
        if (featureHolder == null) {
            return false;
        }

        ConfiguredFeature<?, ?> feature = featureHolder.value();
        BlockState emptyBlock = level.getFluidState(pos).createLegacyBlock();
        level.setBlock(pos, emptyBlock, 260);
        if (feature.place(level, generator, random, pos)) {
            if (level.getBlockState(pos) == emptyBlock) {
                level.sendBlockUpdated(pos, state, emptyBlock, 2);
            }

            return true;
        } else {
            level.setBlock(pos, state, 260);
            return false;
        }
    }

    private static boolean isTwoByTwoSapling(final BlockState state, final BlockGetter level, final BlockPos pos, final int ox, final int oz) {
        Block block = state.getBlock();
        return level.getBlockState(pos.offset(ox, 0, oz)).is(block)
            && level.getBlockState(pos.offset(ox + 1, 0, oz)).is(block)
            && level.getBlockState(pos.offset(ox, 0, oz + 1)).is(block)
            && level.getBlockState(pos.offset(ox + 1, 0, oz + 1)).is(block);
    }

    private boolean hasFlowers(final LevelAccessor level, final BlockPos pos) {
        for (BlockPos p : BlockPos.MutableBlockPos.betweenClosed(pos.below().north(2).west(2), pos.above().south(2).east(2))) {
            if (level.getBlockState(p).is(BlockTags.FLOWERS)) {
                return true;
            }
        }

        return false;
    }

    public OptionalInt getMinimumHeight(final ServerLevel level) {
        ResourceKey<ConfiguredFeature<?, ?>> featureKey = this.tree.orElse(null);
        if (featureKey == null) {
            return OptionalInt.empty();
        }

        Holder<ConfiguredFeature<?, ?>> featureHolder = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).get(featureKey).orElse(null);
        return featureHolder != null && featureHolder.value().config() instanceof TreeConfiguration treeConfig
            ? OptionalInt.of(treeConfig.trunkPlacer.getBaseHeight())
            : OptionalInt.empty();
    }
}