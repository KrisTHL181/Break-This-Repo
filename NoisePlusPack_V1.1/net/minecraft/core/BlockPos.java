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
import org.apache.commons.lang3.tuple.Pair;

@Immutable
public class BlockPos extends Vec3i {
   public static final Codec<BlockPos> CODEC = Codec.INT_STREAM
      .comapFlatMap(
         p_448556_ -> Util.fixedSize(p_448556_, 3).map(p_175270_ -> new BlockPos(p_175270_[0], p_175270_[1], p_175270_[2])),
         p_121924_ -> IntStream.of(p_121924_.getX(), p_121924_.getY(), p_121924_.getZ())
      )
      .stable();
   public static final StreamCodec<ByteBuf, BlockPos> STREAM_CODEC = new StreamCodec<ByteBuf, BlockPos>() {
      public BlockPos decode(ByteBuf p_335731_) {
         return FriendlyByteBuf.readBlockPos(p_335731_);
      }

      public void encode(ByteBuf p_329093_, BlockPos p_330029_) {
         FriendlyByteBuf.writeBlockPos(p_329093_, p_330029_);
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

   public BlockPos(int p_121869_, int p_121870_, int p_121871_) {
      super(p_121869_, p_121870_, p_121871_);
   }

   public BlockPos(Vec3i p_121877_) {
      this(p_121877_.getX(), p_121877_.getY(), p_121877_.getZ());
   }

   public static long offset(long p_121916_, Direction p_121917_) {
      return offset(p_121916_, p_121917_.getStepX(), p_121917_.getStepY(), p_121917_.getStepZ());
   }

   public static long offset(long p_121911_, int p_121912_, int p_121913_, int p_121914_) {
      return asLong(getX(p_121911_) + p_121912_, getY(p_121911_) + p_121913_, getZ(p_121911_) + p_121914_);
   }

   public static int getX(long p_121984_) {
      return (int)(p_121984_ << 64 - X_OFFSET - PACKED_HORIZONTAL_LENGTH >> 64 - PACKED_HORIZONTAL_LENGTH);
   }

   public static int getY(long p_122009_) {
      return (int)(p_122009_ << 64 - PACKED_Y_LENGTH >> 64 - PACKED_Y_LENGTH);
   }

   public static int getZ(long p_122016_) {
      return (int)(p_122016_ << 64 - Z_OFFSET - PACKED_HORIZONTAL_LENGTH >> 64 - PACKED_HORIZONTAL_LENGTH);
   }

   public static BlockPos of(long p_122023_) {
      return new BlockPos(getX(p_122023_), getY(p_122023_), getZ(p_122023_));
   }

   public static BlockPos containing(double p_275310_, double p_275414_, double p_275737_) {
      return new BlockPos(Mth.floor(p_275310_), Mth.floor(p_275414_), Mth.floor(p_275737_));
   }

   public static BlockPos containing(Position p_275443_) {
      return containing(p_275443_.x(), p_275443_.y(), p_275443_.z());
   }

   public static BlockPos min(BlockPos p_328564_, BlockPos p_328313_) {
      return new BlockPos(
         Math.min(p_328564_.getX(), p_328313_.getX()), Math.min(p_328564_.getY(), p_328313_.getY()), Math.min(p_328564_.getZ(), p_328313_.getZ())
      );
   }

   public static BlockPos max(BlockPos p_330903_, BlockPos p_332595_) {
      return new BlockPos(
         Math.max(p_330903_.getX(), p_332595_.getX()), Math.max(p_330903_.getY(), p_332595_.getY()), Math.max(p_330903_.getZ(), p_332595_.getZ())
      );
   }

   public long asLong() {
      return asLong(this.getX(), this.getY(), this.getZ());
   }

   public static long asLong(int p_121883_, int p_121884_, int p_121885_) {
      long i = 0L;
      i |= (p_121883_ & PACKED_X_MASK) << X_OFFSET;
      i |= (p_121884_ & PACKED_Y_MASK) << 0;
      return i | (p_121885_ & PACKED_Z_MASK) << Z_OFFSET;
   }

   public static long getFlatIndex(long p_122028_) {
      return p_122028_ & -16L;
   }

   public BlockPos offset(int p_121973_, int p_121974_, int p_121975_) {
      return p_121973_ == 0 && p_121974_ == 0 && p_121975_ == 0
         ? this
         : new BlockPos(this.getX() + p_121973_, this.getY() + p_121974_, this.getZ() + p_121975_);
   }

   public Vec3 getCenter() {
      return Vec3.atCenterOf(this);
   }

   public Vec3 getBottomCenter() {
      return Vec3.atBottomCenterOf(this);
   }

   public BlockPos offset(Vec3i p_121956_) {
      return this.offset(p_121956_.getX(), p_121956_.getY(), p_121956_.getZ());
   }

   public BlockPos subtract(Vec3i p_121997_) {
      return this.offset(-p_121997_.getX(), -p_121997_.getY(), -p_121997_.getZ());
   }

   public BlockPos multiply(int p_175263_) {
      if (p_175263_ == 1) {
         return this;
      } else {
         return p_175263_ == 0 ? ZERO : new BlockPos(this.getX() * p_175263_, this.getY() * p_175263_, this.getZ() * p_175263_);
      }
   }

   public BlockPos above() {
      return this.relative(Direction.UP);
   }

   public BlockPos above(int p_121972_) {
      return this.relative(Direction.UP, p_121972_);
   }

   public BlockPos below() {
      return this.relative(Direction.DOWN);
   }

   public BlockPos below(int p_122000_) {
      return this.relative(Direction.DOWN, p_122000_);
   }

   public BlockPos north() {
      return this.relative(Direction.NORTH);
   }

   public BlockPos north(int p_122014_) {
      return this.relative(Direction.NORTH, p_122014_);
   }

   public BlockPos south() {
      return this.relative(Direction.SOUTH);
   }

   public BlockPos south(int p_122021_) {
      return this.relative(Direction.SOUTH, p_122021_);
   }

   public BlockPos west() {
      return this.relative(Direction.WEST);
   }

   public BlockPos west(int p_122026_) {
      return this.relative(Direction.WEST, p_122026_);
   }

   public BlockPos east() {
      return this.relative(Direction.EAST);
   }

   public BlockPos east(int p_122031_) {
      return this.relative(Direction.EAST, p_122031_);
   }

   public BlockPos relative(Direction p_121946_) {
      return new BlockPos(this.getX() + p_121946_.getStepX(), this.getY() + p_121946_.getStepY(), this.getZ() + p_121946_.getStepZ());
   }

   public BlockPos relative(Direction p_121948_, int p_121949_) {
      return p_121949_ == 0
         ? this
         : new BlockPos(
            this.getX() + p_121948_.getStepX() * p_121949_, this.getY() + p_121948_.getStepY() * p_121949_, this.getZ() + p_121948_.getStepZ() * p_121949_
         );
   }

   public BlockPos relative(Direction.Axis p_121943_, int p_121944_) {
      if (p_121944_ == 0) {
         return this;
      }

      int i = p_121943_ == Direction.Axis.X ? p_121944_ : 0;
      int j = p_121943_ == Direction.Axis.Y ? p_121944_ : 0;
      int k = p_121943_ == Direction.Axis.Z ? p_121944_ : 0;
      return new BlockPos(this.getX() + i, this.getY() + j, this.getZ() + k);
   }

   public BlockPos rotate(Rotation p_121918_) {
      return switch (p_121918_) {
         case CLOCKWISE_90 -> new BlockPos(-this.getZ(), this.getY(), this.getX());
         case CLOCKWISE_180 -> new BlockPos(-this.getX(), this.getY(), -this.getZ());
         case COUNTERCLOCKWISE_90 -> new BlockPos(this.getZ(), this.getY(), -this.getX());
         case NONE -> this;
      };
   }

   public BlockPos cross(Vec3i p_122011_) {
      return new BlockPos(
         this.getY() * p_122011_.getZ() - this.getZ() * p_122011_.getY(),
         this.getZ() * p_122011_.getX() - this.getX() * p_122011_.getZ(),
         this.getX() * p_122011_.getY() - this.getY() * p_122011_.getX()
      );
   }

   public BlockPos atY(int p_175289_) {
      return new BlockPos(this.getX(), p_175289_, this.getZ());
   }

   public BlockPos immutable() {
      return this;
   }

   public BlockPos.MutableBlockPos mutable() {
      return new BlockPos.MutableBlockPos(this.getX(), this.getY(), this.getZ());
   }

   public Vec3 clampLocationWithin(Vec3 p_342830_) {
      return new Vec3(
         Mth.clamp(p_342830_.x, this.getX() + 1.0E-5F, this.getX() + 1.0 - 1.0E-5F),
         Mth.clamp(p_342830_.y, this.getY() + 1.0E-5F, this.getY() + 1.0 - 1.0E-5F),
         Mth.clamp(p_342830_.z, this.getZ() + 1.0E-5F, this.getZ() + 1.0 - 1.0E-5F)
      );
   }

   public static Iterable<BlockPos> randomInCube(RandomSource p_235651_, int p_235652_, BlockPos p_235653_, int p_235654_) {
      return randomBetweenClosed(
         p_235651_,
         p_235652_,
         p_235653_.getX() - p_235654_,
         p_235653_.getY() - p_235654_,
         p_235653_.getZ() - p_235654_,
         p_235653_.getX() + p_235654_,
         p_235653_.getY() + p_235654_,
         p_235653_.getZ() + p_235654_
      );
   }

   @Deprecated
   public static Stream<BlockPos> squareOutSouthEast(BlockPos p_284978_) {
      return Stream.of(p_284978_, p_284978_.south(), p_284978_.east(), p_284978_.south().east());
   }

   public static Iterable<BlockPos> randomBetweenClosed(
      RandomSource p_235642_, int p_235643_, int p_235644_, int p_235645_, int p_235646_, int p_235647_, int p_235648_, int p_235649_
   ) {
      int i = p_235647_ - p_235644_ + 1;
      int j = p_235648_ - p_235645_ + 1;
      int k = p_235649_ - p_235646_ + 1;
      return () -> new AbstractIterator<BlockPos>() {
         final BlockPos.MutableBlockPos nextPos = new BlockPos.MutableBlockPos();
         int counter = p_235643_;

         protected BlockPos computeNext() {
            if (this.counter <= 0) {
               return (BlockPos)this.endOfData();
            }

            BlockPos blockpos = this.nextPos.set(p_235644_ + p_235642_.nextInt(i), p_235645_ + p_235642_.nextInt(j), p_235646_ + p_235642_.nextInt(k));
            this.counter--;
            return blockpos;
         }
      };
   }

   public static Iterable<BlockPos> withinManhattan(BlockPos p_121926_, int p_121927_, int p_121928_, int p_121929_) {
      int i = p_121927_ + p_121928_ + p_121929_;
      int j = p_121926_.getX();
      int k = p_121926_.getY();
      int l = p_121926_.getZ();
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
               this.cursor.setZ(l - (this.cursor.getZ() - l));
               return this.cursor;
            }

            BlockPos blockpos;
            for (blockpos = null; blockpos == null; this.y++) {
               if (this.y > this.maxY) {
                  this.x++;
                  if (this.x > this.maxX) {
                     this.currentDepth++;
                     if (this.currentDepth > i) {
                        return (BlockPos)this.endOfData();
                     }

                     this.maxX = Math.min(p_121927_, this.currentDepth);
                     this.x = -this.maxX;
                  }

                  this.maxY = Math.min(p_121928_, this.currentDepth - Math.abs(this.x));
                  this.y = -this.maxY;
               }

               int i1 = this.x;
               int j1 = this.y;
               int k1 = this.currentDepth - Math.abs(i1) - Math.abs(j1);
               if (k1 <= p_121929_) {
                  this.zMirror = k1 != 0;
                  blockpos = this.cursor.set(j + i1, k + j1, l + k1);
               }
            }

            return blockpos;
         }
      };
   }

   public static Optional<BlockPos> findClosestMatch(BlockPos p_121931_, int p_121932_, int p_121933_, Predicate<BlockPos> p_121934_) {
      for (BlockPos blockpos : withinManhattan(p_121931_, p_121932_, p_121933_, p_121932_)) {
         if (p_121934_.test(blockpos)) {
            return Optional.of(blockpos);
         }
      }

      return Optional.empty();
   }

   public static Stream<BlockPos> withinManhattanStream(BlockPos p_121986_, int p_121987_, int p_121988_, int p_121989_) {
      return StreamSupport.stream(withinManhattan(p_121986_, p_121987_, p_121988_, p_121989_).spliterator(), false);
   }

   public static Iterable<BlockPos> betweenClosed(AABB p_368631_) {
      BlockPos blockpos = containing(p_368631_.minX, p_368631_.minY, p_368631_.minZ);
      BlockPos blockpos1 = containing(p_368631_.maxX, p_368631_.maxY, p_368631_.maxZ);
      return betweenClosed(blockpos, blockpos1);
   }

   public static Iterable<BlockPos> betweenClosed(BlockPos p_121941_, BlockPos p_121942_) {
      return betweenClosed(
         Math.min(p_121941_.getX(), p_121942_.getX()),
         Math.min(p_121941_.getY(), p_121942_.getY()),
         Math.min(p_121941_.getZ(), p_121942_.getZ()),
         Math.max(p_121941_.getX(), p_121942_.getX()),
         Math.max(p_121941_.getY(), p_121942_.getY()),
         Math.max(p_121941_.getZ(), p_121942_.getZ())
      );
   }

   public static Stream<BlockPos> betweenClosedStream(BlockPos p_121991_, BlockPos p_121992_) {
      return StreamSupport.stream(betweenClosed(p_121991_, p_121992_).spliterator(), false);
   }

   public static Stream<BlockPos> betweenClosedStream(BoundingBox p_121920_) {
      return betweenClosedStream(
         Math.min(p_121920_.minX(), p_121920_.maxX()),
         Math.min(p_121920_.minY(), p_121920_.maxY()),
         Math.min(p_121920_.minZ(), p_121920_.maxZ()),
         Math.max(p_121920_.minX(), p_121920_.maxX()),
         Math.max(p_121920_.minY(), p_121920_.maxY()),
         Math.max(p_121920_.minZ(), p_121920_.maxZ())
      );
   }

   public static Stream<BlockPos> betweenClosedStream(AABB p_121922_) {
      return betweenClosedStream(
         Mth.floor(p_121922_.minX),
         Mth.floor(p_121922_.minY),
         Mth.floor(p_121922_.minZ),
         Mth.floor(p_121922_.maxX),
         Mth.floor(p_121922_.maxY),
         Mth.floor(p_121922_.maxZ)
      );
   }

   public static Stream<BlockPos> betweenClosedStream(int p_121887_, int p_121888_, int p_121889_, int p_121890_, int p_121891_, int p_121892_) {
      return StreamSupport.stream(betweenClosed(p_121887_, p_121888_, p_121889_, p_121890_, p_121891_, p_121892_).spliterator(), false);
   }

   public static Iterable<BlockPos> betweenClosed(int p_121977_, int p_121978_, int p_121979_, int p_121980_, int p_121981_, int p_121982_) {
      int i = p_121980_ - p_121977_ + 1;
      int j = p_121981_ - p_121978_ + 1;
      int k = p_121982_ - p_121979_ + 1;
      int l = i * j * k;
      return () -> new AbstractIterator<BlockPos>() {
         private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
         private int index;

         protected BlockPos computeNext() {
            if (this.index == l) {
               return (BlockPos)this.endOfData();
            }

            int i1 = this.index % i;
            int j1 = this.index / i;
            int k1 = j1 % j;
            int l1 = j1 / j;
            this.index++;
            return this.cursor.set(p_121977_ + i1, p_121978_ + k1, p_121979_ + l1);
         }
      };
   }

   public static Iterable<BlockPos.MutableBlockPos> spiralAround(BlockPos p_121936_, int p_121937_, Direction p_121938_, Direction p_121939_) {
      Validate.validState(p_121938_.getAxis() != p_121939_.getAxis(), "The two directions cannot be on the same axis", new Object[0]);
      return () -> new AbstractIterator<BlockPos.MutableBlockPos>() {
         private final Direction[] directions = new Direction[]{p_121938_, p_121939_, p_121938_.getOpposite(), p_121939_.getOpposite()};
         private final BlockPos.MutableBlockPos cursor = p_121936_.mutable().move(p_121939_);
         private final int legs = 4 * p_121937_;
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
      BlockPos p_278078_,
      int p_277385_,
      int p_277666_,
      BiConsumer<BlockPos, Consumer<BlockPos>> p_277755_,
      Function<BlockPos, BlockPos.TraversalNodeStatus> p_375629_
   ) {
      Queue<Pair<BlockPos, Integer>> queue = new ArrayDeque<>();
      LongSet longset = new LongOpenHashSet();
      queue.add(Pair.of(p_278078_, 0));
      int i = 0;

      while (!queue.isEmpty()) {
         Pair<BlockPos, Integer> pair = queue.poll();
         BlockPos blockpos = (BlockPos)pair.getLeft();
         int j = (Integer)pair.getRight();
         long k = blockpos.asLong();
         if (longset.add(k)) {
            BlockPos.TraversalNodeStatus blockpos$traversalnodestatus = p_375629_.apply(blockpos);
            if (blockpos$traversalnodestatus != BlockPos.TraversalNodeStatus.SKIP) {
               if (blockpos$traversalnodestatus == BlockPos.TraversalNodeStatus.STOP) {
                  break;
               }

               if (++i >= p_277666_) {
                  return i;
               }

               if (j < p_277385_) {
                  p_277755_.accept(blockpos, p_277234_ -> queue.add(Pair.of(p_277234_, j + 1)));
               }
            }
         }
      }

      return i;
   }

   public static Iterable<BlockPos> betweenCornersInDirection(AABB p_423644_, Vec3 p_424214_) {
      Vec3 vec3 = p_423644_.getMinPosition();
      int i = Mth.floor(vec3.x());
      int j = Mth.floor(vec3.y());
      int k = Mth.floor(vec3.z());
      Vec3 vec31 = p_423644_.getMaxPosition();
      int l = Mth.floor(vec31.x());
      int i1 = Mth.floor(vec31.y());
      int j1 = Mth.floor(vec31.z());
      return betweenCornersInDirection(i, j, k, l, i1, j1, p_424214_);
   }

   public static Iterable<BlockPos> betweenCornersInDirection(BlockPos p_429861_, BlockPos p_425231_, Vec3 p_430346_) {
      return betweenCornersInDirection(p_429861_.getX(), p_429861_.getY(), p_429861_.getZ(), p_425231_.getX(), p_425231_.getY(), p_425231_.getZ(), p_430346_);
   }

   public static Iterable<BlockPos> betweenCornersInDirection(
      int p_422704_, int p_423192_, int p_429254_, int p_425751_, int p_422284_, int p_425917_, Vec3 p_428040_
   ) {
      int i = Math.min(p_422704_, p_425751_);
      int j = Math.min(p_423192_, p_422284_);
      int k = Math.min(p_429254_, p_425917_);
      int l = Math.max(p_422704_, p_425751_);
      int i1 = Math.max(p_423192_, p_422284_);
      int j1 = Math.max(p_429254_, p_425917_);
      int k1 = l - i;
      int l1 = i1 - j;
      int i2 = j1 - k;
      int j2 = p_428040_.x >= 0.0 ? i : l;
      int k2 = p_428040_.y >= 0.0 ? j : i1;
      int l2 = p_428040_.z >= 0.0 ? k : j1;
      List<Direction.Axis> list = Direction.axisStepOrder(p_428040_);
      Direction.Axis direction$axis = list.get(0);
      Direction.Axis direction$axis1 = list.get(1);
      Direction.Axis direction$axis2 = list.get(2);
      Direction direction = p_428040_.get(direction$axis) >= 0.0 ? direction$axis.getPositive() : direction$axis.getNegative();
      Direction direction1 = p_428040_.get(direction$axis1) >= 0.0 ? direction$axis1.getPositive() : direction$axis1.getNegative();
      Direction direction2 = p_428040_.get(direction$axis2) >= 0.0 ? direction$axis2.getPositive() : direction$axis2.getNegative();
      int i3 = direction$axis.choose(k1, l1, i2);
      int j3 = direction$axis1.choose(k1, l1, i2);
      int k3 = direction$axis2.choose(k1, l1, i2);
      return () -> new AbstractIterator<BlockPos>() {
         private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
         private int firstIndex;
         private int secondIndex;
         private int thirdIndex;
         private boolean end;
         private final int firstDirX = direction.getStepX();
         private final int firstDirY = direction.getStepY();
         private final int firstDirZ = direction.getStepZ();
         private final int secondDirX = direction1.getStepX();
         private final int secondDirY = direction1.getStepY();
         private final int secondDirZ = direction1.getStepZ();
         private final int thirdDirX = direction2.getStepX();
         private final int thirdDirY = direction2.getStepY();
         private final int thirdDirZ = direction2.getStepZ();

         protected BlockPos computeNext() {
            if (this.end) {
               return (BlockPos)this.endOfData();
            }

            this.cursor
               .set(
                  j2 + this.firstDirX * this.firstIndex + this.secondDirX * this.secondIndex + this.thirdDirX * this.thirdIndex,
                  k2 + this.firstDirY * this.firstIndex + this.secondDirY * this.secondIndex + this.thirdDirY * this.thirdIndex,
                  l2 + this.firstDirZ * this.firstIndex + this.secondDirZ * this.secondIndex + this.thirdDirZ * this.thirdIndex
               );
            if (this.thirdIndex < k3) {
               this.thirdIndex++;
            } else if (this.secondIndex < j3) {
               this.secondIndex++;
               this.thirdIndex = 0;
            } else if (this.firstIndex < i3) {
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

      public MutableBlockPos(int p_122130_, int p_122131_, int p_122132_) {
         super(p_122130_, p_122131_, p_122132_);
      }

      public MutableBlockPos(double p_122126_, double p_122127_, double p_122128_) {
         this(Mth.floor(p_122126_), Mth.floor(p_122127_), Mth.floor(p_122128_));
      }

      @Override
      public BlockPos offset(int p_122163_, int p_122164_, int p_122165_) {
         return super.offset(p_122163_, p_122164_, p_122165_).immutable();
      }

      @Override
      public BlockPos multiply(int p_175305_) {
         return super.multiply(p_175305_).immutable();
      }

      @Override
      public BlockPos relative(Direction p_122152_, int p_122153_) {
         return super.relative(p_122152_, p_122153_).immutable();
      }

      @Override
      public BlockPos relative(Direction.Axis p_122145_, int p_122146_) {
         return super.relative(p_122145_, p_122146_).immutable();
      }

      @Override
      public BlockPos rotate(Rotation p_122138_) {
         return super.rotate(p_122138_).immutable();
      }

      public BlockPos.MutableBlockPos set(int p_122179_, int p_122180_, int p_122181_) {
         this.setX(p_122179_);
         this.setY(p_122180_);
         this.setZ(p_122181_);
         return this;
      }

      public BlockPos.MutableBlockPos set(double p_122170_, double p_122171_, double p_122172_) {
         return this.set(Mth.floor(p_122170_), Mth.floor(p_122171_), Mth.floor(p_122172_));
      }

      public BlockPos.MutableBlockPos set(Vec3i p_122191_) {
         return this.set(p_122191_.getX(), p_122191_.getY(), p_122191_.getZ());
      }

      public BlockPos.MutableBlockPos set(long p_122189_) {
         return this.set(getX(p_122189_), getY(p_122189_), getZ(p_122189_));
      }

      public BlockPos.MutableBlockPos set(AxisCycle p_122140_, int p_122141_, int p_122142_, int p_122143_) {
         return this.set(
            p_122140_.cycle(p_122141_, p_122142_, p_122143_, Direction.Axis.X),
            p_122140_.cycle(p_122141_, p_122142_, p_122143_, Direction.Axis.Y),
            p_122140_.cycle(p_122141_, p_122142_, p_122143_, Direction.Axis.Z)
         );
      }

      public BlockPos.MutableBlockPos setWithOffset(Vec3i p_122160_, Direction p_122161_) {
         return this.set(p_122160_.getX() + p_122161_.getStepX(), p_122160_.getY() + p_122161_.getStepY(), p_122160_.getZ() + p_122161_.getStepZ());
      }

      public BlockPos.MutableBlockPos setWithOffset(Vec3i p_122155_, int p_122156_, int p_122157_, int p_122158_) {
         return this.set(p_122155_.getX() + p_122156_, p_122155_.getY() + p_122157_, p_122155_.getZ() + p_122158_);
      }

      public BlockPos.MutableBlockPos setWithOffset(Vec3i p_175307_, Vec3i p_175308_) {
         return this.set(p_175307_.getX() + p_175308_.getX(), p_175307_.getY() + p_175308_.getY(), p_175307_.getZ() + p_175308_.getZ());
      }

      public BlockPos.MutableBlockPos move(Direction p_122174_) {
         return this.move(p_122174_, 1);
      }

      public BlockPos.MutableBlockPos move(Direction p_122176_, int p_122177_) {
         return this.set(
            this.getX() + p_122176_.getStepX() * p_122177_, this.getY() + p_122176_.getStepY() * p_122177_, this.getZ() + p_122176_.getStepZ() * p_122177_
         );
      }

      public BlockPos.MutableBlockPos move(int p_122185_, int p_122186_, int p_122187_) {
         return this.set(this.getX() + p_122185_, this.getY() + p_122186_, this.getZ() + p_122187_);
      }

      public BlockPos.MutableBlockPos move(Vec3i p_122194_) {
         return this.set(this.getX() + p_122194_.getX(), this.getY() + p_122194_.getY(), this.getZ() + p_122194_.getZ());
      }

      public BlockPos.MutableBlockPos clamp(Direction.Axis p_122148_, int p_122149_, int p_122150_) {
         return switch (p_122148_) {
            case X -> this.set(Mth.clamp(this.getX(), p_122149_, p_122150_), this.getY(), this.getZ());
            case Y -> this.set(this.getX(), Mth.clamp(this.getY(), p_122149_, p_122150_), this.getZ());
            case Z -> this.set(this.getX(), this.getY(), Mth.clamp(this.getZ(), p_122149_, p_122150_));
         };
      }

      public BlockPos.MutableBlockPos setX(int p_175341_) {
         super.setX(p_175341_);
         return this;
      }

      public BlockPos.MutableBlockPos setY(int p_175343_) {
         super.setY(p_175343_);
         return this;
      }

      public BlockPos.MutableBlockPos setZ(int p_175345_) {
         super.setZ(p_175345_);
         return this;
      }

      @Override
      public BlockPos immutable() {
         return new BlockPos(this);
      }
   }

   public enum TraversalNodeStatus {
      ACCEPT,
      SKIP,
      STOP;
   }
}
