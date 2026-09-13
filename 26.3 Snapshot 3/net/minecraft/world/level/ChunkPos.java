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
import org.jspecify.annotations.Nullable;

public record ChunkPos(int x, int z) {
   public static final Codec<ChunkPos> CODEC = Codec.INT_STREAM
      .comapFlatMap(input -> Util.fixedSize(input, 2).map(ints -> new ChunkPos(ints[0], ints[1])), pos -> IntStream.of(pos.x, pos.z))
      .stable();
   public static final StreamCodec<ByteBuf, ChunkPos> STREAM_CODEC = new StreamCodec<ByteBuf, ChunkPos>() {
      public ChunkPos decode(final ByteBuf input) {
         return FriendlyByteBuf.readChunkPos(input);
      }

      public void encode(final ByteBuf output, final ChunkPos value) {
         FriendlyByteBuf.writeChunkPos(output, value);
      }
   };
   private static final int SAFETY_MARGIN = 1056;
   public static final long INVALID_CHUNK_POS = pack(1875066, 1875066);
   public static final ChunkPos ZERO = new ChunkPos(0, 0);
   private static final long COORD_BITS = 32L;
   private static final long COORD_MASK = 4294967295L;
   private static final int REGION_BITS = 5;
   public static final int REGION_SIZE = 32;
   private static final int REGION_MASK = 31;
   public static final int REGION_MAX_INDEX = 31;
   private static final int HASH_A = 1664525;
   private static final int HASH_C = 1013904223;
   private static final int HASH_Z_XOR = -559038737;

   public static ChunkPos containing(final BlockPos pos) {
      return new ChunkPos(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
   }

   public static ChunkPos unpack(final long key) {
      return new ChunkPos((int)key, (int)(key >> 32));
   }

   public static ChunkPos minFromRegion(final int regionX, final int regionZ) {
      return new ChunkPos(regionX << 5, regionZ << 5);
   }

   public static ChunkPos maxFromRegion(final int regionX, final int regionZ) {
      return new ChunkPos((regionX << 5) + 31, (regionZ << 5) + 31);
   }

   public boolean isValid() {
      return isValid(this.x, this.z);
   }

   public static boolean isValid(final int x, final int z) {
      return Mth.absMax(x, z) <= ChunkPyramid.MAX_CHUNK_COORDINATE_VALUE;
   }

   public long pack() {
      return pack(this.x, this.z);
   }

   public static long pack(final int x, final int z) {
      return x & 4294967295L | (z & 4294967295L) << 32;
   }

   public static long fromSectionNode(final long sectionNode) {
      return pack(SectionPos.x(sectionNode), SectionPos.z(sectionNode));
   }

   public static long pack(final BlockPos pos) {
      return pack(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
   }

   public static int getX(final long pos) {
      return (int)(pos & 4294967295L);
   }

   public static int getZ(final long pos) {
      return (int)(pos >>> 32 & 4294967295L);
   }

   @Override
   public int hashCode() {
      return hash(this.x, this.z);
   }

   public static int hash(final int x, final int z) {
      int xTransform = 1664525 * x + 1013904223;
      int zTransform = 1664525 * (z ^ -559038737) + 1013904223;
      return xTransform ^ zTransform;
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

   public static int getRegionX(final long pos) {
      return getX(pos) >> 5;
   }

   public static int getRegionZ(final long pos) {
      return getZ(pos) >> 5;
   }

   public int getRegionLocalX() {
      return this.x & 31;
   }

   public int getRegionLocalZ() {
      return this.z & 31;
   }

   public BlockPos getBlockAt(final int x, final int y, final int z) {
      return new BlockPos(this.getBlockX(x), y, this.getBlockZ(z));
   }

   public int getBlockX(final int offset) {
      return SectionPos.sectionToBlockCoord(this.x, offset);
   }

   public int getBlockZ(final int offset) {
      return SectionPos.sectionToBlockCoord(this.z, offset);
   }

   public BlockPos getMiddleBlockPosition(final int y) {
      return new BlockPos(this.getMiddleBlockX(), y, this.getMiddleBlockZ());
   }

   public boolean contains(final BlockPos pos) {
      return pos.getX() >= this.getMinBlockX() && pos.getZ() >= this.getMinBlockZ() && pos.getX() <= this.getMaxBlockX() && pos.getZ() <= this.getMaxBlockZ();
   }

   @Override
   public String toString() {
      return "[" + this.x + ", " + this.z + "]";
   }

   public BlockPos getWorldPosition() {
      return new BlockPos(this.getMinBlockX(), 0, this.getMinBlockZ());
   }

   public int getChessboardDistance(final ChunkPos pos) {
      return this.getChessboardDistance(pos.x, pos.z);
   }

   public int getChessboardDistance(final int x, final int z) {
      return Mth.chessboardDistance(x, z, this.x, this.z);
   }

   public int distanceSquared(final ChunkPos pos) {
      return this.distanceSquared(pos.x, pos.z);
   }

   public int distanceSquared(final long pos) {
      return this.distanceSquared(getX(pos), getZ(pos));
   }

   private int distanceSquared(final int x, final int z) {
      int deltaX = x - this.x;
      int deltaZ = z - this.z;
      return deltaX * deltaX + deltaZ * deltaZ;
   }

   public static Stream<ChunkPos> rangeClosed(final ChunkPos center, final int radius) {
      return rangeClosed(new ChunkPos(center.x - radius, center.z - radius), new ChunkPos(center.x + radius, center.z + radius));
   }

   public static Stream<ChunkPos> rangeClosed(final ChunkPos from, final ChunkPos to) {
      int xSize = Math.abs(from.x - to.x) + 1;
      int zSize = Math.abs(from.z - to.z) + 1;
      final int xDiff = from.x < to.x ? 1 : -1;
      final int zDiff = from.z < to.z ? 1 : -1;
      return StreamSupport.stream(new AbstractSpliterator<ChunkPos>(xSize * zSize, 64) {
         private @Nullable ChunkPos pos;

         @Override
         public boolean tryAdvance(final Consumer<? super ChunkPos> action) {
            if (this.pos == null) {
               this.pos = from;
            } else {
               int x = this.pos.x;
               int z = this.pos.z;
               if (x == to.x) {
                  if (z == to.z) {
                     return false;
                  }

                  this.pos = new ChunkPos(from.x, z + zDiff);
               } else {
                  this.pos = new ChunkPos(x + xDiff, z);
               }
            }

            action.accept(this.pos);
            return true;
         }
      }, false);
   }
}
