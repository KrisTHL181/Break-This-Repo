package net.minecraft.server.packs.linkfs;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import org.jspecify.annotations.Nullable;

class LinkFSFileStore extends FileStore {
   private final String name;

   public LinkFSFileStore(String p_249242_) {
      this.name = p_249242_;
   }

   @Override
   public String name() {
      return this.name;
   }

   @Override
   public String type() {
      return "index";
   }

   @Override
   public boolean isReadOnly() {
      return true;
   }

   @Override
   public long getTotalSpace() {
      return 0L;
   }

   @Override
   public long getUsableSpace() {
      return 0L;
   }

   @Override
   public long getUnallocatedSpace() {
      return 0L;
   }

   @Override
   public boolean supportsFileAttributeView(Class<? extends FileAttributeView> p_251407_) {
      return p_251407_ == BasicFileAttributeView.class;
   }

   @Override
   public boolean supportsFileAttributeView(String p_250666_) {
      return "basic".equals(p_250666_);
   }

   @Override
   public <V extends FileStoreAttributeView> @Nullable V getFileStoreAttributeView(Class<V> p_251981_) {
      return null;
   }

   @Override
   public Object getAttribute(String p_249050_) throws IOException {
      throw new UnsupportedOperationException();
   }
}
