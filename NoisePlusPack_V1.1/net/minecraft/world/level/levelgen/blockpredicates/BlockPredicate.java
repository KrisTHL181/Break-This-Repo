package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public interface BlockPredicate extends BiPredicate<WorldGenLevel, BlockPos> {
   Codec<BlockPredicate> CODEC = BuiltInRegistries.BLOCK_PREDICATE_TYPE.byNameCodec().dispatch(BlockPredicate::type, BlockPredicateType::codec);
   BlockPredicate ONLY_IN_AIR_PREDICATE = matchesBlocks(Blocks.AIR);
   BlockPredicate ONLY_IN_AIR_OR_WATER_PREDICATE = matchesBlocks(Blocks.AIR, Blocks.WATER);

   BlockPredicateType<?> type();

   static BlockPredicate allOf(List<BlockPredicate> p_190413_) {
      return new AllOfPredicate(p_190413_);
   }

   static BlockPredicate allOf(BlockPredicate... p_190418_) {
      return allOf(List.of(p_190418_));
   }

   static BlockPredicate allOf(BlockPredicate p_190405_, BlockPredicate p_190406_) {
      return allOf(List.of(p_190405_, p_190406_));
   }

   static BlockPredicate anyOf(List<BlockPredicate> p_190426_) {
      return new AnyOfPredicate(p_190426_);
   }

   static BlockPredicate anyOf(BlockPredicate... p_190431_) {
      return anyOf(List.of(p_190431_));
   }

   static BlockPredicate anyOf(BlockPredicate p_190421_, BlockPredicate p_190422_) {
      return anyOf(List.of(p_190421_, p_190422_));
   }

   static BlockPredicate matchesBlocks(Vec3i p_224772_, List<Block> p_224773_) {
      return new MatchingBlocksPredicate(p_224772_, HolderSet.direct(Block::builtInRegistryHolder, p_224773_));
   }

   static BlockPredicate matchesBlocks(List<Block> p_198312_) {
      return matchesBlocks(Vec3i.ZERO, p_198312_);
   }

   static BlockPredicate matchesBlocks(Vec3i p_224775_, Block... p_224776_) {
      return matchesBlocks(p_224775_, List.of(p_224776_));
   }

   static BlockPredicate matchesBlocks(Block... p_224781_) {
      return matchesBlocks(Vec3i.ZERO, p_224781_);
   }

   static BlockPredicate matchesTag(Vec3i p_224769_, TagKey<Block> p_224770_) {
      return new MatchingBlockTagPredicate(p_224769_, p_224770_);
   }

   static BlockPredicate matchesTag(TagKey<Block> p_204678_) {
      return matchesTag(Vec3i.ZERO, p_204678_);
   }

   static BlockPredicate matchesFluids(Vec3i p_224785_, List<Fluid> p_224786_) {
      return new MatchingFluidsPredicate(p_224785_, HolderSet.direct(Fluid::builtInRegistryHolder, p_224786_));
   }

   static BlockPredicate matchesFluids(Vec3i p_224778_, Fluid... p_224779_) {
      return matchesFluids(p_224778_, List.of(p_224779_));
   }

   static BlockPredicate matchesFluids(Fluid... p_224783_) {
      return matchesFluids(Vec3i.ZERO, p_224783_);
   }

   static BlockPredicate not(BlockPredicate p_190403_) {
      return new NotPredicate(p_190403_);
   }

   static BlockPredicate replaceable(Vec3i p_190411_) {
      return new ReplaceablePredicate(p_190411_);
   }

   static BlockPredicate replaceable() {
      return replaceable(Vec3i.ZERO);
   }

   static BlockPredicate wouldSurvive(BlockState p_190400_, Vec3i p_190401_) {
      return new WouldSurvivePredicate(p_190401_, p_190400_);
   }

   static BlockPredicate hasSturdyFace(Vec3i p_198309_, Direction p_198310_) {
      return new HasSturdyFacePredicate(p_198309_, p_198310_);
   }

   static BlockPredicate hasSturdyFace(Direction p_198914_) {
      return hasSturdyFace(Vec3i.ZERO, p_198914_);
   }

   static BlockPredicate solid(Vec3i p_190424_) {
      return new SolidPredicate(p_190424_);
   }

   static BlockPredicate solid() {
      return solid(Vec3i.ZERO);
   }

   static BlockPredicate noFluid() {
      return noFluid(Vec3i.ZERO);
   }

   static BlockPredicate noFluid(Vec3i p_249383_) {
      return matchesFluids(p_249383_, Fluids.EMPTY);
   }

   static BlockPredicate insideWorld(Vec3i p_190434_) {
      return new InsideWorldBoundsPredicate(p_190434_);
   }

   static BlockPredicate alwaysTrue() {
      return TrueBlockPredicate.INSTANCE;
   }

   static BlockPredicate unobstructed(Vec3i p_344704_) {
      return new UnobstructedPredicate(p_344704_);
   }

   static BlockPredicate unobstructed() {
      return unobstructed(Vec3i.ZERO);
   }
}
