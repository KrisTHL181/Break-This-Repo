package net.minecraft.server.packs.linkfs;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.ReadOnlyFileSystemException;
import java.nio.file.StandardOpenOption;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

class LinkFSProvider extends FileSystemProvider {
   public static final String SCHEME = "x-mc-link";

   @Override
   public String getScheme() {
      return "x-mc-link";
   }

   @Override
   public FileSystem newFileSystem(URI p_251867_, Map<String, ?> p_250970_) {
      throw new UnsupportedOperationException();
   }

   @Override
   public FileSystem getFileSystem(URI p_249279_) {
      throw new UnsupportedOperationException();
   }

   @Override
   public Path getPath(URI p_252294_) {
      throw new UnsupportedOperationException();
   }

   @Override
   public SeekableByteChannel newByteChannel(Path p_251835_, Set<? extends OpenOption> p_251780_, FileAttribute<?>... p_250474_) throws IOException {
      if (!p_251780_.contains(StandardOpenOption.CREATE_NEW)
         && !p_251780_.contains(StandardOpenOption.CREATE)
         && !p_251780_.contains(StandardOpenOption.APPEND)
         && !p_251780_.contains(StandardOpenOption.WRITE)) {
         Path path = toLinkPath(p_251835_).toAbsolutePath().getTargetPath();
         if (path == null) {
            throw new NoSuchFileException(p_251835_.toString());
         } else {
            return Files.newByteChannel(path, p_251780_, p_250474_);
         }
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public DirectoryStream<Path> newDirectoryStream(Path p_250116_, final Filter<? super Path> p_251710_) throws IOException {
      final PathContents.DirectoryContents pathcontents$directorycontents = toLinkPath(p_250116_).toAbsolutePath().getDirectoryContents();
      if (pathcontents$directorycontents == null) {
         throw new NotDirectoryException(p_250116_.toString());
      } else {
         return new DirectoryStream<Path>() {
            @Override
            public Iterator<Path> iterator() {
               return pathcontents$directorycontents.children().values().stream().filter(p_250987_ -> {
                  try {
                     return p_251710_.accept(p_250987_);
                  } catch (IOException ioexception) {
                     throw new DirectoryIteratorException(ioexception);
                  }
               }).map(p_249891_ -> (Path)p_249891_).iterator();
            }

            @Override
            public void close() {
            }
         };
      }
   }

   @Override
   public void createDirectory(Path p_252352_, FileAttribute<?>... p_249694_) {
      throw new ReadOnlyFileSystemException();
   }

   @Override
   public void delete(Path p_252069_) {
      throw new ReadOnlyFileSystemException();
   }

   @Override
   public void copy(Path p_250627_, Path p_248906_, CopyOption... p_249289_) {
      throw new ReadOnlyFileSystemException();
   }

   @Override
   public void move(Path p_250866_, Path p_250335_, CopyOption... p_249156_) {
      throw new ReadOnlyFileSystemException();
   }

   @Override
   public boolean isSameFile(Path p_249846_, Path p_251936_) {
      return p_249846_ instanceof LinkFSPath && p_251936_ instanceof LinkFSPath && p_249846_.equals(p_251936_);
   }

   @Override
   public boolean isHidden(Path p_248957_) {
      return false;
   }

   @Override
   public FileStore getFileStore(Path p_249374_) {
      return toLinkPath(p_249374_).getFileSystem().store();
   }

   @Override
   public void checkAccess(Path p_248517_, AccessMode... p_248805_) throws IOException {
      if (p_248805_.length == 0 && !toLinkPath(p_248517_).exists()) {
         throw new NoSuchFileException(p_248517_.toString());
      }

      AccessMode[] aaccessmode = p_248805_;
      int i = p_248805_.length;
      int j = 0;

      while (j < i) {
         AccessMode accessmode = aaccessmode[j];
         switch (accessmode) {
            case READ:
               if (!toLinkPath(p_248517_).exists()) {
                  throw new NoSuchFileException(p_248517_.toString());
               }
            default:
               j++;
               break;
            case EXECUTE:
            case WRITE:
               throw new AccessDeniedException(accessmode.toString());
         }
      }
   }

   @Override
   public <V extends FileAttributeView> @Nullable V getFileAttributeView(Path p_250166_, Class<V> p_252214_, LinkOption... p_250559_) {
      LinkFSPath linkfspath = toLinkPath(p_250166_);
      return (V)(p_252214_ == BasicFileAttributeView.class ? linkfspath.getBasicAttributeView() : null);
   }

   @Override
   public <A extends BasicFileAttributes> A readAttributes(Path p_249764_, Class<A> p_248604_, LinkOption... p_252280_) throws IOException {
      LinkFSPath linkfspath = toLinkPath(p_249764_).toAbsolutePath();
      if (p_248604_ == BasicFileAttributes.class) {
         return (A)linkfspath.getBasicAttributes();
      } else {
         throw new UnsupportedOperationException("Attributes of type " + p_248604_.getName() + " not supported");
      }
   }

   @Override
   public Map<String, Object> readAttributes(Path p_252124_, String p_249064_, LinkOption... p_252305_) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void setAttribute(Path p_251468_, String p_249411_, Object p_249284_, LinkOption... p_250990_) {
      throw new ReadOnlyFileSystemException();
   }

   private static LinkFSPath toLinkPath(@Nullable Path p_252065_) {
      if (p_252065_ == null) {
         throw new NullPointerException();
      } else if (p_252065_ instanceof LinkFSPath linkfspath) {
         return linkfspath;
      } else {
         throw new ProviderMismatchException();
      }
   }
}
