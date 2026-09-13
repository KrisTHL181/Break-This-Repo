package net.minecraft.world.level.biome;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import net.minecraft.core.QuartPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.jspecify.annotations.Nullable;

public class Climate {
   private static final boolean DEBUG_SLOW_BIOME_SEARCH = false;
   private static final float QUANTIZATION_FACTOR = 10000.0F;
   @VisibleForTesting
   protected static final int PARAMETER_COUNT = 7;

   public static Climate.TargetPoint target(
      final float temperature, final float humidity, final float continentalness, final float erosion, final float depth, final float weirdness
   ) {
      return new Climate.TargetPoint(
         quantizeCoord(temperature),
         quantizeCoord(humidity),
         quantizeCoord(continentalness),
         quantizeCoord(erosion),
         quantizeCoord(depth),
         quantizeCoord(weirdness)
      );
   }

   public static Climate.ParameterPoint parameters(
      final float temperature,
      final float humidity,
      final float continentalness,
      final float erosion,
      final float depth,
      final float weirdness,
      final float offset
   ) {
      return new Climate.ParameterPoint(
         Climate.Parameter.point(temperature),
         Climate.Parameter.point(humidity),
         Climate.Parameter.point(continentalness),
         Climate.Parameter.point(erosion),
         Climate.Parameter.point(depth),
         Climate.Parameter.point(weirdness),
         quantizeCoord(offset)
      );
   }

   public static Climate.ParameterPoint parameters(
      final Climate.Parameter temperature,
      final Climate.Parameter humidity,
      final Climate.Parameter continentalness,
      final Climate.Parameter erosion,
      final Climate.Parameter depth,
      final Climate.Parameter weirdness,
      final float offset
   ) {
      return new Climate.ParameterPoint(temperature, humidity, continentalness, erosion, depth, weirdness, quantizeCoord(offset));
   }

   public static long quantizeCoord(final float coord) {
      return (long)(coord * 10000.0F);
   }

   public static float unquantizeCoord(final long coord) {
      return (float)coord / 10000.0F;
   }

   public static Climate.Sampler empty() {
      DensityFunction zero = DensityFunctions.zero();
      return new Climate.Sampler(zero, zero, zero, zero, zero, zero);
   }

   @VisibleForTesting
   interface DistanceMetric<T> {
      long distance(Climate.RTree.Node<T> node, long[] target);
   }

   public record Parameter(long min, long max) {
      public static final Codec<Climate.Parameter> CODEC = ExtraCodecs.intervalCodec(
         Codec.floatRange(-2.0F, 2.0F),
         "min",
         "max",
         (min, max) -> min.compareTo(max) > 0
            ? DataResult.error(() -> "Cannon construct interval, min > max (" + min + " > " + max + ")")
            : DataResult.success(new Climate.Parameter(Climate.quantizeCoord(min), Climate.quantizeCoord(max))),
         p -> Climate.unquantizeCoord(p.min()),
         p -> Climate.unquantizeCoord(p.max())
      );

      public static Climate.Parameter point(final float min) {
         return span(min, min);
      }

      public static Climate.Parameter span(final float min, final float max) {
         if (min > max) {
            throw new IllegalArgumentException("min > max: " + min + " " + max);
         } else {
            return new Climate.Parameter(Climate.quantizeCoord(min), Climate.quantizeCoord(max));
         }
      }

      public static Climate.Parameter span(final Climate.Parameter min, final Climate.Parameter max) {
         if (min.min() > max.max()) {
            throw new IllegalArgumentException("min > max: " + min + " " + max);
         } else {
            return new Climate.Parameter(min.min(), max.max());
         }
      }

      @Override
      public String toString() {
         return this.min == this.max ? String.format(Locale.ROOT, "%d", this.min) : String.format(Locale.ROOT, "[%d-%d]", this.min, this.max);
      }

      public long distance(final long target) {
         long above = target - this.max;
         long below = this.min - target;
         return above > 0L ? above : Math.max(below, 0L);
      }

      public Climate.Parameter span(final Climate.@Nullable Parameter other) {
         return other == null ? this : new Climate.Parameter(Math.min(this.min, other.min()), Math.max(this.max, other.max()));
      }
   }

   public static class ParameterList<T> {
      private final List<Pair<Climate.ParameterPoint, T>> values;
      private final Climate.RTree<T> index;

      // ===== 修改：RecordCodecBuilder.<Pair<...>>group → i.group =====
      public static <T> Codec<Climate.ParameterList<T>> codec(final MapCodec<T> valueCodec) {
         return ExtraCodecs.nonEmptyList(
               RecordCodecBuilder.<Pair<Climate.ParameterPoint, T>>create(
                     i -> i.group(
                           Climate.ParameterPoint.CODEC.fieldOf("parameters").forGetter(Pair::getFirst),
                           valueCodec.forGetter(Pair::getSecond)
                        )
                        .apply(i, Pair::of)
                  )
                  .listOf()
            )
            .xmap(Climate.ParameterList::new, Climate.ParameterList::values);
      }

      public ParameterList(final List<Pair<Climate.ParameterPoint, T>> values) {
         this.values = values;
         this.index = Climate.RTree.create(values);
      }

      public List<Pair<Climate.ParameterPoint, T>> values() {
         return this.values;
      }

      public T findValue(final Climate.TargetPoint target) {
         return this.findValueIndex(target);
      }

      @VisibleForTesting
      public T findValueBruteForce(final Climate.TargetPoint target) {
         Iterator<Pair<Climate.ParameterPoint, T>> iterator = this.values().iterator();
         Pair<Climate.ParameterPoint, T> first = iterator.next();
         long bestFitness = ((Climate.ParameterPoint)first.getFirst()).fitness(target);
         T best = (T)first.getSecond();

         while (iterator.hasNext()) {
            Pair<Climate.ParameterPoint, T> parameter = iterator.next();
            long fitness = ((Climate.ParameterPoint)parameter.getFirst()).fitness(target);
            if (fitness < bestFitness) {
               bestFitness = fitness;
               best = (T)parameter.getSecond();
            }
         }

         return best;
      }

      public T findValueIndex(final Climate.TargetPoint target) {
         return this.findValueIndex(target, Climate.RTree.Node::distance);
      }

      protected T findValueIndex(final Climate.TargetPoint target, final Climate.DistanceMetric<T> distanceMetric) {
         return this.index.search(target, distanceMetric);
      }
   }

   public record ParameterPoint(
      Climate.Parameter temperature,
      Climate.Parameter humidity,
      Climate.Parameter continentalness,
      Climate.Parameter erosion,
      Climate.Parameter depth,
      Climate.Parameter weirdness,
      long offset
   ) {
      // ===== 修改：RecordCodecBuilder.<Climate.ParameterPoint>group → i.group =====
      public static final Codec<Climate.ParameterPoint> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               Climate.Parameter.CODEC.fieldOf("temperature").forGetter(p -> p.temperature),
               Climate.Parameter.CODEC.fieldOf("humidity").forGetter(p -> p.humidity),
               Climate.Parameter.CODEC.fieldOf("continentalness").forGetter(p -> p.continentalness),
               Climate.Parameter.CODEC.fieldOf("erosion").forGetter(p -> p.erosion),
               Climate.Parameter.CODEC.fieldOf("depth").forGetter(p -> p.depth),
               Climate.Parameter.CODEC.fieldOf("weirdness").forGetter(p -> p.weirdness),
               Codec.floatRange(0.0F, 1.0F).fieldOf("offset").xmap(Climate::quantizeCoord, Climate::unquantizeCoord).forGetter(p -> p.offset)
            )
            .apply(i, Climate.ParameterPoint::new)
      );

      public long fitness(final Climate.TargetPoint target) {
         return Mth.square(this.temperature.distance(target.temperature))
            + Mth.square(this.humidity.distance(target.humidity))
            + Mth.square(this.continentalness.distance(target.continentalness))
            + Mth.square(this.erosion.distance(target.erosion))
            + Mth.square(this.depth.distance(target.depth))
            + Mth.square(this.weirdness.distance(target.weirdness))
            + Mth.square(this.offset);
      }

      List<Climate.Parameter> parameterSpace() {
         return ImmutableList.of(
            this.temperature, this.humidity, this.continentalness, this.erosion, this.depth, this.weirdness, new Climate.Parameter(this.offset, this.offset)
         );
      }
   }

   protected static final class RTree<T> {
      private static final int CHILDREN_PER_NODE = 6;
      private final Climate.RTree.Node<T> root;
      private final ThreadLocal<Climate.RTree.@Nullable Leaf<T>> lastResult = new ThreadLocal<>();

      private RTree(final Climate.RTree.Node<T> root) {
         this.root = root;
      }

      public static <T> Climate.RTree<T> create(final List<Pair<Climate.ParameterPoint, T>> values) {
         if (values.isEmpty()) {
            throw new IllegalArgumentException("Need at least one value to build the search tree.");
         }

         int dimensions = ((Climate.ParameterPoint)values.get(0).getFirst()).parameterSpace().size();
         if (dimensions != 7) {
            throw new IllegalStateException("Expecting parameter space to be 7, got " + dimensions);
         }

         List<Climate.RTree.Leaf<T>> leaves = values.stream()
            .map(p -> new Climate.RTree.Leaf<>((Climate.ParameterPoint)p.getFirst(), p.getSecond()))
            .collect(Collectors.toCollection(ArrayList::new));
         return new Climate.RTree<>(build(dimensions, leaves));
      }

      private static <T> Climate.RTree.Node<T> build(final int dimensions, final List<? extends Climate.RTree.Node<T>> children) {
         if (children.isEmpty()) {
            throw new IllegalStateException("Need at least one child to build a node");
         }

         if (children.size() == 1) {
            return (Climate.RTree.Node<T>)children.get(0);
         }

         if (children.size() <= 6) {
            children.sort(Comparator.comparingLong(leaf -> {
               long totalMagnitude = 0L;

               for (int dx = 0; dx < dimensions; dx++) {
                  Climate.Parameter parameter = leaf.parameterSpace[dx];
                  totalMagnitude += Math.abs((parameter.min() + parameter.max()) / 2L);
               }

               return totalMagnitude;
            }));
            return new Climate.RTree.SubTree<>(children);
         }

         long minCost = Long.MAX_VALUE;
         int minDimension = -1;
         List<Climate.RTree.SubTree<T>> minBuckets = null;

         for (int d = 0; d < dimensions; d++) {
            sort(children, dimensions, d, false);
            List<Climate.RTree.SubTree<T>> buckets = bucketize(children);
            long totalCost = 0L;

            for (Climate.RTree.SubTree<T> bucket : buckets) {
               totalCost += cost(bucket.parameterSpace);
            }

            if (minCost > totalCost) {
               minCost = totalCost;
               minDimension = d;
               minBuckets = buckets;
            }
         }

         sort(minBuckets, dimensions, minDimension, true);
         return new Climate.RTree.SubTree<>(minBuckets.stream().map(b -> build(dimensions, Arrays.asList(b.children))).collect(Collectors.toList()));
      }

      private static <T> void sort(final List<? extends Climate.RTree.Node<T>> children, final int dimensions, final int dimension, final boolean absolute) {
         Comparator<Climate.RTree.Node<T>> comparator = comparator(dimension, absolute);

         for (int d = 1; d < dimensions; d++) {
            comparator = comparator.thenComparing(comparator((dimension + d) % dimensions, absolute));
         }

         children.sort(comparator);
      }

      private static <T> Comparator<Climate.RTree.Node<T>> comparator(final int dimension, final boolean absolute) {
         return Comparator.comparingLong(leaf -> {
            Climate.Parameter parameter = leaf.parameterSpace[dimension];
            long center = (parameter.min() + parameter.max()) / 2L;
            return absolute ? Math.abs(center) : center;
         });
      }

      private static <T> List<Climate.RTree.SubTree<T>> bucketize(final List<? extends Climate.RTree.Node<T>> nodes) {
         List<Climate.RTree.SubTree<T>> buckets = Lists.newArrayList();
         List<Climate.RTree.Node<T>> children = Lists.newArrayList();
         int expectedChildrenCount = (int)Math.pow(6.0, Math.floor(Math.log(nodes.size() - 0.01) / Math.log(6.0)));

         for (Climate.RTree.Node<T> child : nodes) {
            children.add(child);
            if (children.size() >= expectedChildrenCount) {
               buckets.add(new Climate.RTree.SubTree<>(children));
               children = Lists.newArrayList();
            }
         }

         if (!children.isEmpty()) {
            buckets.add(new Climate.RTree.SubTree<>(children));
         }

         return buckets;
      }

      private static long cost(final Climate.Parameter[] parameterSpace) {
         long result = 0L;

         for (Climate.Parameter parameter : parameterSpace) {
            result += Math.abs(parameter.max() - parameter.min());
         }

         return result;
      }

      private static <T> List<Climate.Parameter> buildParameterSpace(final List<? extends Climate.RTree.Node<T>> children) {
         if (children.isEmpty()) {
            throw new IllegalArgumentException("SubTree needs at least one child");
         }

         int dimensions = 7;
         List<Climate.Parameter> bounds = Lists.newArrayList();

         for (int d = 0; d < 7; d++) {
            bounds.add(null);
         }

         for (Climate.RTree.Node<T> child : children) {
            for (int d = 0; d < 7; d++) {
               bounds.set(d, child.parameterSpace[d].span(bounds.get(d)));
            }
         }

         return bounds;
      }

      public T search(final Climate.TargetPoint target, final Climate.DistanceMetric<T> distanceMetric) {
         long[] targetArray = target.toParameterArray();
         Climate.RTree.Leaf<T> leaf = this.root.search(targetArray, this.lastResult.get(), distanceMetric);
         this.lastResult.set(leaf);
         return leaf.value;
      }

      private static final class Leaf<T> extends Climate.RTree.Node<T> {
         private final T value;

         private Leaf(final Climate.ParameterPoint parameterPoint, final T value) {
            super(parameterPoint.parameterSpace());
            this.value = value;
         }

         @Override
         protected Climate.RTree.Leaf<T> search(
            final long[] target, final Climate.RTree.@Nullable Leaf<T> candidate, final Climate.DistanceMetric<T> distanceMetric
         ) {
            return this;
         }
      }

      @VisibleForTesting
      abstract static class Node<T> {
         protected final Climate.Parameter[] parameterSpace;

         protected Node(final List<Climate.Parameter> parameterSpace) {
            this.parameterSpace = parameterSpace.toArray(new Climate.Parameter[0]);
         }

         protected abstract Climate.RTree.Leaf<T> search(
            final long[] target, final Climate.RTree.@Nullable Leaf<T> candidate, final Climate.DistanceMetric<T> distanceMetric
         );

         protected long distance(final long[] target) {
            long distance = 0L;

            for (int i = 0; i < 7; i++) {
               distance += Mth.square(this.parameterSpace[i].distance(target[i]));
            }

            return distance;
         }

         @Override
         public String toString() {
            return Arrays.toString(this.parameterSpace);
         }
      }

      private static final class SubTree<T> extends Climate.RTree.Node<T> {
         private final Climate.RTree.Node<T>[] children;

         public SubTree(final List<? extends Climate.RTree.Node<T>> children) {
            this(Climate.RTree.buildParameterSpace(children), children);
         }

         public SubTree(final List<Climate.Parameter> parameterSpace, final List<? extends Climate.RTree.Node<T>> children) {
            super(parameterSpace);
            this.children = children.toArray(new Climate.RTree.Node[0]);
         }

         @Override
         protected Climate.RTree.Leaf<T> search(
            final long[] target, final Climate.RTree.@Nullable Leaf<T> candidate, final Climate.DistanceMetric<T> distanceMetric
         ) {
            long minDistance = candidate == null ? Long.MAX_VALUE : distanceMetric.distance(candidate, target);
            Climate.RTree.Leaf<T> closestLeaf = candidate;

            for (Climate.RTree.Node<T> child : this.children) {
               long childDistance = distanceMetric.distance(child, target);
               if (minDistance > childDistance) {
                  Climate.RTree.Leaf<T> leaf = child.search(target, closestLeaf, distanceMetric);
                  long leafDistance = child == leaf ? childDistance : distanceMetric.distance(leaf, target);
                  if (minDistance > leafDistance) {
                     minDistance = leafDistance;
                     closestLeaf = leaf;
                  }
               }
            }

            return closestLeaf;
         }
      }
   }

   public record Sampler(
      DensityFunction temperature,
      DensityFunction humidity,
      DensityFunction continentalness,
      DensityFunction erosion,
      DensityFunction depth,
      DensityFunction weirdness
   ) {
      public Climate.TargetPoint sample(final int quartX, final int quartY, final int quartZ) {
         int blockX = QuartPos.toBlock(quartX);
         int blockY = QuartPos.toBlock(quartY);
         int blockZ = QuartPos.toBlock(quartZ);
         DensityFunction.SinglePointContext context = new DensityFunction.SinglePointContext(blockX, blockY, blockZ);
         return Climate.target(
            (float)this.temperature.compute(context),
            (float)this.humidity.compute(context),
            (float)this.continentalness.compute(context),
            (float)this.erosion.compute(context),
            (float)this.depth.compute(context),
            (float)this.weirdness.compute(context)
         );
      }
   }

   public record TargetPoint(long temperature, long humidity, long continentalness, long erosion, long depth, long weirdness) {
      @VisibleForTesting
      long[] toParameterArray() {
         return new long[]{this.temperature, this.humidity, this.continentalness, this.erosion, this.depth, this.weirdness, 0L};
      }
   }
}