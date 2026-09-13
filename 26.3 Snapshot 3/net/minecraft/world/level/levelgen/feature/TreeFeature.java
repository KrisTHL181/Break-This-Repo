package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.featuresize.FeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.rootplacers.RootPlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;

public record TreeFeature(
   BlockStateProvider trunkProvider,
   TrunkPlacer trunkPlacer,
   BlockStateProvider foliageProvider,
   FoliagePlacer foliagePlacer,
   Optional<RootPlacer> rootPlacer,
   FeatureSize minimumSize,
   List<TreeDecorator> decorators,
   boolean ignoreVines,
   BlockStateProvider belowTrunkProvider
) implements Feature {
   public static final BlockPredicate CAN_PLACE_BELOW_TREE_TRUNKS = BlockPredicate.not(BlockPredicate.matchesTag(BlockTags.CANNOT_REPLACE_BELOW_TREE_TRUNK));
   public static final MapCodec<TreeFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            BlockStateProvider.CODEC.fieldOf("trunk_provider").forGetter(TreeFeature::trunkProvider),
            TrunkPlacer.CODEC.fieldOf("trunk_placer").forGetter(TreeFeature::trunkPlacer),
            BlockStateProvider.CODEC.fieldOf("foliage_provider").forGetter(TreeFeature::foliageProvider),
            FoliagePlacer.CODEC.fieldOf("foliage_placer").forGetter(TreeFeature::foliagePlacer),
            RootPlacer.CODEC.optionalFieldOf("root_placer").forGetter(TreeFeature::rootPlacer),
            FeatureSize.CODEC.fieldOf("minimum_size").forGetter(TreeFeature::minimumSize),
            TreeDecorator.CODEC.listOf().fieldOf("decorators").forGetter(TreeFeature::decorators),
            Codec.BOOL.fieldOf("ignore_vines").orElse(false).forGetter(TreeFeature::ignoreVines),
            BlockStateProvider.CODEC.fieldOf("below_trunk_provider").forGetter(TreeFeature::belowTrunkProvider)
         )
         .apply(i, TreeFeature::new)
   );
   private static final @Block.UpdateFlags int BLOCK_UPDATE_FLAGS = 19;

   @Override
   public MapCodec<TreeFeature> codec() {
      return CODEC;
   }

   public static BlockStateProvider defaultPlaceBelowTreeTrunkProvider(final HolderGetter<Biome> biomes) {
      return RuleBasedStateProvider.ifTrueThenProvide(CAN_PLACE_BELOW_TREE_TRUNKS, Blocks.DIRT);
   }

   public static boolean isVine(final LevelSimulatedReader level, final BlockPos pos) {
      return level.isStateAtPosition(pos, state -> state.is(Blocks.VINE));
   }

   public static boolean isAirOrLeaves(final LevelSimulatedReader level, final BlockPos pos) {
      return level.isStateAtPosition(pos, state -> state.isAir() || state.is(BlockTags.LEAVES));
   }

   private static void setBlockKnownShape(final LevelWriter level, final BlockPos pos, final BlockState blockState) {
      level.setBlock(pos, blockState, 19);
   }

   public static boolean validTreePos(final LevelSimulatedReader level, final BlockPos pos) {
      return level.isStateAtPosition(pos, state -> state.isAir() || state.is(BlockTags.REPLACEABLE_BY_TREES));
   }

   private boolean doPlace(
      final WorldGenLevel level,
      final RandomSource random,
      final BlockPos origin,
      final BiConsumer<BlockPos, BlockState> rootSetter,
      final BiConsumer<BlockPos, BlockState> trunkSetter,
      final FoliagePlacer.FoliageSetter foliageSetter
   ) {
      int treeHeight = this.trunkPlacer.getTreeHeight(random);
      int foliageHeight = this.foliagePlacer.foliageHeight(random, treeHeight, this);
      int trunkHeight = treeHeight - foliageHeight;
      int leafRadius = this.foliagePlacer.foliageRadius(random, trunkHeight);
      BlockPos trunkOrigin = this.rootPlacer.<BlockPos>map(rootPlacer -> rootPlacer.getTrunkOrigin(origin, random)).orElse(origin);
      int minY = Math.min(origin.getY(), trunkOrigin.getY());
      int maxY = Math.max(origin.getY(), trunkOrigin.getY()) + treeHeight + 1;
      if (minY >= level.getMinY() + 1 && maxY <= level.getMaxY() + 1) {
         OptionalInt minClippedHeight = this.minimumSize.minClippedHeight();
         int clippedTreeHeight = this.getMaxFreeTreeHeight(level, treeHeight, trunkOrigin);
         if (clippedTreeHeight >= treeHeight || !minClippedHeight.isEmpty() && clippedTreeHeight >= minClippedHeight.getAsInt()) {
            if (this.rootPlacer.isPresent() && !this.rootPlacer.get().placeRoots(level, rootSetter, random, origin, trunkOrigin, this)) {
               return false;
            }

            List<FoliagePlacer.FoliageAttachment> foliageAttachments = this.trunkPlacer
               .placeTrunk(level, trunkSetter, random, clippedTreeHeight, trunkOrigin, this);
            foliageAttachments.forEach(
               foliageAttachment -> this.foliagePlacer
                  .createFoliage(level, foliageSetter, random, this, clippedTreeHeight, foliageAttachment, foliageHeight, leafRadius)
            );
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private int getMaxFreeTreeHeight(final WorldGenLevel level, final int maxTreeHeight, final BlockPos treePos) {
      BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

      for (int y = 0; y <= maxTreeHeight + 1; y++) {
         int r = this.minimumSize.getSizeAtHeight(maxTreeHeight, y);

         for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
               blockPos.setWithOffset(treePos, x, y, z);
               if (!this.trunkPlacer.isFree(level, blockPos) || !this.ignoreVines && isVine(level, blockPos)) {
                  return y - 2;
               }
            }
         }
      }

      return maxTreeHeight;
   }

   @Override
   public void setBlock(final LevelWriter level, final BlockPos pos, final BlockState blockState) {
      setBlockKnownShape(level, pos, blockState);
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      Set<BlockPos> rootPositions = Sets.newHashSet();
      Set<BlockPos> trunks = Sets.newHashSet();
      final Set<BlockPos> foliage = Sets.newHashSet();
      Set<BlockPos> decorations = Sets.newHashSet();
      BiConsumer<BlockPos, BlockState> rootSetter = (pos, state) -> {
         rootPositions.add(pos.immutable());
         level.setBlock(pos, state, 19);
      };
      BiConsumer<BlockPos, BlockState> trunkSetter = (pos, state) -> {
         trunks.add(pos.immutable());
         level.setBlock(pos, state, 19);
      };
      FoliagePlacer.FoliageSetter foliageSetter = new FoliagePlacer.FoliageSetter() {
         @Override
         public void set(final BlockPos pos, final BlockState state) {
            foliage.add(pos.immutable());
            level.setBlock(pos, state, 19);
         }

         @Override
         public boolean isSet(final BlockPos pos) {
            return foliage.contains(pos);
         }
      };
      BiConsumer<BlockPos, BlockState> decorationSetter = (pos, state) -> {
         decorations.add(pos.immutable());
         level.setBlock(pos, state, 19);
      };
      boolean result = this.doPlace(level, random, origin, rootSetter, trunkSetter, foliageSetter);
      if (result && (!trunks.isEmpty() || !foliage.isEmpty())) {
         if (!this.decorators.isEmpty()) {
            TreeDecorator.Context decoratorContext = new TreeDecorator.Context(level, decorationSetter, random, trunks, foliage, rootPositions);
            this.decorators.forEach(decorator -> decorator.place(decoratorContext));
         }

         return BoundingBox.encapsulatingPositions(Iterables.concat(rootPositions, trunks, foliage, decorations)).map(bounds -> {
            DiscreteVoxelShape shape = updateLeaves(level, bounds, trunks, decorations, rootPositions);
            StructureTemplate.updateShapeAtEdge(level, 3, shape, bounds.minX(), bounds.minY(), bounds.minZ());
            return true;
         }).orElse(false);
      } else {
         return false;
      }
   }

   private static DiscreteVoxelShape updateLeaves(
      final LevelAccessor level, final BoundingBox bounds, final Set<BlockPos> logs, final Set<BlockPos> decorationSet, final Set<BlockPos> rootPositions
   ) {
      DiscreteVoxelShape shape = new BitSetDiscreteVoxelShape(bounds.getXSpan(), bounds.getYSpan(), bounds.getZSpan());
      int maxDistance = 7;
      List<Set<BlockPos>> toCheck = Lists.newArrayList();

      for (int i = 0; i < 7; i++) {
         toCheck.add(Sets.newHashSet());
      }

      for (BlockPos pos : Lists.newArrayList(Sets.union(decorationSet, rootPositions))) {
         if (bounds.isInside(pos)) {
            shape.fill(pos.getX() - bounds.minX(), pos.getY() - bounds.minY(), pos.getZ() - bounds.minZ());
         }
      }

      BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();
      int smallestDistance = 0;
      toCheck.get(0).addAll(logs);

      while (true) {
         while (smallestDistance >= 7 || !toCheck.get(smallestDistance).isEmpty()) {
            if (smallestDistance >= 7) {
               return shape;
            }

            Iterator<BlockPos> iterator = toCheck.get(smallestDistance).iterator();
            BlockPos pos = iterator.next();
            iterator.remove();
            if (bounds.isInside(pos)) {
               if (smallestDistance != 0) {
                  BlockState state = level.getBlockState(pos);
                  setBlockKnownShape(level, pos, state.setValue(BlockStateProperties.DISTANCE, smallestDistance));
               }

               shape.fill(pos.getX() - bounds.minX(), pos.getY() - bounds.minY(), pos.getZ() - bounds.minZ());

               for (Direction direction : Direction.values()) {
                  neighborPos.setWithOffset(pos, direction);
                  if (bounds.isInside(neighborPos)) {
                     int xInShape = neighborPos.getX() - bounds.minX();
                     int yInShape = neighborPos.getY() - bounds.minY();
                     int zinShape = neighborPos.getZ() - bounds.minZ();
                     if (!shape.isFull(xInShape, yInShape, zinShape)) {
                        BlockState currentState = level.getBlockState(neighborPos);
                        OptionalInt distance = LeavesBlock.getOptionalDistanceAt(currentState);
                        if (!distance.isEmpty()) {
                           int newDistance = Math.min(distance.getAsInt(), smallestDistance + 1);
                           if (newDistance < 7) {
                              toCheck.get(newDistance).add(neighborPos.immutable());
                              smallestDistance = Math.min(smallestDistance, newDistance);
                           }
                        }
                     }
                  }
               }
            }
         }

         smallestDistance++;
      }
   }

   public static List<BlockPos> getLowestTrunkOrRootOfTree(final TreeDecorator.Context context) {
      List<BlockPos> blockPositions = Lists.newArrayList();
      List<BlockPos> roots = context.roots();
      List<BlockPos> logs = context.logs();
      if (roots.isEmpty()) {
         blockPositions.addAll(logs);
      } else if (!logs.isEmpty() && roots.get(0).getY() == logs.get(0).getY()) {
         blockPositions.addAll(logs);
         blockPositions.addAll(roots);
      } else {
         blockPositions.addAll(roots);
      }

      return blockPositions;
   }

   public static class Builder {
      public final BlockStateProvider trunkProvider;
      private final TrunkPlacer trunkPlacer;
      public final BlockStateProvider foliageProvider;
      private final FoliagePlacer foliagePlacer;
      private final Optional<RootPlacer> rootPlacer;
      private final FeatureSize minimumSize;
      private List<TreeDecorator> decorators = List.of();
      private boolean ignoreVines;
      private BlockStateProvider belowTrunkProvider;

      public Builder(
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

      public Builder(
         final BlockStateProvider trunkProvider,
         final TrunkPlacer trunkPlacer,
         final BlockStateProvider foliageProvider,
         final FoliagePlacer foliagePlacer,
         final FeatureSize minimumSize,
         final BlockStateProvider belowTrunkProvider
      ) {
         this(trunkProvider, trunkPlacer, foliageProvider, foliagePlacer, Optional.empty(), minimumSize, belowTrunkProvider);
      }

      public TreeFeature.Builder belowTrunkProvider(final BlockStateProvider belowTrunkProvider) {
         this.belowTrunkProvider = belowTrunkProvider;
         return this;
      }

      public TreeFeature.Builder decorators(final List<TreeDecorator> decorators) {
         this.decorators = decorators;
         return this;
      }

      public TreeFeature.Builder ignoreVines() {
         this.ignoreVines = true;
         return this;
      }

      public TreeFeature build() {
         return new TreeFeature(
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
