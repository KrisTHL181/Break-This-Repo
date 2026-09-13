package net.minecraft.world.level.chunk.storage;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.util.ExceptionCollector;
import net.minecraft.util.FileUtil;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public final class RegionFileStorage implements AutoCloseable {
   public static final String ANVIL_EXTENSION = ".mca";
   private static final int MAX_CACHE_SIZE = 256;
   private final Long2ObjectLinkedOpenHashMap<RegionFile> regionCache = new Long2ObjectLinkedOpenHashMap();
   private final RegionStorageInfo info;
   private final Path folder;
   private final boolean sync;

   RegionFileStorage(RegionStorageInfo p_330451_, Path p_196954_, boolean p_196955_) {
      this.folder = p_196954_;
      this.sync = p_196955_;
      this.info = p_330451_;
   }

   private RegionFile getRegionFile(ChunkPos p_63712_) throws IOException {
      long i = ChunkPos.asLong(p_63712_.getRegionX(), p_63712_.getRegionZ());
      RegionFile regionfile = (RegionFile)this.regionCache.getAndMoveToFirst(i);
      if (regionfile != null) {
         return regionfile;
      }

      if (this.regionCache.size() >= 256) {
         ((RegionFile)this.regionCache.removeLast()).close();
      }

      FileUtil.createDirectoriesSafe(this.folder);
      Path path = this.folder.resolve("r." + p_63712_.getRegionX() + "." + p_63712_.getRegionZ() + ".mca");
      RegionFile regionfile1 = new RegionFile(this.info, path, this.folder, this.sync);
      this.regionCache.putAndMoveToFirst(i, regionfile1);
      return regionfile1;
   }

   public @Nullable CompoundTag read(ChunkPos p_63707_) throws IOException {
      RegionFile regionfile = this.getRegionFile(p_63707_);

      try (DataInputStream datainputstream = regionfile.getChunkDataInputStream(p_63707_)) {
         return datainputstream == null ? null : NbtIo.read(datainputstream);
      }
   }

   public void scanChunk(ChunkPos p_196957_, StreamTagVisitor p_196958_) throws IOException {
      RegionFile regionfile = this.getRegionFile(p_196957_);

      try (DataInputStream datainputstream = regionfile.getChunkDataInputStream(p_196957_)) {
         if (datainputstream != null) {
            NbtIo.parse(datainputstream, p_196958_, NbtAccounter.unlimitedHeap());
         }
      }
   }

   protected void write(ChunkPos p_63709_, @Nullable CompoundTag p_63710_) throws IOException {
      if (!SharedConstants.DEBUG_DONT_SAVE_WORLD) {
         RegionFile regionfile = this.getRegionFile(p_63709_);
         if (p_63710_ == null) {
            regionfile.clear(p_63709_);
         } else {
            try (DataOutputStream dataoutputstream = regionfile.getChunkDataOutputStream(p_63709_)) {
               NbtIo.write(p_63710_, dataoutputstream);
            }
         }
      }
   }

   @Override
   public void close() throws IOException {
      ExceptionCollector<IOException> exceptioncollector = new ExceptionCollector<>();
      ObjectIterator var2 = this.regionCache.values().iterator();

      while (var2.hasNext()) {
         RegionFile regionfile = (RegionFile)var2.next();

         try {
            regionfile.close();
         } catch (IOException ioexception) {
            exceptioncollector.add(ioexception);
         }
      }

      exceptioncollector.throwIfPresent();
   }

   public void flush() throws IOException {
      ObjectIterator var1 = this.regionCache.values().iterator();

      while (var1.hasNext()) {
         RegionFile regionfile = (RegionFile)var1.next();
         regionfile.flush();
      }
   }

   public RegionStorageInfo info() {
      return this.info;
   }
}
