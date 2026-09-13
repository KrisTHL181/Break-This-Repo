package net.minecraft.world.level.validation;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;

public class PathAllowList implements PathMatcher {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String COMMENT_PREFIX = "#";
   private final List<PathAllowList.ConfigEntry> entries;
   private final Map<String, PathMatcher> compiledPaths = new ConcurrentHashMap<>();

   public PathAllowList(final List<PathAllowList.ConfigEntry> entries) {
      this.entries = entries;
   }

   public PathMatcher getForFileSystem(final FileSystem fileSystem) {
      return this.compiledPaths.computeIfAbsent(fileSystem.provider().getScheme(), scheme -> {
         List<PathMatcher> compiledMatchers;
         try {
            compiledMatchers = this.entries.stream().map(e -> e.compile(fileSystem)).toList();
         } catch (Exception e) {
            LOGGER.error("Failed to compile file pattern list", e);
            return path -> false;
         }
         return switch (compiledMatchers.size()) {
            case 0 -> path -> false;
            case 1 -> (PathMatcher)compiledMatchers.get(0);
            default -> path -> {
               for (PathMatcher matcher : compiledMatchers) {
                  if (matcher.matches(path)) {
                     return true;
                  }
               }

               return false;
            };
         };
      });
   }

   @Override
   public boolean matches(final Path path) {
      return this.getForFileSystem(path.getFileSystem()).matches(path);
   }

   public static PathAllowList readPlain(final BufferedReader reader) {
      return new PathAllowList(reader.lines().flatMap(line -> PathAllowList.ConfigEntry.parse(line).stream()).toList());
   }

   public record ConfigEntry(PathAllowList.EntryType type, String pattern) {
      public PathMatcher compile(final FileSystem fileSystem) {
         return this.type().compile(fileSystem, this.pattern);
      }

      public static Optional<PathAllowList.ConfigEntry> parse(final String definition) {
         if (definition.isBlank() || definition.startsWith("#")) {
            return Optional.empty();
         }

         if (!definition.startsWith("[")) {
            return Optional.of(new PathAllowList.ConfigEntry(PathAllowList.EntryType.PREFIX, definition));
         }

         int split = definition.indexOf(93, 1);
         if (split == -1) {
            throw new IllegalArgumentException("Unterminated type in line '" + definition + "'");
         }

         String type = definition.substring(1, split);
         String contents = definition.substring(split + 1);

         return switch (type) {
            case "glob", "regex" -> Optional.of(new PathAllowList.ConfigEntry(PathAllowList.EntryType.FILESYSTEM, type + ":" + contents));
            case "prefix" -> Optional.of(new PathAllowList.ConfigEntry(PathAllowList.EntryType.PREFIX, contents));
            default -> throw new IllegalArgumentException("Unsupported definition type in line '" + definition + "'");
         };
      }

      @VisibleForTesting
      static PathAllowList.ConfigEntry glob(final String pattern) {
         return new PathAllowList.ConfigEntry(PathAllowList.EntryType.FILESYSTEM, "glob:" + pattern);
      }

      @VisibleForTesting
      static PathAllowList.ConfigEntry regex(final String pattern) {
         return new PathAllowList.ConfigEntry(PathAllowList.EntryType.FILESYSTEM, "regex:" + pattern);
      }

      @VisibleForTesting
      static PathAllowList.ConfigEntry prefix(final String pattern) {
         return new PathAllowList.ConfigEntry(PathAllowList.EntryType.PREFIX, pattern);
      }
   }

   @FunctionalInterface
   public interface EntryType {
      PathAllowList.EntryType FILESYSTEM = FileSystem::getPathMatcher;
      PathAllowList.EntryType PREFIX = (fileSystem, pattern) -> path -> path.toString().startsWith(pattern);

      PathMatcher compile(FileSystem fileSystem, String pattern);
   }
}
