package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;

public class TreeFeature extends Feature<TreeConfiguration> {
   @Block.UpdateFlags
   private static final int BLOCK_UPDATE_FLAGS = 19;

   public TreeFeature(Codec<TreeConfiguration> p_67201_) {
      super(p_67201_);
   }

   public static boolean isVine(LevelSimulatedReader p_67278_, BlockPos p_67279_) {
      return p_67278_.isStateAtPosition(p_67279_, p_225299_ -> p_225299_.is(Blocks.VINE));
   }

   public static boolean isAirOrLeaves(LevelSimulatedReader p_67268_, BlockPos p_67269_) {
      return p_67268_.isStateAtPosition(p_67269_, p_360610_ -> p_360610_.isAir() || p_360610_.is(BlockTags.LEAVES));
   }

   private static void setBlockKnownShape(LevelWriter p_67257_, BlockPos p_67258_, BlockState p_67259_) {
      p_67257_.setBlock(p_67258_, p_67259_, 19);
   }

   public static boolean validTreePos(LevelSimulatedReader p_67273_, BlockPos p_67274_) {
      return p_67273_.isStateAtPosition(p_67274_, p_360611_ -> p_360611_.isAir() || p_360611_.is(BlockTags.REPLACEABLE_BY_TREES));
   }

   private boolean doPlace(
      WorldGenLevel p_225258_,
      RandomSource p_225259_,
      BlockPos p_225260_,
      BiConsumer<BlockPos, BlockState> p_225261_,
      BiConsumer<BlockPos, BlockState> p_225262_,
      FoliagePlacer.FoliageSetter p_273670_,
      TreeConfiguration p_225264_
   ) {
      int i = p_225264_.trunkPlacer.getTreeHeight(p_225259_);
      int j = p_225264_.foliagePlacer.foliageHeight(p_225259_, i, p_225264_);
      int k = i - j;
      int l = p_225264_.foliagePlacer.foliageRadius(p_225259_, k);
      BlockPos blockpos = p_225264_.rootPlacer.<BlockPos>map(p_225286_ -> p_225286_.getTrunkOrigin(p_225260_, p_225259_)).orElse(p_225260_);
      int i1 = Math.min(p_225260_.getY(), blockpos.getY());
      int j1 = Math.max(p_225260_.getY(), blockpos.getY()) + i + 1;
      if (i1 >= p_225258_.getMinY() + 1 && j1 <= p_225258_.getMaxY() + 1) {
         OptionalInt optionalint = p_225264_.minimumSize.minClippedHeight();
         int k1 = this.getMaxFreeTreeHeight(p_225258_, i, blockpos, p_225264_);
         if (k1 >= i || !optionalint.isEmpty() && k1 >= optionalint.getAsInt()) {
            if (p_225264_.rootPlacer.isPresent() && !p_225264_.rootPlacer.get().placeRoots(p_225258_, p_225261_, p_225259_, p_225260_, blockpos, p_225264_)) {
               return false;
            }

            List<FoliagePlacer.FoliageAttachment> list = p_225264_.trunkPlacer.placeTrunk(p_225258_, p_225262_, p_225259_, k1, blockpos, p_225264_);
            list.forEach(p_272582_ -> p_225264_.foliagePlacer.createFoliage(p_225258_, p_273670_, p_225259_, p_225264_, k1, p_272582_, j, l));
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private int getMaxFreeTreeHeight(LevelSimulatedReader p_67216_, int p_67217_, BlockPos p_67218_, TreeConfiguration p_67219_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for (int i = 0; i <= p_67217_ + 1; i++) {
         int j = p_67219_.minimumSize.getSizeAtHeight(p_67217_, i);

         for (int k = -j; k <= j; k++) {
            for (int l = -j; l <= j; l++) {
               blockpos$mutableblockpos.setWithOffset(p_67218_, k, i, l);
               if (!p_67219_.trunkPlacer.isFree(p_67216_, blockpos$mutableblockpos) || !p_67219_.ignoreVines && isVine(p_67216_, blockpos$mutableblockpos)) {
                  return i - 2;
               }
            }
         }
      }

      return p_67217_;
   }

   @Override
   protected void setBlock(LevelWriter p_67221_, BlockPos p_67222_, BlockState p_67223_) {
      setBlockKnownShape(p_67221_, p_67222_, p_67223_);
   }

   @Override
   public final boolean place(FeaturePlaceContext<TreeConfiguration> p_160530_) {
      final WorldGenLevel worldgenlevel = p_160530_.level();
      RandomSource randomsource = p_160530_.random();
      BlockPos blockpos = p_160530_.origin();
      TreeConfiguration treeconfiguration = p_160530_.config();
      Set<BlockPos> set = Sets.newHashSet();
      Set<BlockPos> set1 = Sets.newHashSet();
      final Set<BlockPos> set2 = Sets.newHashSet();
      Set<BlockPos> set3 = Sets.newHashSet();
      BiConsumer<BlockPos, BlockState> biconsumer = (p_160555_, p_160556_) -> {
         set.add(p_160555_.immutable());
         worldgenlevel.setBlock(p_160555_, p_160556_, 19);
      };
      BiConsumer<BlockPos, BlockState> biconsumer1 = (p_160548_, p_160549_) -> {
         set1.add(p_160548_.immutable());
         worldgenlevel.setBlock(p_160548_, p_160549_, 19);
      };
      FoliagePlacer.FoliageSetter foliageplacer$foliagesetter = new FoliagePlacer.FoliageSetter() {
         @Override
         public void set(BlockPos p_272825_, BlockState p_273311_) {
            set2.add(p_272825_.immutable());
            worldgenlevel.setBlock(p_272825_, p_273311_, 19);
         }

         @Override
         public boolean isSet(BlockPos p_272999_) {
            return set2.contains(p_272999_);
         }
      };
      BiConsumer<BlockPos, BlockState> biconsumer2 = (p_160543_, p_160544_) -> {
         set3.add(p_160543_.immutable());
         worldgenlevel.setBlock(p_160543_, p_160544_, 19);
      };
      boolean flag = this.doPlace(worldgenlevel, randomsource, blockpos, biconsumer, biconsumer1, foliageplacer$foliagesetter, treeconfiguration);
      if (flag && (!set1.isEmpty() || !set2.isEmpty())) {
         if (!treeconfiguration.decorators.isEmpty()) {
            TreeDecorator.Context treedecorator$context = new TreeDecorator.Context(worldgenlevel, biconsumer2, randomsource, set1, set2, set);
            treeconfiguration.decorators.forEach(p_225282_ -> p_225282_.place(treedecorator$context));
         }

         return BoundingBox.encapsulatingPositions(Iterables.concat(set, set1, set2, set3)).map(p_225270_ -> {
            DiscreteVoxelShape discretevoxelshape = updateLeaves(worldgenlevel, p_225270_, set1, set3, set);
            StructureTemplate.updateShapeAtEdge(worldgenlevel, 3, discretevoxelshape, p_225270_.minX(), p_225270_.minY(), p_225270_.minZ());
            return true;
         }).orElse(false);
      } else {
         return false;
      }
   }

   private static DiscreteVoxelShape updateLeaves(
      LevelAccessor p_225252_, BoundingBox p_225253_, Set<BlockPos> p_225254_, Set<BlockPos> p_225255_, Set<BlockPos> p_225256_
   ) {
      DiscreteVoxelShape discretevoxelshape = new BitSetDiscreteVoxelShape(p_225253_.getXSpan(), p_225253_.getYSpan(), p_225253_.getZSpan());
      int i = 7;
      List<Set<BlockPos>> list = Lists.newArrayList();

      for (int j = 0; j < 7; j++) {
         list.add(Sets.newHashSet());
      }

      for (BlockPos blockpos : Lists.newArrayList(Sets.union(p_225255_, p_225256_))) {
         if (p_225253_.isInside(blockpos)) {
            discretevoxelshape.fill(blockpos.getX() - p_225253_.minX(), blockpos.getY() - p_225253_.minY(), blockpos.getZ() - p_225253_.minZ());
         }
      }

      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
      int k1 = 0;
      list.get(0).addAll(p_225254_);

      while (true) {
         while (k1 >= 7 || !list.get(k1).isEmpty()) {
            if (k1 >= 7) {
               return discretevoxelshape;
            }

            Iterator<BlockPos> iterator = list.get(k1).iterator();
            BlockPos blockpos1 = iterator.next();
            iterator.remove();
            if (p_225253_.isInside(blockpos1)) {
               if (k1 != 0) {
                  BlockState blockstate = p_225252_.getBlockState(blockpos1);
                  setBlockKnownShape(p_225252_, blockpos1, blockstate.setValue(BlockStateProperties.DISTANCE, k1));
               }

               discretevoxelshape.fill(blockpos1.getX() - p_225253_.minX(), blockpos1.getY() - p_225253_.minY(), blockpos1.getZ() - p_225253_.minZ());

               for (Direction direction : Direction.values()) {
                  blockpos$mutableblockpos.setWithOffset(blockpos1, direction);
                  if (p_225253_.isInside(blockpos$mutableblockpos)) {
                     int k = blockpos$mutableblockpos.getX() - p_225253_.minX();
                     int l = blockpos$mutableblockpos.getY() - p_225253_.minY();
                     int i1 = blockpos$mutableblockpos.getZ() - p_225253_.minZ();
                     if (!discretevoxelshape.isFull(k, l, i1)) {
                        BlockState blockstate1 = p_225252_.getBlockState(blockpos$mutableblockpos);
                        OptionalInt optionalint = LeavesBlock.getOptionalDistanceAt(blockstate1);
                        if (!optionalint.isEmpty()) {
                           int j1 = Math.min(optionalint.getAsInt(), k1 + 1);
                           if (j1 < 7) {
                              list.get(j1).add(blockpos$mutableblockpos.immutable());
                              k1 = Math.min(k1, j1);
                           }
                        }
                     }
                  }
               }
            }
         }

         k1++;
      }
   }

   public static List<BlockPos> getLowestTrunkOrRootOfTree(TreeDecorator.Context p_393099_) {
      List<BlockPos> list = Lists.newArrayList();
      List<BlockPos> list1 = p_393099_.roots();
      List<BlockPos> list2 = p_393099_.logs();
      if (list1.isEmpty()) {
         list.addAll(list2);
      } else if (!list2.isEmpty() && list1.get(0).getY() == list2.get(0).getY()) {
         list.addAll(list2);
         list.addAll(list1);
      } else {
         list.addAll(list1);
      }

      return list;
   }
}
