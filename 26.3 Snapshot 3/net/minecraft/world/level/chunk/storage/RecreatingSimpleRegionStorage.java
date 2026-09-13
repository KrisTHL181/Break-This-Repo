package net.minecraft.world.level.chunk.storage;

import com.mojang.datafixers.DataFixer;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import org.apache.commons.io.FileUtils;

public class RecreatingSimpleRegionStorage extends SimpleRegionStorage {
   private final IOWorker writeWorker;
   private final Path writeFolder;

   public RecreatingSimpleRegionStorage(
      final RegionStorageInfo readInfo,
      final Path readFolder,
      final RegionStorageInfo writeInfo,
      final Path writeFolder,
      final DataFixer fixerUpper,
      final boolean syncWrites,
      final DataFixTypes dataFixType
   ) {
      super(readInfo, readFolder, fixerUpper, syncWrites, dataFixType);
      this.writeFolder = writeFolder;
      this.writeWorker = new IOWorker(writeInfo, writeFolder, syncWrites);
   }

   @Override
   public CompletableFuture<Void> write(final ChunkPos pos, final Supplier<CompoundTag> supplier) {
      return this.writeWorker.store(pos, supplier);
   }

   @Override
   public void close() throws IOException {
      super.close();
      this.writeWorker.close();
      if (this.writeFolder.toFile().exists()) {
         FileUtils.deleteDirectory(this.writeFolder.toFile());
      }
   }
}
