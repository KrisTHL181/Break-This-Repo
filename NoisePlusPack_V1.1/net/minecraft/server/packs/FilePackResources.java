package net.minecraft.server.packs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;
import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class FilePackResources extends AbstractPackResources {
   static final Logger LOGGER = LogUtils.getLogger();
   private final FilePackResources.SharedZipFileAccess zipFileAccess;
   private final String prefix;

   FilePackResources(PackLocationInfo p_332104_, FilePackResources.SharedZipFileAccess p_298373_, String p_256076_) {
      super(p_332104_);
      this.zipFileAccess = p_298373_;
      this.prefix = p_256076_;
   }

   private static String getPathFromLocation(PackType p_250585_, Identifier p_459648_) {
      return String.format(Locale.ROOT, "%s/%s/%s", p_250585_.getDirectory(), p_459648_.getNamespace(), p_459648_.getPath());
   }

   @Override
   public @Nullable IoSupplier<InputStream> getRootResource(String... p_248514_) {
      return this.getResource(String.join("/", p_248514_));
   }

   @Override
   public IoSupplier<InputStream> getResource(PackType p_249605_, Identifier p_453422_) {
      return this.getResource(getPathFromLocation(p_249605_, p_453422_));
   }

   private String addPrefix(String p_299206_) {
      return this.prefix.isEmpty() ? p_299206_ : this.prefix + "/" + p_299206_;
   }

   private @Nullable IoSupplier<InputStream> getResource(String p_251795_) {
      ZipFile zipfile = this.zipFileAccess.getOrCreateZipFile();
      if (zipfile == null) {
         return null;
      }

      ZipEntry zipentry = zipfile.getEntry(this.addPrefix(p_251795_));
      return zipentry == null ? null : IoSupplier.create(zipfile, zipentry);
   }

   @Override
   public Set<String> getNamespaces(PackType p_10238_) {
      ZipFile zipfile = this.zipFileAccess.getOrCreateZipFile();
      if (zipfile == null) {
         return Set.of();
      }

      Enumeration<? extends ZipEntry> enumeration = zipfile.entries();
      Set<String> set = Sets.newHashSet();
      String s = this.addPrefix(p_10238_.getDirectory() + "/");

      while (enumeration.hasMoreElements()) {
         ZipEntry zipentry = enumeration.nextElement();
         String s1 = zipentry.getName();
         String s2 = extractNamespace(s, s1);
         if (!s2.isEmpty()) {
            if (Identifier.isValidNamespace(s2)) {
               set.add(s2);
            } else {
               LOGGER.warn("Non [a-z0-9_.-] character in namespace {} in pack {}, ignoring", s2, this.zipFileAccess.file);
            }
         }
      }

      return set;
   }

   @VisibleForTesting
   public static String extractNamespace(String p_298682_, String p_300360_) {
      if (!p_300360_.startsWith(p_298682_)) {
         return "";
      }

      int i = p_298682_.length();
      int j = p_300360_.indexOf(47, i);
      return j == -1 ? p_300360_.substring(i) : p_300360_.substring(i, j);
   }

   @Override
   public void close() {
      this.zipFileAccess.close();
   }

   @Override
   public void listResources(PackType p_250500_, String p_249598_, String p_251613_, PackResources.ResourceOutput p_250655_) {
      ZipFile zipfile = this.zipFileAccess.getOrCreateZipFile();
      if (zipfile != null) {
         Enumeration<? extends ZipEntry> enumeration = zipfile.entries();
         String s = this.addPrefix(p_250500_.getDirectory() + "/" + p_249598_ + "/");
         String s1 = s + p_251613_ + "/";

         while (enumeration.hasMoreElements()) {
            ZipEntry zipentry = enumeration.nextElement();
            if (!zipentry.isDirectory()) {
               String s2 = zipentry.getName();
               if (s2.startsWith(s1)) {
                  String s3 = s2.substring(s.length());
                  Identifier identifier = Identifier.tryBuild(p_249598_, s3);
                  if (identifier != null) {
                     p_250655_.accept(identifier, IoSupplier.create(zipfile, zipentry));
                  } else {
                     LOGGER.warn("Invalid path in datapack: {}:{}, ignoring", p_249598_, s3);
                  }
               }
            }
         }
      }
   }

   public static class FileResourcesSupplier implements Pack.ResourcesSupplier {
      private final File content;

      public FileResourcesSupplier(Path p_301133_) {
         this(p_301133_.toFile());
      }

      public FileResourcesSupplier(File p_299311_) {
         this.content = p_299311_;
      }

      @Override
      public PackResources openPrimary(PackLocationInfo p_336235_) {
         FilePackResources.SharedZipFileAccess filepackresources$sharedzipfileaccess = new FilePackResources.SharedZipFileAccess(this.content);
         return new FilePackResources(p_336235_, filepackresources$sharedzipfileaccess, "");
      }

      @Override
      public PackResources openFull(PackLocationInfo p_332542_, Pack.Metadata p_333099_) {
         FilePackResources.SharedZipFileAccess filepackresources$sharedzipfileaccess = new FilePackResources.SharedZipFileAccess(this.content);
         PackResources packresources = new FilePackResources(p_332542_, filepackresources$sharedzipfileaccess, "");
         List<String> list = p_333099_.overlays();
         if (list.isEmpty()) {
            return packresources;
         }

         List<PackResources> list1 = new ArrayList<>(list.size());

         for (String s : list) {
            list1.add(new FilePackResources(p_332542_, filepackresources$sharedzipfileaccess, s));
         }

         return new CompositePackResources(packresources, list1);
      }
   }

   static class SharedZipFileAccess implements AutoCloseable {
      final File file;
      private @Nullable ZipFile zipFile;
      private boolean failedToLoad;

      SharedZipFileAccess(File p_300196_) {
         this.file = p_300196_;
      }

      @Nullable ZipFile getOrCreateZipFile() {
         if (this.failedToLoad) {
            return null;
         }

         if (this.zipFile == null) {
            try {
               this.zipFile = new ZipFile(this.file);
            } catch (IOException ioexception) {
               FilePackResources.LOGGER.error("Failed to open pack {}", this.file, ioexception);
               this.failedToLoad = true;
               return null;
            }
         }

         return this.zipFile;
      }

      @Override
      public void close() {
         if (this.zipFile != null) {
            IOUtils.closeQuietly(this.zipFile);
            this.zipFile = null;
         }
      }

      @Override
      protected void finalize() throws Throwable {
         this.close();
         super.finalize();
      }
   }
}
