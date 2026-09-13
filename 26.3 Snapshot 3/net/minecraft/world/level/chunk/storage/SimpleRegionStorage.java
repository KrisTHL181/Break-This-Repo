package net.minecraft.world.level.chunk.storage;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public class SimpleRegionStorage implements AutoCloseable {
   private final IOWorker worker;
   private final DataFixer fixerUpper;
   private final DataFixTypes dataFixType;

   public SimpleRegionStorage(
      final RegionStorageInfo info, final Path folder, final DataFixer fixerUpper, final boolean syncWrites, final DataFixTypes dataFixType
   ) {
      this.fixerUpper = fixerUpper;
      this.dataFixType = dataFixType;
      this.worker = new IOWorker(info, folder, syncWrites);
   }

   public boolean isOldChunkAround(final ChunkPos pos, final int range) {
      return this.worker.isOldChunkAround(pos, range);
   }

   public CompletableFuture<Optional<CompoundTag>> read(final ChunkPos pos) {
      return this.worker.loadAsync(pos);
   }

   public CompletableFuture<Void> write(final ChunkPos pos, final CompoundTag value) {
      return this.write(pos, () -> value);
   }

   public CompletableFuture<Void> write(final ChunkPos pos, final Supplier<CompoundTag> supplier) {
      return this.worker.store(pos, supplier);
   }

   public CompoundTag upgradeChunkTag(CompoundTag chunkTag, final int defaultVersion, final @Nullable CompoundTag dataFixContextTag, final int targetVersion) {
      int version = NbtUtils.getDataVersion(chunkTag, defaultVersion);
      if (version >= targetVersion) {
         return chunkTag;
      }

      try {
         injectDatafixingContext(chunkTag, dataFixContextTag);
         chunkTag = this.dataFixType.update(this.fixerUpper, chunkTag, version, targetVersion);
         removeDatafixingContext(chunkTag);
         NbtUtils.addDataVersion(chunkTag, targetVersion);
         return chunkTag;
      } catch (Exception e) {
         CrashReport report = CrashReport.forThrowable(e, "Updated chunk");
         CrashReportCategory details = report.addCategory("Updated chunk details");
         details.setDetail("Data version", version);
         details.setDetail("Target version", targetVersion);
         throw new ReportedException(report);
      }
   }

   public CompoundTag upgradeChunkTag(final CompoundTag chunkTag, final int defaultVersion) {
      return this.upgradeChunkTag(chunkTag, defaultVersion, null, SharedConstants.getCurrentVersion().dataVersion().version());
   }

   public Dynamic<Tag> upgradeChunkTag(final Dynamic<Tag> chunkTag, final int defaultVersion) {
      return new Dynamic(
         chunkTag.getOps(),
         this.upgradeChunkTag((CompoundTag)chunkTag.getValue(), defaultVersion, null, SharedConstants.getCurrentVersion().dataVersion().version())
      );
   }

   public static void injectDatafixingContext(final CompoundTag chunkTag, final @Nullable CompoundTag contextTag) {
      if (contextTag != null) {
         chunkTag.put("__context", contextTag);
      }
   }

   private static void removeDatafixingContext(final CompoundTag chunkTag) {
      chunkTag.remove("__context");
   }

   public CompletableFuture<Void> synchronize(final boolean flush) {
      return this.worker.synchronize(flush);
   }

   @Override
   public void close() throws IOException {
      this.worker.close();
   }

   public ChunkScanAccess chunkScanner() {
      return this.worker;
   }

   public RegionStorageInfo storageInfo() {
      return this.worker.storageInfo();
   }
}
