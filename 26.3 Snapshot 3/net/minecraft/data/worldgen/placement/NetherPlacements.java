package net.minecraft.data.worldgen.placement;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.NetherFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.BiasedToBottomInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountOnEveryLayerPlacement;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.OffsetPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

public class NetherPlacements {
   public static final ResourceKey<PlacedFeature> DELTA = PlacementUtils.createKey("delta");
   public static final ResourceKey<PlacedFeature> SMALL_BASALT_COLUMNS = PlacementUtils.createKey("small_basalt_columns");
   public static final ResourceKey<PlacedFeature> LARGE_BASALT_COLUMNS = PlacementUtils.createKey("large_basalt_columns");
   public static final ResourceKey<PlacedFeature> BASALT_BLOBS = PlacementUtils.createKey("basalt_blobs");
   public static final ResourceKey<PlacedFeature> BLACKSTONE_BLOBS = PlacementUtils.createKey("blackstone_blobs");
   public static final ResourceKey<PlacedFeature> GLOWSTONE_EXTRA = PlacementUtils.createKey("glowstone_extra");
   public static final ResourceKey<PlacedFeature> GLOWSTONE = PlacementUtils.createKey("glowstone");
   public static final ResourceKey<PlacedFeature> CRIMSON_FOREST_VEGETATION = PlacementUtils.createKey("crimson_forest_vegetation");
   public static final ResourceKey<PlacedFeature> WARPED_FOREST_VEGETATION = PlacementUtils.createKey("warped_forest_vegetation");
   public static final ResourceKey<PlacedFeature> NETHER_SPROUTS = PlacementUtils.createKey("nether_sprouts");
   public static final ResourceKey<PlacedFeature> TWISTING_VINES = PlacementUtils.createKey("twisting_vines");
   public static final ResourceKey<PlacedFeature> WEEPING_VINES = PlacementUtils.createKey("weeping_vines");
   public static final ResourceKey<PlacedFeature> PATCH_CRIMSON_ROOTS = PlacementUtils.createKey("patch_crimson_roots");
   public static final ResourceKey<PlacedFeature> BASALT_PILLAR = PlacementUtils.createKey("basalt_pillar");
   public static final ResourceKey<PlacedFeature> SPRING_DELTA = PlacementUtils.createKey("spring_delta");
   public static final ResourceKey<PlacedFeature> SPRING_CLOSED = PlacementUtils.createKey("spring_closed");
   public static final ResourceKey<PlacedFeature> SPRING_CLOSED_DOUBLE = PlacementUtils.createKey("spring_closed_double");
   public static final ResourceKey<PlacedFeature> SPRING_OPEN = PlacementUtils.createKey("spring_open");
   public static final ResourceKey<PlacedFeature> PATCH_SOUL_FIRE = PlacementUtils.createKey("patch_soul_fire");
   public static final ResourceKey<PlacedFeature> PATCH_FIRE = PlacementUtils.createKey("patch_fire");

   public static void bootstrap(final BootstrapContext<PlacedFeature> context) {
      HolderGetter<Feature> configuredFeatures = context.lookup(Registries.FEATURE);
      Holder<Feature> delta = configuredFeatures.getOrThrow(NetherFeatures.DELTA);
      Holder<Feature> smallBasaltColumns = configuredFeatures.getOrThrow(NetherFeatures.SMALL_BASALT_COLUMNS);
      Holder<Feature> largeBasaltColumns = configuredFeatures.getOrThrow(NetherFeatures.LARGE_BASALT_COLUMNS);
      Holder<Feature> basaltBlobs = configuredFeatures.getOrThrow(NetherFeatures.BASALT_BLOBS);
      Holder<Feature> blackstoneBlobs = configuredFeatures.getOrThrow(NetherFeatures.BLACKSTONE_BLOBS);
      Holder<Feature> glowstoneExtra = configuredFeatures.getOrThrow(NetherFeatures.GLOWSTONE_EXTRA);
      Holder<Feature> crimsonForestVegetation = configuredFeatures.getOrThrow(NetherFeatures.CRIMSON_FOREST_VEGETATION);
      Holder<Feature> warpedForestVegetion = configuredFeatures.getOrThrow(NetherFeatures.WARPED_FOREST_VEGETION);
      Holder<Feature> netherSprouts = configuredFeatures.getOrThrow(NetherFeatures.NETHER_SPROUTS);
      Holder<Feature> twistingVines = configuredFeatures.getOrThrow(NetherFeatures.TWISTING_VINES);
      Holder<Feature> weepingVines = configuredFeatures.getOrThrow(NetherFeatures.WEEPING_VINES);
      Holder<Feature> crimsonRoots = configuredFeatures.getOrThrow(NetherFeatures.CRIMSON_ROOTS);
      Holder<Feature> basaltPillar = configuredFeatures.getOrThrow(NetherFeatures.BASALT_PILLAR);
      Holder<Feature> springLavaNether = configuredFeatures.getOrThrow(NetherFeatures.SPRING_LAVA_NETHER);
      Holder<Feature> springNetherClosed = configuredFeatures.getOrThrow(NetherFeatures.SPRING_NETHER_CLOSED);
      Holder<Feature> springNetherOpen = configuredFeatures.getOrThrow(NetherFeatures.SPRING_NETHER_OPEN);
      Holder<Feature> soulFire = configuredFeatures.getOrThrow(NetherFeatures.SOUL_FIRE);
      Holder<Feature> fire = configuredFeatures.getOrThrow(NetherFeatures.FIRE);
      PlacementUtils.register(context, DELTA, delta, CountOnEveryLayerPlacement.of(40), BiomeFilter.biome());
      PlacementUtils.register(context, SMALL_BASALT_COLUMNS, smallBasaltColumns, CountOnEveryLayerPlacement.of(4), BiomeFilter.biome());
      PlacementUtils.register(context, LARGE_BASALT_COLUMNS, largeBasaltColumns, CountOnEveryLayerPlacement.of(2), BiomeFilter.biome());
      PlacementUtils.register(
         context, BASALT_BLOBS, basaltBlobs, CountPlacement.of(75), InSquarePlacement.spread(), PlacementUtils.FULL_RANGE, BiomeFilter.biome()
      );
      PlacementUtils.register(
         context, BLACKSTONE_BLOBS, blackstoneBlobs, CountPlacement.of(25), InSquarePlacement.spread(), PlacementUtils.FULL_RANGE, BiomeFilter.biome()
      );
      PlacementUtils.register(
         context,
         GLOWSTONE_EXTRA,
         glowstoneExtra,
         CountPlacement.of(BiasedToBottomInt.of(0, 9)),
         InSquarePlacement.spread(),
         PlacementUtils.RANGE_4_4,
         BlockPredicateFilter.forPredicate(
            BlockPredicate.allOf(
               BlockPredicate.ONLY_IN_AIR_PREDICATE, BlockPredicate.matchesBlocks(Direction.UP, Blocks.NETHERRACK, Blocks.BASALT, Blocks.BLACKSTONE)
            )
         ),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         context,
         GLOWSTONE,
         glowstoneExtra,
         CountPlacement.of(10),
         InSquarePlacement.spread(),
         PlacementUtils.FULL_RANGE,
         BlockPredicateFilter.forPredicate(
            BlockPredicate.allOf(
               BlockPredicate.ONLY_IN_AIR_PREDICATE, BlockPredicate.matchesBlocks(Direction.UP, Blocks.NETHERRACK, Blocks.BASALT, Blocks.BLACKSTONE)
            )
         ),
         BiomeFilter.biome()
      );
      PlacementUtils.register(context, CRIMSON_FOREST_VEGETATION, crimsonForestVegetation, CountOnEveryLayerPlacement.of(6), BiomeFilter.biome());
      PlacementUtils.register(context, WARPED_FOREST_VEGETATION, warpedForestVegetion, CountOnEveryLayerPlacement.of(5), BiomeFilter.biome());
      PlacementUtils.register(context, NETHER_SPROUTS, netherSprouts, CountOnEveryLayerPlacement.of(4), BiomeFilter.biome());
      PlacementUtils.register(
         context, TWISTING_VINES, twistingVines, CountPlacement.of(10), InSquarePlacement.spread(), PlacementUtils.FULL_RANGE, BiomeFilter.biome()
      );
      PlacementUtils.register(
         context, WEEPING_VINES, weepingVines, CountPlacement.of(10), InSquarePlacement.spread(), PlacementUtils.FULL_RANGE, BiomeFilter.biome()
      );
      PlacementUtils.register(
         context,
         PATCH_CRIMSON_ROOTS,
         crimsonRoots,
         PlacementUtils.FULL_RANGE,
         BiomeFilter.biome(),
         CountPlacement.of(96),
         OffsetPlacement.ofTriangle(7, 3),
         BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
      );
      PlacementUtils.register(
         context,
         BASALT_PILLAR,
         basaltPillar,
         CountPlacement.of(10),
         InSquarePlacement.spread(),
         PlacementUtils.FULL_RANGE,
         BlockPredicateFilter.forPredicate(
            BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE, BlockPredicate.not(BlockPredicate.matchesTag(Direction.UP, BlockTags.AIR)))
         ),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         context, SPRING_DELTA, springLavaNether, CountPlacement.of(16), InSquarePlacement.spread(), PlacementUtils.RANGE_4_4, BiomeFilter.biome()
      );
      PlacementUtils.register(
         context, SPRING_CLOSED, springNetherClosed, CountPlacement.of(16), InSquarePlacement.spread(), PlacementUtils.RANGE_10_10, BiomeFilter.biome()
      );
      PlacementUtils.register(
         context, SPRING_CLOSED_DOUBLE, springNetherClosed, CountPlacement.of(32), InSquarePlacement.spread(), PlacementUtils.RANGE_10_10, BiomeFilter.biome()
      );
      PlacementUtils.register(
         context, SPRING_OPEN, springNetherOpen, CountPlacement.of(8), InSquarePlacement.spread(), PlacementUtils.RANGE_4_4, BiomeFilter.biome()
      );
      PlacementUtils.register(context, PATCH_SOUL_FIRE, soulFire, firePlacement(Blocks.SOUL_SOIL));
      PlacementUtils.register(context, PATCH_FIRE, fire, firePlacement(Blocks.NETHERRACK));
   }

   private static List<PlacementModifier> firePlacement(final Block onlyOnBlock) {
      return List.of(
         CountPlacement.of(UniformInt.of(0, 5)),
         InSquarePlacement.spread(),
         PlacementUtils.RANGE_4_4,
         BiomeFilter.biome(),
         CountPlacement.of(96),
         OffsetPlacement.ofTriangle(7, 3),
         BlockPredicateFilter.forPredicate(
            BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE, BlockPredicate.matchesBlocks(Direction.DOWN, onlyOnBlock))
         )
      );
   }
}
