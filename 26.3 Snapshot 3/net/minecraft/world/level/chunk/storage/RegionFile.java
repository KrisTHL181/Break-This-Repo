package net.minecraft.world.level.chunk.storage;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.logging.LogUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class RegionFile implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int SECTOR_BYTES = 4096;
   @VisibleForTesting
   protected static final int SECTOR_INTS = 1024;
   private static final int CHUNK_HEADER_SIZE = 5;
   private static final int HEADER_OFFSET = 0;
   private static final ByteBuffer PADDING_BUFFER = ByteBuffer.allocateDirect(1);
   private static final String EXTERNAL_FILE_EXTENSION = ".mcc";
   private static final int EXTERNAL_STREAM_FLAG = 128;
   private static final int EXTERNAL_CHUNK_THRESHOLD = 256;
   private static final int CHUNK_NOT_PRESENT = 0;
   private final RegionStorageInfo info;
   private final Path path;
   private final FileChannel file;
   private final Path externalFileDir;
   private final RegionFileVersion version;
   private final ByteBuffer header = ByteBuffer.allocateDirect(8192);
   private final IntBuffer offsets;
   private final IntBuffer timestamps;
   @VisibleForTesting
   protected final RegionBitmap usedSectors = new RegionBitmap();

   public RegionFile(final RegionStorageInfo info, final Path path, final Path externalFileDir, final boolean sync) throws IOException {
      this(info, path, externalFileDir, RegionFileVersion.getSelected(), sync);
   }

   public RegionFile(final RegionStorageInfo info, final Path path, final Path externalFileDir, final RegionFileVersion version, final boolean sync) throws IOException {
      this.info = info;
      this.path = path;
      this.version = version;
      if (!Files.isDirectory(externalFileDir)) {
         throw new IllegalArgumentException("Expected directory, got " + externalFileDir.toAbsolutePath());
      }

      this.externalFileDir = externalFileDir;
      this.offsets = this.header.asIntBuffer();
      this.offsets.limit(1024);
      this.header.position(4096);
      this.timestamps = this.header.asIntBuffer();
      if (sync) {
         this.file = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.DSYNC);
      } else {
         this.file = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
      }

      this.usedSectors.force(0, 2);
      this.header.position(0);
      int readHeaderBytes = this.file.read(this.header, 0L);
      if (readHeaderBytes != -1) {
         if (readHeaderBytes != 8192) {
            LOGGER.warn("Region file {} has truncated header: {}", path, readHeaderBytes);
         }

         long size = Files.size(path);

         for (int i = 0; i < 1024; i++) {
            int offset = this.offsets.get(i);
            if (offset != 0) {
               int sectorNumber = getSectorNumber(offset);
               int numSectors = getNumSectors(offset);
               if (sectorNumber < 2) {
                  LOGGER.warn("Region file {} has invalid sector at index: {}; sector {} overlaps with header", new Object[]{path, i, sectorNumber});
                  this.offsets.put(i, 0);
               } else if (numSectors == 0) {
                  LOGGER.warn("Region file {} has an invalid sector at index: {}; size has to be > 0", path, i);
                  this.offsets.put(i, 0);
               } else if (sectorNumber * 4096L > size) {
                  LOGGER.warn("Region file {} has an invalid sector at index: {}; sector {} is out of bounds", new Object[]{path, i, sectorNumber});
                  this.offsets.put(i, 0);
               } else {
                  this.usedSectors.force(sectorNumber, numSectors);
               }
            }
         }
      }
   }

   public Path getPath() {
      return this.path;
   }

   private Path getExternalChunkPath(final ChunkPos pos) {
      String externalFileName = "c." + pos.x() + "." + pos.z() + ".mcc";
      return this.externalFileDir.resolve(externalFileName);
   }

   public synchronized @Nullable DataInputStream getChunkDataInputStream(final ChunkPos pos) throws IOException {
      int offset = this.getOffset(pos);
      if (offset == 0) {
         return null;
      }

      int sectorNumber = getSectorNumber(offset);
      int numSectors = getNumSectors(offset);
      int sectorsLength = numSectors * 4096;
      ByteBuffer buffer = ByteBuffer.allocate(sectorsLength);
      this.file.read(buffer, sectorNumber * 4096);
      buffer.flip();
      if (buffer.remaining() < 5) {
         LOGGER.error("Chunk {} header is truncated: expected {} but read {}", new Object[]{pos, sectorsLength, buffer.remaining()});
         return null;
      }

      int length = buffer.getInt();
      byte versionId = buffer.get();
      if (length == 0) {
         LOGGER.warn("Chunk {} is allocated, but stream is missing", pos);
         return null;
      }

      int streamLength = length - 1;
      if (isExternalStreamChunk(versionId)) {
         if (streamLength != 0) {
            LOGGER.warn("Chunk has both internal and external streams");
         }

         return this.createExternalChunkInputStream(pos, getExternalChunkVersion(versionId));
      } else if (streamLength > buffer.remaining()) {
         LOGGER.error("Chunk {} stream is truncated: expected {} but read {}", new Object[]{pos, streamLength, buffer.remaining()});
         return null;
      } else if (streamLength < 0) {
         LOGGER.error("Declared size {} of chunk {} is negative", length, pos);
         return null;
      } else {
         JvmProfiler.INSTANCE.onRegionFileRead(this.info, pos, this.version, streamLength);
         return this.createChunkInputStream(pos, versionId, createStream(buffer, streamLength));
      }
   }

   private static int getTimestamp() {
      return (int)(Util.getEpochMillis() / 1000L);
   }

   private static boolean isExternalStreamChunk(final byte version) {
      return (version & 128) != 0;
   }

   private static byte getExternalChunkVersion(final byte version) {
      return (byte)(version & -129);
   }

   private @Nullable DataInputStream createChunkInputStream(final ChunkPos pos, final byte versionId, final InputStream chunkStream) throws IOException {
      RegionFileVersion version = RegionFileVersion.fromId(versionId);
      if (version == RegionFileVersion.VERSION_CUSTOM) {
         String type = new DataInputStream(chunkStream).readUTF();
         Identifier id = Identifier.tryParse(type);
         if (id != null) {
            LOGGER.error("Unrecognized custom compression {}", id);
            return null;
         } else {
            LOGGER.error("Invalid custom compression id {}", type);
            return null;
         }
      } else if (version == null) {
         LOGGER.error("Chunk {} has invalid chunk stream version {}", pos, versionId);
         return null;
      } else {
         return new DataInputStream(version.wrap(chunkStream));
      }
   }

   private @Nullable DataInputStream createExternalChunkInputStream(final ChunkPos pos, final byte versionId) throws IOException {
      Path externalFile = this.getExternalChunkPath(pos);
      if (!Files.isRegularFile(externalFile)) {
         LOGGER.error("External chunk path {} is not file", externalFile);
         return null;
      } else {
         return this.createChunkInputStream(pos, versionId, Files.newInputStream(externalFile));
      }
   }

   private static ByteArrayInputStream createStream(final ByteBuffer buffer, final int length) {
      return new ByteArrayInputStream(buffer.array(), buffer.position(), length);
   }

   private int packSectorOffset(final int index, final int size) {
      return index << 8 | size;
   }

   private static int getNumSectors(final int offset) {
      return offset & 0xFF;
   }

   private static int getSectorNumber(final int offset) {
      return offset >> 8 & 16777215;
   }

   private static int sizeToSectors(final int size) {
      return (size + 4096 - 1) / 4096;
   }

   public boolean doesChunkExist(final ChunkPos pos) {
      int offset = this.getOffset(pos);
      if (offset == 0) {
         return false;
      }

      int sectorNumber = getSectorNumber(offset);
      int numSectors = getNumSectors(offset);
      ByteBuffer streamHeader = ByteBuffer.allocate(5);

      try {
         this.file.read(streamHeader, sectorNumber * 4096);
         streamHeader.flip();
         if (streamHeader.remaining() != 5) {
            return false;
         }

         int length = streamHeader.getInt();
         byte versionId = streamHeader.get();
         if (isExternalStreamChunk(versionId)) {
            if (!RegionFileVersion.isValidVersion(getExternalChunkVersion(versionId))) {
               return false;
            }

            if (!Files.isRegularFile(this.getExternalChunkPath(pos))) {
               return false;
            }
         } else {
            if (!RegionFileVersion.isValidVersion(versionId)) {
               return false;
            }

            if (length == 0) {
               return false;
            }

            int streamLength = length - 1;
            if (streamLength < 0 || streamLength > 4096 * numSectors) {
               return false;
            }
         }

         return true;
      } catch (IOException e) {
         return false;
      }
   }

   public DataOutputStream getChunkDataOutputStream(final ChunkPos pos) throws IOException {
      return new DataOutputStream(this.version.wrap(new RegionFile.ChunkBuffer(pos)));
   }

   public void flush() throws IOException {
      this.file.force(true);
   }

   public void clear(final ChunkPos pos) throws IOException {
      int offsetIndex = getOffsetIndex(pos);
      int offset = this.offsets.get(offsetIndex);
      if (offset != 0) {
         this.offsets.put(offsetIndex, 0);
         this.timestamps.put(offsetIndex, getTimestamp());
         this.writeHeader();
         Files.deleteIfExists(this.getExternalChunkPath(pos));
         this.usedSectors.free(getSectorNumber(offset), getNumSectors(offset));
      }
   }

   protected synchronized void write(final ChunkPos pos, final ByteBuffer data) throws IOException {
      int offsetIndex = getOffsetIndex(pos);
      int offset = this.offsets.get(offsetIndex);
      int sectorNumber = getSectorNumber(offset);
      int currentSectorCount = getNumSectors(offset);
      int dataSize = data.remaining();
      int sectorsNeeded = sizeToSectors(dataSize);
      int newSectorNumber;
      RegionFile.CommitOp commitOp;
      if (sectorsNeeded >= 256) {
         Path externalChunkPath = this.getExternalChunkPath(pos);
         LOGGER.warn("Saving oversized chunk {} ({} bytes} to external file {}", new Object[]{pos, dataSize, externalChunkPath});
         sectorsNeeded = 1;
         newSectorNumber = this.usedSectors.allocate(sectorsNeeded);
         commitOp = this.writeToExternalFile(externalChunkPath, data);
         ByteBuffer stub = this.createExternalStub();
         this.file.write(stub, newSectorNumber * 4096);
      } else {
         newSectorNumber = this.usedSectors.allocate(sectorsNeeded);
         commitOp = () -> Files.deleteIfExists(this.getExternalChunkPath(pos));
         this.file.write(data, newSectorNumber * 4096);
      }

      this.offsets.put(offsetIndex, this.packSectorOffset(newSectorNumber, sectorsNeeded));
      this.timestamps.put(offsetIndex, getTimestamp());
      this.writeHeader();
      commitOp.run();
      if (sectorNumber != 0) {
         this.usedSectors.free(sectorNumber, currentSectorCount);
      }
   }

   private ByteBuffer createExternalStub() {
      ByteBuffer stub = ByteBuffer.allocate(5);
      stub.putInt(1);
      stub.put((byte)(this.version.getId() | 128));
      stub.flip();
      return stub;
   }

   private RegionFile.CommitOp writeToExternalFile(final Path path, final ByteBuffer data) throws IOException {
      Path tmpPath = Files.createTempFile(this.externalFileDir, "tmp", null);

      try (FileChannel extFile = FileChannel.open(tmpPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
         data.position(5);
         extFile.write(data);
      }

      return () -> Files.move(tmpPath, path, StandardCopyOption.REPLACE_EXISTING);
   }

   private void writeHeader() throws IOException {
      this.header.position(0);
      this.file.write(this.header, 0L);
   }

   private int getOffset(final ChunkPos pos) {
      return this.offsets.get(getOffsetIndex(pos));
   }

   public boolean hasChunk(final ChunkPos pos) {
      return this.getOffset(pos) != 0;
   }

   private static int getOffsetIndex(final ChunkPos pos) {
      return pos.getRegionLocalX() + pos.getRegionLocalZ() * 32;
   }

   @Override
   public void close() throws IOException {
      try {
         this.padToFullSector();
      } finally {
         try {
            this.file.force(true);
         } finally {
            this.file.close();
         }
      }
   }

   private void padToFullSector() throws IOException {
      int fileSize = (int)this.file.size();
      int paddedSize = sizeToSectors(fileSize) * 4096;
      if (fileSize != paddedSize) {
         ByteBuffer padding = PADDING_BUFFER.duplicate();
         padding.position(0);
         this.file.write(padding, paddedSize - 1);
      }
   }

   private class ChunkBuffer extends ByteArrayOutputStream {
      private final ChunkPos pos;

      public ChunkBuffer(final ChunkPos pos) {
         super(8096);
         super.write(0);
         super.write(0);
         super.write(0);
         super.write(0);
         super.write(RegionFile.this.version.getId());
         this.pos = pos;
      }

      @Override
      public void close() throws IOException {
         ByteBuffer result = ByteBuffer.wrap(this.buf, 0, this.count);
         int streamLength = this.count - 5 + 1;
         JvmProfiler.INSTANCE.onRegionFileWrite(RegionFile.this.info, this.pos, RegionFile.this.version, streamLength);
         result.putInt(0, streamLength);
         RegionFile.this.write(this.pos, result);
      }
   }

   private interface CommitOp {
      void run() throws IOException;
   }
}
