package net.minecraft.world.level.chunk.storage;

import com.google.common.base.Suppliers;
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
   private final Supplier<LegacyTagFixer> legacyFixer;

   public SimpleRegionStorage(RegionStorageInfo p_327836_, Path p_328804_, DataFixer p_332309_, boolean p_335456_, DataFixTypes p_331426_) {
      this(p_327836_, p_328804_, p_332309_, p_335456_, p_331426_, LegacyTagFixer.EMPTY);
   }

   public SimpleRegionStorage(
      RegionStorageInfo p_452254_, Path p_459771_, DataFixer p_452991_, boolean p_455220_, DataFixTypes p_453017_, Supplier<LegacyTagFixer> p_452228_
   ) {
      this.fixerUpper = p_452991_;
      this.dataFixType = p_453017_;
      this.worker = new IOWorker(p_452254_, p_459771_, p_455220_);
      this.legacyFixer = Suppliers.memoize(p_452228_::get);
   }

   public boolean isOldChunkAround(ChunkPos p_460112_, int p_457756_) {
      return this.worker.isOldChunkAround(p_460112_, p_457756_);
   }

   public CompletableFuture<Optional<CompoundTag>> read(ChunkPos p_328805_) {
      return this.worker.loadAsync(p_328805_);
   }

   public CompletableFuture<Void> write(ChunkPos p_328507_, CompoundTag p_328699_) {
      return this.write(p_328507_, () -> p_328699_);
   }

   public CompletableFuture<Void> write(ChunkPos p_460578_, Supplier<CompoundTag> p_451294_) {
      this.markChunkDone(p_460578_);
      return this.worker.store(p_460578_, p_451294_);
   }

   public CompoundTag upgradeChunkTag(CompoundTag p_457269_, int p_452563_, @Nullable CompoundTag p_454370_) {
      int i = NbtUtils.getDataVersion(p_457269_, p_452563_);
      if (i == SharedConstants.getCurrentVersion().dataVersion().version()) {
         return p_457269_;
      }

      try {
         p_457269_ = this.legacyFixer.get().applyFix(p_457269_);
         injectDatafixingContext(p_457269_, p_454370_);
         p_457269_ = this.dataFixType.updateToCurrentVersion(this.fixerUpper, p_457269_, Math.max(this.legacyFixer.get().targetDataVersion(), i));
         removeDatafixingContext(p_457269_);
         NbtUtils.addCurrentDataVersion(p_457269_);
         return p_457269_;
      } catch (Exception exception) {
         CrashReport crashreport = CrashReport.forThrowable(exception, "Updated chunk");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Updated chunk details");
         crashreportcategory.setDetail("Data version", i);
         throw new ReportedException(crashreport);
      }
   }

   public CompoundTag upgradeChunkTag(CompoundTag p_330988_, int p_328203_) {
      return this.upgradeChunkTag(p_330988_, p_328203_, null);
   }

   public Dynamic<Tag> upgradeChunkTag(Dynamic<Tag> p_329521_, int p_334930_) {
      return new Dynamic(p_329521_.getOps(), this.upgradeChunkTag((CompoundTag)p_329521_.getValue(), p_334930_, null));
   }

   public static void injectDatafixingContext(CompoundTag p_460950_, @Nullable CompoundTag p_456449_) {
      if (p_456449_ != null) {
         p_460950_.put("__context", p_456449_);
      }
   }

   private static void removeDatafixingContext(CompoundTag p_460251_) {
      p_460251_.remove("__context");
   }

   protected void markChunkDone(ChunkPos p_455625_) {
      this.legacyFixer.get().markChunkDone(p_455625_);
   }

   public CompletableFuture<Void> synchronize(boolean p_334675_) {
      return this.worker.synchronize(p_334675_);
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
