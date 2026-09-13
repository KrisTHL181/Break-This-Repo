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
      RegionStorageInfo p_330416_,
      Path p_334038_,
      RegionStorageInfo p_332972_,
      Path p_334447_,
      DataFixer p_330614_,
      boolean p_331908_,
      DataFixTypes p_333003_,
      Supplier<LegacyTagFixer> p_454084_
   ) {
      super(p_330416_, p_334038_, p_330614_, p_331908_, p_333003_, p_454084_);
      this.writeFolder = p_334447_;
      this.writeWorker = new IOWorker(p_332972_, p_334447_, p_331908_);
   }

   @Override
   public CompletableFuture<Void> write(ChunkPos p_333713_, Supplier<CompoundTag> p_460338_) {
      this.markChunkDone(p_333713_);
      return this.writeWorker.store(p_333713_, p_460338_);
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
