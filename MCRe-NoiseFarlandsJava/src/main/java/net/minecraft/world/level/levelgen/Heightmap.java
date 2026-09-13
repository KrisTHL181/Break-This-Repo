package net.minecraft.world.level.levelgen;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.BitStorage;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.slf4j.Logger;

public class Heightmap {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Predicate<BlockState> NOT_AIR = input -> !input.isAir();
    private static final Predicate<BlockState> MATERIAL_MOTION_BLOCKING = BlockBehaviour.BlockStateBase::blocksMotion;
    private final BitStorage data;
    private final Predicate<BlockState> isOpaque;
    private final ChunkAccess chunk;

    public Heightmap(final ChunkAccess chunk, final Heightmap.Types heightmapType) {
        this.isOpaque = heightmapType.isOpaque();
        this.chunk = chunk;
        int heightBits = Mth.ceillog2(chunk.getHeight() + 1);
        this.data = new SimpleBitStorage(heightBits, 256);
    }

    public static void primeHeightmaps(final ChunkAccess chunk, final Set<Heightmap.Types> types) {
        if (!types.isEmpty()) {
            int size = types.size();
            ObjectList<Heightmap> heightmaps = new ObjectArrayList<>(size);
            ObjectListIterator<Heightmap> iterator = heightmaps.iterator();
            int highestSectionPosition = chunk.getHighestSectionPosition() + 16;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (Heightmap.Types type : types) {
                        heightmaps.add(chunk.getOrCreateHeightmapUnprimed(type));
                    }

                    for (int y = highestSectionPosition - 1; y >= chunk.getMinY(); y--) {
                        pos.set(x, y, z);
                        BlockState state = chunk.getBlockState(pos);
                        if (!state.is(Blocks.AIR)) {
                            while (iterator.hasNext()) {
                                Heightmap heightmap = iterator.next();
                                if (heightmap.isOpaque.test(state)) {
                                    heightmap.setHeight(x, z, y + 1);
                                    iterator.remove();
                                }
                            }

                            if (heightmaps.isEmpty()) {
                                break;
                            }

                            iterator.back(size);
                        }
                    }
                }
            }
        }
    }

    public boolean update(final int localX, final int localY, final int localZ, final BlockState state) {
        int firstAvailable = this.getFirstAvailable(localX, localZ);
        if (localY <= firstAvailable - 2) {
            return false;
        }

        if (this.isOpaque.test(state)) {
            if (localY >= firstAvailable) {
                this.setHeight(localX, localZ, localY + 1);
                return true;
            }
        } else if (firstAvailable - 1 == localY) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            for (int y = localY - 1; y >= this.chunk.getMinY(); y--) {
                pos.set(localX, y, localZ);
                if (this.isOpaque.test(this.chunk.getBlockState(pos))) {
                    this.setHeight(localX, localZ, y + 1);
                    return true;
                }
            }

            this.setHeight(localX, localZ, this.chunk.getMinY());
            return true;
        }

        return false;
    }

    public int getFirstAvailable(final int x, final int z) {
        return this.getFirstAvailable(getIndex(x, z));
    }

    public int getHighestTaken(final int x, final int z) {
        return this.getFirstAvailable(getIndex(x, z)) - 1;
    }

    private int getFirstAvailable(final int index) {
        return this.data.get(index) + this.chunk.getMinY();
    }

    private void setHeight(final int x, final int z, final int height) {
        this.data.set(getIndex(x, z), height - this.chunk.getMinY());
    }

    public void setRawData(final ChunkAccess chunk, final Heightmap.Types type, final long[] data) {
        long[] rawData = this.data.getRaw();
        if (rawData.length == data.length) {
            System.arraycopy(data, 0, rawData, 0, data.length);
        } else {
            LOGGER.warn("Ignoring heightmap data for chunk {}, size does not match; expected: {}, got: {}", chunk.getPos(), rawData.length, data.length);
            primeHeightmaps(chunk, EnumSet.of(type));
        }
    }

    public long[] getRawData() {
        return this.data.getRaw();
    }

    private static int getIndex(final int x, final int z) {
        return x + z * 16;
    }

    public enum Types implements StringRepresentable {
        WORLD_SURFACE_WG(0, "WORLD_SURFACE_WG", Heightmap.Usage.WORLDGEN, Heightmap.NOT_AIR),
        WORLD_SURFACE(1, "WORLD_SURFACE", Heightmap.Usage.CLIENT, Heightmap.NOT_AIR),
        OCEAN_FLOOR_WG(2, "OCEAN_FLOOR_WG", Heightmap.Usage.WORLDGEN, Heightmap.MATERIAL_MOTION_BLOCKING),
        OCEAN_FLOOR(3, "OCEAN_FLOOR", Heightmap.Usage.LIVE_WORLD, Heightmap.MATERIAL_MOTION_BLOCKING),
        MOTION_BLOCKING(4, "MOTION_BLOCKING", Heightmap.Usage.CLIENT, input -> input.blocksMotion() || !input.getFluidState().isEmpty()),
        MOTION_BLOCKING_NO_LEAVES(
            5,
            "MOTION_BLOCKING_NO_LEAVES",
            Heightmap.Usage.CLIENT,
            input -> (input.blocksMotion() || !input.getFluidState().isEmpty()) && !(input.getBlock() instanceof LeavesBlock)
        );

        public static final Codec<Heightmap.Types> CODEC = StringRepresentable.fromEnum(Heightmap.Types::values);
        private static final IntFunction<Heightmap.Types> BY_ID = ByIdMap.continuous(t -> t.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        public static final StreamCodec<ByteBuf, Heightmap.Types> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, t -> t.id);
        private final int id;
        private final String serializationKey;
        private final Heightmap.Usage usage;
        private final Predicate<BlockState> isOpaque;

        Types(final int id, final String serializationKey, final Heightmap.Usage usage, final Predicate<BlockState> isOpaque) {
            this.id = id;
            this.serializationKey = serializationKey;
            this.usage = usage;
            this.isOpaque = isOpaque;
        }

        public String getSerializationKey() {
            return this.serializationKey;
        }

        public boolean sendToClient() {
            return this.usage == Heightmap.Usage.CLIENT;
        }

        public boolean keepAfterWorldgen() {
            return this.usage != Heightmap.Usage.WORLDGEN;
        }

        public Predicate<BlockState> isOpaque() {
            return this.isOpaque;
        }

        @Override
        public String getSerializedName() {
            return this.serializationKey;
        }
    }

    public enum Usage {
        WORLDGEN,
        LIVE_WORLD,
        CLIENT;
    }
}