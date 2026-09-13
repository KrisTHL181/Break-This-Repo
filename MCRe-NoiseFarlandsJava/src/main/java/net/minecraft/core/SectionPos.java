package net.minecraft.core;

import io.netty.buffer.ByteBuf;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.entity.EntityAccess;
import net.MinecraftTools.Math._256Bit.Int256;
import net.MinecraftTools.Math._256Bit.utils.Vec3d256;

/**
 * SectionPos — 区块节坐标（MCRe NoiseFarlands 对象化版）
 *
 * <p>原版用 asLong() 将 (int x, int y, int z) 打包进 long（22+20+22 位），
 * 坐标上限被锁死在 ±2^23（约 8,388,607 区块节）。本版：移除打包系统，
 * SectionPos 对象本身即键（不可变 + hashCode/equals），坐标以 int 存储
 * （32 位，为后续 long/256-bit 升级保留），并适配 256-bit（Int256）。
 */
public class SectionPos extends Vec3i {
    public static final int SECTION_BITS = 4;
    public static final int SECTION_SIZE = 16;
    public static final int SECTION_BLOCK_COUNT = 4096;
    public static final int SECTION_MASK = 15;
    public static final int SECTION_HALF_SIZE = 8;
    public static final int SECTION_MAX_INDEX = 15;
    private static final int RELATIVE_X_SHIFT = 8;
    private static final int RELATIVE_Y_SHIFT = 0;
    private static final int RELATIVE_Z_SHIFT = 4;
    public static final StreamCodec<ByteBuf, SectionPos> STREAM_CODEC = new StreamCodec<ByteBuf, SectionPos>() {
        public SectionPos decode(final ByteBuf input) {
            return SectionPos.of(input.readInt(), input.readInt(), input.readInt());
        }

        public void encode(final ByteBuf output, final SectionPos value) {
            output.writeInt(value.x());
            output.writeInt(value.y());
            output.writeInt(value.z());
        }
    };

    private SectionPos(final int x, final int y, final int z) {
        super(x, y, z);
    }

    public static SectionPos of(final int x, final int y, final int z) {
        return new SectionPos(x, y, z);
    }

    public static SectionPos of(final BlockPos pos) {
        return new SectionPos(blockToSectionCoord(pos.getX()), blockToSectionCoord(pos.getY()), blockToSectionCoord(pos.getZ()));
    }

    public static SectionPos of(final ChunkPos pos, final int sectionY) {
        return new SectionPos((int)pos.x(), sectionY, (int)pos.z());
    }

    public static SectionPos of(final EntityAccess entity) {
        return of(entity.blockPosition());
    }

    public static SectionPos of(final Position pos) {
        return new SectionPos(
            (int)(Mth.lfloor(pos.x()) >> 4),
            (int)(Mth.lfloor(pos.y()) >> 4),
            (int)(Mth.lfloor(pos.z()) >> 4)
        );
    }

    public static SectionPos bottomOf(final ChunkAccess chunk) {
        return of(chunk.getPos(), chunk.getMinSectionY());
    }

    public static int posToSectionCoord(final double pos) {
        return blockToSectionCoord(Mth.floor(pos));
    }

    public static int blockToSectionCoord(final int blockCoord) {
        return blockCoord >> 4;
    }

    public static int blockToSectionCoord(final double coord) {
        return Mth.floor(coord) >> 4;
    }

    /** long 坐标 → 区块节坐标（ChunkPos long 化支持） */
    public static long blockToSectionCoord(final long blockCoord) {
        return blockCoord >> 4;
    }

    public static int sectionRelative(final int blockCoord) {
        return blockCoord & 15;
    }

    public static short sectionRelativePos(final BlockPos pos) {
        int x = sectionRelative(pos.getX());
        int y = sectionRelative(pos.getY());
        int z = sectionRelative(pos.getZ());
        return (short)(x << 8 | z << 4 | y << 0);
    }

    public static int sectionRelativeX(final short relative) {
        return relative >>> 8 & 15;
    }

    public static int sectionRelativeY(final short relative) {
        return relative >>> 0 & 15;
    }

    public static int sectionRelativeZ(final short relative) {
        return relative >>> 4 & 15;
    }

    public int relativeToBlockX(final short relative) {
        return this.minBlockX() + sectionRelativeX(relative);
    }

    public int relativeToBlockY(final short relative) {
        return this.minBlockY() + sectionRelativeY(relative);
    }

    public int relativeToBlockZ(final short relative) {
        return this.minBlockZ() + sectionRelativeZ(relative);
    }

    public BlockPos relativeToBlockPos(final short relative) {
        return new BlockPos(this.relativeToBlockX(relative), this.relativeToBlockY(relative), this.relativeToBlockZ(relative));
    }

    public static int sectionToBlockCoord(final int sectionCoord) {
        return sectionCoord << 4;
    }

    public static int sectionToBlockCoord(final int sectionCoord, final int offset) {
        return sectionToBlockCoord(sectionCoord) + offset;
    }

    /** long 区块节坐标 → 方块坐标 */
    public static long sectionToBlockCoordLong(final long sectionCoord) {
        return sectionCoord << 4;
    }

    public static long sectionToBlockCoordLong(final long sectionCoord, final long offset) {
        return (sectionCoord << 4) + offset;
    }

    public int x() {
        return this.getX();
    }

    public int y() {
        return this.getY();
    }

    public int z() {
        return this.getZ();
    }

    public int minBlockX() {
        return sectionToBlockCoord(this.x());
    }

    public int minBlockY() {
        return sectionToBlockCoord(this.y());
    }

    public int minBlockZ() {
        return sectionToBlockCoord(this.z());
    }

    public int maxBlockX() {
        return sectionToBlockCoord(this.x(), 15);
    }

    public int maxBlockY() {
        return sectionToBlockCoord(this.y(), 15);
    }

    public int maxBlockZ() {
        return sectionToBlockCoord(this.z(), 15);
    }

    public BlockPos origin() {
        return new BlockPos(sectionToBlockCoord(this.x()), sectionToBlockCoord(this.y()), sectionToBlockCoord(this.z()));
    }

    // ═══════════ far lands long 化（MCRe NoiseFarlands） ═══════════
    // 区块节坐标（int，2^27 级）本身不会溢出 int，但「section → 方块坐标」的
    // sectionCoord << 4 在 sectionCoord > 2^27 时溢出（即世界方块坐标 > 2^31）。
    // 渲染管线（编译/遮挡剔除/排序）统一用 long 版方块坐标。

    /** section 原点方块坐标（long，防 2^31 溢出） */
    public long minBlockXLong() {
        return (long)this.x() << 4;
    }

    public long minBlockYLong() {
        return (long)this.y() << 4;
    }

    public long minBlockZLong() {
        return (long)this.z() << 4;
    }

    /** section 中心方块坐标（long） */
    public long centerXLong() {
        return ((long)this.x() << 4) + 8;
    }

    public long centerYLong() {
        return ((long)this.y() << 4) + 8;
    }

    public long centerZLong() {
        return ((long)this.z() << 4) + 8;
    }

    public BlockPos center() {
        return this.origin().offset(8, 8, 8);
    }

    public ChunkPos chunk() {
        return new ChunkPos(this.x(), this.z());
    }

    public SectionPos offset(final int x, final int y, final int z) {
        return x == 0 && y == 0 && z == 0 ? this : new SectionPos(this.x() + x, this.y() + y, this.z() + z);
    }

    public Stream<BlockPos> blocksInside() {
        return BlockPos.betweenClosedStream(this.minBlockX(), this.minBlockY(), this.minBlockZ(), this.maxBlockX(), this.maxBlockY(), this.maxBlockZ());
    }

    public static Stream<SectionPos> cube(final SectionPos center, final int radius) {
        int x = center.x();
        int y = center.y();
        int z = center.z();
        return betweenClosedStream(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
    }

    public static Stream<SectionPos> aroundChunk(final ChunkPos center, final int radius, final int minSection, final int maxSection) {
        int x = (int)center.x();
        int z = (int)center.z();
        return betweenClosedStream(x - radius, minSection, z - radius, x + radius, maxSection, z + radius);
    }

    public static Stream<SectionPos> betweenClosedStream(final int minX, final int minY, final int minZ, final int maxX, final int maxY, final int maxZ) {
        return StreamSupport.stream(new AbstractSpliterator<SectionPos>((maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1), 64) {
            private final Cursor3D cursor = new Cursor3D(minX, minY, minZ, maxX, maxY, maxZ);

            @Override
            public boolean tryAdvance(final Consumer<? super SectionPos> action) {
                if (this.cursor.advance()) {
                    action.accept(new SectionPos(this.cursor.nextX(), this.cursor.nextY(), this.cursor.nextZ()));
                    return true;
                } else {
                    return false;
                }
            }
        }, false);
    }

    /** 遍历方块位置周围及所在区块节（对象化，替代原 long 打包回调） */
    public static void aroundAndAtBlockPos(final BlockPos blockPos, final Consumer<SectionPos> sectionConsumer) {
        aroundAndAtBlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ(), sectionConsumer);
    }

    public static void aroundAndAtBlockPos(final int blockX, final int blockY, final int blockZ, final Consumer<SectionPos> sectionConsumer) {
        int minSectionX = blockToSectionCoord(blockX - 1);
        int maxSectionX = blockToSectionCoord(blockX + 1);
        int minSectionY = blockToSectionCoord(blockY - 1);
        int maxSectionY = blockToSectionCoord(blockY + 1);
        int minSectionZ = blockToSectionCoord(blockZ - 1);
        int maxSectionZ = blockToSectionCoord(blockZ + 1);
        if (minSectionX == maxSectionX && minSectionY == maxSectionY && minSectionZ == maxSectionZ) {
            sectionConsumer.accept(of(minSectionX, minSectionY, minSectionZ));
        } else {
            for (int sectionX = minSectionX; sectionX <= maxSectionX; sectionX++) {
                for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
                    for (int sectionZ = minSectionZ; sectionZ <= maxSectionZ; sectionZ++) {
                        sectionConsumer.accept(of(sectionX, sectionY, sectionZ));
                    }
                }
            }
        }
    }

    // ═══════════ 256-bit 适配（MCRe NoiseFarlands） ═══════════

    /** 区块节坐标精确转 Int256 */
    public Int256 x256() {
        return Int256.of(this.x());
    }

    public Int256 y256() {
        return Int256.of(this.y());
    }

    public Int256 z256() {
        return Int256.of(this.z());
    }

    /** 转 256-bit 向量（方块坐标级） */
    public Vec3d256 to256() {
        return Vec3d256.ofInt(Int256.of(this.minBlockX()), Int256.of(this.minBlockY()), Int256.of(this.minBlockZ()));
    }

    /** 256-bit 向量 → 区块节（floor 到 16 对齐） */
    public static SectionPos from256(final Vec3d256 pos) {
        return of(
            (int)blockToSectionCoord(pos.x.floor().longValue()),
            (int)blockToSectionCoord(pos.y.floor().longValue()),
            (int)blockToSectionCoord(pos.z.floor().longValue())
        );
    }
}
