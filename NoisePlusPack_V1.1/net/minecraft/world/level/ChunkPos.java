package net.minecraft.world.level;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jspecify.annotations.Nullable;

public class ChunkPos {
   public static final Codec<ChunkPos> CODEC = Codec.INT_STREAM
      .comapFlatMap(
         p_449872_ -> Util.fixedSize(p_449872_, 2).map(p_364739_ -> new ChunkPos(p_364739_[0], p_364739_[1])),
         p_361145_ -> IntStream.of(p_361145_.x, p_361145_.z)
      )
      .stable();
   public static final StreamCodec<ByteBuf, ChunkPos> STREAM_CODEC = new StreamCodec<ByteBuf, ChunkPos>() {
      public ChunkPos decode(ByteBuf p_366719_) {
         return FriendlyByteBuf.readChunkPos(p_366719_);
      }

      public void encode(ByteBuf p_365664_, ChunkPos p_370100_) {
         FriendlyByteBuf.writeChunkPos(p_365664_, p_370100_);
      }
   };
   private static final int SAFETY_MARGIN = 1056;
   public static final long INVALID_CHUNK_POS = asLong(Integer.MAX_VALUE, Integer.MAX_VALUE);
   private static final int SAFETY_MARGIN_CHUNKS = (32 + ChunkPyramid.GENERATION_PYRAMID.getStepTo(ChunkStatus.FULL).accumulatedDependencies().size() + 1) * 2;
   public static final int MAX_COORDINATE_VALUE = SectionPos.blockToSectionCoord(BlockPos.MAX_HORIZONTAL_COORDINATE) - SAFETY_MARGIN_CHUNKS;
   public static final ChunkPos ZERO = new ChunkPos(0, 0);
   private static final long COORD_BITS = 32L;
   private static final long COORD_MASK = 4294967295L;
   private static final int REGION_BITS = 5;
   public static final int REGION_SIZE = 32;
   private static final int REGION_MASK = 31;
   public static final int REGION_MAX_INDEX = 31;
   public final int x;
   public final int z;
   private static final int HASH_A = 1664525;
   private static final int HASH_C = 1013904223;
   private static final int HASH_Z_XOR = -559038737;

   public ChunkPos(int p_45582_, int p_45583_) {
      this.x = p_45582_;
      this.z = p_45583_;
   }

   public ChunkPos(BlockPos p_45587_) {
      this.x = SectionPos.blockToSectionCoord(p_45587_.getX());
      this.z = SectionPos.blockToSectionCoord(p_45587_.getZ());
   }

   public ChunkPos(long p_45585_) {
      this.x = (int)p_45585_;
      this.z = (int)(p_45585_ >> 32);
   }

   public static ChunkPos minFromRegion(int p_220338_, int p_220339_) {
      return new ChunkPos(p_220338_ << 5, p_220339_ << 5);
   }

   public static ChunkPos maxFromRegion(int p_220341_, int p_220342_) {
      return new ChunkPos((p_220341_ << 5) + 31, (p_220342_ << 5) + 31);
   }

   public boolean isValid() {
      return isValid(this.x, this.z);
   }

   public static boolean isValid(int p_461127_, int p_461118_) {
      return true;
   }

   public long toLong() {
      return asLong(this.x, this.z);
   }

   public static long asLong(int p_45590_, int p_45591_) {
      return p_45590_ & 4294967295L | (p_45591_ & 4294967295L) << 32;
   }

   public static long asLong(BlockPos p_151389_) {
      return asLong(SectionPos.blockToSectionCoord(p_151389_.getX()), SectionPos.blockToSectionCoord(p_151389_.getZ()));
   }

   public static int getX(long p_45593_) {
      return (int)(p_45593_ & 4294967295L);
   }

   public static int getZ(long p_45603_) {
      return (int)(p_45603_ >>> 32 & 4294967295L);
   }

   @Override
   public int hashCode() {
      return hash(this.x, this.z);
   }

   public static int hash(int p_220344_, int p_220345_) {
      int i = 1664525 * p_220344_ + 1013904223;
      int j = 1664525 * (p_220345_ ^ -559038737) + 1013904223;
      return i ^ j;
   }

   @Override
   public boolean equals(Object p_45607_) {
      if (this == p_45607_) {
         return true;
      } else {
         return p_45607_ instanceof ChunkPos chunkpos ? this.x == chunkpos.x && this.z == chunkpos.z : false;
      }
   }

   public int getMiddleBlockX() {
      return this.getBlockX(8);
   }

   public int getMiddleBlockZ() {
      return this.getBlockZ(8);
   }

   public int getMinBlockX() {
      return SectionPos.sectionToBlockCoord(this.x);
   }

   public int getMinBlockZ() {
      return SectionPos.sectionToBlockCoord(this.z);
   }

   public int getMaxBlockX() {
      return this.getBlockX(15);
   }

   public int getMaxBlockZ() {
      return this.getBlockZ(15);
   }

   public int getRegionX() {
      return this.x >> 5;
   }

   public int getRegionZ() {
      return this.z >> 5;
   }

   public int getRegionLocalX() {
      return this.x & 31;
   }

   public int getRegionLocalZ() {
      return this.z & 31;
   }

   public BlockPos getBlockAt(int p_151385_, int p_151386_, int p_151387_) {
      return new BlockPos(this.getBlockX(p_151385_), p_151386_, this.getBlockZ(p_151387_));
   }

   public int getBlockX(int p_151383_) {
      return SectionPos.sectionToBlockCoord(this.x, p_151383_);
   }

   public int getBlockZ(int p_151392_) {
      return SectionPos.sectionToBlockCoord(this.z, p_151392_);
   }

   public BlockPos getMiddleBlockPosition(int p_151395_) {
      return new BlockPos(this.getMiddleBlockX(), p_151395_, this.getMiddleBlockZ());
   }

   public boolean contains(BlockPos p_428269_) {
      return p_428269_.getX() >= this.getMinBlockX()
         && p_428269_.getZ() >= this.getMinBlockZ()
         && p_428269_.getX() <= this.getMaxBlockX()
         && p_428269_.getZ() <= this.getMaxBlockZ();
   }

   @Override
   public String toString() {
      return "[" + this.x + ", " + this.z + "]";
   }

   public BlockPos getWorldPosition() {
      return new BlockPos(this.getMinBlockX(), 0, this.getMinBlockZ());
   }

   public int getChessboardDistance(ChunkPos p_45595_) {
      return this.getChessboardDistance(p_45595_.x, p_45595_.z);
   }

   public int getChessboardDistance(int p_343403_, int p_344625_) {
      return Mth.chessboardDistance(p_343403_, p_344625_, this.x, this.z);
   }

   public int distanceSquared(ChunkPos p_297557_) {
      return this.distanceSquared(p_297557_.x, p_297557_.z);
   }

   public int distanceSquared(long p_300589_) {
      return this.distanceSquared(getX(p_300589_), getZ(p_300589_));
   }

   private int distanceSquared(int p_300851_, int p_301322_) {
      int i = p_300851_ - this.x;
      int j = p_301322_ - this.z;
      return i * i + j * j;
   }

   public static Stream<ChunkPos> rangeClosed(ChunkPos p_45597_, int p_45598_) {
      return rangeClosed(new ChunkPos(p_45597_.x - p_45598_, p_45597_.z - p_45598_), new ChunkPos(p_45597_.x + p_45598_, p_45597_.z + p_45598_));
   }

   public static Stream<ChunkPos> rangeClosed(final ChunkPos p_45600_, final ChunkPos p_45601_) {
      int i = Math.abs(p_45600_.x - p_45601_.x) + 1;
      int j = Math.abs(p_45600_.z - p_45601_.z) + 1;
      final int k = p_45600_.x < p_45601_.x ? 1 : -1;
      final int l = p_45600_.z < p_45601_.z ? 1 : -1;
      return StreamSupport.stream(new AbstractSpliterator<ChunkPos>(i * j, 64) {
         private @Nullable ChunkPos pos;

         @Override
         public boolean tryAdvance(Consumer<? super ChunkPos> p_45630_) {
            if (this.pos == null) {
               this.pos = p_45600_;
            } else {
               int i1 = this.pos.x;
               int j1 = this.pos.z;
               if (i1 == p_45601_.x) {
                  if (j1 == p_45601_.z) {
                     return false;
                  }

                  this.pos = new ChunkPos(p_45600_.x, j1 + l);
               } else {
                  this.pos = new ChunkPos(i1 + k, j1);
               }
            }

            p_45630_.accept(this.pos);
            return true;
         }
      }, false);
   }
}
