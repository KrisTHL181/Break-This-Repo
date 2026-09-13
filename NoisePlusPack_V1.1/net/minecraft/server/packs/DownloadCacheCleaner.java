package net.minecraft.server.packs;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;

public class DownloadCacheCleaner {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static void vacuumCacheDir(Path p_311487_, int p_312653_) {
      try {
         List<DownloadCacheCleaner.PathAndTime> list = listFilesWithModificationTimes(p_311487_);
         int i = list.size() - p_312653_;
         if (i <= 0) {
            return;
         }

         list.sort(DownloadCacheCleaner.PathAndTime.NEWEST_FIRST);
         List<DownloadCacheCleaner.PathAndPriority> list1 = prioritizeFilesInDirs(list);
         Collections.reverse(list1);
         list1.sort(DownloadCacheCleaner.PathAndPriority.HIGHEST_PRIORITY_FIRST);
         Set<Path> set = new HashSet<>();

         for (int j = 0; j < i; j++) {
            DownloadCacheCleaner.PathAndPriority downloadcachecleaner$pathandpriority = list1.get(j);
            Path path = downloadcachecleaner$pathandpriority.path;

            try {
               Files.delete(path);
               if (downloadcachecleaner$pathandpriority.removalPriority == 0) {
                  set.add(path.getParent());
               }
            } catch (IOException ioexception1) {
               LOGGER.warn("Failed to delete cache file {}", path, ioexception1);
            }
         }

         set.remove(p_311487_);

         for (Path path1 : set) {
            try {
               Files.delete(path1);
            } catch (DirectoryNotEmptyException var10) {
            } catch (IOException ioexception) {
               LOGGER.warn("Failed to delete empty(?) cache directory {}", path1, ioexception);
            }
         }
      } catch (UncheckedIOException | IOException ioexception2) {
         LOGGER.error("Failed to vacuum cache dir {}", p_311487_, ioexception2);
      }
   }

   private static List<DownloadCacheCleaner.PathAndTime> listFilesWithModificationTimes(final Path p_311706_) throws IOException {
      try {
         final List<DownloadCacheCleaner.PathAndTime> list = new ArrayList<>();
         Files.walkFileTree(p_311706_, new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path p_312027_, BasicFileAttributes p_309596_) {
               if (p_309596_.isRegularFile() && !p_312027_.getParent().equals(p_311706_)) {
                  FileTime filetime = p_309596_.lastModifiedTime();
                  list.add(new DownloadCacheCleaner.PathAndTime(p_312027_, filetime));
               }

               return FileVisitResult.CONTINUE;
            }
         });
         return list;
      } catch (NoSuchFileException nosuchfileexception) {
         return List.of();
      }
   }

   private static List<DownloadCacheCleaner.PathAndPriority> prioritizeFilesInDirs(List<DownloadCacheCleaner.PathAndTime> p_312641_) {
      List<DownloadCacheCleaner.PathAndPriority> list = new ArrayList<>();
      Object2IntOpenHashMap<Path> object2intopenhashmap = new Object2IntOpenHashMap();

      for (DownloadCacheCleaner.PathAndTime downloadcachecleaner$pathandtime : p_312641_) {
         int i = object2intopenhashmap.addTo(downloadcachecleaner$pathandtime.path.getParent(), 1);
         list.add(new DownloadCacheCleaner.PathAndPriority(downloadcachecleaner$pathandtime.path, i));
      }

      return list;
   }

   record PathAndPriority(Path path, int removalPriority) {
      public static final Comparator<DownloadCacheCleaner.PathAndPriority> HIGHEST_PRIORITY_FIRST = Comparator.comparing(
            DownloadCacheCleaner.PathAndPriority::removalPriority
         )
         .reversed();
   }

   record PathAndTime(Path path, FileTime modifiedTime) {
      public static final Comparator<DownloadCacheCleaner.PathAndTime> NEWEST_FIRST = Comparator.comparing(DownloadCacheCleaner.PathAndTime::modifiedTime)
         .reversed();
   }
}
