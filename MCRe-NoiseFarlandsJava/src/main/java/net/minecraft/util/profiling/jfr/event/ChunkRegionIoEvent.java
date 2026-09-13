package net.minecraft.util.profiling.jfr.event;

import jdk.jfr.Category;
import jdk.jfr.Enabled;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;

@Category({"Minecraft", "Storage"})
@StackTrace(false)
@Enabled(false)
public abstract class ChunkRegionIoEvent extends Event {
    @Name("regionPosX")
    @Label("Region X Position")
    public final int regionPosX;
    @Name("regionPosZ")
    @Label("Region Z Position")
    public final int regionPosZ;
    @Name("localPosX")
    @Label("Local X Position")
    public final int localChunkPosX;
    @Name("localPosZ")
    @Label("Local Z Position")
    public final int localChunkPosZ;
    @Name("chunkPosX")
    @Label("Chunk X Position")
    public final int chunkPosX;
    @Name("chunkPosZ")
    @Label("Chunk Z Position")
    public final int chunkPosZ;
    @Name("level")
    @Label("Level Id")
    public final String levelId;
    @Name("dimension")
    @Label("Dimension")
    public final String dimension;
    @Name("type")
    @Label("Type")
    public final String type;
    @Name("compression")
    @Label("Compression")
    public final String compression;
    @Name("bytes")
    @Label("Bytes")
    public final int bytes;

    protected ChunkRegionIoEvent(final RegionStorageInfo info, final ChunkPos chunkPos, final RegionFileVersion version, final int bytes) {
        this.regionPosX = (int)chunkPos.getRegionX();
        this.regionPosZ = (int)chunkPos.getRegionZ();
        this.localChunkPosX = (int)chunkPos.getRegionLocalX();
        this.localChunkPosZ = (int)chunkPos.getRegionLocalZ();
        this.chunkPosX = (int)chunkPos.x();
        this.chunkPosZ = (int)chunkPos.z();
        this.levelId = info.level();
        this.dimension = info.dimension().identifier().toString();
        this.type = info.type();
        this.compression = "standard:" + version.getId();
        this.bytes = bytes;
    }

    public static class Fields {
        public static final String REGION_POS_X = "regionPosX";
        public static final String REGION_POS_Z = "regionPosZ";
        public static final String LOCAL_POS_X = "localPosX";
        public static final String LOCAL_POS_Z = "localPosZ";
        public static final String CHUNK_POS_X = "chunkPosX";
        public static final String CHUNK_POS_Z = "chunkPosZ";
        public static final String LEVEL = "level";
        public static final String DIMENSION = "dimension";
        public static final String TYPE = "type";
        public static final String COMPRESSION = "compression";
        public static final String BYTES = "bytes";

        private Fields() {
        }
    }
}