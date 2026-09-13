package net.minecraft.server.packs.linkfs;

import com.google.common.base.Splitter;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

public class LinkFileSystem extends FileSystem {
   private static final Set<String> VIEWS = Set.of("basic");
   public static final String PATH_SEPARATOR = "/";
   private static final Splitter PATH_SPLITTER = Splitter.on('/');
   private final FileStore store;
   private final FileSystemProvider provider = new LinkFSProvider();
   private final LinkFSPath root;

   LinkFileSystem(String p_251238_, LinkFileSystem.DirectoryEntry p_248738_) {
      this.store = new LinkFSFileStore(p_251238_);
      this.root = buildPath(p_248738_, this, "", null);
   }

   private static LinkFSPath buildPath(LinkFileSystem.DirectoryEntry p_250914_, LinkFileSystem p_248904_, String p_248935_, @Nullable LinkFSPath p_250296_) {
      Object2ObjectOpenHashMap<String, LinkFSPath> object2objectopenhashmap = new Object2ObjectOpenHashMap();
      LinkFSPath linkfspath = new LinkFSPath(p_248904_, p_248935_, p_250296_, new PathContents.DirectoryContents(object2objectopenhashmap));
      p_250914_.files
         .forEach(
            (p_249491_, p_250850_) -> object2objectopenhashmap.put(
               p_249491_, new LinkFSPath(p_248904_, p_249491_, linkfspath, new PathContents.FileContents(p_250850_))
            )
         );
      p_250914_.children.forEach((p_251592_, p_251728_) -> object2objectopenhashmap.put(p_251592_, buildPath(p_251728_, p_248904_, p_251592_, linkfspath)));
      object2objectopenhashmap.trim();
      return linkfspath;
   }

   @Override
   public FileSystemProvider provider() {
      return this.provider;
   }

   @Override
   public void close() {
   }

   @Override
   public boolean isOpen() {
      return true;
   }

   @Override
   public boolean isReadOnly() {
      return true;
   }

   @Override
   public String getSeparator() {
      return "/";
   }

   @Override
   public Iterable<Path> getRootDirectories() {
      return List.of(this.root);
   }

   @Override
   public Iterable<FileStore> getFileStores() {
      return List.of(this.store);
   }

   @Override
   public Set<String> supportedFileAttributeViews() {
      return VIEWS;
   }

   @Override
   public Path getPath(String p_250018_, String... p_252159_) {
      Stream<String> stream = Stream.of(p_250018_);
      if (p_252159_.length > 0) {
         stream = Stream.concat(stream, Stream.of(p_252159_));
      }

      String s = stream.collect(Collectors.joining("/"));
      if (s.equals("/")) {
         return this.root;
      }

      if (s.startsWith("/")) {
         LinkFSPath linkfspath1 = this.root;

         for (String s2 : PATH_SPLITTER.split(s.substring(1))) {
            if (s2.isEmpty()) {
               throw new IllegalArgumentException("Empty paths not allowed");
            }

            linkfspath1 = linkfspath1.resolveName(s2);
         }

         return linkfspath1;
      } else {
         LinkFSPath linkfspath = null;

         for (String s1 : PATH_SPLITTER.split(s)) {
            if (s1.isEmpty()) {
               throw new IllegalArgumentException("Empty paths not allowed");
            }

            linkfspath = new LinkFSPath(this, s1, linkfspath, PathContents.RELATIVE);
         }

         if (linkfspath == null) {
            throw new IllegalArgumentException("Empty paths not allowed");
         } else {
            return linkfspath;
         }
      }
   }

   @Override
   public PathMatcher getPathMatcher(String p_250757_) {
      throw new UnsupportedOperationException();
   }

   @Override
   public UserPrincipalLookupService getUserPrincipalLookupService() {
      throw new UnsupportedOperationException();
   }

   @Override
   public WatchService newWatchService() {
      throw new UnsupportedOperationException();
   }

   public FileStore store() {
      return this.store;
   }

   public LinkFSPath rootPath() {
      return this.root;
   }

   public static LinkFileSystem.Builder builder() {
      return new LinkFileSystem.Builder();
   }

   public static class Builder {
      private final LinkFileSystem.DirectoryEntry root = new LinkFileSystem.DirectoryEntry();

      public LinkFileSystem.Builder put(List<String> p_249758_, String p_251234_, Path p_248766_) {
         LinkFileSystem.DirectoryEntry linkfilesystem$directoryentry = this.root;

         for (String s : p_249758_) {
            linkfilesystem$directoryentry = linkfilesystem$directoryentry.children.computeIfAbsent(s, p_249671_ -> new LinkFileSystem.DirectoryEntry());
         }

         linkfilesystem$directoryentry.files.put(p_251234_, p_248766_);
         return this;
      }

      public LinkFileSystem.Builder put(List<String> p_250158_, Path p_250483_) {
         if (p_250158_.isEmpty()) {
            throw new IllegalArgumentException("Path can't be empty");
         }

         int i = p_250158_.size() - 1;
         return this.put(p_250158_.subList(0, i), p_250158_.get(i), p_250483_);
      }

      public FileSystem build(String p_251975_) {
         return new LinkFileSystem(p_251975_, this.root);
      }
   }

   record DirectoryEntry(Map<String, LinkFileSystem.DirectoryEntry> children, Map<String, Path> files) {
      public DirectoryEntry() {
         this(new HashMap<>(), new HashMap<>());
      }
   }
}
