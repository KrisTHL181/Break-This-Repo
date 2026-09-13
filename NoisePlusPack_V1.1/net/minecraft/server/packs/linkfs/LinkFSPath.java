package net.minecraft.server.packs.linkfs;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.ReadOnlyFileSystemException;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

class LinkFSPath implements Path {
   private static final BasicFileAttributes DIRECTORY_ATTRIBUTES = new DummyFileAttributes() {
      @Override
      public boolean isRegularFile() {
         return false;
      }

      @Override
      public boolean isDirectory() {
         return true;
      }
   };
   private static final BasicFileAttributes FILE_ATTRIBUTES = new DummyFileAttributes() {
      @Override
      public boolean isRegularFile() {
         return true;
      }

      @Override
      public boolean isDirectory() {
         return false;
      }
   };
   private static final Comparator<LinkFSPath> PATH_COMPARATOR = Comparator.comparing(LinkFSPath::pathToString);
   private final String name;
   private final LinkFileSystem fileSystem;
   private final @Nullable LinkFSPath parent;
   private @Nullable List<String> pathToRoot;
   private @Nullable String pathString;
   private final PathContents pathContents;

   public LinkFSPath(LinkFileSystem p_251111_, String p_250681_, @Nullable LinkFSPath p_251363_, PathContents p_251268_) {
      this.fileSystem = p_251111_;
      this.name = p_250681_;
      this.parent = p_251363_;
      this.pathContents = p_251268_;
   }

   private LinkFSPath createRelativePath(@Nullable LinkFSPath p_249276_, String p_249966_) {
      return new LinkFSPath(this.fileSystem, p_249966_, p_249276_, PathContents.RELATIVE);
   }

   public LinkFileSystem getFileSystem() {
      return this.fileSystem;
   }

   @Override
   public boolean isAbsolute() {
      return this.pathContents != PathContents.RELATIVE;
   }

   @Override
   public File toFile() {
      if (this.pathContents instanceof PathContents.FileContents pathcontents$filecontents) {
         return pathcontents$filecontents.contents().toFile();
      } else {
         throw new UnsupportedOperationException("Path " + this.pathToString() + " does not represent file");
      }
   }

   public @Nullable LinkFSPath getRoot() {
      return this.isAbsolute() ? this.fileSystem.rootPath() : null;
   }

   public LinkFSPath getFileName() {
      return this.createRelativePath(null, this.name);
   }

   public @Nullable LinkFSPath getParent() {
      return this.parent;
   }

   @Override
   public int getNameCount() {
      return this.pathToRoot().size();
   }

   private List<String> pathToRoot() {
      if (this.name.isEmpty()) {
         return List.of();
      }

      if (this.pathToRoot == null) {
         Builder<String> builder = ImmutableList.builder();
         if (this.parent != null) {
            builder.addAll(this.parent.pathToRoot());
         }

         builder.add(this.name);
         this.pathToRoot = builder.build();
      }

      return this.pathToRoot;
   }

   public LinkFSPath getName(int p_248550_) {
      List<String> list = this.pathToRoot();
      if (p_248550_ >= 0 && p_248550_ < list.size()) {
         return this.createRelativePath(null, list.get(p_248550_));
      } else {
         throw new IllegalArgumentException("Invalid index: " + p_248550_);
      }
   }

   public LinkFSPath subpath(int p_251923_, int p_248807_) {
      List<String> list = this.pathToRoot();
      if (p_251923_ >= 0 && p_248807_ <= list.size() && p_251923_ < p_248807_) {
         LinkFSPath linkfspath = null;

         for (int i = p_251923_; i < p_248807_; i++) {
            linkfspath = this.createRelativePath(linkfspath, list.get(i));
         }

         return linkfspath;
      } else {
         throw new IllegalArgumentException();
      }
   }

   @Override
   public boolean startsWith(Path p_248923_) {
      if (p_248923_.isAbsolute() != this.isAbsolute()) {
         return false;
      }

      if (p_248923_ instanceof LinkFSPath linkfspath) {
         if (linkfspath.fileSystem != this.fileSystem) {
            return false;
         }

         List<String> list = this.pathToRoot();
         List<String> list1 = linkfspath.pathToRoot();
         int i = list1.size();
         if (i > list.size()) {
            return false;
         }

         for (int j = 0; j < i; j++) {
            if (!list1.get(j).equals(list.get(j))) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean endsWith(Path p_250070_) {
      if (p_250070_.isAbsolute() && !this.isAbsolute()) {
         return false;
      }

      if (p_250070_ instanceof LinkFSPath linkfspath) {
         if (linkfspath.fileSystem != this.fileSystem) {
            return false;
         }

         List<String> list = this.pathToRoot();
         List<String> list1 = linkfspath.pathToRoot();
         int i = list1.size();
         int j = list.size() - i;
         if (j < 0) {
            return false;
         }

         for (int k = i - 1; k >= 0; k--) {
            if (!list1.get(k).equals(list.get(j + k))) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public LinkFSPath normalize() {
      return this;
   }

   public LinkFSPath resolve(Path p_251657_) {
      LinkFSPath linkfspath = this.toLinkPath(p_251657_);
      return p_251657_.isAbsolute() ? linkfspath : this.resolve(linkfspath.pathToRoot());
   }

   private LinkFSPath resolve(List<String> p_252101_) {
      LinkFSPath linkfspath = this;

      for (String s : p_252101_) {
         linkfspath = linkfspath.resolveName(s);
      }

      return linkfspath;
   }

   LinkFSPath resolveName(String p_249718_) {
      if (isRelativeOrMissing(this.pathContents)) {
         return new LinkFSPath(this.fileSystem, p_249718_, this, this.pathContents);
      } else if (this.pathContents instanceof PathContents.DirectoryContents pathcontents$directorycontents) {
         LinkFSPath linkfspath = pathcontents$directorycontents.children().get(p_249718_);
         return linkfspath != null ? linkfspath : new LinkFSPath(this.fileSystem, p_249718_, this, PathContents.MISSING);
      } else if (this.pathContents instanceof PathContents.FileContents) {
         return new LinkFSPath(this.fileSystem, p_249718_, this, PathContents.MISSING);
      } else {
         throw new AssertionError("All content types should be already handled");
      }
   }

   private static boolean isRelativeOrMissing(PathContents p_248750_) {
      return p_248750_ == PathContents.MISSING || p_248750_ == PathContents.RELATIVE;
   }

   public LinkFSPath relativize(Path p_250294_) {
      LinkFSPath linkfspath = this.toLinkPath(p_250294_);
      if (this.isAbsolute() != linkfspath.isAbsolute()) {
         throw new IllegalArgumentException("absolute mismatch");
      }

      List<String> list = this.pathToRoot();
      List<String> list1 = linkfspath.pathToRoot();
      if (list.size() >= list1.size()) {
         throw new IllegalArgumentException();
      }

      for (int i = 0; i < list.size(); i++) {
         if (!list.get(i).equals(list1.get(i))) {
            throw new IllegalArgumentException();
         }
      }

      return linkfspath.subpath(list.size(), list1.size());
   }

   @Override
   public URI toUri() {
      try {
         return new URI("x-mc-link", this.fileSystem.store().name(), this.pathToString(), null);
      } catch (URISyntaxException urisyntaxexception) {
         throw new AssertionError("Failed to create URI", urisyntaxexception);
      }
   }

   public LinkFSPath toAbsolutePath() {
      return this.isAbsolute() ? this : this.fileSystem.rootPath().resolve(this);
   }

   public LinkFSPath toRealPath(LinkOption... p_251187_) {
      return this.toAbsolutePath();
   }

   @Override
   public WatchKey register(WatchService p_249189_, Kind<?>[] p_249917_, Modifier... p_251602_) {
      throw new UnsupportedOperationException();
   }

   @Override
   public int compareTo(Path p_250005_) {
      LinkFSPath linkfspath = this.toLinkPath(p_250005_);
      return PATH_COMPARATOR.compare(this, linkfspath);
   }

   @Override
   public boolean equals(Object p_248707_) {
      if (p_248707_ == this) {
         return true;
      } else if (!(p_248707_ instanceof LinkFSPath linkfspath)) {
         return false;
      } else if (this.fileSystem != linkfspath.fileSystem) {
         return false;
      } else {
         boolean flag = this.hasRealContents();
         if (flag != linkfspath.hasRealContents()) {
            return false;
         } else {
            return flag
               ? this.pathContents == linkfspath.pathContents
               : Objects.equals(this.parent, linkfspath.parent) && Objects.equals(this.name, linkfspath.name);
         }
      }
   }

   private boolean hasRealContents() {
      return !isRelativeOrMissing(this.pathContents);
   }

   @Override
   public int hashCode() {
      return this.hasRealContents() ? this.pathContents.hashCode() : this.name.hashCode();
   }

   @Override
   public String toString() {
      return this.pathToString();
   }

   private String pathToString() {
      if (this.pathString == null) {
         StringBuilder stringbuilder = new StringBuilder();
         if (this.isAbsolute()) {
            stringbuilder.append("/");
         }

         Joiner.on("/").appendTo(stringbuilder, this.pathToRoot());
         this.pathString = stringbuilder.toString();
      }

      return this.pathString;
   }

   private LinkFSPath toLinkPath(@Nullable Path p_250907_) {
      if (p_250907_ == null) {
         throw new NullPointerException();
      } else if (p_250907_ instanceof LinkFSPath linkfspath && linkfspath.fileSystem == this.fileSystem) {
         return linkfspath;
      } else {
         throw new ProviderMismatchException();
      }
   }

   public boolean exists() {
      return this.hasRealContents();
   }

   public @Nullable Path getTargetPath() {
      return this.pathContents instanceof PathContents.FileContents pathcontents$filecontents ? pathcontents$filecontents.contents() : null;
   }

   public PathContents.@Nullable DirectoryContents getDirectoryContents() {
      return this.pathContents instanceof PathContents.DirectoryContents pathcontents$directorycontents ? pathcontents$directorycontents : null;
   }

   public BasicFileAttributeView getBasicAttributeView() {
      return new BasicFileAttributeView() {
         @Override
         public String name() {
            return "basic";
         }

         @Override
         public BasicFileAttributes readAttributes() throws IOException {
            return LinkFSPath.this.getBasicAttributes();
         }

         @Override
         public void setTimes(FileTime p_249505_, FileTime p_250498_, FileTime p_251700_) {
            throw new ReadOnlyFileSystemException();
         }
      };
   }

   public BasicFileAttributes getBasicAttributes() throws IOException {
      if (this.pathContents instanceof PathContents.DirectoryContents) {
         return DIRECTORY_ATTRIBUTES;
      } else if (this.pathContents instanceof PathContents.FileContents) {
         return FILE_ATTRIBUTES;
      } else {
         throw new NoSuchFileException(this.pathToString());
      }
   }
}
