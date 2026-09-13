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

   private LinkFileSystem(final String name, final LinkFileSystem.DirectoryEntry rootEntry) {
      this.store = new LinkFSFileStore(name);
      this.root = buildPath(rootEntry, this, "", null);
   }

   private static LinkFSPath buildPath(
      final LinkFileSystem.DirectoryEntry entry, final LinkFileSystem fileSystem, final String selfName, final @Nullable LinkFSPath parent
   ) {
      Object2ObjectOpenHashMap<String, LinkFSPath> children = new Object2ObjectOpenHashMap();
      LinkFSPath result = new LinkFSPath(fileSystem, selfName, parent, new PathContents.DirectoryContents(children));
      entry.files.forEach((name, linkTarget) -> children.put(name, new LinkFSPath(fileSystem, name, result, new PathContents.FileContents(linkTarget))));
      entry.children.forEach((name, childEntry) -> children.put(name, buildPath(childEntry, fileSystem, name, result)));
      children.trim();
      return result;
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
   public Path getPath(final String first, final String... more) {
      Stream<String> path = Stream.of(first);
      if (more.length > 0) {
         path = Stream.concat(path, Stream.of(more));
      }

      String joinedPath = path.collect(Collectors.joining("/"));
      if (joinedPath.equals("/")) {
         return this.root;
      }

      if (joinedPath.startsWith("/")) {
         LinkFSPath result = this.root;

         for (String segment : PATH_SPLITTER.split(joinedPath.substring(1))) {
            if (segment.isEmpty()) {
               throw new IllegalArgumentException("Empty paths not allowed");
            }

            result = result.resolveName(segment);
         }

         return result;
      } else {
         LinkFSPath result = null;

         for (String segment : PATH_SPLITTER.split(joinedPath)) {
            if (segment.isEmpty()) {
               throw new IllegalArgumentException("Empty paths not allowed");
            }

            result = new LinkFSPath(this, segment, result, PathContents.RELATIVE);
         }

         if (result == null) {
            throw new IllegalArgumentException("Empty paths not allowed");
         } else {
            return result;
         }
      }
   }

   @Override
   public PathMatcher getPathMatcher(final String syntaxAndPattern) {
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

      public LinkFileSystem.Builder put(final List<String> path, final String name, final Path target) {
         LinkFileSystem.DirectoryEntry currentEntry = this.root;

         for (String segment : path) {
            currentEntry = currentEntry.children.computeIfAbsent(segment, n -> new LinkFileSystem.DirectoryEntry());
         }

         currentEntry.files.put(name, target);
         return this;
      }

      public LinkFileSystem.Builder put(final List<String> path, final Path target) {
         if (path.isEmpty()) {
            throw new IllegalArgumentException("Path can't be empty");
         }

         int lastIndex = path.size() - 1;
         return this.put(path.subList(0, lastIndex), path.get(lastIndex), target);
      }

      public FileSystem build(final String name) {
         return new LinkFileSystem(name, this.root);
      }
   }

   private record DirectoryEntry(Map<String, LinkFileSystem.DirectoryEntry> children, Map<String, Path> files) {
      public DirectoryEntry() {
         this(new HashMap<>(), new HashMap<>());
      }
   }
}
