package net.minecraft.world.level;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Util;
import net.MinecraftTools.Math._256Bit.Int256;
import net.minecraft.client.gui.screens.worldselection.WorldMainSettingScreen;
import org.jspecify.annotations.Nullable;

/**
 * ChunkPos — 区块坐标（MCRe NoiseFarlands 对象化版）
 *
 * <p>原版用 pack() 将 (int x, int z) 打包进 long（32+32 位），坐标上限被锁死在 int。 本版：坐标升级为 long（突破 2^31
 * 区块限制）、移除打包系统、record 天然对象键 （equals/hashCode 按坐标相等），并适配 256-bit（Int256）。
 */
public record ChunkPos(long x, long z) {
    public static final Codec<ChunkPos> CODEC = Codec.LONG_STREAM
            .<ChunkPos>
                    comapFlatMap(input -> Util.fixedSize(input, 2).map(longs -> new ChunkPos(longs[
                    0], longs[1])), pos -> LongStream.of(pos.x, pos.z))
            .stable();
    public static final StreamCodec<ByteBuf, ChunkPos> STREAM_CODEC = new StreamCodec<
            ByteBuf, ChunkPos>() {
        public ChunkPos decode(final ByteBuf input) {
            return FriendlyByteBuf.readChunkPos(input);
        }

        public void encode(final ByteBuf output, final ChunkPos value) {
            FriendlyByteBuf.writeChunkPos(output, value);
        }
    };
    private static final int SAFETY_MARGIN = 1056;
    public static final ChunkPos ZERO = new ChunkPos(0L, 0L);

    /** 无效区块坐标哨兵（原版 INVALID_CHUNK_POS = pack(1875066, 1875066)，对象化后为常量值） */
    public static final ChunkPos INVALID_CHUNK_POS = new ChunkPos(1875066L, 1875066L);

    public static final int REGION_BITS = 5;
    public static final int REGION_SIZE = 32;
    private static final int REGION_MASK = 31;
    public static final int REGION_MAX_INDEX = 31;
    private static final int HASH_A = 1664525;
    private static final int HASH_C = 1013904223;
    private static final long HASH_Z_XOR = -559038737L;

    public static ChunkPos containing(final BlockPos pos) {
        return new ChunkPos(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    public static ChunkPos minFromRegion(final int regionX, final int regionZ) {
        return new ChunkPos((long) regionX << 5, (long) regionZ << 5);
    }

    public static ChunkPos maxFromRegion(final int regionX, final int regionZ) {
        return new ChunkPos(((long) regionX << 5) + 31L, ((long) regionZ << 5) + 31L);
    }

    private static boolean fixChunkOutOfBoundsMode() {
        WorldMainSettingScreen.FarLandsConfigData config = WorldMainSettingScreen.FarLandsConfigData.activeConfig;
        return config != null && config.fixChunkOutOfBounds;
    }

    public boolean isValid() {
        return true;
    }

    /** 区块坐标合法性：移除原版 2^31-1 打包限制，仅检查安全裕度 */
    public static boolean isValid(final long x, final long z) {
        return true;
    }

    public boolean isChunkPosValid() {
        if (fixChunkOutOfBoundsMode()) {
            return true;
        }
        return isValid(this.x, this.z);
    }

    public static boolean isChunkPosValid(final int x, final int z) {
        if (fixChunkOutOfBoundsMode()) {
            return true;
        }
        return Mth.absMax(x, z) <= ChunkPyramid.MAX_CHUNK_COORDINATE_VALUE;
    }

    @Override
    public int hashCode() {
        return hash(this.x, this.z);
    }

    /** 稳定混合哈希（对象键用） */
    public static int hash(final long x, final long z) {
        long xTransform = HASH_A * x + HASH_C;
        long zTransform = HASH_A * (z ^ HASH_Z_XOR) + HASH_C;
        return (int) (xTransform ^ zTransform ^ (xTransform >>> 32) ^ (zTransform >>> 32));
    }

    public long getMiddleBlockX() {
        return this.getBlockX(8L);
    }

    public long getMiddleBlockZ() {
        return this.getBlockZ(8L);
    }

    public long getMinBlockX() {
        return SectionPos.sectionToBlockCoordLong(this.x);
    }

    public long getMinBlockZ() {
        return SectionPos.sectionToBlockCoordLong(this.z);
    }

    public long getMaxBlockX() {
        return this.getBlockX(15L);
    }

    public long getMaxBlockZ() {
        return this.getBlockZ(15L);
    }

    public long getRegionX() {
        return this.x >> 5;
    }

    public long getRegionZ() {
        return this.z >> 5;
    }

    public long getRegionLocalX() {
        return this.x & 31L;
    }

    public long getRegionLocalZ() {
        return this.z & 31L;
    }

    public BlockPos getBlockAt(final int x, final int y, final int z) {
        return new BlockPos((int) this.getBlockX(x), y, (int) this.getBlockZ(z));
    }

    public long getBlockX(final long offset) {
        return SectionPos.sectionToBlockCoordLong(this.x, offset);
    }

    public long getBlockZ(final long offset) {
        return SectionPos.sectionToBlockCoordLong(this.z, offset);
    }

    public BlockPos getMiddleBlockPosition(final int y) {
        return new BlockPos((int) this.getMiddleBlockX(), y, (int) this.getMiddleBlockZ());
    }

    public boolean contains(final BlockPos pos) {
        return pos.getX() >= this.getMinBlockX()
                && pos.getZ() >= this.getMinBlockZ()
                && pos.getX() <= this.getMaxBlockX()
                && pos.getZ() <= this.getMaxBlockZ();
    }

    @Override
    public String toString() {
        return "[" + this.x + ", " + this.z + "]";
    }

    public BlockPos getWorldPosition() {
        return new BlockPos((int) this.getMinBlockX(), 0, (int) this.getMinBlockZ());
    }

    public long getChessboardDistance(final ChunkPos pos) {
        return this.getChessboardDistance(pos.x, pos.z);
    }

    public long getChessboardDistance(final long x, final long z) {
        return Math.max(Math.abs(x - this.x), Math.abs(z - this.z));
    }

    public long distanceSquared(final ChunkPos pos) {
        return this.distanceSquared(pos.x, pos.z);
    }

    private long distanceSquared(final long x, final long z) {
        long deltaX = x - this.x;
        long deltaZ = z - this.z;
        return deltaX * deltaX + deltaZ * deltaZ;
    }

    // ═══════════ 256-bit 适配（MCRe NoiseFarlands） ═══════════

    /** 区块坐标精确转 Int256 */
    public Int256 x256() {
        return Int256.of(this.x);
    }

    public Int256 z256() {
        return Int256.of(this.z);
    }

    /** 区块坐标转 256-bit 复合键（高 128 位 = x，低 128 位 = z） */
    public Int256 key256() {
        return this.x256().shiftLeft(128).or(this.z256());
    }

    public static Stream<ChunkPos> rangeClosed(final ChunkPos center, final long radius) {
        return rangeClosed(new ChunkPos(center.x - radius, center.z - radius), new ChunkPos(center.x + radius, center.z + radius));
    }

    public static Stream<ChunkPos> rangeClosed(final ChunkPos from, final ChunkPos to) {
        long xSize = Math.abs(from.x - to.x) + 1L;
        long zSize = Math.abs(from.z - to.z) + 1L;
        final long xDiff = from.x < to.x ? 1L : -1L;
        final long zDiff = from.z < to.z ? 1L : -1L;
        return StreamSupport.stream(new AbstractSpliterator<ChunkPos>(xSize * zSize, 64) {
            private @Nullable ChunkPos pos;

            @Override
            public boolean tryAdvance(final Consumer<? super ChunkPos> action) {
                if (this.pos == null) {
                    this.pos = from;
                } else {
                    long x = this.pos.x;
                    long z = this.pos.z;
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
