package net.minecraft.world.level.validation;

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

   public PathAllowList(List<PathAllowList.ConfigEntry> p_289956_) {
      this.entries = p_289956_;
   }

   public PathMatcher getForFileSystem(FileSystem p_289975_) {
      return this.compiledPaths.computeIfAbsent(p_289975_.provider().getScheme(), p_289958_ -> {
         List<PathMatcher> list;
         try {
            list = this.entries.stream().map(p_289937_ -> p_289937_.compile(p_289975_)).toList();
         } catch (Exception exception) {
            LOGGER.error("Failed to compile file pattern list", exception);
            return p_289987_ -> false;
         }
         return switch (list.size()) {
            case 0 -> p_289982_ -> false;
            case 1 -> (PathMatcher)list.get(0);
            default -> p_289927_ -> {
               for (PathMatcher pathmatcher : list) {
                  if (pathmatcher.matches(p_289927_)) {
                     return true;
                  }
               }

               return false;
            };
         };
      });
   }

   @Override
   public boolean matches(Path p_289964_) {
      return this.getForFileSystem(p_289964_.getFileSystem()).matches(p_289964_);
   }

   public static PathAllowList readPlain(BufferedReader p_289921_) {
      return new PathAllowList(p_289921_.lines().flatMap(p_289962_ -> PathAllowList.ConfigEntry.parse(p_289962_).stream()).toList());
   }

   public record ConfigEntry(PathAllowList.EntryType type, String pattern) {
      public PathMatcher compile(FileSystem p_289936_) {
         return this.type().compile(p_289936_, this.pattern);
      }

      static Optional<PathAllowList.ConfigEntry> parse(String p_289947_) {
         if (p_289947_.isBlank() || p_289947_.startsWith("#")) {
            return Optional.empty();
         }

         if (!p_289947_.startsWith("[")) {
            return Optional.of(new PathAllowList.ConfigEntry(PathAllowList.EntryType.PREFIX, p_289947_));
         }

         int i = p_289947_.indexOf(93, 1);
         if (i == -1) {
            throw new IllegalArgumentException("Unterminated type in line '" + p_289947_ + "'");
         }

         String s = p_289947_.substring(1, i);
         String s1 = p_289947_.substring(i + 1);

         return switch (s) {
            case "glob", "regex" -> Optional.of(new PathAllowList.ConfigEntry(PathAllowList.EntryType.FILESYSTEM, s + ":" + s1));
            case "prefix" -> Optional.of(new PathAllowList.ConfigEntry(PathAllowList.EntryType.PREFIX, s1));
            default -> throw new IllegalArgumentException("Unsupported definition type in line '" + p_289947_ + "'");
         };
      }

      static PathAllowList.ConfigEntry glob(String p_289983_) {
         return new PathAllowList.ConfigEntry(PathAllowList.EntryType.FILESYSTEM, "glob:" + p_289983_);
      }

      static PathAllowList.ConfigEntry regex(String p_289944_) {
         return new PathAllowList.ConfigEntry(PathAllowList.EntryType.FILESYSTEM, "regex:" + p_289944_);
      }

      static PathAllowList.ConfigEntry prefix(String p_289918_) {
         return new PathAllowList.ConfigEntry(PathAllowList.EntryType.PREFIX, p_289918_);
      }
   }

   @FunctionalInterface
   public interface EntryType {
      PathAllowList.EntryType FILESYSTEM = FileSystem::getPathMatcher;
      PathAllowList.EntryType PREFIX = (p_289949_, p_289938_) -> p_289955_ -> p_289955_.toString().startsWith(p_289938_);

      PathMatcher compile(FileSystem var1, String var2);
   }
}
