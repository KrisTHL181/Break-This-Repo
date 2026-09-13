package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public interface BlockPredicate extends BiPredicate<WorldGenLevel, BlockPos> {
    Codec<BlockPredicate> CODEC = BuiltInRegistries.BLOCK_PREDICATE_TYPE.byNameCodec().dispatch(BlockPredicate::type, BlockPredicateType::codec);
    BlockPredicate ONLY_IN_AIR_PREDICATE = matchesTag(BlockTags.AIR);
    BlockPredicate ONLY_IN_AIR_OR_WATER_PREDICATE = anyOf(ONLY_IN_AIR_PREDICATE, matchesBlocks(Blocks.WATER));

    BlockPredicateType<?> type();

    static BlockPredicate allOf(final List<BlockPredicate> predicates) {
        return new AllOfPredicate(predicates);
    }

    static BlockPredicate allOf(final BlockPredicate... predicates) {
        return allOf(List.of(predicates));
    }

    static BlockPredicate allOf(final BlockPredicate a, final BlockPredicate b) {
        return allOf(List.of(a, b));
    }

    static BlockPredicate anyOf(final List<BlockPredicate> predicates) {
        return new AnyOfPredicate(predicates);
    }

    static BlockPredicate anyOf(final BlockPredicate... predicates) {
        return anyOf(List.of(predicates));
    }

    static BlockPredicate anyOf(final BlockPredicate a, final BlockPredicate b) {
        return anyOf(List.of(a, b));
    }

    static BlockPredicate matchesBlocks(final Vec3i offset, final List<Block> blocks) {
        return new MatchingBlocksPredicate(offset, HolderSet.direct(Block::builtInRegistryHolder, blocks));
    }

    static BlockPredicate matchesBlocks(final List<Block> blocks) {
        return matchesBlocks(Vec3i.ZERO, blocks);
    }

    static BlockPredicate matchesBlocks(final Vec3i offset, final Block... blocks) {
        return matchesBlocks(offset, List.of(blocks));
    }

    static BlockPredicate matchesBlocks(final Block... blocks) {
        return matchesBlocks(Vec3i.ZERO, blocks);
    }

    static BlockPredicate matchesTag(final Vec3i offset, final TagKey<Block> tag) {
        return new MatchingBlockTagPredicate(offset, tag);
    }

    static BlockPredicate matchesTag(final TagKey<Block> tag) {
        return matchesTag(Vec3i.ZERO, tag);
    }

    static BlockPredicate matchesFluids(final Vec3i offset, final List<Fluid> fluids) {
        return new MatchingFluidsPredicate(offset, HolderSet.direct(Fluid::builtInRegistryHolder, fluids));
    }

    static BlockPredicate matchesFluids(final Vec3i offset, final Fluid... fluids) {
        return matchesFluids(offset, List.of(fluids));
    }

    static BlockPredicate matchesFluids(final Fluid... fluids) {
        return matchesFluids(Vec3i.ZERO, fluids);
    }

    static BlockPredicate matchesBiomes(final HolderSet<Biome> biomes) {
        return new MatchingBiomesPredicate(biomes);
    }

    static BlockPredicate not(final BlockPredicate predicate) {
        return new NotPredicate(predicate);
    }

    static BlockPredicate replaceable(final Vec3i offset) {
        return new ReplaceablePredicate(offset);
    }

    static BlockPredicate replaceable() {
        return replaceable(Vec3i.ZERO);
    }

    static BlockPredicate wouldSurvive(final BlockState state, final Vec3i offset) {
        return new WouldSurvivePredicate(offset, state);
    }

    static BlockPredicate hasSturdyFace(final Vec3i offset, final Direction direction) {
        return new HasSturdyFacePredicate(offset, direction);
    }

    static BlockPredicate hasSturdyFace(final Direction direction) {
        return hasSturdyFace(Vec3i.ZERO, direction);
    }

    static BlockPredicate solid(final Vec3i offset) {
        return new SolidPredicate(offset);
    }

    static BlockPredicate solid() {
        return solid(Vec3i.ZERO);
    }

    static BlockPredicate noFluid() {
        return noFluid(Vec3i.ZERO);
    }

    static BlockPredicate noFluid(final Vec3i offset) {
        return matchesFluids(offset, Fluids.EMPTY);
    }

    static BlockPredicate insideWorld(final Vec3i offset) {
        return new InsideWorldBoundsPredicate(offset);
    }

    static BlockPredicate alwaysTrue() {
        return TrueBlockPredicate.INSTANCE;
    }

    static BlockPredicate unobstructed(final Vec3i offset) {
        return new UnobstructedPredicate(offset);
    }

    static BlockPredicate unobstructed() {
        return unobstructed(Vec3i.ZERO);
    }
}