package net.minecraft.core;

import com.google.common.collect.AbstractIterator;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.concurrent.Immutable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;

@Immutable
public class BlockPos extends Vec3i {
   public static final Codec<BlockPos> CODEC = Codec.INT_STREAM
      .comapFlatMap(
         input -> Util.fixedSize(input, 3).map(ints -> new BlockPos(ints[0], ints[1], ints[2])), pos -> IntStream.of(pos.getX(), pos.getY(), pos.getZ())
      )
      .stable();
   public static final StreamCodec<ByteBuf, BlockPos> STREAM_CODEC = new StreamCodec<ByteBuf, BlockPos>() {
      public BlockPos decode(final ByteBuf input) {
         return FriendlyByteBuf.readBlockPos(input);
      }

      public void encode(final ByteBuf output, final BlockPos value) {
         FriendlyByteBuf.writeBlockPos(output, value);
      }
   };
   public static final BlockPos ZERO = new BlockPos(0, 0, 0);
   public static final int PACKED_HORIZONTAL_LENGTH = 1 + Mth.log2(Mth.smallestEncompassingPowerOfTwo(30000000));
   public static final int PACKED_Y_LENGTH = 64 - 2 * PACKED_HORIZONTAL_LENGTH;
   private static final long PACKED_X_MASK = (1L << PACKED_HORIZONTAL_LENGTH) - 1L;
   private static final long PACKED_Y_MASK = (1L << PACKED_Y_LENGTH) - 1L;
   private static final long PACKED_Z_MASK = (1L << PACKED_HORIZONTAL_LENGTH) - 1L;
   private static final int Y_OFFSET = 0;
   private static final int Z_OFFSET = PACKED_Y_LENGTH;
   private static final int X_OFFSET = PACKED_Y_LENGTH + PACKED_HORIZONTAL_LENGTH;
   public static final int MAX_HORIZONTAL_COORDINATE = (1 << PACKED_HORIZONTAL_LENGTH) / 2 - 1;

   public BlockPos(final int x, final int y, final int z) {
      super(x, y, z);
   }

   public static long offset(final long blockNode, final Direction offset) {
      return offset(blockNode, offset.getStepX(), offset.getStepY(), offset.getStepZ());
   }

   public static long offset(final long blockNode, final int stepX, final int stepY, final int stepZ) {
      return asLong(getX(blockNode) + stepX, getY(blockNode) + stepY, getZ(blockNode) + stepZ);
   }

   public static int getX(final long blockNode) {
      return (int)(blockNode << 64 - X_OFFSET - PACKED_HORIZONTAL_LENGTH >> 64 - PACKED_HORIZONTAL_LENGTH);
   }

   public static int getY(final long blockNode) {
      return (int)(blockNode << 64 - PACKED_Y_LENGTH >> 64 - PACKED_Y_LENGTH);
   }

   public static int getZ(final long blockNode) {
      return (int)(blockNode << 64 - Z_OFFSET - PACKED_HORIZONTAL_LENGTH >> 64 - PACKED_HORIZONTAL_LENGTH);
   }

   public static BlockPos of(final long blockNode) {
      return new BlockPos(getX(blockNode), getY(blockNode), getZ(blockNode));
   }

   public static BlockPos containing(final double x, final double y, final double z) {
      return new BlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z));
   }

   public static BlockPos containing(final Position pos) {
      return containing(pos.x(), pos.y(), pos.z());
   }

   public static BlockPos min(final BlockPos a, final BlockPos b) {
      return new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
   }

   public static BlockPos max(final BlockPos a, final BlockPos b) {
      return new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
   }

   public long asLong() {
      return asLong(this.getX(), this.getY(), this.getZ());
   }

   public static long asLong(final int x, final int y, final int z) {
      long node = 0L;
      node |= (x & PACKED_X_MASK) << X_OFFSET;
      node |= (y & PACKED_Y_MASK) << 0;
      return node | (z & PACKED_Z_MASK) << Z_OFFSET;
   }

   public static long getFlatIndex(final long neighborBlockNode) {
      return neighborBlockNode & -16L;
   }

   public BlockPos offset(final int x, final int y, final int z) {
      return x == 0 && y == 0 && z == 0 ? this : new BlockPos(this.getX() + x, this.getY() + y, this.getZ() + z);
   }

   public BlockPos offset(final Vec3i vec) {
      return this.offset(vec.getX(), vec.getY(), vec.getZ());
   }

   public BlockPos subtract(final Vec3i vec) {
      return this.offset(-vec.getX(), -vec.getY(), -vec.getZ());
   }

   public BlockPos multiply(final int scale) {
      if (scale == 1) {
         return this;
      } else {
         return scale == 0 ? ZERO : new BlockPos(this.getX() * scale, this.getY() * scale, this.getZ() * scale);
      }
   }

   public BlockPos above() {
      return this.relative(Direction.UP);
   }

   public BlockPos above(final int steps) {
      return this.relative(Direction.UP, steps);
   }

   public BlockPos below() {
      return this.relative(Direction.DOWN);
   }

   public BlockPos below(final int steps) {
      return this.relative(Direction.DOWN, steps);
   }

   public BlockPos north() {
      return this.relative(Direction.NORTH);
   }

   public BlockPos north(final int steps) {
      return this.relative(Direction.NORTH, steps);
   }

   public BlockPos south() {
      return this.relative(Direction.SOUTH);
   }

   public BlockPos south(final int steps) {
      return this.relative(Direction.SOUTH, steps);
   }

   public BlockPos west() {
      return this.relative(Direction.WEST);
   }

   public BlockPos west(final int steps) {
      return this.relative(Direction.WEST, steps);
   }

   public BlockPos east() {
      return this.relative(Direction.EAST);
   }

   public BlockPos east(final int steps) {
      return this.relative(Direction.EAST, steps);
   }

   public BlockPos relative(final Direction direction) {
      return new BlockPos(this.getX() + direction.getStepX(), this.getY() + direction.getStepY(), this.getZ() + direction.getStepZ());
   }

   public BlockPos relative(final Direction direction, final int steps) {
      return steps == 0
         ? this
         : new BlockPos(this.getX() + direction.getStepX() * steps, this.getY() + direction.getStepY() * steps, this.getZ() + direction.getStepZ() * steps);
   }

   public BlockPos relative(final Direction.Axis axis, final int steps) {
      if (steps == 0) {
         return this;
      }

      int xStep = axis == Direction.Axis.X ? steps : 0;
      int yStep = axis == Direction.Axis.Y ? steps : 0;
      int zStep = axis == Direction.Axis.Z ? steps : 0;
      return new BlockPos(this.getX() + xStep, this.getY() + yStep, this.getZ() + zStep);
   }

   public BlockPos rotate(final Rotation rotation) {
      return switch (rotation) {
         case CLOCKWISE_90 -> new BlockPos(-this.getZ(), this.getY(), this.getX());
         case CLOCKWISE_180 -> new BlockPos(-this.getX(), this.getY(), -this.getZ());
         case COUNTERCLOCKWISE_90 -> new BlockPos(this.getZ(), this.getY(), -this.getX());
         case NONE -> this;
      };
   }

   public BlockPos cross(final Vec3i upVector) {
      return new BlockPos(
         this.getY() * upVector.getZ() - this.getZ() * upVector.getY(),
         this.getZ() * upVector.getX() - this.getX() * upVector.getZ(),
         this.getX() * upVector.getY() - this.getY() * upVector.getX()
      );
   }

   public BlockPos atY(final int y) {
      return new BlockPos(this.getX(), y, this.getZ());
   }

   public BlockPos immutable() {
      return this;
   }

   public BlockPos.MutableBlockPos mutable() {
      return new BlockPos.MutableBlockPos(this.getX(), this.getY(), this.getZ());
   }

   public Vec3 clampLocationWithin(final Vec3 location) {
      return new Vec3(
         Mth.clamp(location.x, this.getX() + 1.0E-5F, this.getX() + 1.0 - 1.0E-5F),
         Mth.clamp(location.y, this.getY() + 1.0E-5F, this.getY() + 1.0 - 1.0E-5F),
         Mth.clamp(location.z, this.getZ() + 1.0E-5F, this.getZ() + 1.0 - 1.0E-5F)
      );
   }

   public static Iterable<BlockPos> randomInCube(final RandomSource random, final int limit, final BlockPos center, final int sizeToScanInAllDirections) {
      return randomBetweenClosed(
         random,
         limit,
         center.getX() - sizeToScanInAllDirections,
         center.getY() - sizeToScanInAllDirections,
         center.getZ() - sizeToScanInAllDirections,
         center.getX() + sizeToScanInAllDirections,
         center.getY() + sizeToScanInAllDirections,
         center.getZ() + sizeToScanInAllDirections
      );
   }

   @Deprecated
   public static Stream<BlockPos> squareOutSouthEast(final BlockPos from) {
      return Stream.of(from, from.south(), from.east(), from.south().east());
   }

   public static Iterable<BlockPos> randomBetweenClosed(
      final RandomSource random, final int limit, final int minX, final int minY, final int minZ, final int maxX, final int maxY, final int maxZ
   ) {
      int width = maxX - minX + 1;
      int height = maxY - minY + 1;
      int depth = maxZ - minZ + 1;
      return () -> new AbstractIterator<BlockPos>() {
         private final BlockPos.MutableBlockPos nextPos = new BlockPos.MutableBlockPos();
         private int counter = limit;

         protected BlockPos computeNext() {
            if (this.counter <= 0) {
               return (BlockPos)this.endOfData();
            }

            BlockPos next = this.nextPos.set(minX + random.nextInt(width), minY + random.nextInt(height), minZ + random.nextInt(depth));
            this.counter--;
            return next;
         }
      };
   }

   public static Iterable<BlockPos> withinManhattan(final BlockPos origin, final int reachX, final int reachY, final int reachZ) {
      int maxDepth = reachX + reachY + reachZ;
      int originX = origin.getX();
      int originY = origin.getY();
      int originZ = origin.getZ();
      return () -> new AbstractIterator<BlockPos>() {
         private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
         private int currentDepth;
         private int maxX;
         private int maxY;
         private int x;
         private int y;
         private boolean zMirror;

         protected BlockPos computeNext() {
            if (this.zMirror) {
               this.zMirror = false;
               this.cursor.setZ(originZ - (this.cursor.getZ() - originZ));
               return this.cursor;
            }

            BlockPos found;
            for (found = null; found == null; this.y++) {
               if (this.y > this.maxY) {
                  this.x++;
                  if (this.x > this.maxX) {
                     this.currentDepth++;
                     if (this.currentDepth > maxDepth) {
                        return (BlockPos)this.endOfData();
                     }

                     this.maxX = Math.min(reachX, this.currentDepth);
                     this.x = -this.maxX;
                  }

                  this.maxY = Math.min(reachY, this.currentDepth - Math.abs(this.x));
                  this.y = -this.maxY;
               }

               int xx = this.x;
               int yy = this.y;
               int zz = this.currentDepth - Math.abs(xx) - Math.abs(yy);
               if (zz <= reachZ) {
                  this.zMirror = zz != 0;
                  found = this.cursor.set(originX + xx, originY + yy, originZ + zz);
               }
            }

            return found;
         }
      };
   }

   public static Optional<BlockPos> findClosestMatch(
      final BlockPos startPos, final int horizontalSearchRadius, final int verticalSearchRadius, final Predicate<BlockPos> predicate
   ) {
      for (BlockPos blockPos : withinManhattan(startPos, horizontalSearchRadius, verticalSearchRadius, horizontalSearchRadius)) {
         if (predicate.test(blockPos)) {
            return Optional.of(blockPos);
         }
      }

      return Optional.empty();
   }

   public static Stream<BlockPos> withinManhattanStream(final BlockPos origin, final int reachX, final int reachY, final int reachZ) {
      return StreamSupport.stream(withinManhattan(origin, reachX, reachY, reachZ).spliterator(), false);
   }

   public static Iterable<BlockPos> betweenClosed(final AABB box) {
      BlockPos startPos = containing(box.minX, box.minY, box.minZ);
      BlockPos endPos = containing(box.maxX, box.maxY, box.maxZ);
      return betweenClosed(startPos, endPos);
   }

   public static Iterable<BlockPos> betweenClosed(final BlockPos a, final BlockPos b) {
      return betweenClosed(
         Math.min(a.getX(), b.getX()),
         Math.min(a.getY(), b.getY()),
         Math.min(a.getZ(), b.getZ()),
         Math.max(a.getX(), b.getX()),
         Math.max(a.getY(), b.getY()),
         Math.max(a.getZ(), b.getZ())
      );
   }

   public static Stream<BlockPos> betweenClosedStream(final BlockPos a, final BlockPos b) {
      return StreamSupport.stream(betweenClosed(a, b).spliterator(), false);
   }

   public static Stream<BlockPos> betweenClosedStream(final BoundingBox boundingBox) {
      return betweenClosedStream(
         Math.min(boundingBox.minX(), boundingBox.maxX()),
         Math.min(boundingBox.minY(), boundingBox.maxY()),
         Math.min(boundingBox.minZ(), boundingBox.maxZ()),
         Math.max(boundingBox.minX(), boundingBox.maxX()),
         Math.max(boundingBox.minY(), boundingBox.maxY()),
         Math.max(boundingBox.minZ(), boundingBox.maxZ())
      );
   }

   public static Stream<BlockPos> betweenClosedStream(final AABB box) {
      return betweenClosedStream(Mth.floor(box.minX), Mth.floor(box.minY), Mth.floor(box.minZ), Mth.floor(box.maxX), Mth.floor(box.maxY), Mth.floor(box.maxZ));
   }

   public static Stream<BlockPos> betweenClosedStream(final int minX, final int minY, final int minZ, final int maxX, final int maxY, final int maxZ) {
      return StreamSupport.stream(betweenClosed(minX, minY, minZ, maxX, maxY, maxZ).spliterator(), false);
   }

   public static Iterable<BlockPos> betweenClosed(final int minX, final int minY, final int minZ, final int maxX, final int maxY, final int maxZ) {
      int width = maxX - minX + 1;
      int height = maxY - minY + 1;
      int depth = maxZ - minZ + 1;
      int end = width * height * depth;
      return () -> new AbstractIterator<BlockPos>() {
         private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
         private int index;

         protected BlockPos computeNext() {
            if (this.index == end) {
               return (BlockPos)this.endOfData();
            }

            int x = this.index % width;
            int slice = this.index / width;
            int y = slice % height;
            int z = slice / height;
            this.index++;
            return this.cursor.set(minX + x, minY + y, minZ + z);
         }
      };
   }

   public static Iterable<BlockPos> neighborColumn(final int startX, final int startY, final int startZ, final int endY) {
      int yDirection = endY > startY ? 1 : -1;
      int height = Math.abs(endY - startY) + 1;
      Vec3i[] steps = new Vec3i[]{
         new Vec3i(0, 0, 0), Direction.NORTH.getUnitVec3i(), Direction.EAST.getUnitVec3i(), Direction.SOUTH.getUnitVec3i(), Direction.WEST.getUnitVec3i()
      };
      int stepCount = steps.length * height;
      return () -> new AbstractIterator<BlockPos>() {
         private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
         private int index;

         protected BlockPos computeNext() {
            if (this.index == stepCount) {
               return (BlockPos)this.endOfData();
            }

            int y = this.index % height;
            int stepIndex = this.index / height;
            Vec3i step = steps[stepIndex];
            this.index++;
            return this.cursor.set(startX + step.getX(), startY + y * yDirection, startZ + step.getZ());
         }
      };
   }

   public static Iterable<BlockPos.MutableBlockPos> spiralAround(
      final BlockPos center, final int radius, final Direction firstDirection, final Direction secondDirection
   ) {
      Validate.validState(firstDirection.getAxis() != secondDirection.getAxis(), "The two directions cannot be on the same axis", new Object[0]);
      return () -> new AbstractIterator<BlockPos.MutableBlockPos>() {
         private final Direction[] directions = new Direction[]{firstDirection, secondDirection, firstDirection.getOpposite(), secondDirection.getOpposite()};
         private final BlockPos.MutableBlockPos cursor = center.mutable().move(secondDirection);
         private final int legs = 4 * radius;
         private int leg = -1;
         private int legSize;
         private int legIndex;
         private int lastX = this.cursor.getX();
         private int lastY = this.cursor.getY();
         private int lastZ = this.cursor.getZ();

         protected BlockPos.MutableBlockPos computeNext() {
            this.cursor.set(this.lastX, this.lastY, this.lastZ).move(this.directions[(this.leg + 4) % 4]);
            this.lastX = this.cursor.getX();
            this.lastY = this.cursor.getY();
            this.lastZ = this.cursor.getZ();
            if (this.legIndex >= this.legSize) {
               if (this.leg >= this.legs) {
                  return (BlockPos.MutableBlockPos)this.endOfData();
               }

               this.leg++;
               this.legIndex = 0;
               this.legSize = this.leg / 2 + 1;
            }

            this.legIndex++;
            return this.cursor;
         }
      };
   }

   public static int breadthFirstTraversal(
      final BlockPos startPos,
      final int maxDepth,
      final int maxCount,
      final BiConsumer<BlockPos, Consumer<BlockPos>> neighbourProvider,
      final Function<BlockPos, BlockPos.TraversalNodeStatus> nodeProcessor
   ) {
      record Node(BlockPos pos, int depth) {
      }

      Queue<Node> nodes = new ArrayDeque<>();
      LongSet visited = new LongOpenHashSet();
      nodes.add(new Node(startPos, 0));
      int count = 0;

      while (!nodes.isEmpty()) {
         Node node = nodes.poll();
         BlockPos currentPos = node.pos;
         int depth = node.depth;
         long currentPosLong = currentPos.asLong();
         if (visited.add(currentPosLong)) {
            BlockPos.TraversalNodeStatus next = nodeProcessor.apply(currentPos);
            if (next != BlockPos.TraversalNodeStatus.SKIP) {
               if (next == BlockPos.TraversalNodeStatus.STOP) {
                  break;
               }

               if (++count >= maxCount) {
                  return count;
               }

               if (depth < maxDepth) {
                  neighbourProvider.accept(currentPos, pos -> nodes.add(new Node(pos, depth + 1)));
               }
            }
         }
      }

      return count;
   }

   public static Iterable<BlockPos> betweenCornersInDirection(final AABB aabb, final Vec3 direction) {
      Vec3 minCorner = aabb.getMinPosition();
      int firstCornerX = Mth.floor(minCorner.x());
      int firstCornerY = Mth.floor(minCorner.y());
      int firstCornerZ = Mth.floor(minCorner.z());
      Vec3 maxCorner = aabb.getMaxPosition();
      int secondCornerX = Mth.floor(maxCorner.x());
      int secondCornerY = Mth.floor(maxCorner.y());
      int secondCornerZ = Mth.floor(maxCorner.z());
      return betweenCornersInDirection(firstCornerX, firstCornerY, firstCornerZ, secondCornerX, secondCornerY, secondCornerZ, direction);
   }

   public static Iterable<BlockPos> betweenCornersInDirection(final BlockPos firstCorner, final BlockPos secondCorner, final Vec3 direction) {
      return betweenCornersInDirection(
         firstCorner.getX(), firstCorner.getY(), firstCorner.getZ(), secondCorner.getX(), secondCorner.getY(), secondCorner.getZ(), direction
      );
   }

   public static Iterable<BlockPos> betweenCornersInDirection(
      final int firstCornerX,
      final int firstCornerY,
      final int firstCornerZ,
      final int secondCornerX,
      final int secondCornerY,
      final int secondCornerZ,
      final Vec3 direction
   ) {
      int minCornerX = Math.min(firstCornerX, secondCornerX);
      int minCornerY = Math.min(firstCornerY, secondCornerY);
      int minCornerZ = Math.min(firstCornerZ, secondCornerZ);
      int maxCornerX = Math.max(firstCornerX, secondCornerX);
      int maxCornerY = Math.max(firstCornerY, secondCornerY);
      int maxCornerZ = Math.max(firstCornerZ, secondCornerZ);
      int diffX = maxCornerX - minCornerX;
      int diffY = maxCornerY - minCornerY;
      int diffZ = maxCornerZ - minCornerZ;
      int startCornerX = direction.x >= 0.0 ? minCornerX : maxCornerX;
      int startCornerY = direction.y >= 0.0 ? minCornerY : maxCornerY;
      int startCornerZ = direction.z >= 0.0 ? minCornerZ : maxCornerZ;
      List<Direction.Axis> axes = Direction.axisStepOrder(direction);
      Direction.Axis firstVisitAxis = axes.get(0);
      Direction.Axis secondVisitAxis = axes.get(1);
      Direction.Axis thirdVisitAxis = axes.get(2);
      Direction firstVisitDir = direction.get(firstVisitAxis) >= 0.0 ? firstVisitAxis.getPositive() : firstVisitAxis.getNegative();
      Direction secondVisitDir = direction.get(secondVisitAxis) >= 0.0 ? secondVisitAxis.getPositive() : secondVisitAxis.getNegative();
      Direction thirdVisitDir = direction.get(thirdVisitAxis) >= 0.0 ? thirdVisitAxis.getPositive() : thirdVisitAxis.getNegative();
      int firstMax = firstVisitAxis.choose(diffX, diffY, diffZ);
      int secondMax = secondVisitAxis.choose(diffX, diffY, diffZ);
      int thirdMax = thirdVisitAxis.choose(diffX, diffY, diffZ);
      return () -> new AbstractIterator<BlockPos>() {
         private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
         private int firstIndex;
         private int secondIndex;
         private int thirdIndex;
         private boolean end;
         private final int firstDirX = firstVisitDir.getStepX();
         private final int firstDirY = firstVisitDir.getStepY();
         private final int firstDirZ = firstVisitDir.getStepZ();
         private final int secondDirX = secondVisitDir.getStepX();
         private final int secondDirY = secondVisitDir.getStepY();
         private final int secondDirZ = secondVisitDir.getStepZ();
         private final int thirdDirX = thirdVisitDir.getStepX();
         private final int thirdDirY = thirdVisitDir.getStepY();
         private final int thirdDirZ = thirdVisitDir.getStepZ();

         protected BlockPos computeNext() {
            if (this.end) {
               return (BlockPos)this.endOfData();
            }

            this.cursor
               .set(
                  startCornerX + this.firstDirX * this.firstIndex + this.secondDirX * this.secondIndex + this.thirdDirX * this.thirdIndex,
                  startCornerY + this.firstDirY * this.firstIndex + this.secondDirY * this.secondIndex + this.thirdDirY * this.thirdIndex,
                  startCornerZ + this.firstDirZ * this.firstIndex + this.secondDirZ * this.secondIndex + this.thirdDirZ * this.thirdIndex
               );
            if (this.thirdIndex < thirdMax) {
               this.thirdIndex++;
            } else if (this.secondIndex < secondMax) {
               this.secondIndex++;
               this.thirdIndex = 0;
            } else if (this.firstIndex < firstMax) {
               this.firstIndex++;
               this.thirdIndex = 0;
               this.secondIndex = 0;
            } else {
               this.end = true;
            }

            return this.cursor;
         }
      };
   }

   public static class MutableBlockPos extends BlockPos {
      public MutableBlockPos() {
         this(0, 0, 0);
      }

      public MutableBlockPos(final int x, final int y, final int z) {
         super(x, y, z);
      }

      public MutableBlockPos(final double x, final double y, final double z) {
         this(Mth.floor(x), Mth.floor(y), Mth.floor(z));
      }

      @Override
      public BlockPos offset(final int x, final int y, final int z) {
         return super.offset(x, y, z).immutable();
      }

      @Override
      public BlockPos multiply(final int scale) {
         return super.multiply(scale).immutable();
      }

      @Override
      public BlockPos relative(final Direction direction, final int steps) {
         return super.relative(direction, steps).immutable();
      }

      @Override
      public BlockPos relative(final Direction.Axis axis, final int steps) {
         return super.relative(axis, steps).immutable();
      }

      @Override
      public BlockPos rotate(final Rotation rotation) {
         return super.rotate(rotation).immutable();
      }

      public BlockPos.MutableBlockPos set(final int x, final int y, final int z) {
         this.setX(x);
         this.setY(y);
         this.setZ(z);
         return this;
      }

      public BlockPos.MutableBlockPos set(final double x, final double y, final double z) {
         return this.set(Mth.floor(x), Mth.floor(y), Mth.floor(z));
      }

      public BlockPos.MutableBlockPos set(final Vec3i vec) {
         return this.set(vec.getX(), vec.getY(), vec.getZ());
      }

      public BlockPos.MutableBlockPos set(final long pos) {
         return this.set(getX(pos), getY(pos), getZ(pos));
      }

      public BlockPos.MutableBlockPos set(final AxisCycle transform, final int x, final int y, final int z) {
         return this.set(transform.cycle(x, y, z, Direction.Axis.X), transform.cycle(x, y, z, Direction.Axis.Y), transform.cycle(x, y, z, Direction.Axis.Z));
      }

      public BlockPos.MutableBlockPos setWithOffset(final Vec3i pos, final Direction direction) {
         return this.set(pos.getX() + direction.getStepX(), pos.getY() + direction.getStepY(), pos.getZ() + direction.getStepZ());
      }

      public BlockPos.MutableBlockPos setWithOffset(final Vec3i pos, final int x, final int y, final int z) {
         return this.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
      }

      public BlockPos.MutableBlockPos setWithOffset(final Vec3i pos, final Vec3i offset) {
         return this.set(pos.getX() + offset.getX(), pos.getY() + offset.getY(), pos.getZ() + offset.getZ());
      }

      public BlockPos.MutableBlockPos move(final Direction direction) {
         return this.move(direction, 1);
      }

      public BlockPos.MutableBlockPos move(final Direction direction, final int steps) {
         return this.set(this.getX() + direction.getStepX() * steps, this.getY() + direction.getStepY() * steps, this.getZ() + direction.getStepZ() * steps);
      }

      public BlockPos.MutableBlockPos move(final int x, final int y, final int z) {
         return this.set(this.getX() + x, this.getY() + y, this.getZ() + z);
      }

      public BlockPos.MutableBlockPos move(final Vec3i pos) {
         return this.set(this.getX() + pos.getX(), this.getY() + pos.getY(), this.getZ() + pos.getZ());
      }

      public BlockPos.MutableBlockPos clamp(final Direction.Axis axis, final int minimum, final int maximum) {
         return switch (axis) {
            case X -> this.set(Mth.clamp(this.getX(), minimum, maximum), this.getY(), this.getZ());
            case Y -> this.set(this.getX(), Mth.clamp(this.getY(), minimum, maximum), this.getZ());
            case Z -> this.set(this.getX(), this.getY(), Mth.clamp(this.getZ(), minimum, maximum));
         };
      }

      public BlockPos.MutableBlockPos setX(final int x) {
         super.setX(x);
         return this;
      }

      public BlockPos.MutableBlockPos setY(final int y) {
         super.setY(y);
         return this;
      }

      public BlockPos.MutableBlockPos setZ(final int z) {
         super.setZ(z);
         return this;
      }

      @Override
      public BlockPos immutable() {
         return new BlockPos(this.getX(), this.getY(), this.getZ());
      }
   }

   public enum TraversalNodeStatus {
      ACCEPT,
      SKIP,
      STOP;
   }
}
