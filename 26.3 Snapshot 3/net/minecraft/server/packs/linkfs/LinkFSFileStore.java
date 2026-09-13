package net.minecraft.server.packs.linkfs;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import org.jspecify.annotations.Nullable;

class LinkFSFileStore extends FileStore {
   private final String name;

   public LinkFSFileStore(final String name) {
      this.name = name;
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
   public boolean supportsFileAttributeView(final Class<? extends FileAttributeView> type) {
      return type == BasicFileAttributeView.class;
   }

   @Override
   public boolean supportsFileAttributeView(final String name) {
      return "basic".equals(name);
   }

   @Override
   public <V extends FileStoreAttributeView> @Nullable V getFileStoreAttributeView(final Class<V> type) {
      return null;
   }

   @Override
   public Object getAttribute(final String attribute) throws IOException {
      throw new UnsupportedOperationException();
   }
}
