package net.minecraft.util.filefix.access;

import com.mojang.logging.LogUtils;
import java.nio.file.Path;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class FileAccess<T extends AutoCloseable> implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final FileAccessProvider fileAccessProvider;
   private final FileResourceType<T> type;
   private final FileRelation fileRelation;
   private @Nullable List<T> files;

   public FileAccess(final FileAccessProvider fileAccessProvider, final FileResourceType<T> type, final FileRelation fileRelation) {
      this.fileAccessProvider = fileAccessProvider;
      this.type = type;
      this.fileRelation = fileRelation;
   }

   public List<T> get() {
      if (this.files == null) {
         Path baseDirectory = this.fileAccessProvider.baseDirectory().get();
         if (baseDirectory == null) {
            throw new IllegalStateException("Cannot access world files");
         }

         this.files = this.fileRelation.getPaths(baseDirectory).stream().map(path -> this.type.create(path, this.fileAccessProvider.dataVersion())).toList();
      }

      return this.files;
   }

   public T getOnlyFile() {
      List<T> files = this.get();
      if (files.size() != 1) {
         throw new IllegalStateException("Trying to get only file, but there are " + files.size() + " files");
      } else {
         return files.getFirst();
      }
   }

   @Override
   public void close() {
      if (this.files != null) {
         for (T file : this.files) {
            try {
               file.close();
            } catch (Exception e) {
               LOGGER.error("Failed to close file: ", e);
            }
         }

         this.files = null;
      }
   }
}
