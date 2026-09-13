package net.minecraft.data.worldgen.features;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.references.BlockItemIds;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.BlockColumnFeature;
import net.minecraft.world.level.levelgen.feature.CoralClawFeature;
import net.minecraft.world.level.levelgen.feature.CoralTreeFeature;
import net.minecraft.world.level.levelgen.feature.CuboidPlacement;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.NoOpFeature;
import net.minecraft.world.level.levelgen.feature.OverlayFeature;
import net.minecraft.world.level.levelgen.feature.SimpleBlockFeature;
import net.minecraft.world.level.levelgen.feature.SimpleRandomSelectorFeature;
import net.minecraft.world.level.levelgen.feature.WeightedRandomSelectorFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RandomBlockProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RandomizedIntStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RotatedBlockProvider;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.OffsetPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RandomChancePlacement;

public class AquaticFeatures {
   public static final ResourceKey<Feature> SEAGRASS_SHORT = FeatureUtils.createKey("seagrass_short");
   public static final ResourceKey<Feature> SEAGRASS_SLIGHTLY_LESS_SHORT = FeatureUtils.createKey("seagrass_slightly_less_short");
   public static final ResourceKey<Feature> SEAGRASS_MID = FeatureUtils.createKey("seagrass_mid");
   public static final ResourceKey<Feature> SEAGRASS_TALL = FeatureUtils.createKey("seagrass_tall");
   public static final ResourceKey<Feature> SEA_PICKLE = FeatureUtils.createKey("sea_pickle");
   public static final ResourceKey<Feature> KELP = FeatureUtils.createKey("kelp");
   public static final ResourceKey<Feature> WARM_OCEAN_VEGETATION = FeatureUtils.createKey("warm_ocean_vegetation");

   public static void bootstrap(final BootstrapContext<Feature> context) {
      HolderGetter<Block> blocks = context.lookup(Registries.BLOCK);
      HolderGetter<Feature> features = context.lookup(Registries.FEATURE);
      context.register(SEAGRASS_SHORT, seagrass(30));
      context.register(SEAGRASS_SLIGHTLY_LESS_SHORT, seagrass(40));
      context.register(SEAGRASS_MID, seagrass(60));
      context.register(SEAGRASS_TALL, seagrass(80));
      context.register(
         SEA_PICKLE,
         new SimpleBlockFeature(new RandomizedIntStateProvider(BlockStateProvider.simple(Blocks.SEA_PICKLE), BlockStateProperties.PICKLES, UniformInt.of(1, 4)))
      );
      context.register(
         KELP,
         new BlockColumnFeature(
            List.of(
               BlockColumnFeature.layer(UniformInt.of(0, 9), BlockStateProvider.simple(Blocks.KELP_PLANT)),
               BlockColumnFeature.layer(
                  ConstantInt.of(1), new RandomizedIntStateProvider(BlockStateProvider.simple(Blocks.KELP), KelpBlock.AGE, UniformInt.of(20, 23))
               )
            ),
            Direction.UP,
            BlockPredicate.allOf(BlockPredicate.matchesBlocks(Blocks.WATER), BlockPredicate.matchesBlocks(Direction.UP, Blocks.WATER)),
            true
         )
      );
      BlockPredicateFilter coralAllowed = BlockPredicateFilter.forPredicate(
         BlockPredicate.allOf(
            BlockPredicate.anyOf(BlockPredicate.matchesBlocks(Blocks.WATER), BlockPredicate.matchesTag(BlockTags.CORALS)),
            BlockPredicate.matchesBlocks(Direction.UP, Blocks.WATER)
         )
      );
      context.register(
         WARM_OCEAN_VEGETATION,
         new SimpleRandomSelectorFeature(
            HolderSet.direct(
               Stream.of(
                     BlockItemIds.TUBE_CORAL_BLOCK,
                     BlockItemIds.BRAIN_CORAL_BLOCK,
                     BlockItemIds.BUBBLE_CORAL_BLOCK,
                     BlockItemIds.FIRE_CORAL_BLOCK,
                     BlockItemIds.HORN_CORAL_BLOCK
                  )
                  .map(id -> blocks.getOrThrow(id.block()).value())
                  .map(block -> coral(features, blocks, block))
                  .flatMap(
                     coralType -> Stream.of(
                        PlacementUtils.inlinePlaced(new CoralTreeFeature(PlacementUtils.inlinePlaced(coralType, coralAllowed))),
                        PlacementUtils.inlinePlaced(new CoralClawFeature(PlacementUtils.inlinePlaced(coralType, coralAllowed))),
                        PlacementUtils.inlinePlaced(
                           coralType,
                           OffsetPlacement.vertical(UniformInt.of(-3, -1)),
                           new CuboidPlacement(UniformInt.of(3, 5), UniformInt.of(3, 5), false, false),
                           new RandomChancePlacement(0.9F),
                           coralAllowed
                        )
                     )
                  )
                  .toList()
            )
         )
      );
   }

   private static Feature coral(final HolderGetter<Feature> features, final HolderGetter<Block> blocks, final Block block) {
      return new OverlayFeature(
         HolderSet.direct(
            PlacementUtils.inlinePlaced(new SimpleBlockFeature(BlockStateProvider.simple(block))),
            PlacementUtils.inlinePlaced(
               new WeightedRandomSelectorFeature(
                  WeightedList.of(
                     new Weighted<>(PlacementUtils.inlinePlaced(new SimpleBlockFeature(new RandomBlockProvider(blocks.getOrThrow(BlockTags.CORALS)))), 20),
                     new Weighted<>(PlacementUtils.inlinePlaced(features.getOrThrow(SEA_PICKLE)), 3),
                     new Weighted<>(PlacementUtils.inlinePlaced(new NoOpFeature()), 57)
                  )
               ),
               OffsetPlacement.above()
            ),
            wallCoral(blocks, Direction.NORTH),
            wallCoral(blocks, Direction.EAST),
            wallCoral(blocks, Direction.SOUTH),
            wallCoral(blocks, Direction.WEST)
         )
      );
   }

   private static Holder<PlacedFeature> wallCoral(final HolderGetter<Block> blocks, final Direction direction) {
      return PlacementUtils.inlinePlaced(
         new SimpleBlockFeature(new RotatedBlockProvider(new RandomBlockProvider(blocks.getOrThrow(BlockTags.WALL_CORALS)), Optional.of(direction))),
         new RandomChancePlacement(0.2F),
         OffsetPlacement.of(direction),
         BlockPredicateFilter.forPredicate(BlockPredicate.matchesBlocks(Blocks.WATER))
      );
   }

   private static Feature seagrass(final int tallPercentage) {
      return new WeightedRandomSelectorFeature(
         WeightedList.of(
            new Weighted<>(
               PlacementUtils.inlinePlaced(
                  new SimpleBlockFeature(BlockStateProvider.simple(Blocks.TALL_SEAGRASS)),
                  BlockPredicateFilter.forPredicate(BlockPredicate.matchesBlocks(Direction.UP, Blocks.WATER))
               ),
               tallPercentage
            ),
            new Weighted<>(PlacementUtils.inlinePlaced(new SimpleBlockFeature(BlockStateProvider.simple(Blocks.SEAGRASS))), 100 - tallPercentage)
         )
      );
   }
}
